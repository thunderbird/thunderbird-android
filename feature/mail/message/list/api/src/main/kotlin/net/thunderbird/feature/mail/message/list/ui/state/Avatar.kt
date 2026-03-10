package net.thunderbird.feature.mail.message.list.ui.state

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents the different types of avatars that can be displayed for a message sender.
 */
sealed interface Avatar {
    /**
     * Represents a text-based avatar, typically composed of the sender's initials.
     *
     * @param value The string to be displayed in the monogram, e.g., "JO" for "John Doe".
     */
    data class Monogram(val value: String) : Avatar

    /**
     * Represents an avatar that is an image loaded from a URL.
     *
     * @property url The URL of the image to display.
     */
    data class Image(val url: String) : Avatar

    /**
     * Represents an avatar that is a vector graphic.
     *
     * @property imageVector The [ImageVector] to be displayed as the avatar.
     */
    data class Icon(val imageVector: ImageVector) : Avatar
}
