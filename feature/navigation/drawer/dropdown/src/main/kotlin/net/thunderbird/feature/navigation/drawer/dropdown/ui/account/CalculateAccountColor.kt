package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.core.ui.compose.theme2.toHarmonizedColor

@Composable
internal fun calculateAccountColor(accountColor: Color): Color {
    return if (accountColor == Color.Unspecified) {
        MainTheme.colors.primary
    } else {
        accountColor.toHarmonizedColor(MainTheme.colors.surface)
    }
}
