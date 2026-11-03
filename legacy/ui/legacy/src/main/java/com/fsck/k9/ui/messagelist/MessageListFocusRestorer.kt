package com.fsck.k9.ui.messagelist

import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.recyclerview.widget.RecyclerView
import app.k9mail.legacy.message.controller.MessageReference

/** Restores the exact reading target after the list becomes visible, without selecting a replacement message. */
internal class MessageListFocusRestorer(
    private val recyclerView: RecyclerView,
    private val findPosition: (MessageReference) -> Int?,
    private val canRestoreFocus: () -> Boolean,
) : AutoCloseable {
    private val adapter = requireNotNull(recyclerView.adapter)
    private val originalDescendantFocusability = recyclerView.descendantFocusability
    private val originalFocusableInTouchMode = recyclerView.isFocusableInTouchMode
    private var pending: MessageReference? = null
    private var restoredMessage: MessageReference? = null
    private var restoredItemId: Long? = null
    private val restore = Runnable { restorePendingFocus() }
    private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> retry() }
    private val windowFocusListener = ViewTreeObserver.OnWindowFocusChangeListener { focused ->
        if (focused) retry() else cancelPending()
    }
    private val adapterObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() = onMessagesChanged()
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) = onMessagesChanged()
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = onMessagesChanged()
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = onMessagesChanged()
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = onMessagesChanged()
    }

    val isPending: Boolean get() = pending != null

    init {
        recyclerView.addOnLayoutChangeListener(layoutListener)
        recyclerView.viewTreeObserver.addOnWindowFocusChangeListener(windowFocusListener)
        adapter.registerAdapterDataObserver(adapterObserver)
    }

    fun revealAndFocus(message: MessageReference) {
        pending = message
        restoredMessage = null
        restoredItemId = null
        holdContainerFocus()
        retry()
    }

    fun retry() {
        if (pending == null) return
        recyclerView.removeCallbacks(restore)
        recyclerView.post(restore)
    }

    fun cancel() {
        pending = null
        restoredMessage = null
        restoredItemId = null
        recyclerView.removeCallbacks(restore)
        restoreFocusBehavior()
    }

    fun cancelPending() {
        pending = null
        recyclerView.removeCallbacks(restore)
    }

    @Suppress("ReturnCount")
    private fun restorePendingFocus() {
        val message = pending ?: return
        if (!canRestoreFocus() || !recyclerView.isLaidOut) return
        val position = findPosition(message)
        if (position == null) {
            pending = null
            holdContainerFocus()
            return
        }
        val holder = recyclerView.findViewHolderForAdapterPosition(position)
        if (holder == null || recyclerView.hasPendingAdapterUpdates()) {
            recyclerView.scrollToPosition(position)
            return
        }
        val expectedId = adapter.getItemId(position)
        if (holder.itemId != expectedId || findPosition(message) != position) return
        restoreFocusBehavior()
        if (holder.itemView.requestFocusFromTouch()) {
            pending = null
            restoredMessage = message
            restoredItemId = expectedId
        } else {
            holdContainerFocus()
        }
    }

    private fun onMessagesChanged() {
        val restored = restoredMessage
        if (restored != null && findPosition(restored) == null) {
            val focusedId = recyclerView.focusedChild?.let(recyclerView::findContainingViewHolder)?.itemId
            if (focusedId == restoredItemId && canRestoreFocus()) holdContainerFocus()
            restoredMessage = null
            restoredItemId = null
        }
        retry()
    }

    private fun holdContainerFocus() {
        // RecyclerView otherwise recovers focus to the first row when the old focused row disappears.
        recyclerView.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        recyclerView.isFocusableInTouchMode = true
        recyclerView.requestFocus()
    }

    private fun restoreFocusBehavior() {
        recyclerView.descendantFocusability = originalDescendantFocusability
        recyclerView.isFocusableInTouchMode = originalFocusableInTouchMode
    }

    override fun close() {
        cancel()
        recyclerView.removeOnLayoutChangeListener(layoutListener)
        if (recyclerView.viewTreeObserver.isAlive) {
            recyclerView.viewTreeObserver.removeOnWindowFocusChangeListener(windowFocusListener)
        }
        adapter.unregisterAdapterDataObserver(adapterObserver)
    }
}
