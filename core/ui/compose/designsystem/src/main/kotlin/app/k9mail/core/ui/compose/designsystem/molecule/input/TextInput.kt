package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlined

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
) {
    InputLayout(
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
            modifier = modifier.fillMaxWidth(),
            keyboardOptions = keyboardOptions,
        )
    }
}
