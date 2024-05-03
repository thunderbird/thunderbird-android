package com.fsck.k9.preferences

import com.fsck.k9.mail.AuthType
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber

/**
 * Parser for K-9 Mail's settings file format.
 */
internal class SettingsFileParser {
    @Throws(SettingsImportExportException::class)
    fun parseSettings(inputStream: InputStream): Imported {
        try {
            return XmlSettingsParser(inputStream).parse()
        } catch (e: XmlPullParserException) {
            throw SettingsImportExportException("Error parsing settings XML", e)
        } catch (e: SettingsParserException) {
            throw SettingsImportExportException("Error parsing settings XML", e)
        }
    }
}

@Suppress("TooManyFunctions")
private class XmlSettingsParser(
    private val inputStream: InputStream,
) {
    private val pullParser: XmlPullParser = XmlPullParserFactory.newInstance().newPullParser().apply {
        setInput(InputStreamReader(inputStream))
    }

    fun parse(): Imported {
        var imported: Imported? = null
        do {
            val eventType = pullParser.next()

            if (eventType == XmlPullParser.START_TAG) {
                when (pullParser.name) {
                    SettingsExporter.ROOT_ELEMENT -> {
                        imported = readRoot()
                    }
                    else -> {
                        skipElement()
                    }
                }
            }
        } while (eventType != XmlPullParser.END_DOCUMENT)

        if (imported == null) {
            parserError("Missing '${SettingsExporter.ROOT_ELEMENT}' element")
        }

        return imported
    }

    private fun readRoot(): Imported {
        var generalSettings: ImportedSettings? = null
        var accounts: Map<String, ImportedAccount>? = null

        val fileFormatVersion = readFileFormatVersion()
        if (fileFormatVersion != SettingsExporter.FILE_FORMAT_VERSION) {
            parserError("Unsupported file format version: $fileFormatVersion")
        }

        val contentVersion = readContentVersion()

        readElement { eventType ->
            if (eventType == XmlPullParser.START_TAG) {
                when (pullParser.name) {
                    SettingsExporter.GLOBAL_ELEMENT -> {
                        if (generalSettings == null) {
                            generalSettings = readGlobalSettings()
                        } else {
                            Timber.w("More than one '${SettingsExporter.GLOBAL_ELEMENT}' element!")
                            skipElement()
                        }
                    }
                    SettingsExporter.ACCOUNTS_ELEMENT -> {
                        if (accounts == null) {
                            accounts = readAccounts()
                        } else {
                            Timber.w("More than one '${SettingsExporter.ACCOUNTS_ELEMENT}' element!")
                            skipElement()
                        }
                    }
                    else -> {
                        skipElement()
                    }
                }
            }
        }

        return Imported(contentVersion, generalSettings, accounts)
    }

    private fun readFileFormatVersion(): Int {
        val versionString = readAttribute(SettingsExporter.FILE_FORMAT_ATTRIBUTE)
        return versionString.toIntOrNull()
            ?: parserError("Invalid file format version: $versionString")
    }

    private fun readContentVersion(): Int {
        val versionString = readAttribute(SettingsExporter.VERSION_ATTRIBUTE)
        return versionString.toIntOrNull()?.takeIf { it > 0 }
            ?: parserError("Invalid content version: $versionString")
    }

    private fun readGlobalSettings(): ImportedSettings? {
        return readSettingsContainer()
    }

    private fun readSettingsContainer(): ImportedSettings? {
        val settings = mutableMapOf<String, String>()

        readElement { eventType ->
            if (eventType == XmlPullParser.START_TAG) {
                when (pullParser.name) {
                    SettingsExporter.VALUE_ELEMENT -> {
                        val key = readAttribute(SettingsExporter.KEY_ATTRIBUTE)
                        val value = readText()

                        if (settings.containsKey(key)) {
                            Timber.w("Already read key \"%s\". Ignoring value \"%s\"", key, value)
                        } else {
                            settings[key] = value
                        }
                    }
                    else -> {
                        skipElement()
                    }
                }
            }
        }

        return if (settings.isNotEmpty()) {
            ImportedSettings(settings)
        } else {
            null
        }
    }

    private fun readAccounts(): Map<String, ImportedAccount>? {
        val accounts = mutableMapOf<String, ImportedAccount>()

        readElement { eventType ->
            if (eventType == XmlPullParser.START_TAG) {
                when (pullParser.name) {
                    SettingsExporter.ACCOUNT_ELEMENT -> {
                        val account = readAccount()

                        if (account == null) {
                            // Do nothing - readAccount() already logged a message
                        } else if (!accounts.containsKey(account.uuid)) {
                            accounts[account.uuid] = account
                        } else {
                            Timber.w("Duplicate account entries with UUID %s. Ignoring!", account.uuid)
                        }
                    }
                    else -> {
                        skipElement()
                    }
                }
            }
        }

        return accounts.takeIf { it.isNotEmpty() }
    }

    private fun readAccount(): ImportedAccount? {
        val uuid = readUuid()
        if (uuid == null) {
            skipElement()
            return null
        }

        var name: String? = null
        var incoming: ImportedServer? = null
        var outgoing: ImportedServer? = null
        var settings: ImportedSettings? = null
        var identities: List<ImportedIdentity>? = null
        var folders: List<ImportedFolder>? = null

        readElement { eventType ->
            if (eventType == XmlPullParser.START_TAG) {
                when (pullParser.name) {
                    SettingsExporter.NAME_ELEMENT -> {
                        name = readText()
                    }
                    SettingsExporter.INCOMING_SERVER_ELEMENT -> {
                        incoming = readServerSettings()
                    }
                    SettingsExporter.OUTGOING_SERVER_ELEMENT -> {
                        outgoing = readServerSettings()
                    }
                    SettingsExporter.SETTINGS_ELEMENT -> {
                        settings = readSettingsContainer()
                    }
                    SettingsExporter.IDENTITIES_ELEMENT -> {
                        identities = readIdentities()
                    }
                    SettingsExporter.FOLDERS_ELEMENT -> {
                        folders = readFolders()
                    }
                    else -> {
                        skipElement()
                    }
                }
            }
        }

        // If we couldn't find an account name use the UUID
        if (name == null) {
            name = uuid
        }

        return ImportedAccount(uuid, name, incoming, outgoing, settings, identities, folders)
    }

    private fun readUuid(): String? {
        val uuid = pullParser.getAttributeValue(null, SettingsExporter.UUID_ATTRIBUTE)

        try {
            UUID.fromString(uuid)
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Invalid account UUID: %s", uuid)
            return null
        }

        return uuid
    }

    private fun readServerSettings(): ImportedServer {
        var host: String? = null
        var port: String? = null
        var connectionSecurity: String? = null
        var authenticationType: AuthType? = null
        var username: String? = null
        var password: String? = null
        var clientCertificateAlias: String? = null
        var extras: ImportedSettings? = null

        val type = pullParser.getAttributeValue(null, SettingsExporter.TYPE_ATTRIBUTE)

        readElement { eventType ->
            if (eventType == XmlPullParser.START_TAG) {
                when (pullParser.name) {
                    SettingsExporter.HOST_ELEMENT -> {
                        host = readText()
                    }
                    SettingsExporter.PORT_ELEMENT -> {
                        port = readText()
                    }
                    SettingsExporter.CONNECTION_SECURITY_ELEMENT -> {
                        connectionSecurity = readText()
                    }
                    SettingsExporter.AUTHENTICATION_TYPE_ELEMENT -> {
                        val text = readText()
                        authenticationType = AuthType.valueOf(text)
                    }
                    SettingsExporter.USERNAME_ELEMENT -> {
                        username = readText()
                    }
                    SettingsExporter.CLIENT_CERTIFICATE_ALIAS_ELEMENT -> {
                        clientCertificateAlias = readText()
                    }
                    SettingsExporter.PASSWORD_ELEMENT -> {
                        password = readText()
                    }
                    SettingsExporter.EXTRA_ELEMENT -> {
                        extras = readSettingsContainer()
                    }
                    else -> {
                        skipElement()
                    }
                }
            }
        }

        return ImportedServer(
            type,
            host,
            port,
            connectionSecurity,
            authenticationType,
            username,
            password,
            clientCertificateAlias,
            extras,
        )
    }

    private fun readIdentities(): List<ImportedIdentity> {
        val identities = mutableListOf<ImportedIdentity>()

        readElement { eventType ->
            if (eventType == XmlPullParser.START_TAG) {
                when (pullParser.name) {
                    SettingsExporter.IDENTITY_ELEMENT -> {
                        val identity = readIdentity()
                        identities.add(identity)
                    }
                    else -> {
                        skipElement()
                    }
                }
            }
        }

        return identities
    }

    private fun readIdentity(): ImportedIdentity {
        var name: String? = null
        var email: String? = null
        var description: String? = null
        var settings: ImportedSettings? = null

        readElement { eventType ->
            if (eventType == XmlPullParser.START_TAG) {
                when (pullParser.name) {
                    SettingsExporter.NAME_ELEMENT -> {
                        name = readText()
                    }
                    SettingsExporter.EMAIL_ELEMENT -> {
                        email = readText()
                    }
                    SettingsExporter.DESCRIPTION_ELEMENT -> {
                        description = readText()
                    }
                    SettingsExporter.SETTINGS_ELEMENT -> {
                        settings = readSettingsContainer()
                    }
                    else -> {
                        skipElement()
                    }
                }
            }
        }

        return ImportedIdentity(name, email, description, settings)
    }

    private fun readFolders(): List<ImportedFolder> {
        val folders = mutableListOf<ImportedFolder>()

        readElement { eventType ->
            if (eventType == XmlPullParser.START_TAG) {
                when (pullParser.name) {
                    SettingsExporter.FOLDER_ELEMENT -> {
                        val folder = readFolder()
                        if (folder != null) {
                            folders.add(folder)
                        }
                    }
                    else -> {
                        skipElement()
                    }
                }
            }
        }

        return folders
    }

    private fun readFolder(): ImportedFolder? {
        val name = pullParser.getAttributeValue(null, SettingsExporter.NAME_ATTRIBUTE) ?: return null
        val settings = readSettingsContainer()

        return ImportedFolder(name, settings)
    }

    private fun readText(): String {
        return if (pullParser.next() == XmlPullParser.TEXT) {
            pullParser.text
        } else {
            ""
        }
    }

    private fun readAttribute(name: String): String {
        return pullParser.getAttributeValue(null, name) ?: parserError("Missing '$name' attribute")
    }

    private fun readElement(block: (Int) -> Unit) {
        require(pullParser.eventType == XmlPullParser.START_TAG)

        val tagName = pullParser.name
        val depth = pullParser.depth
        while (true) {
            when (val eventType = pullParser.next()) {
                XmlPullParser.END_DOCUMENT -> {
                    parserError("End of document reached while reading element '$tagName'")
                }
                XmlPullParser.END_TAG -> {
                    if (pullParser.name == tagName && pullParser.depth == depth) return
                }
                else -> {
                    block(eventType)
                }
            }
        }
    }

    private fun skipElement() {
        Timber.d("Skipping element '%s'", pullParser.name)
        readElement { /* Do nothing */ }
    }

    private fun parserError(message: String): Nothing {
        throw SettingsParserException(message)
    }
}
