package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun PasswordInputPreview() {
    PreviewWithThemes {
        PasswordInput(
            onPasswordChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun PasswordInputWithErrorPreview() {
    PreviewWithThemes {
        PasswordInput(
            onPasswordChange = {},
            errorMessage = "Password error",
        )
    }
}
