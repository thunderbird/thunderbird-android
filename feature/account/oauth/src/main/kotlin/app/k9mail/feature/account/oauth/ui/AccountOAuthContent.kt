package app.k9mail.feature.account.oauth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.k9mail.feature.account.oauth.R
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Event
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.State
import app.k9mail.feature.account.oauth.ui.view.GoogleSignInSupportText
import app.k9mail.feature.account.oauth.ui.view.SignInView
import net.thunderbird.components.ui.bolt.molecule.ErrorView
import net.thunderbird.components.ui.bolt.molecule.LoadingView
import net.thunderbird.components.ui.bolt.theme.BoltTheme

@Composable
internal fun AccountOAuthContent(
    state: State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    val resources = LocalResources.current

    Column(
        modifier = Modifier.testTag("AccountOAuthContent")
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.double, Alignment.CenterVertically),
    ) {
        if (state.isLoading) {
            LoadingView(
                message = stringResource(id = R.string.account_oauth_loading_message),
            )
        } else if (state.error != null) {
            Column {
                ErrorView(
                    title = stringResource(id = R.string.account_oauth_loading_error),
                    message = state.error.toResourceString(resources),
                    onRetry = { onEvent(Event.OnRetryClicked) },
                )

                if (state.isGoogleSignIn) {
                    GoogleSignInSupportText()
                }
            }
        } else {
            SignInView(
                onSignInClick = { onEvent(Event.SignInClicked) },
                isGoogleSignIn = state.isGoogleSignIn,
                isEnabled = isEnabled,
            )
        }
    }
}
