package net.thunderbird.core.android.common.keypad

import android.view.KeyEvent

/** Runs a context's optional shortcuts once on release, without consuming text or modified keys. */
class KeypadShortcutHandler<Action>(private val actions: Map<Int, Action>) {
    private val pending = mutableMapOf<Key, Action>()

    fun dispatchKeyEvent(event: KeyEvent, enabled: Boolean, performAction: (Action) -> Unit): Boolean {
        val key = Key(event.deviceId, event.keyCode)
        return when (event.action) {
            KeyEvent.ACTION_UP -> release(event, key, enabled, performAction)
            KeyEvent.ACTION_DOWN -> press(event, key, enabled)
            else -> false
        }
    }

    private fun press(event: KeyEvent, key: Key, enabled: Boolean): Boolean {
        if (key in pending) return true
        val canStart = enabled && event.hasNoModifiers() && event.repeatCount == 0
        val action = if (canStart && !event.isCanceled) actions[event.keyCode] else null
        return if (action != null) {
            pending[key] = action
            true
        } else {
            false
        }
    }

    private fun release(event: KeyEvent, key: Key, enabled: Boolean, performAction: (Action) -> Unit): Boolean {
        val action = pending.remove(key) ?: return false
        if (enabled && event.hasNoModifiers() && !event.isCanceled) performAction(action)
        return true
    }

    fun reset() {
        pending.clear()
    }

    private data class Key(val deviceId: Int, val keyCode: Int)
}
