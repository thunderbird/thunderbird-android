package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.Checkbox as MaterialCheckbox

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    MaterialCheckbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    )
}

@Preview(showBackground = true)
@Composable
internal fun CheckboxPreview() {
    PreviewWithThemes {
        Checkbox(
            checked = true,
            onCheckedChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun CheckboxDisabledPreview() {
    PreviewWithThemes {
        Checkbox(
            checked = true,
            onCheckedChange = {},
            enabled = false,
        )
    }
}
