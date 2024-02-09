package app.k9mail.feature.account.server.validation.domain.usecase

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
    private val authStateStorage = FakeAuthStateStorage()

    @Test
    fun `should check with imap validator when protocol is imap`() = runTest {
        val testSubject = ValidateServerSettings(
            authStateStorage = authStateStorage,
            imapValidator = { _, _ -> ServerSettingsValidationResult.Success },
            pop3Validator = { _, _ -> ServerSettingsValidationResult.NetworkError(IOException("Failed POP3")) },
            smtpValidator = { _, _ -> ServerSettingsValidationResult.NetworkError(IOException("Failed SMTP")) },
        )

        val result = testSubject.execute(IMAP_SERVER_SETTINGS)

        assertThat(result).isEqualTo(ServerSettingsValidationResult.Success)
    }

    @Test
    fun `should check with imap validator when protocol is imap and return failure`() = runTest {
        val failure = ServerSettingsValidationResult.ServerError("Failed")
        val testSubject = ValidateServerSettings(
            authStateStorage = authStateStorage,
            imapValidator = { _, _ -> failure },
            pop3Validator = { _, _ -> ServerSettingsValidationResult.NetworkError(IOException("Failed POP3")) },
            smtpValidator = { _, _ -> ServerSettingsValidationResult.NetworkError(IOException("Failed SMTP")) },
        )

        val result = testSubject.execute(IMAP_SERVER_SETTINGS)

        assertThat(result).isEqualTo(failure)
    }

    @Test
    fun `should check with pop3 validator when protocol is pop3`() = runTest {
        val testSubject = ValidateServerSettings(
            authStateStorage = authStateStorage,
            imapValidator = { _, _ -> ServerSettingsValidationResult.NetworkError(IOException("Failed IMAP")) },
            pop3Validator = { _, _ -> ServerSettingsValidationResult.Success },
            smtpValidator = { _, _ -> ServerSettingsValidationResult.NetworkError(IOException("Failed SMTP")) },
        )

        val result = testSubject.execute(POP3_SERVER_SETTINGS)

        assertThat(result).isEqualTo(ServerSettingsValidationResult.Success)
    }

    @Test
    fun `should check with pop3 validator when protocol is pop3 and return failure`() = runTest {
        val failure = ServerSettingsValidationResult.ServerError("Failed POP3")
        val testSubject = ValidateServerSettings(
            authStateStorage = authStateStorage,
            imapValidator = { _, _ -> ServerSettingsValidationResult.NetworkError(IOException("Failed IMAP")) },
            pop3Validator = { _, _ -> failure },
            smtpValidator = { _, _ -> ServerSettingsValidationResult.NetworkError(IOException("Failed SMTP")) },
        )

        val result = testSubject.execute(POP3_SERVER_SETTINGS)

        assertThat(result).isEqualTo(failure)
    }

    @Test
    fun `should check with smtp validator when protocol is smtp`() = runTest {
        val testSubject = ValidateServerSettings(
            authStateStorage = authStateStorage,
            imapValidator = { _, _ -> ServerSettingsValidationResult.NetworkError(IOException("Failed IMAP")) },
            pop3Validator = { _, _ -> ServerSettingsValidationResult.NetworkError(IOException("Failed POP3")) },
            smtpValidator = { _, _ -> ServerSettingsValidationResult.Success },
        )

        val result = testSubject.execute(SMTP_SERVER_SETTINGS)

        assertThat(result).isEqualTo(ServerSettingsValidationResult.Success)
    }

    @Test
    fun `should check with smtp validator when protocol is smtp and return failure`() = runTest {
        val failure = ServerSettingsValidationResult.ServerError("Failed SMTP")
        val testSubject = ValidateServerSettings(
            authStateStorage = authStateStorage,
            imapValidator = { _, _ -> ServerSettingsValidationResult.NetworkError(IOException("Failed IMAP")) },
            pop3Validator = { _, _ -> ServerSettingsValidationResult.NetworkError(IOException("Failed POP3")) },
            smtpValidator = { _, _ -> failure },
        )

        val result = testSubject.execute(SMTP_SERVER_SETTINGS)

        assertThat(result).isEqualTo(failure)
    }

    @Test
    fun `should validate successfully for demo settings`() = runTest {
        val testSubject = ValidateServerSettings(
            authStateStorage = authStateStorage,
            imapValidator = { _, _ -> ServerSettingsValidationResult.NetworkError(IOException("Failed IMAP")) },
            pop3Validator = { _, _ -> ServerSettingsValidationResult.NetworkError(IOException("Failed POP3")) },
            smtpValidator = { _, _ -> ServerSettingsValidationResult.NetworkError(IOException("Failed SMTP")) },
        )

        val result = testSubject.execute(
            ServerSettings(
                type = "demo",
                host = "demo.example.com",
                port = 993,
                connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                authenticationType = AuthType.PLAIN,
                username = "user",
                password = "password",
                clientCertificateAlias = null,
            ),
        )

        assertThat(result).isEqualTo(ServerSettingsValidationResult.Success)
    }

    private companion object {

        val IMAP_SERVER_SETTINGS = ServerSettings(
            type = "imap",
            host = "imap.example.com",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
        )

        val POP3_SERVER_SETTINGS = ServerSettings(
            type = "pop3",
            host = "pop3.example.com",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
        )

        val SMTP_SERVER_SETTINGS = ServerSettings(
            type = "smtp",
            host = "smtp.example.com",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
        )
    }
}
