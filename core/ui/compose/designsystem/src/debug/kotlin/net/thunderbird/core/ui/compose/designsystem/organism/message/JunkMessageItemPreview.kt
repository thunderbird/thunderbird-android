package net.thunderbird.core.ui.compose.designsystem.organism.message

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private class JunkMessageItemPrevParamCol : CollectionPreviewParameterProvider<MessageItemPrevParams>(
    collection = listOf(
        MessageItemPrevParams(
            sender = "Sender Name",
            subject = "The subject",
            preview = LoremIpsum(words = 3).values.joinToString(),
            hasAttachments = false,
            selected = false,
            favourite = false,
            threadCount = 0,
            swapSenderWithSubject = false,
        ),
        MessageItemPrevParams(
            sender = LoremIpsum(words = 100).values.joinToString(),
            subject = LoremIpsum(words = 100).values.joinToString(),
            preview = LoremIpsum(words = 5).values.joinToString(),
            hasAttachments = true,
            selected = false,
            favourite = false,
            threadCount = 1,
            swapSenderWithSubject = false,
        ),
        MessageItemPrevParams(
            sender = "Sender Name",
            subject = "The subject",
            preview = LoremIpsum(words = 10).values.joinToString(),
            hasAttachments = false,
            selected = true,
            favourite = true,
            threadCount = 10,
            swapSenderWithSubject = false,
        ),
        MessageItemPrevParams(
            sender = "Sender Name",
            subject = "The subject",
            preview = LoremIpsum(words = 20).values.joinToString(),
            hasAttachments = true,
            selected = true,
            threadCount = 100,
            swapSenderWithSubject = false,
        ),
        MessageItemPrevParams(
            sender = "Sender Name",
            subject = "The subject",
            preview = LoremIpsum(words = 3).values.joinToString(),
            hasAttachments = false,
            selected = false,
            threadCount = 0,
            swapSenderWithSubject = true,
        ),
        MessageItemPrevParams(
            sender = LoremIpsum(words = 100).values.joinToString(),
            subject = LoremIpsum(words = 100).values.joinToString(),
            preview = LoremIpsum(words = 5).values.joinToString(),
            hasAttachments = true,
            selected = false,
            threadCount = 1,
            swapSenderWithSubject = true,
        ),
        MessageItemPrevParams(
            sender = "Sender Name",
            subject = "The subject",
            preview = LoremIpsum(words = 10).values.joinToString(),
            hasAttachments = false,
            selected = true,
            threadCount = 10,
            swapSenderWithSubject = true,
        ),
        MessageItemPrevParams(
            sender = "Sender Name",
            subject = "The subject",
            preview = LoremIpsum(words = 20).values.joinToString(),
            hasAttachments = true,
            selected = true,
            threadCount = 100,
            swapSenderWithSubject = true,
        ),
    ),
)

@Preview
@Composable
private fun PreviewDefault(
    @PreviewParameter(JunkMessageItemPrevParamCol::class) params: MessageItemPrevParams,
) {
    PreviewWithThemes {
        JunkMessageItem(
            sender = params.sender,
            subject = params.subject,
            preview = params.preview,
            receivedAt = @OptIn(ExperimentalTime::class) Clock.System.now().toLocalDateTime(TimeZone.UTC),
            avatar = {
                Box(
                    modifier = Modifier
                        .size(MainTheme.sizes.iconAvatar)
                        .background(
                            color = MainTheme.colors.primaryContainer.copy(alpha = 0.15f),
                            shape = CircleShape,
                        )
                        .border(width = 1.dp, color = MainTheme.colors.primary, shape = CircleShape),
                ) {
                    TextTitleSmall(text = "SN", modifier = Modifier.align(Alignment.Center))
                }
            },
            onClick = { },
            modifier = Modifier.padding(MainTheme.spacings.double),
            hasAttachments = params.hasAttachments,
            selected = params.selected,
            threadCount = params.threadCount,
            swapSenderWithSubject = params.swapSenderWithSubject,
        )
    }
}

@Preview
@Composable
private fun PreviewCompact(
    @PreviewParameter(JunkMessageItemPrevParamCol::class) params: MessageItemPrevParams,
) {
    PreviewWithThemes {
        JunkMessageItem(
            sender = params.sender,
            subject = params.subject,
            preview = params.preview,
            receivedAt = @OptIn(ExperimentalTime::class) Clock.System.now().toLocalDateTime(TimeZone.UTC),
            avatar = { },
            onClick = { },
            modifier = Modifier.padding(MainTheme.spacings.double),
            hasAttachments = params.hasAttachments,
            selected = params.selected,
            threadCount = params.threadCount,
            swapSenderWithSubject = params.swapSenderWithSubject,
            contentPadding = MessageItemDefaults.compactContentPadding,
        )
    }
}

@Preview
@Composable
private fun PreviewRelaxed(
    @PreviewParameter(JunkMessageItemPrevParamCol::class) params: MessageItemPrevParams,
) {
    PreviewWithThemes {
        JunkMessageItem(
            sender = params.sender,
            subject = params.subject,
            preview = params.preview,
            receivedAt = @OptIn(ExperimentalTime::class) Clock.System.now().toLocalDateTime(TimeZone.UTC),
            avatar = { },
            onClick = { },
            modifier = Modifier.padding(MainTheme.spacings.double),
            hasAttachments = params.hasAttachments,
            selected = params.selected,
            threadCount = params.threadCount,
            swapSenderWithSubject = params.swapSenderWithSubject,
            contentPadding = MessageItemDefaults.relaxedContentPadding,
        )
    }
}
