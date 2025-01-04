package app.k9mail.feature.account.common.data

import app.k9mail.feature.account.common.domain.entity.AccountDisplayOptions
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AccountSyncOptions
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import org.junit.Test

class InMemoryAccountStateRepositoryTest {

    @Test
    fun `should initialize with empty state`() {
        val testSubject = InMemoryAccountStateRepository()

        val result = testSubject.getState()

        assertThat(result).isEqualTo(
            AccountState(
                uuid = null,
                emailAddress = null,
                incomingServerSettings = null,
                outgoingServerSettings = null,
                authorizationState = null,
                displayOptions = null,
                syncOptions = null,
            ),
        )
    }

    @Test
    fun `should set state`() {
        val testSubject = InMemoryAccountStateRepository(
            AccountState(
                uuid = "uuid",
                emailAddress = "emailAddress",
                incomingServerSettings = INCOMING_SERVER_SETTINGS,
                outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
                authorizationState = AuthorizationState("authorizationState"),
                displayOptions = DISPLAY_OPTIONS,
                syncOptions = SYNC_OPTIONS,
            ),
        )
        val newState = AccountState(
            uuid = "uuid2",
            emailAddress = "emailAddress2",
            incomingServerSettings = INCOMING_SERVER_SETTINGS.copy(host = "imap2.example.org"),
            outgoingServerSettings = OUTGOING_SERVER_SETTINGS.copy(host = "smtp2.example.org"),
            authorizationState = AuthorizationState("authorizationState2"),
            displayOptions = DISPLAY_OPTIONS.copy(
                accountName = "accountName2",
                displayName = "displayName2",
                emailSignature = "emailSignature2",
            ),
            syncOptions = SYNC_OPTIONS.copy(
                checkFrequencyInMinutes = 50,
                messageDisplayCount = 60,
                showNotification = false,
            ),
        )

        testSubject.setState(newState)

        assertThat(testSubject.getState()).isEqualTo(newState)
    }

    @Test
    fun `should set email address`() {
        val testSubject = InMemoryAccountStateRepository()

        testSubject.setEmailAddress("emailAddress")

        assertThat(testSubject.getState().emailAddress)
            .isEqualTo("emailAddress")
    }

    @Test
    fun `should set incoming server settings`() {
        val testSubject = InMemoryAccountStateRepository()

        testSubject.setIncomingServerSettings(INCOMING_SERVER_SETTINGS)

        assertThat(testSubject.getState().incomingServerSettings)
            .isEqualTo(INCOMING_SERVER_SETTINGS)
    }

    @Test
    fun `should set outgoing server settings`() {
        val testSubject = InMemoryAccountStateRepository()

        testSubject.setOutgoingServerSettings(OUTGOING_SERVER_SETTINGS)

        assertThat(testSubject.getState().outgoingServerSettings)
            .isEqualTo(OUTGOING_SERVER_SETTINGS)
    }

    @Test
    fun `should set authorization state`() {
        val testSubject = InMemoryAccountStateRepository()

        testSubject.setAuthorizationState(AuthorizationState("authorizationState"))

        assertThat(testSubject.getState().authorizationState)
            .isEqualTo(AuthorizationState("authorizationState"))
    }

    @Test
    fun `should set display options`() {
        val testSubject = InMemoryAccountStateRepository()

        testSubject.setDisplayOptions(DISPLAY_OPTIONS)

        assertThat(testSubject.getState().displayOptions).isEqualTo(DISPLAY_OPTIONS)
    }

    @Test
    fun `should set sync options`() {
        val testSubject = InMemoryAccountStateRepository()

        testSubject.setSyncOptions(SYNC_OPTIONS)

        assertThat(testSubject.getState().syncOptions).isEqualTo(SYNC_OPTIONS)
    }

    @Test
    fun `should clear state`() {
        val testSubject = InMemoryAccountStateRepository(
            AccountState(
                uuid = "uuid",
                emailAddress = "emailAddress",
                incomingServerSettings = INCOMING_SERVER_SETTINGS,
                outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
                authorizationState = AuthorizationState("authorizationState"),
                displayOptions = DISPLAY_OPTIONS,
                syncOptions = SYNC_OPTIONS,
            ),
        )

        testSubject.clear()

        assertThat(testSubject.getState()).isEqualTo(
            AccountState(
                uuid = null,
                emailAddress = null,
                incomingServerSettings = null,
                outgoingServerSettings = null,
                authorizationState = null,
                displayOptions = null,
                syncOptions = null,
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

        val DISPLAY_OPTIONS = AccountDisplayOptions(
            accountName = "accountName",
            displayName = "displayName",
            emailSignature = "emailSignature",
            showInUnifiedInbox = true,
        )

        val SYNC_OPTIONS = AccountSyncOptions(
            checkFrequencyInMinutes = 10,
            messageDisplayCount = 20,
            showNotification = true,
        )
    }
}
