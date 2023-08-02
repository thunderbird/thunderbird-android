package app.k9mail.feature.account.setup.data

import app.k9mail.feature.account.oauth.domain.entity.AuthorizationState
import app.k9mail.feature.account.setup.domain.entity.AccountOptions
import app.k9mail.feature.account.setup.domain.entity.AccountSetupState
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import org.junit.Test

class InMemoryAccountSetupStateRepositoryTest {

    @Test
    fun `should initialize with empty state`() {
        val testSubject = InMemoryAccountSetupStateRepository()

        val result = testSubject.getState()

        assertThat(result).isEqualTo(
            AccountSetupState(
                emailAddress = null,
                incomingServerSettings = null,
                outgoingServerSettings = null,
                authorizationState = null,
                options = null,
            ),
        )
    }

    @Test
    fun `should save state`() {
        val testSubject = InMemoryAccountSetupStateRepository(
            AccountSetupState(
                emailAddress = "emailAddress",
                incomingServerSettings = INCOMING_SERVER_SETTINGS,
                outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
                authorizationState = AuthorizationState("authorizationState"),
                options = OPTIONS,
            ),
        )
        val newState = AccountSetupState(
            emailAddress = "emailAddress2",
            incomingServerSettings = INCOMING_SERVER_SETTINGS.copy(host = "imap2.example.org"),
            outgoingServerSettings = OUTGOING_SERVER_SETTINGS.copy(host = "smtp2.example.org"),
            authorizationState = AuthorizationState("authorizationState2"),
            options = OPTIONS.copy(
                accountName = "accountName2",
                displayName = "displayName2",
                emailSignature = "emailSignature2",
                checkFrequencyInMinutes = 50,
                messageDisplayCount = 60,
                showNotification = false,
            ),
        )

        testSubject.save(newState)

        assertThat(testSubject.getState()).isEqualTo(newState)
    }

    @Test
    fun `should save email address`() {
        val testSubject = InMemoryAccountSetupStateRepository()

        testSubject.saveEmailAddress("emailAddress")

        assertThat(testSubject.getState().emailAddress)
            .isEqualTo("emailAddress")
    }

    @Test
    fun `should save incoming server settings`() {
        val testSubject = InMemoryAccountSetupStateRepository()

        testSubject.saveIncomingServerSettings(INCOMING_SERVER_SETTINGS)

        assertThat(testSubject.getState().incomingServerSettings)
            .isEqualTo(INCOMING_SERVER_SETTINGS)
    }

    @Test
    fun `should save outgoing server settings`() {
        val testSubject = InMemoryAccountSetupStateRepository()

        testSubject.saveOutgoingServerSettings(OUTGOING_SERVER_SETTINGS)

        assertThat(testSubject.getState().outgoingServerSettings)
            .isEqualTo(OUTGOING_SERVER_SETTINGS)
    }

    @Test
    fun `should save authorization state`() {
        val testSubject = InMemoryAccountSetupStateRepository()

        testSubject.saveAuthorizationState(AuthorizationState("authorizationState"))

        assertThat(testSubject.getState().authorizationState)
            .isEqualTo(AuthorizationState("authorizationState"))
    }

    @Test
    fun `should save options`() {
        val testSubject = InMemoryAccountSetupStateRepository()

        testSubject.saveOptions(OPTIONS)

        assertThat(testSubject.getState().options)
            .isEqualTo(OPTIONS)
    }

    @Test
    fun `should clear state`() {
        val testSubject = InMemoryAccountSetupStateRepository(
            AccountSetupState(
                emailAddress = "emailAddress",
                incomingServerSettings = INCOMING_SERVER_SETTINGS,
                outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
                authorizationState = AuthorizationState("authorizationState"),
                options = OPTIONS,
            ),
        )

        testSubject.clear()

        assertThat(testSubject.getState()).isEqualTo(
            AccountSetupState(
                emailAddress = null,
                incomingServerSettings = null,
                outgoingServerSettings = null,
                authorizationState = null,
                options = null,
            ),
        )
    }

    private companion object {
        val INCOMING_SERVER_SETTINGS = ServerSettings(
            "imap",
            "imap.example.org",
            993,
            ConnectionSecurity.SSL_TLS_REQUIRED,
            AuthType.PLAIN,
            "username",
            "password",
            null,
        )

        val OUTGOING_SERVER_SETTINGS = ServerSettings(
            "smtp",
            "smtp.example.org",
            465,
            ConnectionSecurity.SSL_TLS_REQUIRED,
            AuthType.PLAIN,
            "username",
            "password",
            null,
        )

        val OPTIONS = AccountOptions(
            accountName = "accountName",
            displayName = "displayName",
            emailSignature = "emailSignature",
            checkFrequencyInMinutes = 10,
            messageDisplayCount = 20,
            showNotification = true,
        )
    }
}
