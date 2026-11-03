package net.thunderbird.app.common.activity

import android.view.Window
import net.thunderbird.core.android.common.keypad.KeypadWindowInstaller
import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager

internal class DefaultKeypadWindowInstaller(
    private val interactionPreferences: InteractionSettingsPreferenceManager,
) : KeypadWindowInstaller {
    override fun install(window: Window): AutoCloseable {
        val delegate = window.callback
        if (delegate == null || delegate is KeypadDialogWindowCallback) return AutoCloseable {}

        val callback = KeypadDialogWindowCallback(window, delegate, interactionPreferences)
        window.callback = callback
        return AutoCloseable {
            callback.reset()
            if (window.callback === callback) window.callback = delegate
        }
    }
}
