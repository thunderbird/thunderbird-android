package net.thunderbird.components.ui.bolt.template

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevices
import net.thunderbird.components.ui.bolt.theme.MainTheme

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
    ResponsiveContent { contentPadding ->
        Surface(
            modifier = modifier.padding(contentPadding),
            color = MainTheme.colors.surface,
        ) {
            content()
        }
    }
}

@Composable
@PreviewDevices
internal fun ResponsiveContentWithBackgroundPreview() {
    PreviewWithTheme {
        ResponsiveContentWithSurface {
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
