package net.thunderbird.feature.account.avatar.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.feature.account.avatar.AvatarIcon
import net.thunderbird.feature.account.avatar.AvatarIconCatalog
import org.koin.compose.koinInject

/**
 * Displays an avatar icon from the icon catalog.
 *
 * @param iconName The name of the icon to display.
 * @param size The size of the avatar.
 * @param modifier The modifier to be applied to the icon.
 * @param iconCatalog The catalog used to retrieve avatar icons.
 */
@Composable
internal fun AvatarIcon(
    iconName: String,
    size: AvatarSize,
    modifier: Modifier = Modifier,
    iconCatalog: AvatarIconCatalog<AvatarIcon<ImageVector>> = koinInject(),
) {
    val avatarIcon = remember(iconName) {
        iconCatalog.get(iconName)
    }
    val iconSize = getIconSize(size)

    Icon(
        imageVector = avatarIcon.icon,
        contentDescription = null,
        tint = MainTheme.colors.onSecondary,
        modifier = modifier.size(iconSize),
    )
}

@Composable
private fun getIconSize(size: AvatarSize): Dp {
    return when (size) {
        AvatarSize.MEDIUM -> MainTheme.sizes.iconLarge
        AvatarSize.LARGE -> MainTheme.sizes.iconAvatar
    }
}
