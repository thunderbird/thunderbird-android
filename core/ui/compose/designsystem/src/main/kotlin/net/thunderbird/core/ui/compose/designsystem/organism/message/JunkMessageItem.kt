package net.thunderbird.core.ui.compose.designsystem.organism.message

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.datetime.LocalDateTime
import net.thunderbird.core.ui.compose.designsystem.molecule.message.MessageItemSenderBodyMedium

@Composable
fun JunkMessageItem(
    sender: String,
    subject: String,
    preview: String,
    receivedAt: LocalDateTime,
    avatar: @Composable () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onLeadingClick: () -> Unit,
    modifier: Modifier = Modifier,
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
            TextBodyMedium(
                text = if (swapSenderWithSubject) sender else subject,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        preview = preview,
        action = {
            Icon(
                imageVector = Icons.Outlined.Report,
                tint = MainTheme.colors.onErrorContainer,
            )
        },
        receivedAt = receivedAt,
        onClick = onClick,
        onLongClick = onLongClick,
        onLeadingClick = onLeadingClick,
        colors = if (selected) {
            MessageItemDefaults.selectedMessageItemColors(
                containerColor = MainTheme.colors.errorContainer,
            )
        } else {
            MessageItemDefaults.junkMessageItemColors()
        },
        modifier = modifier,
        hasAttachments = hasAttachments,
        selected = selected,
        maxPreviewLines = maxPreviewLines,
        contentPadding = contentPadding,
    )
}
