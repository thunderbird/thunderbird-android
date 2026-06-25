package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun TextInputPreview() {
    PreviewWithThemes {
        TextInput(
            onTextChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextInputIsRequiredPreview() {
    PreviewWithThemes {
        TextInput(
            onTextChange = {},
            label = "Text input is required",
            isRequired = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextInputWithErrorPreview() {
    PreviewWithThemes {
        TextInput(
            onTextChange = {},
            errorMessage = "Text input error",
        )
    }
}
