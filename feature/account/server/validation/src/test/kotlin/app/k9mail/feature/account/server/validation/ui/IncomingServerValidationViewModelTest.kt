package app.k9mail.feature.account.server.validation.ui

import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.AccountDisplayOptions
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.oauth.domain.AccountOAuthDomainContract
import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import app.k9mail.feature.account.oauth.ui.fake.FakeAccountOAuthViewModel
import app.k9mail.feature.account.server.certificate.data.InMemoryServerCertificateErrorRepository
import app.k9mail.feature.account.server.certificate.domain.ServerCertificateDomainContract
import app.k9mail.feature.account.server.validation.domain.OAuthAccountIdentity
import app.k9mail.feature.account.server.validation.domain.OAuthAccountIdentityResolver
import app.k9mail.feature.account.server.validation.domain.ServerValidationDomainContract
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Error
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Event
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import kotlinx.coroutines.delay
import org.junit.Test

class IncomingServerValidationViewModelTest : BaseServerValidationViewModelTest<IncomingServerValidationViewModel>() {

    @Test
    fun `should set isIncoming to true`() {
        val testSubject = createTestSubject(
            serverSettingsValidationResult = ServerSettingsValidationResult.Success,
            accountState = AccountState(
                incomingServerSettings = SERVER_SETTINGS,
            ),
            initialState = State(
                serverSettings = null,
                isLoading = true,
                error = Error.ServerError("server error"),
                isSuccess = true,
            ),
        )

        assertThat(testSubject.isIncomingValidation).isTrue()
    }

    @Test
    fun `Graph OAuth success persists authorization state before validating Graph settings`() = runMviTest {
        val emailAddress = "user@example.com"
        val graphSettings = ServerSettings(
            type = "graph",
            host = "graph.microsoft.com",
            port = 443,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.XOAUTH2,
            username = emailAddress,
            password = null,
            clientCertificateAlias = null,
        )
        val accountStateRepository = InMemoryAccountStateRepository(
            state = AccountState(
                emailAddress = emailAddress,
                incomingServerSettings = graphSettings,
            ),
        )
        var validatedServerType: String? = null
        val testSubject = createTestSubject(
            accountStateRepository = accountStateRepository,
            validateServerSettings = { settings ->
                validatedServerType = settings.type
                ServerSettingsValidationResult.Success
            },
            authorizationStateRepository = { true },
            certificateErrorRepository = InMemoryServerCertificateErrorRepository(),
            oAuthViewModel = FakeAccountOAuthViewModel(),
            initialState = State(
                emailAddress = emailAddress,
                serverSettings = graphSettings,
                needsAuthorization = true,
            ),
        )

        testSubject.event(Event.OnOAuthResult(OAuthResult.Success(AuthorizationState("graph-authorization"))))

        assertThat(accountStateRepository.getState().authorizationState)
            .isEqualTo(AuthorizationState("graph-authorization"))
        assertThat(validatedServerType).isEqualTo("graph")
        assertThat(testSubject.state.value.isSuccess).isTrue()
    }

    @Test
    fun `OAuth profile identity fills the email and sender display name before validation`() = runMviTest {
        val graphSettings = ServerSettings(
            type = "graph",
            host = "graph.microsoft.com",
            port = 443,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.XOAUTH2,
            username = "searched@example.com",
            password = null,
            clientCertificateAlias = null,
        )
        val accountStateRepository = InMemoryAccountStateRepository(
            state = AccountState(
                emailAddress = "searched@example.com",
                incomingServerSettings = graphSettings,
            ),
        )
        var validatedUsername: String? = null
        val identityResolver = OAuthAccountIdentityResolver { _, _ ->
            OAuthAccountIdentity(
                emailAddress = "provider@example.com",
                displayName = "Provider Sender",
            )
        }
        val testSubject = IncomingServerValidationViewModel(
            accountStateRepository = accountStateRepository,
            validateServerSettings = { settings ->
                validatedUsername = settings.username
                ServerSettingsValidationResult.Success
            },
            authorizationStateRepository = { true },
            certificateErrorRepository = InMemoryServerCertificateErrorRepository(),
            oAuthViewModel = FakeAccountOAuthViewModel(),
            initialState = State(
                emailAddress = "searched@example.com",
                serverSettings = graphSettings,
                needsAuthorization = true,
            ),
            accountIdentityResolver = identityResolver,
        )

        testSubject.event(Event.OnOAuthResult(OAuthResult.Success(AuthorizationState("profile-test"))))
        delay(50)

        assertThat(accountStateRepository.getState().emailAddress).isEqualTo("provider@example.com")
        assertThat(accountStateRepository.getState().incomingServerSettings?.username)
            .isEqualTo("provider@example.com")
        assertThat(accountStateRepository.getState().displayOptions).isEqualTo(
            AccountDisplayOptions(
                accountName = "provider@example.com",
                displayName = "Provider Sender",
                emailSignature = null,
            ),
        )
        assertThat(validatedUsername).isEqualTo("provider@example.com")
    }

    override fun createTestSubject(
        serverSettingsValidationResult: ServerSettingsValidationResult,
        accountState: AccountState,
        initialState: State,
    ): IncomingServerValidationViewModel {
        return IncomingServerValidationViewModel(
            validateServerSettings = {
                delay(50)
                serverSettingsValidationResult
            },
            accountStateRepository = InMemoryAccountStateRepository(
                state = accountState,
            ),
            authorizationStateRepository = { true },
            certificateErrorRepository = InMemoryServerCertificateErrorRepository(),
            oAuthViewModel = FakeAccountOAuthViewModel(),
            initialState = initialState,
        )
    }

    override fun createTestSubject(
        accountStateRepository: AccountDomainContract.AccountStateRepository,
        validateServerSettings: ServerValidationDomainContract.UseCase.ValidateServerSettings,
        authorizationStateRepository: AccountOAuthDomainContract.AuthorizationStateRepository,
        certificateErrorRepository: ServerCertificateDomainContract.ServerCertificateErrorRepository,
        oAuthViewModel: AccountOAuthContract.ViewModel,
        initialState: State,
    ) = IncomingServerValidationViewModel(
        accountStateRepository = accountStateRepository,
        validateServerSettings = validateServerSettings,
        authorizationStateRepository = authorizationStateRepository,
        certificateErrorRepository = certificateErrorRepository,
        oAuthViewModel = oAuthViewModel,
        initialState = initialState,
    )

    override val isIncomingValidation: Boolean = true
}
