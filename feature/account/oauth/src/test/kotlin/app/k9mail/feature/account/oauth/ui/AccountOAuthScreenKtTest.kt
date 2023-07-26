package app.k9mail.feature.account.oauth.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.onNodeWithTextIgnoreCase
import app.k9mail.core.ui.compose.testing.setContent
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.common.ui.WizardNavigationBarState
import app.k9mail.feature.account.oauth.R
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationState
import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Effect
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccountOAuthScreenKtTest : ComposeTest() {

    @Test
    fun `should delegate navigation effects`() = runTest {
        val initialState = State()
        val viewModel = FakeAccountOAuthViewModel(initialState)
        var oAuthResult: OAuthResult? = null
        val authorizationState = AuthorizationState()

        setContent {
            ThunderbirdTheme {
                AccountOAuthScreen(
                    onOAuthResult = { oAuthResult = it },
                    viewModel = viewModel,
                )
            }
        }

        assertThat(oAuthResult).isNull()

        viewModel.effect(Effect.NavigateNext(authorizationState))

        assertThat(oAuthResult).isEqualTo(OAuthResult.Success(authorizationState))

        viewModel.effect(Effect.NavigateBack)

        assertThat(oAuthResult).isEqualTo(OAuthResult.Failure)
    }

    @Test
    fun `should set navigation bar enabled state`() {
        val initialState = State(
            wizardNavigationBarState = WizardNavigationBarState(
                isNextEnabled = true,
                isBackEnabled = true,
            ),
        )
        val viewModel = FakeAccountOAuthViewModel(initialState)

        setContent {
            ThunderbirdTheme {
                AccountOAuthScreen(
                    onOAuthResult = {},
                    viewModel = viewModel,
                )
            }
        }

        onNodeWithTextIgnoreCase(R.string.account_oauth_button_next).assertIsEnabled()
        onNodeWithTextIgnoreCase(R.string.account_oauth_button_back).assertIsEnabled()
    }

    @Test
    fun `should set navigation bar disabled state`() {
        val initialState = State(
            wizardNavigationBarState = WizardNavigationBarState(
                isNextEnabled = false,
                isBackEnabled = false,
            ),
        )
        val viewModel = FakeAccountOAuthViewModel(initialState)

        setContent {
            ThunderbirdTheme {
                AccountOAuthScreen(
                    onOAuthResult = {},
                    viewModel = viewModel,
                )
            }
        }

        onNodeWithTextIgnoreCase(R.string.account_oauth_button_next).assertIsNotEnabled()
        onNodeWithTextIgnoreCase(R.string.account_oauth_button_back).assertIsNotEnabled()
    }
}
