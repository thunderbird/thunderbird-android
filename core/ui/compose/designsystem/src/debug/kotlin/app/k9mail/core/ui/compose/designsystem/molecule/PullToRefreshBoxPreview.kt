package app.k9mail.core.ui.compose.designsystem.molecule

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.theme2.MainTheme

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
