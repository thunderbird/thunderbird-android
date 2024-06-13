package app.k9mail.feature.account.setup.ui.specialfolders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.molecule.ContentLoadingErrorView
import app.k9mail.core.ui.compose.designsystem.molecule.ErrorView
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingView
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import app.k9mail.feature.account.common.ui.loadingerror.rememberContentLoadingErrorViewState
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.Event
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.State
import app.k9mail.feature.account.common.R as CommonR

@Composable
fun SpecialFoldersContent(
    state: State,
    onEvent: (Event) -> Unit,
    contentPadding: PaddingValues,
    appName: String,
    modifier: Modifier = Modifier,
) {
    ResponsiveWidthContainer(
        modifier = Modifier
            .testTag("SpecialFoldersContent")
            .padding(contentPadding)
            .then(modifier),
    ) {
        Column {
            AppTitleTopHeader(
                title = appName,
            )

            ContentLoadingErrorView(
                state = rememberContentLoadingErrorViewState(state = state),
                loading = {
                    LoadingView(
                        message = stringResource(id = R.string.account_setup_special_folders_loading_message),
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                error = {
                    SpecialFoldersErrorView(
                        failure = state.error!!,
                        onRetry = { onEvent(Event.OnRetryClicked) },
                    )
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.isSuccess) {
                    LoadingView(
                        message = stringResource(id = R.string.account_setup_special_folders_success_message),
                        modifier = Modifier.padding(horizontal = MainTheme.spacings.double),
                    )
                } else {
                    SpecialFoldersFormContent(
                        state = state.formState,
                        onEvent = onEvent,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SpecialFoldersErrorView(
    failure: SpecialFoldersContract.Failure,
    onRetry: () -> Unit,
) {
    val message = when (failure) {
        is SpecialFoldersContract.Failure.LoadFoldersFailed -> {
            failure.messageFromServer?.let { messageFromServer ->
                stringResource(id = CommonR.string.account_common_error_server_message, messageFromServer)
            }
        }
    }

    ErrorView(
        title = stringResource(id = R.string.account_setup_special_folders_error_message),
        message = message,
        onRetry = onRetry,
        modifier = Modifier
            .fillMaxWidth()
            .padding(MainTheme.spacings.double),
    )
}
