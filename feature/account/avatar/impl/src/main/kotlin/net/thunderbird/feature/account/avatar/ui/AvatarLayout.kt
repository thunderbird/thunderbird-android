package net.thunderbird.feature.account.avatar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
internal fun AvatarLayout(
    color: Color,
    backgroundColor: Color,
    size: AvatarSize,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val resolvedSize: Dp = getAvatarSize(size)

    Box(
        modifier = modifier
            .size(resolvedSize)
            .clip(CircleShape)
            .clickable(
                enabled = onClick != null,
                onClick = { onClick?.invoke() },
            )
            .background(
                color = backgroundColor,
                shape = CircleShape,
            )
            .border(
                width = 2.dp,
                shape = CircleShape,
                color = color,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun getAvatarSize(size: AvatarSize): Dp {
    return when (size) {
        AvatarSize.MEDIUM -> MainTheme.sizes.iconAvatar
        AvatarSize.LARGE -> MainTheme.sizes.large
    }
}
