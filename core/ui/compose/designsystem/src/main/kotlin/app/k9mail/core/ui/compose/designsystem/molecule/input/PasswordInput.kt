package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.R
import app.k9mail.core.ui.compose.designsystem.atom.text.TextCaption
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlinedPassword
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes

@Composable
fun PasswordInput(
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    password: String = "",
    errorMessage: String? = null,
    contentPadding: PaddingValues = inputContentPadding(),
) {
    Column(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxWidth()
            .then(modifier),
    ) {
        TextFieldOutlinedPassword(
            value = password,
            onValueChange = onPasswordChange,
            label = stringResource(id = R.string.designsystem_molecule_password_input_label),
            hasError = errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
        )
        AnimatedVisibility(visible = errorMessage != null) {
            TextCaption(
                text = errorMessage ?: "",
                modifier = Modifier.padding(start = MainTheme.spacings.double, top = MainTheme.spacings.half),
                color = MainTheme.colors.error,
            )
        }
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
