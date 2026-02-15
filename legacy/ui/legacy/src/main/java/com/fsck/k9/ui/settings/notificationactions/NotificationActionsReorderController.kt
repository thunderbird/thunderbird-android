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
    private val renderedItemsState = mutableStateListOf<NotificationListItem>()

    var draggedKey by mutableStateOf<String?>(null)
        private set

    var draggedOffsetY by mutableFloatStateOf(0f)
        private set

    private var dragDidMove = false

    val items: List<NotificationListItem>
        get() = renderedItemsState

    val cutoffIndex: Int
        get() = renderedItemsState.indexOfFirst { it is NotificationListItem.Cutoff }

    init {
        setItems(initialItems)
    }

    fun setItems(initialItems: List<NotificationListItem>) {
        renderedItemsState.clear()
        renderedItemsState.addAll(normalizeInitialItems(initialItems))
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
                if (moveRenderedItem(from = fromIndex, to = nextItem.index)) {
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
                if (moveRenderedItem(from = fromIndex, to = previousItem.index)) {
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
            enforceInvariants()
            onItemsChanged(renderedItemsState.toList())
        }
    }

    fun moveByStep(itemKey: String, delta: Int): Boolean {
        val from = renderedItemsState.indexOfFirst { it.key == itemKey }
        if (from == -1) return false

        val to = from + delta
        if (!moveRenderedItem(from = from, to = to)) return false

        enforceInvariants()
        onItemsChanged(renderedItemsState.toList())
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

        renderedItemsState.add(to, renderedItemsState.removeAt(from))
        return true
    }

    private fun enforceInvariants() {
        // At most 3 actions can stay above the divider. If needed, move the divider up.
        val cutoff = renderedItemsState.indexOfFirst { it is NotificationListItem.Cutoff }
        if (cutoff == -1) return

        val maxCutoff = NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN.coerceAtMost(renderedItemsState.lastIndex)
        if (cutoff > maxCutoff) {
            val divider = renderedItemsState.removeAt(cutoff)
            renderedItemsState.add(maxCutoff, divider)
        }
    }

    private fun normalizeInitialItems(items: List<NotificationListItem>): List<NotificationListItem> {
        val actions = items.filterIsInstance<NotificationListItem.Action>()
        val cutoff = items.indexOfFirst { it is NotificationListItem.Cutoff }

        val clampedCutoff = when {
            cutoff < 0 -> NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN.coerceAtMost(actions.size)
            else -> cutoff.coerceAtMost(actions.size)
        }

        return buildList {
            actions.forEachIndexed { index, action ->
                if (index == clampedCutoff) add(NotificationListItem.Cutoff)
                add(action)
            }
            if (none { it is NotificationListItem.Cutoff }) {
                add(NotificationListItem.Cutoff)
            }
        }
    }
}
