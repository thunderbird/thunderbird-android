package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.CircularProgressIndicator as Material3CircularProgressIndicator

@Composable
fun CircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
) {
    Material3CircularProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
    )
}

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
) {
    Material3CircularProgressIndicator(
        modifier = modifier,
        color = color,
    )
}
