package net.thunderbird.components.ui.bolt.molecule

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox as Material3PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.theme.BoltTheme

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
        modifier = modifier.testTag("PullToRefreshBox"),
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

@Composable
@Preview(showBackground = true)
internal fun PullToRefreshBoxPreview() {
    PreviewWithThemes {
        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = {},
            modifier = Modifier.fillMaxWidth()
                .height(BoltTheme.sizes.medium),
        ) {
            Surface {
                TextBodyLarge("Pull to refresh")
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun PullToRefreshBoxRefreshingPreview() {
    PreviewWithThemes {
        PullToRefreshBox(
            isRefreshing = true,
            onRefresh = {},
            modifier = Modifier.fillMaxWidth()
                .height(BoltTheme.sizes.medium),
        ) {
            Surface {
                TextBodyLarge("Refreshing ...")
            }
        }
    }
}
