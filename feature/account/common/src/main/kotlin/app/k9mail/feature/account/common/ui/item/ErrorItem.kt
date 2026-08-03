package app.k9mail.feature.account.common.ui.item

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.molecule.ErrorView

@Composable
fun LazyItemScope.ErrorItem(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    onRetry: () -> Unit = { },
) {
    ListItem(
        modifier = modifier,
    ) {
        ErrorView(
            title = title,
            message = message,
            onRetry = onRetry,
        )
    }
}
