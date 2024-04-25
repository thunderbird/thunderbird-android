package app.k9mail.ui.catalog.ui.atom.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
@Preview(showBackground = true)
@PreviewDevices
internal fun ColorContentPreview() {
    PreviewWithTheme {
        ColorContent(
            text = "Primary",
            color = MainTheme.colors.primary,
            textColor = MainTheme.colors.onPrimary,
        )
    }
}
