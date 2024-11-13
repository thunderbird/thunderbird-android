package app.k9mail.feature.navigation.drawer.ui.account

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.ColorRoles
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.core.ui.compose.theme2.toColorRoles
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount
import app.k9mail.feature.navigation.drawer.ui.common.labelForCount

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

    val avatarSize by animateDpAsState(
        targetValue = if (selected) selectedAvatarSize else MainTheme.sizes.iconAvatar,
        label = "Avatar size",
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd,
    ) {
        val clickableModifier = if (onClick != null && !selected) Modifier.clickable { onClick(account) } else Modifier

        Box(
            modifier = Modifier
                .size(MainTheme.sizes.iconAvatar),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .border(2.dp, accountColor, CircleShape)
                    .padding(2.dp)
                    .then(clickableModifier),
                color = accountColor.copy(alpha = 0.3f),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .border(2.dp, MainTheme.colors.surfaceContainerLowest, CircleShape),
                ) {
                    Placeholder(
                        displayName = account.name,
                    )
                    // TODO: Add image loading
                }
            }
        }

        UnreadBadge(
            unreadCount = account.unreadMessageCount,
            accountColorRoles = accountColorRoles,
        )
    }
}

@Composable
private fun Placeholder(
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
