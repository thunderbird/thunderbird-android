package net.thunderbird.feature.mail.message.list.ui.component.config

import androidx.compose.ui.graphics.Color
import net.thunderbird.feature.mail.message.list.ui.state.Avatar

/**
 * Configuration for the leading area of a message item.
 *
 * This class defines the visual elements displayed at the start of a message item,
 * such as status badges and sender avatars.
 *
 * ```
 * Message Item structure:
 * ┌───────────┬──────────────────────┬──────────┐
 * │  Leading  │  Primary Line        │ Trailing │
 * │   Area    ├──────────────────────┤   Area   │
 * │   [X]     │  Secondary Line      │          │
 * │           │  Excerpt Line        │          │
 * └───────────┴──────────────────────┴──────────┘
 * [X] = Position where this configuration is rendered.
 * ```
 *
 * @property badgeStyle The style of the badge indicating message status (e.g., unread, new).
 * @property avatar The visual representation of the sender, such as an image, monogram, or icon.
 * @property avatarColor The background or tint color applied to the avatar.
 * @see MessageItemConfiguration
 * @see MessageBadgeStyle
 */
data class MessageItemLeadingConfiguration(
    val badgeStyle: MessageBadgeStyle? = null,
    val avatar: Avatar? = null,
    val avatarColor: Color? = null,
)

/**
 * Defines the visual style variants for message status badges.
 */
enum class MessageBadgeStyle { New, Unread }
