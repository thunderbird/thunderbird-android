package app.k9mail.ui.catalog.ui.molecule.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.molecule.ContentLoadingErrorState
import app.k9mail.core.ui.compose.designsystem.molecule.ContentLoadingErrorView
import app.k9mail.core.ui.compose.designsystem.molecule.ErrorView
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingView
import app.k9mail.ui.catalog.ui.common.list.ItemOutlined
import app.k9mail.ui.catalog.ui.common.list.sectionHeaderItem
import app.k9mail.ui.catalog.ui.common.list.sectionInfoItem
import app.k9mail.ui.catalog.ui.common.list.sectionSubtitleItem

fun LazyGridScope.stateItems() {
    sectionHeaderItem(text = "ErrorView")
    item {
        ItemOutlined {
            ErrorView(
                title = "Error",
            )
        }
    }
    item {
        ItemOutlined {
            ErrorView(
                title = "Error with message",
                message = "Something went wrong",
            )
        }
    }
    item {
        ItemOutlined {
            ErrorView(
                title = "Error with retry",
                onRetry = {},
            )
        }
    }
    item {
        ItemOutlined {
            ErrorView(
                title = "Error with retry and message",
                message = "Something went wrong",
                onRetry = {},
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

    sectionHeaderItem(text = "ContentLoadingErrorView")
    sectionInfoItem(text = "Click below to change state")
    item {
        Column {
            ItemOutlined {
                StatefulContentLoadingErrorState()
            }
        }
    }
}

@Composable
private fun StatefulContentLoadingErrorState() {
    val state = remember {
        mutableStateOf(ContentLoadingErrorState.Loading)
    }

    ContentLoadingErrorView(
        state = state.value,
        modifier = Modifier
            .clickable {
                when (state.value) {
                    ContentLoadingErrorState.Loading -> {
                        state.value = ContentLoadingErrorState.Content
                    }

                    ContentLoadingErrorState.Content -> {
                        state.value = ContentLoadingErrorState.Error
                    }

                    ContentLoadingErrorState.Error -> {
                        state.value = ContentLoadingErrorState.Loading
                    }
                }
            }
            .height(200.dp)
            .fillMaxSize(),
        error = {
            TextTitleMedium(text = "Error")
        },
        loading = {
            TextTitleMedium(text = "Loading...")
        },
        content = {
            TextTitleMedium(text = "Content")
        },
    )
}
