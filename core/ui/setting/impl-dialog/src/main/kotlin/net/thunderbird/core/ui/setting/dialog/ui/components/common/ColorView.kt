package net.thunderbird.core.ui.setting.dialog.ui.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.theme.MainTheme

@Composable
internal fun ColorView(
    color: Int,
    onClick: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    size: Dp = MainTheme.sizes.icon,
) {
    Surface(
        color = Color(color),
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .let {
                if (onClick != null) {
                    it.clickable(onClick = { onClick(color) })
                } else {
                    it
                }
            },
    ) {
        if (isSelected) {
            Icon(
                tint = MainTheme.colors.onSecondary,
                imageVector = Icons.Outlined.Check,
                modifier = Modifier.padding(MainTheme.spacings.default),
            )
        }
    }
}
