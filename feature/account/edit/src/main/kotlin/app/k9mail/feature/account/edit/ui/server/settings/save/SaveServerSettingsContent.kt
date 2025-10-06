package app.k9mail.feature.account.edit.ui.server.settings.save

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.molecule.ContentLoadingErrorView
import app.k9mail.core.ui.compose.designsystem.molecule.ErrorView
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingView
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.feature.account.edit.R
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId

@Composable
fun SaveServerSettingsContent(
    state: SaveServerSettingsContract.State,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ResponsiveWidthContainer(
        modifier = Modifier
            .testTagAsResourceId("SaveServerSettingsContent")
            .padding(contentPadding)
            .then(modifier),
    ) { contentPadding ->
        ContentLoadingErrorView(
            state = state,
            loading = {
                LoadingView(
                    message = stringResource(id = R.string.account_edit_save_server_settings_loading_message),
                )
            },
            error = {
                ErrorView(
                    title = stringResource(id = R.string.account_edit_save_server_settings_error_message),
                )
            },
            content = {
                LoadingView(
                    message = stringResource(id = R.string.account_edit_save_server_settings_success_message),
                )
            },
            modifier = Modifier.fillMaxSize().padding(contentPadding),
        )
    }
}
