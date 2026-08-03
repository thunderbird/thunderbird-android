package net.thunderbird.components.ui.bolt.molecule.input

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.textfield.TextFieldOutlined

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
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    contentType: ContentType? = null,
) {
    InputLayout(
        modifier = modifier,
        contentPadding = contentPadding,
        errorMessage = errorMessage,
    ) {
        val textFieldModifier = if (contentType != null) {
            Modifier.semantics { this.contentType = contentType }
        } else {
            Modifier
        }

        TextFieldOutlined(
            value = text,
            onValueChange = onTextChange,
            label = label,
            isEnabled = isEnabled,
            isRequired = isRequired,
            hasError = errorMessage != null,
            isSingleLine = isSingleLine,
            modifier = textFieldModifier.fillMaxWidth(),
            keyboardOptions = keyboardOptions,
        )
    }
}

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
