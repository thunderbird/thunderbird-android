package net.thunderbird.core.ui.compose.designsystem.template.pager

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.coroutines.launch
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.tab.TabPrimary
import net.thunderbird.core.ui.compose.designsystem.molecule.tab.TabRowPrimary

/**
 * A Composable that displays a horizontal pager with a tab row.
 *
 * This component combines a [TabRowPrimary] with a [HorizontalPager] to create a
 * swipeable view with corresponding tabs. The content of the pager is defined using
 * a DSL provided by the [HorizontalTabPagerScope].
 *
 * Example usage:
 * ```
 * val pages = listOf(
 *      Icons.Outlined.Inbox to "Inbox",
 *      Icons.Outlined.Outbox to "Outbox",
 *      Icons.Outlined.Spam to "Spam",
 * )
 * HorizontalTabPagerPrimary(initialSelected = pages.first()) {
 *     pages(
 *         items = pages,
 *         tabConfigBuilder = { (icon, title) ->
 *              TabPrimaryConfig(title = title, icon = icon)
 *         },
 *     ) { item ->
 *         // Content for each page
 *         Box(
 *             modifier = Modifier.fillMaxSize(),
 *             contentAlignment = Alignment.Center,
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
fun <T> HorizontalTabPagerPrimary(
    initialSelected: T,
    modifier: Modifier = Modifier,
    @FloatRange(from = -0.5, to = 0.5) initialPageOffsetFraction: Float = 0f,
    onPageChange: (T) -> Unit = {},
    content: HorizontalTabPagerScope<T, TabPrimaryConfig>.() -> Unit,
) {
    val scope = remember(initialSelected, content) {
        HorizontalTabPagerPrimaryImpl(initialSelected).apply { content() }
    }

    if (scope.pages.isEmpty()) return
    val state = rememberPagerState(
        initialPage = scope.initialPageIndex,
        pageCount = { scope.pages.size },
        initialPageOffsetFraction = initialPageOffsetFraction,
    )

    LaunchedEffect(state.settledPage, onPageChange) {
        onPageChange(scope.pages[state.settledPage].value)
    }

    val coroutineScope = rememberCoroutineScope()
    Column(modifier = modifier) {
        TabRowPrimary(
            selectedTabIndex = state.currentPage,
            modifier = Modifier.fillMaxWidth(),
        ) {
            scope.pages.forEachIndexed { index, page ->
                TabPrimary(
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
                    icon = page.tabConfig.icon?.let { icon ->
                        { Icon(imageVector = icon, contentDescription = page.tabConfig.contentDescription) }
                    },
                    badge = page.tabConfig.badgeCount?.let { count -> { TextLabelSmall(text = count.toString()) } },
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

private class HorizontalTabPagerPrimaryImpl<T>(
    private val initialPage: T,
) : HorizontalTabPagerScope<T, TabPrimaryConfig> {
    val initialPageIndex get() = pages.indexOfFirst { it.value == initialPage }.takeIf { it >= 0 } ?: 0
    val pages = mutableListOf<HorizontalPagerPage<T, TabPrimaryConfig>>()

    override fun pages(
        items: List<T>,
        tabConfigBuilder: (T) -> TabPrimaryConfig,
        itemContent: @Composable (HorizontalTabPagerScope<T, TabPrimaryConfig>.(T) -> Unit),
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
