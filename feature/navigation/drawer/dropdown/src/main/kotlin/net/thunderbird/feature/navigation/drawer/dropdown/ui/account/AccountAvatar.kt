package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.thunderbird.feature.account.avatar.ui.Avatar
import net.thunderbird.feature.account.avatar.ui.AvatarSize
import net.thunderbird.feature.account.avatar.ui.rememberCompatAvatar
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.getDisplayAccountAvatar
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.getDisplayAccountColor
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.getDisplayAccountName

@Composable
internal fun AccountAvatar(
    account: DisplayAccount,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: ((DisplayAccount) -> Unit)? = null,
    showBadge: Boolean = false,
) {
    val name = getDisplayAccountName(account)
    val color = getDisplayAccountColor(account)
    val accountColor = rememberCalculatedAccountColor(color)
    val accountColorRoles = rememberCalculatedAccountColorRoles(accountColor)
    val avatar = getDisplayAccountAvatar(account)
    val compatAvatar = rememberCompatAvatar(avatar, name)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd,
    ) {
        Avatar(
            avatar = compatAvatar,
            color = accountColor,
            size = AvatarSize.MEDIUM,
            selected = selected,
            onClick = onClick?.let {
                { onClick(account) }
            },
        )
        if (showBadge) {
            UnreadBadge(
                unreadCount = account.unreadMessageCount,
                accountColorRoles = accountColorRoles,
            )
        }
    }
}
