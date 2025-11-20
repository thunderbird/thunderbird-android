package net.thunderbird.core.ui.compose.designsystem.atom.button

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIconDefaults
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons

private val FAVOURITE_ICON_COLOR = Color(color = 0xFFFF8C00)

@Composable
fun FavouriteButtonIcon(
    favourite: Boolean,
    onFavouriteChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = MainTheme.sizes.icon,
) {
    ButtonIcon(
        onClick = { onFavouriteChange(!favourite) },
        imageVector = if (favourite) Icons.Filled.Star else Icons.Outlined.Star,
        colors = ButtonIconDefaults.buttonIconColors(
            contentColor = if (favourite) FAVOURITE_ICON_COLOR else MainTheme.colors.onSurface,
        ),
        modifier = modifier.size(size),
    )
}
