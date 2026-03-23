package net.thunderbird.feature.mail.message.list.ui.component.config

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Configuration class that defines the elements displayed on the trailing edge of a message item.
 *
 * This data class encapsulates the configuration for visual elements that appear at the end
 * (trailing edge) of a message item, such as badges and action buttons. The elements are
 * displayed in the order specified in the list.
 *
 * ```
 * Message Item structure:
 * ┌───────────┬──────────────────────────┬──────────┐
 * │  Leading  │  Primary Line            │ Trailing │
 * │   Area    ├──────────────────────────┤   Area   │
 * │           │  Secondary Line          │   [X]    │
 * │           │  Excerpt Line            │          │
 * └───────────┴──────────────────────────┴──────────┘
 * [X] = Position where this configuration is rendered.
 * ```
 *
 * @property elements An immutable list of trailing elements to be displayed.
 *  Defaults to an empty list if not specified.
 * @see MessageItemConfiguration
 */
data class MessageItemTrailingConfiguration(
    val elements: ImmutableList<MessageItemTrailingElement> = persistentListOf(),
)

/**
 * Represents the types of elements that can be displayed on the trailing edge of a message item.
 */
@Immutable
sealed interface MessageItemTrailingElement {
    data object EncryptedBadge : MessageItemTrailingElement
    data class FavouriteIconButton(val favourite: Boolean) : MessageItemTrailingElement
}
