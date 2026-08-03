package net.thunderbird.components.ui.bolt.atom.textfield

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField as Material3OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons

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

/**
 * Overload of [TextFieldOutlined] that accepts a [TextFieldValue] instead of a [String].
 */
@Suppress("LongParameterList")
@Composable
fun TextFieldOutlined(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
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
        onValueChange = if (isSingleLine) stripTextFieldValueLineBreaks(onValueChange) else onValueChange,
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

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedPreview() {
    PreviewWithThemes {
        TextFieldOutlined(
            value = "Input text",
            onValueChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedWithLabelPreview() {
    PreviewWithThemes {
        TextFieldOutlined(
            value = "Input text",
            onValueChange = {},
            label = "Label",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedDisabledPreview() {
    PreviewWithThemes {
        TextFieldOutlined(
            value = "Input text",
            onValueChange = {},
            isEnabled = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedErrorPreview() {
    PreviewWithThemes {
        TextFieldOutlined(
            value = "Input text",
            onValueChange = {},
            hasError = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
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

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedWithTrailingIconPreview() {
    PreviewWithThemes {
        TextFieldOutlined(
            value = "",
            onValueChange = {},
            trailingIcon = { Icon(imageVector = Icons.Outlined.AccountCircle) },
            isRequired = true,
        )
    }
}
