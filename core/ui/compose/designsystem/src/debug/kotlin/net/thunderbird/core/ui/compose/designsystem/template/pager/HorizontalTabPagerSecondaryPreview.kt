package net.thunderbird.core.ui.compose.designsystem.template.pager

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemeLightDark
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge

@PreviewLightDark
@Composable
private fun HorizontalTabPagerSecondaryPreview() {
    PreviewWithThemeLightDark {
        val pages by remember {
            mutableStateOf(
                List(size = 10) {
                    object {
                        val tab = "Tab $it"
                        val page = "Content $it"
                    }
                },
            )
        }
        HorizontalTabPagerSecondary(
            initialSelected = pages.first(),
            modifier = Modifier.padding(top = 24.dp),
        ) {
            pages(items = pages, tabConfigBuilder = { TabSecondaryConfig(it.tab) }) {
                TextBodyLarge(it.page)
            }
        }
    }
}
