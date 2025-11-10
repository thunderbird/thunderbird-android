package net.thunderbird.core.ui.compose.designsystem.organism.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.datetime.LocalDateTime
import net.thunderbird.core.ui.compose.designsystem.atom.button.FavouriteButtonIcon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.designsystem.atom.icon.filled.NewMailBadge
import net.thunderbird.core.ui.compose.designsystem.molecule.message.MessageItemSenderTitleSmall

/**
 * Represents a message item in its New Message state.
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
fun NewMessageItem(
    sender: String,
    subject: String,
    preview: String,
    receivedAt: LocalDateTime,
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
        leading = {
            Box {
                avatar()
                Image(
                    imageVector = Icons.Filled.NewMailBadge,
                    contentDescription = null,
                    modifier = Modifier.padding(start = MainTheme.spacings.half, top = MainTheme.spacings.half),
                )
            }
        },
        sender = {
            MessageItemSenderTitleSmall(
                sender = sender,
                subject = subject,
                swapSenderWithSubject = swapSenderWithSubject,
                threadCount = threadCount,
                color = if (swapSenderWithSubject) MainTheme.colors.primary else MainTheme.colors.onSurface,
            )
        },
        subject = {
            TextLabelLarge(
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
        onLeadingClick = onLeadingClick,
        colors = if (selected) {
            MessageItemDefaults.selectedMessageItemColors()
        } else {
            MessageItemDefaults.newMessageItemColors(
                subjectColor = if (swapSenderWithSubject) MainTheme.colors.onSurface else MainTheme.colors.primary,
            )
        },
        modifier = modifier,
        hasAttachments = hasAttachments,
        selected = selected,
        maxPreviewLines = maxPreviewLines,
        contentPadding = contentPadding,
    )
}
