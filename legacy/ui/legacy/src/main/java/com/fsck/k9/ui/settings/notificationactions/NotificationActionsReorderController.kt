package com.fsck.k9.ui.settings.notificationactions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.thunderbird.core.preference.notification.NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN

private const val SWAP_HYSTERESIS_PX = 8f

internal data class ReorderVisibleItem(
    val key: String,
    val offset: Int,
    val size: Int,
)

internal class NotificationActionsReorderController(
    initialActions: List<MessageNotificationAction>,
    initialCutoff: Int,
    private val onStateChanged: (List<MessageNotificationAction>, Int) -> Unit,
) {
    private val renderedItemsState = mutableStateListOf<NotificationListItem>()
    private var cutoffIndexState by mutableIntStateOf(0)

    var draggedKey by mutableStateOf<String?>(null)
        private set

    var draggedOffsetY by mutableFloatStateOf(0f)
        private set

    private var dragDidMove = false

    val items: List<NotificationListItem>
        get() = renderedItemsState

    val cutoffIndex: Int
        get() = cutoffIndexState

    init {
        setState(initialActions = initialActions, initialCutoff = initialCutoff)
    }

    fun setState(
        initialActions: List<MessageNotificationAction>,
        initialCutoff: Int,
    ) {
        renderedItemsState.clear()
        renderedItemsState.addAll(buildRenderedItems(actions = initialActions, cutoff = initialCutoff))
        cutoffIndexState = renderedItemsState.indexOfFirst { it is NotificationListItem.Cutoff }
        draggedKey = null
        draggedOffsetY = 0f
        dragDidMove = false
    }

    fun startDrag(itemKey: String) {
        draggedKey = itemKey
        draggedOffsetY = 0f
        dragDidMove = false
    }

    fun dragBy(deltaY: Float, visibleItems: List<ReorderVisibleItem>) {
        val key = draggedKey ?: return
        draggedOffsetY += deltaY

        val fromIndex = renderedItemsState.indexOfFirst { it.key == key }
        val draggedItem = visibleItems.firstOrNull { it.key == key }
        if (fromIndex == -1 || draggedItem == null) return

        val swapDecision = when {
            deltaY > 0f -> {
                val nextItem = renderedItemsState
                    .getOrNull(fromIndex + 1)
                    ?.key
                    ?.let { nextKey -> visibleItems.firstOrNull { it.key == nextKey } }

                nextItem
                    ?.takeIf {
                        val draggedBottom = draggedItem.offset + draggedItem.size + draggedOffsetY
                        val nextThreshold = it.offset + (it.size / 2f)
                        draggedBottom >= nextThreshold + SWAP_HYSTERESIS_PX
                    }
                    ?.let {
                        SwapDecision(
                            toIndex = fromIndex + 1,
                            offsetCompensation = -it.size.toFloat(),
                        )
                    }
            }

            deltaY < 0f -> {
                val upwardSwap = calculateUpwardSwap(
                    fromIndex = fromIndex,
                    visibleItems = visibleItems,
                )
                upwardSwap
                    ?.takeIf {
                        val draggedTop = draggedItem.offset + draggedOffsetY
                        draggedTop <= it.threshold - SWAP_HYSTERESIS_PX
                    }
                    ?.let {
                        SwapDecision(
                            toIndex = fromIndex - 1,
                            offsetCompensation = it.offsetCompensation,
                        )
                    }
            }

            else -> null
        }

        if (swapDecision != null && moveRenderedItem(from = fromIndex, to = swapDecision.toIndex)) {
            dragDidMove = true
            draggedOffsetY += swapDecision.offsetCompensation
        }
    }

    private data class SwapDecision(
        val toIndex: Int,
        val offsetCompensation: Float,
    )

    private data class UpwardSwapConfig(
        val threshold: Float,
        val offsetCompensation: Float,
    )

    private fun calculateUpwardSwap(
        fromIndex: Int,
        visibleItems: List<ReorderVisibleItem>,
    ): UpwardSwapConfig? {
        val previousItemKey = renderedItemsState.getOrNull(fromIndex - 1)?.key
        val previousItem = previousItemKey?.let { previousKey -> visibleItems.firstOrNull { it.key == previousKey } }
        val maxPos = NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN
            .coerceAtMost(renderedItemsState.lastIndex)

        return if (previousItemKey == null || previousItem == null) {
            null
        } else if (previousItemKey == NotificationListItem.Cutoff.key && cutoffIndexState >= maxPos) {
            val lastAboveKey = renderedItemsState.getOrNull(fromIndex - 2)?.key
            val lastAboveItem = lastAboveKey?.let { aboveKey -> visibleItems.firstOrNull { it.key == aboveKey } }
            lastAboveItem?.let {
                UpwardSwapConfig(
                    threshold = it.offset + (it.size / 2f),
                    offsetCompensation = (previousItem.size + it.size).toFloat(),
                )
            }
        } else {
            UpwardSwapConfig(
                threshold = previousItem.offset + (previousItem.size / 2f),
                offsetCompensation = previousItem.size.toFloat(),
            )
        }
    }

    fun endDrag() {
        val shouldNotify = dragDidMove

        draggedOffsetY = 0f
        draggedKey = null
        dragDidMove = false

        if (!shouldNotify) return

        notifyStateChanged()
    }

    fun moveByStep(itemKey: String, delta: Int): Boolean {
        val from = renderedItemsState.indexOfFirst { it.key == itemKey }
        val to = from + delta
        val didMove = from != -1 && moveRenderedItem(from = from, to = to)
        if (didMove) {
            notifyStateChanged()
        }
        return didMove
    }

    fun canMove(itemKey: String, delta: Int): Boolean {
        val from = renderedItemsState.indexOfFirst { it.key == itemKey }
        if (from == -1) return false

        val to = from + delta
        val maxPos = NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN
            .coerceAtMost(renderedItemsState.lastIndex)

        // Don't allow the divider to be dragged to more than the max position
        return from in renderedItemsState.indices &&
            to in renderedItemsState.indices &&
            from != to &&
            !(from == cutoffIndexState && to > maxPos)
    }

    private fun moveRenderedItem(from: Int, to: Int): Boolean {
        val maxPos = NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN
            .coerceAtMost(renderedItemsState.lastIndex)
        val invalidIndices = from !in renderedItemsState.indices || to !in renderedItemsState.indices || from == to
        val dividerPastMax = from == cutoffIndexState && to > maxPos
        if (invalidIndices || dividerPastMax) return false

        val previousCutoff = cutoffIndexState
        val crossedCutoffUpward = previousCutoff in to..<from
        val previousLastAboveKey = renderedItemsState
            .getOrNull(previousCutoff - 1)
            ?.key

        renderedItemsState.add(to, renderedItemsState.removeAt(from))
        cutoffIndexState = renderedItemsState.indexOfFirst { it is NotificationListItem.Cutoff }

        if (crossedCutoffUpward && cutoffIndexState > maxPos && previousLastAboveKey != null) {
            val previousLastAboveIndex = renderedItemsState.indexOfFirst { it.key == previousLastAboveKey }
            if (previousLastAboveIndex in 0 until cutoffIndexState) {
                val kickedItem = renderedItemsState.removeAt(previousLastAboveIndex)
                cutoffIndexState -= 1
                renderedItemsState.add(cutoffIndexState + 1, kickedItem)
            }
        }
        if (cutoffIndexState > maxPos) {
            renderedItemsState.add(maxPos, renderedItemsState.removeAt(cutoffIndexState))
            cutoffIndexState = maxPos
        }

        return true
    }

    private fun buildRenderedItems(
        actions: List<MessageNotificationAction>,
        cutoff: Int,
    ): List<NotificationListItem> {
        val clampedCutoff = cutoff.coerceIn(0, actions.size)
        return buildList {
            actions.forEachIndexed { index, action ->
                if (index == clampedCutoff) add(NotificationListItem.Cutoff)
                add(NotificationListItem.Action(action = action))
            }
            if (clampedCutoff == actions.size) {
                add(NotificationListItem.Cutoff)
            }
        }
    }

    private fun notifyStateChanged() {
        val actions = renderedItemsState
            .filterIsInstance<NotificationListItem.Action>()
            .map { it.action }
        onStateChanged(actions, cutoffIndexState)
    }
}
