package net.thunderbird.ui.catalog.ui.page.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveContentWithSurface
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
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
    val pagerState = rememberPagerState(
        initialPage = pages.indexOf(initialPage),
        initialPageOffsetFraction = 0f,
    ) {
        pages.size
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.settledPage) {
        onEvent(CatalogPageContract.Event.OnPageChanged(pages[pagerState.settledPage]))
    }

    Column(
        modifier = modifier,
    ) {
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
        ) {
            pages.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                            onEvent(CatalogPageContract.Event.OnPageChanged(pages[index]))
                        }
                    },
                    text = { Text(text = title.toString()) },
                )
            }
        }
        ResponsiveContentWithSurface {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize(),
            ) { pageIndex ->
                if (pages[pageIndex].isFullScreen) {
                    onRenderFullScreenPage(pages[pageIndex])
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(MainTheme.sizes.larger),
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding(),
                        horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                    ) {
                        onRenderPage(pages[pageIndex])
                        fullSpanItem { Spacer(modifier = Modifier.height(MainTheme.sizes.smaller)) }
                    }
                }
            }
        }
    }
}
