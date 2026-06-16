package net.thunderbird.components.ui.bolt.template

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevices
import net.thunderbird.components.ui.bolt.common.padding.calculateResponsiveWidthPadding
import net.thunderbird.components.ui.bolt.common.window.WindowHeightSizeClass
import net.thunderbird.components.ui.bolt.common.window.WindowSizeClass
import net.thunderbird.components.ui.bolt.common.window.WindowWidthSizeClass
import net.thunderbird.components.ui.bolt.common.window.calculateWindowSizeInfo
import net.thunderbird.components.ui.bolt.theme.BoltTheme

/**
 * The [ResponsiveContent] composable automatically adapts its child content to different screen sizes and resolutions,
 * providing a responsive layout for a better user experience.
 *
 * It uses the [WindowSizeClass] (Compact, Medium, or Expanded) to make appropriate layout adjustments.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param content The content to be displayed.
 */
@Composable
fun ResponsiveContent(
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val windowSizeInfo = calculateWindowSizeInfo()

    when (windowSizeInfo.sizeClass.widthSizeClass) {
        WindowWidthSizeClass.Small -> CompactContent(modifier = modifier, content = content)
        WindowWidthSizeClass.Compact -> CompactContent(modifier = modifier, content = content)
        WindowWidthSizeClass.Medium -> MediumContent(modifier = modifier, content = content)
        WindowWidthSizeClass.Expanded -> ExpandedContent(modifier = modifier, content = content)
    }
}

@Composable
private fun CompactContent(
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
    ) {
        content(calculateResponsiveWidthPadding())
    }
}

@Composable
private fun MediumContent(
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        contentAlignment = Alignment.TopCenter,
    ) {
        content(calculateResponsiveWidthPadding())
    }
}

@Composable
private fun ExpandedContent(
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val windowSizeInfo = calculateWindowSizeInfo()

    when (windowSizeInfo.sizeClass.heightSizeClass) {
        WindowHeightSizeClass.Small -> CompactContent(modifier, content)

        WindowHeightSizeClass.Compact -> MediumContent(modifier, content)

        WindowHeightSizeClass.Medium -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(modifier),
                contentAlignment = Alignment.TopCenter,
            ) {
                Surface(
                    tonalElevation = BoltTheme.elevations.level1,
                ) {
                    content(calculateResponsiveWidthPadding())
                }
            }
        }

        WindowHeightSizeClass.Expanded -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(modifier),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier
                        .requiredHeight(WindowHeightSizeClass.BREAKPOINT_MEDIUM),
                    tonalElevation = BoltTheme.elevations.level1,
                ) {
                    content(calculateResponsiveWidthPadding())
                }
            }
        }
    }
}

@Composable
@PreviewDevices
internal fun ResponsiveContentPreview() {
    PreviewWithTheme {
        Surface {
            ResponsiveContent { contentPadding ->
                Surface(
                    color = BoltTheme.colors.info,
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                ) {}
            }
        }
    }
}
