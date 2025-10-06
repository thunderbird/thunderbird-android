package net.thunderbird.core.ui.compose.designsystem.molecule.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
internal fun MessageItemSenderTitleSmall(
    sender: String,
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
            TextTitleSmall(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                color = color,
            )
        },
        threadCount = threadCount,
        modifier = modifier,
        color = color,
    )
}

@Composable
internal fun MessageItemSenderBodyMedium(
    sender: String,
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
            TextBodyMedium(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                color = color,
            )
        },
        threadCount = threadCount,
        modifier = modifier,
        color = color,
    )
}

@Composable
private fun MessageItemSenderText(
    sender: String,
    subject: String,
    swapSenderWithSubject: Boolean,
    text: @Composable RowScope.(text: String) -> Unit,
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
