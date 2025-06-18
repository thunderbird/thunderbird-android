package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.organism.drawer.NavigationDrawerItem
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.account.avatar.ui.AvatarOutlined
import net.thunderbird.feature.account.avatar.ui.AvatarSize
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayAccount

@Composable
internal fun AccountListItem(
    account: MailDisplayAccount,
    onClick: (MailDisplayAccount) -> Unit,
    selected: Boolean,
    showStarredCount: Boolean,
    modifier: Modifier = Modifier,
) {
    NavigationDrawerItem(
        label = { AccountLabel(account = account) },
        selected = selected,
        onClick = { onClick(account) },
        modifier = modifier.fillMaxWidth()
            .height(MainTheme.sizes.large),
        icon = {
            AvatarOutlined(
                color = Color(account.color),
                name = account.name,
                size = AvatarSize.MEDIUM,
            )
        },
        badge = {
            AccountListItemBadge(
                unreadCount = account.unreadMessageCount,
                starredCount = account.starredMessageCount,
                showStarredCount = showStarredCount,
            )
        },
    )
}

@Composable
private fun AccountLabel(
    account: MailDisplayAccount,
    modifier: Modifier = Modifier,
) {
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
        if (account.name != account.email) {
            TextBodyMedium(
                text = account.email,
            )
        }
    }
}
