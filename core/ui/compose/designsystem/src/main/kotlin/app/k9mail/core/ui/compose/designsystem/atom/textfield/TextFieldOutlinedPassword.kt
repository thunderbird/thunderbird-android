package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
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
import app.k9mail.core.ui.compose.theme.Icons
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.OutlinedTextField as MaterialOutlinedTextField

@Composable
fun TextFieldOutlinedPassword(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    isRequired: Boolean = false,
    isError: Boolean = false,
) {
    var passwordVisibilityState by rememberSaveable { mutableStateOf(false) }

    MaterialOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        label = selectLabel(label, isRequired),
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

private fun selectTrailingIcon(
    isEnabled: Boolean,
    isPasswordVisible: Boolean,
    onClick: () -> Unit,
    hasTrailingIcon: Boolean = true,
): @Composable (() -> Unit)? {
    return if (hasTrailingIcon) {
        {
            val image = if (isShowPasswordAllowed(isEnabled, isPasswordVisible)) {
                Icons.passwordVisibility
            } else {
                Icons.passwordVisibilityOff
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
        TextFieldOutlinedPassword(
            value = "Input text",
            onValueChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextFieldOutlinedPasswordWithLabelPreview() {
    PreviewWithThemes {
        TextFieldOutlinedPassword(
            value = "Input text",
            label = "Label",
            onValueChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextFieldOutlinedPasswordDisabledPreview() {
    PreviewWithThemes {
        TextFieldOutlinedPassword(
            value = "Input text",
            onValueChange = {},
            enabled = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextFieldOutlinedPasswordErrorPreview() {
    PreviewWithThemes {
        TextFieldOutlinedPassword(
            value = "Input text",
            onValueChange = {},
            isError = true,
        )
    }
}
