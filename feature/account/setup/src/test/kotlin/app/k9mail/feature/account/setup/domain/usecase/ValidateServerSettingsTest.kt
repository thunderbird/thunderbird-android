package app.k9mail.feature.account.setup.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ValidateServerSettingsTest {

    @Test
    fun `should check with imap validator when protocol is imap`() = runTest {
        val testSubject = ValidateServerSettings(
            imapValidator = { ServerSettingsValidationResult.Success },
            pop3Validator = { ServerSettingsValidationResult.NetworkError(IOException("Failed POP3")) },
            smtpValidator = { ServerSettingsValidationResult.NetworkError(IOException("Failed SMTP")) },
        )

        val result = testSubject.execute(IMAP_SERVER_SETTINGS)

        assertThat(result).isEqualTo(ServerSettingsValidationResult.Success)
    }

    @Test
    fun `should check with imap validator when protocol is imap and return failure`() = runTest {
        val failure = ServerSettingsValidationResult.ServerError("Failed")
        val testSubject = ValidateServerSettings(
            imapValidator = { failure },
            pop3Validator = { ServerSettingsValidationResult.NetworkError(IOException("Failed POP3")) },
            smtpValidator = { ServerSettingsValidationResult.NetworkError(IOException("Failed SMTP")) },
        )

        val result = testSubject.execute(IMAP_SERVER_SETTINGS)

        assertThat(result).isEqualTo(failure)
    }

    @Test
    fun `should check with pop3 validator when protocol is pop3`() = runTest {
        val testSubject = ValidateServerSettings(
            imapValidator = { ServerSettingsValidationResult.NetworkError(IOException("Failed IMAP")) },
            pop3Validator = { ServerSettingsValidationResult.Success },
            smtpValidator = { ServerSettingsValidationResult.NetworkError(IOException("Failed SMTP")) },
        )

        val result = testSubject.execute(POP3_SERVER_SETTINGS)

        assertThat(result).isEqualTo(ServerSettingsValidationResult.Success)
    }

    @Test
    fun `should check with pop3 validator when protocol is pop3 and return failure`() = runTest {
        val failure = ServerSettingsValidationResult.ServerError("Failed POP3")
        val testSubject = ValidateServerSettings(
            imapValidator = { ServerSettingsValidationResult.NetworkError(IOException("Failed IMAP")) },
            pop3Validator = { failure },
            smtpValidator = { ServerSettingsValidationResult.NetworkError(IOException("Failed SMTP")) },
        )

        val result = testSubject.execute(POP3_SERVER_SETTINGS)

        assertThat(result).isEqualTo(failure)
    }

    @Test
    fun `should check with smtp validator when protocol is smtp`() = runTest {
        val testSubject = ValidateServerSettings(
            imapValidator = { ServerSettingsValidationResult.NetworkError(IOException("Failed IMAP")) },
            pop3Validator = { ServerSettingsValidationResult.NetworkError(IOException("Failed POP3")) },
            smtpValidator = { ServerSettingsValidationResult.Success },
        )

        val result = testSubject.execute(SMTP_SERVER_SETTINGS)

        assertThat(result).isEqualTo(ServerSettingsValidationResult.Success)
    }

    @Test
    fun `should check with smtp validator when protocol is smtp and return failure`() = runTest {
        val failure = ServerSettingsValidationResult.ServerError("Failed SMTP")
        val testSubject = ValidateServerSettings(
            imapValidator = { ServerSettingsValidationResult.NetworkError(IOException("Failed IMAP")) },
            pop3Validator = { ServerSettingsValidationResult.NetworkError(IOException("Failed POP3")) },
            smtpValidator = { failure },
        )

        val result = testSubject.execute(SMTP_SERVER_SETTINGS)

        assertThat(result).isEqualTo(failure)
    }

    private companion object {

        val IMAP_SERVER_SETTINGS = ServerSettings(
            type = "imap",
            host = "imap.example.org",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
        )

        val POP3_SERVER_SETTINGS = ServerSettings(
            type = "pop3",
            host = "pop3.example.org",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
        )

        val SMTP_SERVER_SETTINGS = ServerSettings(
            type = "smtp",
            host = "smtp.example.org",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
        )
    }
}
