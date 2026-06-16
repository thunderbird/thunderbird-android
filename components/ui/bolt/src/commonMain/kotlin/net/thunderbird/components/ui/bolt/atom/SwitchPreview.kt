package net.thunderbird.components.ui.bolt.atom

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun SwitchPreview() {
    PreviewWithThemes {
        Switch(
            checked = true,
            onCheckedChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SwitchDisabledPreview() {
    PreviewWithThemes {
        Switch(
            checked = true,
            onCheckedChange = {},
            enabled = false,
        )
    }
}
