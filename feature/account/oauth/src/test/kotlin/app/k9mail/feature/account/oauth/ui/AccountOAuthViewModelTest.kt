package app.k9mail.feature.account.oauth.ui

import android.app.Activity
import android.content.Intent
import app.cash.turbine.testIn
import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationIntentResult
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationResult
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Effect
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Error
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Event
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.State
import assertk.assertThat
import assertk.assertions.assertThatAndTurbinesConsumed
import assertk.assertions.isEqualTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountOAuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should change state when google hostname found on initState`() = runTest {
        val testSubject = createTestSubject(
            isGoogleSignIn = true,
        )

        val stateTurbine = testSubject.state.testIn(backgroundScope)
        val effectTurbine = testSubject.effect.testIn(backgroundScope)
        val turbines = listOf(stateTurbine, effectTurbine)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(State())
        }

        testSubject.initState(defaultState)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(defaultState.copy(isGoogleSignIn = true))
        }
    }

    @Test
    fun `should not change state when no google hostname found on initState`() = runTest {
        val testSubject = createTestSubject(
            isGoogleSignIn = false,
        )

        val stateTurbine = testSubject.state.testIn(backgroundScope)
        val effectTurbine = testSubject.effect.testIn(backgroundScope)
        val turbines = listOf(stateTurbine, effectTurbine)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(State())
        }

        testSubject.initState(defaultState)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(defaultState.copy(isGoogleSignIn = false))
        }
    }

    @Test
    fun `should launch OAuth when SignInClicked event received`() = runTest {
        val initialState = defaultState
        val testSubject = createTestSubject(initialState = initialState)
        val stateTurbine = testSubject.state.testIn(backgroundScope)
        val effectTurbine = testSubject.effect.testIn(backgroundScope)
        val turbines = listOf(stateTurbine, effectTurbine)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(initialState)
        }

        testSubject.event(Event.SignInClicked)

        assertThatAndTurbinesConsumed(
            actual = effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.LaunchOAuth(intent))
        }
    }

    @Test
    fun `should show error when SignInClicked event received and OAuth is not supported`() = runTest {
        val initialState = defaultState
        val testSubject = createTestSubject(
            authorizationIntentResult = AuthorizationIntentResult.NotSupported,
            initialState = initialState,
        )

        val stateTurbine = testSubject.state.testIn(backgroundScope)
        val effectTurbine = testSubject.effect.testIn(backgroundScope)
        val turbines = listOf(stateTurbine, effectTurbine)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(initialState)
        }

        testSubject.event(Event.SignInClicked)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(initialState.copy(error = Error.NotSupported))
        }
    }

    @Test
    fun `should remove error and launch OAuth when OnRetryClicked event received`() = runTest {
        val initialState = defaultState.copy(
            error = Error.NotSupported,
        )
        val testSubject = createTestSubject(initialState = initialState)
        val stateTurbine = testSubject.state.testIn(backgroundScope)
        val effectTurbine = testSubject.effect.testIn(backgroundScope)
        val turbines = listOf(stateTurbine, effectTurbine)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(initialState)
        }

        testSubject.event(Event.OnRetryClicked)

        assertThat(stateTurbine.awaitItem()).isEqualTo(
            initialState.copy(error = null),
        )

        assertThatAndTurbinesConsumed(
            actual = effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.LaunchOAuth(intent))
        }
    }

    @Test
    fun `should finish OAuth sign in when onOAuthResult received with success`() = runTest {
        val initialState = defaultState
        val authorizationState = AuthorizationState(value = "state")
        val testSubject = createTestSubject(
            authorizationResult = AuthorizationResult.Success(authorizationState),
            initialState = initialState,
        )
        val stateTurbine = testSubject.state.testIn(backgroundScope)
        val effectTurbine = testSubject.effect.testIn(backgroundScope)
        val turbines = listOf(stateTurbine, effectTurbine)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(initialState)
        }

        testSubject.event(Event.OnOAuthResult(resultCode = Activity.RESULT_OK, data = intent))

        val loadingState = initialState.copy(isLoading = true)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(loadingState)
        }

        val successState = loadingState.copy(
            isLoading = false,
        )

        assertThat(stateTurbine.awaitItem()).isEqualTo(successState)

        assertThatAndTurbinesConsumed(
            actual = effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateNext(authorizationState))
        }
    }

    @Test
    fun `should set error state when onOAuthResult received with canceled`() = runTest {
        val initialState = defaultState
        val testSubject = createTestSubject(
            authorizationResult = AuthorizationResult.Canceled,
            initialState = initialState,
        )
        val stateTurbine = testSubject.state.testIn(backgroundScope)
        val effectTurbine = testSubject.effect.testIn(backgroundScope)
        val turbines = listOf(stateTurbine, effectTurbine)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(initialState)
        }

        testSubject.event(Event.OnOAuthResult(resultCode = Activity.RESULT_CANCELED, data = intent))

        val failureState = initialState.copy(
            error = Error.Canceled,
        )

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(failureState)
        }
    }

    @Test
    fun `should finish OAuth sign in when onOAuthResult received with success but authorization result is cancelled`() =
        runTest {
            val initialState = defaultState
            val testSubject = createTestSubject(
                authorizationResult = AuthorizationResult.Canceled,
                initialState = initialState,
            )
            val stateTurbine = testSubject.state.testIn(backgroundScope)
            val effectTurbine = testSubject.effect.testIn(backgroundScope)
            val turbines = listOf(stateTurbine, effectTurbine)

            assertThatAndTurbinesConsumed(
                actual = stateTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(initialState)
            }

            testSubject.event(Event.OnOAuthResult(resultCode = Activity.RESULT_OK, data = intent))

            val loadingState = initialState.copy(isLoading = true)

            assertThatAndTurbinesConsumed(
                actual = stateTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(loadingState)
            }

            val failureState = loadingState.copy(
                isLoading = false,
                error = Error.Canceled,
            )

            assertThatAndTurbinesConsumed(
                actual = stateTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(failureState)
            }
        }

    @Test
    fun `should finish OAuth sign in when onOAuthResult received with success but authorization result is failure`() =
        runTest {
            val initialState = defaultState
            val failure = Exception("failure")
            val testSubject = createTestSubject(
                authorizationResult = AuthorizationResult.Failure(failure),
                initialState = initialState,
            )
            val stateTurbine = testSubject.state.testIn(backgroundScope)
            val effectTurbine = testSubject.effect.testIn(backgroundScope)
            val turbines = listOf(stateTurbine, effectTurbine)

            assertThatAndTurbinesConsumed(
                actual = stateTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(initialState)
            }

            testSubject.event(Event.OnOAuthResult(resultCode = Activity.RESULT_OK, data = intent))

            val loadingState = initialState.copy(isLoading = true)

            assertThatAndTurbinesConsumed(
                actual = stateTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(loadingState)
            }

            val failureState = loadingState.copy(
                isLoading = false,
                error = Error.Unknown(failure),
            )

            assertThatAndTurbinesConsumed(
                actual = stateTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(failureState)
            }
        }

    @Test
    fun `should emit NavigateBack effect when OnBackClicked event received`() = runTest {
        val viewModel = createTestSubject()
        val stateTurbine = viewModel.state.testIn(backgroundScope)
        val effectTurbine = viewModel.effect.testIn(backgroundScope)
        val turbines = listOf(stateTurbine, effectTurbine)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(State())
        }

        viewModel.event(Event.OnBackClicked)

        assertThatAndTurbinesConsumed(
            actual = effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateBack)
        }
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
