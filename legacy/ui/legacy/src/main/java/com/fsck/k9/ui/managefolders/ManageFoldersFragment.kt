package com.fsck.k9.ui.managefolders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import app.k9mail.legacy.ui.folder.DisplayFolder
import app.k9mail.legacy.ui.folder.FolderIconProvider
import app.k9mail.legacy.ui.folder.FolderNameFormatter
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.livedata.observeNotNull
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import java.util.Locale
import net.thunderbird.core.android.account.LegacyAccount
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ManageFoldersFragment : Fragment() {
    private val viewModel: ManageFoldersViewModel by viewModel()
    private val folderNameFormatter: FolderNameFormatter by inject()
    private val messagingController: MessagingController by inject()
    private val preferences: Preferences by inject()
    private val folderIconProvider: FolderIconProvider by inject { parametersOf(requireActivity().theme) }

    private lateinit var account: LegacyAccount
    private lateinit var itemAdapter: ItemAdapter<FolderListItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val arguments = arguments ?: error("Missing arguments")
        val accountUuid = arguments.getString(EXTRA_ACCOUNT) ?: error("Missing argument '$EXTRA_ACCOUNT'")
        account = preferences.getAccount(accountUuid) ?: error("Missing account: $accountUuid")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_manage_folders, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initializeFolderList()

        viewModel.getFolders(account).observeNotNull(this) { folders ->
            updateFolderList(folders)
        }
    }

    private fun initializeFolderList() {
        itemAdapter = ItemAdapter()
        itemAdapter.itemFilter.filterPredicate = ::folderListFilter

        val folderListAdapter = FastAdapter.with(itemAdapter).apply {
            setHasStableIds(true)
            onClickListener = { _, _, item: FolderListItem, _ ->
                openFolderSettings(item.folderId)
                true
            }
        }

        val recyclerView = requireView().findViewById<RecyclerView>(R.id.folderList)
        recyclerView.adapter = folderListAdapter
    }

    private fun updateFolderList(displayFolders: List<DisplayFolder>) {
        val folderListItems = displayFolders.map { displayFolder ->
            val databaseId = displayFolder.folder.id
            val folderIconResource = folderIconProvider.getFolderIcon(displayFolder.folder.type)
            val displayName = folderNameFormatter.displayName(displayFolder.folder)

            FolderListItem(databaseId, folderIconResource, displayName)
        }

        itemAdapter.set(folderListItems)
    }

    private fun openFolderSettings(folderId: Long) {
        val folderSettingsArguments = bundleOf(
            FolderSettingsFragment.EXTRA_ACCOUNT to account.uuid,
            FolderSettingsFragment.EXTRA_FOLDER_ID to folderId,
        )
        findNavController().navigate(R.id.action_manageFoldersScreen_to_folderSettingsScreen, folderSettingsArguments)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.folder_list_option, menu)
        configureFolderSearchView(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val folderMenuItem = menu.findItem(R.id.filter_folders)
        folderMenuItem.isVisible = !folderMenuItem.isActionViewExpanded
    }

    private fun configureFolderSearchView(menu: Menu) {
        val folderMenuItem = menu.findItem(R.id.filter_folders)
        val folderSearchView = folderMenuItem.actionView as SearchView
        folderSearchView.queryHint = getString(R.string.folder_list_filter_hint)
        folderSearchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    itemAdapter.filter(query)
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    itemAdapter.filter(newText)
                    return true
                }
            },
        )
        folderMenuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem): Boolean {
                requireActivity().invalidateOptionsMenu()
                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.list_folders -> refreshFolderList()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    private fun refreshFolderList() {
        messagingController.refreshFolderList(account)
    }

    private fun folderListFilter(item: FolderListItem, constraint: CharSequence?): Boolean {
        if (constraint.isNullOrEmpty()) return true

        val locale = Locale.getDefault()
        val displayName = item.displayName.lowercase(locale)
        return constraint.splitToSequence(" ")
            .filter { it.isNotEmpty() }
            .map { it.lowercase(locale) }
            .any { it in displayName }
    }

    companion object {
        const val EXTRA_ACCOUNT = "account"
    }
}
