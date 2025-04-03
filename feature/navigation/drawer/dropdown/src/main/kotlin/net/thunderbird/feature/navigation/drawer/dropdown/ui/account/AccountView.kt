package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.account.avatar.ui.AvatarOutlined
import net.thunderbird.feature.account.avatar.ui.AvatarSize
import net.thunderbird.feature.navigation.drawer.dropdown.R
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayAccount

@Composable
internal fun AccountView(
    account: DisplayAccount,
    onClick: () -> Unit,
    showAccount: Boolean,
    modifier: Modifier = Modifier,
) {
    AccountLayout(
        onClick = onClick,
        modifier = modifier,
    ) {
        if (showAccount) {
            AccountSelectedView(
                account = account,
            )
        } else {
            AccountSelectionView()
        }

        AnimatedSelectionIcon(
            showAccount,
        )
    }
}

@Composable
private fun AnimatedSelectionIcon(showAccount: Boolean) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (showAccount) 0f else 180f,
        label = "rotationAngle",
    )

    Icon(
        imageVector = Icons.Outlined.KeyboardArrowDown,
        contentDescription = null,
        tint = MainTheme.colors.onSurfaceVariant,
        modifier = Modifier
            .padding(end = MainTheme.spacings.double)
            .rotate(rotationAngle),
    )
}

@Composable
private fun RowScope.AccountSelectedView(
    account: DisplayAccount,
) {
    AvatarOutlined(
        color = Color(account.color),
        name = account.name,
        size = AvatarSize.MEDIUM,
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
    ) {
        TextBodyLarge(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(account.name)
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

@Composable
private fun RowScope.AccountSelectionView() {
    TextBodyLarge(
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(stringResource(R.string.navigation_drawer_dropdown_avount_view_selection_title))
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
    )
}

@Composable
private fun AccountLayout(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val horizontalInsetPadding = getDisplayCutOutHorizontalInsetPadding()

    Box(
        modifier = modifier
            .windowInsetsPadding(horizontalInsetPadding)
            .clickable(onClick = onClick)
            .padding(
                top = MainTheme.spacings.default,
                start = MainTheme.spacings.triple,
                end = MainTheme.spacings.double,
                bottom = MainTheme.spacings.default,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MainTheme.sizes.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
        ) {
            content()
        }
    }
}

@Composable
fun getDisplayCutOutHorizontalInsetPadding(): WindowInsets {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    return WindowInsets.displayCutout.only(if (isRtl) WindowInsetsSides.Right else WindowInsetsSides.Left)
}
