package net.thunderbird.ui.catalog.ui.page.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveContentWithSurface
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.core.ui.compose.designsystem.template.pager.HorizontalTabPagerPrimary
import net.thunderbird.core.ui.compose.designsystem.template.pager.TabPrimaryConfig
import net.thunderbird.ui.catalog.ui.page.CatalogPageContract
import net.thunderbird.ui.catalog.ui.page.CatalogPageContract.CatalogPage
import net.thunderbird.ui.catalog.ui.page.common.list.fullSpanItem

@Composable
fun <T : CatalogPage> PagedContent(
    pages: ImmutableList<T>,
    initialPage: T,
    onRenderPage: LazyGridScope.(T) -> Unit,
    onEvent: (CatalogPageContract.Event) -> Unit,
    modifier: Modifier = Modifier,
    onRenderFullScreenPage: @Composable (T) -> Unit = {},
) {
    HorizontalTabPagerPrimary(
        initialSelected = initialPage,
        onPageChange = { page ->
            onEvent(CatalogPageContract.Event.OnPageChanged(page))
        },
        initialPageOffsetFraction = 0f,
        modifier = modifier,
    ) {
        pages(
            items = pages,
            tabConfigBuilder = { page ->
                TabPrimaryConfig(title = page.displayName)
            },
        ) { page ->
            ResponsiveContentWithSurface {
                if (page.isFullScreen) {
                    onRenderFullScreenPage(page)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(MainTheme.sizes.larger),
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding(),
                        horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                    ) {
                        onRenderPage(page)
                        fullSpanItem { Spacer(modifier = Modifier.height(MainTheme.sizes.smaller)) }
                    }
                }
            }
        }
    }
}
