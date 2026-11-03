package com.fsck.k9.view

import android.view.KeyEvent
import android.widget.ListPopupWindow

/** Keeps alternate addresses navigable without sending confirmation keys back to the recipient editor. */
class RecipientAlternatesKeyboardNavigation(
    private val popup: ListPopupWindow,
    private val isEnabled: () -> Boolean,
) {
    private var pendingConfirmation: Key? = null
    private var pendingBack: Key? = null
    private val popupBackNavigation = PopupKeypadBackNavigation(isEnabled) { popup.dismiss() }
    private var initialSelection: AutoCloseable? = null

    fun prepareToShow() {
        initialSelection?.close()
        initialSelection = null
        pendingConfirmation = null
        pendingBack = null
        popupBackNavigation.reset()
        popup.isModal = isEnabled()
    }

    fun onPopupShown() {
        if (!isEnabled()) return
        popup.listView?.apply {
            setOnKeyListener(popupBackNavigation)
            val firstEnabled = (0 until count).firstOrNull { adapter.isEnabled(it) }
            if (firstEnabled != null) {
                initialSelection?.close()
                initialSelection = PopupListInitialSelection(this, firstEnabled) { popup.isShowing && isEnabled() }
            }
        }
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!isEnabled() || !popup.isShowing || !event.hasNoModifiers()) return false
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                popup.onKeyDown(keyCode, event)
                true
            }

            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                if (event.repeatCount == 0) {
                    pendingConfirmation = Key(event.deviceId, keyCode)
                    popup.onKeyDown(keyCode, event)
                }
                true
            }

            else -> false
        }
    }

    fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (pendingConfirmation != Key(event.deviceId, keyCode)) return false
        pendingConfirmation = null
        val popupActive = isEnabled() && popup.isShowing
        if (popupActive && !event.isCanceled && event.hasNoModifiers()) {
            popup.onKeyUp(keyCode, event)
        }
        return true
    }

    fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode != KeyEvent.KEYCODE_BACK) return false
        val key = Key(event.deviceId, keyCode)
        val popupActive = isEnabled() && popup.isShowing
        return when {
            event.action == KeyEvent.ACTION_UP && pendingBack == key -> {
                pendingBack = null
                if (isEnabled() && !event.isCanceled && event.hasNoModifiers()) popup.dismiss()
                true
            }

            event.action == KeyEvent.ACTION_DOWN && popupActive && event.hasNoModifiers() -> {
                if (event.repeatCount == 0) pendingBack = key
                true
            }

            else -> false
        }
    }

    private data class Key(val deviceId: Int, val keyCode: Int)
}
