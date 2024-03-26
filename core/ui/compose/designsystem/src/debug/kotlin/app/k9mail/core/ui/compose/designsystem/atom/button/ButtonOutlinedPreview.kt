package app.k9mail.core.ui.compose.designsystem.atom.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun ButtonOutlinedPreview() {
    PreviewWithThemes {
        ButtonOutlined(
            text = "Button Outlined",
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonOutlinedDisabledPreview() {
    PreviewWithThemes {
        ButtonOutlined(
            text = "Button Outlined Disabled",
            onClick = {},
            enabled = false,
        )
    }
}
