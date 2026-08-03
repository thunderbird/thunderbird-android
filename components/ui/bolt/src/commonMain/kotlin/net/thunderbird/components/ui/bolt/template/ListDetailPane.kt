package net.thunderbird.components.ui.bolt.template

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevices

/**
 * A list and detail pane layout that can be used to display a list of items and a detail view.
 *
 * @param navigationController A [ListDetailNavigationController] that can be used to navigate between list
 *                             and detail panes.
 * @param listPane A composable that displays the list of items.
 * @param detailPane A composable that displays the detail view of an item.
 * @param modifier The modifier to apply to this layout.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun <T> ListDetailPane(
    navigationController: MutableState<ListDetailNavigationController<T>>,
    listPane: @Composable () -> Unit,
    detailPane: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<T>()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(navigator) {
        navigationController.value = DefaultListDetailNavigationController(
            navigator = navigator,
        )
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = navigator.canNavigateBack(),
    ) {
        coroutineScope.launch {
            navigator.navigateBack()
        }
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                listPane()
            }
        },
        detailPane = {
            navigator.currentDestination?.contentKey?.let { item ->
                AnimatedPane {
                    detailPane(item)
                }
            }
        },
        modifier = modifier,
    )
}

/**
 * Creates a [ListDetailNavigationController] that can be used to navigate between list and
 * detail panes in a [ListDetailPane].
 */
@Composable
fun <T> rememberListDetailNavigationController(): MutableState<ListDetailNavigationController<T>> {
    val defaultController = remember { NoOpListDetailNavigationController<T>() }
    return remember { mutableStateOf(defaultController) }
}

/**
 * A controller that can be used to navigate between list and detail panes in a [ListDetailPane].
 *
 * It is recommended to use [rememberListDetailNavigationController] to create an instance of this controller.
 *
 * @see rememberListDetailNavigationController
 */
interface ListDetailNavigationController<T> {
    fun canNavigateBack(): Boolean
    suspend fun navigateBack(): Boolean
    suspend fun navigateToDetail(item: T)

    fun paneCount(): Int
}

/**
 * A [ListDetailNavigationController] that does nothing.
 */
internal class NoOpListDetailNavigationController<T> : ListDetailNavigationController<T> {
    override fun canNavigateBack() = false
    override suspend fun navigateBack() = false
    override suspend fun navigateToDetail(item: T) = Unit

    override fun paneCount() = 1
}

/**
 * A [ListDetailNavigationController] that wrappes a [ThreePaneScaffoldNavigator] to navigate
 * between list and detail panes.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal class DefaultListDetailNavigationController<T>(
    private val navigator: ThreePaneScaffoldNavigator<T>,
) : ListDetailNavigationController<T> {
    override fun canNavigateBack() = navigator.canNavigateBack()
    override suspend fun navigateBack() = navigator.navigateBack()
    override suspend fun navigateToDetail(item: T) = navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, item)

    override fun paneCount(): Int = navigator.scaffoldDirective.maxHorizontalPartitions
}

@Composable
@PreviewDevices
internal fun ListDetailPanePreview() {
    PreviewWithTheme {
        val navigationController = rememberListDetailNavigationController<ListItem>()
        val coroutineScope = rememberCoroutineScope()

        ListDetailPane(
            navigationController = navigationController,
            listPane = {
                Surface(
                    color = Color.Yellow,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn {
                        itemsIndexed(createItems()) { _, item ->
                            ListItem(
                                item = item,
                                onClick = {
                                    coroutineScope.launch {
                                        navigationController.value.navigateToDetail(item)
                                    }
                                },
                            )
                        }
                    }
                }
            },
            detailPane = { item ->
                Surface(
                    color = Color.Red,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    ListItem(
                        item = item,
                        onClick = {
                            coroutineScope.launch {
                                navigationController.value.navigateBack()
                            }
                        },
                    )
                }
            },
        )
    }
}

@Composable
private fun ListItem(
    item: ListItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        TextTitleMedium(item.id)
        TextBodyMedium(item.title)
    }
}

@Serializable
internal data class ListItem(
    val id: String,
    val title: String,
)

private fun createItems(): List<ListItem> {
    return listOf(
        ListItem(
            id = "1",
            title = "Item 1",
        ),
        ListItem(
            id = "2",
            title = "Item 2",
        ),
        ListItem(
            id = "3",
            title = "Item 3",
        ),
    )
}
