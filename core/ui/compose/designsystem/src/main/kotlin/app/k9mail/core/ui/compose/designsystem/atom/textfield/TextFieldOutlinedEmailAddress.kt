package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.OutlinedTextField as Material3OutlinedTextField

@Suppress("LongParameterList")
@Composable
fun TextFieldOutlinedEmailAddress(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    isEnabled: Boolean = true,
    isReadOnly: Boolean = false,
    isRequired: Boolean = false,
    hasError: Boolean = false,
) {
    Material3OutlinedTextField(
        value = value,
        onValueChange = stripLineBreaks(onValueChange),
        modifier = modifier,
        enabled = isEnabled,
        label = selectLabel(label, isRequired),
        readOnly = isReadOnly,
        isError = hasError,
        keyboardOptions = KeyboardOptions(
            autoCorrect = false,
            keyboardType = KeyboardType.Email,
        ),
        singleLine = true,
    )
}
