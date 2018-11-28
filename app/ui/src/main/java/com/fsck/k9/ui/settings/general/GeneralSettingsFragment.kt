package com.fsck.k9.ui.settings.general

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v14.preference.MultiSelectListPreference
import android.support.v4.provider.DocumentFile
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceGroup
import com.fsck.k9.ui.R
import com.fsck.k9.notification.NotificationController
import com.fsck.k9.ui.settings.onClick
import com.fsck.k9.ui.settings.remove
import com.fsck.k9.ui.settings.removeEntry
import com.fsck.k9.ui.withArguments
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.File

class GeneralSettingsFragment : PreferenceFragmentCompat() {
    private val dataStore: GeneralSettingsDataStore by inject()

    private lateinit var attachmentDefaultPathPreference: Preference

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = dataStore

        setPreferencesFromResource(R.xml.general_settings, rootKey)

        initializeAttachmentDefaultPathPreference()
        initializeStartInUnifiedInbox()
        initializeConfirmActions()
        initializeLockScreenNotificationVisibility()
        initializeNotificationQuickDelete()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.title = preferenceScreen.title
        dataStore.activity = activity
    }

    private fun initializeAttachmentDefaultPathPreference() {
        //KITKAT does not support ACTION_OPEN_DOCUMENT_TREE, hide the category since attachmentDefaultPath is the only entry
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            findPreference(PREFERENCE_MISC_CATEGORY)?.apply {
                isVisible = false;
            }
        }

        findPreference(PREFERENCE_ATTACHMENT_DEFAULT_PATH)?.apply {
            attachmentDefaultPathPreference = this

            summary = attachmentDefaultPath()
            onClick {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    startActivityForResult(intent, REQUEST_PICK_DIRECTORY_URI_TREE)

                }
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

    private fun initializeConfirmActions() {
        val notificationActionsSupported = NotificationController.platformSupportsExtendedNotifications()
        if (!notificationActionsSupported) {
            (findPreference(PREFERENCE_CONFIRM_ACTIONS) as? MultiSelectListPreference)?.apply {
                removeEntry(CONFIRM_ACTION_DELETE_FROM_NOTIFICATION)
            }
        }
    }

    private fun initializeLockScreenNotificationVisibility() {
        val lockScreenNotificationsSupported = NotificationController.platformSupportsLockScreenNotifications()
        if (!lockScreenNotificationsSupported) {
            findPreference(PREFERENCE_LOCK_SCREEN_NOTIFICATION_VISIBILITY)?.apply { remove() }
        }
    }

    private fun initializeNotificationQuickDelete() {
        val notificationActionsSupported = NotificationController.platformSupportsExtendedNotifications()
        if (!notificationActionsSupported) {
            findPreference(PREFERENCE_NOTIFICATION_QUICK_DELETE)?.apply { remove() }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        if (resultCode != Activity.RESULT_OK || result == null  || getContext() == null) {
            return
        }

        when(requestCode) {
            REQUEST_PICK_DIRECTORY_PATH -> {
                result.data?.path?.let {
                    setAttachmentDefaultPath(it)
                }
            }

            REQUEST_PICK_DIRECTORY_URI_TREE -> {
                val uriTree = result.getData()
                val documentFile = DocumentFile.fromTreeUri(getContext(), uriTree)

                Timber.i("ACTIVITY_SAVE_ATTACHMENT_TREE uri " + uriTree.toString())
                setAttachmentDefaultPath(uriTree.toString())
            }
        }
    }

    override fun onActivityResult(group: PreferenceGroup?, requestCode: Int, resultCode: Int, data: Intent?) {



    }


    private fun attachmentDefaultPath() = dataStore.getString(PREFERENCE_ATTACHMENT_DEFAULT_PATH, "")

    private fun setAttachmentDefaultPath(path: String) {
        attachmentDefaultPathPreference.summary = path
        dataStore.putString(PREFERENCE_ATTACHMENT_DEFAULT_PATH, path)
    }

    private fun hideSpecialAccounts() = dataStore.getBoolean(PREFERENCE_HIDE_SPECIAL_ACCOUNTS, false)


    companion object {
        private const val REQUEST_PICK_DIRECTORY_PATH = 1
        private const val REQUEST_PICK_DIRECTORY_URI_TREE = 2

        private const val PREFERENCE_MISC_CATEGORY = "misc_preferences"
        private const val PREFERENCE_ATTACHMENT_DEFAULT_PATH = "attachment_default_path"
        private const val PREFERENCE_START_IN_UNIFIED_INBOX = "start_integrated_inbox"
        private const val PREFERENCE_HIDE_SPECIAL_ACCOUNTS = "hide_special_accounts"
        private const val PREFERENCE_CONFIRM_ACTIONS = "confirm_actions"
        private const val PREFERENCE_LOCK_SCREEN_NOTIFICATION_VISIBILITY = "lock_screen_notification_visibility"
        private const val PREFERENCE_NOTIFICATION_QUICK_DELETE = "notification_quick_delete"
        private const val CONFIRM_ACTION_DELETE_FROM_NOTIFICATION = "delete_notif"




        fun create(rootKey: String? = null) = GeneralSettingsFragment().withArguments(
                PreferenceFragmentCompat.ARG_PREFERENCE_ROOT to rootKey)
    }
}
