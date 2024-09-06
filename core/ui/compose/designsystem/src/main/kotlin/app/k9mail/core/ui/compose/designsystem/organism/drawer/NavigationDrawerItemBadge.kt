package app.k9mail.core.ui.compose.designsystem.organism.drawer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge

@Composable
fun NavigationDrawerItemBadge(
    label: String,
    modifier: Modifier = Modifier,
) {
    TextLabelLarge(
        text = label,
        modifier = modifier,
    )
}
