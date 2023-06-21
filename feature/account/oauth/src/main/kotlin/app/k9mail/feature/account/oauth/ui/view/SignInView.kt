package app.k9mail.feature.account.oauth.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.k9mail.core.ui.compose.designsystem.atom.button.Button
import app.k9mail.core.ui.compose.designsystem.atom.text.TextCaption
import app.k9mail.core.ui.compose.designsystem.atom.text.TextSubtitle1
import app.k9mail.core.ui.compose.designsystem.molecule.input.EmailAddressInput
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.feature.account.oauth.R

@Composable
internal fun SignInView(
    emailAddress: String,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
        modifier = modifier,
    ) {
        TextSubtitle1(text = stringResource(id = R.string.account_oauth_sign_in_title))

        EmailAddressInput(
            emailAddress = emailAddress,
            onEmailAddressChange = {},
            isEnabled = false,
        )

        TextCaption(
            text = stringResource(id = R.string.account_oauth_sign_in_description),
            textAlign = TextAlign.Center,
        )

        Button(
            text = stringResource(id = R.string.account_oauth_sign_in_button),
            onClick = onSignInClick,
        )
    }
}
