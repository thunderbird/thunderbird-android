package app.k9mail.feature.account.setup.ui.validation

import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.core.ui.compose.testing.mvi.assertThatAndMviTurbinesConsumed
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import app.k9mail.feature.account.setup.data.InMemoryAccountSetupStateRepository
import app.k9mail.feature.account.setup.data.InMemoryCertificateErrorRepository
import app.k9mail.feature.account.setup.domain.entity.AccountSetupState
import app.k9mail.feature.account.setup.ui.FakeAccountOAuthViewModel
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.Effect
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.Error
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.Event
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class AccountValidationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should update state when LoadAccountSetupStateAndValidate event received and validate`() = runTest {
        val accountSetupState = AccountSetupState(
            incomingServerSettings = IMAP_SERVER_SETTINGS,
        )
        val initialState = State(
            serverSettings = null,
            isLoading = true,
            error = Error.ServerError("server error"),
            isSuccess = true,
        )
        val testSubject = createTestSubject(
            accountSetupState = accountSetupState,
            initialState = initialState,
        )

        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        val expectedState = State(
            serverSettings = IMAP_SERVER_SETTINGS,
            isLoading = false,
            error = null,
            isSuccess = false,
        )

        testSubject.event(Event.LoadAccountSetupStateAndValidate)

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
    fun `should fail when ValidateServerSettings event received and server settings null`() = runTest {
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
    fun `should validate server settings when ValidateServerSettings event received`() = runTest {
        val initialState = State(
            serverSettings = IMAP_SERVER_SETTINGS,
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
    fun `should set error state when ValidateServerSettings received and check settings failed`() = runTest {
        val initialState = State(
            serverSettings = IMAP_SERVER_SETTINGS,
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
    fun `should emit effect NavigateNext when ValidateConfig is successful`() = runTest {
        val initialState = State(
            serverSettings = IMAP_SERVER_SETTINGS,
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
    fun `should emit NavigateBack effect when OnBackClicked event received`() = runTest {
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
    fun `should clear error and trigger check settings when OnRetryClicked event received`() = runTest {
        val initialState = State(
            serverSettings = IMAP_SERVER_SETTINGS,
            error = Error.ServerError("server error"),
        )
        var checkSettingsCalled = false
        val testSubject = AccountValidationViewModel(
            validateServerSettings = {
                delay(50)
                checkSettingsCalled = true
                ServerSettingsValidationResult.Success
            },
            accountSetupStateRepository = InMemoryAccountSetupStateRepository(),
            authorizationStateRepository = { true },
            certificateErrorRepository = InMemoryCertificateErrorRepository(),
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

    private companion object {
        fun createTestSubject(
            serverSettingsValidationResult: ServerSettingsValidationResult = ServerSettingsValidationResult.Success,
            accountSetupState: AccountSetupState = AccountSetupState(),
            initialState: State = State(),
        ): AccountValidationViewModel {
            return AccountValidationViewModel(
                validateServerSettings = {
                    delay(50)
                    serverSettingsValidationResult
                },
                accountSetupStateRepository = InMemoryAccountSetupStateRepository(accountSetupState),
                authorizationStateRepository = { true },
                certificateErrorRepository = InMemoryCertificateErrorRepository(),
                oAuthViewModel = FakeAccountOAuthViewModel(),
                initialState = initialState,
            )
        }

        val IMAP_SERVER_SETTINGS = ServerSettings(
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
