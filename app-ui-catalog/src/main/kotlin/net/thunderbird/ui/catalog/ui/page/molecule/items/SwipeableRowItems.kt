package net.thunderbird.ui.catalog.ui.page.molecule.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.organism.snackbar.SnackbarHost
import app.k9mail.core.ui.compose.designsystem.organism.snackbar.rememberSnackbarHostState
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeBehaviour
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeDirection
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeDirectionAccessibilityAction
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeableRow
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.rememberSwipeableRowState
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.ui.catalog.R

@Composable
fun SwipeableRowItems(modifier: Modifier = Modifier) {
    val snackbarHostState = rememberSnackbarHostState()
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxWidth(),
    ) {
        val coroutineScope = rememberCoroutineScope()
        var snackbarShowingJob: Job? by remember { mutableStateOf(null) }

        DisposableEffect(Unit) {
            onDispose {
                snackbarShowingJob?.cancel()
                snackbarShowingJob = null
            }
        }
        fun onSwipeEnd(direction: SwipeDirection) {
            snackbarShowingJob?.cancel()
            snackbarShowingJob = coroutineScope.launch {
                snackbarHostState.showSnackbar("Swiped to the ${direction.name.lowercase()} direction.")
                snackbarShowingJob = null
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(MainTheme.spacings.triple),
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
        ) {
            TextTitleLarge(text = "Swipeable Row Items")
            DividerHorizontal()

            var threshold by remember { mutableFloatStateOf(value = 0.5f) }
            Column(verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default)) {
                TextLabelLarge(text = "Control the threshold to trigger the swipe: ")
                Slider(
                    value = threshold,
                    onValueChange = { threshold = it },
                    valueRange = 0f..1f,
                    steps = 9,
                    modifier = Modifier.padding(horizontal = MainTheme.spacings.triple),
                )
                TextLabelSmall(text = "Current selected: ${threshold * 100}% of the content's width.")
            }

            SwipeSection(
                threshold = threshold,
                subtitle = "Swipe to Dismiss",
                onSwipeEnd = ::onSwipeEnd,
            ) { threshold -> SwipeBehaviour.Dismiss(threshold = threshold) }
            SwipeSection(
                threshold = threshold,
                subtitle = "Swipe to Reveal",
                onSwipeEnd = ::onSwipeEnd,
            ) { threshold -> SwipeBehaviour.Reveal(threshold = threshold) }
            SwipeSection(
                threshold = threshold,
                subtitle = "Swipe to Reveal with Auto reset",
                onSwipeEnd = ::onSwipeEnd,
            ) { threshold ->
                SwipeBehaviour.Reveal(threshold = threshold, autoReset = true)
            }
        }
    }
}

@Composable
private fun SwipeSection(
    threshold: Float,
    subtitle: String,
    onSwipeEnd: (SwipeDirection) -> Unit,
    behaviourFactory: (Float) -> SwipeBehaviour,
) {
    TextTitleMedium(text = subtitle)
    DividerHorizontal()
    Column(
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
    ) {
        val behaviour = behaviourFactory(threshold)
        SwipeableRowBothDirections(behaviour, onSwipeEnd)
        SwipeableRowStartToEnd(behaviour, onSwipeEnd)
        SwipeableRowEndToStart(behaviour, onSwipeEnd)
        SwipeableRowDisabled(behaviour, onSwipeEnd)
        SwipeableRowCustomBackground(behaviour, onSwipeEnd)
    }
}

@Composable
private fun SwipeableRowBothDirections(behaviour: SwipeBehaviour, onSwipeEnd: (SwipeDirection) -> Unit) {
    Column {
        TextLabelLarge("Both directions")
        SwipeableRowItems(
            behaviour = behaviour,
            foregroundItemText = "Enabled on both directions.",
            backgroundItemText = { direction ->
                "Swiping from ${direction.name.lowercase()}."
            },
            directions = persistentSetOf(SwipeDirection.StartToEnd, SwipeDirection.EndToStart),
            onSwipeEnd = onSwipeEnd,
        )
    }
}

@Composable
private fun SwipeableRowStartToEnd(behaviour: SwipeBehaviour, onSwipeEnd: (SwipeDirection) -> Unit) {
    Column {
        TextLabelLarge("Start-to-End only")
        SwipeableRowItems(
            behaviour = behaviour,
            foregroundItemText = "Enabled on start-to-end direction.",
            backgroundItemText = { "Background Item" },
            directions = persistentSetOf(SwipeDirection.StartToEnd),
            onSwipeEnd = onSwipeEnd,
        )
    }
}

