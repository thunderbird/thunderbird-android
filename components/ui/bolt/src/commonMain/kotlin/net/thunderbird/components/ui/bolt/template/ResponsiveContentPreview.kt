package net.thunderbird.components.ui.bolt.template

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevices
import net.thunderbird.components.ui.bolt.theme.MainTheme

@Composable
@PreviewDevices
internal fun ResponsiveContentPreview() {
    PreviewWithTheme {
        Surface {
            ResponsiveContent { contentPadding ->
                Surface(
                    color = MainTheme.colors.info,
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                ) {}
            }
        }
    }
}
