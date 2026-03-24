package net.thunderbird.feature.mail.message.list.ui.component.organism

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemAccountIndicator
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemTrailingElement
import net.thunderbird.feature.mail.message.list.ui.component.config.rememberMessageItemConfiguration
import net.thunderbird.feature.mail.message.list.ui.component.molecule.MessageConversationCounterBadgeDefaults
import net.thunderbird.feature.mail.message.list.ui.component.molecule.MessageItemSenderTitleSmall
import net.thunderbird.feature.mail.message.list.ui.component.organism.MessageItemDefaults.toContentPadding
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi

/**
 * Represents a message item in its New Message state.
 *
 * @param state The UI state containing all message information to be displayed.
 * @param preferences User preferences that control the appearance and layout of
 *  the message item.
 * @param accountIndicator Optional visual indicator to identify which account
 *  the message belongs to, useful in unified inbox view. Pass `null` if no
 *  indicator should be shown.
 * @param onClick Callback invoked when the message item is clicked.
 * @param onLongClick Callback invoked when the message item is long-clicked.
 * @param onAvatarClick Callback invoked when the message avatar is clicked.
 * @param onFavouriteChange Callback invoked when the favourite/starred state
 *  changes. Receives the new favourite state as a parameter.
 * @param modifier The modifier to be applied to the message item.
 */
@Suppress("LongParameterList")
@Composable
fun NewMessageItem(
    state: MessageItemUi,
    preferences: MessageListPreferences,
    accountIndicator: MessageItemAccountIndicator?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onFavouriteChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val swapSenderWithSubject = !preferences.senderAboveSubject
    MessageItem(
        firstLine = {
            MessageItemSenderTitleSmall(
                sender = AnnotatedString(state.senders.displayName),
                subject = state.subject,
                swapSenderWithSubject = swapSenderWithSubject,
                threadCount = state.threadCount,
                color = if (swapSenderWithSubject) MainTheme.colors.primary else MainTheme.colors.onSurface,
            )
        },
        secondaryLine = { _, _ ->
            if (swapSenderWithSubject) {
                TextLabelLarge(text = state.senders.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                TextLabelLarge(text = state.subject, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        excerpt = state.excerpt,
        receivedAt = state.formattedReceivedAt,
        configuration = rememberMessageItemConfiguration(
            messageItemUi = state,
            preferences = preferences,
            color = MessageConversationCounterBadgeDefaults.newMessageColor(),
            accountIndicator = accountIndicator,
        ),
        onClick = onClick,
        onLongClick = onLongClick,
        onAvatarClick = onAvatarClick,
        onTrailingClick = { element ->
            when (element) {
                is MessageItemTrailingElement.FavouriteIconButton if preferences.showFavouriteButton ->
                    onFavouriteChange(element.favourite)

                else -> Unit
            }
        },
        modifier = modifier,
        selected = state.selected,
        colors = when {
            state.selected -> MessageItemDefaults.selectedMessageItemColors()
            state.isActive -> MessageItemDefaults.activeMessageItemColors()
            else -> MessageItemDefaults.newMessageItemColors()
        },
        contentPadding = preferences.density.toContentPadding(),
    )
}
