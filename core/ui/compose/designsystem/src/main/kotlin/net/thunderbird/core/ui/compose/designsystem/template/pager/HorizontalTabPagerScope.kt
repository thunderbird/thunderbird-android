package net.thunderbird.core.ui.compose.designsystem.template.pager

import androidx.compose.runtime.Composable

@DslMarker
private annotation class HorizontalTabPagerScopeDslMarker

/**
 * A DSL scope for building the pages within a [HorizontalTabPagerPrimary] or [HorizontalTabPagerSecondary].
 *
 * This scope provides the `pages` function to define the content and configuration for each tab/page
 * in the pager.
 *
 * @param T The type of the data associated with each page.
 */
@HorizontalTabPagerScopeDslMarker
interface HorizontalTabPagerScope<T, TConfig : TabConfig> {
    /**
     * Defines the pages for the Pager.
     *
     * This function should be called within the content lambda of the Horizontal Pager to specify
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
        tabConfigBuilder: (T) -> TConfig,
        itemContent: @Composable HorizontalTabPagerScope<T, TConfig>.(item: T) -> Unit,
    )
}

/**
 * Represents a single page within a horizontal pager, combining its data, tab configuration, and content.
 *
 * This class encapsulates all the necessary information to render one tab and its corresponding page content
 * in a Horizontal Pager.
 *
 * @param T The type of the value associated with the page.
 * @property tabConfig The configuration for the tab associated with this page, such as its title.
 * @property value The unique value or data model instance representing this page.
 * @property content The composable lambda that defines the UI content for this page. It is rendered
 *   within the scope of a [HorizontalTabPagerScope].
 */
internal data class HorizontalPagerPage<T, TConfig : TabConfig>(
    val tabConfig: TConfig,
    val value: T,
    val content: @Composable HorizontalTabPagerScope<T, TConfig>.(page: T) -> Unit,
)
