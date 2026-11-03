package net.thunderbird.app.common.activity

import android.app.Dialog
import android.view.FocusFinder
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager

/** Dispatches only to the dialog window, so shortcuts never activate the activity behind it. */
internal class KeypadDialogWindowCallback(
    private val window: Window,
    val delegate: Window.Callback,
    private val interactionPreferences: InteractionSettingsPreferenceManager,
) : Window.Callback by delegate {
    constructor(
        dialog: Dialog,
        delegate: Window.Callback,
        interactionPreferences: InteractionSettingsPreferenceManager,
    ) : this(requireNotNull(dialog.window), delegate, interactionPreferences)

    private val controller = KeypadNavigationController()

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handled = controller.dispatchKeyEvent(
            event = event,
            enabled = interactionPreferences.getConfig().isKeypadNavigationEnabled,
            isTextInputFocused = isTextInputFocused(),
        ) { action ->
            when (action) {
                KeypadNavigationAction.OPEN_MENU -> moveFocus(event, backwards = false)
                KeypadNavigationAction.GO_BACK -> navigateBack(event)
                KeypadNavigationAction.PREVIOUS_CONTROL -> moveFocus(event, backwards = true)
                KeypadNavigationAction.NEXT_CONTROL -> moveFocus(event, backwards = false)
            }
        }
        return handled || delegate.dispatchKeyEvent(event)
    }

    private fun isTextInputFocused(): Boolean {
        val focusedView = window.currentFocus ?: return false
        val inputMethodManager = window.context.getSystemService(InputMethodManager::class.java)
        return focusedView.onCheckIsTextEditor() || inputMethodManager.isAcceptingText
    }

    private fun navigateBack(event: KeyEvent) {
        val ime = WindowInsetsCompat.Type.ime()
        if (ViewCompat.getRootWindowInsets(window.decorView)?.isVisible(ime) == true) {
            WindowCompat.getInsetsController(window, window.decorView).hide(ime)
        } else {
            dispatchNavigationKey(event, KeyEvent.KEYCODE_BACK)
        }
    }

    private fun moveFocus(event: KeyEvent, backwards: Boolean) {
        val root = window.decorView as? ViewGroup ?: return
        val focusedView = window.currentFocus
        val direction = if (backwards) View.FOCUS_BACKWARD else View.FOCUS_FORWARD
        if (focusedView is EditText) {
            // F1 leaves native editors without inserting a tab into multiline fields.
            FocusFinder.getInstance().findNextFocus(root, focusedView, direction)?.requestFocus(direction)
            return
        }
        val handled = dispatchNavigationKey(event, KeyEvent.KEYCODE_TAB, shift = backwards)
        if (!handled) {
            FocusFinder.getInstance().findNextFocus(root, focusedView, direction)?.requestFocus(direction)
        }
    }

    private fun dispatchNavigationKey(event: KeyEvent, keyCode: Int, shift: Boolean = false): Boolean {
        val metaState = if (shift) KeyEvent.META_SHIFT_ON else 0
        var handled = false
        for (action in listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            handled = delegate.dispatchKeyEvent(
                KeyEvent(
                    event.downTime,
                    event.eventTime,
                    action,
                    keyCode,
                    0,
                    metaState,
                    event.deviceId,
                    0,
                    0,
                    event.source,
                ),
            ) || handled
        }
        return handled
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (!hasFocus) reset()
        delegate.onWindowFocusChanged(hasFocus)
    }

    fun reset() {
        controller.reset()
    }
}
