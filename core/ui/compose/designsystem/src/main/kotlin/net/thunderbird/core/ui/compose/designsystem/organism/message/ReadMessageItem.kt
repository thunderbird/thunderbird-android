package net.thunderbird.core.ui.compose.designsystem.organism.message

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import kotlinx.datetime.LocalDateTime
import net.thunderbird.core.ui.compose.designsystem.atom.button.FavouriteButtonIcon
import net.thunderbird.core.ui.compose.designsystem.molecule.message.MessageItemSenderBodyMedium

/**
 * Represents a message item in its Read state.
 *
 * @param sender The name of the sender.
 * @param subject The subject of the message.
 * @param preview A short preview of the message content.
 * @param receivedAt The date and time the message was received.
 * @param favourite Whether the message is marked as favourite.
 * @param avatar A composable function to display the sender's avatar.
 * @param onClick A lambda function to be invoked when the message item is clicked.
 * @param onLongClick A lambda function to be invoked when the message item is long-clicked.
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
@Composable
fun ReadMessageItem(
    sender: String,
    subject: String,
    preview: String,
    receivedAt: LocalDateTime,
    avatar: @Composable () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
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
        leading = {
            avatar()
        },
        sender = {
            MessageItemSenderBodyMedium(
                sender = sender,
                subject = subject,
                swapSenderWithSubject = swapSenderWithSubject,
                threadCount = threadCount,
            )
        },
        subject = {
            TextBodyMedium(
                text = if (swapSenderWithSubject) sender else subject,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        preview = preview,
        action = { FavouriteButtonIcon(favourite = favourite, onFavouriteChange = onFavouriteChange) },
        receivedAt = receivedAt,
        onClick = onClick,
        onLongClick = onLongClick,
        colors = if (selected) {
            MessageItemDefaults.selectedMessageItemColors()
        } else {
            MessageItemDefaults.readMessageItemColors()
        },
        modifier = modifier,
        hasAttachments = hasAttachments,
        selected = selected,
        maxPreviewLines = maxPreviewLines,
        contentPadding = contentPadding,
    )
}
