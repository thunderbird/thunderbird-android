package app.k9mail.feature.account.setup.ui.autoconfig

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.Event
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.State
import app.k9mail.feature.account.setup.ui.autoconfig.item.contentItems
import app.k9mail.feature.account.setup.ui.common.item.ErrorItem
import app.k9mail.feature.account.setup.ui.common.item.LoadingItem

@Composable
internal fun AccountAutoConfigContent(
    state: State,
    onEvent: (Event) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ResponsiveWidthContainer(
        modifier = Modifier
            .testTag("AccountAutoConfigContent")
            .padding(contentPadding)
            .fillMaxWidth()
            .then(modifier),
    ) {
        val resources = LocalContext.current.resources

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double, Alignment.CenterVertically),
        ) {
            if (state.isLoading) {
                item(key = "loading") {
                    LoadingItem(
                        message = stringResource(id = R.string.account_setup_auto_config_loading_message),
                    )
                }
            } else if (state.error != null) {
                item(key = "error") {
                    ErrorItem(
                        title = stringResource(id = R.string.account_setup_auto_config_loading_error),
                        message = state.error.toResourceString(resources),
                        onRetry = { onEvent(Event.OnRetryClicked) },
                    )
                }
            } else {
                contentItems(
                    state = state,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@Composable
@DevicePreviews
internal fun AccountAutoConfigContentK9Preview() {
    K9Theme {
        AccountAutoConfigContent(
            state = State(),
            onEvent = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountAutoConfigContentThunderbirdPreview() {
    ThunderbirdTheme {
        AccountAutoConfigContent(
            state = State(),
            onEvent = {},
            contentPadding = PaddingValues(),
        )
    }
}
