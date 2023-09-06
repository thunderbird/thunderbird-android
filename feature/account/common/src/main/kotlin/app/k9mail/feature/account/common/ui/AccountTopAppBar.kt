package app.k9mail.feature.account.common.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBar
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.common.R

@Composable
fun AccountTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = title,
        modifier = modifier,
        subtitle = stringResource(id = R.string.account_common_title),
        titleContentPadding = PaddingValues(
            start = MainTheme.spacings.double,
        ),
    )
}

@DevicePreviews
@Composable
internal fun AccountTopAppBarK9Preview() {
    K9Theme {
        AccountTopAppBar(
            title = "Title",
        )
    }
}

@DevicePreviews
@Composable
internal fun AccountTopAppBarThunderbirdPreview() {
    ThunderbirdTheme {
        AccountTopAppBar(
            title = "Title",
        )
    }
}
