package net.thunderbird.core.ui.compose.preference.ui.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.theme2.MainTheme

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
