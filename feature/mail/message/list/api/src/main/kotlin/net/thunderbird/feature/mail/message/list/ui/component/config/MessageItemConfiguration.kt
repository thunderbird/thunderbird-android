package net.thunderbird.feature.mail.message.list.ui.component.config

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.component.molecule.MessageConversationCounterBadgeColor
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi

/**
 * Configuration class that defines the visual presentation and layout settings for
 * a message item.
 *
 * ```
 * Message Item structure:
 * ┌───────────┬──────────────────────┬──────────┐
 * │  Leading  │  Primary Line        │ Trailing │
 * │   Area    ├──────────────────────┤   Area   │
 * │           │  Secondary Line      │          │
 * │           │  Excerpt Line        │          │
 * └───────────┴──────────────────────┴──────────┘
 * ```
 *
 * @property maxExcerptLines The maximum number of lines to display for the message
 *  excerpt before truncation.
 * @property swapPrimaryLineWithSecondaryLine When true, the primary and secondary
 *  lines of the message display are swapped in their positions.
 * @property leadingConfiguration Configuration for elements displayed on the leading
 *  edge of the message item, such as badges and avatars.
 * @property accountIndicator Optional account indicator that can be displayed to
 *  distinguish messages from different accounts.
 * @property secondaryLineConfiguration Configuration for the secondary line of the
 *  message item, including any leading items to display.
 * @property excerptLineConfiguration Configuration for the excerpt/preview line of
 *  the message, including any leading items to display.
 * @property trailingConfiguration Configuration for elements displayed on the trailing
 *  edge of the message item, such as badges and action buttons.
 */
data class MessageItemConfiguration(
    val maxExcerptLines: Int = 2,
    val swapPrimaryLineWithSecondaryLine: Boolean = false,
    val leadingConfiguration: MessageItemLeadingConfiguration = MessageItemLeadingConfiguration(),
    val accountIndicator: MessageItemAccountIndicator? = null,
    val secondaryLineConfiguration: MessageSublineConfiguration = MessageSublineConfiguration(),
    val excerptLineConfiguration: MessageSublineConfiguration = MessageSublineConfiguration(),
    val trailingConfiguration: MessageItemTrailingConfiguration = MessageItemTrailingConfiguration(),
)

/**
 * Remembers and creates a configuration for a message item based on current parameters.
 *
 * @param messageItemUi The UI state of the message item containing information about state,
 *  senders, content, and metadata flags needed to build the configuration.
 * @param preferences The user's message list preferences that control display options such as
 *  number of excerpt lines and other visual settings.
 * @param color The color scheme for the conversation counter badge, defining container,
 *  content, and border colors.
 * @param accountIndicator Optional account indicator containing the color to display for
 *  account identification in unified inbox view. Can be null if no indicator is needed.
 * @return A MessageItemConfiguration instance containing all layout and display settings
 *  for rendering the message item.
 */
@Composable
internal fun rememberMessageItemConfiguration(
    messageItemUi: MessageItemUi,
    preferences: MessageListPreferences,
    color: MessageConversationCounterBadgeColor,
    accountIndicator: MessageItemAccountIndicator?,
): MessageItemConfiguration = remember(messageItemUi, preferences) {
    val leadingItems = buildLeadingItems(messageItemUi, color)
    MessageItemConfiguration(
        maxExcerptLines = preferences.excerptLines,
        leadingConfiguration = MessageItemLeadingConfiguration(
            badgeStyle = messageItemUi.state.toBadgeStyle(),
            avatar = messageItemUi.senders.avatar,
            avatarColor = messageItemUi.senders.color,
        ),
        accountIndicator = accountIndicator,
        secondaryLineConfiguration = MessageSublineConfiguration(
            leadingItems = if (preferences.excerptLines == 0) leadingItems else persistentListOf(),
        ),
        excerptLineConfiguration = MessageSublineConfiguration(
            leadingItems = if (preferences.excerptLines > 0) leadingItems else persistentListOf(),
        ),
        trailingConfiguration = MessageItemTrailingConfiguration(
            elements = buildTrailingElementsList(preferences, messageItemUi),
        ),
    )
}

private fun buildTrailingElementsList(
    preferences: MessageListPreferences,
    messageItemUi: MessageItemUi,
): PersistentList<MessageItemTrailingElement> = buildList {
    if (messageItemUi.encrypted) {
        add(MessageItemTrailingElement.EncryptedBadge)
    }
    if (preferences.showFavouriteButton) {
        add(MessageItemTrailingElement.FavouriteIconButton(favourite = messageItemUi.starred))
    }
}.toPersistentList()

private fun buildLeadingItems(
    messageItemUi: MessageItemUi,
    color: MessageConversationCounterBadgeColor,
): PersistentList<MessageSublineLeadingIndicator> = buildList {
    if (messageItemUi.threadCount > 1) {
        add(
            MessageSublineLeadingIndicator.ConversationCounterBadge(
                count = messageItemUi.threadCount,
                color = color,
            ),
        )
    }
    if (messageItemUi.hasAttachments) {
        add(MessageSublineLeadingIndicator.AttachmentIcon)
    }
}.toPersistentList()

private fun MessageItemUi.State.toBadgeStyle(): MessageBadgeStyle? = when (this) {
    MessageItemUi.State.New -> MessageBadgeStyle.New
    MessageItemUi.State.Unread -> MessageBadgeStyle.Unread
    MessageItemUi.State.Read -> null
}
