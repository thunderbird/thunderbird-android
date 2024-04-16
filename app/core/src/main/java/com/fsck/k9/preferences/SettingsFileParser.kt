package com.fsck.k9.preferences

import com.fsck.k9.mail.AuthType
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber

internal class SettingsFileParser {
    @Throws(SettingsImportExportException::class)
    fun parseSettings(
        inputStream: InputStream?,
        globalSettings: Boolean,
        accountUuids: List<String>?,
        overview: Boolean,
    ): Imported {
        require(!(!overview && accountUuids == null)) { "Argument 'accountUuids' must not be null." }

        try {
            val factory = XmlPullParserFactory.newInstance()
            val xpp = factory.newPullParser()

            val reader = InputStreamReader(inputStream)
            xpp.setInput(reader)

            var imported: Imported? = null
            var eventType = xpp.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (SettingsExporter.ROOT_ELEMENT == xpp.name) {
                        imported = parseRoot(xpp, globalSettings, accountUuids, overview)
                    } else {
                        Timber.w("Unexpected start tag: %s", xpp.name)
                    }
                }
                eventType = xpp.next()
            }

            if (imported == null || (overview && imported.globalSettings == null && imported.accounts == null)) {
                throw SettingsImportExportException("Invalid import data")
            }

            return imported
        } catch (e: Exception) {
            throw SettingsImportExportException(e)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class, SettingsImportExportException::class)
    private fun parseRoot(
        xpp: XmlPullParser,
        globalSettings: Boolean,
        accountUuids: List<String>?,
        overview: Boolean,
    ): Imported {
        val result = Imported()

        val fileFormatVersionString = xpp.getAttributeValue(null, SettingsExporter.FILE_FORMAT_ATTRIBUTE)
        validateFileFormatVersion(fileFormatVersionString)

        val contentVersionString = xpp.getAttributeValue(null, SettingsExporter.VERSION_ATTRIBUTE)
        result.contentVersion = validateContentVersion(contentVersionString)

        var eventType = xpp.next()
        while (!(eventType == XmlPullParser.END_TAG && SettingsExporter.ROOT_ELEMENT == xpp.name)) {
            if (eventType == XmlPullParser.START_TAG) {
                val element = xpp.name
                if (SettingsExporter.GLOBAL_ELEMENT == element) {
                    if (overview || globalSettings) {
                        if (result.globalSettings == null) {
                            if (overview) {
                                result.globalSettings = ImportedSettings()
                                skipToEndTag(xpp, SettingsExporter.GLOBAL_ELEMENT)
                            } else {
                                result.globalSettings = parseSettings(xpp, SettingsExporter.GLOBAL_ELEMENT)
                            }
                        } else {
                            skipToEndTag(xpp, SettingsExporter.GLOBAL_ELEMENT)
                            Timber.w("More than one global settings element. Only using the first one!")
                        }
                    } else {
                        skipToEndTag(xpp, SettingsExporter.GLOBAL_ELEMENT)
                        Timber.i("Skipping global settings")
                    }
                } else if (SettingsExporter.ACCOUNTS_ELEMENT == element) {
                    if (result.accounts == null) {
                        result.accounts = parseAccounts(xpp, accountUuids, overview)
                    } else {
                        Timber.w("More than one accounts element. Only using the first one!")
                    }
                } else {
                    Timber.w("Unexpected start tag: %s", xpp.name)
                }
            }
            eventType = xpp.next()
        }

        return result
    }

    @Throws(SettingsImportExportException::class)
    private fun validateFileFormatVersion(versionString: String?): Int {
        if (versionString == null) {
            throw SettingsImportExportException("Missing file format version")
        }

        val version: Int
        try {
            version = versionString.toInt()
        } catch (e: NumberFormatException) {
            throw SettingsImportExportException("Invalid file format version: $versionString")
        }

        if (version != SettingsExporter.FILE_FORMAT_VERSION) {
            throw SettingsImportExportException("Unsupported file format version: $versionString")
        }

        return version
    }

    @Throws(SettingsImportExportException::class)
    private fun validateContentVersion(versionString: String?): Int {
        if (versionString == null) {
            throw SettingsImportExportException("Missing content version")
        }

        val version: Int
        try {
            version = versionString.toInt()
        } catch (e: NumberFormatException) {
            throw SettingsImportExportException("Invalid content version: $versionString")
        }

        if (version < 1) {
            throw SettingsImportExportException("Unsupported content version: $versionString")
        }

        return version
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseSettings(xpp: XmlPullParser, endTag: String): ImportedSettings? {
        var result: ImportedSettings? = null

        var eventType = xpp.next()
        while (!(eventType == XmlPullParser.END_TAG && endTag == xpp.name)) {
            if (eventType == XmlPullParser.START_TAG) {
                val element = xpp.name
                if (SettingsExporter.VALUE_ELEMENT == element) {
                    val key = xpp.getAttributeValue(null, SettingsExporter.KEY_ATTRIBUTE)
                    val value = getText(xpp)

                    if (result == null) {
                        result = ImportedSettings()
                    }

                    if (result.settings.containsKey(key)) {
                        Timber.w("Already read key \"%s\". Ignoring value \"%s\"", key, value)
                    } else {
                        result.settings[key] = value
                    }
                } else {
                    Timber.w("Unexpected start tag: %s", xpp.name)
                }
            }
            eventType = xpp.next()
        }

        return result
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseAccounts(
        xpp: XmlPullParser,
        accountUuids: List<String>?,
        overview: Boolean,
    ): Map<String, ImportedAccount>? {
        var accounts: MutableMap<String, ImportedAccount>? = null

        var eventType = xpp.next()
        while (!(eventType == XmlPullParser.END_TAG && SettingsExporter.ACCOUNTS_ELEMENT == xpp.name)) {
            if (eventType == XmlPullParser.START_TAG) {
                val element = xpp.name
                if (SettingsExporter.ACCOUNT_ELEMENT == element) {
                    if (accounts == null) {
                        accounts = LinkedHashMap()
                    }

                    val account = parseAccount(xpp, accountUuids, overview)

                    if (account == null) {
                        // Do nothing - parseAccount() already logged a message
                    } else if (!accounts.containsKey(account.uuid)) {
                        accounts[account.uuid!!] = account
                    } else {
                        Timber.w("Duplicate account entries with UUID %s. Ignoring!", account.uuid)
                    }
                } else {
                    Timber.w("Unexpected start tag: %s", xpp.name)
                }
            }
            eventType = xpp.next()
        }

        return accounts
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseAccount(xpp: XmlPullParser, accountUuids: List<String>?, overview: Boolean): ImportedAccount? {
        val uuid = xpp.getAttributeValue(null, SettingsExporter.UUID_ATTRIBUTE)

        try {
            UUID.fromString(uuid)
        } catch (e: Exception) {
            skipToEndTag(xpp, SettingsExporter.ACCOUNT_ELEMENT)
            Timber.w("Skipping account with invalid UUID %s", uuid)
            return null
        }

        val account = ImportedAccount()
        account.uuid = uuid

        if (overview || accountUuids!!.contains(uuid)) {
            var eventType = xpp.next()
            while (!(eventType == XmlPullParser.END_TAG && SettingsExporter.ACCOUNT_ELEMENT == xpp.name)) {
                if (eventType == XmlPullParser.START_TAG) {
                    val element = xpp.name
                    if (SettingsExporter.NAME_ELEMENT == element) {
                        account.name = getText(xpp)
                    } else if (SettingsExporter.INCOMING_SERVER_ELEMENT == element) {
                        if (overview) {
                            skipToEndTag(xpp, SettingsExporter.INCOMING_SERVER_ELEMENT)
                        } else {
                            account.incoming = parseServerSettings(xpp, SettingsExporter.INCOMING_SERVER_ELEMENT)
                        }
                    } else if (SettingsExporter.OUTGOING_SERVER_ELEMENT == element) {
                        if (overview) {
                            skipToEndTag(xpp, SettingsExporter.OUTGOING_SERVER_ELEMENT)
                        } else {
                            account.outgoing = parseServerSettings(xpp, SettingsExporter.OUTGOING_SERVER_ELEMENT)
                        }
                    } else if (SettingsExporter.SETTINGS_ELEMENT == element) {
                        if (overview) {
                            skipToEndTag(xpp, SettingsExporter.SETTINGS_ELEMENT)
                        } else {
                            account.settings = parseSettings(xpp, SettingsExporter.SETTINGS_ELEMENT)
                        }
                    } else if (SettingsExporter.IDENTITIES_ELEMENT == element) {
                        account.identities = parseIdentities(xpp)
                    } else if (SettingsExporter.FOLDERS_ELEMENT == element) {
                        if (overview) {
                            skipToEndTag(xpp, SettingsExporter.FOLDERS_ELEMENT)
                        } else {
                            account.folders = parseFolders(xpp)
                        }
                    } else {
                        Timber.w("Unexpected start tag: %s", xpp.name)
                    }
                }
                eventType = xpp.next()
            }
        } else {
            skipToEndTag(xpp, SettingsExporter.ACCOUNT_ELEMENT)
            Timber.i("Skipping account with UUID %s", uuid)
        }

        // If we couldn't find an account name use the UUID
        if (account.name == null) {
            account.name = uuid
        }

        return account
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseServerSettings(xpp: XmlPullParser, endTag: String): ImportedServer {
        val server = ImportedServer()

        server.type = xpp.getAttributeValue(null, SettingsExporter.TYPE_ATTRIBUTE)

        var eventType = xpp.next()
        while (!(eventType == XmlPullParser.END_TAG && endTag == xpp.name)) {
            if (eventType == XmlPullParser.START_TAG) {
                val element = xpp.name
                if (SettingsExporter.HOST_ELEMENT == element) {
                    server.host = getText(xpp)
                } else if (SettingsExporter.PORT_ELEMENT == element) {
                    server.port = getText(xpp)
                } else if (SettingsExporter.CONNECTION_SECURITY_ELEMENT == element) {
                    server.connectionSecurity = getText(xpp)
                } else if (SettingsExporter.AUTHENTICATION_TYPE_ELEMENT == element) {
                    val text = getText(xpp)
                    server.authenticationType = AuthType.valueOf(text)
                } else if (SettingsExporter.USERNAME_ELEMENT == element) {
                    server.username = getText(xpp)
                } else if (SettingsExporter.CLIENT_CERTIFICATE_ALIAS_ELEMENT == element) {
                    server.clientCertificateAlias = getText(xpp)
                } else if (SettingsExporter.PASSWORD_ELEMENT == element) {
                    server.password = getText(xpp)
                } else if (SettingsExporter.EXTRA_ELEMENT == element) {
                    server.extras = parseSettings(xpp, SettingsExporter.EXTRA_ELEMENT)
                } else {
                    Timber.w("Unexpected start tag: %s", xpp.name)
                }
            }
            eventType = xpp.next()
        }

        return server
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseIdentities(xpp: XmlPullParser): List<ImportedIdentity>? {
        var identities: MutableList<ImportedIdentity>? = null

        var eventType = xpp.next()
        while (!(eventType == XmlPullParser.END_TAG && SettingsExporter.IDENTITIES_ELEMENT == xpp.name)) {
            if (eventType == XmlPullParser.START_TAG) {
                val element = xpp.name
                if (SettingsExporter.IDENTITY_ELEMENT == element) {
                    if (identities == null) {
                        identities = ArrayList()
                    }

                    val identity = parseIdentity(xpp)
                    identities.add(identity)
                } else {
                    Timber.w("Unexpected start tag: %s", xpp.name)
                }
            }
            eventType = xpp.next()
        }

        return identities
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseIdentity(xpp: XmlPullParser): ImportedIdentity {
        val identity = ImportedIdentity()

        var eventType = xpp.next()
        while (!(eventType == XmlPullParser.END_TAG && SettingsExporter.IDENTITY_ELEMENT == xpp.name)) {
            if (eventType == XmlPullParser.START_TAG) {
                val element = xpp.name
                if (SettingsExporter.NAME_ELEMENT == element) {
                    identity.name = getText(xpp)
                } else if (SettingsExporter.EMAIL_ELEMENT == element) {
                    identity.email = getText(xpp)
                } else if (SettingsExporter.DESCRIPTION_ELEMENT == element) {
                    identity.description = getText(xpp)
                } else if (SettingsExporter.SETTINGS_ELEMENT == element) {
                    identity.settings = parseSettings(xpp, SettingsExporter.SETTINGS_ELEMENT)
                } else {
                    Timber.w("Unexpected start tag: %s", xpp.name)
                }
            }
            eventType = xpp.next()
        }

        return identity
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseFolders(xpp: XmlPullParser): List<ImportedFolder>? {
        var folders: MutableList<ImportedFolder>? = null

        var eventType = xpp.next()
        while (!(eventType == XmlPullParser.END_TAG && SettingsExporter.FOLDERS_ELEMENT == xpp.name)) {
            if (eventType == XmlPullParser.START_TAG) {
                val element = xpp.name
                if (SettingsExporter.FOLDER_ELEMENT == element) {
                    if (folders == null) {
                        folders = ArrayList()
                    }

                    val folder = parseFolder(xpp)
                    folders.add(folder)
                } else {
                    Timber.w("Unexpected start tag: %s", xpp.name)
                }
            }
            eventType = xpp.next()
        }

        return folders
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseFolder(xpp: XmlPullParser): ImportedFolder {
        val folder = ImportedFolder()

        folder.name = xpp.getAttributeValue(null, SettingsExporter.NAME_ATTRIBUTE)

        folder.settings = parseSettings(xpp, SettingsExporter.FOLDER_ELEMENT)

        return folder
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun getText(xpp: XmlPullParser): String {
        val eventType = xpp.next()
        if (eventType != XmlPullParser.TEXT) {
            return ""
        }
        return xpp.text
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skipToEndTag(xpp: XmlPullParser, endTag: String) {
        var eventType = xpp.next()
        while (!(eventType == XmlPullParser.END_TAG && endTag == xpp.name)) {
            eventType = xpp.next()
        }
    }
}
