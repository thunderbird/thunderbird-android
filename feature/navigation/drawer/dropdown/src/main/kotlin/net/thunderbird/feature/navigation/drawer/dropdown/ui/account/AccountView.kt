package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.navigation.drawer.dropdown.R
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.AnimatedExpandIcon
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.getDisplayAccountName

@Composable
internal fun AccountView(
    account: DisplayAccount,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit,
    showAccountSelection: Boolean,
    modifier: Modifier = Modifier,
) {
    AccountLayout(
        onClick = onClick,
        modifier = modifier,
    ) {
        if (showAccountSelection) {
            AccountSelectionView()
        } else {
            AccountSelectedView(
                account = account,
                onAvatarClick = onAvatarClick,
            )
        }

        AnimatedExpandIcon(
            isExpanded = showAccountSelection,
            modifier = Modifier.padding(end = MainTheme.spacings.double),
            tint = MainTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun RowScope.AccountSelectedView(
    account: DisplayAccount,
    onAvatarClick: () -> Unit,
) {
    AnimatedContent(
        targetState = account,
        transitionSpec = {
            (slideInHorizontally { it } + fadeIn()) togetherWith
                (slideOutHorizontally { -it } + fadeOut())
        },
        label = "AccountSelectedContent",
        contentKey = { it.id },
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
    ) { targetAccount ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
        ) {
            AccountAvatar(
                account = targetAccount,
                onClick = { onAvatarClick() },
                selected = false,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val name = getDisplayAccountName(targetAccount)
                TextBodyLarge(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(name)
                        }
                    },
                )
                if (targetAccount is MailDisplayAccount && targetAccount.name != targetAccount.email) {
                    TextBodyMedium(
                        text = targetAccount.email,
                    )
                }
            }
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
