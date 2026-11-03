package net.thunderbird.app.common.activity

import android.view.KeyEvent

/** Maps optional keypad navigation without taking characters away from text editors. */
internal class KeypadNavigationController {
    private val pendingActions = mutableMapOf<Key, KeypadNavigationAction>()

    fun dispatchKeyEvent(
        event: KeyEvent,
        enabled: Boolean,
        isTextInputFocused: Boolean,
        performAction: (KeypadNavigationAction) -> Unit,
    ): Boolean {
        val key = Key(event.deviceId, event.keyCode)
        return when (event.action) {
            KeyEvent.ACTION_UP -> onKeyUp(event, key, enabled, performAction)
            KeyEvent.ACTION_DOWN -> onKeyDown(event, key, enabled, isTextInputFocused)
            else -> false
        }
    }

    private fun onKeyUp(
        event: KeyEvent,
        key: Key,
        enabled: Boolean,
        performAction: (KeypadNavigationAction) -> Unit,
    ): Boolean {
        val action = pendingActions.remove(key) ?: return false
        if (enabled && !event.isCanceled && event.hasNoModifiers()) {
            performAction(action)
        }
        return true
    }

    private fun onKeyDown(event: KeyEvent, key: Key, enabled: Boolean, isTextInputFocused: Boolean): Boolean {
        if (key in pendingActions) return true
        val action = if (enabled && event.hasNoModifiers() && event.repeatCount == 0) {
            findAction(event.keyCode, isTextInputFocused)
        } else {
            null
        }
        return if (action != null) {
            pendingActions[key] = action
            true
        } else {
            false
        }
    }

    private fun findAction(keyCode: Int, isTextInputFocused: Boolean): KeypadNavigationAction? =
        when (keyCode) {
            KeyEvent.KEYCODE_SOFT_LEFT, KeyEvent.KEYCODE_F1 -> KeypadNavigationAction.OPEN_MENU
            KeyEvent.KEYCODE_SOFT_RIGHT, KeyEvent.KEYCODE_F2 -> KeypadNavigationAction.GO_BACK
            KeyEvent.KEYCODE_STAR -> if (isTextInputFocused) null else KeypadNavigationAction.PREVIOUS_CONTROL
            KeyEvent.KEYCODE_POUND -> if (isTextInputFocused) null else KeypadNavigationAction.NEXT_CONTROL
            else -> null
        }

    fun reset() {
        pendingActions.clear()
    }

    private data class Key(val deviceId: Int, val keyCode: Int)
}

internal enum class KeypadNavigationAction {
    OPEN_MENU,
    GO_BACK,
    PREVIOUS_CONTROL,
    NEXT_CONTROL,
}
