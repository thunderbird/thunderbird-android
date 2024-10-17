package app.k9mail.feature.navigation.drawer.ui.account

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
internal fun AccountAvatar(
    account: DisplayAccount,
    onClick: (DisplayAccount) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val accountColor = calculateAccountColor(account.account.chipColor)
    val accountColorRoles = accountColor.toColorRoles(context)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd,
    ) {
        Surface(
            modifier = Modifier
                .size(MainTheme.sizes.iconAvatar)
                .border(2.dp, accountColor, CircleShape)
                .padding(2.dp)
                .clickable(onClick = { onClick(account) }),
            color = accountColor.copy(alpha = 0.3f),
            shape = CircleShape,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .border(2.dp, MainTheme.colors.surfaceContainerLowest, CircleShape),
            ) {
                Placeholder(
                    email = account.account.email,
                )
                // TODO: Add image loading
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
    email: String,
    modifier: Modifier = Modifier,
) {
    TextTitleMedium(
        text = extractDomainInitials(email).uppercase(),
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

private fun extractDomainInitials(email: String): String {
    return email.split("@")[1].take(2)
}
