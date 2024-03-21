package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.OutlinedTextField as Material3OutlinedTextField

@Suppress("LongParameterList")
@Composable
fun TextFieldOutlined(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isEnabled: Boolean = true,
    isReadOnly: Boolean = false,
    isRequired: Boolean = false,
    hasError: Boolean = false,
    isSingleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Material3OutlinedTextField(
        value = value,
        onValueChange = if (isSingleLine) stripLineBreaks(onValueChange) else onValueChange,
        modifier = modifier,
        enabled = isEnabled,
        label = selectLabel(label, isRequired),
        trailingIcon = trailingIcon,
        readOnly = isReadOnly,
        isError = hasError,
        singleLine = isSingleLine,
        keyboardOptions = keyboardOptions,
    )
}
