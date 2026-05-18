package net.thunderbird.feature.mail.message.list.ui.component.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemConfiguration
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageSublineConfiguration
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageSublineLeadingIndicator
import net.thunderbird.feature.mail.message.list.ui.component.organism.MessageItemColors
import net.thunderbird.feature.mail.message.list.ui.component.organism.MessageItemDefaults

/**
 * Displays the main content body of a message item, including the subject line and optional excerpt.
 *
 * This composable renders a two-line layout where the first line shows the subject with optional
 * inline content (such as attachment icons or conversation counter badges), and the second line
 * displays the message excerpt if configured. The subject line uses the provided subject color
 * from the [MessageItemColors], while the excerpt uses the body small text style.
 *
 * @param excerpt The excerpt text to display below the subject line.
 * @param configuration The configuration that controls the appearance and behavior of the message
 *  item, including the maximum number of excerpt lines and line-specific configurations.
 * @param modifier The modifier to be applied to the root Row layout.
 * @param subject A composable lambda that renders the subject line, receiving an optional
 *  [AnnotatedString] for prefix content and a map of inline text content for rendering icons or
 *  badges within the text.
 */
@Composable
internal fun MessageBodyContent(
    excerpt: String,
    configuration: MessageItemConfiguration,
    modifier: Modifier = Modifier,
    subject: @Composable (AnnotatedString?, ImmutableMap<String, InlineTextContent>) -> Unit,
) {
    Row(modifier = modifier) {
        Column {
            val (prefixAnnotatedString, inlineContent) = secondaryLineContent(configuration)
            subject(prefixAnnotatedString, inlineContent)
            Spacer(modifier = Modifier.height(MainTheme.spacings.half))
            if (configuration.maxExcerptLines > 0) {
                val inlineContent = rememberInlineContent(configuration.excerptLineConfiguration)
                val prefixAnnotatedString = rememberPrefixAnnotatedString(configuration.excerptLineConfiguration)
                TextBodySmall(
                    text = buildAnnotatedString {
                        append(prefixAnnotatedString)
                        append(excerpt)
                    },
                    maxLines = configuration.maxExcerptLines,
                    overflow = TextOverflow.Ellipsis,
                    inlineContent = inlineContent,
                )
            }
        }
    }
}

@Composable
private fun secondaryLineContent(
    configuration: MessageItemConfiguration,
): Pair<AnnotatedString?, ImmutableMap<String, InlineTextContent>> = if (configuration.maxExcerptLines == 0) {
    val secondaryLineConfiguration = configuration.secondaryLineConfiguration
    rememberPrefixAnnotatedString(secondaryLineConfiguration) to rememberInlineContent(secondaryLineConfiguration)
} else {
    null to persistentMapOf()
}

@Composable
private fun rememberPrefixAnnotatedString(
    configuration: MessageSublineConfiguration,
): AnnotatedString = remember(configuration) {
    buildAnnotatedString {
        configuration.leadingItems.forEach { leadingItem ->
            when (leadingItem) {
                is MessageSublineLeadingIndicator.AttachmentIcon -> {
                    appendInlineContent(
                        id = MessageItemDefaults.ATTACHMENT_ICON_INLINE_COMPOSABLE_ID,
                        alternateText = MessageItemDefaults.ATTACHMENT_ICON_INLINE_COMPOSABLE_REPLACEMENT,
                    )
                    append(" ")
                }

                is MessageSublineLeadingIndicator.ConversationCounterBadge -> {
                    appendInlineContent(
                        id = MessageItemDefaults.CONVERSATION_COUNTER_INLINE_COMPOSABLE_ID,
                        alternateText = MessageItemDefaults.CONVERSATION_COUNTER_INLINE_COMPOSABLE_REPLACEMENT,
                    )
                    append(" ")
                }
            }
        }
    }
}

@Composable
private fun rememberInlineContent(configuration: MessageSublineConfiguration): ImmutableMap<String, InlineTextContent> {
    val badgeHeight = calculateBadgeHeight()
    return remember(configuration) {
        configuration
            .leadingItems
            .associate { leadingItem ->
                when (leadingItem) {
                    is MessageSublineLeadingIndicator.AttachmentIcon ->
                        MessageItemDefaults.ATTACHMENT_ICON_INLINE_COMPOSABLE_ID to InlineTextContent(
                            Placeholder(
                                width = 16.sp,
                                height = 16.sp,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                            ),
                        ) {
                            Icon(imageVector = Icons.Outlined.Attachment, contentDescription = null)
                        }

                    is MessageSublineLeadingIndicator.ConversationCounterBadge ->
                        MessageItemDefaults.CONVERSATION_COUNTER_INLINE_COMPOSABLE_ID to InlineTextContent(
                            Placeholder(
                                width = calculateConversationCounterBadgeWidth(leadingItem.count),
                                height = badgeHeight,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                            ),
                        ) {
                            MessageConversationCounterBadge(leadingItem.count, leadingItem.color)
                        }
                }
            }
            .toImmutableMap()
    }
}

@Composable
private fun calculateBadgeHeight(): TextUnit {
    val fontSize = MainTheme.typography.bodySmall.fontSize
    val badgeHeight = with(LocalDensity.current) {
        (fontSize.toDp() + (MESSAGE_CONVERSATION_COUNTER_BADGE_PADDING.dp * 2)).toSp()
    }
    return badgeHeight
}

private const val TEN_QUANTITY = 10
private const val HUNDRED_QUANTITY = 100
private fun calculateConversationCounterBadgeWidth(count: Int): TextUnit = when (count) {
    in 1..<TEN_QUANTITY -> 14.sp
    in TEN_QUANTITY..<HUNDRED_QUANTITY -> 22.sp
    else -> 28.sp
}
