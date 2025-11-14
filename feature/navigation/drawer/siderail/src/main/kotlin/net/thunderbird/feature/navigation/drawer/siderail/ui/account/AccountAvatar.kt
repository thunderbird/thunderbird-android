package net.thunderbird.feature.navigation.drawer.siderail.ui.account

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.ColorRoles
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.core.ui.compose.theme2.toColorRoles
import net.thunderbird.feature.navigation.drawer.siderail.domain.entity.DisplayAccount
import net.thunderbird.feature.navigation.drawer.siderail.ui.common.labelForCount

val selectedAvatarSize = 40.dp

@Composable
internal fun AccountAvatar(
    account: DisplayAccount,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: ((DisplayAccount) -> Unit)? = null,
) {
    val context = LocalContext.current
    val accountColor = calculateAccountColor(account.color)
    val accountColorRoles = accountColor.toColorRoles(context)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd,
    ) {
        SideRailAvatar(
            color = accountColor,
            name = account.name,
            onClick = onClick?.let { { onClick(account) } },
            selected = selected,
        )
        UnreadBadge(
            unreadCount = account.unreadMessageCount,
            accountColorRoles = accountColorRoles,
        )
    }
}

@Composable
private fun SideRailAvatar(
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

@Composable
private fun UnreadBadge(
    unreadCount: Int,
    accountColorRoles: ColorRoles,
    modifier: Modifier = Modifier,
) {
    if (unreadCount > 0) {
        val resources = LocalContext.current.resources

        Surface(
            color = accountColorRoles.accent,
            shape = CircleShape,
            modifier = modifier,
        ) {
            TextLabelSmall(
                text = labelForCount(
                    count = unreadCount,
                    resources = resources,
                ),
                color = accountColorRoles.onAccent,
                modifier = Modifier.padding(
                    horizontal = 3.dp,
                    vertical = 2.dp,
                ),
            )
        }
    }
}

private fun extractNameInitials(displayName: String): String {
    return displayName.take(2)
}
