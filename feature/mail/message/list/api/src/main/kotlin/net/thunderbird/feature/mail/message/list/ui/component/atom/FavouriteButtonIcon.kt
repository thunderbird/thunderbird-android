package net.thunderbird.feature.mail.message.list.ui.component.atom

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import net.thunderbird.components.ui.bolt.atom.button.ButtonIcon
import net.thunderbird.components.ui.bolt.atom.button.ButtonIconDefaults
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.theme.BoltTheme

private val FAVOURITE_ICON_COLOR = Color(color = 0xFFF4C430)

@VisibleForTesting
const val MESSAGE_ITEM_FAVOURITE_ICON_BUTTON_TEST_TAG = "MessageItem_FavouriteButtonIcon"

@Composable
fun FavouriteButtonIcon(
    favourite: Boolean,
    onFavouriteChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = BoltTheme.sizes.icon,
) {
    ButtonIcon(
        onClick = { onFavouriteChange(!favourite) },
        imageVector = if (favourite) Icons.Filled.Star else Icons.Outlined.Star,
        colors = ButtonIconDefaults.buttonIconColors(
            contentColor = if (favourite) FAVOURITE_ICON_COLOR else BoltTheme.colors.onSurface,
        ),
        modifier = modifier
            .size(size)
            .testTag(MESSAGE_ITEM_FAVOURITE_ICON_BUTTON_TEST_TAG),
    )
}
