package app.k9mail.feature.account.server.validation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.account.common.ui.item.ErrorItem
import app.k9mail.feature.account.common.ui.item.ListItem
import app.k9mail.feature.account.common.ui.item.LoadingItem
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import app.k9mail.feature.account.oauth.ui.AccountOAuthView
import app.k9mail.feature.account.server.validation.R
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Event
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.State
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId

@Suppress("LongMethod", "ViewModelForwarding")
@Composable
internal fun ServerValidationContent(
    state: State,
    isIncomingValidation: Boolean,
    oAuthViewModel: AccountOAuthContract.ViewModel,
    onEvent: (Event) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources

    ResponsiveWidthContainer(
        modifier = Modifier
            .testTagAsResourceId("AccountValidationContent")
            .padding(contentPadding)
            .fillMaxWidth()
            .then(modifier),
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = contentPadding,
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double, Alignment.CenterVertically),
        ) {
            if (state.error != null) {
                item(key = "error") {
                    // TODO add raw error message
                    ErrorItem(
                        title = stringResource(
                            id = if (isIncomingValidation) {
                                R.string.account_server_validation_incoming_loading_error
                            } else {
                                R.string.account_server_validation_outgoing_loading_error
                            },
                        ),
                        message = state.error.toResourceString(resources),
                        onRetry = { onEvent(Event.OnRetryClicked) },
                    )
                }
            } else if (state.isSuccess) {
                item(key = "success") {
                    LoadingItem(
                        message = stringResource(
                            id = if (isIncomingValidation) {
                                R.string.account_server_validation_incoming_success
                            } else {
                                R.string.account_server_validation_outgoing_success
                            },
                        ),
                    )
                }
            } else if (state.needsAuthorization) {
                item(key = "oauth") {
                    ListItem {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            TextTitleMedium(
                                text = stringResource(
                                    id = R.string.account_server_validation_sign_in,
                                ),
                            )
                            Spacer(modifier = Modifier.padding(MainTheme.spacings.default))
                            AccountOAuthView(
                                onOAuthResult = { result -> onEvent(Event.OnOAuthResult(result)) },
                                viewModel = oAuthViewModel,
                            )
                        }
                    }
                }
            } else {
                item(key = "loading") {
                    LoadingItem(
                        message = stringResource(
                            id = if (isIncomingValidation) {
                                R.string.account_server_validation_incoming_loading_message
                            } else {
                                R.string.account_server_validation_outgoing_loading_message
                            },
                        ),
                    )
                }
            }
        }
    }
}
