package app.k9mail.feature.account.setup.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBar
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.setup.R.string

@Composable
internal fun AccountSetupTopAppBar(
    title: String,
) {
    TopAppBar(
        title = title,
        subtitle = stringResource(id = string.account_setup_title),
        titleContentPadding = PaddingValues(
            start = MainTheme.spacings.double,
        ),
    )
}

@DevicePreviews
@Composable
internal fun AccountSetupTopAppBarK9Preview() {
    K9Theme {
        AccountSetupTopAppBar(
            title = "Title",
        )
    }
}

@DevicePreviews
@Composable
internal fun AccountSetupTopAppBarThunderbirdPreview() {
    ThunderbirdTheme {
        AccountSetupTopAppBar(
            title = "Title",
        )
    }
}
