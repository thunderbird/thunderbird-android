package net.thunderbird.components.ui.bolt.molecule.notification

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.theme.LocalContentColor
import net.thunderbird.components.ui.bolt.theme.MainTheme

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
