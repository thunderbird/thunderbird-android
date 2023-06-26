package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.entity.IncomingProtocolType
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CheckIncomingServerConfigTest {

    @Test
    fun `should check with imap validator when protocol is imap`() = runTest {
        val testSubject = CheckIncomingServerConfig(
            imapValidator = { ServerSettingsValidationResult.Success },
            pop3Validator = { ServerSettingsValidationResult.NetworkError(IOException("Failed")) },
        )
        val protocolType = IncomingProtocolType.IMAP
        val serverSettings = serverSettings(protocolType = protocolType)

        val result = testSubject.execute(protocolType, serverSettings)

        assertThat(result).isEqualTo(ServerSettingsValidationResult.Success)
    }

    @Test
    fun `should check with imap validator when protocol is imap and return failure`() = runTest {
        val failure = ServerSettingsValidationResult.ServerError("Failed")
        val testSubject = CheckIncomingServerConfig(
            imapValidator = { failure },
            pop3Validator = { ServerSettingsValidationResult.NetworkError(IOException("Failed")) },
        )
        val protocolType = IncomingProtocolType.IMAP
        val serverSettings = serverSettings(protocolType = protocolType)

        val result = testSubject.execute(protocolType, serverSettings)

        assertThat(result).isEqualTo(failure)
    }

    @Test
    fun `should check with pop3 validator when protocol is pop3`() = runTest {
        val testSubject = CheckIncomingServerConfig(
            imapValidator = { ServerSettingsValidationResult.NetworkError(IOException("Failed")) },
            pop3Validator = { ServerSettingsValidationResult.Success },
        )
        val protocolType = IncomingProtocolType.POP3
        val serverSettings = serverSettings(protocolType = protocolType)

        val result = testSubject.execute(protocolType, serverSettings)

        assertThat(result).isEqualTo(ServerSettingsValidationResult.Success)
    }

    @Test
    fun `should check with pop3 validator when protocol is pop3 and return failure`() = runTest {
        val failure = ServerSettingsValidationResult.ServerError("Failed")
        val testSubject = CheckIncomingServerConfig(
            imapValidator = { ServerSettingsValidationResult.NetworkError(IOException("Failed")) },
            pop3Validator = { failure },
        )
        val protocolType = IncomingProtocolType.POP3
        val serverSettings = serverSettings(protocolType = protocolType)

        val result = testSubject.execute(protocolType, serverSettings)

        assertThat(result).isEqualTo(failure)
    }

    private companion object {
        fun serverSettings(protocolType: IncomingProtocolType) = ServerSettings(
            type = protocolType.defaultName,
            host = "${protocolType.defaultName}.example.org",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
        )
    }
}
