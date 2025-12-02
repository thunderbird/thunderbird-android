package net.thunderbird.core.ui.compose.designsystem.molecule.message

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.thunderbird.core.ui.compose.designsystem.atom.chip.ColorChip

@Composable
fun AccountIndicatorIcon(
    color: Int,
    modifier: Modifier = Modifier,
) {
    ColorChip(
        color = color,
        width = AccountIndicatorIcon.ACCOUNT_INDICATOR_WIDTH,
        height = AccountIndicatorIcon.ACCOUNT_INDICATOR_HEIGHT,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun PreviewAccountIndicatorIconDefault() {
    AccountIndicatorIcon(
        color = Color.RED,
    )
}

private object AccountIndicatorIcon {
    val ACCOUNT_INDICATOR_WIDTH = 3.dp
    val ACCOUNT_INDICATOR_HEIGHT = 20.dp
}
