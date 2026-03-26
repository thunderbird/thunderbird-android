package net.thunderbird.feature.mail.message.list.ui.component.organism

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
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
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIconDefaults
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.theme2.LocalContentColor
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.mail.message.list.ui.component.atom.FavouriteButtonIcon
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemConfiguration
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemTrailingElement
import net.thunderbird.feature.mail.message.list.ui.component.molecule.AdaptiveMessageItemHeaderRow

/**
 * Displays a message item in a list with configurable layout and interactions.
 *
 * The layout consists of three main areas:
 * - Leading area: Contains selection indicator and sender avatar
 * - Content area:
 *   - Primary line, usually: sender name, date
 *   - Secondary line, usually: subject, and message excerpt
 * - Trailing area: Contains action buttons like favourite and attachment
 *   indicators
 *
 * ```
 * Message Item structure:
 * ┌───────────┬──────────────────────┬──────────┐
 * │  Leading  │  Primary Line        │ Trailing │
 * │   Area    ├──────────────────────┤   Area   │
 * │           │  Secondary Line      │          │
 * │           │  Excerpt Line        │          │
 * └───────────┴──────────────────────┴──────────┘
 * ```
 *
 * @param firstLine Composable content for the first line, typically displaying
 *  the sender name
 * @param secondaryLine Composable content for the secondary line, typically
 *  displaying the subject, with optional prefix and inline content support
 * @param excerpt The preview text of the message body
 * @param receivedAt The timestamp or formatted date string indicating when the
 *  message was received
 * @param configuration The configuration object defining visual presentation and
 *  layout settings
 * @param modifier The modifier to be applied to the message item
 * @param colors The color scheme to be applied to the message item components
 * @param selected Whether the message item is currently in a selected state
 * @param contentPadding The padding values to be applied around the message item
 *  content
 * @param onClick Callback invoked when the message item is clicked
 * @param onLongClick Callback invoked when the message item is long-pressed
 * @param onAvatarClick Callback invoked when the sender avatar is clicked
 * @param onTrailingClick Callback invoked when a trailing element is clicked,
 *  providing the specific element that was interacted with
 */
@Composable
@Suppress("LongMethod")
internal fun MessageItem(
    firstLine: @Composable () -> Unit,
    secondaryLine: @Composable (
        prefix: AnnotatedString?,
        inlineContent: ImmutableMap<String, InlineTextContent>,
    ) -> Unit,
    excerpt: String,
    receivedAt: String,
    configuration: MessageItemConfiguration,
    modifier: Modifier = Modifier,
    colors: MessageItemColors = MessageItemDefaults.readMessageItemColors(),
    selected: Boolean = false,
    contentPadding: PaddingValues = MessageItemDefaults.defaultContentPadding,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onTrailingClick: (MessageItemTrailingElement) -> Unit = {},
) {
    // NOTE: The current implementation changes are just to fix the compilation errors the API changes have caused.
    // The actual implementation will be done in a follow-up PR.
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
        Row(modifier = Modifier.padding(contentPadding)) {
            // Unread/New Indicator and Sender Avatar
            Column(verticalArrangement = Arrangement.Center) {
                LeadingElements(selected, onAvatarClick, leading = {})
            }
            Spacer(modifier = Modifier.width(MainTheme.spacings.default))
            // Message Content and Contents
            Column(
                modifier = Modifier
                    .weight(1f)
                    .onPlaced { contentStart = it.positionInParent().x },
            ) {
                AdaptiveMessageItemHeaderRow(configuration, receivedAt, firstLine)
                MessageContent(
                    colors = colors,
                    preview = excerpt,
                    maxPreviewLines = configuration.maxExcerptLines,
                    subject = {
                        secondaryLine(null, persistentMapOf())
                    },
                )
            }
            Spacer(modifier = Modifier.width(MainTheme.spacings.default))
            // Message controls and interaction items
            TrailingElements(
                action = {
                    FavouriteButtonIcon(
                        favourite = false,
                        onFavouriteChange = { onTrailingClick(MessageItemTrailingElement.FavouriteIconButton(it)) },
                        size = MainTheme.sizes.minTouchTarget,
                    )
                },
                hasAttachments = false,
                modifier = Modifier.heightIn(min = MainTheme.sizes.large),
            )
        }
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
            contentColor = MainTheme.colors.onSecondaryContainer,
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
