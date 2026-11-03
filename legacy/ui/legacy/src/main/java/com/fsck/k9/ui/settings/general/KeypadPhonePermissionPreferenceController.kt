package com.fsck.k9.ui.settings.general

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.fsck.k9.ui.R
import com.takisoft.preferencex.PreferenceFragmentCompat
import net.thunderbird.core.preference.interaction.KeypadMessageAction

/** Registers during fragment construction so permission results survive settings view recreation. */
internal class KeypadPhonePermissionPreferenceController(private val fragment: PreferenceFragmentCompat) {
    private val permissionLauncher = fragment.registerForActivityResult(RequestPermission()) {
        updatePermissionState()
    }

    fun bindPreferences() {
        val actionChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            val action = KeypadMessageAction.entries.firstOrNull { it.name == newValue }
                ?: return@OnPreferenceChangeListener false
            val previousAction = (preference as ListPreference).value
            if (previousAction == KeypadMessageAction.NONE.name && action != KeypadMessageAction.NONE) {
                requestPermission()
            }
            true
        }
        fragment.findPreference<Preference>("keypad_call_action")?.onPreferenceChangeListener = actionChangeListener
        fragment.findPreference<Preference>("keypad_end_call_action")?.onPreferenceChangeListener = actionChangeListener
        fragment.findPreference<Preference>("keypad_phone_permission")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                requestPermission()
                true
            }
        updatePermissionState()
    }

    fun updatePermissionState() {
        fragment.findPreference<Preference>("keypad_phone_permission")?.apply {
            val granted = hasPermission()
            isSelectable = !granted
            setSummary(
                if (granted) R.string.keypad_phone_permission_granted else R.string.keypad_phone_permission_summary,
            )
        }
    }

    private fun requestPermission() {
        if (!hasPermission()) {
            permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED
}
