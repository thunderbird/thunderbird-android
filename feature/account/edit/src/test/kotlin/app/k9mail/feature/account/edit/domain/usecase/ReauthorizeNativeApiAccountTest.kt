package app.k9mail.feature.account.edit.domain.usecase

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdaterResult
import app.k9mail.feature.account.edit.domain.ApiAccountReauthorizationResult
import app.k9mail.feature.account.edit.domain.GmailAccountIdentityVerifier
import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.mail.NativeApiMailServerSettings
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.common.mail.ZohoMailServerSettings
import net.thunderbird.core.common.oauth.OAuthConfiguration
import net.thunderbird.core.common.oauth.OAuthConfigurationProvider
import net.thunderbird.feature.account.AccountIdFactory
import org.junit.Test

class ReauthorizeNativeApiAccountTest {

    @Test
    fun `Gmail authorization is updated only when profile identity matches`() = runTest {
        var persistedAuthorization = OLD_AUTHORIZATION_STATE
        var expectedAuthorization: AuthorizationState? = null
        val testSubject = createTestSubject(
            currentState = gmailAccountState(),
            isGmailIdentityMatch = true,
            updateAuthorization = { expected, new ->
                expectedAuthorization = expected
                persistedAuthorization = new
                AccountUpdaterResult.Success(ACCOUNT_ID)
            },
        )

        val result = testSubject.execute(ACCOUNT_ID, OAuthResult.Success(NEW_AUTHORIZATION_STATE))

        assertThat(result).isEqualTo(ApiAccountReauthorizationResult.Success)
        assertThat(expectedAuthorization).isEqualTo(OLD_AUTHORIZATION_STATE)
        assertThat(persistedAuthorization).isEqualTo(NEW_AUTHORIZATION_STATE)
        assertThat(GMAIL_CONFIGURATION.scopes).isEqualTo(listOf(GMAIL_SCOPE))
    }

    @Test
    fun `Gmail identity mismatch leaves current authorization unchanged`() = runTest {
        var updateCalled = false
        val testSubject = createTestSubject(
            currentState = gmailAccountState(),
            isGmailIdentityMatch = false,
            updateAuthorization = { _, _ ->
                updateCalled = true
                AccountUpdaterResult.Success(ACCOUNT_ID)
            },
        )

        val result = testSubject.execute(ACCOUNT_ID, OAuthResult.Success(NEW_AUTHORIZATION_STATE))

        assertThat(result).isEqualTo(ApiAccountReauthorizationResult.GmailAccountMismatch)
        assertThat(updateCalled).isEqualTo(false)
    }

    @Test
    fun `Gmail account Client ID overrides a missing provider default`() = runTest {
        var updateCalled = false
        val accountState = gmailAccountState().copy(
            incomingServerSettings = gmailServerSettings().copy(
                extra = mapOf(NativeApiMailServerSettings.EXTRA_OAUTH_CLIENT_ID to "account-specific-client"),
            ),
        )
        val testSubject = createTestSubject(
            currentState = accountState,
            isGmailIdentityMatch = true,
            updateAuthorization = { _, _ ->
                updateCalled = true
                AccountUpdaterResult.Success(ACCOUNT_ID)
            },
            oauthConfiguration = GMAIL_CONFIGURATION.copy(clientId = ""),
        )

        val result = testSubject.execute(ACCOUNT_ID, OAuthResult.Success(NEW_AUTHORIZATION_STATE))

        assertThat(result).isEqualTo(ApiAccountReauthorizationResult.Success)
        assertThat(updateCalled).isEqualTo(true)
    }

    @Test
    fun `Zoho reauthorization updates tokens without invoking Gmail profile verifier`() = runTest {
        var persistedAuthorization: AuthorizationState? = null
        val testSubject = createTestSubject(
            currentState = zohoAccountState(),
            isGmailIdentityMatch = false,
            updateAuthorization = { _, new ->
                persistedAuthorization = new
                AccountUpdaterResult.Success(ACCOUNT_ID)
            },
        )

        val result = testSubject.execute(ACCOUNT_ID, OAuthResult.Success(NEW_AUTHORIZATION_STATE))

        assertThat(result).isEqualTo(ApiAccountReauthorizationResult.Success)
        assertThat(persistedAuthorization).isEqualTo(NEW_AUTHORIZATION_STATE)
    }

