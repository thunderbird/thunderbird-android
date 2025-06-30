package net.thunderbird.feature.account.avatar.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.core.ui.compose.theme2.toSurfaceContainer

private const val AVATAR_ALPHA = 0.2f

@Composable
fun AvatarOutlined(
    color: Color,
    name: String,
    modifier: Modifier = Modifier,
    size: AvatarSize = AvatarSize.MEDIUM,
    onClick: (() -> Unit)? = null,
) {
    val avatarColor = calculateAvatarColor(color)
    val containerColor = avatarColor.toSurfaceContainer(alpha = AVATAR_ALPHA)

    AvatarLayout(
        color = containerColor,
        borderColor = avatarColor,
        onClick = onClick,
        modifier = modifier.size(getAvatarSize(size)),
    ) {
        AvatarPlaceholder(
            color = avatarColor,
            displayName = name,
            size = size,
        )
        // TODO: Add image loading
    }
}

@Composable
private fun AvatarLayout(
    color: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        color = color,
        shape = CircleShape,
        modifier = modifier
            .border(
                width = 2.dp,
                shape = CircleShape,
                color = borderColor,
            )
            .clickable(
                enabled = onClick != null,
                onClick = { onClick?.invoke() },
            ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun AvatarPlaceholder(
    color: Color,
    displayName: String,
    size: AvatarSize,
    modifier: Modifier = Modifier,
) {
    when (size) {
        AvatarSize.MEDIUM -> {
            TextTitleMedium(
                text = extractNameInitials(displayName).uppercase(),
                color = color,
                modifier = modifier,
            )
        }

        AvatarSize.LARGE -> {
            TextTitleLarge(
                text = extractNameInitials(displayName).uppercase(),
                color = color,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun getAvatarSize(size: AvatarSize): Dp {
    return when (size) {
        AvatarSize.MEDIUM -> MainTheme.sizes.iconAvatar
        AvatarSize.LARGE -> MainTheme.sizes.large
    }
}

private fun extractNameInitials(displayName: String): String {
    return displayName.take(2)
}
