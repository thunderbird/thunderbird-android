package com.fsck.k9.widget.unread

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.activity.ChooseAccount
import com.fsck.k9.activity.K9PreferenceActivity
import com.fsck.k9.search.SearchAccount
import com.fsck.k9.ui.choosefolder.ChooseFolderActivity
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Activity to select an account for the unread widget.
 */
class UnreadWidgetConfigurationActivity : K9PreferenceActivity() {
    private val repository: UnreadWidgetRepository by inject()
    private val unreadWidgetUpdater: UnreadWidgetUpdater by inject()

    /**
     * The ID of the widget we are configuring.
     */
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private lateinit var unreadAccount: Preference
    private lateinit var unreadFolderEnabled: CheckBoxPreference
    private lateinit var unreadFolder: Preference

    private var selectedAccountUuid: String? = null
    private var selectedFolder: String? = null

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        // Find the widget ID from the intent.
        val extras = this.intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        // If they gave us an intent without the widget ID, just bail.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Timber.e("Received an invalid widget ID")
            finish()
            return
        }

        addPreferencesFromResource(R.xml.unread_widget_configuration)
        unreadAccount = findPreference(PREFERENCE_UNREAD_ACCOUNT)
        unreadAccount.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(this@UnreadWidgetConfigurationActivity, ChooseAccount::class.java)
            startActivityForResult(intent, REQUEST_CHOOSE_ACCOUNT)
            false
        }

        unreadFolderEnabled = findPreference(PREFERENCE_UNREAD_FOLDER_ENABLED) as CheckBoxPreference
        unreadFolderEnabled.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
            unreadFolder.summary = getString(R.string.unread_widget_folder_summary)
            selectedFolder = null
            true
        }

        unreadFolder = findPreference(PREFERENCE_UNREAD_FOLDER)
        unreadFolder.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = ChooseFolderActivity.buildLaunchIntent(
                context = this@UnreadWidgetConfigurationActivity,
                accountUuid = selectedAccountUuid!!,
                showDisplayableOnly = true
            )
            startActivityForResult(intent, REQUEST_CHOOSE_FOLDER)
            false
        }
        setTitle(R.string.unread_widget_select_account)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CHOOSE_ACCOUNT -> handleChooseAccount(data.getStringExtra(ChooseAccount.EXTRA_ACCOUNT_UUID))
                REQUEST_CHOOSE_FOLDER -> {
                    val folderServerId = data.getStringExtra(ChooseFolderActivity.RESULT_SELECTED_FOLDER)
                    val folderDisplayName = data.getStringExtra(ChooseFolderActivity.RESULT_FOLDER_DISPLAY_NAME)
                    handleChooseFolder(folderServerId, folderDisplayName)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleChooseAccount(accountUuid: String) {
        val userSelectedSameAccount = accountUuid == selectedAccountUuid
        if (userSelectedSameAccount) {
            return
        }

        selectedAccountUuid = accountUuid
        selectedFolder = null
        unreadFolder.summary = getString(R.string.unread_widget_folder_summary)
        if (SearchAccount.UNIFIED_INBOX == selectedAccountUuid) {
            handleSearchAccount()
        } else {
            handleRegularAccount()
        }
    }

    private fun handleSearchAccount() {
        if (SearchAccount.UNIFIED_INBOX == selectedAccountUuid) {
            unreadAccount.setSummary(R.string.unread_widget_unified_inbox_account_summary)
        }
        unreadFolderEnabled.isEnabled = false
        unreadFolderEnabled.isChecked = false
        unreadFolder.isEnabled = false
        selectedFolder = null
    }

    private fun handleRegularAccount() {
        val selectedAccount = Preferences.getPreferences(this).getAccount(selectedAccountUuid)
        val accountDescription: String? = selectedAccount.description
        val summary = if (accountDescription.isNullOrEmpty()) selectedAccount.email else accountDescription

        unreadAccount.summary = summary
        unreadFolderEnabled.isEnabled = true
        unreadFolder.isEnabled = true
    }

    private fun handleChooseFolder(folderServerId: String, folderDisplayName: String) {
        selectedFolder = folderServerId
        unreadFolder.summary = folderDisplayName
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.unread_widget_option, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.done -> {
                if (validateWidget()) {
                    updateWidgetAndExit()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun validateWidget(): Boolean {
        if (selectedAccountUuid == null) {
            Toast.makeText(this, R.string.unread_widget_account_not_selected, Toast.LENGTH_LONG).show()
            return false
        } else if (unreadFolderEnabled.isChecked && selectedFolder == null) {
            Toast.makeText(this, R.string.unread_widget_folder_not_selected, Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun updateWidgetAndExit() {
        val configuration = UnreadWidgetConfiguration(appWidgetId, selectedAccountUuid!!, selectedFolder)
        repository.saveWidgetConfiguration(configuration)

        unreadWidgetUpdater.update(appWidgetId)

        // Let the caller know that the configuration was successful
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }

    companion object {
        private const val PREFERENCE_UNREAD_ACCOUNT = "unread_account"
        private const val PREFERENCE_UNREAD_FOLDER_ENABLED = "unread_folder_enabled"
        private const val PREFERENCE_UNREAD_FOLDER = "unread_folder"

        private const val REQUEST_CHOOSE_ACCOUNT = 1
        private const val REQUEST_CHOOSE_FOLDER = 2
    }
}
