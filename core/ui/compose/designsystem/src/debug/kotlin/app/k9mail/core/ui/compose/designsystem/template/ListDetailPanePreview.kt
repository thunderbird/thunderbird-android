package app.k9mail.core.ui.compose.designsystem.template

import android.os.Parcelable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

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
                        itemsIndexed(createItems()) { index, item ->
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

@Parcelize
internal data class ListItem(
    val id: String,
    val title: String,
) : Parcelable

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
