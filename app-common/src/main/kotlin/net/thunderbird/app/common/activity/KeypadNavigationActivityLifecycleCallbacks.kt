package net.thunderbird.app.common.activity

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.FocusFinder
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager

/** Installs navigation after AppCompat has finished setting up each activity's window. */
internal class KeypadNavigationActivityLifecycleCallbacks(
    private val application: Application,
    private val interactionPreferences: InteractionSettingsPreferenceManager,
) : Application.ActivityLifecycleCallbacks {
    fun register() {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(activity: Activity) {
        val callback = activity.window.callback ?: return
        if (callback !is KeypadWindowCallback) {
            activity.window.callback = KeypadWindowCallback(activity, callback, interactionPreferences)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        (activity.window.callback as? KeypadWindowCallback)?.reset()
    }

    override fun onActivityDestroyed(activity: Activity) {
        val callback = activity.window.callback as? KeypadWindowCallback ?: return
        callback.reset()
        activity.window.callback = callback.delegate
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}

internal class KeypadWindowCallback(
    private val activity: Activity,
    val delegate: Window.Callback,
    private val interactionPreferences: InteractionSettingsPreferenceManager,
) : Window.Callback by delegate {
    private val controller = KeypadNavigationController()

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handled = controller.dispatchKeyEvent(
            event = event,
            enabled = interactionPreferences.getConfig().isKeypadNavigationEnabled,
            isTextInputFocused = isTextInputFocused(),
        ) { action ->
            when (action) {
                KeypadNavigationAction.OPEN_MENU -> activity.openOptionsMenu()
                KeypadNavigationAction.GO_BACK -> navigateBack(event)
                KeypadNavigationAction.PREVIOUS_CONTROL -> moveFocus(event, backwards = true)
                KeypadNavigationAction.NEXT_CONTROL -> moveFocus(event, backwards = false)
            }
        }
        return handled || delegate.dispatchKeyEvent(event)
    }

    private fun isTextInputFocused(): Boolean {
        val focusedView = activity.currentFocus ?: return false
        val inputMethodManager = activity.getSystemService(InputMethodManager::class.java)
        return focusedView.onCheckIsTextEditor() || inputMethodManager.isAcceptingText
    }

    private fun navigateBack(event: KeyEvent) {
        val ime = WindowInsetsCompat.Type.ime()
        if (ViewCompat.getRootWindowInsets(activity.window.decorView)?.isVisible(ime) == true) {
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).hide(ime)
            return
        }
        val owner = activity as? OnBackPressedDispatcherOwner
        if (owner != null) {
            owner.onBackPressedDispatcher.onBackPressed()
        } else {
            dispatchNavigationKey(event, KeyEvent.KEYCODE_BACK)
        }
    }

    private fun moveFocus(event: KeyEvent, backwards: Boolean) {
        val handled = dispatchNavigationKey(event, KeyEvent.KEYCODE_TAB, shift = backwards)
        if (!handled) {
            val direction = if (backwards) View.FOCUS_BACKWARD else View.FOCUS_FORWARD
            val focusedView = activity.currentFocus
            val root = activity.window.decorView as? ViewGroup ?: return
            val next = FocusFinder.getInstance().findNextFocus(root, focusedView, direction)
            next?.requestFocus(direction)
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
