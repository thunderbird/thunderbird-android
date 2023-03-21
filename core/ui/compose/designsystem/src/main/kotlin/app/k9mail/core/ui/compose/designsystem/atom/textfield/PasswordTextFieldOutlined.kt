package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.R
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.OutlinedTextField as MaterialOutlinedTextField

@Composable
fun PasswordTextFieldOutlined(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    isError: Boolean = false,
) {
    var passwordVisibilityState by rememberSaveable { mutableStateOf(false) }

    MaterialOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        label = selectLabel(label),
        trailingIcon = selectTrailingIcon(
            isEnabled = enabled,
            isPasswordVisible = passwordVisibilityState,
            onClick = { passwordVisibilityState = !passwordVisibilityState },
        ),
        isError = isError,
        visualTransformation = selectVisualTransformation(
            isEnabled = enabled,
            isPasswordVisible = passwordVisibilityState,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
    )
}

private fun selectLabel(label: String?): @Composable (() -> Unit)? {
    return if (label != null) {
        {
            Text(text = label)
        }
    } else {
        null
    }
}

private fun selectTrailingIcon(
    isEnabled: Boolean,
    isPasswordVisible: Boolean,
    onClick: () -> Unit,
    hasTrailingIcon: Boolean = true,
): @Composable (() -> Unit)? {
    return if (hasTrailingIcon) {
        {
            val image = if (isShowPasswordAllowed(isEnabled, isPasswordVisible)) {
                Icons.Filled.Visibility
            } else {
                Icons.Filled.VisibilityOff
            }

            val description = if (isShowPasswordAllowed(isEnabled, isPasswordVisible)) {
                stringResource(id = R.string.designsystem_atom_password_textfield_hide_password)
            } else {
                stringResource(id = R.string.designsystem_atom_password_textfield_show_password)
            }

            IconButton(onClick = onClick) {
                Icon(imageVector = image, contentDescription = description)
            }
        }
    } else {
        null
    }
}

private fun selectVisualTransformation(
    isEnabled: Boolean,
    isPasswordVisible: Boolean,
): VisualTransformation {
    return if (isShowPasswordAllowed(isEnabled, isPasswordVisible)) {
        VisualTransformation.None
    } else {
        PasswordVisualTransformation()
    }
}

private fun isShowPasswordAllowed(isEnabled: Boolean, isPasswordVisible: Boolean) = isEnabled && isPasswordVisible

@Preview(showBackground = true)
@Composable
internal fun PasswordTextFieldOutlinedPreview() {
    PreviewWithThemes {
        PasswordTextFieldOutlined(
            value = "Input text",
            onValueChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun PasswordTextFieldOutlinedWithLabelPreview() {
    PreviewWithThemes {
        PasswordTextFieldOutlined(
            value = "Input text",
            label = "Label",
            onValueChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun PasswordTextFieldOutlinedDisabledPreview() {
    PreviewWithThemes {
        PasswordTextFieldOutlined(
            value = "Input text",
            onValueChange = {},
            enabled = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun PasswordTextFieldOutlinedErrorPreview() {
    PreviewWithThemes {
        PasswordTextFieldOutlined(
            value = "Input text",
            onValueChange = {},
            isError = true,
        )
    }
}
