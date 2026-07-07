package net.thunderbird.components.ui.catalog.ui.page.molecule.items

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
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.components.ui.bolt.molecule.ContentLoadingErrorState
import net.thunderbird.components.ui.bolt.molecule.ContentLoadingErrorView
import net.thunderbird.components.ui.bolt.molecule.ContentLoadingState
import net.thunderbird.components.ui.bolt.molecule.ContentLoadingView
import net.thunderbird.components.ui.bolt.molecule.ErrorView
import net.thunderbird.components.ui.bolt.molecule.LoadingView
import net.thunderbird.components.ui.catalog.ui.page.common.list.ItemOutlinedView
import net.thunderbird.components.ui.catalog.ui.page.common.list.fullSpanItem
import net.thunderbird.components.ui.catalog.ui.page.common.list.sectionHeaderItem
import net.thunderbird.components.ui.catalog.ui.page.common.list.sectionInfoItem
import net.thunderbird.components.ui.catalog.ui.page.common.list.sectionSubtitleItem

@Suppress("LongMethod")
fun LazyGridScope.stateItems() {
    sectionHeaderItem(text = "ErrorView")
    fullSpanItem {
        ItemOutlinedView {
            ErrorView(
                title = "Error",
            )
        }
    }
    fullSpanItem {
        ItemOutlinedView {
            ErrorView(
                title = "Error with message",
                message = "Something went wrong",
            )
        }
    }
    fullSpanItem {
        ItemOutlinedView {
            ErrorView(
                title = "Error with retry",
                onRetry = {},
            )
        }
    }
    fullSpanItem {
        ItemOutlinedView {
            ErrorView(
                title = "Error with retry and message",
                message = "Something went wrong",
                onRetry = {},
            )
        }
    }

    sectionHeaderItem(text = "LoadingView")
    sectionSubtitleItem(text = "Default")
    fullSpanItem {
        ItemOutlinedView {
            LoadingView()
        }
    }
    sectionSubtitleItem(text = "With message")
    fullSpanItem {
        ItemOutlinedView {
            LoadingView(
                message = "Loading...",
            )
        }
    }

    sectionHeaderItem(text = "ContentLoadingView")
    sectionInfoItem(text = "Click below to change state")
    fullSpanItem {
        Column {
            ItemOutlinedView {
                StatefulContentLoadingView()
            }
        }
    }

    sectionHeaderItem(text = "ContentLoadingErrorView")
    sectionInfoItem(text = "Click below to change state")
    fullSpanItem {
        Column {
            ItemOutlinedView {
                StatefulContentLoadingErrorView()
            }
        }
    }
}

@Composable
private fun StatefulContentLoadingView() {
    val state = remember {
        mutableStateOf<ContentLoadingState>(ContentLoadingState.Loading)
    }

    ContentLoadingView(
        state = state.value,
        modifier = Modifier
            .clickable {
                when (state.value) {
                    ContentLoadingState.Loading -> {
                        state.value = ContentLoadingState.Content
                    }

                    ContentLoadingState.Content -> {
                        state.value = ContentLoadingState.Loading
                    }
                }
            }
            .height(200.dp)
            .fillMaxSize(),
        loading = {
            TextTitleMedium(text = "Loading...")
        },
        content = {
            TextTitleMedium(text = "Content")
        },
    )
}

@Composable
private fun StatefulContentLoadingErrorView() {
    val state = remember {
        mutableStateOf<ContentLoadingErrorState>(ContentLoadingErrorState.Loading)
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
