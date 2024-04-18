package app.k9mail.core.ui.compose.designsystem.template

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
@PreviewDevices
internal fun ResponsiveWidthContainerPreview() {
    PreviewWithTheme {
        Surface(
            color = MainTheme.colors.surfaceContainer,
        ) {
            ResponsiveWidthContainer {
                Surface(
                    color = MainTheme.colors.error,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    TextBodyLarge("Hello, World!")
                }
            }
        }
    }
}
