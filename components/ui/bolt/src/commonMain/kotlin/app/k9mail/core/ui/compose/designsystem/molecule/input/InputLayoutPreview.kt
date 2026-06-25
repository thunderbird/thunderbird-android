package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlined

@Composable
@Preview(showBackground = true)
internal fun InputLayoutPreview() {
    PreviewWithThemes {
        InputLayout {
            TextFieldOutlined(value = "InputLayout", onValueChange = {})
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun InputLayoutWithErrorPreview() {
    PreviewWithThemes {
        InputLayout(
            errorMessage = "Error message",
        ) {
            TextFieldOutlined(value = "InputLayout", onValueChange = {})
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun InputLayoutWithWarningPreview() {
    PreviewWithThemes {
        InputLayout(
            warningMessage = "Warning message",
        ) {
            TextFieldOutlined(value = "InputLayout", onValueChange = {})
        }
    }
}
