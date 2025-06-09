package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.material3.LinearProgressIndicator as Material3LinearProgressIndicator

@Composable
fun LinearProgressIndicator(
    progress: Int,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (visible) {
        Material3LinearProgressIndicator(
            progress = { progress.toFloat() / 10000.0f },
            modifier = modifier
                .fillMaxWidth()
                .height(8.dp)
                .offset(y = ((-6).dp))
                .zIndex(1f),
        )
    }
}
