package net.thunderbird.ui.catalog.ui.page.molecule.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.molecule.PullToRefreshBox
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("MagicNumber")
@Composable
fun PullToRefresh(
    modifier: Modifier = Modifier,
) {
    val isRefreshing = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = isRefreshing.value,
        onRefresh = {
            isRefreshing.value = true

            coroutineScope.launch {
                delay(2000)
                isRefreshing.value = false
            }
        },
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth(),
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
        ) {
            items(10) {
                TextTitleMedium(text = "Item $it")
            }
        }
    }
}
