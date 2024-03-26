package app.k9mail.core.ui.compose.designsystem.atom.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun ButtonFilledPreview() {
    PreviewWithThemes {
        ButtonFilled(
            text = "Button Filled",
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonFilledlDisabledPreview() {
    PreviewWithThemes {
        ButtonFilled(
            text = "Button Filled Disabled",
            onClick = {},
            enabled = false,
        )
    }
}
