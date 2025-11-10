package app.k9mail.core.ui.compose.designsystem.molecule.notification

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.theme2.LocalContentColor
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined.OpenInNew

@Composable
fun NotificationActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isExternalLink: Boolean = false,
) {
    val leadingIcon = remember(isExternalLink) {
        if (isExternalLink) {
            movableContentOf {
                Icon(
                    imageVector = Icons.Outlined.OpenInNew,
                    modifier = Modifier.size(MainTheme.sizes.iconSmall),
                )
                Spacer(modifier = Modifier.width(MainTheme.spacings.default))
            }
        } else {
            null
        }
    }
    ButtonText(
        text = text,
        onClick = onClick,
        leadingIcon = leadingIcon,
        modifier = modifier,
        color = LocalContentColor.current,
    )
}
