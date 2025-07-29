package app.k9mail.core.ui.compose.designsystem.molecule.notification

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.theme2.LocalContentColor

@Composable
fun NotificationActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ButtonText(
        text = text,
        onClick = onClick,
        modifier = modifier,
        color = LocalContentColor.current,
    )
}
