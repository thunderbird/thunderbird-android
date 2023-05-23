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
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlinedEmailAddress
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes

@Composable
fun EmailAddressInput(
    onEmailAddressChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    emailAddress: String = "",
    errorMessage: String? = null,
    contentPadding: PaddingValues = inputContentPadding(),
) {
    Column(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxWidth()
            .then(modifier),
    ) {
        TextFieldOutlinedEmailAddress(
            value = emailAddress,
            onValueChange = onEmailAddressChange,
            label = stringResource(id = R.string.designsystem_molecule_email_address_input_label),
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
internal fun EmailAddressInputPreview() {
    PreviewWithThemes {
        EmailAddressInput(
            onEmailAddressChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun EmailAddressInputWithErrorPreview() {
    PreviewWithThemes {
        EmailAddressInput(
            onEmailAddressChange = {},
            errorMessage = "Email address error",
        )
    }
}
