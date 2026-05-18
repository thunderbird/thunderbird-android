package net.thunderbird.feature.mail.message.list.ui.component.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import net.thunderbird.core.ui.compose.theme2.MainTheme

internal const val MESSAGE_BADGE_SIZE = 14

/**
 * A composable function that displays a badge indicator for new messages.
 *
 * The badge is rendered as a small circular shape filled with the primary theme color,
 * typically used to indicate the presence of new unread messages in a message list or conversation.
 *
 * @param modifier The modifier to be applied to the badge container.
 */
@Composable
fun NewMessageBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size = MESSAGE_BADGE_SIZE.dp)
            .padding(all = 1.dp)
            .clip(CircleShape)
            .background(MainTheme.colors.primary),
    )
}

/**
 * A composable function that displays a badge indicator for unread messages.
 *
 * The badge is rendered as a small circular shape with a surface background color and
 * a primary-colored border, typically used to indicate the presence of unread messages
 * that are not new in a message list or conversation.
 *
 * @param modifier The modifier to be applied to the badge container.
 */
@Composable
fun UnreadMessageBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size = MESSAGE_BADGE_SIZE.dp)
            .padding(MainTheme.spacings.quarter)
            .clip(CircleShape)
            .background(MainTheme.colors.surface)
            .border(width = 2.dp, color = MainTheme.colors.primary, shape = CircleShape),
    )
}
