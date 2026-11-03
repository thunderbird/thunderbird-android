package net.thunderbird.app.common.activity

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.FocusFinder
import android.view.KeyEvent
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.fragment.app.FragmentActivity
import app.k9mail.feature.launcher.FeatureLauncherActivity
import com.fsck.k9.activity.MessageHomeActivity
import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager
import com.fsck.k9.ui.R as LegacyUiR

/** Installs navigation after AppCompat has finished setting up each activity's window. */
internal class KeypadNavigationActivityLifecycleCallbacks(
    private val application: Application,
    private val interactionPreferences: InteractionSettingsPreferenceManager,
) : Application.ActivityLifecycleCallbacks {
    private val dialogCallbacks = mutableMapOf<FragmentActivity, KeypadDialogFragmentLifecycleCallbacks>()

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
        if (activity is FragmentActivity) {
            dialogCallbacks.remove(activity)?.let { callbacks ->
                activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(callbacks)
                callbacks.clear()
            }
        }
        val callback = activity.window.callback as? KeypadWindowCallback ?: return
        callback.reset()
        callback.closeMenu()
        activity.window.callback = callback.delegate
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is FragmentActivity && activity !in dialogCallbacks) {
            val callbacks = KeypadDialogFragmentLifecycleCallbacks(interactionPreferences)
            dialogCallbacks[activity] = callbacks
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(callbacks, true)
        }
    }
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
    private var menuNavigator: KeypadMenuNavigator? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            (activity as? MessageHomeActivity)?.onKeypadReaderUserInteraction()
        }
        val keypadEnabled = interactionPreferences.getConfig().isKeypadNavigationEnabled
        val isSetupMenu = activity is FeatureLauncherActivity &&
            event.keyCode in listOf(KeyEvent.KEYCODE_F1, KeyEvent.KEYCODE_SOFT_LEFT)
        val handled = controller.dispatchKeyEvent(
            event = event,
            enabled = keypadEnabled || isSetupMenu,
            isTextInputFocused = isTextInputFocused(),
        ) { action ->
            when (action) {
                KeypadNavigationAction.OPEN_MENU -> openKeypadOptionsMenu(event)
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

    private fun openKeypadOptionsMenu(event: KeyEvent) {
        val fallback = KeypadFallbackMenu(activity)
        val keypadEnabled = interactionPreferences.getConfig().isKeypadNavigationEnabled
        val contextMenu = (activity as? MessageHomeActivity)?.keypadContextMenu
        val toolbarMenu = findToolbarMenu(activity.window.decorView)?.takeIf {
            keypadEnabled && contextMenu == null
        }?.takeIf {
            delegate.onPreparePanel(Window.FEATURE_OPTIONS_PANEL, null, it) && it.hasVisibleItems()
        }
        val menu = if (!keypadEnabled) {
            fallback.activation {
                interactionPreferences.save(interactionPreferences.getConfig().copy(isKeypadNavigationEnabled = true))
            }
        } else {
            contextMenu ?: toolbarMenu ?: fallback.navigation(
                next = { activity.window.decorView.post { moveFocus(event, backwards = false) } },
                previous = { activity.window.decorView.post { moveFocus(event, backwards = true) } },
                back = { activity.window.decorView.post { navigateBack(event) } },
            )
        }
        val navigator = menuNavigator ?: KeypadMenuNavigator(
            activity,
            DefaultKeypadWindowInstaller(interactionPreferences),
        ).also { menuNavigator = it }
        val fieldActions = listOf(
            LegacyUiR.id.keypad_compose_next_field,
            LegacyUiR.id.keypad_compose_previous_field,
            LegacyUiR.id.keypad_compose_body,
            LegacyUiR.id.keypad_compose_attachments,
        )
        if (!navigator.showMenu(menu, preferredItemIds = fieldActions)) activity.openOptionsMenu()
    }

    private fun findToolbarMenu(view: View): Menu? = when {
        view is Toolbar && view.isShown -> view.menu
        view is ViewGroup -> view.children.firstNotNullOfOrNull { findToolbarMenu(it) }
        else -> null
    }

    fun closeMenu() {
        menuNavigator?.close()
        menuNavigator = null
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
        if (activity.currentFocus is EditText) {
            moveViewFocus(backwards)
            return
        }
        val handled = dispatchNavigationKey(event, KeyEvent.KEYCODE_TAB, shift = backwards)
        if (!handled) moveViewFocus(backwards)
    }

    private fun moveViewFocus(backwards: Boolean) {
        val direction = if (backwards) View.FOCUS_BACKWARD else View.FOCUS_FORWARD
        val root = activity.window.decorView as? ViewGroup ?: return
        FocusFinder.getInstance().findNextFocus(root, activity.currentFocus, direction)?.requestFocus(direction)
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
