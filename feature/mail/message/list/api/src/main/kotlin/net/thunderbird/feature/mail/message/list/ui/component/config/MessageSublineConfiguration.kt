package net.thunderbird.feature.mail.message.list.ui.component.config

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.mail.message.list.ui.component.molecule.MessageConversationCounterBadgeColor

/**
 * Configuration that defines the leading elements displayed on a subline (Secondary or Excerpt)
 * within a message list item.
 *
 * This class determines which visual indicators, such as attachment icons or conversation
 * counters, appear at the start of the line. Items are rendered in the order they appear
 * in the list.
 *
 * ```
 * Message Item structure:
 * ┌───────────┬──────────────────────────┬──────────┐
 * │  Leading  │  Primary Line            │ Trailing │
 * │   Area    ├──────────────────────────┤   Area   │
 * │           │  Secondary Line [X]      │          │
 * │           │  Excerpt Line   [X]      │          │
 * └───────────┴──────────────────────────┴──────────┘
 * [X] = Position where this configuration is rendered.
 * ```
 *
 * @property leadingItems The list of indicators to be displayed at the leading edge of the line.
 * @see MessageItemConfiguration
 * @see MessageSublineLeadingIndicator
 */
data class MessageSublineConfiguration(
    val leadingItems: ImmutableList<MessageSublineLeadingIndicator> = persistentListOf(),
)

/**
 * Sealed interface representing visual elements that can be displayed at the
 * start of the secondary and excerpt lines within a message item.
 */
@Immutable
sealed interface MessageSublineLeadingIndicator {
    /**
     * Represents a visual element displaying an icon indicator for messages with
     * attachments.
     */
    data object AttachmentIcon : MessageSublineLeadingIndicator

    /**
     * Represents a badge displaying the count of messages in a conversation thread.
     *
     * @property count The number of messages in the conversation thread to be displayed
     *  on the badge
     * @property color The color scheme defining the visual appearance of the counter
     *  badge
     */
    data class ConversationCounterBadge(
        val count: Int,
        val color: MessageConversationCounterBadgeColor,
    ) : MessageSublineLeadingIndicator
}
