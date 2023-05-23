package app.k9mail.core.ui.compose.designsystem.molecule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
) {
    Column(
        modifier = Modifier
            .padding(
                horizontal = MainTheme.spacings.double,
                vertical = MainTheme.spacings.default,
            )
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextFieldOutlinedEmailAddress(
            value = emailAddress,
            onValueChange = onEmailAddressChange,
            label = stringResource(id = R.string.designsystem_molecule_email_address_input_label),
            hasError = errorMessage != null,
        )
        AnimatedVisibility(visible = errorMessage != null) {
            TextCaption(
                text = errorMessage ?: "",
                modifier = Modifier.padding(top = MainTheme.spacings.default),
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
