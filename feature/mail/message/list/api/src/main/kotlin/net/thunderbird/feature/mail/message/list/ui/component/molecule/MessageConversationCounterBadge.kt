package net.thunderbird.feature.mail.message.list.ui.component.molecule

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.theme2.MainTheme

/**
 * A composable that displays a message conversation counter badge with a customizable appearance.
 *
 * This component renders a counter within a styled surface container, displaying the count value
 * up to a specified limit. When the count exceeds the limit, it shows the limit value followed by
 * a "+" symbol (e.g., "99+").
 *
 * The counter uses a large shape from the theme and includes a 1dp border. The text is rendered
 * using a small label style with half-spacing padding.
 *
 * @param count The numeric value to display in the counter badge
 * @param color The color scheme defining the container, content, and border colors for the counter
 * @param modifier The modifier to be applied to the component
 * @param limit The maximum value to display before showing a "+" suffix; defaults to 99
 */
@Composable
internal fun MessageConversationCounterBadge(
    count: Int,
    color: MessageConversationCounterBadgeColor,
    modifier: Modifier = Modifier,
    limit: Int = MessageConversationCounterBadgeDefaults.CONVERSATION_COUNTER_LIMIT,
) {
    Surface(
        color = color.containerColor,
        contentColor = color.contentColor,
        shape = MainTheme.shapes.large,
        modifier = modifier.border(
            width = 1.dp,
            color = color.borderColor ?: color.containerColor,
            shape = MainTheme.shapes.large,
        ),
    ) {
        TextLabelSmall(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                    append(count.coerceAtMost(limit).toString())
                    if (count > limit) {
                        append("+")
                    }
                }
            },
            modifier = Modifier.padding(horizontal = MainTheme.spacings.half, vertical = MainTheme.spacings.quarter),
        )
    }
}

/**
 * A composable that displays a counter badge specifically styled for new messages in a conversation.
 *
 * This is a convenience wrapper around MessageConversationCounterBadge that applies the default
 * new message color scheme using primary theme colors. The counter displays the provided count
 * value and automatically adds a "+" suffix when the count exceeds the default limit.
 *
 * @param count The number of new messages to display in the badge
 * @param modifier The modifier to be applied to the component
 */
@Composable
fun NewMessageConversationCounterBadge(count: Int, modifier: Modifier = Modifier) {
    MessageConversationCounterBadge(
        count = count,
        color = MessageConversationCounterBadgeDefaults.newMessageColor(),
        modifier = modifier,
    )
}

/**
 * A composable that displays a counter badge specifically styled for read messages in a conversation.
 *
 * This is a convenience wrapper around MessageConversationCounterBadge that applies the default
 * read message color scheme using primary theme colors. The counter displays the provided count
 * value and automatically adds a "+" suffix when the count exceeds the default limit.
 *
 * @param count The number of read messages to display in the badge
 * @param modifier The modifier to be applied to the component
 */
@Composable
fun ReadMessageConversationCounterBadge(count: Int, modifier: Modifier = Modifier) {
    MessageConversationCounterBadge(
        count = count,
        color = MessageConversationCounterBadgeDefaults.readMessageColor(),
        modifier = modifier,
    )
}

/**
 * A composable that displays a counter badge specifically styled for unread messages in a conversation.
 *
 * This is a convenience wrapper around MessageConversationCounterBadge that applies the default
 * unread message color scheme using primary theme colors. The counter displays the provided count
 * value and automatically adds a "+" suffix when the count exceeds the default limit.
 *
 * @param count The number of unread messages to display in the badge
 * @param modifier The modifier to be applied to the component
 */
@Composable
fun UnreadMessageConversationCounterBadge(count: Int, modifier: Modifier = Modifier) {
    MessageConversationCounterBadge(
        count = count,
        color = MessageConversationCounterBadgeDefaults.unreadMessageColor(),
        modifier = modifier,
    )
}

/**
 * Provides default color configurations for message counter components.
 *
 * This object contains factory methods that create MessageConversationCounterBadgeColor instances with predefined
 * color schemes based on the application's theme. Each method returns a different color configuration
 * suitable for different message counter states.
 */
object MessageConversationCounterBadgeDefaults {
    const val CONVERSATION_COUNTER_LIMIT = 99

    /**
     * Creates a [MessageConversationCounterBadgeColor] that represent a new message item counter.
     *
     * @param containerColor The container color of this [MessageConversationCounterBadge].
     * @param contentColor The content color of this [MessageConversationCounterBadge].
     * @param borderColor The border color of this [MessageConversationCounterBadge].
     */
    @Composable
    fun newMessageColor(
        containerColor: Color = MainTheme.colors.primary,
        contentColor: Color = MainTheme.colors.onPrimary,
        borderColor: Color? = null,
    ): MessageConversationCounterBadgeColor = MessageConversationCounterBadgeColor(
        contentColor = contentColor,
        containerColor = containerColor,
        borderColor = borderColor,
    )

    /**
     * Creates a [MessageConversationCounterBadgeColor] that represent a read message item counter.
     *
     * @param containerColor The container color of this [MessageConversationCounterBadge].
     * @param contentColor The content color of this [MessageConversationCounterBadge].
     * @param borderColor The border color of this [MessageConversationCounterBadge].
     */
    @Composable
    fun readMessageColor(
        containerColor: Color = MainTheme.colors.surfaceContainerLow,
        contentColor: Color = MainTheme.colors.onSurface,
        borderColor: Color? = MainTheme.colors.outline,
    ): MessageConversationCounterBadgeColor = MessageConversationCounterBadgeColor(
        contentColor = contentColor,
        containerColor = containerColor,
        borderColor = borderColor,
    )

    /**
     * Creates a [MessageConversationCounterBadgeColor] that represent a read message item counter.
     *
     * @param containerColor The container color of this [MessageConversationCounterBadge].
     * @param contentColor The content color of this [MessageConversationCounterBadge].
     * @param borderColor The border color of this [MessageConversationCounterBadge].
     */
    @Composable
    fun unreadMessageColor(
        containerColor: Color = MainTheme.colors.inverseSurface,
        contentColor: Color = MainTheme.colors.inverseOnSurface,
        borderColor: Color? = null,
    ): MessageConversationCounterBadgeColor = MessageConversationCounterBadgeColor(
        contentColor = contentColor,
        containerColor = containerColor,
        borderColor = borderColor,
    )
}

/**
 * Defines the color scheme for a message counter component.
 *
 * This data class encapsulates the colors used to render a message counter,
 * including the text/content color, background color, and an optional border color.
 *
 * @property containerColor The color used for the counter's background
 * @property contentColor The color used for the counter's content (typically text or icons)
 * @property borderColor The optional color used for the counter's border; when `null`, no border is applied
 */
data class MessageConversationCounterBadgeColor(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color? = null,
)
