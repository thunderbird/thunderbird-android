package app.k9mail.feature.account.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBar

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
