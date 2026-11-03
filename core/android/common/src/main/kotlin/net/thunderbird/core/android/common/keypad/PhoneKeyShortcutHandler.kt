package net.thunderbird.core.android.common.keypad

import android.view.KeyEvent

/** Pairs phone-key presses while leaving all telephone handling to Android whenever the phone is not idle. */
class PhoneKeyShortcutHandler<Target, Action>(
    private val isPhoneIdle: () -> Boolean,
    private val getShortcut: (keyCode: Int) -> Action?,
    private val getTarget: () -> Target?,
    private val performAction: (Action, Target) -> Unit,
) {
    private val pendingShortcuts = mutableMapOf<Key, PendingShortcut<Target, Action>>()

    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_CALL && event.keyCode != KeyEvent.KEYCODE_ENDCALL) return false

        val key = Key(event.deviceId, event.keyCode)
        return if (!isPhoneIdle()) {
            pendingShortcuts.clear()
            false
        } else {
            when (event.action) {
                KeyEvent.ACTION_UP -> onKeyUp(event, key)
                KeyEvent.ACTION_DOWN -> onKeyDown(event, key)
                else -> false
            }
        }
    }

    private fun onKeyUp(event: KeyEvent, key: Key): Boolean {
        val pending = pendingShortcuts.remove(key) ?: return false
        val isValidRelease = !event.isCanceled && event.hasNoModifiers()
        if (isValidRelease && getShortcut(event.keyCode) == pending.action && getTarget() == pending.target) {
            performAction(pending.action, pending.target)
        }
        return true
    }

    private fun onKeyDown(event: KeyEvent, key: Key): Boolean {
        if (key in pendingShortcuts) return true
        return if (event.hasNoModifiers() && event.repeatCount == 0) {
            startShortcut(event.keyCode, key)
        } else {
            false
        }
    }

    private fun startShortcut(keyCode: Int, key: Key): Boolean {
        val action = getShortcut(keyCode)
        val target = getTarget()
        return if (action != null && target != null) {
            pendingShortcuts[key] = PendingShortcut(target, action)
            true
        } else {
            false
        }
    }

    fun reset() {
        pendingShortcuts.clear()
    }

    private data class Key(val deviceId: Int, val keyCode: Int)
    private data class PendingShortcut<Target, Action>(val target: Target, val action: Action)
}
