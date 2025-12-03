package net.thunderbird.core.ui.compose.designsystem.molecule.message

import android.graphics.Color
import androidx.compose.foundation.layout.padding
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
        width = AccountIndicatorIcon.ACCOUNT_INDICATOR_DEFAULT_WIDTH,
        height = AccountIndicatorIcon.ACCOUNT_INDICATOR_DEFAULT_HEIGHT,
        modifier = modifier.padding(
            end = AccountIndicatorIcon.ACCOUNT_INDICATOR_DEFAULT_PADDING,
        ),
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
    val ACCOUNT_INDICATOR_DEFAULT_WIDTH = 3.dp
    val ACCOUNT_INDICATOR_DEFAULT_HEIGHT = 20.dp
    val ACCOUNT_INDICATOR_DEFAULT_PADDING = 4.dp
}
