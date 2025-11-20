package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.theme2.ColorRoles
import net.thunderbird.feature.account.avatar.ui.Avatar
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.getDisplayAccountColor
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.getDisplayAccountName
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.labelForCount

@Composable
internal fun AccountAvatar(
    account: DisplayAccount,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: ((DisplayAccount) -> Unit)? = null,
) {
    val name = getDisplayAccountName(account)
    val color = getDisplayAccountColor(account)
    val accountColor = rememberCalculatedAccountColor(color)
    val accountColorRoles = rememberCalculatedAccountColorRoles(accountColor)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd,
    ) {
        Avatar(
            color = accountColor,
            name = name,
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
        val resources = LocalResources.current

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
