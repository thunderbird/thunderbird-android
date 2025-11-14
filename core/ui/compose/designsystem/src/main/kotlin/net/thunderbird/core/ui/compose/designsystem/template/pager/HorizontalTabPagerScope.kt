package net.thunderbird.core.ui.compose.designsystem.template.pager

import androidx.compose.runtime.Composable

@DslMarker
private annotation class HorizontalTabPagerScopeDslMarker

/**
 * A DSL scope for building the pages within a [HorizontalTabPager].
 *
 * This scope provides the `pages` function to define the content and configuration for each tab/page
 * in the pager.
 *
 * @param T The type of the data associated with each page.
 */
@HorizontalTabPagerScopeDslMarker
interface HorizontalTabPagerScope<T> {
    /**
     * Defines the pages for the [HorizontalTabPager].
     *
     * This function should be called within the content lambda of the [HorizontalTabPager] to specify
     * the items that will be displayed as pages.
     *
     * @param items The list of data items to be represented as pages.
     * @param tabConfigBuilder A lambda that returns a [TabConfig] for a given item,
     *   used to configure the corresponding tab's appearance (e.g., its title).
     * @param itemContent The composable content to display for each page. The lambda receives the
     *   specific item for that page.
     */
    fun pages(
        items: List<T>,
        tabConfigBuilder: (T) -> TabConfig,
        itemContent: @Composable HorizontalTabPagerScope<T>.(item: T) -> Unit,
    )
}

/**
 * Configuration for a single tab within a horizontal pager.
 *
 * @param title The text to be displayed on the tab.
 */
data class TabConfig(
    val title: String,
)

/**
 * Represents a single page within a horizontal pager, combining its data, tab configuration, and content.
 *
 * This class encapsulates all the necessary information to render one tab and its corresponding page content
 * in a [HorizontalTabPager].
 *
 * @param T The type of the value associated with the page.
 * @property tabConfig The configuration for the tab associated with this page, such as its title.
 * @property value The unique value or data model instance representing this page.
 * @property content The composable lambda that defines the UI content for this page. It is rendered
 *   within the scope of a [HorizontalTabPagerScope].
 */
internal data class HorizontalPagerPage<T>(
    val tabConfig: TabConfig,
    val value: T,
    val content: @Composable HorizontalTabPagerScope<T>.(page: T) -> Unit,
)
