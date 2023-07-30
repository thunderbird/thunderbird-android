package app.k9mail.feature.account.oauth.ui.item

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.feature.account.common.ui.item.ListItem
import app.k9mail.feature.account.oauth.ui.view.SignInView

@Composable
internal fun LazyItemScope.SignInItem(
    onSignInClick: () -> Unit,
    isGoogleSignIn: Boolean,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
    ) {
        SignInView(
            onSignInClick = onSignInClick,
            isGoogleSignIn = isGoogleSignIn,
        )
    }
}
