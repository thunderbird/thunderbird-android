package app.k9mail.core.ui.compose.designsystem.molecule

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.pulltorefresh.PullToRefreshBox as Material3PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()

    Material3PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .testTag("PullToRefreshBox"),
        state = state,
        contentAlignment = contentAlignment,
        indicator = {
            Indicator(
                modifier = Modifier.align(Alignment.TopCenter)
                    .testTag("PullToRefreshIndicator"),
                isRefreshing = isRefreshing,
                state = state,
            )
        },
        content = content,
    )
}
