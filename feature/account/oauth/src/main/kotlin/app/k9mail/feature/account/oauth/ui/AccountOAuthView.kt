package app.k9mail.feature.account.oauth.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Effect
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Event
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.ViewModel

@Composable
fun AccountOAuthView(
    onOAuthResult: (OAuthResult) -> Unit,
    viewModel: ViewModel,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    val oAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.event(Event.OnOAuthResult(it.resultCode, it.data))
    }

    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.NavigateNext -> onOAuthResult(OAuthResult.Success(effect.state))
            is Effect.NavigateBack -> onOAuthResult(OAuthResult.Failure)
            is Effect.LaunchOAuth -> oAuthLauncher.launch(effect.intent)
        }
    }

    AccountOAuthContent(
        state = state.value,
        onEvent = { dispatch(it) },
        modifier = modifier,
        isEnabled = isEnabled,
    )
}
