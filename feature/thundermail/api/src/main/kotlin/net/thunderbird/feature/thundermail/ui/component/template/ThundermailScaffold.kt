package net.thunderbird.feature.thundermail.ui.component.template

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.thundermail.ui.component.organism.ThundermailToolbar
import net.thunderbird.feature.thundermail.ui.screen.ThundermailConstants

@Composable
fun ThundermailScaffold(
    header: @Composable (PaddingValues) -> Unit,
    subHeaderText: String,
    bottomBar: @Composable (PaddingValues, containerColor: Color) -> Unit,
    modifier: Modifier = Modifier,
    canScrollForward: Boolean = false,
    maxWidth: Dp = ThundermailConstants.MaxContainerWidth,
    headerContentPadding: PaddingValues = PaddingValues(
        horizontal = MainTheme.spacings.quadruple,
    ),
    content: @Composable (scaffoldPaddingValues: PaddingValues, responsivePaddingValues: PaddingValues, maxWidth: Dp) -> Unit,
) {
    ThundermailScaffold(
        toolbar = {
            ResponsiveWidthContainer { paddingValues ->
                ThundermailToolbar(
                    header = { header(paddingValues) },
                    subHeaderText = subHeaderText,
                    maxWidth = maxWidth,
                    contentPadding = headerContentPadding,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        },
        bottomBar = bottomBar,
        modifier = modifier,
        canScrollForward = canScrollForward,
        content = content,
    )
}

@Composable
fun ThundermailScaffold(
    toolbar: @Composable () -> Unit,
    bottomBar: @Composable (PaddingValues, containerColor: Color) -> Unit,
    modifier: Modifier = Modifier,
    canScrollForward: Boolean = false,
    maxWidth: Dp = ThundermailConstants.MaxContainerWidth,
    content: @Composable (scaffoldPaddingValues: PaddingValues, responsivePaddingValues: PaddingValues, maxWidth: Dp) -> Unit,
) {
    Scaffold(
        topBar = toolbar,
        bottomBar = {
            ResponsiveWidthContainer { paddingValues ->
                // Elevate the bottom bar when some scrollable content is "underneath" it
                val containerColor by animateColorAsState(
                    targetValue = if (canScrollForward) {
                        MainTheme.colors.surfaceContainerLowest.copy(alpha = .25f)
                    } else {
                        Color.Transparent
                    },
                    label = "ThundermailBottomBarContainerColor",
                )
                bottomBar(paddingValues, containerColor)
            }
        },
        modifier = modifier,
    ) { paddingValues ->
        ResponsiveWidthContainer { contentPadding ->
            content(paddingValues, contentPadding, maxWidth)
        }
    }
}
