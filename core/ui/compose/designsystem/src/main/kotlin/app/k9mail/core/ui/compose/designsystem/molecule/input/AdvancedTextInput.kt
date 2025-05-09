package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlined

/**
 * A text input field that uses [TextFieldValue] to support text selection and composition.
 *
 * It supports annotated strings, which can be used to display rich text or formatted text.
 */
@Suppress("LongParameterList")
@Composable
fun AdvancedTextInput(
    onTextChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    text: TextFieldValue = TextFieldValue(""),
    label: String? = null,
    isRequired: Boolean = false,
    errorMessage: String? = null,
    contentPadding: PaddingValues = inputContentPadding(),
    isSingleLine: Boolean = true,
    isEnabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
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
            keyboardOptions = keyboardOptions,
        )
    }
}
