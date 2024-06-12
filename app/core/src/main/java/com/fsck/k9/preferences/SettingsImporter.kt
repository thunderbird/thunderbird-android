package com.fsck.k9.preferences

import android.content.Context
import com.fsck.k9.AccountPreferenceSerializer
import com.fsck.k9.Core
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.ServerSettingsSerializer
import com.fsck.k9.mailstore.SpecialLocalFoldersCreator
import com.fsck.k9.preferences.Settings.InvalidSettingValueException
import java.io.InputStream
import kotlinx.datetime.Clock
import timber.log.Timber

// TODO: Further refactor this class to be able to get rid of these detekt issues.
@Suppress(
    "LongMethod",
    "CyclomaticComplexMethod",
    "NestedBlockDepth",
    "TooManyFunctions",
    "TooGenericExceptionCaught",
    "SwallowedException",
    "ReturnCount",
    "ThrowsCount",
)
class SettingsImporter internal constructor(
    private val settingsFileParser: SettingsFileParser,
    private val preferences: Preferences,
    private val generalSettingsManager: RealGeneralSettingsManager,
    private val localFoldersCreator: SpecialLocalFoldersCreator,
    private val serverSettingsSerializer: ServerSettingsSerializer,
    private val clock: Clock,
    private val context: Context,
) {
    private val generalSettingsValidator = GeneralSettingsValidator()
    private val accountSettingsValidator = AccountSettingsValidator()

    private val generalSettingsUpgrader = GeneralSettingsUpgrader()
    private val accountSettingsUpgrader = AccountSettingsUpgrader()

    private val generalSettingsWriter = GeneralSettingsWriter(preferences)
    private val accountSettingsWriter = AccountSettingsWriter(preferences, clock, serverSettingsSerializer)

    /**
     * Parses an import [InputStream] and returns information on whether it contains global settings and/or account
     * settings. For all account configurations found, the name of the account along with the account UUID is returned.
     *
     * @param inputStream An `InputStream` to read the settings from.
     *
     * @return An [ImportContents] instance containing information about the contents of the settings file.
     *
     * @throws SettingsImportExportException In case of an error.
     */
    @Throws(SettingsImportExportException::class)
    fun getImportStreamContents(inputStream: InputStream): ImportContents {
        try {
            val imported = settingsFileParser.parseSettings(inputStream)

            // If the stream contains global settings the "globalSettings" member will not be null
            val globalSettings = (imported.globalSettings != null)

            val accounts = imported.accounts.map { importedAccount ->
                AccountDescription(
                    name = getAccountDisplayName(importedAccount),
                    uuid = importedAccount.uuid,
                )
            }

            // TODO: throw exception if neither global settings nor account settings could be found
            return ImportContents(globalSettings, accounts)
        } catch (e: SettingsImportExportException) {
            throw e
        } catch (e: Exception) {
            throw SettingsImportExportException(e)
        }
    }

    /**
     * Reads an import [InputStream] and imports the global settings and/or account configurations specified by the
     * arguments.
     *
     * @param inputStream The `InputStream` to read the settings from.
     * @param globalSettings `true` if global settings should be imported from the file.
     * @param accountUuids A list of UUIDs of the accounts that should be imported.
     *
     * @return An [ImportResults] instance containing information about errors and successfully imported accounts.
     *
     * @throws SettingsImportExportException In case of an error.
     */
    @Throws(SettingsImportExportException::class)
    fun importSettings(
        inputStream: InputStream,
        globalSettings: Boolean,
        accountUuids: List<String>,
    ): ImportResults {
        try {
            var globalSettingsImported = false
            val importedAccounts = mutableListOf<AccountDescriptionPair>()
            val erroneousAccounts = mutableListOf<AccountDescription>()

            val settings = settingsFileParser.parseSettings(inputStream)

            val filteredGlobalSettings = if (globalSettings) {
                settings.globalSettings
            } else {
                null
            }

            val filteredAccounts = settings.accounts.filter { it.uuid in accountUuids }

            val imported = settings.copy(
                globalSettings = filteredGlobalSettings,
                accounts = filteredAccounts,
            )

            if (globalSettings) {
                if (imported.globalSettings != null) {
                    globalSettingsImported = importGeneralSettings(imported.contentVersion, imported.globalSettings)
                } else {
                    Timber.w("Was asked to import global settings but none found.")
                }
            }

            if (accountUuids.isNotEmpty()) {
                val foundAccountUuids = imported.accounts.map { it.uuid }.toSet()
                val missingAccountUuids = accountUuids.toSet() - foundAccountUuids
                if (missingAccountUuids.isNotEmpty()) {
                    for (accountUuid in missingAccountUuids) {
                        Timber.w("Was asked to import account %s. But this account wasn't found.", accountUuid)
                    }
                }

                for (account in imported.accounts) {
                    try {
                        var editor = preferences.createStorageEditor()

                        val importResult = importAccount(editor, imported.contentVersion, account)

                        if (editor.commit()) {
                            Timber.v(
                                "Committed settings for account \"%s\" to the settings database.",
                                importResult.imported.name,
                            )

                            // Add UUID of the account we just imported to the list of account UUIDs
                            editor = preferences.createStorageEditor()

                            val newUuid = importResult.imported.uuid
                            val oldAccountUuids = preferences.storage.getString("accountUuids", "")
                            val newAccountUuids = if (oldAccountUuids.isNotEmpty()) {
                                "$oldAccountUuids,$newUuid"
                            } else {
                                newUuid
                            }

                            putString(editor, "accountUuids", newAccountUuids)

                            if (!editor.commit()) {
                                throw SettingsImportExportException("Failed to set account UUID list")
                            }

                            // Reload accounts
                            preferences.loadAccounts()

                            importedAccounts.add(importResult)
                        } else {
                            Timber.w(
                                "Error while committing settings for account \"%s\" to the settings database.",
                                importResult.original.name,
                            )

                            erroneousAccounts.add(importResult.original)
                        }
                    } catch (e: InvalidSettingValueException) {
                        Timber.e(e, "Encountered invalid setting while importing account \"%s\"", account.name)

                        erroneousAccounts.add(AccountDescription(account.name!!, account.uuid))
                    } catch (e: Exception) {
                        Timber.e(e, "Exception while importing account \"%s\"", account.name)

                        erroneousAccounts.add(AccountDescription(account.name!!, account.uuid))
                    }
                }

                val editor = preferences.createStorageEditor()

                if (!editor.commit()) {
                    throw SettingsImportExportException("Failed to set default account")
                }
            }

            preferences.loadAccounts()

            // Create special local folders
            for (importedAccount in importedAccounts) {
                val accountUuid = importedAccount.imported.uuid
                val account = preferences.getAccount(accountUuid) ?: error("Failed to load account: $accountUuid")

                localFoldersCreator.createSpecialLocalFolders(account)
            }

            generalSettingsManager.loadSettings()
            Core.setServicesEnabled(context)

            return ImportResults(globalSettingsImported, importedAccounts, erroneousAccounts)
        } catch (e: SettingsImportExportException) {
            throw e
        } catch (e: Exception) {
            throw SettingsImportExportException(e)
        }
    }

    private fun importGeneralSettings(contentVersion: Int, settings: SettingsMap): Boolean {
        return try {
            val validatedSettings = generalSettingsValidator.validate(contentVersion, settings)

            val currentSettings = generalSettingsUpgrader.upgrade(contentVersion, validatedSettings)

            generalSettingsWriter.write(currentSettings)
        } catch (e: Exception) {
            Timber.e(e, "Exception while importing general settings")
            false
        }
    }

    @Throws(InvalidSettingValueException::class)
    private fun importAccount(
        editor: StorageEditor,
        contentVersion: Int,
        account: SettingsFile.Account,
    ): AccountDescriptionPair {
        val validatedAccount = accountSettingsValidator.validate(contentVersion, account)

        val currentAccount = accountSettingsUpgrader.upgrade(contentVersion, validatedAccount)

        val accountMapping = accountSettingsWriter.write(editor, currentAccount)

        val incoming = currentAccount.incoming
        val incomingServerName = incoming.host
        val incomingPasswordNeeded =
            incoming.authenticationType != "EXTERNAL" && incoming.authenticationType != "XOAUTH2" &&
                incoming.password.isNullOrEmpty()

        var authorizationNeeded = incoming.authenticationType == "XOAUTH2"

        val outgoing = currentAccount.outgoing
        val outgoingServerName = outgoing.host
        val outgoingPasswordNeeded =
            outgoing.authenticationType != "EXTERNAL" && outgoing.authenticationType != "XOAUTH2" &&
                outgoing.username.isNotEmpty() && outgoing.password.isNullOrEmpty()

        authorizationNeeded = authorizationNeeded || outgoing.authenticationType == "XOAUTH2"

        return AccountDescriptionPair(
            accountMapping.first,
            accountMapping.second,
            authorizationNeeded,
            incomingPasswordNeeded,
            outgoingPasswordNeeded,
            incomingServerName!!,
            outgoingServerName!!,
        )
    }

    /**
     * Write to a [StorageEditor] while logging what is written if debug logging is enabled.
     *
     * @param editor The `Editor` to write to.
     * @param key The name of the preference to modify.
     * @param value The new value for the preference.
     */
    private fun putString(editor: StorageEditor, key: String, value: String?) {
        if (K9.isDebugLoggingEnabled) {
            var outputValue = value
            if (!K9.isSensitiveDebugLoggingEnabled &&
                (
                    key.endsWith("." + AccountPreferenceSerializer.OUTGOING_SERVER_SETTINGS_KEY) ||
                        key.endsWith("." + AccountPreferenceSerializer.INCOMING_SERVER_SETTINGS_KEY)
                    )
            ) {
                outputValue = "*sensitive*"
            }

            Timber.v("Setting %s=%s", key, outputValue)
        }

        editor.putString(key, value)
    }

    private fun getAccountDisplayName(account: SettingsFile.Account): String {
        return account.name?.takeIf { it.isNotEmpty() }
            ?: account.identities?.firstOrNull()?.email
            ?: error("Account name missing")
    }
}
