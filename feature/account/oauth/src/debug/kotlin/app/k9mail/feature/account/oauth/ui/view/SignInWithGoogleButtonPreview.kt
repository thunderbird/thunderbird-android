package app.k9mail.feature.account.oauth.ui.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun SignInWithGoogleButtonPreview() {
    PreviewWithThemes {
        SignInWithGoogleButton(
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SignInWithGoogleButtonDisabledPreview() {
    PreviewWithThemes {
        SignInWithGoogleButton(
            onClick = {},
            enabled = false,
        )
    }
}
