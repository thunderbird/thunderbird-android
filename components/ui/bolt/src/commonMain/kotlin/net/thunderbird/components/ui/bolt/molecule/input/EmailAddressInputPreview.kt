package net.thunderbird.components.ui.bolt.molecule.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun EmailAddressInputPreview() {
    PreviewWithThemes {
        EmailAddressInput(
            onEmailAddressChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun EmailAddressInputWithErrorPreview() {
    PreviewWithThemes {
        EmailAddressInput(
            onEmailAddressChange = {},
            errorMessage = "Email address error",
        )
    }
}
