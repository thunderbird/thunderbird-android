package app.k9mail.feature.account.server.validation.ui

import app.k9mail.core.ui.compose.testing.mvi.assertThatAndMviTurbinesConsumed
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.oauth.domain.AccountOAuthDomainContract
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import app.k9mail.feature.account.oauth.ui.fake.FakeAccountOAuthViewModel
import app.k9mail.feature.account.server.certificate.data.InMemoryServerCertificateErrorRepository
import app.k9mail.feature.account.server.certificate.domain.ServerCertificateDomainContract
import app.k9mail.feature.account.server.validation.domain.ServerValidationDomainContract
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Effect
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
import net.thunderbird.core.testing.coroutines.MainDispatcherRule
import org.junit.Rule
import org.junit.Test

abstract class BaseServerValidationViewModelTest<T : BaseServerValidationViewModel> {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should update state when LoadAccountStateAndValidate event received and validate`() = runMviTest {
        val accountState = if (isIncomingValidation) {
            AccountState(
                incomingServerSettings = SERVER_SETTINGS,
            )
        } else {
            AccountState(
                outgoingServerSettings = SERVER_SETTINGS,
            )
        }
        val initialState = State(
            serverSettings = null,
            isLoading = true,
            error = Error.ServerError("server error"),
            isSuccess = true,
        )
        val testSubject = createTestSubject(
            accountState = accountState,
            initialState = initialState,
        )

        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        val expectedState = State(
            serverSettings = SERVER_SETTINGS,
            isLoading = false,
            error = null,
            isSuccess = false,
        )

        testSubject.event(Event.LoadAccountStateAndValidate)

        assertThat(turbines.awaitStateItem()).isEqualTo(expectedState)

        val loadingState = expectedState.copy(isLoading = true)

        assertThat(turbines.awaitStateItem()).isEqualTo(loadingState)

        val successState = loadingState.copy(isLoading = false, isSuccess = true)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(successState)
        }
    }

    @Test
    fun `should fail when ValidateServerSettings event received and server settings null`() = runMviTest {
        val initialState = State()
        val testSubject = createTestSubject()
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.ValidateServerSettings)

        val errorState = initialState.copy(
            error = Error.UnknownError("Server settings not set"),
        )
        assertThatAndMviTurbinesConsumed(
            actual = turbines.stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(errorState)
        }
    }

    @Test
    fun `should validate server settings when ValidateServerSettings event received`() = runMviTest {
        val initialState = State(
            serverSettings = SERVER_SETTINGS,
        )
        val testSubject = createTestSubject(
            serverSettingsValidationResult = ServerSettingsValidationResult.Success,
            initialState = initialState,
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.ValidateServerSettings)

        val loadingState = initialState.copy(isLoading = true)
        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(loadingState)

        val successState = loadingState.copy(
            isLoading = false,
            isSuccess = true,
        )
        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(successState)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateNext)
        }
    }

    @Test
    fun `should set error state when ValidateServerSettings received and check settings failed`() = runMviTest {
        val initialState = State(
            serverSettings = SERVER_SETTINGS,
        )
        val testSubject = createTestSubject(
            serverSettingsValidationResult = ServerSettingsValidationResult.ServerError("server error"),
            initialState = initialState,
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.ValidateServerSettings)

        val loadingState = initialState.copy(isLoading = true)
        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(loadingState)

        val failureState = loadingState.copy(
            isLoading = false,
            error = Error.ServerError("server error"),
        )
        assertThatAndMviTurbinesConsumed(
            actual = turbines.stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(failureState)
        }
    }

    @Test
    fun `should emit effect NavigateNext when ValidateConfig is successful`() = runMviTest {
        val initialState = State(
            serverSettings = SERVER_SETTINGS,
            isSuccess = true,
        )
        val testSubject = createTestSubject(
            initialState = initialState,
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.ValidateServerSettings)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateNext)
        }
    }

    @Test
    fun `should emit NavigateBack effect when OnBackClicked event received`() = runMviTest {
        val testSubject = createTestSubject()
        val turbines = turbinesWithInitialStateCheck(testSubject, State())

        testSubject.event(Event.OnBackClicked)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateBack)
        }
    }

    @Test
    fun `should clear error and trigger check settings when OnRetryClicked event received`() = runMviTest {
        val initialState = State(
            serverSettings = SERVER_SETTINGS,
            error = Error.ServerError("server error"),
        )
        var checkSettingsCalled = false

        val testSubject = createTestSubject(
            validateServerSettings = {
                delay(50)
                checkSettingsCalled = true
                ServerSettingsValidationResult.Success
            },
            accountStateRepository = InMemoryAccountStateRepository(),
            authorizationStateRepository = { true },
            certificateErrorRepository = InMemoryServerCertificateErrorRepository(),
            oAuthViewModel = FakeAccountOAuthViewModel(),
            initialState = initialState,
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.OnRetryClicked)

        val stateWithoutError = initialState.copy(error = null)
        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(stateWithoutError)

        val loadingState = stateWithoutError.copy(isLoading = true)
        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(loadingState)

        val successState = loadingState.copy(isLoading = false, isSuccess = true)
        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(successState)
        assertThat(checkSettingsCalled).isTrue()

        assertThatAndMviTurbinesConsumed(
            actual = turbines.effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateNext)
        }
    }

    abstract fun createTestSubject(
        serverSettingsValidationResult: ServerSettingsValidationResult = ServerSettingsValidationResult.Success,
        accountState: AccountState = AccountState(),
        initialState: State = State(),
    ): T

    abstract fun createTestSubject(
        accountStateRepository: AccountDomainContract.AccountStateRepository,
        validateServerSettings: ServerValidationDomainContract.UseCase.ValidateServerSettings,
        authorizationStateRepository: AccountOAuthDomainContract.AuthorizationStateRepository,
        certificateErrorRepository: ServerCertificateDomainContract.ServerCertificateErrorRepository,
        oAuthViewModel: AccountOAuthContract.ViewModel,
        initialState: State,
    ): T

    abstract val isIncomingValidation: Boolean

    protected companion object {
        val SERVER_SETTINGS = ServerSettings(
            type = "imap",
            host = "imap.example.com",
            port = 465,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "username",
            password = "password",
            clientCertificateAlias = null,
        )
    }
}
