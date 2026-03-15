package net.thunderbird.feature.mail.message.list.ui.component.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.CircularProgressIndicator
import app.k9mail.core.ui.compose.designsystem.atom.image.RemoteImage
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.theme2.contentColorFor
import net.thunderbird.feature.mail.message.list.ui.state.Avatar

/**
 * A circular avatar component for displaying message sender information in various formats.
 *
 * This composable renders a circular container with a border and background, displaying one of three
 * avatar types: an icon, a monogram (initials), or a remote image. The avatar is clickable and supports
 * enabled/disabled states.
 *
 * @param avatar The avatar content to display, which can be an icon, monogram, or image.
 * @param colors The color scheme defining the border, container, and content colors for the avatar circle.
 * @param onClick Callback invoked when the avatar is clicked.
 * @param modifier The modifier to be applied to the avatar container.
 * @param enabled Whether the avatar is clickable. When false, click events are ignored.
 */
@Composable
fun MessageItemAvatarCircle(
    avatar: Avatar,
    colors: MessageItemAvatarCircleColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // Intentionally set the `combinedClickable` modifier is after `clip(CircleShape)`
    // and before `padding`.
    //
    // This ensures that the ripple effect covers the entire touch target rather than
    // just the avatar icon itself.
    //
    // While Detekt may issue a warning regarding the order of modifiers, this specific
    // arrangement is intentional.
    @Suppress("ModifierClickableOrder")
    Box(
        modifier = modifier
            .clip(CircleShape)
            .combinedClickable(enabled = enabled, onClick = onClick)
            .padding(MainTheme.spacings.half)
            .background(color = colors.containerColor, shape = CircleShape)
            .border(width = 1.dp, color = colors.borderColor, shape = CircleShape)
            .size(MainTheme.sizes.iconAvatar),
        contentAlignment = Alignment.Center,
    ) {
        when (avatar) {
            is Avatar.Icon -> Icon(
                imageVector = avatar.imageVector,
                modifier = Modifier.size(MainTheme.sizes.iconSmall),
            )

            is Avatar.Monogram -> TextTitleSmall(text = avatar.value)
            is Avatar.Image -> RemoteImage(
                url = avatar.url,
                placeholder = {
                    CircularProgressIndicator()
                },
                previewPlaceholder = rememberVectorPainter(Icons.Outlined.AccountCircle),
                modifier = Modifier.clip(CircleShape),
            )
        }
    }
}

/**
 * Contains default values and factory methods for creating AvatarCircle components.
 *
 * This object provides default configurations for AvatarCircle styling, particularly
 * color schemes that maintain visual consistency across the application. It serves
 * as a centralized source for avatar appearance defaults.
 */
@Immutable
object MessageItemAvatarCircleDefaults {
    /**
     * Creates an AvatarCircleColors instance derived from a base color.
     *
     * This composable function generates a complete color scheme for an avatar circle component
     * by deriving the container and content colors from the provided base color. The container
     * color is created with reduced opacity for a subtle background effect, while the content
     * color is automatically determined based on the base color to ensure proper contrast.
     *
     * @param color The base color used to derive the avatar circle's color scheme. This color
     *  is used directly as the border color, with reduced opacity for the container, and
     *  automatically calculated contrast color for the content.
     * @return An AvatarCircleColors instance with border, container, and content colors derived
     *  from the provided base color.
     */
    @Composable
    fun colorsFrom(color: Color) = MessageItemAvatarCircleColors(
        borderColor = color,
        containerColor = color.copy(alpha = .15f),
        contentColor = contentColorFor(color),
    )
}

/**
 * Defines the color scheme for an avatar circle component.
 *
 * This data class encapsulates the color properties used to style circular avatar components,
 * including the border, container background, and content colors. By default, the container
 * color is derived from the border color with reduced opacity to create a subtle background effect.
 *
 * @property borderColor The color of the circular border around the avatar.
 * @property containerColor The background color of the avatar container. Defaults to the border
 *  color with 15% opacity.
 * @property contentColor The color applied to the content displayed within the avatar, such as
 *  icons or text initials.
 */
data class MessageItemAvatarCircleColors(
    val borderColor: Color,
    val containerColor: Color = borderColor.copy(alpha = .15f),
    val contentColor: Color,
)
