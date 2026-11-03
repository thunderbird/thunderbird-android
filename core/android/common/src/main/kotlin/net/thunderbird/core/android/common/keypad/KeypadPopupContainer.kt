package net.thunderbird.core.android.common.keypad

import android.content.Context
import android.view.FocusFinder
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout

/** Keeps optional keypad navigation inside a popup's own view hierarchy. */
class KeypadPopupContainer(
    context: Context,
    private val isNavigationEnabled: () -> Boolean,
    private val onDismissRequest: () -> Unit,
) : FrameLayout(context) {
    private val pendingKeys = mutableMapOf<Pair<Int, Int>, Int>()

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = when (event.action) {
        KeyEvent.ACTION_UP -> onKeyReleased(event) || super.dispatchKeyEvent(event)
        KeyEvent.ACTION_DOWN -> onKeyPressed(event) || super.dispatchKeyEvent(event)
        else -> super.dispatchKeyEvent(event)
    }

    private fun onKeyReleased(event: KeyEvent): Boolean {
        val key = event.deviceId to event.keyCode
        val navigationKey = pendingKeys.remove(key) ?: return false
        if (isNavigationEnabled() && !event.isCanceled && event.hasNoModifiers()) {
            if (navigationKey == KeyEvent.KEYCODE_BACK) {
                onDismissRequest()
            } else {
                moveFocus(event, backwards = navigationKey == KeyEvent.KEYCODE_STAR)
            }
        }
        return true
    }

    private fun onKeyPressed(event: KeyEvent): Boolean {
        val key = event.deviceId to event.keyCode
        if (key in pendingKeys) return true
        val navigationKey = if (isNavigationEnabled() && event.hasNoModifiers() && event.repeatCount == 0) {
            findNavigationKey(event.keyCode)
        } else {
            null
        }
        return if (navigationKey != null) {
            pendingKeys[key] = navigationKey
            true
        } else {
            false
        }
    }

    private fun findNavigationKey(keyCode: Int): Int? =
        when (keyCode) {
            KeyEvent.KEYCODE_F2, KeyEvent.KEYCODE_SOFT_RIGHT -> KeyEvent.KEYCODE_BACK
            KeyEvent.KEYCODE_STAR, KeyEvent.KEYCODE_POUND -> keyCode.takeUnless { isTextInputFocused() }
            else -> null
        }

    private fun isTextInputFocused(): Boolean {
        val focusedView = findFocus() ?: return false
        return focusedView.onCheckIsTextEditor() ||
            context.getSystemService(InputMethodManager::class.java).isAcceptingText
    }

    private fun moveFocus(event: KeyEvent, backwards: Boolean) {
        val metaState = if (backwards) KeyEvent.META_SHIFT_ON else 0
        var handled = false
        for (action in listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            handled = super.dispatchKeyEvent(
                KeyEvent(
                    event.downTime, event.eventTime, action, KeyEvent.KEYCODE_TAB, 0,
                    metaState, event.deviceId, 0, 0, event.source,
                ),
            ) || handled
        }
        if (!handled) {
            val direction = if (backwards) View.FOCUS_BACKWARD else View.FOCUS_FORWARD
            FocusFinder.getInstance().findNextFocus(this, findFocus(), direction)?.requestFocus(direction)
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        if (!hasWindowFocus) pendingKeys.clear()
        super.onWindowFocusChanged(hasWindowFocus)
    }

    override fun onDetachedFromWindow() {
        pendingKeys.clear()
        super.onDetachedFromWindow()
    }
}
