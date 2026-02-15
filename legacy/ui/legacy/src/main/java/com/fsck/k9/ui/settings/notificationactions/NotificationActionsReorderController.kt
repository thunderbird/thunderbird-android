package com.fsck.k9.ui.settings.notificationactions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.thunderbird.core.preference.notification.NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN

internal data class ReorderVisibleItem(
    val key: String,
    val index: Int,
    val offset: Int,
    val size: Int,
)

internal class NotificationActionsReorderController(
    initialItems: List<NotificationListItem>,
    private val onItemsChanged: (List<NotificationListItem>) -> Unit,
) {
    private val itemsState = mutableStateListOf<NotificationListItem>()

    var draggedKey by mutableStateOf<String?>(null)
        private set

    var draggedOffsetY by mutableFloatStateOf(0f)
        private set

    private var dragDidMove = false

    val items: List<NotificationListItem>
        get() = itemsState

    val cutoffIndex: Int
        get() = itemsState.indexOfFirst { it is NotificationListItem.Cutoff }

    init {
        setItems(initialItems)
    }

    fun setItems(initialItems: List<NotificationListItem>) {
        itemsState.clear()
        itemsState.addAll(normalizeItems(initialItems))
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
        val fromIndex = draggedItem.index

        if (deltaY > 0f) {
            val nextItem = visibleItems
                .filter { it.index > fromIndex }
                .minByOrNull { it.index } ?: return

            val draggedBottom = draggedItem.offset + draggedItem.size + draggedOffsetY
            val nextThreshold = nextItem.offset + (nextItem.size / 2f)
            if (draggedBottom >= nextThreshold) {
                val moved = moveItem(from = fromIndex, to = nextItem.index)
                if (moved) {
                    dragDidMove = true
                    draggedOffsetY -= nextItem.size.toFloat()
                }
            }
        } else if (deltaY < 0f) {
            val previousItem = visibleItems
                .filter { it.index < fromIndex }
                .maxByOrNull { it.index } ?: return

            val draggedTop = draggedItem.offset + draggedOffsetY
            val previousThreshold = previousItem.offset + (previousItem.size / 2f)
            if (draggedTop <= previousThreshold) {
                val moved = moveItem(from = fromIndex, to = previousItem.index)
                if (moved) {
                    dragDidMove = true
                    draggedOffsetY += previousItem.size.toFloat()
                }
            }
        }
    }

    fun endDrag() {
        draggedOffsetY = 0f
        draggedKey = null

        if (dragDidMove) {
            dragDidMove = false
            onItemsChanged(itemsState.toList())
        }
    }

    fun moveByStep(itemKey: String, delta: Int): Boolean {
        val from = itemsState.indexOfFirst { it.key == itemKey }
        if (from == -1) return false

        val moved = moveItem(from = from, to = from + delta)
        if (moved) {
            onItemsChanged(itemsState.toList())
        }

        return moved
    }

    fun canMove(itemKey: String, delta: Int): Boolean {
        val from = itemsState.indexOfFirst { it.key == itemKey }
        if (from == -1) return false

        return canMove(from = from, delta = delta)
    }

    private fun moveItem(from: Int, to: Int): Boolean {
        val lastIndex = itemsState.lastIndex
        if (from !in 0..lastIndex || to !in 0..lastIndex) return false

        val item = itemsState[from]
        val oldCutoff = itemsState.indexOfFirst { it is NotificationListItem.Cutoff }
            .coerceIn(0, NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN)

        val boundedTarget = if (item is NotificationListItem.Cutoff) {
            to.coerceIn(0, NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN.coerceAtMost(lastIndex))
        } else {
            val targetItem = itemsState.getOrNull(to)
            when {
                targetItem is NotificationListItem.Cutoff && to < from -> (to - 1).coerceAtLeast(0)
                targetItem is NotificationListItem.Cutoff && to > from -> (to + 1).coerceAtMost(lastIndex)
                else -> to
            }
        }

        if (boundedTarget == from) return false

        val normalized = if (item is NotificationListItem.Cutoff) {
            val actions = itemsState.filterIsInstance<NotificationListItem.Action>().map { it.action }
            normalizeItems(actions = actions, cutoff = boundedTarget)
        } else {
            val mutable = itemsState.toMutableList()
            mutable.add(boundedTarget, mutable.removeAt(from))
            val reorderedActions = mutable.filterIsInstance<NotificationListItem.Action>().map { it.action }
            normalizeItems(actions = reorderedActions, cutoff = oldCutoff)
        }

        itemsState.clear()
        itemsState.addAll(normalized)
        return true
    }

    private fun canMove(from: Int, delta: Int): Boolean {
        val lastIndex = itemsState.lastIndex
        val to = from + delta
        if (from !in 0..lastIndex || to !in 0..lastIndex) return false

        val item = itemsState[from]
        val boundedTarget = if (item is NotificationListItem.Cutoff) {
            to.coerceIn(0, NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN.coerceAtMost(lastIndex))
        } else {
            val targetItem = itemsState.getOrNull(to)
            when {
                targetItem is NotificationListItem.Cutoff && to < from -> (to - 1).coerceAtLeast(0)
                targetItem is NotificationListItem.Cutoff && to > from -> (to + 1).coerceAtMost(lastIndex)
                else -> to
            }
        }

        return boundedTarget != from
    }

    private fun normalizeItems(items: List<NotificationListItem>): List<NotificationListItem> {
        val actions = items.filterIsInstance<NotificationListItem.Action>().map { it.action }
        val cutoff = items.indexOfFirst { it is NotificationListItem.Cutoff }
            .coerceIn(0, NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN)
            .coerceAtMost(actions.size)

        return normalizeItems(actions = actions, cutoff = cutoff)
    }

    private fun normalizeItems(
        actions: List<MessageNotificationAction>,
        cutoff: Int,
    ): List<NotificationListItem> {
        val clampedCutoff = cutoff
            .coerceIn(0, NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN)
            .coerceAtMost(actions.size)

        return buildList {
            actions.forEachIndexed { index, action ->
                if (index == clampedCutoff) add(NotificationListItem.Cutoff)
                add(NotificationListItem.Action(action = action))
            }
            if (clampedCutoff == actions.size) add(NotificationListItem.Cutoff)
        }
    }
}
