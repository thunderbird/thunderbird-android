package net.thunderbird.core.ui.compose.designsystem.molecule.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import app.k9mail.core.ui.compose.theme2.MainTheme

/**
 * Displays the sender or subject of a message item with small title styling.
 *
 * The component shows either the sender or subject based on the swap parameter,
 * with an optional thread count indicator.
 *
 * Text is displayed with ellipsis overflow when it exceeds available space.
 *
 * @param sender The sender information as an AnnotatedString to display
 * @param subject The subject line of the message
 * @param swapSenderWithSubject If `true`, displays the subject instead of the sender;
 *  if `false`, displays the sender
 * @param threadCount The number of messages in the thread; displays the count when greater than 0
 * @param modifier The modifier to be applied to the composable
 * @param color The text color to use for the displayed content
 */
@Composable
internal fun MessageItemSenderTitleSmall(
    sender: AnnotatedString,
    subject: String,
    swapSenderWithSubject: Boolean,
    threadCount: Int,
    modifier: Modifier = Modifier,
    color: Color = MainTheme.colors.onSurface,
) {
    MessageItemSenderText(
        sender = sender,
        subject = subject,
        swapSenderWithSubject = swapSenderWithSubject,
        text = { text ->
            when (text) {
                is AnnotatedString -> TextTitleSmall(
                    text = text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier,
                    color = color,
                )

                else -> TextTitleSmall(
                    text = text.toString(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier,
                    color = color,
                )
            }
        },
        threadCount = threadCount,
        modifier = modifier,
        color = color,
    )
}

/**
 * Displays the sender and subject information for a message item using medium-sized
 * body text.
 *
 * The component shows either the sender or subject based on the swap parameter,
 * with an optional thread count indicator.
 *
 * Text is displayed with ellipsis overflow when it exceeds available space.
 *
 * @param sender The sender information as an [AnnotatedString] to display
 * @param subject The subject line of the message
 * @param swapSenderWithSubject If `true`, displays the subject instead of the sender;
 *  if `false`, displays the sender
 * @param threadCount The number of messages in the thread; displays the count when greater than 0
 * @param modifier The modifier to be applied to the composable
 * @param color The text color to use for the displayed content
 */
@Composable
internal fun MessageItemSenderBodyMedium(
    sender: AnnotatedString,
    subject: String,
    swapSenderWithSubject: Boolean,
    threadCount: Int,
    modifier: Modifier = Modifier,
    color: Color = MainTheme.colors.onSurface,
) {
    MessageItemSenderText(
        sender = sender,
        subject = subject,
        swapSenderWithSubject = swapSenderWithSubject,
        text = { text ->
            when (text) {
                is AnnotatedString -> TextBodyMedium(
                    text = text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier,
                    color = color,
                )

                else -> TextBodyMedium(
                    text = text.toString(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier,
                    color = color,
                )
            }
        },
        threadCount = threadCount,
        modifier = modifier,
        color = color,
    )
}

@Composable
private fun MessageItemSenderText(
    sender: AnnotatedString,
    subject: String,
    swapSenderWithSubject: Boolean,
    text: @Composable RowScope.(text: CharSequence) -> Unit,
    threadCount: Int,
    modifier: Modifier = Modifier,
    color: Color = MainTheme.colors.onSurface,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
        modifier = modifier,
    ) {
        text(if (swapSenderWithSubject) subject else sender)
        if (threadCount > 0) {
            TextLabelSmall(
                text = threadCount.toString(),
                color = color,
            )
        }
    }
}
