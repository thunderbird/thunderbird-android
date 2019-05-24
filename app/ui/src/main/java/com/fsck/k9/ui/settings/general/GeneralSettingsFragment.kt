package com.fsck.k9.ui.settings.general

import android.os.Build
import android.os.Bundle
import androidx.preference.ListPreference
import com.fsck.k9.ui.R
import com.fsck.k9.notification.NotificationController
import com.fsck.k9.ui.settings.remove
import com.fsck.k9.ui.withArguments
import com.takisoft.preferencex.PreferenceFragmentCompat
import org.koin.android.ext.android.inject

class GeneralSettingsFragment : PreferenceFragmentCompat() {
    private val dataStore: GeneralSettingsDataStore by inject()


    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = dataStore

        setPreferencesFromResource(R.xml.general_settings, rootKey)

        initializeTheme()
        initializeStartInUnifiedInbox()
        initializeLockScreenNotificationVisibility()
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

    private fun initializeStartInUnifiedInbox() {
        findPreference(PREFERENCE_START_IN_UNIFIED_INBOX)?.apply {
            if (hideSpecialAccounts()) {
                isEnabled = false
            }
        }
    }

    private fun initializeLockScreenNotificationVisibility() {
        val lockScreenNotificationsSupported = NotificationController.platformSupportsLockScreenNotifications()
        if (!lockScreenNotificationsSupported) {
            findPreference(PREFERENCE_LOCK_SCREEN_NOTIFICATION_VISIBILITY)?.apply { remove() }
        }
    }

    private fun hideSpecialAccounts() = dataStore.getBoolean(PREFERENCE_HIDE_SPECIAL_ACCOUNTS, false)


    companion object {
        private const val PREFERENCE_THEME = "theme"
        private const val PREFERENCE_START_IN_UNIFIED_INBOX = "start_integrated_inbox"
        private const val PREFERENCE_HIDE_SPECIAL_ACCOUNTS = "hide_special_accounts"
        private const val PREFERENCE_LOCK_SCREEN_NOTIFICATION_VISIBILITY = "lock_screen_notification_visibility"

        fun create(rootKey: String? = null) = GeneralSettingsFragment().withArguments(ARG_PREFERENCE_ROOT to rootKey)
    }
}
