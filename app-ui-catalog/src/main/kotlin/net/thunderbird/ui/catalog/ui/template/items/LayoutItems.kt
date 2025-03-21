package net.thunderbird.ui.catalog.ui.template.items

import android.os.Parcelable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.template.ListDetailPane
import app.k9mail.core.ui.compose.designsystem.template.rememberListDetailNavigationController
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.parcelize.Parcelize
import net.thunderbird.ui.catalog.ui.common.list.defaultItem
import net.thunderbird.ui.catalog.ui.common.list.sectionHeaderItem

fun LazyGridScope.layoutItems() {
    sectionHeaderItem(text = "ListDetailPane")
    defaultItem {
        ListDetailPaneItem()
    }
}

@Composable
private fun ListDetailPaneItem() {
    val navigationController = rememberListDetailNavigationController<ListItem>()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(MainTheme.sizes.huger)
            .padding(MainTheme.spacings.double),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ListDetailPane(
            navigationController = navigationController,
            listPane = {
                Surface(
                    color = MainTheme.colors.primaryContainer,
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                    ) {
                        TextTitleMedium("List pane")
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
                        ) {
                            itemsIndexed(createItems()) { index, item ->
                                ListItem(
                                    item = item,
                                    onClick = {
                                        navigationController.value.navigateToDetail(item)
                                    },
                                )
                            }
                        }
                    }
                }
            },
            detailPane = { item ->
                Surface(
                    color = MainTheme.colors.secondaryContainer,
                ) {
                    ListDetail(
                        item = item,
                        onClick = { navigationController.value.navigateBack() },
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
        modifier = Modifier.clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(MainTheme.spacings.default),
    ) {
        TextBodyLarge(item.title)
    }
}

@Composable
private fun ListDetail(
    item: ListItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(MainTheme.spacings.default),
    ) {
        TextTitleMedium("Detail pane")
        Spacer(modifier = Modifier.height(MainTheme.spacings.default))
        TextBodyLarge(item.title)
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
