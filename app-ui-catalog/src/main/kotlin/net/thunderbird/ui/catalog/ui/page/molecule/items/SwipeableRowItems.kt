package net.thunderbird.ui.catalog.ui.page.molecule.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.organism.snackbar.SnackbarHost
import app.k9mail.core.ui.compose.designsystem.organism.snackbar.rememberSnackbarHostState
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.launch
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeDirection
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeableRow
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.rememberSwipeableRowState
import net.thunderbird.ui.catalog.ui.page.common.list.fullSpanItem
import net.thunderbird.ui.catalog.ui.page.common.list.sectionHeaderItem
import net.thunderbird.ui.catalog.ui.page.common.list.sectionSubtitleItem

fun LazyGridScope.swipeableRowItems() {
    sectionHeaderItem("Swipeable Row Items")
    swipeableRowBothDirections()
    swipeableRowStartToEnd()
    swipeableRowEndToStart()
    swipeableRowDisabled()
    swipeableRowCustomBackground()
    swipeableRowWithEarlyDismiss()
}

private fun LazyGridScope.swipeableRowBothDirections() {
    sectionSubtitleItem("Both directions")
    fullSpanItem {
        SwipeableRowItems(
            foregroundItemText = "Enabled on both directions.",
            backgroundItemText = { direction ->
                "Swiping from ${direction.name.lowercase()}."
            },
            direction = persistentSetOf(SwipeDirection.StartToEnd, SwipeDirection.EndToStart),
        )
    }
}

private fun LazyGridScope.swipeableRowStartToEnd() {
    sectionSubtitleItem("Start-to-End only")
    fullSpanItem {
        SwipeableRowItems(
            foregroundItemText = "Enabled on start-to-end direction.",
            backgroundItemText = { "Background Item" },
            direction = persistentSetOf(SwipeDirection.StartToEnd),
        )
    }
}

private fun LazyGridScope.swipeableRowEndToStart() {
    sectionSubtitleItem("End-to-Start only")
    fullSpanItem {
        SwipeableRowItems(
            foregroundItemText = "Enabled on end-to-start direction.",
            backgroundItemText = { "Background Item" },
            direction = persistentSetOf(SwipeDirection.EndToStart),
        )
    }
}

private fun LazyGridScope.swipeableRowDisabled() {
    sectionSubtitleItem("Disabled")
    fullSpanItem {
        SwipeableRowItems(
            foregroundItemText = "Disabled.",
            backgroundItemText = { error("Should not be visible") },
            direction = persistentSetOf(),
        )
    }
}

private fun LazyGridScope.swipeableRowCustomBackground() {
    sectionSubtitleItem("Custom background")
    fullSpanItem {
        SwipeableRowItems(
            foregroundItemText = "Custom background.",
            backgroundItemText = { direction ->
                "Swiped from ${direction.name.lowercase()}."
            },
            backgroundContent = { direction ->
                Surface(
                    color = if (direction == SwipeDirection.StartToEnd) {
                        MainTheme.colors.error
                    } else {
                        MainTheme.colors.success
                    },
                    contentColor = MainTheme.colors.onPrimary,
                ) {
                    TextBodyLarge(
                        text = "Swiped from ${direction.name.lowercase()}.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MainTheme.spacings.quadruple),
                        textAlign = when (direction) {
                            SwipeDirection.StartToEnd -> TextAlign.Start
                            SwipeDirection.EndToStart -> TextAlign.End
                            SwipeDirection.Settled -> TextAlign.Unspecified
                        },
                    )
                }
            },
            direction = persistentSetOf(SwipeDirection.StartToEnd, SwipeDirection.EndToStart),
        )
    }
}

private fun LazyGridScope.swipeableRowWithEarlyDismiss() {
    sectionSubtitleItem("With early dismiss cancellation")
    fullSpanItem {
        SwipeableRowItems(
            foregroundItemText = "Swiping this item won't dismiss it, but it triggers the early dismiss.",
            backgroundItemText = { direction ->
                "Swiping from ${direction.name.lowercase()}."
            },
            direction = persistentSetOf(SwipeDirection.StartToEnd, SwipeDirection.EndToStart),
            swipeActionThreshold = { 0.5f },
        )
    }
}

@Composable
private fun SwipeableRowItems(
    foregroundItemText: String,
    backgroundItemText: (SwipeDirection) -> String,
    modifier: Modifier = Modifier,
    direction: ImmutableSet<SwipeDirection> = persistentSetOf(),
    backgroundContent: @Composable RowScope.(SwipeDirection) -> Unit = { direction ->
        Surface(
            color = MainTheme.colors.primaryContainer,
            contentColor = MainTheme.colors.onPrimaryContainer,
            modifier = Modifier.fillMaxSize(),
        ) {
            Row {
                TextBodyLarge(
                    text = backgroundItemText(direction),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MainTheme.spacings.quadruple, horizontal = MainTheme.spacings.double),
                    textAlign = when (direction) {
                        SwipeDirection.StartToEnd -> TextAlign.Start
                        SwipeDirection.EndToStart -> TextAlign.End
                        SwipeDirection.Settled -> TextAlign.Unspecified
                    },
                )
            }
        }
    },
    swipeActionThreshold: (SwipeDirection) -> Float? = { null },
) {
    val snackbarHostState = rememberSnackbarHostState()
    val coroutineScope = rememberCoroutineScope()
    var swipeDirection by remember { mutableStateOf<SwipeDirection?>(null) }
    val swipeableRowState = rememberSwipeableRowState(swipeActionThreshold = swipeActionThreshold)
    Column(
        modifier = modifier.padding(MainTheme.spacings.triple),
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
    ) {
        SwipeableRow(
            state = swipeableRowState,
            backgroundContent = backgroundContent,
            enableDismissFromEndToStart = direction.contains(SwipeDirection.EndToStart),
            enableDismissFromStartToEnd = direction.contains(SwipeDirection.StartToEnd),
            gesturesEnabled = direction.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            onSwipeEnd = {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Swiped to the ${it.name.lowercase()} direction.")
                }
            },
            onSwipeChange = { swipeDirection = it },
        ) {
            Surface(
                color = MainTheme.colors.surfaceContainer,
            ) {
                TextBodyLarge(
                    text = foregroundItemText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MainTheme.spacings.quadruple),
                )
            }
        }

        TextBodyLarge(
            text = swipeDirection?.let { "Swiping to the ${it.name.lowercase()} direction." } ?: "Not swiping yet.",
        )

        SnackbarHost(snackbarHostState)
    }
}
