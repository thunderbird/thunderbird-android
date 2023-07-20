package app.k9mail.feature.account.oauth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.common.ui.ContentListView
import app.k9mail.feature.account.common.ui.item.ErrorItem
import app.k9mail.feature.account.common.ui.item.LoadingItem
import app.k9mail.feature.account.oauth.R
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Event
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.State
import app.k9mail.feature.account.oauth.ui.item.SignInItem

@Composable
internal fun AccountOAuthContent(
    state: State,
    onEvent: (Event) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources

    ContentListView(
        modifier = Modifier
            .testTag("AccountOAuthContent")
            .then(modifier),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double, Alignment.CenterVertically),
    ) {
        if (state.isLoading) {
            item(key = "loading") {
                LoadingItem(
                    message = stringResource(id = R.string.account_oauth_loading_message),
                )
            }
        } else if (state.error != null) {
            item(key = "error") {
                ErrorItem(
                    title = stringResource(id = R.string.account_oauth_loading_error),
                    message = state.error.toResourceString(resources),
                    onRetry = { onEvent(Event.OnRetryClicked) },
                )
            }
        } else {
            item(key = "sign_in") {
                SignInItem(
                    emailAddress = state.emailAddress,
                    onSignInClick = { onEvent(Event.SignInClicked) },
                    isGoogleSignIn = state.isGoogleSignIn,
                )
            }
        }
    }
}

@Composable
@DevicePreviews
internal fun AccountOAuthContentK9Preview() {
    K9Theme {
        AccountOAuthContent(
            state = State(),
            onEvent = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountOAuthContentThunderbirdPreview() {
    ThunderbirdTheme {
        AccountOAuthContent(
            state = State(),
            onEvent = {},
            contentPadding = PaddingValues(),
        )
    }
}
