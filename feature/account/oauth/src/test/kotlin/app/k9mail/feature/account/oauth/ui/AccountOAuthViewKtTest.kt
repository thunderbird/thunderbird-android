package app.k9mail.feature.account.oauth.ui

import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Effect
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccountOAuthViewKtTest : ComposeTest() {

    @Test
    fun `should delegate navigation effects`() = runTest {
        val initialState = State()
        val viewModel = FakeAccountOAuthViewModel(initialState)
        var oAuthResult: OAuthResult? = null
        val authorizationState = AuthorizationState()

        setContentWithTheme {
            AccountOAuthView(
                onOAuthResult = { oAuthResult = it },
                viewModel = viewModel,
            )
        }

        assertThat(oAuthResult).isNull()

        viewModel.effect(Effect.NavigateNext(authorizationState))

        assertThat(oAuthResult).isEqualTo(OAuthResult.Success(authorizationState))

        viewModel.effect(Effect.NavigateBack)

        assertThat(oAuthResult).isEqualTo(OAuthResult.Failure)
    }
}
