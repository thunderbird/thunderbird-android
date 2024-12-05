package app.k9mail.feature.account.avatar.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme

val selectedAvatarSize = 40.dp

@Composable
fun Avatar(
    color: Color,
    name: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val avatarSize by animateDpAsState(
        targetValue = if (selected) selectedAvatarSize else MainTheme.sizes.iconAvatar,
        label = "Avatar size",
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(enabled = onClick != null && !selected, onClick = { onClick?.invoke() }),
        contentAlignment = Alignment.Center,
    ) {
        AvatarOutline(
            color = color,
            modifier = Modifier.size(avatarSize),
        ) {
            AvatarPlaceholder(
                displayName = name,
            )
            // TODO: Add image loading
        }
    }
}

@Composable
private fun AvatarOutline(
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .clip(CircleShape)
            .border(2.dp, color, CircleShape)
            .padding(2.dp),
        color = color.copy(alpha = 0.3f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, MainTheme.colors.surfaceContainerLowest, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun AvatarPlaceholder(
    displayName: String,
    modifier: Modifier = Modifier,
) {
    TextTitleMedium(
        text = extractNameInitials(displayName).uppercase(),
        modifier = modifier,
    )
}

private fun extractNameInitials(displayName: String): String {
    return displayName.take(2)
}
