package app.k9mail.core.ui.compose.designsystem.atom.textfield

import android.os.Build
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import app.k9mail.core.ui.compose.designsystem.R
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import androidx.compose.material3.Icon as Material3Icon
import androidx.compose.material3.IconButton as Material3IconButton
import androidx.compose.material3.OutlinedTextField as Material3OutlinedTextField

@Suppress("LongParameterList")
@Composable
fun TextFieldOutlinedPassword(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    isEnabled: Boolean = true,
    isReadOnly: Boolean = false,
    isRequired: Boolean = false,
    hasError: Boolean = false,
) {
    var passwordVisibilityState by rememberSaveable { mutableStateOf(false) }

    Material3OutlinedTextField(
        value = value,
        onValueChange = stripLineBreaks(onValueChange),
        modifier = modifier.applyLegacyPasswordSemantics().semantics { contentType = ContentType.Password },
        enabled = isEnabled,
        label = selectLabel(label, isRequired),
        trailingIcon = selectTrailingIcon(
            isEnabled = isEnabled,
            isPasswordVisible = passwordVisibilityState,
            onClick = { passwordVisibilityState = !passwordVisibilityState },
        ),
        readOnly = isReadOnly,
        isError = hasError,
        visualTransformation = selectVisualTransformation(
            isEnabled = isEnabled,
            isPasswordVisible = passwordVisibilityState,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
    )
}

@Composable
fun TextFieldOutlinedPassword(
    value: String,
    onValueChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onPasswordVisibilityToggleClicked: () -> Unit,
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
        modifier = modifier.applyLegacyPasswordSemantics().semantics { contentType = ContentType.Password },
        enabled = isEnabled,
        label = selectLabel(label, isRequired),
        trailingIcon = selectTrailingIcon(
            isEnabled = isEnabled,
            isPasswordVisible = isPasswordVisible,
            onClick = onPasswordVisibilityToggleClicked,
        ),
        readOnly = isReadOnly,
        isError = hasError,
        visualTransformation = selectVisualTransformation(
            isEnabled = isEnabled,
            isPasswordVisible = isPasswordVisible,
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
                Icons.Outlined.Visibility
            } else {
                Icons.Outlined.VisibilityOff
            }

            val description = if (isShowPasswordAllowed(isEnabled, isPasswordVisible)) {
                stringResource(id = R.string.designsystem_atom_password_textfield_hide_password)
            } else {
                stringResource(id = R.string.designsystem_atom_password_textfield_show_password)
            }

            Material3IconButton(onClick = onClick) {
                Material3Icon(imageVector = image, contentDescription = description)
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

private fun Modifier.applyLegacyPasswordSemantics(): Modifier {
    /*
     * Workaround for a crash that can occur when the password visibility state changes
     * while an accessibility service is enabled on devices running Android API level 25 or below.
     * This approach mitigates the issue by applying password semantics only on affected versions.
     */
    return this.semantics {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            password()
        }
    }
}
