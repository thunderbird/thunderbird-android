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
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.setting.SettingValue.IconList.IconOption

@Composable
internal fun IconView(
    icon: IconOption,
    color: Color,
    onClick: ((IconOption) -> Unit)?,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    size: Dp = MainTheme.sizes.icon,
) {
    Surface(
        color = if (isSelected) color else Color.Unspecified,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                enabled = onClick != null,
                onClick = { onClick?.invoke(icon) },
            ),
    ) {
        Icon(
            tint = if (isSelected) MainTheme.colors.onSecondary else MainTheme.colors.onSurface,
            imageVector = icon.icon(),
            modifier = Modifier.size(size).padding(MainTheme.spacings.default),
        )
    }
}
