package net.thunderbird.core.ui.testing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize

/**
 * Sets Compose test content with a deterministic window size.
 *
 * This keeps [LocalWindowInfo] and the rendered root size in sync, which is required for responsive layout and
 * screenshot tests.
 */
public fun ComposeUiTestScope.setContentWithWindowSize(
    windowSize: DpSize,
    content: @Composable () -> Unit,
) {
    setContent {
        WindowSizeTestContainer(windowSize = windowSize, content = content)
    }
}

@Composable
private fun WindowSizeTestContainer(
    windowSize: DpSize,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val containerSize = with(density) {
        IntSize(
            width = windowSize.width.roundToPx(),
            height = windowSize.height.roundToPx(),
        )
    }
    val windowInfo = remember(containerSize) {
        TestWindowInfo(containerSize = containerSize)
    }

    CompositionLocalProvider(LocalWindowInfo provides windowInfo) {
        Box(
            modifier = Modifier.requiredSize(
                width = windowSize.width,
                height = windowSize.height,
            ),
        ) {
            content()
        }
    }
}

private class TestWindowInfo(
    override val containerSize: IntSize,
) : WindowInfo {
    override val isWindowFocused: Boolean = true
}
