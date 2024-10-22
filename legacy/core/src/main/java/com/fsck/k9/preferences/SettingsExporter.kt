package com.fsck.k9.preferences

import android.content.ContentResolver
import android.net.Uri
import android.util.Xml
import app.k9mail.core.common.provider.BrandNameProvider
import app.k9mail.legacy.account.Account
import app.k9mail.legacy.mailstore.FolderRepository
import com.fsck.k9.AccountPreferenceSerializer.Companion.ACCOUNT_DESCRIPTION_KEY
import com.fsck.k9.AccountPreferenceSerializer.Companion.IDENTITY_DESCRIPTION_KEY
import com.fsck.k9.AccountPreferenceSerializer.Companion.IDENTITY_EMAIL_KEY
import com.fsck.k9.AccountPreferenceSerializer.Companion.IDENTITY_NAME_KEY
import com.fsck.k9.Preferences
import com.fsck.k9.notification.NotificationSettingsUpdater
import com.fsck.k9.preferences.ServerTypeConverter.fromServerSettingsType
import com.fsck.k9.preferences.Settings.InvalidSettingValueException
import com.fsck.k9.preferences.Settings.SettingsDescription
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import org.xmlpull.v1.XmlSerializer
import timber.log.Timber

class SettingsExporter(
    private val contentResolver: ContentResolver,
    private val preferences: Preferences,
    private val folderSettingsProvider: FolderSettingsProvider,
    private val folderRepository: FolderRepository,
    private val notificationSettingsUpdater: NotificationSettingsUpdater,
    private val brandNameProvider: BrandNameProvider,
) {
    @Throws(SettingsImportExportException::class)
    fun exportToUri(includeGlobals: Boolean, accountUuids: Set<String>, uri: Uri) {
        try {
            contentResolver.openOutputStream(uri, "wt")!!.use { outputStream ->
                exportPreferences(outputStream, includeGlobals, accountUuids, includePasswords = false)
            }
        } catch (e: Exception) {
            throw SettingsImportExportException(e)
        }
    }

    @Throws(SettingsImportExportException::class)
    fun exportPreferences(
        outputStream: OutputStream,
        includeGlobals: Boolean,
        accountUuids: Set<String>,
        includePasswords: Boolean,
    ) {
        updateNotificationSettings(accountUuids)

        try {
            val serializer = Xml.newSerializer()
            serializer.setOutput(outputStream, "UTF-8")

            serializer.startDocument(null, true)

            // Output with indentation
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag(null, ROOT_ELEMENT)
            serializer.attribute(null, VERSION_ATTRIBUTE, Settings.VERSION.toString())
            serializer.attribute(null, FILE_FORMAT_ATTRIBUTE, FILE_FORMAT_VERSION.toString())

            Timber.i("Exporting preferences")

            val storage = preferences.storage

            val prefs: Map<String, Any> = storage.all.toSortedMap()
            if (includeGlobals) {
                serializer.startTag(null, GLOBAL_ELEMENT)
                writeSettings(serializer, prefs)
                serializer.endTag(null, GLOBAL_ELEMENT)
            }

            serializer.startTag(null, ACCOUNTS_ELEMENT)
            for (accountUuid in accountUuids) {
                preferences.getAccount(accountUuid)?.let { account ->
                    writeAccount(serializer, account, prefs, includePasswords)
                }
            }
            serializer.endTag(null, ACCOUNTS_ELEMENT)

            serializer.endTag(null, ROOT_ELEMENT)
            serializer.endDocument()
            serializer.flush()
        } catch (e: Exception) {
            throw SettingsImportExportException(e.localizedMessage, e)
        }
    }

    private fun updateNotificationSettings(accountUuids: Set<String>) {
        try {
            notificationSettingsUpdater.updateNotificationSettings(accountUuids)
        } catch (e: Exception) {
            // An error here could mean we export notification settings that don't reflect the current configuration
            // of the notification channels. But we prefer stale data over failing the export.
            Timber.w(e, "Error while updating accounts with notification configuration from system")
        }
    }

    private fun writeSettings(serializer: XmlSerializer, prefs: Map<String, Any>) {
        for ((key, versions) in GeneralSettingsDescriptions.SETTINGS) {
            val valueString = prefs[key] as String?
            val highestVersion = versions.lastKey()
            val setting = versions[highestVersion] ?: continue // Setting was removed

            if (valueString != null) {
                try {
                    writeKeyAndPrettyValueFromSetting(serializer, key, setting, valueString)
                } catch (e: InvalidSettingValueException) {
                    Timber.w(
                        "Global setting \"%s\" has invalid value \"%s\" in preference storage. This shouldn't happen!",
                        key,
                        valueString,
                    )
                }
            } else {
                Timber.d("Couldn't find key \"%s\" in preference storage. Using default value.", key)
                writeKeyAndDefaultValueFromSetting(serializer, key, setting)
            }
        }
    }

    private fun writeAccount(
        serializer: XmlSerializer,
        account: Account,
        prefs: Map<String, Any>,
        includePasswords: Boolean,
    ) {
        val identities = mutableSetOf<Int>()
        val accountUuid = account.uuid

        serializer.startTag(null, ACCOUNT_ELEMENT)
        serializer.attribute(null, UUID_ATTRIBUTE, accountUuid)

        val name = prefs["$accountUuid.$ACCOUNT_DESCRIPTION_KEY"] as String?
        if (name != null) {
            serializer.startTag(null, NAME_ELEMENT)
            serializer.text(name)
            serializer.endTag(null, NAME_ELEMENT)
        }

        // Write incoming server settings
        val incoming = account.incomingServerSettings
        serializer.startTag(null, INCOMING_SERVER_ELEMENT)
        serializer.attribute(null, TYPE_ATTRIBUTE, fromServerSettingsType(incoming.type))

        writeElement(serializer, HOST_ELEMENT, incoming.host)
        if (incoming.port != -1) {
            writeElement(serializer, PORT_ELEMENT, incoming.port.toString())
        }
        writeElement(serializer, CONNECTION_SECURITY_ELEMENT, incoming.connectionSecurity.name)
        writeElement(serializer, AUTHENTICATION_TYPE_ELEMENT, incoming.authenticationType.name)
        writeElement(serializer, USERNAME_ELEMENT, incoming.username)
        writeElement(serializer, CLIENT_CERTIFICATE_ALIAS_ELEMENT, incoming.clientCertificateAlias)

        if (includePasswords && !incoming.password.isNullOrEmpty()) {
            writeElement(serializer, PASSWORD_ELEMENT, incoming.password)
        }

        var extras = incoming.extra
        if (!extras.isNullOrEmpty()) {
            serializer.startTag(null, EXTRA_ELEMENT)
            for ((key, value) in extras) {
                writeKeyAndPrettyValueFromSetting(serializer, key, value)
            }
            serializer.endTag(null, EXTRA_ELEMENT)
        }
        serializer.endTag(null, INCOMING_SERVER_ELEMENT)

        // Write outgoing server settings
        val outgoing = account.outgoingServerSettings
        serializer.startTag(null, OUTGOING_SERVER_ELEMENT)
        serializer.attribute(null, TYPE_ATTRIBUTE, fromServerSettingsType(outgoing.type))

        writeElement(serializer, HOST_ELEMENT, outgoing.host)
        if (outgoing.port != -1) {
            writeElement(serializer, PORT_ELEMENT, outgoing.port.toString())
        }
        writeElement(serializer, CONNECTION_SECURITY_ELEMENT, outgoing.connectionSecurity.name)
        writeElement(serializer, AUTHENTICATION_TYPE_ELEMENT, outgoing.authenticationType.name)
        writeElement(serializer, USERNAME_ELEMENT, outgoing.username)
        writeElement(serializer, CLIENT_CERTIFICATE_ALIAS_ELEMENT, outgoing.clientCertificateAlias)

        if (includePasswords && !outgoing.password.isNullOrEmpty()) {
            writeElement(serializer, PASSWORD_ELEMENT, outgoing.password)
        }

        extras = outgoing.extra
        if (!extras.isNullOrEmpty()) {
            serializer.startTag(null, EXTRA_ELEMENT)
            for ((key, value) in extras) {
                writeKeyAndPrettyValueFromSetting(serializer, key, value)
            }
            serializer.endTag(null, EXTRA_ELEMENT)
        }
        serializer.endTag(null, OUTGOING_SERVER_ELEMENT)

        // Write account settings
        serializer.startTag(null, SETTINGS_ELEMENT)
        for ((key, value) in prefs) {
            val valueString = value.toString()
            val comps = key.split(".", limit = 2)

            if (comps.size < 2) {
                // Skip global settings
                continue
            }

            val keyUuid = comps[0]
            val keyPart = comps[1]

            if (keyUuid != accountUuid) {
                // Setting doesn't belong to the account we're currently writing.
                continue
            }

            val indexOfLastDot = keyPart.lastIndexOf(".")
            val hasThirdPart = indexOfLastDot != -1 && indexOfLastDot < keyPart.length - 1
            if (hasThirdPart) {
                val secondPart = keyPart.substring(0, indexOfLastDot)
                val thirdPart = keyPart.substring(indexOfLastDot + 1)
                if (secondPart == IDENTITY_EMAIL_KEY) {
                    // This is an identity key. Save identity index for later...
                    thirdPart.toIntOrNull()?.let {
                        identities.add(it)
                    }
                    // ... but don't write it now.
                    continue
                }

                if (FolderSettingsDescriptions.SETTINGS.containsKey(thirdPart)) {
                    // This is a folder key. Ignore it.
                    continue
                }
            }

            if (keyPart !in FOLDER_NAME_KEYS) {
                writeAccountSettingIfValid(serializer, keyPart, valueString, account)
            }
        }

        writeFolderNameSettings(account, folderRepository, serializer)

        serializer.endTag(null, SETTINGS_ELEMENT)

        if (identities.isNotEmpty()) {
            serializer.startTag(null, IDENTITIES_ELEMENT)

            // Sort identity indices (that's why we store them as Integers)
            val sortedIdentities = identities.sorted()
            for (identityIndex in sortedIdentities) {
                writeIdentity(serializer, accountUuid, identityIndex.toString(), prefs)
            }
            serializer.endTag(null, IDENTITIES_ELEMENT)
        }

        val folders = folderSettingsProvider.getFolderSettings(account)
        if (folders.isNotEmpty()) {
            serializer.startTag(null, FOLDERS_ELEMENT)
            for (folder in folders) {
                writeFolder(serializer, folder)
            }
            serializer.endTag(null, FOLDERS_ELEMENT)
        }

        serializer.endTag(null, ACCOUNT_ELEMENT)
    }

    private fun writeAccountSettingIfValid(
        serializer: XmlSerializer,
        keyPart: String,
        valueString: String,
        account: Account,
    ) {
        val versionedSetting = AccountSettingsDescriptions.SETTINGS[keyPart]
        if (versionedSetting != null) {
            val highestVersion = versionedSetting.lastKey()

            val setting = versionedSetting[highestVersion]
            if (setting != null) {
                // Only export account settings that can be found in AccountSettings.SETTINGS
                try {
                    writeKeyAndPrettyValueFromSetting(serializer, keyPart, setting, valueString)
                } catch (e: InvalidSettingValueException) {
                    Timber.w(
                        "Account setting \"%s\" (%s) has invalid value \"%s\" in preference storage. " +
                            "This shouldn't happen!",
                        keyPart,
                        account,
                        valueString,
                    )
                }
            }
        }
    }

    private fun writeFolderNameSettings(
        account: Account,
        folderRepository: FolderRepository,
        serializer: XmlSerializer,
    ) {
        fun writeFolderNameSetting(
            key: String,
            folderId: Long?,
            importedFolderServerId: String?,
            writeEmptyValue: Boolean = false,
        ) {
            val folderServerId = folderId?.let {
                folderRepository.getFolderServerId(account, folderId)
            } ?: importedFolderServerId

            if (folderServerId != null) {
                writeAccountSettingIfValid(serializer, key, folderServerId, account)
            } else if (writeEmptyValue) {
                writeAccountSettingIfValid(serializer, key, valueString = "", account)
            }
        }

        writeFolderNameSetting(
            "autoExpandFolderName",
            account.autoExpandFolderId,
            account.importedAutoExpandFolder,
            writeEmptyValue = true,
        )
        writeFolderNameSetting("archiveFolderName", account.archiveFolderId, account.importedArchiveFolder)
        writeFolderNameSetting("draftsFolderName", account.draftsFolderId, account.importedDraftsFolder)
        writeFolderNameSetting("sentFolderName", account.sentFolderId, account.importedSentFolder)
        writeFolderNameSetting("spamFolderName", account.spamFolderId, account.importedSpamFolder)
        writeFolderNameSetting("trashFolderName", account.trashFolderId, account.importedTrashFolder)
    }

    private fun writeIdentity(
        serializer: XmlSerializer,
        accountUuid: String,
        identity: String,
        prefs: Map<String, Any>,
    ) {
        serializer.startTag(null, IDENTITY_ELEMENT)

        val prefix = "$accountUuid."
        val suffix = ".$identity"

        // Write name belonging to the identity
        val name = prefs[prefix + IDENTITY_NAME_KEY + suffix] as String?
        if (name != null) {
            serializer.startTag(null, NAME_ELEMENT)
            serializer.text(name)
            serializer.endTag(null, NAME_ELEMENT)
        }

        // Write email address belonging to the identity
        val email = prefs[prefix + IDENTITY_EMAIL_KEY + suffix] as String?
        serializer.startTag(null, EMAIL_ELEMENT)
        serializer.text(email)
        serializer.endTag(null, EMAIL_ELEMENT)

        // Write identity description
        val description = prefs[prefix + IDENTITY_DESCRIPTION_KEY + suffix] as String?
        if (description != null) {
            serializer.startTag(null, DESCRIPTION_ELEMENT)
            serializer.text(description)
            serializer.endTag(null, DESCRIPTION_ELEMENT)
        }

        // Write identity settings
        serializer.startTag(null, SETTINGS_ELEMENT)
        for ((key, value) in prefs) {
            val valueString = value.toString()

            val comps = key.split(".")
            if (comps.size < 3) {
                // Skip non-identity config entries
                continue
            }

            val keyUuid = comps[0]
            val identityKey = comps[1]
            val identityIndex = comps[2]
            if (keyUuid != accountUuid || identityIndex != identity) {
                // Skip entries that belong to another identity
                continue
            }

            val versionedSetting = IdentitySettingsDescriptions.SETTINGS[identityKey]
            if (versionedSetting != null) {
                val highestVersion = versionedSetting.lastKey()
                val setting = versionedSetting[highestVersion]
                if (setting != null) {
                    // Only write settings that have an entry in IdentitySettings.SETTINGS
                    try {
                        writeKeyAndPrettyValueFromSetting(serializer, identityKey, setting, valueString)
                    } catch (e: InvalidSettingValueException) {
                        Timber.w(
                            "Identity setting \"%s\" has invalid value \"%s\" in preference storage. " +
                                "This shouldn't happen!",
                            identityKey,
                            valueString,
                        )
                    }
                }
            }
        }
        serializer.endTag(null, SETTINGS_ELEMENT)

        serializer.endTag(null, IDENTITY_ELEMENT)
    }

    private fun writeFolder(serializer: XmlSerializer, folder: FolderSettings) {
        serializer.startTag(null, FOLDER_ELEMENT)
        serializer.attribute(null, NAME_ATTRIBUTE, folder.serverId)

        // Write folder settings
        writeFolderSetting(serializer, "integrate", folder.isIntegrate.toString())
        writeFolderSetting(serializer, "inTopGroup", folder.isInTopGroup.toString())
        writeFolderSetting(serializer, "syncEnabled", folder.isSyncEnabled.toString())
        writeFolderSetting(serializer, "visible", folder.isVisible.toString())
        writeFolderSetting(serializer, "notificationsEnabled", folder.isNotificationsEnabled.toString())
        writeFolderSetting(serializer, "pushEnabled", folder.isPushEnabled.toString())

        serializer.endTag(null, FOLDER_ELEMENT)
    }

    private fun writeFolderSetting(serializer: XmlSerializer, key: String, value: String) {
        val versionedSetting = FolderSettingsDescriptions.SETTINGS[key]
        if (versionedSetting != null) {
            val highestVersion = versionedSetting.lastKey()
            val setting = versionedSetting[highestVersion]
            if (setting != null) {
                // Only write settings that have an entry in FolderSettings.SETTINGS
                try {
                    writeKeyAndPrettyValueFromSetting(serializer, key, setting, value)
                } catch (e: InvalidSettingValueException) {
                    Timber.w(
                        "Folder setting \"%s\" has invalid value \"%s\" in preference storage. This shouldn't happen!",
                        key,
                        value,
                    )
                }
            }
        }
    }

    private fun writeElement(serializer: XmlSerializer, elementName: String, value: String?) {
        if (value != null) {
            serializer.startTag(null, elementName)
            serializer.text(value)
            serializer.endTag(null, elementName)
        }
    }

    private fun <T> writeKeyAndPrettyValueFromSetting(
        serializer: XmlSerializer,
        key: String,
        setting: SettingsDescription<T>,
        valueString: String,
    ) {
        val value = setting.fromString(valueString)
        val outputValue = setting.toPrettyString(value)
        writeKeyAndPrettyValueFromSetting(serializer, key, outputValue)
    }

    private fun <T> writeKeyAndDefaultValueFromSetting(
        serializer: XmlSerializer,
        key: String,
        setting: SettingsDescription<T>,
    ) {
        val value = setting.getDefaultValue()
        val outputValue = setting.toPrettyString(value)
        writeKeyAndPrettyValueFromSetting(serializer, key, outputValue)
    }

    private fun writeKeyAndPrettyValueFromSetting(serializer: XmlSerializer, key: String, literalValue: String?) {
        serializer.startTag(null, VALUE_ELEMENT)
        serializer.attribute(null, KEY_ATTRIBUTE, key)
        if (literalValue != null) {
            serializer.text(literalValue)
        }
        serializer.endTag(null, VALUE_ELEMENT)
    }

    fun generateDatedExportFileName(): String {
        val now = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return String.format("%s_%s.%s", getExportFileName(), dateFormat.format(now.time), EXPORT_FILENAME_SUFFIX)
    }

    private fun getExportFileName() : String {
        return "${brandNameProvider.brandName}_settings_export".lowercase()
    }

    companion object {
        private const val EXPORT_FILENAME_SUFFIX = "k9s"

        /**
         * File format version number.
         *
         * Increment this if you need to change the structure of the settings file. When you do this
         * remember that we also have to be able to handle old file formats. So have fun adding support
         * for that to [SettingsImporter] :)
         */
        const val FILE_FORMAT_VERSION = 1

        const val ROOT_ELEMENT = "k9settings"
        const val VERSION_ATTRIBUTE = "version"
        const val FILE_FORMAT_ATTRIBUTE = "format"
        const val GLOBAL_ELEMENT = "global"
        const val SETTINGS_ELEMENT = "settings"
        const val ACCOUNTS_ELEMENT = "accounts"
        const val ACCOUNT_ELEMENT = "account"
        const val UUID_ATTRIBUTE = "uuid"
        const val INCOMING_SERVER_ELEMENT = "incoming-server"
        const val OUTGOING_SERVER_ELEMENT = "outgoing-server"
        const val TYPE_ATTRIBUTE = "type"
        const val HOST_ELEMENT = "host"
        const val PORT_ELEMENT = "port"
        const val CONNECTION_SECURITY_ELEMENT = "connection-security"
        const val AUTHENTICATION_TYPE_ELEMENT = "authentication-type"
        const val USERNAME_ELEMENT = "username"
        const val CLIENT_CERTIFICATE_ALIAS_ELEMENT = "client-cert-alias"
        const val PASSWORD_ELEMENT = "password"
        const val EXTRA_ELEMENT = "extra"
        const val IDENTITIES_ELEMENT = "identities"
        const val IDENTITY_ELEMENT = "identity"
        const val FOLDERS_ELEMENT = "folders"
        const val FOLDER_ELEMENT = "folder"
        const val NAME_ATTRIBUTE = "name"
        const val VALUE_ELEMENT = "value"
        const val KEY_ATTRIBUTE = "key"
        const val NAME_ELEMENT = "name"
        const val EMAIL_ELEMENT = "email"
        const val DESCRIPTION_ELEMENT = "description"

        private val FOLDER_NAME_KEYS = setOf(
            "autoExpandFolderName",
            "archiveFolderName",
            "draftsFolderName",
            "sentFolderName",
            "spamFolderName",
            "trashFolderName",
        )
    }
}
