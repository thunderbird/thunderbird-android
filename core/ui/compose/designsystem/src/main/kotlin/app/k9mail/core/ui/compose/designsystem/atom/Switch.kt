package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.Switch as MaterialSwitch

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    MaterialSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    )
}

@Preview(showBackground = true)
@Composable
internal fun SwitchPreview() {
    PreviewWithThemes {
        Switch(
            checked = true,
            onCheckedChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun SwitchDisabledPreview() {
    PreviewWithThemes {
        Switch(
            checked = true,
            onCheckedChange = {},
            enabled = false,
        )
    }
}
