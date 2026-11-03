package com.fsck.k9.view

import android.view.View
import android.view.ViewTreeObserver
import android.widget.ListView

/** Selects once after a popup window is ready, so initial layout cannot replace the user's first choice. */
internal class PopupListInitialSelection(
    private val list: ListView,
    private val position: Int,
    private val isActive: () -> Boolean,
) : AutoCloseable, View.OnAttachStateChangeListener {
    private var initialized = false
    private var closed = false
    private val initialize = Runnable { initializeIfReady() }
    private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        if (!initialized) list.post(initialize)
    }
    private val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
        if (hasFocus && !initialized) list.post(initialize)
    }

    init {
        list.addOnLayoutChangeListener(layoutListener)
        list.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        list.addOnAttachStateChangeListener(this)
        list.post(initialize)
    }

    private fun initializeIfReady() {
        if (closed || initialized) return
        val windowReady = list.isAttachedToWindow && list.isLaidOut && list.hasWindowFocus()
        if (!isActive() || !windowReady) return
        initialized = true
        list.requestFocusFromTouch()
        list.setSelection(position)
    }

    override fun onViewAttachedToWindow(view: View) {
        list.post(initialize)
    }

    override fun onViewDetachedFromWindow(view: View) {
        close()
    }

    override fun close() {
        if (closed) return
        closed = true
        list.removeCallbacks(initialize)
        list.removeOnLayoutChangeListener(layoutListener)
        list.removeOnAttachStateChangeListener(this)
        if (list.viewTreeObserver.isAlive) {
            list.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
        }
    }
}
