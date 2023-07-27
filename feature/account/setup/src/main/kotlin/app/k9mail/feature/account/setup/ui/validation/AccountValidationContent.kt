package app.k9mail.feature.account.setup.ui.validation

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
import app.k9mail.core.ui.compose.designsystem.atom.text.TextSubtitle1
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.common.ui.item.ErrorItem
import app.k9mail.feature.account.common.ui.item.LoadingItem
import app.k9mail.feature.account.common.ui.item.SuccessItem
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.Event
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.State

@Suppress("LongMethod")
@Composable
internal fun AccountValidationContent(
    state: State,
    onEvent: (Event) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources

    ResponsiveWidthContainer(
        modifier = Modifier
            .testTag("AccountIncomingConfigContent")
            .padding(contentPadding)
            .fillMaxWidth()
            .then(modifier),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = if (state.isLoading || state.error != null || state.isSuccess) {
                Arrangement.spacedBy(MainTheme.spacings.double, Alignment.CenterVertically)
            } else {
                Arrangement.spacedBy(MainTheme.spacings.default)
            },
        ) {
            if (state.isLoading) {
                item(key = "loading") {
                    LoadingItem(
                        message = stringResource(id = R.string.account_setup_incoming_config_loading_message),
                    )
                }
            } else if (state.error != null) {
                item(key = "error") {
                    // TODO add raw error message
                    ErrorItem(
                        title = stringResource(id = R.string.account_setup_incoming_config_loading_error),
                        message = state.error.toResourceString(resources),
                        onRetry = { onEvent(Event.OnRetryClicked) },
                    )
                }
            } else if (state.isSuccess) {
                item(key = "success") {
                    SuccessItem(
                        message = stringResource(id = R.string.account_setup_incoming_config_success),
                    )
                }
            } else {
                item {
                    TextSubtitle1(text = "Should not happen")
                }
            }
        }
    }
}

@Composable
@DevicePreviews
internal fun AccountIncomingConfigContentK9Preview() {
    K9Theme {
        AccountValidationContent(
            onEvent = { },
            state = State(),
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountIncomingConfigContentThunderbirdPreview() {
    ThunderbirdTheme {
        AccountValidationContent(
            onEvent = { },
            state = State(),
            contentPadding = PaddingValues(),
        )
    }
}
