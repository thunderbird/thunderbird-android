package app.k9mail.core.ui.compose.designsystem.template

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.atom.Background
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme

/**
 * The [ResponsiveContentWithBackground] composable embeds its content in [ResponsiveContent] with [Background].
 *
 * @param modifier The modifier to be applied to the layout.
 * @param content The content to be displayed.
 */
@Composable
fun ResponsiveContentWithBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ResponsiveContent {
        Background(
            modifier = modifier,
        ) {
            content()
        }
    }
}

@Composable
@DevicePreviews
internal fun ResponsiveContentWithBackgroundPreview() {
    K9Theme {
        ResponsiveContentWithBackground {
            Surface(
                color = MainTheme.colors.info,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MainTheme.spacings.double),
                content = {},
            )
        }
    }
}
