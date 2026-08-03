package app.k9mail.feature.account.oauth.ui.view

import androidx.compose.runtime.Composable
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevices

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
