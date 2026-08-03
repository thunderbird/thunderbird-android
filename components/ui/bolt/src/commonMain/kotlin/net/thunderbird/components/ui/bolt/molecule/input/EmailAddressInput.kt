package net.thunderbird.components.ui.bolt.molecule.input

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.textfield.TextFieldOutlinedEmailAddress
import net.thunderbird.components.ui.bolt.resources.Res
import net.thunderbird.components.ui.bolt.resources.bolt_molecule_email_address_input_label
import org.jetbrains.compose.resources.stringResource

@Composable
fun EmailAddressInput(
    onEmailAddressChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    emailAddress: String = "",
    errorMessage: String? = null,
    isEnabled: Boolean = true,
    contentPadding: PaddingValues = inputContentPadding(),
) {
    InputLayout(
        modifier = modifier,
        contentPadding = contentPadding,
        errorMessage = errorMessage,
    ) {
        TextFieldOutlinedEmailAddress(
            value = emailAddress,
            onValueChange = onEmailAddressChange,
            label = stringResource(Res.string.bolt_molecule_email_address_input_label),
            isEnabled = isEnabled,
            hasError = errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

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
