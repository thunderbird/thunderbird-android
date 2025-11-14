package net.thunderbird.core.ui.compose.designsystem.template.pager

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.coroutines.launch
import net.thunderbird.core.ui.compose.designsystem.atom.pager.Tab
import net.thunderbird.core.ui.compose.designsystem.molecule.pager.TabRow

/**
 * A Composable that displays a horizontal pager with a tab row.
 *
 * This component combines a [TabRow] with a [HorizontalPager] to create a swipeable view with
 * corresponding tabs. The content of the pager is defined using a DSL provided by the [HorizontalTabPagerScope].
 *
 * Example usage:
 * ```
 * HorizontalTabPager(initialSelected = "Tab 1") {
 *     pages(
 *         items = listOf("Tab 1", "Tab 2", "Tab 3"),
 *         tabConfigBuilder = { TabConfig(title = it) },
 *     ) { item ->
 *         // Content for each page
 *         Box(
 *             modifier = Modifier.fillMaxSize(),
 *             contentAlignment = Alignment.Center
 *         ) {
 *             Text(text = "Content for $item")
 *         }
 *     }
 * }
 * ```
 *
 * @param T The type of the data representing each page.
 * @param initialSelected The initial page to be selected.
 * @param modifier The [Modifier] to be applied to the container [Column].
 * @param content A lambda with a receiver of [HorizontalTabPagerScope] that defines the pages.
 *                Use the `pages` function within this scope to configure the tabs and their content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> HorizontalTabPager(
    initialSelected: T,
    modifier: Modifier = Modifier,
    content: HorizontalTabPagerScope<T>.() -> Unit,
) {
    val scope = remember(initialSelected, content) {
        HorizontalTabPagerImpl(initialSelected).apply { content() }
    }

    if (scope.pages.isEmpty()) return
    val state = rememberPagerState(
        initialPage = scope.initialPageIndex,
        pageCount = { scope.pages.size },
    )
    val coroutineScope = rememberCoroutineScope()
    Column(modifier = modifier) {
        TabRow(
            selectedTabIndex = state.currentPage,
            modifier = Modifier.fillMaxWidth(),
        ) {
            scope.pages.forEachIndexed { index, page ->
                Tab(
                    selected = state.currentPage == index,
                    title = {
                        if (state.currentPage == index) {
                            TextTitleMedium(page.tabConfig.title)
                        } else {
                            TextBodyLarge(page.tabConfig.title)
                        }
                    },
                    onClick = {
                        coroutineScope.launch {
                            state.animateScrollToPage(scope.pages.indexOf(page))
                        }
                    },
                )
            }
        }

        HorizontalPager(
            state = state,
            contentPadding = PaddingValues(all = MainTheme.spacings.default),
        ) { index ->
            val page = scope.pages[index]
            page.content(scope, page.value)
        }
    }
}

private class HorizontalTabPagerImpl<T>(
    private val initialPage: T,
) : HorizontalTabPagerScope<T> {
    val initialPageIndex get() = pages.indexOfFirst { it.value == initialPage }.takeIf { it >= 0 } ?: 0
    val pages = mutableListOf<HorizontalPagerPage<T>>()

    override fun pages(
        items: List<T>,
        tabConfigBuilder: (T) -> TabConfig,
        itemContent: @Composable (HorizontalTabPagerScope<T>.(T) -> Unit),
    ) {
        pages.addAll(
            items.map { item ->
                HorizontalPagerPage(
                    tabConfig = tabConfigBuilder(item),
                    value = item,
                    content = itemContent,
                )
            },
        )
    }
}