@Composable
private fun SwipeableRowEndToStart(behaviour: SwipeBehaviour, onSwipeEnd: (SwipeDirection) -> Unit) {
    Column {
        TextLabelLarge("End-to-Start only")
        SwipeableRowItems(
            behaviour = behaviour,
            foregroundItemText = "Enabled on end-to-start direction.",
            backgroundItemText = { "Background Item" },
            directions = persistentSetOf(SwipeDirection.EndToStart),
            onSwipeEnd = onSwipeEnd,
        )
    }
}

@Composable
private fun SwipeableRowDisabled(behaviour: SwipeBehaviour, onSwipeEnd: (SwipeDirection) -> Unit) {
    Column {
        TextLabelLarge("Disabled")
        SwipeableRowItems(
            foregroundItemText = "Disabled.",
            backgroundItemText = { error("Should not be visible") },
            directions = persistentSetOf(),
            behaviour = behaviour,
            onSwipeEnd = onSwipeEnd,
        ) { error("Should not be visible") }
    }
}

@Composable
private fun SwipeableRowCustomBackground(behaviour: SwipeBehaviour, onSwipeEnd: (SwipeDirection) -> Unit) {
    Column {
        TextLabelLarge("Custom background")
        SwipeableRowItems(
            behaviour = behaviour,
            foregroundItemText = "Custom background.",
            backgroundItemText = { direction ->
                "Swiped from ${direction.name.lowercase()}."
            },
            directions = persistentSetOf(SwipeDirection.StartToEnd, SwipeDirection.EndToStart),
            onSwipeEnd = onSwipeEnd,
        ) { direction ->
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
        }
    }
}

@Composable
private fun SwipeableRowItems(
    behaviour: SwipeBehaviour,
    foregroundItemText: String,
    backgroundItemText: (SwipeDirection) -> String,
    modifier: Modifier = Modifier,
    directions: ImmutableSet<SwipeDirection> = persistentSetOf(),
    onSwipeEnd: (SwipeDirection) -> Unit,
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
) {
    var swipeDirection by remember { mutableStateOf<SwipeDirection?>(null) }
    val startToEndBehaviour = if (SwipeDirection.StartToEnd in directions) behaviour else SwipeBehaviour.Disabled
    val endToStartBehaviour = if (SwipeDirection.EndToStart in directions) behaviour else SwipeBehaviour.Disabled
    val swipeableRowState = rememberSwipeableRowState(
        startToEndBehaviour = startToEndBehaviour,
        endToStartBehaviour = endToStartBehaviour,
        accessibilityActions = buildAccessibilityActions(startToEndBehaviour, endToStartBehaviour),
    )
    Column(
        modifier = modifier.padding(MainTheme.spacings.triple),
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
    ) {
        SwipeableRow(
            state = swipeableRowState,
            backgroundContent = {
                backgroundContent(swipeDirection ?: SwipeDirection.Settled)
            },
            gesturesEnabled = directions.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            onSwipeEnd = onSwipeEnd,
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

    }
}

@Composable
private fun buildAccessibilityActions(
    startToEndBehaviour: SwipeBehaviour,
    endToStartBehaviour: SwipeBehaviour,
): PersistentList<SwipeDirectionAccessibilityAction> = buildList {
    if (startToEndBehaviour != SwipeBehaviour.Disabled) {
        add(
            SwipeDirectionAccessibilityAction.StartToEndAccessibilityAction(startToEndBehaviour.actionId),
        )
    }
    if (endToStartBehaviour != SwipeBehaviour.Disabled) {
        add(
            SwipeDirectionAccessibilityAction.EndToStartAccessibilityAction(endToStartBehaviour.actionId),
        )
    }
}.toPersistentList()

private val SwipeBehaviour.actionId: Int
    get() = when (this) {
        SwipeBehaviour.Disabled -> -1
        is SwipeBehaviour.Dismiss -> R.string.swipe_accessibility_dismiss_custom_action
        is SwipeBehaviour.Reveal if autoReset -> R.string.swipe_accessibility_reveal_and_reset_custom_action
        is SwipeBehaviour.Reveal -> R.string.swipe_accessibility_reveal_custom_action
    }
