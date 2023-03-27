package app.k9mail.core.ui.compose.theme.color

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun SetSystemBarsColor(
    color: Color,
) {
    val systemUiController = rememberSystemUiController()

    DisposableEffect(Unit) {
        systemUiController.setSystemBarsColor(color = color)

        onDispose { }
    }
}
