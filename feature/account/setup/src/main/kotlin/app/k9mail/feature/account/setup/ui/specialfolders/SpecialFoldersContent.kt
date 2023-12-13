package app.k9mail.feature.account.setup.ui.specialfolders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.atom.Icon
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.text.TextSubtitle1
import app.k9mail.core.ui.compose.designsystem.molecule.ContentLoadingErrorView
import app.k9mail.core.ui.compose.designsystem.molecule.ErrorView
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingView
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme.Icons
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import app.k9mail.feature.account.common.ui.loadingerror.rememberContentLoadingErrorViewState
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.Event
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.State

@Composable
fun SpecialFoldersContent(
    state: State,
    onEvent: (Event) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ResponsiveWidthContainer(
        modifier = Modifier
            .testTag("SpecialFoldersContent")
            .padding(contentPadding)
            .then(modifier),
    ) {
        ContentLoadingErrorView(
            state = rememberContentLoadingErrorViewState(state = state),
            loading = {
                LoadingView(
                    message = stringResource(id = R.string.account_setup_special_folders_loading_message),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            error = {
                ErrorView(
                    title = stringResource(id = R.string.account_setup_special_folders_error_message),
                    message = state.error?.message,
                    onRetry = { onEvent(Event.OnRetryClicked) },
                )
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.isSuccess) {
                SuccessView(
                    message = stringResource(id = R.string.account_setup_special_folders_success_message),
                    onEditClick = { onEvent(Event.OnEditClicked) },
                    modifier = Modifier.fillMaxWidth(),
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

@Composable
fun SuccessView(
    message: String,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MainTheme.spacings.default)
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
    ) {
        TextSubtitle1(text = message)

        Row(
            modifier = Modifier.height(MainTheme.sizes.larger),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.celebration,
                tint = MainTheme.colors.secondary,
                modifier = Modifier.requiredSize(MainTheme.sizes.large),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ButtonText(
                text = stringResource(id = R.string.account_setup_special_folders_edit_folders_button_label),
                onClick = onEditClick,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun SpecialFoldersContentPreview() {
    PreviewWithThemes {
        SpecialFoldersContent(
            state = State(
                isLoading = false,
                error = null,
            ),
            onEvent = {},
            contentPadding = PaddingValues(),
        )
    }
}
