package app.k9mail.ui.catalog.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveContentWithSurface
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> PagedContent(
    pages: ImmutableList<T>,
    initialPage: T,
    modifier: Modifier = Modifier,
    onRenderPage: LazyGridScope.(T) -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = pages.indexOf(initialPage),
        initialPageOffsetFraction = 0f,
    ) {
        pages.size
    }
    val coroutineScope = rememberCoroutineScope()

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
            ) { page ->
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(300.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                    horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                    verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                ) {
                    onRenderPage(pages[page])
                    item { Spacer(modifier = Modifier.height(MainTheme.sizes.smaller)) }
                }
            }
        }
    }
}
