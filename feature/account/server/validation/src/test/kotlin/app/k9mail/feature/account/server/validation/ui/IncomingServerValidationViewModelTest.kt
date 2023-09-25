package app.k9mail.feature.account.server.validation.ui

import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.oauth.domain.AccountOAuthDomainContract
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import app.k9mail.feature.account.oauth.ui.fake.FakeAccountOAuthViewModel
import app.k9mail.feature.account.server.certificate.data.InMemoryServerCertificateErrorRepository
import app.k9mail.feature.account.server.certificate.domain.ServerCertificateDomainContract
import app.k9mail.feature.account.server.validation.domain.ServerValidationDomainContract
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Error
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.State
import assertk.assertThat
import assertk.assertions.isTrue
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
