package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlined
import app.k9mail.core.ui.compose.theme.PreviewWithThemes

@Suppress("LongParameterList")
@Composable
fun TextInput(
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    label: String? = null,
    isRequired: Boolean = false,
    errorMessage: String? = null,
    contentPadding: PaddingValues = inputContentPadding(),
    isSingleLine: Boolean = true,
    isEnabled: Boolean = true,
) {
    InputLayout(
        modifier = modifier,
        contentPadding = contentPadding,
        errorMessage = errorMessage,
    ) {
        TextFieldOutlined(
            value = text,
            onValueChange = onTextChange,
            label = label,
            isEnabled = isEnabled,
            isRequired = isRequired,
            hasError = errorMessage != null,
            isSingleLine = isSingleLine,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextInputPreview() {
    PreviewWithThemes {
        TextInput(
            onTextChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextInputIsRequiredPreview() {
    PreviewWithThemes {
        TextInput(
            onTextChange = {},
            label = "Text input is required",
            isRequired = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextInputWithErrorPreview() {
    PreviewWithThemes {
        TextInput(
            onTextChange = {},
            errorMessage = "Text input error",
        )
    }
}
