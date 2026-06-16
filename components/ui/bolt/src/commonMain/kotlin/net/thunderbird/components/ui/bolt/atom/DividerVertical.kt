package net.thunderbird.components.ui.bolt.atom

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.VerticalDivider as Material3VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.theme.MainTheme

@Composable
fun DividerVertical(
    modifier: Modifier = Modifier,
    thickness: Dp = DividerDefaults.Thickness,
    color: Color = DividerDefaults.color,
) {
    Material3VerticalDivider(
        modifier = modifier,
        thickness = thickness,
        color = color,
    )
}

@Composable
@Preview(showBackground = true)
internal fun DividerVerticalPreview() {
    PreviewWithThemes {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(MainTheme.spacings.double),
        ) {
            DividerVertical(
                modifier = Modifier.fillMaxHeight(),
            )
        }
    }
}
