package app.k9mail.feature.account.oauth.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.account.oauth.R

@Composable
internal fun SignInView(
    onSignInClick: () -> Unit,
    isGoogleSignIn: Boolean,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
        modifier = modifier,
    ) {
        TextBodySmall(
            text = stringResource(id = R.string.account_oauth_sign_in_description),
            textAlign = TextAlign.Center,
        )

        if (isGoogleSignIn) {
            SignInWithGoogleButton(
                onClick = onSignInClick,
                enabled = isEnabled,
            )
        } else {
            ButtonFilled(
                text = stringResource(id = R.string.account_oauth_sign_in_button),
                onClick = onSignInClick,
                enabled = isEnabled,
            )
        }
    }
}
