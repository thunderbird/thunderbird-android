package net.thunderbird.feature.mail.message.list.ui.component.organism

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.mail.message.list.ui.component.atom.FavouriteButtonIcon
import net.thunderbird.feature.mail.message.list.ui.component.molecule.MessageItemSenderBodyMedium

/**
 * Represents a message item in its Active state.
 *
 * @param sender The name of the sender.
 * @param subject The subject of the message.
 * @param preview A short preview of the message content.
 * @param receivedAt The date and time the message was received.
 * @param favourite Whether the message is marked as favourite.
 * @param avatar A composable function to display the sender's avatar.
 * @param onClick A lambda function to be invoked when the message item is clicked.
 * @param onLongClick A lambda function to be invoked when the message item is long-clicked.
 * @param onLeadingClick A lambda function to be invoked when the leading avatar is clicked.
 * @param onFavouriteChange A lambda function to be invoked when the favourite button is clicked.
 * @param modifier A [Modifier] to be applied to the message item.
 * @param hasAttachments Whether the message has attachments. Defaults to `false`.
 * @param threadCount The number of messages in the thread. Defaults to `0`. If greater than 0,
 * it will be displayed next to the sender.
 * @param selected Whether the message item is currently selected. Defaults to `false`.
 * @param maxPreviewLines The maximum number of lines to display for the preview. Defaults to `2`.
 * @param contentPadding The padding to apply to the content of the message item. Defaults to
 * [MessageItemDefaults.defaultContentPadding].
 * @param swapSenderWithSubject If `true`, the sender and subject will be swapped in their display positions.
 * Defaults to `false`.
 */
@Suppress("LongParameterList")
@Composable
fun ActiveMessageItem(
    sender: String,
    subject: String,
    preview: String,
    receivedAt: String,
    showAccountIndicator: Boolean,
    accountIndicatorColor: Color?,
    avatar: @Composable () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onLeadingClick: () -> Unit,
    onFavouriteChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    favourite: Boolean = false,
    hasAttachments: Boolean = false,
    threadCount: Int = 0,
    selected: Boolean = false,
    maxPreviewLines: Int = 2,
    contentPadding: PaddingValues = MessageItemDefaults.defaultContentPadding,
    swapSenderWithSubject: Boolean = false,
) {
    ActiveMessageItem(
        sender = AnnotatedString(sender),
        subject = subject,
        preview = preview,
        receivedAt = receivedAt,
        showAccountIndicator = showAccountIndicator,
        accountIndicatorColor = accountIndicatorColor,
        avatar = avatar,
        onClick = onClick,
        onLongClick = onLongClick,
        onLeadingClick = onLeadingClick,
        onFavouriteChange = onFavouriteChange,
        modifier = modifier,
        favourite = favourite,
        hasAttachments = hasAttachments,
        threadCount = threadCount,
        selected = selected,
        maxPreviewLines = maxPreviewLines,
        contentPadding = contentPadding,
        swapSenderWithSubject = swapSenderWithSubject,
    )
}

/**
 * Represents a message item in its Active state.
 *
 * @param sender The name of the sender.
 * @param subject The subject of the message.
 * @param preview A short preview of the message content.
 * @param receivedAt The date and time the message was received.
 * @param favourite Whether the message is marked as favourite.
 * @param avatar A composable function to display the sender's avatar.
 * @param onClick A lambda function to be invoked when the message item is clicked.
 * @param onLongClick A lambda function to be invoked when the message item is long-clicked.
 * @param onLeadingClick A lambda function to be invoked when the leading avatar is clicked.
 * @param onFavouriteChange A lambda function to be invoked when the favourite button is clicked.
 * @param modifier A [Modifier] to be applied to the message item.
 * @param hasAttachments Whether the message has attachments. Defaults to `false`.
 * @param threadCount The number of messages in the thread. Defaults to `0`. If greater than 0,
 * it will be displayed next to the sender.
 * @param selected Whether the message item is currently selected. Defaults to `false`.
 * @param maxPreviewLines The maximum number of lines to display for the preview. Defaults to `2`.
 * @param contentPadding The padding to apply to the content of the message item. Defaults to
 * [MessageItemDefaults.defaultContentPadding].
 * @param swapSenderWithSubject If `true`, the sender and subject will be swapped in their display positions.
 * Defaults to `false`.
 */
@Suppress("LongParameterList")
@Composable
fun ActiveMessageItem(
    sender: AnnotatedString,
    subject: String,
    preview: String,
    receivedAt: String,
    showAccountIndicator: Boolean,
    accountIndicatorColor: Color?,
    avatar: @Composable () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onLeadingClick: () -> Unit,
    onFavouriteChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    favourite: Boolean = false,
    hasAttachments: Boolean = false,
    threadCount: Int = 0,
    selected: Boolean = false,
    maxPreviewLines: Int = 2,
    contentPadding: PaddingValues = MessageItemDefaults.defaultContentPadding,
    swapSenderWithSubject: Boolean = false,
) {
    MessageItem(
        leading = avatar,
        sender = {
            MessageItemSenderBodyMedium(
                sender = sender,
                subject = subject,
                swapSenderWithSubject = swapSenderWithSubject,
                threadCount = threadCount,
                color = MainTheme.colors.onSurfaceVariant,
            )
        },
        subject = {
            if (swapSenderWithSubject) {
                TextBodyMedium(text = sender, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                TextBodyMedium(text = subject, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        preview = preview,
        action = { FavouriteButtonIcon(favourite = favourite, onFavouriteChange = onFavouriteChange) },
        receivedAt = receivedAt,
        showAccountIndicator = showAccountIndicator,
        accountIndicatorColor = accountIndicatorColor,
        onClick = onClick,
        onLongClick = onLongClick,
        onLeadingClick = onLeadingClick,
        colors = if (selected) {
            MessageItemDefaults.selectedMessageItemColors()
        } else {
            MessageItemDefaults.activeMessageItemColors()
        },
        modifier = modifier,
        hasAttachments = hasAttachments,
        selected = selected,
        maxPreviewLines = maxPreviewLines,
        contentPadding = contentPadding,
    )
}
