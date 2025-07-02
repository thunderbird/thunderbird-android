package app.k9mail.core.ui.compose.designsystem.atom.divider

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.theme2.MainTheme
import androidx.compose.material3.HorizontalDivider as MaterialHorizontalDivider

@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = MainTheme.sizes.tiny,
    color: Color = MainTheme.colors.outline,
) {
    MaterialHorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = color,
    )
}
