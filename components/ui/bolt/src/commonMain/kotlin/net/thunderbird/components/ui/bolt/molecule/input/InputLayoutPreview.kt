package net.thunderbird.components.ui.bolt.molecule.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.textfield.TextFieldOutlined

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
