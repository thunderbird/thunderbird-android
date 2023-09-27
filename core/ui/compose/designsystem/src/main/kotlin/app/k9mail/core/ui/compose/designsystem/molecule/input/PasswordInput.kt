package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.R
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlinedPassword
import app.k9mail.core.ui.compose.theme.PreviewWithThemes

@Composable
fun PasswordInput(
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    password: String = "",
    checkRevealPasswordPermission: () -> Boolean = { false },
    isRequired: Boolean = false,
    errorMessage: String? = null,
    contentPadding: PaddingValues = inputContentPadding(),
) {
    var showPermissionInfo by remember { mutableStateOf(false) }

    InputLayout(
        modifier = modifier,
        contentPadding = contentPadding,
        errorMessage = errorMessage,
        warningMessage = if (showPermissionInfo) {
            stringResource(id = R.string.designsystem_molecule_password_input_permission_warning)
        } else {
            null
        },
    ) {
        TextFieldOutlinedPassword(
            value = password,
            onValueChange = {
                onPasswordChange(it)
                showPermissionInfo = false
            },
            label = stringResource(id = R.string.designsystem_molecule_password_input_label),
            isRequired = isRequired,
            checkRevealPasswordPermission = {
                checkRevealPasswordPermission().also {
                    showPermissionInfo = !it
                }
            },
            hasError = errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun PasswordInputPreview() {
    PreviewWithThemes {
        PasswordInput(
            onPasswordChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun PasswordInputWithErrorPreview() {
    PreviewWithThemes {
        PasswordInput(
            onPasswordChange = {},
            errorMessage = "Password error",
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun PasswordInputWithNoPermissionPreview() {
    PreviewWithThemes {
        PasswordInput(
            onPasswordChange = {},
            checkRevealPasswordPermission = { false },
        )
    }
}
