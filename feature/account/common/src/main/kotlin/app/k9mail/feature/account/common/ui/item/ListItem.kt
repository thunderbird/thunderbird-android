package app.k9mail.feature.account.common.ui.item

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.ListItem(
    modifier: Modifier = Modifier,
    contentPaddingValues: PaddingValues = defaultItemPadding(),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(contentPaddingValues)
            .animateItem()
            .fillMaxWidth()
            .then(modifier),
    ) {
        content()
    }
}
