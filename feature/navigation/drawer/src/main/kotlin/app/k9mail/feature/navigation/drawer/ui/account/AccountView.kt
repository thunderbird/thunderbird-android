package app.k9mail.feature.navigation.drawer.ui.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount

@Composable
internal fun AccountView(
    account: DisplayAccount,
    onClick: () -> Unit,
    showAvatar: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .height(intrinsicSize = IntrinsicSize.Max),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(visible = showAvatar) {
            Surface(
                color = MainTheme.colors.surfaceContainer,
                modifier = Modifier.fillMaxHeight(),
            ) {
                Box(
                    modifier = Modifier.width(MainTheme.sizes.large),
                    contentAlignment = Alignment.Center,
                ) {
                    AccountAvatar(
                        account = account,
                        onClick = { },
                    )
                }
            }
        }
        Row(
            modifier = modifier
                .clickable(onClick = onClick)
                .height(intrinsicSize = IntrinsicSize.Max)
                .fillMaxWidth()
                .defaultMinSize(minHeight = MainTheme.sizes.large)
                .padding(
                    top = MainTheme.spacings.double,
                    start = MainTheme.spacings.double,
                    end = MainTheme.spacings.triple,
                    bottom = MainTheme.spacings.double,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountIndicator(
                accountColor = account.color,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = MainTheme.spacings.oneHalf),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
            ) {
                TextBodyLarge(
                    text = account.name,
                    color = MainTheme.colors.onSurface,
                )
                TextBodyMedium(
                    text = account.email,
                    color = MainTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}
