package app.k9mail.ui.catalog.ui.molecule.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import app.k9mail.core.ui.compose.designsystem.molecule.ErrorView
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingView
import app.k9mail.ui.catalog.ui.common.list.ItemOutlined
import app.k9mail.ui.catalog.ui.common.list.sectionHeaderItem
import app.k9mail.ui.catalog.ui.common.list.sectionSubtitleItem

fun LazyGridScope.stateItems() {
    sectionHeaderItem(text = "ErrorView")
    item {
        ItemOutlined {
            ErrorView(
                title = "Error",
                message = "Something went wrong",
            )
        }
    }

    sectionHeaderItem(text = "LoadingView")
    sectionSubtitleItem(text = "Default")
    item {
        ItemOutlined {
            LoadingView()
        }
    }
    sectionSubtitleItem(text = "With message")
    item {
        ItemOutlined {
            LoadingView(
                message = "Loading...",
            )
        }
    }
}
