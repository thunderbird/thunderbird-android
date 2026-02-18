package net.thunderbird.core.ui.compose.designsystem.organism.message

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIconDefaults
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import app.k9mail.core.ui.compose.theme2.LocalContentColor
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.designsystem.molecule.message.AccountIndicatorIcon

/**
 * Displays a single message item.
 *
 * This composable function is responsible for rendering a single message item within a list. It includes
 * information such as the sender, subject, preview, received time, and actions.
 *
 * @param leading A composable function to display the leading content (e.g., avatar).
 * @param sender A composable function to display the sender's information.
 * @param subject A composable function to display the message subject.
 * @param preview The message preview text.
 * @param action A composable function to display actions related to the message (e.g., star).
 * @param receivedAt The date and time the message was received.
 * @param showAccountIndicator Whether or not account indicator for universal inbox is shown.
 * @param accountIndicatorColor The color of the account indicator, if shown.
 * @param onClick A callback function to be invoked when the message item is clicked.
 * @param onLongClick A lambda function to be invoked when the message item is long-clicked.
 * @param onLeadingClick A callback function to be invoked when the leading content is clicked.
 * @param colors The colors to be used for the message item. See [MessageItemDefaults].
 * @param modifier The modifier to be applied to the message item.
 * @param hasAttachments A boolean indicating whether the message has attachments.
 *  Defaults to `false`.
 * @param selected A boolean indicating whether the message item is selected.
 *  Defaults to `false`.
 * @param maxPreviewLines The maximum number of lines to display for the message preview.
 *  Defaults to `2`.
 * @param contentPadding The padding to be applied to the content of the message item.
 *  Defaults to [MessageItemDefaults.defaultContentPadding].
 * @see MessageItemDefaults
 */
@Suppress("LongParameterList")
@Composable
internal fun MessageItem(
    leading: @Composable () -> Unit,
    sender: @Composable () -> Unit,
    subject: @Composable () -> Unit,
    preview: CharSequence,
    action: @Composable () -> Unit,
    receivedAt: String,
    showAccountIndicator: Boolean,
    accountIndicatorColor: Color?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onLeadingClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: MessageItemColors = MessageItemDefaults.readMessageItemColors(),
    hasAttachments: Boolean = false,
    selected: Boolean = false,
    maxPreviewLines: Int = 2,
    contentPadding: PaddingValues = MessageItemDefaults.defaultContentPadding,
) {
    val outlineVariant = MainTheme.colors.outlineVariant
    var contentStart by remember { mutableFloatStateOf(0f) }
    val layoutDirection = LocalLayoutDirection.current
    Surface(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    val x = contentStart + contentPadding.calculateStartPadding(layoutDirection).toPx()
                    drawOutline(
                        outline = Outline.Rectangle(
                            rect = Rect(
                                offset = Offset(x = x, y = size.height - 1.dp.toPx()),
                                size = Size(width = size.width - x, height = 1.dp.toPx()),
                            ),
                        ),
                        color = outlineVariant,
                    )
                }
            },
        color = colors.containerColor,
        contentColor = colors.contentColor,
    ) {
        Row(
            modifier = Modifier
                .padding(contentPadding)
                .height(intrinsicSize = IntrinsicSize.Min),
        ) {
            // Unread/New Indicator and Sender Avatar
            Column(verticalArrangement = Arrangement.Center) {
                LeadingElements(selected, onLeadingClick, leading)
            }
            Spacer(modifier = Modifier.width(MainTheme.spacings.default))
            // Message Content and Contents
            Column(modifier = Modifier.weight(1f).onPlaced { contentStart = it.positionInParent().x }) {
                HeaderRow(
                    showAccountIndicator = showAccountIndicator,
                    accountIndicatorColor = accountIndicatorColor,
                    receivedAt = receivedAt,
                    senderOrSubject = sender,
                )
                MessageContent(colors = colors, preview = preview, maxPreviewLines = maxPreviewLines, subject = subject)
            }
            Spacer(modifier = Modifier.width(MainTheme.spacings.default))
            // Message controls and interaction items
            TrailingElements(
                action = action,
                hasAttachments = hasAttachments,
                modifier = Modifier.heightIn(min = MainTheme.sizes.large),
            )
        }
    }
}

