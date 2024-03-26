package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

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
