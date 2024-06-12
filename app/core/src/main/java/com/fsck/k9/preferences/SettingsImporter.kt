package com.fsck.k9.preferences

import android.content.Context
import com.fsck.k9.Preferences
import com.fsck.k9.ServerSettingsSerializer
import com.fsck.k9.helper.mapCollectionToSet
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
    private val accountSettingsWriter = AccountSettingsWriter(
        preferences,
        localFoldersCreator,
        clock,
        serverSettingsSerializer,
        context,
    )

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

            val allContents = settingsFileParser.parseSettings(inputStream)

            val contents = filterSettings(allContents, globalSettings, accountUuids)

            if (contents.globalSettings != null) {
                globalSettingsImported = importGeneralSettings(contents.contentVersion, contents.globalSettings)
            }

            for (account in contents.accounts) {
                try {
                    val importResult = importAccount(contents.contentVersion, account)
                    importedAccounts.add(importResult)
                } catch (e: InvalidSettingValueException) {
                    Timber.e(e, "Encountered invalid setting while importing account \"%s\"", account.name)

                    erroneousAccounts.add(AccountDescription(account.name!!, account.uuid))
                } catch (e: Exception) {
                    Timber.e(e, "Exception while importing account \"%s\"", account.name)

                    erroneousAccounts.add(AccountDescription(account.name!!, account.uuid))
                }
            }

            generalSettingsManager.loadSettings()

            return ImportResults(globalSettingsImported, importedAccounts, erroneousAccounts)
        } catch (e: SettingsImportExportException) {
            throw e
        } catch (e: Exception) {
            throw SettingsImportExportException(e)
        }
    }

    private fun filterSettings(
        contents: SettingsFile.Contents,
        importGeneralSettings: Boolean,
        importAccountUuids: List<String>,
    ): SettingsFile.Contents {
        if (importGeneralSettings && contents.globalSettings == null) {
            Timber.w("Was asked to import global settings but none found.")
        }

        val accountUuids = contents.accounts.mapCollectionToSet { it.uuid }
        for (importAccountUuid in importAccountUuids) {
            if (importAccountUuid !in accountUuids) {
                Timber.w("Was asked to import account %s. But this account wasn't found.", importAccountUuid)
            }
        }

        return contents.copy(
            globalSettings = contents.globalSettings.takeIf { importGeneralSettings },
            accounts = contents.accounts.filter { it.uuid in importAccountUuids },
        )
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

    private fun importAccount(
        contentVersion: Int,
        account: SettingsFile.Account,
    ): AccountDescriptionPair {
        val validatedAccount = accountSettingsValidator.validate(contentVersion, account)

        val currentAccount = accountSettingsUpgrader.upgrade(contentVersion, validatedAccount)

        val accountMapping = accountSettingsWriter.write(currentAccount)

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

    private fun getAccountDisplayName(account: SettingsFile.Account): String {
        return account.name?.takeIf { it.isNotEmpty() }
            ?: account.identities?.firstOrNull()?.email
            ?: error("Account name missing")
    }
}
