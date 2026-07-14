package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.material3.DividerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.VerticalDivider as Material3VerticalDivider

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
