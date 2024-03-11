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
    MaterialOutlinedTextField(
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

@Preview(showBackground = true)
@Composable
internal fun TextFieldOutlinedEmailAddressPreview() {
    PreviewWithThemes {
        TextFieldOutlinedEmailAddress(
            value = "Input text",
            onValueChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextFieldOutlinedEmailAddressWithLabelPreview() {
    PreviewWithThemes {
        TextFieldOutlinedEmailAddress(
            value = "Input text",
            label = "Label",
            onValueChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextFieldOutlinedEmailDisabledPreview() {
    PreviewWithThemes {
        TextFieldOutlinedEmailAddress(
            value = "Input text",
            onValueChange = {},
            isEnabled = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextFieldOutlinedEmailErrorPreview() {
    PreviewWithThemes {
        TextFieldOutlinedEmailAddress(
            value = "Input text",
            onValueChange = {},
            hasError = true,
        )
    }
}
