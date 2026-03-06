package net.thunderbird.feature.mail.message.list.ui.component.molecule

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
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
    TextTitleSmall(
        text = styledSenderOrSubject(
            useSender = useSender,
            senders = senders,
            subject = subject,
            prefix = null,
            forceRegularFontWeight = forceRegularFontWeight,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
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
    TextBodySmall(
        text = styledSenderOrSubject(useSender = useSender, senders = senders, subject = subject, prefix = prefix),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
        inlineContent = inlineContent,
    )
}

@Composable
private fun styledSenderOrSubject(
    useSender: Boolean,
    senders: ComposedAddressUi,
    subject: AnnotatedString,
    prefix: AnnotatedString? = null,
    forceRegularFontWeight: Boolean = false,
): AnnotatedString = buildAnnotatedString {
    prefix?.let { append(it) }
    val text = if (useSender) senders.displayName else subject
    append(text)
    when {
        forceRegularFontWeight -> {
            addStyle(SpanStyle(fontWeight = FontWeight.Normal), 0, text.length)
        }

        useSender -> {
            senders.displayNameStyles.forEach { style ->
                when (style) {
                    ComposedAddressStyle.AllBold -> addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold),
                        start = 0,
                        end = text.length,
                    )

                    is ComposedAddressStyle.Bold -> addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold),
                        style.start,
                        style.end ?: text.length,
                    )

                    is ComposedAddressStyle.Regular -> addStyle(
                        SpanStyle(fontWeight = FontWeight.Normal),
                        style.start,
                        style.end ?: text.length,
                    )
                }
            }
        }
    }
}
