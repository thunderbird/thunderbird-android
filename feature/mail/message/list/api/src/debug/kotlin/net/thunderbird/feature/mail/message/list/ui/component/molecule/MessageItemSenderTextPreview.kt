package net.thunderbird.feature.mail.message.list.ui.component.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.mail.message.list.ui.state.ComposedAddressStyle
import net.thunderbird.feature.mail.message.list.ui.state.ComposedAddressUi

private data class MessageItemSenderTextPreviewParams(
    val senders: ComposedAddressUi,
    val subject: String,
    val useSender: Boolean,
)

private class MessageItemSenderTextPreviewCol : CollectionPreviewParameterProvider<MessageItemSenderTextPreviewParams>(
    listOf(
        MessageItemSenderTextPreviewParams(
            senders = ComposedAddressUi(displayName = "Sender", avatar = null, color = null),
            subject = "Subject",
            useSender = false,
        ),
        MessageItemSenderTextPreviewParams(
            senders = ComposedAddressUi(displayName = "Sender", avatar = null, color = null),
            subject = "Subject",
            useSender = true,
        ),
        MessageItemSenderTextPreviewParams(
            senders = ComposedAddressUi(displayName = "Sender", avatar = null, color = null),
            subject = "Subject",
            useSender = false,
        ),
        MessageItemSenderTextPreviewParams(
            senders = ComposedAddressUi(displayName = "Sender", avatar = null, color = null),
            subject = "Subject",
            useSender = true,
        ),
        MessageItemSenderTextPreviewParams(
            senders = ComposedAddressUi(
                displayName = LoremIpsum(words = 10).values.joinToString(" "),
                avatar = null,
                color = null,
            ),
            subject = "Subject",
            useSender = false,
        ),
        MessageItemSenderTextPreviewParams(
            senders = ComposedAddressUi(displayName = "Sender", avatar = null, color = null),
            subject = LoremIpsum(words = 10).values.joinToString(" "),
            useSender = true,
        ),
        MessageItemSenderTextPreviewParams(
            senders = ComposedAddressUi(
                displayName = "Mason Tran, Me, Ryan Thomas",
                displayNameStyles = ComposedAddressStyle.styles(
                    ComposedAddressStyle.Bold(start = 0, end = 11),
                    ComposedAddressStyle.Regular(start = 12),
                ),
                avatar = null,
                color = null,
            ),
            subject = LoremIpsum(words = 10).values.joinToString(" "),
            useSender = true,
        ),
        MessageItemSenderTextPreviewParams(
            senders = ComposedAddressUi(
                displayName = "Mason Tran, Me, Ryan Thomas",
                displayNameStyles = ComposedAddressStyle.styles(
                    ComposedAddressStyle.Bold(start = 0, end = 11),
                    ComposedAddressStyle.Regular(start = 12),
                ),
                avatar = null,
                color = null,
            ),
            subject = LoremIpsum(words = 10).values.joinToString(" "),
            useSender = false,
        ),
    ),
) {
    override fun getDisplayName(index: Int): String {
        return values.toList()[index].let {
            buildString {
                append("useSender: ${it.useSender}, ")
                append(
                    "senders(name: ${it.senders.displayName.take(n = 10)}, styles: ${it.senders.displayNameStyles}), ",
                )
                append("subject: ${it.subject}")
            }
        }
    }
}

@Preview
@Composable
private fun MessageItemSenderTextPreview(
    @PreviewParameter(MessageItemSenderTextPreviewCol::class) params: MessageItemSenderTextPreviewParams,
) {
    PreviewWithThemes {
        Column(verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default)) {
            TextLabelSmall(text = "MessageItemSenderTitleSmall:")
            MessageItemSenderSubjectFirstLine(
                senders = params.senders,
                subject = AnnotatedString(params.subject),
                useSender = params.useSender,
            )
            DividerHorizontal(modifier = Modifier.padding(vertical = MainTheme.spacings.default))
            TextLabelSmall(text = "MessageItemSenderBodyMedium:")
            MessageItemSenderSubjectSecondLine(
                senders = params.senders,
                subject = AnnotatedString(params.subject),
                useSender = params.useSender,
            )
        }
    }
}
