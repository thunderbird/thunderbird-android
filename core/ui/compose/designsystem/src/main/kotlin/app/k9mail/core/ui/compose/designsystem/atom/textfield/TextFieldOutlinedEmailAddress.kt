package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.OutlinedTextField as MaterialOutlinedTextField

@Composable
fun TextFieldOutlinedEmailAddress(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    isError: Boolean = false,
) {
    MaterialOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        label = selectLabel(label),
        isError = isError,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
        ),
        singleLine = true,
    )
}

@Preview(showBackground = true)
@Composable
internal fun TextFieldOutlinedEmailAddressPreview() {
    PreviewWithThemes {
        TextFieldOutlined(
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
            enabled = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextFieldOutlinedEmailErrorPreview() {
    PreviewWithThemes {
        TextFieldOutlined(
            value = "Input text",
            onValueChange = {},
            isError = true,
        )
    }
}