@Composable
private fun HeaderRow(
    showAccountIndicator: Boolean,
    accountIndicatorColor: Color?,
    receivedAt: String,
    modifier: Modifier = Modifier,
    senderOrSubject: @Composable () -> Unit,
) {
    FlowRow(
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.Start,
        maxLines = 2,
        modifier = modifier.defaultMinSize(
            minHeight = AccountIndicatorIcon.ACCOUNT_INDICATOR_DEFAULT_HEIGHT,
        ),
    ) {
        SenderText(
            showAccountIndicator = showAccountIndicator,
            accountIndicatorColor = accountIndicatorColor,
            modifier = Modifier.align(Alignment.CenterVertically),
        ) {
            senderOrSubject()
        }
        Spacer(modifier = Modifier.weight(1f))
        MessageItemDate(
            receivedAt = receivedAt,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
    }
}

@Composable
private fun MessageContent(
    preview: CharSequence,
    maxPreviewLines: Int,
    modifier: Modifier = Modifier,
    colors: MessageItemColors = MessageItemDefaults.readMessageItemColors(),
    subject: @Composable () -> Unit,
) {
    Row(modifier = modifier) {
        Column {
            SubjectText(colors.subjectColor) {
                subject()
            }
            Spacer(modifier = Modifier.height(MainTheme.spacings.half))
            PreviewText(preview = preview, maxLines = maxPreviewLines)
        }
    }
}

@Composable
private fun SenderText(
    showAccountIndicator: Boolean,
    accountIndicatorColor: Color?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .wrapContentWidth()
            .defaultMinSize(
                minHeight = AccountIndicatorIcon.ACCOUNT_INDICATOR_DEFAULT_HEIGHT,
            ),
    ) {
        if (showAccountIndicator && accountIndicatorColor != null) {
            AccountIndicatorIcon(accountIndicatorColor)
        }
        content()
    }
}

@Composable
private fun SubjectText(
    subjectColor: Color,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides subjectColor) {
        content()
    }
}

@Composable
private fun PreviewText(
    preview: CharSequence,
    maxLines: Int,
    modifier: Modifier = Modifier,
) {
    when (preview) {
        is AnnotatedString -> TextBodySmall(
            text = preview,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )

        else -> TextBodySmall(
            text = preview.toString(),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    }
}

@Composable
private fun LeadingElements(
    selected: Boolean,
    onClick: () -> Unit,
    leading: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = selected,
        modifier = modifier,
    ) { selected ->
        if (selected) {
            SelectedIcon(onClick = onClick)
        } else {
            leading()
        }
    }
}

@Composable
private fun SelectedIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ButtonIcon(
        onClick = onClick,
        imageVector = Icons.Outlined.Check,
        colors = ButtonIconDefaults.buttonIconFilledColors(
            containerColor = MainTheme.colors.secondaryContainer,
            contentColor = contentColorFor(backgroundColor = MainTheme.colors.secondaryContainer),
        ),
        modifier = modifier,
    )
}

@Composable
private fun TrailingElements(
    action: @Composable (() -> Unit),
    hasAttachments: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
        modifier = modifier,
    ) {
        action()
        if (hasAttachments) {
            Icon(
                imageVector = Icons.Outlined.Attachment,
                modifier = Modifier.size(MainTheme.sizes.icon),
            )
        }
    }
}

@Composable
private fun MessageItemDate(
    receivedAt: String,
    modifier: Modifier = Modifier,
) {
    TextTitleSmall(
        text = receivedAt,
        modifier = modifier,
    )
}
