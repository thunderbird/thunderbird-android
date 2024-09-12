package app.k9mail.feature.navigation.drawer.ui.account

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.core.ui.compose.theme2.toHarmonizedColor

@Composable
fun AccountIndicator(
    accountColor: Int,
    modifier: Modifier = Modifier,
) {
    val color = if (accountColor == 0) {
        MainTheme.colors.primary
    } else {
        Color(accountColor).toHarmonizedColor(MainTheme.colors.surface)
    }

    Surface(
        modifier = modifier
            .width(MainTheme.spacings.half)
            .defaultMinSize(
                minHeight = MainTheme.spacings.default,
            ),
        color = color,
        shape = MainTheme.shapes.medium,
    ) {}
}
