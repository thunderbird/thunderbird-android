package com.fsck.k9.ui.managefolders

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import com.fsck.k9.fragment.ConfirmationDialogFragment
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.livedata.observeNotNull
import com.fsck.k9.ui.folders.FolderNameFormatter
import com.takisoft.preferencex.PreferenceFragmentCompat
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.fsck.k9.ui.base.R as BaseR

class FolderSettingsFragment : PreferenceFragmentCompat(), ConfirmationDialogFragmentListener {
    private val viewModel: FolderSettingsViewModel by viewModel()
    private val folderNameFormatter: FolderNameFormatter by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        // Set empty preferences resource while data is being loaded
        setPreferencesFromResource(R.xml.empty_preferences, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val arguments = arguments ?: error("Arguments missing")
        val accountUuid = arguments.getString(EXTRA_ACCOUNT) ?: error("Missing argument '$EXTRA_ACCOUNT'")
        val folderId = arguments.getLong(EXTRA_FOLDER_ID)

        viewModel.getFolderSettingsLiveData(accountUuid, folderId)
            .observeNotNull(viewLifecycleOwner) { folderSettingsResult ->
                when (folderSettingsResult) {
                    is FolderNotFound -> navigateBack()
                    is FolderSettingsData -> initPreferences(folderSettingsResult)
                }
            }

        viewModel.getActionEvents().observeNotNull(this) { handleActionEvents(it) }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.folder_settings_option, menu)

        val clearFolderItem = menu.findItem(R.id.clear_local_folder)
        clearFolderItem.isVisible = viewModel.showClearFolderInMenu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.clear_local_folder -> {
                viewModel.showClearFolderConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun navigateBack() {
        findNavController().popBackStack()
    }

    private fun initPreferences(folderSettings: FolderSettingsData) {
        preferenceManager.preferenceDataStore = folderSettings.dataStore
        setPreferencesFromResource(R.xml.folder_settings_preferences, null)

        setCategoryTitle(folderSettings)
        updateMenu()
        setPreferenceVisibility(folderSettings)
    }

    private fun updateMenu() {
        requireActivity().invalidateOptionsMenu()
    }

    private fun setCategoryTitle(folderSettings: FolderSettingsData) {
        val folderDisplayName = folderNameFormatter.displayName(folderSettings.folder)
        findPreference<Preference>(PREFERENCE_TOP_CATEGORY)!!.title = folderDisplayName
    }

    private fun handleActionEvents(action: Action) {
        when (action) {
            is Action.ShowClearFolderConfirmationDialog -> showClearFolderConfirmationDialog()
        }
    }

    private fun showClearFolderConfirmationDialog() {
        val dialogFragment = ConfirmationDialogFragment.newInstance(
            DIALOG_CLEAR_FOLDER,
            getString(R.string.dialog_confirm_clear_local_folder_title),
            getString(R.string.dialog_confirm_clear_local_folder_message),
            getString(R.string.dialog_confirm_clear_local_folder_action),
            getString(BaseR.string.cancel_action),
        )
        dialogFragment.setTargetFragment(this, REQUEST_CLEAR_FOLDER)
        dialogFragment.show(requireFragmentManager(), TAG_CLEAR_FOLDER_CONFIRMATION)
    }

    private fun setPreferenceVisibility(folderSettings: FolderSettingsData) {
        if (folderSettings.folder.isLocalOnly) {
            requirePreference<Preference>(PREFERENCE_POLL_CLASS).isVisible = false
            requirePreference<Preference>(PREFERENCE_PUSH_CLASS).isVisible = false
            requirePreference<Preference>(PREFERENCE_NOTIFICATION_CLASS).isVisible = false
        }
    }

    override fun doPositiveClick(dialogId: Int) {
        when (dialogId) {
            DIALOG_CLEAR_FOLDER -> {
                viewModel.onClearFolderConfirmation()
            }
        }
    }

    override fun doNegativeClick(dialogId: Int) = Unit

    override fun dialogCancelled(dialogId: Int) = Unit

    private fun <T : Preference> requirePreference(key: String): T {
        return findPreference(key) ?: error("Preference '$key' not found")
    }

    companion object {
        const val EXTRA_ACCOUNT = "account"
        const val EXTRA_FOLDER_ID = "folderId"

        private const val DIALOG_CLEAR_FOLDER = 1
        private const val REQUEST_CLEAR_FOLDER = 1
        private const val TAG_CLEAR_FOLDER_CONFIRMATION = "clear_folder_confirmation"

        private const val PREFERENCE_TOP_CATEGORY = "folder_settings"
        private const val PREFERENCE_POLL_CLASS = "folder_settings_folder_sync_mode"
        private const val PREFERENCE_PUSH_CLASS = "folder_settings_folder_push_mode"
        private const val PREFERENCE_NOTIFICATION_CLASS = "folder_settings_folder_notify_mode"
    }
}
