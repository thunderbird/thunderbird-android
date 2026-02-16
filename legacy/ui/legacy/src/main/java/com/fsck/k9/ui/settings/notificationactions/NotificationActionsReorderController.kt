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
    val index: Int,
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
        val currentActions = renderedItemsState
            .filterIsInstance<NotificationListItem.Action>()
            .map { it.action }
        val currentCutoff = cutoffIndexState

        val normalizedIncomingCutoff = initialCutoff.coerceIn(0, initialActions.size)
        if (currentActions == initialActions && currentCutoff == normalizedIncomingCutoff) {
            return
        }

        renderedItemsState.clear()
        renderedItemsState.addAll(buildRenderedItems(actions = initialActions, cutoff = initialCutoff))
        updateCutoffIndexFromRendered()
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

        if (deltaY > 0f) {
            val nextItemKey = renderedItemsState.getOrNull(fromIndex + 1)?.key ?: return
            val nextItem = visibleItems.firstOrNull { it.key == nextItemKey } ?: return

            val draggedBottom = draggedItem.offset + draggedItem.size + draggedOffsetY
            val nextThreshold = nextItem.offset + (nextItem.size / 2f)
            if (draggedBottom >= nextThreshold + SWAP_HYSTERESIS_PX) {
                if (moveRenderedItem(from = fromIndex, to = fromIndex + 1)) {
                    dragDidMove = true
                    draggedOffsetY -= nextItem.size.toFloat()
                }
            }
        } else if (deltaY < 0f) {
            val previousItemKey = renderedItemsState.getOrNull(fromIndex - 1)?.key ?: return
            val previousItem = visibleItems.firstOrNull { it.key == previousItemKey } ?: return

            val draggedTop = draggedItem.offset + draggedOffsetY
            val previousThreshold = previousItem.offset + (previousItem.size / 2f)
            if (draggedTop <= previousThreshold - SWAP_HYSTERESIS_PX) {
                if (moveRenderedItem(from = fromIndex, to = fromIndex - 1)) {
                    dragDidMove = true
                    draggedOffsetY += previousItem.size.toFloat()
                }
            }
        }
    }

    fun endDrag() {
        val shouldNotify = dragDidMove

        draggedOffsetY = 0f
        draggedKey = null
        dragDidMove = false

        if (!shouldNotify) return

        clampDividerToMaxPosition()
        notifyStateChanged()
    }

    fun moveByStep(itemKey: String, delta: Int): Boolean {
        val from = renderedItemsState.indexOfFirst { it.key == itemKey }
        if (from == -1) return false

        val to = from + delta
        if (!moveRenderedItem(from = from, to = to)) return false

        clampDividerToMaxPosition()
        notifyStateChanged()
        return true
    }

    fun canMove(itemKey: String, delta: Int): Boolean {
        val from = renderedItemsState.indexOfFirst { it.key == itemKey }
        if (from == -1) return false

        val to = from + delta
        return from in renderedItemsState.indices && to in renderedItemsState.indices && from != to
    }

    private fun moveRenderedItem(from: Int, to: Int): Boolean {
        if (from !in renderedItemsState.indices || to !in renderedItemsState.indices || from == to) return false

        // Don't allow the divider to be dragged to more than the max position
        if (from == cutoffIndexState && to > NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN) return false

        renderedItemsState.add(to, renderedItemsState.removeAt(from))
        updateCutoffIndexFromRendered()
        return true
    }

    private fun clampDividerToMaxPosition() {
        val maxCutoffIndex = NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN
            .coerceAtMost(renderedItemsState.lastIndex)

        if (cutoffIndexState <= maxCutoffIndex) return

        moveRenderedItem(cutoffIndexState, maxCutoffIndex)
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

    private fun updateCutoffIndexFromRendered() {
        cutoffIndexState = renderedItemsState.indexOfFirst { it is NotificationListItem.Cutoff }.coerceAtLeast(0)
    }

    private fun notifyStateChanged() {
        val actions = renderedItemsState
            .filterIsInstance<NotificationListItem.Action>()
            .map { it.action }
        onStateChanged(actions, cutoffIndexState)
    }
}
