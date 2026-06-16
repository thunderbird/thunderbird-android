package net.thunderbird.components.ui.bolt.molecule

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.theme.MainTheme

@Composable
@Preview(showBackground = true)
internal fun PullToRefreshBoxPreview() {
    PreviewWithThemes {
        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = {},
            modifier = Modifier.fillMaxWidth()
                .height(MainTheme.sizes.medium),
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
                .height(MainTheme.sizes.medium),
        ) {
            Surface {
                TextBodyLarge("Refreshing ...")
            }
        }
    }
}
