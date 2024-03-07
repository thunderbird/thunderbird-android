package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.atom.Icon
import app.k9mail.core.ui.compose.theme.Icons
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.OutlinedTextField as MaterialOutlinedTextField

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
    MaterialOutlinedTextField(
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

@Preview(showBackground = true)
@Composable
internal fun TextFieldOutlinedPreview() {
    PreviewWithThemes {
        TextFieldOutlined(
            value = "Input text",
            onValueChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextFieldOutlinedWithLabelPreview() {
    PreviewWithThemes {
        TextFieldOutlined(
            value = "Input text",
            onValueChange = {},
            label = "Label",
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextFieldOutlinedDisabledPreview() {
    PreviewWithThemes {
        TextFieldOutlined(
            value = "Input text",
            onValueChange = {},
            isEnabled = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextFieldOutlinedErrorPreview() {
    PreviewWithThemes {
        TextFieldOutlined(
            value = "Input text",
            onValueChange = {},
            hasError = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextFieldOutlinedRequiredPreview() {
    PreviewWithThemes {
        TextFieldOutlined(
            value = "",
            onValueChange = {},
            label = "Label",
            isRequired = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextFieldOutlinedWithTrailingIconPreview() {
    PreviewWithThemes {
        TextFieldOutlined(
            value = "",
            onValueChange = {},
            trailingIcon = { Icon(imageVector = Icons.Filled.user) },
            isRequired = true,
        )
    }
}
