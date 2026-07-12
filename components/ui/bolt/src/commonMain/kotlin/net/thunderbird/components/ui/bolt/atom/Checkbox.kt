package net.thunderbird.components.ui.bolt.atom

import androidx.compose.material3.Checkbox as Material3Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Material3Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
@Preview(showBackground = true)
internal fun CheckboxPreview() {
    PreviewWithThemes {
        Checkbox(
            checked = true,
            onCheckedChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun CheckboxDisabledPreview() {
    PreviewWithThemes {
        Checkbox(
            checked = true,
            onCheckedChange = {},
            enabled = false,
        )
    }
}
