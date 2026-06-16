package net.thunderbird.components.ui.bolt.atom

import androidx.compose.material3.Switch as Material3Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Material3Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    )
}

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
