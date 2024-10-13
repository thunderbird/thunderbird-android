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
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount

@Composable
internal fun AccountAvatar(
    account: DisplayAccount,
    onClick: (DisplayAccount) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accountColor = calculateAccountColor(account.account.chipColor)

    Surface(
        modifier = modifier
            .size(MainTheme.sizes.iconAvatar)
            .border(2.dp, accountColor, CircleShape)
            .padding(2.dp),
        color = accountColor.copy(alpha = 0.3f),
        shape = CircleShape,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .border(2.dp, MainTheme.colors.surfaceContainerLowest, CircleShape)
                .clickable(onClick = { onClick(account) }),
        ) {
            Placeholder(
                email = account.account.email,
            )
            // TODO: Add image loading
        }
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

private fun extractDomainInitials(email: String): String {
    return email.split("@")[1].take(2)
}
