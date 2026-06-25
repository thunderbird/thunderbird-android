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
 *
 * @param onTextChange Callback that is triggered when the text changes.
 * @param modifier Modifier to be applied to the input field.
 * @param text The current text value of the input field.
 * @param label Optional label to be displayed above the input field.
 * @param isRequired Whether the input field is required.
 * @param errorMessage Optional error message to be displayed below the input field.
 * @param contentPadding Padding values to be applied around the input field.
 * @param isSingleLine Whether the input field is single line.
 * @param isEnabled Whether the input field is enabled.
 * @param keyboardOptions Software keyboard options.
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
