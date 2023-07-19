package app.k9mail.feature.account.common.ui.item

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingView

@Composable
fun LazyItemScope.LoadingItem(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    ListItem(
        modifier = modifier,
    ) {
        LoadingView(
            message = message,
        )
    }
}
