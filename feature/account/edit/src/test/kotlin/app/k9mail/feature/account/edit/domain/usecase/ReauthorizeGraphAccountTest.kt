package app.k9mail.feature.account.edit.domain.usecase

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdaterFailure
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdaterResult
import app.k9mail.feature.account.edit.domain.AccountEditDomainContract
import app.k9mail.feature.account.edit.domain.GraphAccountReauthorizationResult
import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.mail.NativeApiMailServerSettings
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.common.oauth.OAuthConfiguration
import net.thunderbird.feature.account.AccountIdFactory
import org.junit.Test

class ReauthorizeGraphAccountTest {

    @Test
    fun `successful reauthorization replaces only authorization state`() = runTest {
        var persistedState = OLD_AUTHORIZATION_STATE
        var expectedState: AuthorizationState? = null
        var observedNewState: AuthorizationState? = null
        val testSubject = createTestSubject(
            currentState = ACCOUNT_STATE,
            isIdentityMatch = true,
            updateState = { expected, new ->
                expectedState = expected
                observedNewState = new
                if (expected == persistedState) {
                    persistedState = new
                    AccountUpdaterResult.Success(ACCOUNT_ID)
                } else {
                    AccountUpdaterResult.Failure(AccountUpdaterFailure.UnknownError(IllegalStateException()))
                }
            },
        )

        val result = testSubject.execute(ACCOUNT_ID, OAuthResult.Success(NEW_AUTHORIZATION_STATE))

        assertThat(result).isEqualTo(GraphAccountReauthorizationResult.Success)
        assertThat(expectedState).isEqualTo(OLD_AUTHORIZATION_STATE)
        assertThat(observedNewState).isEqualTo(NEW_AUTHORIZATION_STATE)
        assertThat(persistedState).isEqualTo(NEW_AUTHORIZATION_STATE)
    }

    @Test
    fun `canceled OAuth result leaves current authorization untouched`() = runTest {
        var updaterCalled = false
        val testSubject = createTestSubject(
            currentState = ACCOUNT_STATE,
            isIdentityMatch = true,
            updateState = { _, _ ->
                updaterCalled = true
                AccountUpdaterResult.Success(ACCOUNT_ID)
            },
        )

        val result = testSubject.execute(ACCOUNT_ID, OAuthResult.Failure)

        assertThat(result).isEqualTo(GraphAccountReauthorizationResult.Canceled)
        assertThat(updaterCalled).isEqualTo(false)
        assertThat(ACCOUNT_STATE.authorizationState).isEqualTo(OLD_AUTHORIZATION_STATE)
    }

    @Test
    fun `identity mismatch leaves current authorization untouched`() = runTest {
        var updaterCalled = false
        val testSubject = createTestSubject(
            currentState = ACCOUNT_STATE,
            isIdentityMatch = false,
            updateState = { _, _ ->
                updaterCalled = true
                AccountUpdaterResult.Success(ACCOUNT_ID)
            },
        )

        val result = testSubject.execute(ACCOUNT_ID, OAuthResult.Success(NEW_AUTHORIZATION_STATE))

        assertThat(result).isEqualTo(GraphAccountReauthorizationResult.MicrosoftAccountMismatch)
        assertThat(updaterCalled).isEqualTo(false)
        assertThat(ACCOUNT_STATE.authorizationState).isEqualTo(OLD_AUTHORIZATION_STATE)
    }

    @Test
    fun `uses the account specific Client ID for identity verification`() = runTest {
        var verifiedClientId: String? = null
        val currentState = ACCOUNT_STATE.copy(
            incomingServerSettings = GRAPH_SERVER_SETTINGS.copy(
                extra = mapOf(NativeApiMailServerSettings.EXTRA_OAUTH_CLIENT_ID to "account-specific-client"),
            ),
        )
        val testSubject = createTestSubject(
            currentState = currentState,
            isIdentityMatch = true,
            updateState = { _, _ -> AccountUpdaterResult.Success(ACCOUNT_ID) },
            onClientIdVerified = { verifiedClientId = it },
        )

        val result = testSubject.execute(ACCOUNT_ID, OAuthResult.Success(NEW_AUTHORIZATION_STATE))

        assertThat(result).isEqualTo(GraphAccountReauthorizationResult.Success)
        assertThat(verifiedClientId).isEqualTo("account-specific-client")
    }

