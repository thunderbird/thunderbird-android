package com.fsck.k9.ui.choosefolder

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.Account
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.Preferences
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mailstore.DisplayFolder
import com.fsck.k9.ui.R
import com.fsck.k9.ui.folders.FolderIconProvider
import com.fsck.k9.ui.folders.FolderNameFormatter
import com.fsck.k9.ui.folders.FoldersLiveData
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import java.util.Locale
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ChooseFolderActivity : K9Activity() {
    private val viewModel: ChooseFolderViewModel by viewModel()
    private val preferences: Preferences by inject()
    private val messagingController: MessagingController by inject()
    private val folderNameFormatter: FolderNameFormatter by inject { parametersOf(this) }
    private val folderIconProvider by lazy { FolderIconProvider(theme) }

    private lateinit var recyclerView: RecyclerView
    private lateinit var itemAdapter: ItemAdapter<FolderListItem>
    private lateinit var account: Account
    private var currentFolder: String? = null
    private var scrollToFolder: String? = null
    private var messageReference: String? = null
    private var showDisplayableOnly = false
    private var foldersLiveData: FoldersLiveData? = null

    private val folderListObserver = Observer<List<DisplayFolder>> { folders ->
        updateFolderList(folders)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.folder_list)

        if (!decodeArguments(savedInstanceState)) {
            finish()
            return
        }

        initializeFolderList()

        val savedDisplayMode = savedInstanceState?.getString(STATE_DISPLAY_MODE)?.let { FolderMode.valueOf(it) }
        val displayMode = savedDisplayMode ?: getInitialDisplayMode()

        foldersLiveData = viewModel.getFolders(account, displayMode).apply {
            observe(this@ChooseFolderActivity, folderListObserver)
        }
    }

    private fun decodeArguments(savedInstanceState: Bundle?): Boolean {
        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT) ?: return false
        account = preferences.getAccount(accountUuid) ?: return false

        messageReference = intent.getStringExtra(EXTRA_MESSAGE_REFERENCE)
        currentFolder = intent.getStringExtra(EXTRA_CURRENT_FOLDER)
        showDisplayableOnly = intent.getBooleanExtra(EXTRA_SHOW_DISPLAYABLE_ONLY, false)

        scrollToFolder = if (savedInstanceState != null) {
            savedInstanceState.getString(STATE_SCROLL_TO_FOLDER)
        } else {
            intent.getStringExtra(EXTRA_SCROLL_TO_FOLDER)
        }

        return true
    }

    private fun getInitialDisplayMode(): FolderMode {
        return if (showDisplayableOnly) account.folderDisplayMode else account.folderTargetMode
    }

    private fun initializeFolderList() {
        itemAdapter = ItemAdapter()
        itemAdapter.itemFilter.filterPredicate = ::folderListFilter

        val folderListAdapter = FastAdapter.with(itemAdapter).apply {
            setHasStableIds(true)
            onClickListener = { _, _, item: FolderListItem, _ ->
                returnResult(item.serverId, item.displayName)
                true
            }
        }

        recyclerView = findViewById(R.id.folderList)
        recyclerView.adapter = folderListAdapter
    }

    private fun updateFolderList(displayFolders: List<DisplayFolder>) {
        val foldersToHide = if (currentFolder != null) {
            setOf(currentFolder, Account.OUTBOX)
        } else {
            setOf(Account.OUTBOX)
        }

        val folderListItems = displayFolders.asSequence()
            .filterNot { it.folder.serverId in foldersToHide }
            .map { displayFolder ->
                val databaseId = displayFolder.folder.id
                val folderIconResource = folderIconProvider.getFolderIcon(displayFolder.folder.type)
                val displayName = folderNameFormatter.displayName(displayFolder.folder)
                val serverId = displayFolder.folder.serverId

                FolderListItem(databaseId, folderIconResource, displayName, serverId)
            }
            .toList()

        itemAdapter.set(folderListItems)

        scrollToFolder(folderListItems)
    }

    private fun scrollToFolder(folders: List<FolderListItem>) {
        if (scrollToFolder == null) return

        val index = folders.indexOfFirst { it.serverId == scrollToFolder }
        if (index != -1) {
            recyclerView.scrollToPosition(index)
        }

        scrollToFolder = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SCROLL_TO_FOLDER, scrollToFolder)
        outState.putString(STATE_DISPLAY_MODE, foldersLiveData?.displayMode?.name)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.choose_folder_option, menu)
        configureFolderSearchView(menu)
        return true
    }

    private fun configureFolderSearchView(menu: Menu) {
        val folderMenuItem = menu.findItem(R.id.filter_folders)
        val folderSearchView = folderMenuItem.actionView as SearchView
        folderSearchView.queryHint = getString(R.string.folder_list_filter_hint)
        folderSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                itemAdapter.filter(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                itemAdapter.filter(newText)
                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.display_1st_class -> setDisplayMode(FolderMode.FIRST_CLASS)
            R.id.display_1st_and_2nd_class -> setDisplayMode(FolderMode.FIRST_AND_SECOND_CLASS)
            R.id.display_not_second_class -> setDisplayMode(FolderMode.NOT_SECOND_CLASS)
            R.id.display_all -> setDisplayMode(FolderMode.ALL)
            R.id.list_folders -> refreshFolderList()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun refreshFolderList() {
        messagingController.refreshFolderList(account)
    }

    private fun setDisplayMode(displayMode: FolderMode) {
        foldersLiveData?.removeObserver(folderListObserver)
        foldersLiveData = viewModel.getFolders(account, displayMode).apply {
            observe(this@ChooseFolderActivity, folderListObserver)
        }
    }

    private fun returnResult(folderServerId: String, displayName: String) {
        val result = Intent().apply {
            putExtra(RESULT_SELECTED_FOLDER, folderServerId)
            putExtra(RESULT_FOLDER_DISPLAY_NAME, displayName)
            putExtra(RESULT_MESSAGE_REFERENCE, messageReference)
        }

        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun folderListFilter(item: FolderListItem, constraint: CharSequence?): Boolean {
        if (constraint.isNullOrEmpty()) return true

        val locale = Locale.getDefault()
        val displayName = item.displayName.toLowerCase(locale)
        return constraint.splitToSequence(" ")
            .map { it.toLowerCase(locale) }
            .any { it in displayName }
    }

    companion object {
        private const val STATE_SCROLL_TO_FOLDER = "scrollToFolder"
        private const val STATE_DISPLAY_MODE = "displayMode"
        private const val EXTRA_ACCOUNT = "accountUuid"
        private const val EXTRA_CURRENT_FOLDER = "currentFolder"
        private const val EXTRA_SCROLL_TO_FOLDER = "scrollToFolder"
        private const val EXTRA_MESSAGE_REFERENCE = "messageReference"
        private const val EXTRA_SHOW_DISPLAYABLE_ONLY = "showDisplayableOnly"
        const val RESULT_SELECTED_FOLDER = "selectedFolder"
        const val RESULT_FOLDER_DISPLAY_NAME = "folderDisplayName"
        const val RESULT_MESSAGE_REFERENCE = "messageReference"

        @JvmStatic
        fun buildLaunchIntent(
            context: Context,
            accountUuid: String,
            currentFolder: String? = null,
            scrollToFolder: String? = null,
            showDisplayableOnly: Boolean = false,
            messageReference: MessageReference? = null
        ): Intent {
            return Intent(context, ChooseFolderActivity::class.java).apply {
                putExtra(EXTRA_ACCOUNT, accountUuid)
                putExtra(EXTRA_CURRENT_FOLDER, currentFolder)
                putExtra(EXTRA_SCROLL_TO_FOLDER, scrollToFolder)
                putExtra(EXTRA_SHOW_DISPLAYABLE_ONLY, showDisplayableOnly)
                messageReference?.let { putExtra(EXTRA_MESSAGE_REFERENCE, it.toIdentityString()) }
            }
        }
    }
}
