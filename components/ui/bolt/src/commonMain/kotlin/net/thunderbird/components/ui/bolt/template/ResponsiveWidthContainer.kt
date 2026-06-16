package net.thunderbird.components.ui.bolt.template

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevices
import net.thunderbird.components.ui.bolt.common.padding.calculateResponsiveWidthPadding
import net.thunderbird.components.ui.bolt.theme.MainTheme

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

@Composable
@PreviewDevices
internal fun ResponsiveWidthContainerPreview() {
    PreviewWithTheme {
        Surface {
            ResponsiveWidthContainer { contentPadding ->
                Surface(
                    color = MainTheme.colors.error,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                ) {
                    TextBodyLarge("Hello, World!")
                }
            }
        }
    }
}
