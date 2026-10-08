package app.k9mail.feature.account.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.organism.TopAppBar

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
