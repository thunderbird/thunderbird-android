package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.organism.drawer.NavigationDrawerItem
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.feature.account.avatar.ui.Avatar
import net.thunderbird.feature.account.avatar.ui.AvatarSize
import net.thunderbird.feature.account.avatar.ui.rememberCompatAvatar
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.getDisplayAccountColor
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.getDisplayAccountName

@Composable
internal fun AccountListItem(
    account: DisplayAccount,
    onClick: (DisplayAccount) -> Unit,
    selected: Boolean,
    showStarredCount: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = getDisplayAccountColor(account)
    val name = getDisplayAccountName(account)
    val compatAvatar = rememberCompatAvatar(null, name)

    NavigationDrawerItem(
        label = { AccountLabel(account = account) },
        selected = selected,
        onClick = { onClick(account) },
        modifier = modifier
            .fillMaxWidth()
            .height(MainTheme.sizes.large),
        icon = {
            Avatar(
                avatar = compatAvatar,
                color = color,
                size = AvatarSize.MEDIUM,
            )
        },
        badge = {
            Crossfade(account.hasError) { hasError ->
                if (hasError) {
                    Icon(
                        imageVector = Icons.DualTone.Warning,
                        tint = if (selected) {
                            MainTheme.colors.onSecondaryContainer
                        } else {
                            MainTheme.colors.error
                        },
                    )
                } else {
                    AccountListItemBadge(
                        unreadCount = account.unreadMessageCount,
                        starredCount = account.starredMessageCount,
                        showStarredCount = showStarredCount,
                    )
                }
            }
        },
    )
}

@Composable
private fun AccountLabel(
    account: DisplayAccount,
    modifier: Modifier = Modifier,
) {
    val name = getDisplayAccountName(account)

    Column(
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
        modifier = modifier.fillMaxWidth(),
    ) {
        TextBodyLarge(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(name)
                }
            },
        )
        if (account is MailDisplayAccount && account.name != account.email) {
            TextBodyMedium(
                text = account.email,
            )
        }
    }
}
