package com.fsck.k9.preferences

import com.fsck.k9.helper.mapCollectionToSet
import com.fsck.k9.preferences.ServerSettingsDescriptions.AUTHENTICATION_TYPE
import com.fsck.k9.preferences.ServerSettingsDescriptions.HOST
import com.fsck.k9.preferences.ServerSettingsDescriptions.PASSWORD
import com.fsck.k9.preferences.ServerSettingsDescriptions.USERNAME
import com.fsck.k9.preferences.Settings.InvalidSettingValueException
import java.io.InputStream
import net.thunderbird.core.logging.legacy.Log

@Suppress("LongParameterList")
class SettingsImporter internal constructor(
    private val settingsFileParser: SettingsFileParser,
    private val generalSettingsValidator: GeneralSettingsValidator,
    private val accountSettingsValidator: AccountSettingsValidator,
    private val generalSettingsUpgrader: GeneralSettingsUpgrader,
    private val accountSettingsUpgrader: AccountSettingsUpgrader,
    private val generalSettingsWriter: GeneralSettingsWriter,
    private val accountSettingsWriter: AccountSettingsWriter,
    private val unifiedInboxConfigurator: UnifiedInboxConfigurator,
) {
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
    @Suppress("TooGenericExceptionCaught", "ThrowsCount")
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

            if (!globalSettings && accounts.isEmpty()) {
                throw SettingsImportExportException("Neither global settings nor account settings could be found")
            }

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
    @Suppress("TooGenericExceptionCaught")
    @Throws(SettingsImportExportException::class)
    suspend fun importSettings(
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
                    Log.e(e, "Encountered invalid setting while importing account \"%s\"", account.name)

                    erroneousAccounts.add(AccountDescription(account.name!!, account.uuid))
                } catch (e: Exception) {
                    Log.e(e, "Exception while importing account \"%s\"", account.name)

                    erroneousAccounts.add(AccountDescription(account.name!!, account.uuid))
                }
            }

            if (!globalSettingsImported) {
                unifiedInboxConfigurator.configureUnifiedInbox()
            }

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
            Log.w("Was asked to import global settings but none found.")
        }

        val accountUuids = contents.accounts.mapCollectionToSet { it.uuid }
        for (importAccountUuid in importAccountUuids) {
            if (importAccountUuid !in accountUuids) {
                Log.w("Was asked to import account %s. But this account wasn't found.", importAccountUuid)
            }
        }

        return contents.copy(
            globalSettings = contents.globalSettings.takeIf { importGeneralSettings },
            accounts = contents.accounts.filter { it.uuid in importAccountUuids },
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private fun importGeneralSettings(contentVersion: Int, settings: SettingsMap): Boolean {
        return try {
            val validatedSettings = generalSettingsValidator.validate(contentVersion, settings)

            val currentSettings = generalSettingsUpgrader.upgrade(contentVersion, validatedSettings)

            generalSettingsWriter.write(currentSettings)
        } catch (e: Exception) {
            Log.e(e, "Exception while importing general settings")
            false
        }
    }

    private suspend fun importAccount(
        contentVersion: Int,
        account: SettingsFile.Account,
    ): AccountDescriptionPair {
        val validatedAccount = accountSettingsValidator.validate(contentVersion, account)

        val currentAccount = accountSettingsUpgrader.upgrade(contentVersion, validatedAccount)

        val accountMapping = accountSettingsWriter.write(currentAccount)

        val incoming = currentAccount.incoming
        val incomingServerName = incoming.settings[HOST] as? String
        val incomingAuthenticationType = incoming.settings[AUTHENTICATION_TYPE] as String
        val incomingPassword = incoming.settings[PASSWORD] as? String
        val incomingPasswordNeeded =
            incomingAuthenticationType != "EXTERNAL" &&
                incomingAuthenticationType != "XOAUTH2" &&
                incomingPassword.isNullOrEmpty()

        var authorizationNeeded = incomingAuthenticationType == "XOAUTH2"

        val outgoing = currentAccount.outgoing
        val outgoingServerName = outgoing.settings[HOST] as? String
        val outgoingAuthenticationType = outgoing.settings[AUTHENTICATION_TYPE] as String
        val outgoingUsername = outgoing.settings[USERNAME] as String
        val outgoingPassword = outgoing.settings[PASSWORD] as? String
        val outgoingPasswordNeeded =
            outgoingAuthenticationType != "EXTERNAL" &&
                outgoingAuthenticationType != "XOAUTH2" &&
                outgoingUsername.isNotEmpty() &&
                outgoingPassword.isNullOrEmpty()

        authorizationNeeded = authorizationNeeded || outgoingAuthenticationType == "XOAUTH2"

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
