package net.thunderbird.feature.mail.message.list.ui.component.molecule

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import net.thunderbird.components.ui.bolt.atom.text.TextBodySmall
import net.thunderbird.components.ui.bolt.atom.text.TextTitleSmall
import net.thunderbird.feature.mail.message.list.ui.state.ComposedAddressStyle
import net.thunderbird.feature.mail.message.list.ui.state.ComposedAddressUi

/**
 * Displays the sender or subject of a message item with small title styling.
 *
 * The component shows either the sender or subject based on the swap parameter,
 * with an optional thread count indicator.
 *
 * Text is displayed with ellipsis overflow when it exceeds available space.
 */
@Composable
internal fun MessageItemSenderSubjectFirstLine(
    senders: ComposedAddressUi,
    subject: AnnotatedString,
    useSender: Boolean,
    modifier: Modifier = Modifier,
    forceRegularFontWeight: Boolean = false,
) {
    val text = styledSenderOrSubject(
        useSender = useSender,
        senders = senders,
        subject = subject,
        prefix = null,
        forceRegularFontWeight = forceRegularFontWeight,
    )
    if (text.isPlain()) {
        // Plain strings take a cheaper text layout path than annotated ones.
        TextTitleSmall(text = text.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
    } else {
        TextTitleSmall(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
    }
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
 * @param modifier The modifier to be applied to the composable
 * @param color The text color to use for the displayed content
 */
@Composable
internal fun MessageItemSenderSubjectSecondLine(
    senders: ComposedAddressUi,
    subject: AnnotatedString,
    useSender: Boolean,
    modifier: Modifier = Modifier,
    prefix: AnnotatedString? = null,
    inlineContent: ImmutableMap<String, InlineTextContent> = persistentMapOf(),
) {
    val text = styledSenderOrSubject(useSender = useSender, senders = senders, subject = subject, prefix = prefix)
    if (text.isPlain()) {
        // Plain strings take a cheaper text layout path than annotated ones.
        TextBodySmall(text = text.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
    } else {
        TextBodySmall(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
            inlineContent = inlineContent,
        )
    }
}

@Composable
private fun styledSenderOrSubject(
    useSender: Boolean,
    senders: ComposedAddressUi,
    subject: AnnotatedString,
    prefix: AnnotatedString? = null,
    forceRegularFontWeight: Boolean = false,
): AnnotatedString = remember(useSender, senders, subject, prefix, forceRegularFontWeight) {
    buildStyledSenderOrSubject(useSender, senders, subject, prefix, forceRegularFontWeight)
}

internal fun buildStyledSenderOrSubject(
    useSender: Boolean,
    senders: ComposedAddressUi,
    subject: AnnotatedString,
    prefix: AnnotatedString?,
    forceRegularFontWeight: Boolean,
): AnnotatedString = buildAnnotatedString {
    prefix?.let { append(it) }
    // Style offsets are relative to the sender or subject, which starts after the prefix.
    val offset = prefix?.length ?: 0
    val text = if (useSender) senders.displayName else subject
    append(text)
    val end = offset + text.length
    when {
        forceRegularFontWeight -> {
            addStyle(style = SpanStyle(fontWeight = FontWeight.Normal), start = offset, end = end)
        }

        useSender -> {
            senders.displayNameStyles.forEach { style ->
                when (style) {
                    ComposedAddressStyle.AllBold -> addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold),
                        start = offset,
                        end = end,
                    )

                    is ComposedAddressStyle.Bold -> addStyle(
                        style = SpanStyle(fontWeight = FontWeight.Bold),
                        start = offset + style.start,
                        end = style.end?.let { offset + it } ?: end,
                    )

                    is ComposedAddressStyle.Regular -> addStyle(
                        style = SpanStyle(fontWeight = FontWeight.Normal),
                        start = offset + style.start,
                        end = style.end?.let { offset + it } ?: end,
                    )
                }
            }
        }
    }
}

private fun AnnotatedString.isPlain(): Boolean =
    spanStyles.isEmpty() && paragraphStyles.isEmpty() && getStringAnnotations(0, length).isEmpty() &&
        !hasLinkAnnotations(0, length)
