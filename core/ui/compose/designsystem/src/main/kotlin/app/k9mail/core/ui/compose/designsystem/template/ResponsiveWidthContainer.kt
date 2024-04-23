package app.k9mail.core.ui.compose.designsystem.template

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.common.window.WindowSizeClass
import app.k9mail.core.ui.compose.common.window.WindowSizeClass.Compact
import app.k9mail.core.ui.compose.common.window.WindowSizeClass.Expanded
import app.k9mail.core.ui.compose.common.window.WindowSizeClass.Medium
import app.k9mail.core.ui.compose.common.window.getWindowSizeInfo

/**
 * A container that adjusts its width depending on the screen size.
 *
 * This composable function acts as a wrapper for its content, applying a modifier that changes
 * the width of the content based on the current screen size. It uses the `getWindowSizeInfo`
 * function to determine the screen size, and then applies the appropriate modifier.
 *
 * @param modifier Any modifier that should be applied to the outer container. This can be used
 * to add padding, background colors, click events, etc.
 * @param content The content to be placed inside this container. The content is expected to be
 * a composable function.
 *
 * Example usage:
 * ```
 * ResponsiveWidthContainer {
 *     Text("Hello, World!")
 * }
 * ```
 */
@Composable
fun ResponsiveWidthContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val windowSizeInfo = getWindowSizeInfo()

    val responsiveModifier = when (windowSizeInfo.screenWidthSizeClass) {
        Compact -> Modifier.fillMaxWidth()

        Medium -> Modifier.requiredWidth(WindowSizeClass.COMPACT_MAX_WIDTH.dp)

        Expanded -> Modifier.requiredWidth(WindowSizeClass.MEDIUM_MAX_WIDTH.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = responsiveModifier,
        ) {
            content()
        }
    }
}
