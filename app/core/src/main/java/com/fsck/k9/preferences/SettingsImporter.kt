package com.fsck.k9.preferences

import android.content.Context
import android.text.TextUtils
import com.fsck.k9.Account
import com.fsck.k9.AccountPreferenceSerializer
import com.fsck.k9.Core
import com.fsck.k9.DI
import com.fsck.k9.Identity
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.ServerSettingsSerializer
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mailstore.SpecialLocalFoldersCreator
import com.fsck.k9.preferences.ServerTypeConverter.toServerSettingsType
import com.fsck.k9.preferences.Settings.InvalidSettingValueException
import java.io.InputStream
import java.util.Collections
import java.util.UUID
import kotlinx.datetime.Clock
import timber.log.Timber

object SettingsImporter {
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
    fun getImportStreamContents(inputStream: InputStream?): ImportContents {
        try {
            // Parse the import stream but don't save individual settings (overview=true)
            val settingsFileParser = SettingsFileParser()
            val imported = settingsFileParser.parseSettings(inputStream!!, false, null, true)

            // If the stream contains global settings the "globalSettings" member will not be null
            val globalSettings = (imported.globalSettings != null)

            val accounts: MutableList<AccountDescription> = ArrayList()
            // If the stream contains at least one account configuration the "accounts" member will not be null.
            if (imported.accounts != null) {
                for (account in imported.accounts.values) {
                    val accountName = getAccountDisplayName(account)
                    accounts.add(AccountDescription(accountName!!, account.uuid))
                }
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
     * @param context A [Context] instance.
     * @param inputStream The `InputStream` to read the settings from.
     * @param globalSettings `true` if global settings should be imported from the file.
     * @param accountUuids A list of UUIDs of the accounts that should be imported.
     * @param overwrite `true` if existing accounts should be overwritten when an account with the same UUID is found
     *   in the settings file.
     *
     *   **Note:** This can have side-effects we currently don't handle, e.g. changing the account type from IMAP to
     *   POP3. So don't use this for now!
     *
     * @return An [ImportResults] instance containing information about errors and successfully imported accounts.
     *
     * @throws SettingsImportExportException In case of an error.
     */
    @Throws(SettingsImportExportException::class)
    fun importSettings(
        context: Context,
        inputStream: InputStream?,
        globalSettings: Boolean,
        accountUuids: List<String>?,
        overwrite: Boolean,
    ): ImportResults {
        try {
            var globalSettingsImported = false
            val importedAccounts: MutableList<AccountDescriptionPair> = ArrayList()
            val erroneousAccounts: MutableList<AccountDescription> = ArrayList()

            val settingsFileParser = SettingsFileParser()
            val imported = settingsFileParser.parseSettings(inputStream!!, globalSettings, accountUuids, false)

            val preferences = Preferences.getPreferences()
            val storage = preferences.storage

            if (globalSettings) {
                try {
                    val editor = preferences.createStorageEditor()
                    if (imported.globalSettings != null) {
                        importGlobalSettings(storage, editor, imported.contentVersion, imported.globalSettings)
                    } else {
                        Timber.w("Was asked to import global settings but none found.")
                    }
                    if (editor.commit()) {
                        Timber.v("Committed global settings to the preference storage.")
                        globalSettingsImported = true
                    } else {
                        Timber.v("Failed to commit global settings to the preference storage")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Exception while importing global settings")
                }
            }

            if (accountUuids != null && accountUuids.size > 0) {
                if (imported.accounts != null) {
                    for (accountUuid in accountUuids) {
                        if (imported.accounts.containsKey(accountUuid)) {
                            val account = imported.accounts[accountUuid]
                            try {
                                var editor = preferences.createStorageEditor()

                                val importResult = importAccount(editor, imported.contentVersion, account, overwrite)

                                if (editor.commit()) {
                                    Timber.v(
                                        "Committed settings for account \"%s\" to the settings database.",
                                        importResult.imported.name,
                                    )

                                    // Add UUID of the account we just imported to the list of account UUIDs
                                    if (!importResult.overwritten) {
                                        editor = preferences.createStorageEditor()

                                        val newUuid = importResult.imported.uuid
                                        val oldAccountUuids = preferences.storage.getString("accountUuids", "")
                                        val newAccountUuids =
                                            if ((oldAccountUuids.length > 0)) "$oldAccountUuids,$newUuid" else newUuid

                                        putString(editor, "accountUuids", newAccountUuids)

                                        if (!editor.commit()) {
                                            throw SettingsImportExportException("Failed to set account UUID list")
                                        }
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
                                var reason = e.message

                                if (TextUtils.isEmpty(reason)) {
                                    reason = "Unknown"
                                }

                                Timber.e(
                                    e,
                                    "Encountered invalid setting while importing account \"%s\", reason: \"%s\"",
                                    account!!.name,
                                    reason,
                                )

                                erroneousAccounts.add(AccountDescription(account.name!!, account.uuid))
                            } catch (e: Exception) {
                                Timber.e(e, "Exception while importing account \"%s\"", account!!.name)

                                erroneousAccounts.add(AccountDescription(account.name!!, account.uuid))
                            }
                        } else {
                            Timber.w(
                                "Was asked to import account with UUID %s. But this account wasn't found.",
                                accountUuid,
                            )
                        }
                    }

                    val editor = preferences.createStorageEditor()

                    if (!editor.commit()) {
                        throw SettingsImportExportException("Failed to set default account")
                    }
                } else {
                    Timber.w("Was asked to import at least one account but none found.")
                }
            }

            preferences.loadAccounts()

            val localFoldersCreator = DI.get(SpecialLocalFoldersCreator::class.java)

            // Create special local folders
            for ((_, imported1) in importedAccounts) {
                val accountUuid = imported1.uuid
                val account = preferences.getAccount(accountUuid)

                localFoldersCreator.createSpecialLocalFolders(account!!)
            }

            DI.get(RealGeneralSettingsManager::class.java).loadSettings()
            Core.setServicesEnabled(context)

            return ImportResults(globalSettingsImported, importedAccounts, erroneousAccounts)
        } catch (e: SettingsImportExportException) {
            throw e
        } catch (e: Exception) {
            throw SettingsImportExportException(e)
        }
    }

    private fun importGlobalSettings(
        storage: Storage,
        editor: StorageEditor,
        contentVersion: Int,
        settings: ImportedSettings?,
    ) {
        // Validate global settings
        val validatedSettings = GeneralSettingsDescriptions.validate(contentVersion, settings!!.settings)

        // Upgrade global settings to current content version
        if (contentVersion != Settings.VERSION) {
            GeneralSettingsDescriptions.upgrade(contentVersion, validatedSettings)
        }

        // Convert global settings to the string representation used in preference storage
        val stringSettings = GeneralSettingsDescriptions.convert(validatedSettings)

        // Use current global settings as base and overwrite with validated settings read from the import file.
        val mergedSettings: MutableMap<String, String> = HashMap(GeneralSettingsDescriptions.getGlobalSettings(storage))
        mergedSettings.putAll(stringSettings)

        for ((key, value) in mergedSettings) {
            putString(editor, key, value)
        }
    }

    @Throws(InvalidSettingValueException::class)
    private fun importAccount(
        editor: StorageEditor,
        contentVersion: Int,
        account: ImportedAccount?,
        overwrite: Boolean,
    ): AccountDescriptionPair {
        val original = AccountDescription(account!!.name!!, account.uuid)

        val prefs = Preferences.getPreferences()
        val accounts = prefs.getAccounts()

        var uuid = account.uuid
        val existingAccount = prefs.getAccount(uuid)
        val mergeImportedAccount = (overwrite && existingAccount != null)

        if (!overwrite && existingAccount != null) {
            // An account with this UUID already exists, but we're not allowed to overwrite it. So generate a new UUID.
            uuid = UUID.randomUUID().toString()
        }

        // Make sure the account name is unique
        var accountName = account.name
        if (isAccountNameUsed(accountName, accounts)) {
            // Account name is already in use. So generate a new one by appending " (x)", where x is the first
            // number >= 1 that results in an unused account name.
            for (i in 1..accounts.size) {
                accountName = account.name + " (" + i + ")"
                if (!isAccountNameUsed(accountName, accounts)) {
                    break
                }
            }
        }

        // Write account name
        val accountKeyPrefix = "$uuid."
        putString(editor, accountKeyPrefix + AccountPreferenceSerializer.ACCOUNT_DESCRIPTION_KEY, accountName)

        if (account.incoming == null) {
            // We don't import accounts without incoming server settings
            throw InvalidSettingValueException("Missing incoming server settings")
        }

        // Write incoming server settings
        val incoming = createServerSettings(account.incoming)
        val serverSettingsSerializer = DI.get(ServerSettingsSerializer::class.java)
        val incomingServer = serverSettingsSerializer.serialize(incoming)
        putString(editor, accountKeyPrefix + AccountPreferenceSerializer.INCOMING_SERVER_SETTINGS_KEY, incomingServer)

        val incomingServerName = incoming.host
        val incomingPasswordNeeded =
            AuthType.EXTERNAL != incoming.authenticationType && AuthType.XOAUTH2 != incoming.authenticationType &&
                (incoming.password == null || incoming.password!!.isEmpty())

        var authorizationNeeded = incoming.authenticationType == AuthType.XOAUTH2

        if (account.outgoing == null) {
            throw InvalidSettingValueException("Missing outgoing server settings")
        }

        var outgoingServerName: String? = null
        var outgoingPasswordNeeded = false
        // Write outgoing server settings
        val outgoing = createServerSettings(account.outgoing)
        val outgoingServer = serverSettingsSerializer.serialize(outgoing)
        putString(editor, accountKeyPrefix + AccountPreferenceSerializer.OUTGOING_SERVER_SETTINGS_KEY, outgoingServer)

        /*
         * Mark account as disabled if the settings file contained a username but no password, except when the
         * AuthType is EXTERNAL.
         */
        outgoingPasswordNeeded =
            (
                AuthType.EXTERNAL != outgoing.authenticationType && AuthType.XOAUTH2 != outgoing.authenticationType &&
                    outgoing.username != null && !outgoing.username.isEmpty()
                ) &&
            (outgoing.password == null || outgoing.password!!.isEmpty())

        authorizationNeeded = authorizationNeeded or (outgoing.authenticationType == AuthType.XOAUTH2)

        outgoingServerName = outgoing.host

        val createAccountDisabled = incomingPasswordNeeded || outgoingPasswordNeeded || authorizationNeeded
        if (createAccountDisabled) {
            editor.putBoolean(accountKeyPrefix + "enabled", false)
        }

        // Validate account settings
        val validatedSettings =
            AccountSettingsDescriptions.validate(contentVersion, account.settings!!.settings, !mergeImportedAccount)

        // Upgrade account settings to current content version
        if (contentVersion != Settings.VERSION) {
            AccountSettingsDescriptions.upgrade(contentVersion, validatedSettings)
        }

        // Convert account settings to the string representation used in preference storage
        val stringSettings = AccountSettingsDescriptions.convert(validatedSettings)

        // Merge account settings if necessary
        val writeSettings: MutableMap<String, String>
        if (mergeImportedAccount) {
            writeSettings = HashMap(AccountSettingsDescriptions.getAccountSettings(prefs.storage, uuid))
            writeSettings.putAll(stringSettings)
        } else {
            writeSettings = stringSettings
        }

        // Write account settings
        for ((key1, value) in writeSettings) {
            val key = accountKeyPrefix + key1
            putString(editor, key, value)
        }

        // If it's a new account generate and write a new "accountNumber"
        if (!mergeImportedAccount) {
            val newAccountNumber = prefs.generateAccountNumber()
            putString(editor, accountKeyPrefix + "accountNumber", newAccountNumber.toString())
        }

        // Write identities
        if (account.identities != null) {
            importIdentities(editor, contentVersion, uuid, account, overwrite, existingAccount, prefs)
        } else if (!mergeImportedAccount) {
            // Require accounts to at least have one identity
            throw InvalidSettingValueException("Missing identities, there should be at least one.")
        }

        // Write folder settings
        if (account.folders != null) {
            for (folder in account.folders) {
                importFolder(editor, contentVersion, uuid, folder, mergeImportedAccount, prefs)
            }
        }

        // When deleting an account and then restoring it using settings import, the same account UUID will be used.
        // To avoid reusing a previously existing notification channel ID, we need to make sure to use a unique value
        // for `messagesNotificationChannelVersion`.
        val clock = DI.get(Clock::class.java)
        val messageNotificationChannelVersion = clock.now().epochSeconds.toString()
        putString(editor, accountKeyPrefix + "messagesNotificationChannelVersion", messageNotificationChannelVersion)

        val imported = AccountDescription(accountName!!, uuid)
        return AccountDescriptionPair(
            original,
            imported,
            mergeImportedAccount,
            authorizationNeeded,
            incomingPasswordNeeded,
            outgoingPasswordNeeded,
            incomingServerName!!,
            outgoingServerName!!,
        )
    }

    private fun importFolder(
        editor: StorageEditor,
        contentVersion: Int,
        uuid: String,
        folder: ImportedFolder,
        overwrite: Boolean,
        prefs: Preferences,
    ) {
        // Validate folder settings

        val validatedSettings =
            FolderSettingsDescriptions.validate(contentVersion, folder.settings!!.settings, !overwrite)

        // Upgrade folder settings to current content version
        if (contentVersion != Settings.VERSION) {
            FolderSettingsDescriptions.upgrade(contentVersion, validatedSettings)
        }

        // Convert folder settings to the string representation used in preference storage
        val stringSettings = FolderSettingsDescriptions.convert(validatedSettings)

        // Merge folder settings if necessary
        val writeSettings: MutableMap<String, String>
        if (overwrite) {
            writeSettings = FolderSettingsDescriptions.getFolderSettings(prefs.storage, uuid, folder.name)
            writeSettings.putAll(stringSettings)
        } else {
            writeSettings = stringSettings
        }

        // Write folder settings
        val prefix = uuid + "." + folder.name + "."
        for ((key1, value) in writeSettings) {
            val key = prefix + key1
            putString(editor, key, value)
        }
    }

    @Throws(InvalidSettingValueException::class)
    private fun importIdentities(
        editor: StorageEditor,
        contentVersion: Int,
        uuid: String,
        account: ImportedAccount?,
        overwrite: Boolean,
        existingAccount: Account?,
        prefs: Preferences,
    ) {
        val accountKeyPrefix = "$uuid."

        // Gather information about existing identities for this account (if any)
        var nextIdentityIndex = 0
        val existingIdentities: List<Identity>
        if (overwrite && existingAccount != null) {
            existingIdentities = existingAccount.identities
            nextIdentityIndex = existingIdentities.size
        } else {
            existingIdentities = ArrayList()
        }

        // Write identities
        for (identity in account!!.identities!!) {
            var writeIdentityIndex = nextIdentityIndex
            var mergeSettings = false
            if (overwrite && existingIdentities.size > 0) {
                val identityIndex = findIdentity(identity, existingIdentities)
                if (identityIndex != -1) {
                    writeIdentityIndex = identityIndex
                    mergeSettings = true
                }
            }
            if (!mergeSettings) {
                nextIdentityIndex++
            }

            val identitySuffix = ".$writeIdentityIndex"

            // Write name used in identity
            val identityName = if ((identity.name == null)) "" else identity.name
            putString(
                editor,
                accountKeyPrefix + AccountPreferenceSerializer.IDENTITY_NAME_KEY + identitySuffix,
                identityName,
            )

            // Validate email address
            if (!IdentitySettingsDescriptions.isEmailAddressValid(identity.email)) {
                throw InvalidSettingValueException("Invalid email address: " + identity.email)
            }

            // Write email address
            putString(
                editor,
                accountKeyPrefix + AccountPreferenceSerializer.IDENTITY_EMAIL_KEY + identitySuffix,
                identity.email,
            )

            // Write identity description
            if (identity.description != null) {
                putString(
                    editor,
                    accountKeyPrefix + AccountPreferenceSerializer.IDENTITY_DESCRIPTION_KEY + identitySuffix,
                    identity.description,
                )
            }

            if (identity.settings != null) {
                // Validate identity settings
                val validatedSettings = IdentitySettingsDescriptions.validate(
                    contentVersion,
                    identity.settings.settings,
                    !mergeSettings,
                )

                // Upgrade identity settings to current content version
                if (contentVersion != Settings.VERSION) {
                    IdentitySettingsDescriptions.upgrade(contentVersion, validatedSettings)
                }

                // Convert identity settings to the representation used in preference storage
                val stringSettings = IdentitySettingsDescriptions.convert(validatedSettings)

                // Merge identity settings if necessary
                var writeSettings: MutableMap<String, String?>
                if (mergeSettings) {
                    writeSettings = HashMap(
                        IdentitySettingsDescriptions.getIdentitySettings(
                            prefs.storage,
                            uuid,
                            writeIdentityIndex,
                        ),
                    )
                    writeSettings.putAll(stringSettings)
                } else {
                    writeSettings = stringSettings
                }

                // Write identity settings
                for ((key1, value) in writeSettings) {
                    val key = accountKeyPrefix + key1 + identitySuffix
                    putString(editor, key, value)
                }
            }
        }
    }

    private fun isAccountNameUsed(name: String?, accounts: List<Account>): Boolean {
        for (account in accounts) {
            if (account == null) {
                continue
            }

            if (account.displayName == name) {
                return true
            }
        }
        return false
    }

    private fun findIdentity(identity: ImportedIdentity, identities: List<Identity>): Int {
        for (i in identities.indices) {
            val existingIdentity = identities[i]
            if (existingIdentity.name == identity.name && existingIdentity.email == identity.email) {
                return i
            }
        }
        return -1
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

    private fun getAccountDisplayName(account: ImportedAccount): String? {
        var name = account.name
        if (TextUtils.isEmpty(name) && account.identities != null && account.identities.size > 0) {
            name = account.identities[0].email
        }

        return name
    }

    private fun createServerSettings(importedServer: ImportedServer?): ServerSettings {
        val type = toServerSettingsType(importedServer!!.type!!)
        val port = convertPort(importedServer.port)
        val connectionSecurity = convertConnectionSecurity(importedServer.connectionSecurity)
        val password = if (importedServer.authenticationType == AuthType.XOAUTH2) "" else importedServer.password
        val extra = if (importedServer.extras != null) {
            Collections.unmodifiableMap(importedServer.extras.settings)
        } else {
            emptyMap()
        }

        return ServerSettings(
            type,
            importedServer.host,
            port,
            connectionSecurity,
            importedServer.authenticationType!!,
            importedServer.username!!,
            password,
            importedServer.clientCertificateAlias,
            extra,
        )
    }

    private fun convertPort(port: String?): Int {
        return port?.toIntOrNull() ?: -1
    }

    private fun convertConnectionSecurity(connectionSecurity: String?): ConnectionSecurity {
        try {
            // TODO: Add proper settings validation and upgrade capability for server settings. Once that exists, move
            //  this code into a SettingsUpgrader.
            if ("SSL_TLS_OPTIONAL" == connectionSecurity) {
                return ConnectionSecurity.SSL_TLS_REQUIRED
            } else if ("STARTTLS_OPTIONAL" == connectionSecurity) {
                return ConnectionSecurity.STARTTLS_REQUIRED
            }
            return ConnectionSecurity.valueOf(connectionSecurity!!)
        } catch (e: Exception) {
            return ConnectionSecurity.SSL_TLS_REQUIRED
        }
    }
}
