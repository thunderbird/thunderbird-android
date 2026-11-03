package net.thunderbird.app.common.activity

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.Window
import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.thunderbird.core.preference.interaction.InteractionSettings
import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowViewRootImpl
import org.robolectric.util.ReflectionHelpers

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

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(androidx.appcompat.R.style.Theme_AppCompat)
        super.onCreate(savedInstanceState)
    }

    override fun openOptionsMenu() {
        menuOpenCount++
    }

    // ShadowActivity stores a separate currentFocus field; use the real view hierarchy for navigation tests.
    override fun getCurrentFocus(): View? = window.decorView.findFocus()
}

internal fun keypadEvent(action: Int, keyCode: Int) = KeyEvent(action, keyCode)

/** Finishes attachment and focuses the actual ViewRoot, including its key dispatcher state. */
internal fun focusKeypadDialogWindow(dialog: Dialog) {
    shadowOf(Looper.getMainLooper()).idle()
    val decor = requireNotNull(dialog.window).decorView
    val viewRoot = ReflectionHelpers.callInstanceMethod<Any>(decor, "getViewRootImpl")
    Shadow.extract<ShadowViewRootImpl>(viewRoot).callWindowFocusChanged(true)
    shadowOf(Looper.getMainLooper()).idle()
    assertThat(decor.hasWindowFocus()).isTrue()
    assertThat(decor.keyDispatcherState).isNotNull()
}
