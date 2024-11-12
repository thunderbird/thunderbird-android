package app.k9mail.feature.account.setup.ui.createaccount

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.molecule.ContentLoadingErrorView
import app.k9mail.core.ui.compose.designsystem.molecule.ErrorView
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingView
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.feature.account.setup.R

@Composable
internal fun CreateAccountContent(
    state: CreateAccountContract.State,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ResponsiveWidthContainer(
        modifier = Modifier
            .padding(contentPadding)
            .testTag("CreateAccountContent")
            .then(modifier),
    ) {
        ContentLoadingErrorView(
            state = state,
            loading = {
                LoadingView(
                    message = stringResource(R.string.account_setup_create_account_creating),
                )
            },
            error = {
                ErrorView(
                    title = stringResource(R.string.account_setup_create_account_error),
                )
            },
            content = {
                LoadingView(
                    message = stringResource(R.string.account_setup_create_account_created),
                )
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
