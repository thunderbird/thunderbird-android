package com.fsck.k9.preferences

import com.fsck.k9.mail.AuthType
import com.fsck.k9.preferences.SettingsFile.Account
import com.fsck.k9.preferences.SettingsFile.Contents
import com.fsck.k9.preferences.SettingsFile.Folder
import com.fsck.k9.preferences.SettingsFile.Identity
import com.fsck.k9.preferences.SettingsFile.Server
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
    fun parseSettings(inputStream: InputStream): Contents {
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

    fun parse(): Contents {
        var contents: Contents? = null
        do {
            val eventType = pullParser.next()

            if (eventType == XmlPullParser.START_TAG) {
                when (pullParser.name) {
                    SettingsExporter.ROOT_ELEMENT -> {
                        contents = readRoot()
                    }
                    else -> {
                        skipElement()
                    }
                }
            }
        } while (eventType != XmlPullParser.END_DOCUMENT)

        if (contents == null) {
            parserError("Missing '${SettingsExporter.ROOT_ELEMENT}' element")
        }

        return contents
    }

    private fun readRoot(): Contents {
        var generalSettings: SettingsMap? = null
        var accounts: Map<String, Account>? = null

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

        return Contents(contentVersion, generalSettings, accounts.orEmpty())
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

    private fun readGlobalSettings(): SettingsMap? {
        return readSettingsContainer()
    }

    private fun readSettingsContainer(): SettingsMap? {
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

        return settings.takeIf { it.isNotEmpty() }
    }

    private fun readAccounts(): Map<String, Account> {
        val accounts = mutableMapOf<String, Account>()

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

        return accounts
    }

    private fun readAccount(): Account? {
        val uuid = readUuid()
        if (uuid == null) {
            skipElement()
            return null
        }

        var name: String? = null
        var incoming: Server? = null
        var outgoing: Server? = null
        var settings: SettingsMap? = null
        var identities: List<Identity>? = null
        var folders: List<Folder>? = null

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

        return Account(uuid, name, incoming, outgoing, settings, identities, folders)
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

    private fun readServerSettings(): Server {
        var host: String? = null
        var port: String? = null
        var connectionSecurity: String? = null
        var authenticationType: AuthType? = null
        var username: String? = null
        var password: String? = null
        var clientCertificateAlias: String? = null
        var extras: SettingsMap? = null

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

        return Server(
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

    private fun readIdentities(): List<Identity> {
        val identities = mutableListOf<Identity>()

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

    private fun readIdentity(): Identity {
        var name: String? = null
        var email: String? = null
        var description: String? = null
        var settings: SettingsMap? = null

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

        return Identity(name, email, description, settings)
    }

    private fun readFolders(): List<Folder> {
        val folders = mutableListOf<Folder>()

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

    private fun readFolder(): Folder? {
        val name = pullParser.getAttributeValue(null, SettingsExporter.NAME_ATTRIBUTE) ?: return null
        val settings = readSettingsContainer()

        return Folder(name, settings)
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
