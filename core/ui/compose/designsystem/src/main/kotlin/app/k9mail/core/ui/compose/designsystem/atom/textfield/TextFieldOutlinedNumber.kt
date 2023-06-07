package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.OutlinedTextField as MaterialOutlinedTextField

@Suppress("LongParameterList")
@Composable
fun TextFieldOutlinedNumber(
    value: Long?,
    onValueChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    isEnabled: Boolean = true,
    isReadOnly: Boolean = false,
    isRequired: Boolean = false,
    hasError: Boolean = false,
) {
    MaterialOutlinedTextField(
        value = value?.toString() ?: "",
        onValueChange = {
            onValueChange(
                it.takeIf { it.isNotBlank() }?.toLongOrNull(),
            )
        },
        modifier = modifier,
        enabled = isEnabled,
        label = selectLabel(label, isRequired),
        readOnly = isReadOnly,
        isError = hasError,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
        ),
        singleLine = true,
    )
}

@Preview(showBackground = true)
@Composable
internal fun TextFieldOutlinedNumberPreview() {
    PreviewWithThemes {
        TextFieldOutlinedNumber(
            value = 123L,
            onValueChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextFieldOutlinedNumberWithLabelPreview() {
    PreviewWithThemes {
        TextFieldOutlinedNumber(
            value = 123L,
            label = "Label",
            onValueChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextFieldOutlinedNumberDisabledPreview() {
    PreviewWithThemes {
        TextFieldOutlinedNumber(
            value = 123L,
            onValueChange = {},
            isEnabled = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextFieldOutlinedNumberErrorPreview() {
    PreviewWithThemes {
        TextFieldOutlinedNumber(
            value = 123L,
            onValueChange = {},
            hasError = true,
        )
    }
}
