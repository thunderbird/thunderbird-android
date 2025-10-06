package app.k9mail.core.ui.compose.designsystem.template

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.padding.calculateResponsiveWidthPadding

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
    content: @Composable (paddingValues: PaddingValues) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        contentAlignment = Alignment.TopCenter,
    ) {
        content(calculateResponsiveWidthPadding())
    }
}
