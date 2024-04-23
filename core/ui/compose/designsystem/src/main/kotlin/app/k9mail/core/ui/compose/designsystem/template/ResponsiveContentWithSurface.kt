package app.k9mail.core.ui.compose.designsystem.template

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme2.MainTheme

/**
 * The [ResponsiveContentWithSurface] composable embeds its content in [ResponsiveContent] with [Surface].
 *
 * @param modifier The modifier to be applied to the layout.
 * @param content The content to be displayed.
 */
@Composable
fun ResponsiveContentWithSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ResponsiveContent {
        Surface(
            modifier = modifier,
            color = MainTheme.colors.surfaceContainer,
        ) {
            content()
        }
    }
}
