package app.k9mail.feature.account.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBar
import app.k9mail.core.ui.compose.theme.PreviewWithThemes

/**
 * Top app bar for the account screens.
 */
@Composable
fun AccountTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = title,
        modifier = modifier,
    )
}

@PreviewDevices
@Composable
internal fun AccountTopAppBarPreview() {
    PreviewWithThemes {
        AccountTopAppBar(
            title = "Title",
        )
    }
}
