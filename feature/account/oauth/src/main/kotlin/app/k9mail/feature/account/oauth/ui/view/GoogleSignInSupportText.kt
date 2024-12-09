package app.k9mail.feature.account.oauth.ui.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import app.k9mail.core.ui.compose.common.resources.annotatedStringResource
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.account.oauth.R

private const val GOOGLE_OAUTH_SUPPORT_PAGE = "https://support.thunderbird.net/kb/gmail-thunderbird-android"

@Composable
internal fun GoogleSignInSupportText() {
    val extraText = annotatedStringResource(
        id = R.string.account_oauth_google_sign_in_support_text,
        argument = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    color = MainTheme.colors.primary,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                withLink(LinkAnnotation.Url(GOOGLE_OAUTH_SUPPORT_PAGE)) {
                    append(stringResource(R.string.account_oauth_google_sign_in_support_text_link_text))
                }
            }
        },
    )

    TextBodySmall(
        text = extraText,
        textAlign = TextAlign.Center,
    )
}
