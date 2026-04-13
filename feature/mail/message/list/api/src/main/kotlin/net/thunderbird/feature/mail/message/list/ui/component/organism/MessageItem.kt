package net.thunderbird.feature.mail.message.list.ui.component.organism

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIconDefaults
import kotlinx.collections.immutable.ImmutableMap
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.theme2.contentColorFor
import net.thunderbird.feature.mail.message.list.ui.component.atom.FavouriteButtonIcon
import net.thunderbird.feature.mail.message.list.ui.component.atom.MESSAGE_BADGE_SIZE
import net.thunderbird.feature.mail.message.list.ui.component.atom.NewMessageBadge
import net.thunderbird.feature.mail.message.list.ui.component.atom.UnreadMessageBadge
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageBadgeStyle
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemConfiguration
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemLeadingConfiguration
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemTrailingElement
import net.thunderbird.feature.mail.message.list.ui.component.molecule.AdaptiveMessageItemHeaderRow
import net.thunderbird.feature.mail.message.list.ui.component.molecule.MessageBodyContent
import net.thunderbird.feature.mail.message.list.ui.component.molecule.MessageItemAvatarCircle
import net.thunderbird.feature.mail.message.list.ui.component.molecule.MessageItemAvatarCircleDefaults

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
    Surface(
        modifier = modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .borderBottom(width = 1.dp, color = MainTheme.colors.outlineVariant),
        color = colors.containerColor,
        contentColor = colors.contentColor,
    ) {
        Row(
            modifier = Modifier
                .padding(contentPadding)
                .height(IntrinsicSize.Min),
        ) {
            // Unread/New Indicator and Sender Avatar
            LeadingElements(
                selected = selected,
                configuration = configuration.leadingConfiguration,
                onClick = onAvatarClick,
            )
            Spacer(Modifier.width(MainTheme.spacings.default))
            // Message Content and Contents
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.half, Alignment.CenterVertically),
            ) {
                AdaptiveMessageItemHeaderRow(configuration, receivedAt, firstLine)
                MessageBodyContent(
                    excerpt = excerpt,
                    configuration = configuration,
                    subject = secondaryLine,
                )
            }
            Spacer(Modifier.width(MainTheme.spacings.half))
            // Message controls and interaction items
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = MainTheme.sizes.minTouchTarget),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                configuration.trailingConfiguration.elements.forEach { element ->
                    when (element) {
                        is MessageItemTrailingElement.EncryptedBadge -> Icon(
                            imageVector = Icons.Outlined.Encrypted,
                            contentDescription = null,
                        )

                        is MessageItemTrailingElement.FavouriteIconButton -> FavouriteButtonIcon(
                            favourite = element.favourite,
                            onFavouriteChange = { onTrailingClick(element) },
                            size = MainTheme.sizes.minTouchTarget,
                            modifier = Modifier.height(MainTheme.sizes.minTouchTarget / 2),
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.borderBottom(width: Dp, color: Color): Modifier = this then Modifier
    .drawWithCache {
        onDrawWithContent {
            drawContent()
            drawOutline(
                outline = Outline.Rectangle(
                    rect = Rect(
                        offset = Offset(x = 0f, y = size.height - width.toPx()),
                        size = Size(width = size.width, height = width.toPx()),
                    ),
                ),
                color = color,
            )
        }
    }

@Composable
private fun LeadingElements(
    selected: Boolean,
    configuration: MessageItemLeadingConfiguration,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.quarter),
        modifier = modifier.fillMaxHeight(),
    ) {
        when (configuration.badgeStyle) {
            MessageBadgeStyle.New -> NewMessageBadge()

            MessageBadgeStyle.Unread -> UnreadMessageBadge()

            null -> Spacer(Modifier.width(MESSAGE_BADGE_SIZE.dp))
        }
        AnimatedContent(targetState = selected) { selected ->
            when {
                selected -> ButtonIcon(
                    onClick = onClick,
                    imageVector = Icons.Outlined.Check,
                    colors = ButtonIconDefaults.buttonIconFilledColors(
                        containerColor = MainTheme.colors.secondaryContainer,
                        contentColor = contentColorFor(backgroundColor = MainTheme.colors.secondaryContainer),
                    ),
                    modifier = Modifier
                        .size(MainTheme.sizes.iconAvatar)
                        .padding(MainTheme.spacings.half),
                )

                configuration.avatar != null ->
                    MessageItemAvatarCircle(
                        avatar = configuration.avatar,
                        colors = MessageItemAvatarCircleDefaults.colorsFrom(
                            configuration.avatarColor ?: MainTheme.colors.secondaryContainer,
                        ),
                        onClick = onClick,
                    )

                else -> Unit
            }
        }
    }
}
