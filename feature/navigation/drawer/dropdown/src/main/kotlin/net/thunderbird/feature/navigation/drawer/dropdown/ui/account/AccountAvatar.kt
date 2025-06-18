package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.theme2.ColorRoles
import app.k9mail.core.ui.compose.theme2.toColorRoles
import net.thunderbird.feature.account.avatar.ui.Avatar
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.labelForCount

@Composable
internal fun AccountAvatar(
    account: MailDisplayAccount,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: ((MailDisplayAccount) -> Unit)? = null,
) {
    val context = LocalContext.current
    val accountColor = calculateAccountColor(account.color)
    val accountColorRoles = accountColor.toColorRoles(context)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd,
    ) {
        Avatar(
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
