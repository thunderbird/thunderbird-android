package app.k9mail.core.ui.compose.designsystem.atom.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun ButtonElevatedPreview() {
    PreviewWithThemes {
        ButtonElevated(
            text = "Button Elevated",
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonElevatedDisabledPreview() {
    PreviewWithThemes {
        ButtonElevated(
            text = "Button Elevated Disabled",
            onClick = {},
            enabled = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonElevatedMultiLinePreview() {
    PreviewWithThemes {
        ButtonElevated(
            text = "First\nSecond line",
            onClick = {},
        )
    }
}
