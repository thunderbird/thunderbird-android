package com.fsck.k9.view

import android.view.KeyEvent
import android.view.View

/** A local soft-right mapping for popup content that does not pass through an activity window. */
class PopupKeypadBackNavigation(
    private val isEnabled: () -> Boolean,
    private val dismiss: () -> Unit,
) : View.OnKeyListener {
    private var pendingKey: Key? = null

    override fun onKey(view: View, keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode != KeyEvent.KEYCODE_F2 && keyCode != KeyEvent.KEYCODE_SOFT_RIGHT) return false
        val key = Key(event.deviceId, keyCode)
        return when {
            event.action == KeyEvent.ACTION_UP && pendingKey == key -> {
                pendingKey = null
                if (isEnabled() && !event.isCanceled && event.hasNoModifiers()) dismiss()
                true
            }

            event.action == KeyEvent.ACTION_DOWN && isEnabled() && event.hasNoModifiers() -> {
                if (event.repeatCount == 0) pendingKey = key
                true
            }

            else -> false
        }
    }

    fun reset() {
        pendingKey = null
    }

    private data class Key(val deviceId: Int, val keyCode: Int)
}
