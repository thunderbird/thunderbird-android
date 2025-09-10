package net.thunderbird.core.ui.compose.designsystem.organism.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thunderbird.core.ui.compose.designsystem.atom.icon.filled.Star

private class MessageItemPrevParamCol : CollectionPreviewParameterProvider<MessageItemPrevParams>(
    collection = listOf(
        MessageItemPrevParams(
            sender = "Sender Name",
            subject = "The subject",
            preview = LoremIpsum(words = 3).values.joinToString(),
            hasAttachments = false,
            selected = false,
            receivedAt = @OptIn(ExperimentalTime::class) Clock.System
                .now()
                .toLocalDateTime(TimeZone.currentSystemDefault()),
        ),
        MessageItemPrevParams(
            sender = "Sender Name",
            subject = "The subject",
            preview = LoremIpsum(words = 3).values.joinToString(),
            hasAttachments = false,
            selected = false,
            receivedAt = @OptIn(ExperimentalTime::class) Clock.System
                .now()
                .minus(1.minutes)
                .toLocalDateTime(TimeZone.currentSystemDefault()),
        ),
        MessageItemPrevParams(
            sender = "Sender Name",
            subject = "The subject",
            preview = LoremIpsum(words = 5).values.joinToString(),
            hasAttachments = true,
            selected = false,
            receivedAt = @OptIn(ExperimentalTime::class) Clock.System
                .now()
                .minus(1.days)
                .toLocalDateTime(TimeZone.currentSystemDefault()),
        ),
        MessageItemPrevParams(
            sender = "Sender Name",
            subject = "The subject",
            preview = LoremIpsum(words = 10).values.joinToString(),
            hasAttachments = false,
            selected = true,
            receivedAt = @OptIn(ExperimentalTime::class) Clock.System
                .now()
                .minus(31.days)
                .toLocalDateTime(TimeZone.currentSystemDefault()),
        ),
        MessageItemPrevParams(
            sender = "Sender Name",
            subject = "The subject",
            preview = LoremIpsum(words = 20).values.joinToString(),
            hasAttachments = true,
            selected = true,
            receivedAt = @OptIn(ExperimentalTime::class) Clock.System
                .now()
                .minus(365.days)
                .toLocalDateTime(TimeZone.currentSystemDefault()),
        ),
    ),
)

@Preview
@Composable
private fun PreviewDefault(
    @PreviewParameter(MessageItemPrevParamCol::class) params: MessageItemPrevParams,
) {
    PreviewWithThemes {
        MessageItem(
            leading = {
                Box(
                    modifier = Modifier
                        .size(MainTheme.sizes.iconAvatar)
                        .padding(MainTheme.spacings.half)
                        .background(color = MainTheme.colors.primary, shape = CircleShape),
                )
            },
            sender = { TextTitleSmall(text = params.sender) },
            subject = { TextLabelLarge(text = params.subject) },
            preview = params.preview,
            action = {
                IconButton(
                    onClick = { },
                    modifier = Modifier.size(MainTheme.sizes.iconLarge),
                ) {
                    Image(imageVector = Icons.Filled.Star, contentDescription = null)
                }
            },
            receivedAt = params.receivedAt,
            onClick = { },
            onLongClick = { },
            modifier = Modifier.padding(MainTheme.spacings.double),
            hasAttachments = params.hasAttachments,
            selected = params.selected,
            colors = MessageItemDefaults.newMessageItemColors(),
        )
    }
}

@Preview
@Composable
private fun PreviewCompact(
    @PreviewParameter(MessageItemPrevParamCol::class) params: MessageItemPrevParams,
) {
    PreviewWithThemes {
        MessageItem(
            leading = {
                Box(
                    modifier = Modifier
                        .size(MainTheme.sizes.iconAvatar)
                        .padding(MainTheme.spacings.half)
                        .background(color = MainTheme.colors.primary, shape = CircleShape),
                )
            },
            sender = { TextTitleSmall(text = params.sender) },
            subject = { TextLabelLarge(text = params.subject) },
            preview = params.preview,
            action = {
                IconButton(
                    onClick = { },
                    modifier = Modifier.size(MainTheme.sizes.iconLarge),
                ) {
                    Image(imageVector = Icons.Filled.Star, contentDescription = null)
                }
            },
            receivedAt = params.receivedAt,
            onClick = { },
            onLongClick = { },
            modifier = Modifier.padding(MainTheme.spacings.double),
            hasAttachments = params.hasAttachments,
            selected = params.selected,
            contentPadding = MessageItemDefaults.compactContentPadding,
            colors = MessageItemDefaults.unreadMessageItemColors(),
        )
    }
}

@Preview
@Composable
private fun PreviewRelaxed(
    @PreviewParameter(MessageItemPrevParamCol::class) params: MessageItemPrevParams,
) {
    PreviewWithThemes {
        MessageItem(
            leading = {
                Box(
                    modifier = Modifier
                        .size(MainTheme.sizes.iconAvatar)
                        .padding(MainTheme.spacings.half)
                        .background(color = MainTheme.colors.primary, shape = CircleShape),
                )
            },
            sender = { TextTitleSmall(text = params.sender) },
            subject = { TextLabelLarge(text = params.subject) },
            preview = params.preview,
            action = {
                IconButton(
                    onClick = { },
                    modifier = Modifier.size(MainTheme.sizes.iconLarge),
                ) {
                    Image(imageVector = Icons.Filled.Star, contentDescription = null)
                }
            },
            receivedAt = params.receivedAt,
            onClick = { },
            onLongClick = { },
            modifier = Modifier.padding(MainTheme.spacings.double),
            hasAttachments = params.hasAttachments,
            selected = params.selected,
            contentPadding = MessageItemDefaults.relaxedContentPadding,
            colors = MessageItemDefaults.readMessageItemColors(),
        )
    }
}
