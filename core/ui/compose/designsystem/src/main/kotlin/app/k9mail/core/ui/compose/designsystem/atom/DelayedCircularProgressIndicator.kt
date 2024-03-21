package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.k9mail.core.ui.compose.common.visibility.hide
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LOADING_INDICATOR_DELAY = 500L

/**
 * Only show a [CircularProgressIndicator] after [LOADING_INDICATOR_DELAY] ms.
 *
 * Use this to avoid flashing a loading indicator for loads that are usually very fast.
 */
@Composable
fun DelayedCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
) {
    var progressIndicatorVisible by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = Unit) {
        launch {
            delay(LOADING_INDICATOR_DELAY)
            progressIndicatorVisible = true
        }
    }

    CircularProgressIndicator(
        modifier = Modifier
            .hide(!progressIndicatorVisible)
            .then(modifier),
        color = color,
    )
}
