package net.thunderbird.app.common.activity

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager

/** Gives fragment dialogs the same optional keypad navigation as their activity. */
internal class KeypadDialogFragmentLifecycleCallbacks(
    private val interactionPreferences: InteractionSettingsPreferenceManager,
) : FragmentManager.FragmentLifecycleCallbacks() {
    private val installedCallbacks = mutableMapOf<DialogFragment, KeypadDialogWindowCallback>()

    override fun onFragmentStarted(fragmentManager: FragmentManager, fragment: Fragment) {
        val dialogFragment = fragment as? DialogFragment ?: return
        val window = dialogFragment.dialog?.window
        val delegate = window?.callback
        if (window != null && delegate != null && delegate !is KeypadDialogWindowCallback) {
            val callback = KeypadDialogWindowCallback(window, delegate, interactionPreferences)
            installedCallbacks[dialogFragment] = callback
            window.callback = callback
        }
    }

    override fun onFragmentPaused(fragmentManager: FragmentManager, fragment: Fragment) {
        installedCallbacks[fragment as? DialogFragment ?: return]?.reset()
    }

    override fun onFragmentStopped(fragmentManager: FragmentManager, fragment: Fragment) {
        uninstall(fragment)
    }

    override fun onFragmentDestroyed(fragmentManager: FragmentManager, fragment: Fragment) {
        uninstall(fragment)
    }

    fun clear() {
        installedCallbacks.keys.toList().forEach(::uninstall)
    }

    private fun uninstall(fragment: Fragment) {
        val dialogFragment = fragment as? DialogFragment ?: return
        val callback = installedCallbacks.remove(dialogFragment) ?: return
        callback.reset()
        val window = dialogFragment.dialog?.window
        if (window != null && window.callback === callback) window.callback = callback.delegate
    }
}
