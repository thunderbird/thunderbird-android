package app.k9mail.feature.account.avatar.ui

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.ColorRoles
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.core.ui.compose.theme2.toColorRoles

@Composable
fun AvatarOutlined(
    color: Color,
    name: String,
    modifier: Modifier = Modifier,
    size: AvatarSize = AvatarSize.MEDIUM,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val colorRoles = color.toColorRoles(context)

    AvatarLayout(
        color = color,
        colorRoles = colorRoles,
        onClick = onClick,
        modifier = modifier.size(getAvatarSize(size)),
    ) {
        AvatarPlaceholder(
            displayName = name,
            color = color,
            size = size,
        )
        // TODO: Add image loading
    }
}

@Composable
private fun AvatarLayout(
    color: Color,
    colorRoles: ColorRoles,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        color = colorRoles.accentContainer,
        modifier = modifier
            .clip(CircleShape)
            .border(
                width = 2.dp,
                shape = CircleShape,
                color = color,
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
