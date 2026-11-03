package net.thunderbird.app.common.activity

import android.app.Activity
import android.view.KeyEvent
import android.view.View
import android.view.Window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.thunderbird.core.preference.interaction.InteractionSettings
import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager

internal class FakeKeypadInteractionPreferences(enabled: Boolean = true) : InteractionSettingsPreferenceManager {
    private val config = MutableStateFlow(InteractionSettings(isKeypadNavigationEnabled = enabled))

    override fun save(config: InteractionSettings) {
        this.config.value = config
    }

    override fun getConfig() = config.value

    override fun getConfigFlow() = config.asStateFlow()
}

internal class RecordingKeypadWindowCallback(activity: Activity) : Window.Callback by activity {
    val events = mutableListOf<KeyEvent>()
    val windowFocusChanges = mutableListOf<Boolean>()
    var handlesKeyEvents = false

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        events.add(event)
        return handlesKeyEvents
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        windowFocusChanges.add(hasFocus)
    }
}

class RecordingKeypadActivity : Activity() {
    var menuOpenCount = 0
        private set

    override fun openOptionsMenu() {
        menuOpenCount++
    }

    // ShadowActivity stores a separate currentFocus field; use the real view hierarchy for navigation tests.
    override fun getCurrentFocus(): View? = window.decorView.findFocus()
}

internal fun keypadEvent(action: Int, keyCode: Int) = KeyEvent(action, keyCode)
