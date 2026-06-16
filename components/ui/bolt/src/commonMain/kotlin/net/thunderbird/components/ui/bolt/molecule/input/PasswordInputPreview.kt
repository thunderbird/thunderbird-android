package net.thunderbird.components.ui.bolt.molecule.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes

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
