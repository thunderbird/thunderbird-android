package app.k9mail.feature.account.oauth.ui

import android.app.Activity
import android.content.Intent
import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationIntentResult
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationResult
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Effect
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Error
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Event
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountOAuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should change state when google hostname found on initState`() = runMviTest {
        val testSubject = createTestSubject(
            isGoogleSignIn = true,
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, State())

        testSubject.initState(defaultState)

        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(
            defaultState.copy(isGoogleSignIn = true),
        )
    }

    @Test
    fun `should not change state when no google hostname found on initState`() = runMviTest {
        val testSubject = createTestSubject(
            isGoogleSignIn = false,
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, State())

        testSubject.initState(defaultState)

        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(
            defaultState.copy(isGoogleSignIn = false),
        )
    }

    @Test
    fun `should launch OAuth when SignInClicked event received`() = runMviTest {
        val initialState = defaultState
        val testSubject = createTestSubject(initialState = initialState)
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.SignInClicked)

        assertThat(turbines.effectTurbine.awaitItem()).isEqualTo(
            Effect.LaunchOAuth(intent),
        )
    }

    @Test
    fun `should show error when SignInClicked event received and OAuth is not supported`() = runMviTest {
        val initialState = defaultState
        val testSubject = createTestSubject(
            authorizationIntentResult = AuthorizationIntentResult.NotSupported,
            initialState = initialState,
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.SignInClicked)

        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(
            initialState.copy(error = Error.NotSupported),
        )
    }

    @Test
    fun `should remove error and launch OAuth when OnRetryClicked event received`() = runMviTest {
        val initialState = defaultState.copy(
            error = Error.NotSupported,
        )
        val testSubject = createTestSubject(initialState = initialState)
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.OnRetryClicked)

        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(
            initialState.copy(error = null),
        )

        assertThat(turbines.effectTurbine.awaitItem()).isEqualTo(
            Effect.LaunchOAuth(intent),
        )
    }

    @Test
    fun `should finish OAuth sign in when onOAuthResult received with success`() = runMviTest {
        val initialState = defaultState
        val authorizationState = AuthorizationState(value = "state")
        val testSubject = createTestSubject(
            authorizationResult = AuthorizationResult.Success(authorizationState),
            initialState = initialState,
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.OnOAuthResult(resultCode = Activity.RESULT_OK, data = intent))

        val loadingState = initialState.copy(isLoading = true)

        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(loadingState)

        val successState = loadingState.copy(
            isLoading = false,
        )

        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(successState)
        assertThat(turbines.effectTurbine.awaitItem()).isEqualTo(
            Effect.NavigateNext(authorizationState),
        )
    }

    @Test
    fun `should set error state when onOAuthResult received with canceled`() = runMviTest {
        val initialState = defaultState
        val testSubject = createTestSubject(
            authorizationResult = AuthorizationResult.Canceled,
            initialState = initialState,
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.OnOAuthResult(resultCode = Activity.RESULT_CANCELED, data = intent))

        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(
            initialState.copy(
                error = Error.Canceled,
            ),
        )
    }

    @Test
    fun `should finish OAuth sign in when onOAuthResult received with success but authorization result is cancelled`() =
        runMviTest {
            val initialState = defaultState
            val testSubject = createTestSubject(
                authorizationResult = AuthorizationResult.Canceled,
                initialState = initialState,
            )
            val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

            testSubject.event(Event.OnOAuthResult(resultCode = Activity.RESULT_OK, data = intent))

            val loadingState = initialState.copy(isLoading = true)

            assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(loadingState)

            val failureState = loadingState.copy(
                isLoading = false,
                error = Error.Canceled,
            )

            assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(failureState)
        }

    @Test
    fun `should finish OAuth sign in when onOAuthResult received with success but authorization result is failure`() =
        runMviTest {
            val initialState = defaultState
            val failure = Exception("failure")
            val testSubject = createTestSubject(
                authorizationResult = AuthorizationResult.Failure(failure),
                initialState = initialState,
            )
            val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

            testSubject.event(Event.OnOAuthResult(resultCode = Activity.RESULT_OK, data = intent))

            val loadingState = initialState.copy(isLoading = true)

            assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(loadingState)

            val failureState = loadingState.copy(
                isLoading = false,
                error = Error.Unknown(failure),
            )

            assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(failureState)
        }

    @Test
    fun `should emit NavigateBack effect when OnBackClicked event received`() = runMviTest {
        val viewModel = createTestSubject()
        val turbines = turbinesWithInitialStateCheck(viewModel, State())

        viewModel.event(Event.OnBackClicked)

        assertThat(turbines.effectTurbine.awaitItem()).isEqualTo(Effect.NavigateBack)
    }

    private companion object {
        val defaultState = State(
            hostname = "example.com",
            emailAddress = "test@example.com",
        )

        val intent = Intent()

        fun createTestSubject(
            authorizationIntentResult: AuthorizationIntentResult = AuthorizationIntentResult.Success(intent = intent),
            authorizationResult: AuthorizationResult = AuthorizationResult.Success(AuthorizationState()),
            isGoogleSignIn: Boolean = false,
            initialState: State = State(),
        ) = AccountOAuthViewModel(
            getOAuthRequestIntent = { _, _ ->
                authorizationIntentResult
            },
            finishOAuthSignIn = { _ ->
                delay(50)
                authorizationResult
            },
            checkIsGoogleSignIn = { _ ->
                isGoogleSignIn
            },
            initialState = initialState,
        )
    }
}
