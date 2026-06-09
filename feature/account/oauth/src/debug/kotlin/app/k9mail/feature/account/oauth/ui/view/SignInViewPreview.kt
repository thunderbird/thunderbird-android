package app.k9mail.feature.account.oauth.ui.view

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import net.thunderbird.core.ui.common.annotation.PreviewDevices

@PreviewDevices
@Composable
internal fun SignInViewPreview() {
    PreviewWithTheme {
        SignInView(
            onSignInClick = {},
            isGoogleSignIn = false,
        )
    }
}

@PreviewDevices
@Composable
internal fun SignInViewWithGooglePreview() {
    PreviewWithTheme {
        SignInView(
            onSignInClick = {},
            isGoogleSignIn = true,
        )
    }
}
