package app.k9mail.core.ui.compose.designsystem.template

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.common.padding.calculateResponsiveWidthPadding
import app.k9mail.core.ui.compose.common.window.WindowSizeClass
import app.k9mail.core.ui.compose.common.window.getWindowSizeInfo
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import net.thunderbird.core.ui.compose.theme2.MainTheme

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
    useSurfaceForExpandedContent: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    val windowSizeClass = getWindowSizeInfo()

    when (windowSizeClass.screenWidthSizeClass) {
        WindowSizeClass.Small -> CompactContent(modifier = modifier, content = content)
        WindowSizeClass.Compact -> CompactContent(modifier = modifier, content = content)
        WindowSizeClass.Medium -> MediumContent(modifier = modifier, content = content)
        WindowSizeClass.Expanded -> ExpandedContent(
            modifier = modifier,
            useSurface = useSurfaceForExpandedContent,
            content = content,
        )
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
    useSurface: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    when (getWindowSizeInfo().screenHeightSizeClass) {
        WindowSizeClass.Small -> CompactContent(modifier, content)
        WindowSizeClass.Compact -> MediumContent(modifier, content)
        WindowSizeClass.Medium -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(modifier),
                contentAlignment = Alignment.TopCenter,
            ) {
                ContentWithOrWithoutSurface(useSurface = useSurface, content = content)
            }
        }

        WindowSizeClass.Expanded -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(modifier),
                contentAlignment = Alignment.Center,
            ) {
                ContentWithOrWithoutSurface(
                    useSurface = useSurface,
                    modifier = Modifier
                        .requiredHeight(WindowSizeClass.MEDIUM_MAX_HEIGHT.dp),
                    content = content,
                )
                content(calculateResponsiveWidthPadding())
            }
        }
    }
}

@Composable
private fun ContentWithOrWithoutSurface(
    useSurface: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ((PaddingValues) -> Unit),
) {
    val effectiveContent = remember { movableContentOf { content(calculateResponsiveWidthPadding()) } }
    if (useSurface) {
        Surface(
            tonalElevation = MainTheme.elevations.level1,
            modifier = modifier,
        ) {
            effectiveContent()
        }
    } else {
        effectiveContent()
    }
}
