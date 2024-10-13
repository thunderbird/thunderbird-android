package app.k9mail.feature.migration.qrcode.settings

import app.k9mail.core.common.mail.toUserEmailAddress
import app.k9mail.core.common.net.toHostname
import app.k9mail.core.common.net.toPort
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData.ConnectionSecurity
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import okio.Buffer
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class XmlSettingWriterTest {
    private val xmlSettingsWriter = XmlSettingWriter(
        uuidGenerator = { "test-uuid" },
    )

    @Test
    fun `XML should match expected output`() {
        val buffer = Buffer()
        val accounts = listOf(ACCOUNT)

        buffer.outputStream().use { outputStream ->
            xmlSettingsWriter.writeSettings(outputStream, accounts)
        }

        val xmlString = buffer.readUtf8().normalizeLineBreaks()
        assertThat(xmlString).isEqualTo(EXPECTED_OUTPUT)
    }

    private fun String.normalizeLineBreaks() = replace("\r\n", "\n")

    companion object {
        private val ACCOUNT = AccountData.Account(
            accountName = "Account name",
            incomingServer = AccountData.IncomingServer(
                protocol = AccountData.IncomingServerProtocol.Imap,
                hostname = "imap.domain.example".toHostname(),
                port = 993.toPort(),
                connectionSecurity = ConnectionSecurity.Tls,
                authenticationType = AccountData.AuthenticationType.PasswordCleartext,
                username = "user@domain.example",
                password = "password",
            ),
            outgoingServerGroups = listOf(
                AccountData.OutgoingServerGroup(
                    outgoingServer = AccountData.OutgoingServer(
                        protocol = AccountData.OutgoingServerProtocol.Smtp,
                        hostname = "smtp.domain.example".toHostname(),
                        port = 465.toPort(),
                        connectionSecurity = ConnectionSecurity.Tls,
                        authenticationType = AccountData.AuthenticationType.PasswordCleartext,
                        username = "user@domain.example",
                        password = "password",
                    ),
                    identities = listOf(
                        AccountData.Identity(
                            emailAddress = "user@domain.example".toUserEmailAddress(),
                            displayName = "Firstname Lastname",
                        ),
                    ),
                ),
            ),
        )

        private val EXPECTED_OUTPUT =
            """
            <?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
            <k9settings version="99" format="1">
              <accounts>
                <account uuid="test-uuid">
                  <name>Account name</name>
                  <incoming-server type="IMAP">
                    <host>imap.domain.example</host>
                    <port>993</port>
                    <connection-security>SSL_TLS_REQUIRED</connection-security>
                    <authentication-type>PLAIN</authentication-type>
                    <username>user@domain.example</username>
                    <password>password</password>
                  </incoming-server>
                  <outgoing-server type="SMTP">
                    <host>smtp.domain.example</host>
                    <port>465</port>
                    <connection-security>SSL_TLS_REQUIRED</connection-security>
                    <authentication-type>PLAIN</authentication-type>
                    <username>user@domain.example</username>
                    <password>password</password>
                  </outgoing-server>
                  <identities>
                    <identity>
                      <name>Firstname Lastname</name>
                      <email>user@domain.example</email>
                    </identity>
                  </identities>
                </account>
              </accounts>
            </k9settings>
            """.trimIndent()
    }
}