    @Test
    fun `canceled sign in does not update authorization`() = runTest {
        var updateCalled = false
        val testSubject = createTestSubject(
            currentState = gmailAccountState(),
            isGmailIdentityMatch = true,
            updateAuthorization = { _, _ ->
                updateCalled = true
                AccountUpdaterResult.Success(ACCOUNT_ID)
            },
        )

        val result = testSubject.execute(ACCOUNT_ID, OAuthResult.Failure)

        assertThat(result).isEqualTo(ApiAccountReauthorizationResult.Canceled)
        assertThat(updateCalled).isEqualTo(false)
    }

    @Test
    fun `non API account is not updated`() = runTest {
        var updateCalled = false
        val testSubject = createTestSubject(
            currentState = gmailAccountState().copy(
                incomingServerSettings = gmailServerSettings().copy(type = Protocols.IMAP),
            ),
            isGmailIdentityMatch = true,
            updateAuthorization = { _, _ ->
                updateCalled = true
                AccountUpdaterResult.Success(ACCOUNT_ID)
            },
        )

        val result = testSubject.execute(ACCOUNT_ID, OAuthResult.Success(NEW_AUTHORIZATION_STATE))

        assertThat(result).isEqualTo(ApiAccountReauthorizationResult.NotAvailable)
        assertThat(updateCalled).isEqualTo(false)
    }

    private fun createTestSubject(
        currentState: AccountState,
        isGmailIdentityMatch: Boolean,
        updateAuthorization: suspend (AuthorizationState?, AuthorizationState) -> AccountUpdaterResult,
        oauthConfiguration: OAuthConfiguration = GMAIL_CONFIGURATION,
    ) = ReauthorizeNativeApiAccount(
        loadAccountState = { accountId ->
            assertThat(accountId).isEqualTo(ACCOUNT_ID)
            currentState
        },
        accountAuthorizationStateUpdater = { accountId, expected, new ->
            assertThat(accountId).isEqualTo(ACCOUNT_ID)
            updateAuthorization(expected, new)
        },
        oauthConfigurationProvider = OAuthConfigurationProvider { hostname ->
            if (hostname == GMAIL_HOST) oauthConfiguration else null
        },
        gmailIdentityVerifier = GmailAccountIdentityVerifier { email, _ ->
            email == EMAIL_ADDRESS && isGmailIdentityMatch
        },
    )

    private fun gmailAccountState() = AccountState(
        id = ACCOUNT_ID,
        emailAddress = EMAIL_ADDRESS,
        incomingServerSettings = gmailServerSettings(),
        authorizationState = OLD_AUTHORIZATION_STATE,
    )

    private fun gmailServerSettings() = ServerSettings(
        type = Protocols.GMAIL_API,
        host = GMAIL_HOST,
        port = 443,
        connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
        authenticationType = AuthType.XOAUTH2,
        username = EMAIL_ADDRESS,
        password = null,
        clientCertificateAlias = null,
    )

    private fun zohoAccountState() = AccountState(
        id = ACCOUNT_ID,
        emailAddress = EMAIL_ADDRESS,
        incomingServerSettings = ServerSettings(
            type = Protocols.ZOHO_MAIL_API,
            host = "mail.zoho.com",
            port = 443,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.XOAUTH2,
            username = EMAIL_ADDRESS,
            password = "test-secret",
            clientCertificateAlias = null,
            extra = mapOf(
                ZohoMailServerSettings.EXTRA_CLIENT_ID to "zoho-client",
                ZohoMailServerSettings.EXTRA_REDIRECT_URI to "thunderbird://oauth2redirect",
            ),
        ),
        authorizationState = OLD_AUTHORIZATION_STATE,
    )

    private companion object {
        val ACCOUNT_ID = AccountIdFactory.create()
        const val EMAIL_ADDRESS = "user@example.com"
        const val GMAIL_HOST = "imap.gmail.com"
        const val GMAIL_SCOPE = "https://mail.google.com/"
        val OLD_AUTHORIZATION_STATE = AuthorizationState("old authorization")
        val NEW_AUTHORIZATION_STATE = AuthorizationState("new authorization")
        val GMAIL_CONFIGURATION = OAuthConfiguration(
            clientId = "gmail-client",
            scopes = listOf(GMAIL_SCOPE),
            authorizationEndpoint = "https://accounts.google.com/o/oauth2/v2/auth",
            tokenEndpoint = "https://oauth2.googleapis.com/token",
            redirectUri = "thunderbird://oauth2redirect",
        )
    }
}
