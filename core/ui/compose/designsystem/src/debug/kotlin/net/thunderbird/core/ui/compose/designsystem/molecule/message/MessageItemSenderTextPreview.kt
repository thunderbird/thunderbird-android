package net.thunderbird.core.ui.compose.designsystem.molecule.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.theme2.MainTheme

private data class MessageItemSenderTextPreviewParams(
    val sender: String,
    val subject: String,
    val swapSenderWithSubject: Boolean,
    val threadCount: Int,
)

private class MessageItemSenderTextPreviewCol : CollectionPreviewParameterProvider<MessageItemSenderTextPreviewParams>(
    listOf(
        MessageItemSenderTextPreviewParams(
            sender = "Sender",
            subject = "Subject",
            swapSenderWithSubject = false,
            threadCount = 0,
        ),
        MessageItemSenderTextPreviewParams(
            sender = "Sender",
            subject = "Subject",
            swapSenderWithSubject = true,
            threadCount = 0,
        ),
        MessageItemSenderTextPreviewParams(
            sender = "Sender",
            subject = "Subject",
            swapSenderWithSubject = false,
            threadCount = 10,
        ),
        MessageItemSenderTextPreviewParams(
            sender = "Sender",
            subject = "Subject",
            swapSenderWithSubject = true,
            threadCount = 10,
        ),
        MessageItemSenderTextPreviewParams(
            sender = LoremIpsum(words = 10).values.joinToString(" "),
            subject = "Subject",
            swapSenderWithSubject = false,
            threadCount = 10,
        ),
        MessageItemSenderTextPreviewParams(
            sender = "Sender",
            subject = LoremIpsum(words = 10).values.joinToString(" "),
            swapSenderWithSubject = true,
            threadCount = 10,
        ),
    ),
)

@Preview
@Composable
private fun MessageItemSenderTextPreview(
    @PreviewParameter(MessageItemSenderTextPreviewCol::class) params: MessageItemSenderTextPreviewParams,
) {
    PreviewWithThemes {
        Column(verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default)) {
            DividerHorizontal(modifier = Modifier.padding(vertical = MainTheme.spacings.default))
            TextBodySmall(text = "Params: $params")
            DividerHorizontal(modifier = Modifier.padding(vertical = MainTheme.spacings.default))
            TextLabelSmall(text = "MessageItemSenderTitleSmall:")
            MessageItemSenderTitleSmall(
                subject = params.subject,
                sender = params.sender,
                swapSenderWithSubject = params.swapSenderWithSubject,
                threadCount = params.threadCount,
            )
            DividerHorizontal(modifier = Modifier.padding(vertical = MainTheme.spacings.default))
            TextLabelSmall(text = "MessageItemSenderBodyMedium:")
            MessageItemSenderBodyMedium(
                subject = params.subject,
                sender = params.sender,
                swapSenderWithSubject = params.swapSenderWithSubject,
                threadCount = params.threadCount,
            )
        }
    }
}
