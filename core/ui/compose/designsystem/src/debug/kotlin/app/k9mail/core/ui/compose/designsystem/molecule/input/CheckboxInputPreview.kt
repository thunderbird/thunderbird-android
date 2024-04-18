package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun CheckboxInputPreview() {
    PreviewWithThemes {
        CheckboxInput(
            text = "CheckboxInput",
            checked = false,
            onCheckedChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun CheckboxInputWithErrorPreview() {
    PreviewWithThemes {
        CheckboxInput(
            text = "CheckboxInput",
            checked = false,
            onCheckedChange = {},
            errorMessage = "Error message",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun CheckboxInputCheckedPreview() {
    PreviewWithThemes {
        CheckboxInput(
            text = "CheckboxInput",
            checked = true,
            onCheckedChange = {},
        )
    }
}
