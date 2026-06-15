package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.material3.DividerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.HorizontalDivider as Material3HorizontalDivider

@Composable
fun DividerHorizontal(
    modifier: Modifier = Modifier,
    thickness: Dp = DividerDefaults.Thickness,
    color: Color = DividerDefaults.color,
) {
    Material3HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = color,
    )
}
