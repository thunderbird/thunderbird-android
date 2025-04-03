package net.thunderbird.feature.navigation.drawer.siderail.ui.account

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.navigation.drawer.dropdown.ui.account.calculateAccountColor

@Composable
internal fun SideRailAccountIndicator(
    accountColor: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .width(MainTheme.spacings.half)
            .defaultMinSize(
                minHeight = MainTheme.spacings.default,
            ),
        color = calculateAccountColor(accountColor),
        shape = MainTheme.shapes.medium,
    ) {}
}
