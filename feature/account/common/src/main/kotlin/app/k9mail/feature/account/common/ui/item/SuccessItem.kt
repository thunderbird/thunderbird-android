package app.k9mail.feature.account.common.ui.item

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.feature.account.common.ui.view.SuccessView

@Composable
fun LazyItemScope.SuccessItem(
    message: String,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
    ) {
        SuccessView(
            message = message,
        )
    }
}