    @Test
    fun `persistence failure leaves current authorization untouched`() = runTest {
        val testSubject = createTestSubject(
            currentState = ACCOUNT_STATE,
            isIdentityMatch = true,
            updateState = { _, _ ->
                AccountUpdaterResult.Failure(AccountUpdaterFailure.UnknownError(IllegalStateException()))
            },
        )

        val result = testSubject.execute(ACCOUNT_ID, OAuthResult.Success(NEW_AUTHORIZATION_STATE))

        assertThat(result).isEqualTo(GraphAccountReauthorizationResult.Failed)
        assertThat(ACCOUNT_STATE.authorizationState).isEqualTo(OLD_AUTHORIZATION_STATE)
    }

    @Test
    fun `non Graph account is not updated`() = runTest {
        var updaterCalled = false
        val state = ACCOUNT_STATE.copy(incomingServerSettings = GRAPH_SERVER_SETTINGS.copy(type = "imap"))
        val testSubject = createTestSubject(
            currentState = state,
            isIdentityMatch = true,
            updateState = { _, _ ->
                updaterCalled = true
                AccountUpdaterResult.Success(ACCOUNT_ID)
            },
        )

        val result = testSubject.execute(ACCOUNT_ID, OAuthResult.Success(NEW_AUTHORIZATION_STATE))

        assertThat(result).isEqualTo(GraphAccountReauthorizationResult.NotAvailable)
        assertThat(updaterCalled).isEqualTo(false)
    }

    private fun createTestSubject(
        currentState: AccountState,
        isIdentityMatch: Boolean,
        updateState: suspend (AuthorizationState?, AuthorizationState) -> AccountUpdaterResult,
        onClientIdVerified: (String) -> Unit = {},
    ) = ReauthorizeGraphAccount(
        loadAccountState = { requestedId ->
            assertThat(requestedId).isEqualTo(ACCOUNT_ID)
            currentState
        },
        accountAuthorizationStateUpdater = { requestedId, expectedState, newState ->
            assertThat(requestedId).isEqualTo(ACCOUNT_ID)
            updateState(expectedState, newState)
        },
        oauthConfigurationProvider = { hostname ->
            if (hostname == "graph.microsoft.com") GRAPH_CONFIGURATION else null
        },
        identityVerifier = AccountEditDomainContract.MicrosoftAccountIdentityVerifier { _, _, _, clientId ->
            onClientIdVerified(clientId)
            isIdentityMatch
        },
    )

    private companion object {
        val ACCOUNT_ID = AccountIdFactory.create()
        const val EMAIL_ADDRESS = "user@example.com"
        val OLD_AUTHORIZATION_STATE = AuthorizationState("old authorization")
        val NEW_AUTHORIZATION_STATE = AuthorizationState("new authorization")
        val GRAPH_SERVER_SETTINGS = ServerSettings(
            type = Protocols.GRAPH,
            host = "graph.microsoft.com",
            port = 443,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.XOAUTH2,
            username = EMAIL_ADDRESS,
            password = null,
            clientCertificateAlias = null,
        )
        val GRAPH_CONFIGURATION = OAuthConfiguration(
            clientId = "test-client",
            scopes = listOf("openid", "Mail.ReadWrite"),
            authorizationEndpoint = "https://login.microsoftonline.com/authorize",
            tokenEndpoint = "https://login.microsoftonline.com/token",
            redirectUri = "thunderbird://oauth",
        )
        val ACCOUNT_STATE = AccountState(
            id = ACCOUNT_ID,
            emailAddress = EMAIL_ADDRESS,
            incomingServerSettings = GRAPH_SERVER_SETTINGS,
            authorizationState = OLD_AUTHORIZATION_STATE,
        )
    }
}
