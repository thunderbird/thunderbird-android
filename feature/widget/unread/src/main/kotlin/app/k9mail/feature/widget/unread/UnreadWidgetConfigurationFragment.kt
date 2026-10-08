package app.k9mail.feature.widget.unread

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import com.fsck.k9.ui.choosefolder.ChooseFolderActivity
import com.fsck.k9.ui.choosefolder.ChooseFolderResultContract
import com.takisoft.preferencex.PreferenceFragmentCompat
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.UnifiedAccountId
import net.thunderbird.feature.search.legacy.SearchAccount
import org.koin.android.ext.android.inject

@Suppress("TooManyFunctions")
class UnreadWidgetConfigurationFragment : PreferenceFragmentCompat() {
    private val accountManager: LegacyAccountManager by inject()
    private val repository: UnreadWidgetRepository by inject()
    private val unreadWidgetUpdater: UnreadWidgetUpdater by inject()

    private val chooseAccountLauncher: ActivityResultLauncher<Unit> =
        registerForActivityResult(UnreadWidgetChooseAccountResultContract()) { accountId ->
            handleChooseAccount(accountId)
        }
    private val chooseFolderLauncher: ActivityResultLauncher<ChooseFolderResultContract.Input> =
        registerForActivityResult(ChooseFolderResultContract(action = ChooseFolderActivity.Action.CHOOSE)) { result ->
            if (result != null) {
                handleChooseFolder(
                    folderId = result.folderId,
                    folderDisplayName = result.folderDisplayName,
                )
            }
        }

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var unreadAccount: Preference
    private lateinit var unreadFolderEnabled: CheckBoxPreference
    private lateinit var unreadFolder: Preference

    private var selectedAccountId: AccountId? = null
    private var selectedFolderId: Long? = null
    private var selectedFolderDisplayName: String? = null

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.unread_widget_configuration, rootKey)

        appWidgetId = arguments?.getInt(ARGUMENT_APP_WIDGET_ID) ?: error("Missing argument '$ARGUMENT_APP_WIDGET_ID'")

        unreadAccount = findPreference(PREFERENCE_UNREAD_ACCOUNT)!!
        unreadAccount.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            chooseAccountLauncher.launch(Unit)
            false
        }

        unreadFolderEnabled = findPreference(PREFERENCE_UNREAD_FOLDER_ENABLED)!!
        unreadFolderEnabled.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
            unreadFolder.summary = getString(R.string.unread_widget_folder_summary)
            selectedFolderId = null
            selectedFolderDisplayName = null
            true
        }

        unreadFolder = findPreference(PREFERENCE_UNREAD_FOLDER)!!
        unreadFolder.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            chooseFolderLauncher.launch(
                input = ChooseFolderResultContract.Input(
                    accountId = selectedAccountId!!,
                ),
            )
            false
        }

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureMenuHost()
    }

    private fun configureMenuHost() {
        val menuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.unread_widget_option, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.done -> {
                            if (validateWidget()) {
                                updateWidgetAndExit()
                            }
                            true
                        }

                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED,
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTED_ACCOUNT_UUID, selectedAccountId.toString())
        outState.putLongIfPresent(STATE_SELECTED_FOLDER_ID, selectedFolderId)
        outState.putString(STATE_SELECTED_FOLDER_DISPLAY_NAME, selectedFolderDisplayName)
    }

    private fun restoreInstanceState(savedInstanceState: Bundle) {
        val accountIdRaw = savedInstanceState.getString(STATE_SELECTED_ACCOUNT_UUID)
        if (accountIdRaw != null) {
            handleChooseAccount(AccountIdFactory.of(accountIdRaw))
            val folderId = savedInstanceState.getLongOrNull(STATE_SELECTED_FOLDER_ID)
            val folderSummary = savedInstanceState.getString(STATE_SELECTED_FOLDER_DISPLAY_NAME)
            if (folderId != null && folderSummary != null) {
                handleChooseFolder(folderId, folderSummary)
            }
        }
    }

    private fun handleChooseAccount(accountId: AccountId?) {
        val userSelectedSameAccount = accountId == selectedAccountId
        if (userSelectedSameAccount) {
            return
        }

        selectedAccountId = accountId
        selectedFolderId = null
        selectedFolderDisplayName = null
        unreadFolder.summary = getString(R.string.unread_widget_folder_summary)
        if (UnifiedAccountId == selectedAccountId) {
            handleUnifiedFoldersSearch()
        } else {
            handleRegularSearch()
        }
    }

    private fun handleUnifiedFoldersSearch() {
        if (UnifiedAccountId == selectedAccountId) {
            unreadAccount.setSummary(R.string.unread_widget_unified_inbox_account_summary)
        }
        unreadFolderEnabled.isEnabled = false
        unreadFolderEnabled.isChecked = false
        unreadFolder.isEnabled = false
        selectedFolderId = null
        selectedFolderDisplayName = null
    }

    private fun handleRegularSearch() {
        val selectedAccount = accountManager.findById(selectedAccountId!!)
            ?: error("Account $selectedAccountId not found")

        unreadAccount.summary = selectedAccount.profile.name
        unreadFolderEnabled.isEnabled = true
        unreadFolder.isEnabled = true
    }

    private fun handleChooseFolder(folderId: Long, folderDisplayName: String) {
        selectedFolderId = folderId
        selectedFolderDisplayName = folderDisplayName
        unreadFolder.summary = folderDisplayName
    }

    private fun validateWidget(): Boolean {
        return if (selectedAccountId == null) {
            Toast.makeText(requireContext(), R.string.unread_widget_account_not_selected, Toast.LENGTH_LONG).show()
            false
        } else if (unreadFolderEnabled.isChecked && selectedFolderId == null) {
            Toast.makeText(requireContext(), R.string.unread_widget_folder_not_selected, Toast.LENGTH_LONG).show()
            false
        } else {
            true
        }
    }

    private fun updateWidgetAndExit() {
        val configuration = UnreadWidgetConfiguration(appWidgetId, selectedAccountId!!, selectedFolderId)
        repository.saveWidgetConfiguration(configuration)

        unreadWidgetUpdater.update(appWidgetId)

        // Let the caller know that the configuration was successful
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

        val activity = requireActivity()
        activity.setResult(Activity.RESULT_OK, resultValue)
        activity.finish()
    }

    private fun Bundle.putLongIfPresent(key: String, value: Long?) {
        if (value != null) {
            putLong(key, value)
        }
    }

    private fun Bundle.getLongOrNull(key: String): Long? {
        return if (containsKey(key)) getLong(key) else null
    }

    companion object {
        private const val ARGUMENT_APP_WIDGET_ID = "app_widget_id"

        private const val PREFERENCE_UNREAD_ACCOUNT = "unread_account"
        private const val PREFERENCE_UNREAD_FOLDER_ENABLED = "unread_folder_enabled"
        private const val PREFERENCE_UNREAD_FOLDER = "unread_folder"

        private const val STATE_SELECTED_ACCOUNT_UUID = "com.fsck.k9.widget.unread.selectedAccountUuid"
        private const val STATE_SELECTED_FOLDER_ID = "com.fsck.k9.widget.unread.selectedFolderId"
        private const val STATE_SELECTED_FOLDER_DISPLAY_NAME = "com.fsck.k9.widget.unread.selectedFolderDisplayName"

        fun create(appWidgetId: Int): UnreadWidgetConfigurationFragment {
            return UnreadWidgetConfigurationFragment().apply {
                arguments = bundleOf(ARGUMENT_APP_WIDGET_ID to appWidgetId)
            }
        }
    }
}
