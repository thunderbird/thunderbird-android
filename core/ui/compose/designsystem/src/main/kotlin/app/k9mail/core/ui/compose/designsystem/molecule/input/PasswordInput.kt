package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
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
    isRequired: Boolean = false,
    errorMessage: String? = null,
    contentPadding: PaddingValues = inputContentPadding(),
) {
    InputLayout(
        modifier = modifier,
        contentPadding = contentPadding,
        errorMessage = errorMessage,
    ) {
        TextFieldOutlinedPassword(
            value = password,
            onValueChange = onPasswordChange,
            label = stringResource(id = R.string.designsystem_molecule_password_input_label),
            isRequired = isRequired,
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
