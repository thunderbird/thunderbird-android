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

        val draggedItem = visibleItems.firstOrNull { it.key == key } ?: return
        val fromIndex = renderedItemsState.indexOfFirst { it.key == key }
        if (fromIndex == -1) return

        val swapDecision = findSwapDecision(
            deltaY = deltaY,
            fromIndex = fromIndex,
            draggedItem = draggedItem,
            visibleItems = visibleItems,
        ) ?: return

        if (moveRenderedItem(from = fromIndex, to = swapDecision.toIndex)) {
            dragDidMove = true
            draggedOffsetY += swapDecision.offsetCompensation
        }
    }

    private data class SwapDecision(
        val toIndex: Int,
        val offsetCompensation: Float,
    )

    private fun findSwapDecision(
        deltaY: Float,
        fromIndex: Int,
        draggedItem: ReorderVisibleItem,
        visibleItems: List<ReorderVisibleItem>,
    ): SwapDecision? {
        return when {
            deltaY > 0f -> findDownwardSwapDecision(
                fromIndex = fromIndex,
                draggedItem = draggedItem,
                visibleItems = visibleItems,
            )

            deltaY < 0f -> findUpwardSwapDecision(
                fromIndex = fromIndex,
                draggedItem = draggedItem,
                visibleItems = visibleItems,
            )

            else -> null
        }
    }

    private fun findDownwardSwapDecision(
        fromIndex: Int,
        draggedItem: ReorderVisibleItem,
        visibleItems: List<ReorderVisibleItem>,
    ): SwapDecision? {
        val nextItemKey = renderedItemsState.getOrNull(fromIndex + 1)?.key ?: return null
        val nextItem = visibleItems.firstOrNull { it.key == nextItemKey } ?: return null

        val draggedBottom = draggedItem.offset + draggedItem.size + draggedOffsetY
        val nextThreshold = nextItem.offset + (nextItem.size / 2f)
        if (draggedBottom < nextThreshold + SWAP_HYSTERESIS_PX) return null

        return SwapDecision(
            toIndex = fromIndex + 1,
            offsetCompensation = -nextItem.size.toFloat(),
        )
    }

    private fun findUpwardSwapDecision(
        fromIndex: Int,
        draggedItem: ReorderVisibleItem,
        visibleItems: List<ReorderVisibleItem>,
    ): SwapDecision? {
        val upwardSwap = calculateUpwardSwap(
            fromIndex = fromIndex,
            visibleItems = visibleItems,
        ) ?: return null
        val draggedTop = draggedItem.offset + draggedOffsetY
        if (draggedTop > upwardSwap.threshold - SWAP_HYSTERESIS_PX) return null

        return SwapDecision(
            toIndex = fromIndex - 1,
            offsetCompensation = upwardSwap.offsetCompensation,
        )
    }

    private data class UpwardSwapConfig(
        val threshold: Float,
        val offsetCompensation: Float,
    )

    private fun calculateUpwardSwap(
        fromIndex: Int,
        visibleItems: List<ReorderVisibleItem>,
    ): UpwardSwapConfig? {
        val previousItemKey = renderedItemsState.getOrNull(fromIndex - 1)?.key ?: return null
        val previousItem = visibleItems.firstOrNull { it.key == previousItemKey } ?: return null

        val maxPos = maxAllowedCutoffIndex()
        val crossingUpThroughFullCutoff = previousItemKey == NotificationListItem.Cutoff.key &&
            cutoffIndexState >= maxPos

        if (!crossingUpThroughFullCutoff) {
            return UpwardSwapConfig(
                threshold = previousItem.offset + (previousItem.size / 2f),
                offsetCompensation = previousItem.size.toFloat(),
            )
        }

        val lastAboveKey = renderedItemsState.getOrNull(fromIndex - 2)?.key ?: return null
        val lastAboveItem = visibleItems.firstOrNull { it.key == lastAboveKey } ?: return null
        return UpwardSwapConfig(
            threshold = lastAboveItem.offset + (lastAboveItem.size / 2f),
            offsetCompensation = (previousItem.size + lastAboveItem.size).toFloat(),
        )
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
        if (from == -1) return false

        val to = from + delta
        if (!moveRenderedItem(from = from, to = to)) return false

        notifyStateChanged()
        return true
    }

    fun canMove(itemKey: String, delta: Int): Boolean {
        val from = renderedItemsState.indexOfFirst { it.key == itemKey }
        if (from == -1) return false

        val to = from + delta
        return isMoveAllowed(from = from, to = to)
    }

    private fun isMoveAllowed(from: Int, to: Int): Boolean {
        if (from !in renderedItemsState.indices || to !in renderedItemsState.indices || from == to) return false

        val maxPos = maxAllowedCutoffIndex()

        // Don't allow the divider to be dragged to more than the max position
        return !(from == cutoffIndexState && to > maxPos)
    }

    private fun moveRenderedItem(from: Int, to: Int): Boolean {
        if (!isMoveAllowed(from = from, to = to)) return false

        val maxPos = maxAllowedCutoffIndex()

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

    private fun maxAllowedCutoffIndex(): Int {
        return NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN
            .coerceAtMost(renderedItemsState.lastIndex)
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
