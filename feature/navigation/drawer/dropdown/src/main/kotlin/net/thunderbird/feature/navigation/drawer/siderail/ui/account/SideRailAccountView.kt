package net.thunderbird.feature.navigation.drawer.siderail.ui.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.ui.account.AccountAvatar
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.getDisplayAccountColor
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.getDisplayAccountName

@Suppress("LongMethod")
@Composable
internal fun SideRailAccountView(
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
                val horizontalInsetPadding = getDisplayCutOutHorizontalInsetPadding()

                Box(
                    modifier = Modifier
                        .windowInsetsPadding(horizontalInsetPadding)
                        .width(MainTheme.sizes.large),
                    contentAlignment = Alignment.Center,
                ) {
                    AccountAvatar(
                        account = account,
                        onClick = null,
                        selected = false,
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
            val color = getDisplayAccountColor(account)
            val name = getDisplayAccountName(account)

            SideRailAccountIndicator(
                accountColor = color,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = MainTheme.spacings.oneHalf),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
            ) {
                TextBodyLarge(
                    text = name,
                    color = MainTheme.colors.onSurface,
                )
                if (account is MailDisplayAccount && account.name != account.email) {
                    TextBodyMedium(
                        text = account.email,
                        color = MainTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun getDisplayCutOutHorizontalInsetPadding(): WindowInsets {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    return WindowInsets.displayCutout.only(if (isRtl) WindowInsetsSides.Right else WindowInsetsSides.Left)
}
