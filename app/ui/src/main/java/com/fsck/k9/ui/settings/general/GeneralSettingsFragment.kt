package com.fsck.k9.ui.settings.general

import android.os.Build
import android.os.Bundle
import androidx.preference.ListPreference
import com.fsck.k9.ui.R
import com.fsck.k9.ui.withArguments
import com.takisoft.preferencex.PreferenceFragmentCompat
import org.koin.android.ext.android.inject

class GeneralSettingsFragment : PreferenceFragmentCompat() {
    private val dataStore: GeneralSettingsDataStore by inject()

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = dataStore

        setPreferencesFromResource(R.xml.general_settings, rootKey)

        initializeTheme()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.title = preferenceScreen.title
        dataStore.activity = activity
    }

    private fun initializeTheme() {
        (findPreference(PREFERENCE_THEME) as? ListPreference)?.apply {
            if (Build.VERSION.SDK_INT < 28) {
                setEntries(R.array.theme_entries_legacy)
                setEntryValues(R.array.theme_values_legacy)
            }
        }
    }

    companion object {
        private const val PREFERENCE_THEME = "theme"

        fun create(rootKey: String? = null) = GeneralSettingsFragment().withArguments(ARG_PREFERENCE_ROOT to rootKey)
    }
}
