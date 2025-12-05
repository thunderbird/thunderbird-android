package net.thunderbird.feature.account.avatar.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.designsystem.atom.image.RemoteImage
import app.k9mail.core.ui.compose.designsystem.atom.image.rememberPreviewPlaceholder
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons

/**
 * Displays an avatar image from a remote URL.
 *
 * If the image is loading or fails to load, a placeholder icon is shown instead.
 *
 * @param imageUri The URI of the image to display.
 * @param size The size of the avatar.
 * @param modifier The modifier to be applied to the image.
 */
@Composable
internal fun AvatarImage(
    imageUri: String,
    size: AvatarSize,
    modifier: Modifier = Modifier,
) {
    val iconPadding = getIconPadding(size)

    RemoteImage(
        url = imageUri,
        modifier = modifier.fillMaxSize(),
        contentDescription = null,
        placeholder = {
            Placeholder(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(iconPadding),
            )
        },
        previewPlaceholder = rememberPreviewPlaceholder(
            image = Icons.Outlined.Image,
            tint = MainTheme.colors.onSecondary,
            padding = iconPadding,
        ),
    )
}

@Composable
private fun Placeholder(
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = Icons.Outlined.Image,
        contentDescription = null,
        tint = MainTheme.colors.onSecondary,
        modifier = modifier,
    )
}

@Composable
private fun getIconPadding(size: AvatarSize): Dp {
    return when (size) {
        AvatarSize.MEDIUM -> MainTheme.spacings.half
        AvatarSize.LARGE -> MainTheme.spacings.default
    }
}
