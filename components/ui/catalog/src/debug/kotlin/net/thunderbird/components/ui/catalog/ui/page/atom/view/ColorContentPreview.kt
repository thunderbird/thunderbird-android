package net.thunderbird.components.ui.catalog.ui.page.atom.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevices
import net.thunderbird.components.ui.bolt.theme.BoltTheme

@Composable
@Preview(showBackground = true)
@PreviewDevices
internal fun ColorContentPreview() {
    PreviewWithTheme {
        ColorContent(
            text = "Primary",
            color = BoltTheme.colors.primary,
            textColor = BoltTheme.colors.onPrimary,
        )
    }
}
