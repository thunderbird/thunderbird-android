package app.k9mail.feature.account.oauth.ui

import android.content.Intent
import app.cash.turbine.testIn
import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationIntentResult
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Effect
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Error
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Event
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.State
import assertk.assertThat
import assertk.assertions.assertThatAndTurbinesConsumed
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountOAuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val testSubject = AccountOAuthViewModel(
        getOAuthRequestIntent = { _, _ ->
            AuthorizationIntentResult.Success(intent = Intent())
        },
    )

    @Test
    fun `should launch OAuth when SignInClicked event received`() = runTest {
        val initialState = State(
            hostname = "example.com",
            emailAddress = "test@example.com",
        )
        val intent = Intent()
        val testSubject = AccountOAuthViewModel(
            getOAuthRequestIntent = { _, _ ->
                AuthorizationIntentResult.Success(intent = intent)
            },
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
            actual = effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.LaunchOAuth(intent))
        }
    }

    @Test
    fun `should show error when SignInClicked event received and OAuth is not supported`() = runTest {
        val initialState = State(
            hostname = "example.com",
            emailAddress = "test@example.com",
        )
        val testSubject = AccountOAuthViewModel(
            getOAuthRequestIntent = { _, _ ->
                AuthorizationIntentResult.NotSupported
            },
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
        val initialState = State(
            hostname = "example.com",
            emailAddress = "test@example.com",
            error = Error.NotSupported,
        )
        val intent = Intent()
        val testSubject = AccountOAuthViewModel(
            getOAuthRequestIntent = { _, _ ->
                AuthorizationIntentResult.Success(intent = intent)
            },
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

        testSubject.event(Event.OnRetryClicked)

        assertThat(stateTurbine.awaitItem()).isEqualTo(
            initialState.copy(error = null)
        )

        assertThatAndTurbinesConsumed(
            actual = effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.LaunchOAuth(intent))
        }
    }

    @Test
    fun `should emit NavigateBack effect when OnBackClicked event received`() = runTest {
        val viewModel = testSubject
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
}
