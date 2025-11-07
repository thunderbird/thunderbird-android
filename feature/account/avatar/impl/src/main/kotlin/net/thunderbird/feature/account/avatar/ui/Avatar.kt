package net.thunderbird.feature.account.avatar.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.core.ui.compose.theme2.toSurfaceContainer
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.feature.account.avatar.Avatar

private const val AVATAR_ALPHA = 0.2f
private const val CHECK_ICON_PADDING = 1.25f

/**
 * Shows an avatar based on the provided [Avatar] type.
 *
 * @param avatar The avatar to display.
 * @param color The color used for the avatar.
 * @param size The size of the avatar.
 * @param modifier The modifier to be applied to the avatar.
 * @param onClick Optional click handler for the avatar.
 * @param selected Whether the avatar is selected or not.
 */
@Composable
fun Avatar(
    avatar: Avatar,
    color: Color,
    size: AvatarSize,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
) {
    val backgroundColor = color.toSurfaceContainer(alpha = AVATAR_ALPHA)

    Box(
        contentAlignment = Alignment.TopStart,
        modifier = modifier,
    ) {
        AvatarLayout(
            color = color,
            backgroundColor = backgroundColor,
            size = size,
            onClick = onClick,
        ) {
            when (avatar) {
                is Avatar.Monogram -> {
                    AvatarMonogram(
                        monogram = avatar.value,
                        color = color,
                        size = size,
                    )
                }

                is Avatar.Image -> {
                    // TODO add support for image avatars
                }

                is Avatar.Icon -> {
                    // TODO add support for icon avatars
                }
            }
        }
        if (selected) {
            Surface(
                color = color,
                shape = CircleShape,
                modifier = Modifier
                    .size(MainTheme.sizes.iconSmall),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null, // The selection is visual and should be announced by the parent
                    tint = backgroundColor,
                    modifier = Modifier
                        .padding(CHECK_ICON_PADDING.dp),
                )
            }
        }
    }
}
