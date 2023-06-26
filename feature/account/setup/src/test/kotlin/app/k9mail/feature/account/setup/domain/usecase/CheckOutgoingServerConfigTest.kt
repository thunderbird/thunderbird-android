package app.k9mail.feature.account.setup.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CheckOutgoingServerConfigTest {

    @Test
    fun `should validate server settings and return success`() = runTest {
        val testSubject = CheckOutgoingServerConfig(
            smtpValidator = { ServerSettingsValidationResult.Success },
        )

        val result = testSubject.execute(serverSettings)

        assertThat(result).isEqualTo(ServerSettingsValidationResult.Success)
    }

    @Test
    fun `should validate server settings and return failure`() = runTest {
        val failure = ServerSettingsValidationResult.ServerError("Failed")
        val testSubject = CheckOutgoingServerConfig(
            smtpValidator = { failure },
        )

        val result = testSubject.execute(serverSettings)

        assertThat(result).isEqualTo(failure)
    }

    private companion object {
        val serverSettings = ServerSettings(
            type = "smtp",
            host = "smtp.example.org",
            port = 587,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
        )
    }
}
