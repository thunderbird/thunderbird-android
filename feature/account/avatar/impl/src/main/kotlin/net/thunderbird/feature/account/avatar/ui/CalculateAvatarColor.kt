package net.thunderbird.feature.account.avatar.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.core.ui.compose.theme2.toHarmonizedColor

@Composable
internal fun calculateAvatarColor(accountColor: Color): Color {
    return if (accountColor == Color.Unspecified) {
        MainTheme.colors.tertiary
    } else {
        accountColor.toHarmonizedColor(MainTheme.colors.surface)
    }
}
