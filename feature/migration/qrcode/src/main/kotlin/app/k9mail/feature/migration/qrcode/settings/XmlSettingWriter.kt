package app.k9mail.feature.migration.qrcode.settings

import android.util.Xml
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData.Account
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData.Identity
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData.IncomingServer
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData.OutgoingServer
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData.OutgoingServerGroup
import app.k9mail.legacy.account.Account.DeletePolicy
import java.io.OutputStream
import org.xmlpull.v1.XmlSerializer

// TODO: This duplicates much of the code in SettingsExporter. Add an abstraction layer for the input data, so we can
//  use a single XML writer class for exporting accounts and writing QR code payloads to a settings file.
@Suppress("TooManyFunctions")
internal class XmlSettingWriter(
    private val uuidGenerator: UuidGenerator,
) {
    fun writeSettings(outputStream: OutputStream, accounts: List<Account>) {
        val serializer = Xml.newSerializer()
        serializer.setOutput(outputStream, "UTF-8")

        serializer.startDocument(null, true)

        // Output with indentation
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

        serializer.writeRoot(accounts)

        serializer.endDocument()
        serializer.flush()
    }

    private fun XmlSerializer.writeRoot(accounts: List<Account>) {
        startTag(null, ROOT_ELEMENT)
        attribute(null, VERSION_ATTRIBUTE, SETTINGS_VERSION.toString())
        attribute(null, FILE_FORMAT_ATTRIBUTE, FILE_FORMAT_VERSION.toString())

        writeAccounts(accounts)

        endTag(null, ROOT_ELEMENT)
    }

    private fun XmlSerializer.writeAccounts(accounts: List<Account>) {
        startTag(null, ACCOUNTS_ELEMENT)

        for (account in accounts) {
            writeAccount(account)
        }

        endTag(null, ACCOUNTS_ELEMENT)
    }

    private fun XmlSerializer.writeAccount(account: Account) {
        val accountUuid = uuidGenerator.generateUuid()

        startTag(null, ACCOUNT_ELEMENT)
        attribute(null, UUID_ATTRIBUTE, accountUuid)

        writeElement(NAME_ELEMENT, account.accountName)
        writeSettings(account)
        writeIncomingServer(account.incomingServer)
        writeOutgoingServers(account.outgoingServerGroups)

        endTag(null, ACCOUNT_ELEMENT)
    }

    private fun XmlSerializer.writeSettings(account: Account) {
        startTag(null, SETTINGS_ELEMENT)
        writeKeyValue("deletePolicy", account.deletePolicy.toSettingsFileValue())
        endTag(null, SETTINGS_ELEMENT)
    }

    private fun XmlSerializer.writeKeyValue(key: String, value: String?) {
        startTag(null, VALUE_ELEMENT)
        attribute(null, KEY_ATTRIBUTE, key)
        if (value != null) {
            text(value)
        }
        endTag(null, VALUE_ELEMENT)
    }

    private fun XmlSerializer.writeIncomingServer(incomingServer: IncomingServer) {
        startTag(null, INCOMING_SERVER_ELEMENT)
        attribute(null, TYPE_ATTRIBUTE, incomingServer.protocol.mapToSettingsString())

        writeElement(HOST_ELEMENT, incomingServer.hostname.value)
        writeElement(PORT_ELEMENT, incomingServer.port.value.toString())
        writeElement(CONNECTION_SECURITY_ELEMENT, incomingServer.connectionSecurity.mapToSettingsString())
        writeElement(AUTHENTICATION_TYPE_ELEMENT, incomingServer.authenticationType.mapToSettingsString())
        writeElement(USERNAME_ELEMENT, incomingServer.username)

        incomingServer.password?.let { password ->
            writeElement(PASSWORD_ELEMENT, password)
        }

        endTag(null, INCOMING_SERVER_ELEMENT)
    }

    private fun XmlSerializer.writeOutgoingServers(outgoingServerGroups: List<OutgoingServerGroup>) {
        val outgoingServerGroup = outgoingServerGroups.first()
        writeOutgoingServer(outgoingServerGroup.outgoingServer)
        writeIdentities(outgoingServerGroup.identities)
    }

    private fun XmlSerializer.writeOutgoingServer(outgoingServer: OutgoingServer) {
        startTag(null, OUTGOING_SERVER_ELEMENT)
        attribute(null, TYPE_ATTRIBUTE, outgoingServer.protocol.mapToSettingsString())

        writeElement(HOST_ELEMENT, outgoingServer.hostname.value)
        writeElement(PORT_ELEMENT, outgoingServer.port.value.toString())
        writeElement(CONNECTION_SECURITY_ELEMENT, outgoingServer.connectionSecurity.mapToSettingsString())
        writeElement(AUTHENTICATION_TYPE_ELEMENT, outgoingServer.authenticationType.mapToSettingsString())
        writeElement(USERNAME_ELEMENT, outgoingServer.username)

        outgoingServer.password?.let { password ->
            writeElement(PASSWORD_ELEMENT, password)
        }

        endTag(null, OUTGOING_SERVER_ELEMENT)
    }

    private fun XmlSerializer.writeIdentities(identities: List<Identity>) {
        startTag(null, IDENTITIES_ELEMENT)

        for (identity in identities) {
            writeIdentity(identity)
        }

        endTag(null, IDENTITIES_ELEMENT)
    }

    private fun XmlSerializer.writeIdentity(identity: Identity) {
        startTag(null, IDENTITY_ELEMENT)

        writeElement(NAME_ELEMENT, identity.displayName)
        writeElement(EMAIL_ELEMENT, identity.emailAddress.address)

        endTag(null, IDENTITY_ELEMENT)
    }

    private fun XmlSerializer.writeElement(elementName: String, value: String) {
        startTag(null, elementName)
        text(value)
        endTag(null, elementName)
    }

    companion object {
        // It's not necessary to keep this in sync with com.fsck.k9.preferences.Settings.VERSION because the import
        // code supports importing old settings files.
        private const val SETTINGS_VERSION = 99
        private const val FILE_FORMAT_VERSION = 1

        private const val ROOT_ELEMENT = "k9settings"
        private const val VERSION_ATTRIBUTE = "version"
        private const val FILE_FORMAT_ATTRIBUTE = "format"
        private const val ACCOUNTS_ELEMENT = "accounts"
        private const val ACCOUNT_ELEMENT = "account"
        private const val UUID_ATTRIBUTE = "uuid"
        private const val SETTINGS_ELEMENT = "settings"
        private const val VALUE_ELEMENT = "value"
        private const val KEY_ATTRIBUTE = "key"
        private const val INCOMING_SERVER_ELEMENT = "incoming-server"
        private const val OUTGOING_SERVER_ELEMENT = "outgoing-server"
        private const val TYPE_ATTRIBUTE = "type"
        private const val HOST_ELEMENT = "host"
        private const val PORT_ELEMENT = "port"
        private const val CONNECTION_SECURITY_ELEMENT = "connection-security"
        private const val AUTHENTICATION_TYPE_ELEMENT = "authentication-type"
        private const val USERNAME_ELEMENT = "username"
        private const val PASSWORD_ELEMENT = "password"
        private const val IDENTITIES_ELEMENT = "identities"
        private const val IDENTITY_ELEMENT = "identity"
        private const val NAME_ELEMENT = "name"
        private const val EMAIL_ELEMENT = "email"
    }
}

private fun DeletePolicy.toSettingsFileValue(): String {
    return when (this) {
        DeletePolicy.NEVER -> "NEVER"
        DeletePolicy.SEVEN_DAYS -> error("Unsupported value")
        DeletePolicy.ON_DELETE -> "DELETE"
        DeletePolicy.MARK_AS_READ -> "MARK_AS_READ"
    }
}
