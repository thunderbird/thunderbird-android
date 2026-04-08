package net.thunderbird.feature.mail.message.list.ui.component.organism

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.mail.message.list.ui.component.molecule.MessageItemSenderBodyMedium

@Suppress("LongParameterList")
@Composable
fun JunkMessageItem(
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
    modifier: Modifier = Modifier,
    hasAttachments: Boolean = false,
    threadCount: Int = 0,
    selected: Boolean = false,
    maxPreviewLines: Int = 2,
    contentPadding: PaddingValues = MessageItemDefaults.defaultContentPadding,
    swapSenderWithSubject: Boolean = false,
) {
    JunkMessageItem(
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
        modifier = modifier,
        hasAttachments = hasAttachments,
        threadCount = threadCount,
        selected = selected,
        maxPreviewLines = maxPreviewLines,
        contentPadding = contentPadding,
        swapSenderWithSubject = swapSenderWithSubject,
    )
}

@Suppress("LongParameterList")
@Composable
fun JunkMessageItem(
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
            if (swapSenderWithSubject) {
                TextLabelLarge(text = sender, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                TextLabelLarge(text = subject, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        preview = preview,
        action = {
            Icon(
                imageVector = Icons.Outlined.Report,
                tint = MainTheme.colors.onErrorContainer,
            )
        },
        receivedAt = receivedAt,
        showAccountIndicator = showAccountIndicator,
        accountIndicatorColor = accountIndicatorColor,
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
