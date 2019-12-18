package com.fsck.k9.ui.choosefolder

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.Window
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
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

class ChooseFolderActivity : K9Activity() {
    private val viewModel: ChooseFolderViewModel by viewModel()
    private val folderNameFormatter: FolderNameFormatter by inject()
    private val folderIconProvider by lazy { FolderIconProvider(theme) }

    private lateinit var itemAdapter: ItemAdapter<FolderListItem>
    private lateinit var account: Account
    private var currentFolder: String? = null
    private var selectFolder: String? = null
    private var messageReference: MessageReference? = null
    private var hideCurrentFolder = true
    private var showDisplayableOnly = false
    private var foldersLiveData: FoldersLiveData? = null
    private val folderListObserver = Observer<List<DisplayFolder>> { folders ->
        updateFolderList(folders)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        setLayout(R.layout.folder_list)

        val intent = intent
        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT)
        account = Preferences.getPreferences(this).getAccount(accountUuid)
        if (intent.hasExtra(EXTRA_MESSAGE)) {
            val messageReferenceString = intent.getStringExtra(EXTRA_MESSAGE)
            messageReference = MessageReference.parse(messageReferenceString)
        }
        currentFolder = intent.getStringExtra(EXTRA_CUR_FOLDER)
        selectFolder = intent.getStringExtra(EXTRA_SEL_FOLDER)
        if (intent.getStringExtra(EXTRA_SHOW_CURRENT) != null) {
            hideCurrentFolder = false
        }
        if (intent.getStringExtra(EXTRA_SHOW_DISPLAYABLE_ONLY) != null) {
            showDisplayableOnly = true
        }
        if (currentFolder == null) currentFolder = ""

        initializeFolderList()

        foldersLiveData = viewModel.getFolders(account, account.folderTargetMode).apply {
            observe(this@ChooseFolderActivity, folderListObserver)
        }
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

        val recyclerView = findViewById<RecyclerView>(R.id.folderList)
        recyclerView.adapter = folderListAdapter
    }

    private fun updateFolderList(displayFolders: List<DisplayFolder>) {
        val foldersToHide = if (hideCurrentFolder) setOf(currentFolder, Account.OUTBOX) else setOf(Account.OUTBOX)

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
    }

    private fun returnResult(folderServerId: String, displayName: String) {
        val result = Intent()
        result.putExtra(EXTRA_ACCOUNT, account.uuid)
        result.putExtra(EXTRA_CUR_FOLDER, currentFolder)
        result.putExtra(EXTRA_NEW_FOLDER, folderServerId)
        if (messageReference != null) {
            result.putExtra(EXTRA_MESSAGE, messageReference!!.toIdentityString())
        }
        result.putExtra(RESULT_FOLDER_DISPLAY_NAME, displayName)
        setResult(Activity.RESULT_OK, result)
        finish()
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
        val id = item.itemId
        return if (id == R.id.display_1st_class) {
            setDisplayMode(FolderMode.FIRST_CLASS)
            true
        } else if (id == R.id.display_1st_and_2nd_class) {
            setDisplayMode(FolderMode.FIRST_AND_SECOND_CLASS)
            true
        } else if (id == R.id.display_not_second_class) {
            setDisplayMode(FolderMode.NOT_SECOND_CLASS)
            true
        } else if (id == R.id.display_all) {
            setDisplayMode(FolderMode.ALL)
            true
        } else if (id == R.id.list_folders) {
            onRefresh()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun onRefresh() {
        MessagingController.getInstance(application).listFolders(account, true, null)
    }

    private fun setDisplayMode(displayMode: FolderMode) {
        foldersLiveData?.removeObserver(folderListObserver)
        foldersLiveData = viewModel.getFolders(account, displayMode).apply {
            observe(this@ChooseFolderActivity, folderListObserver)
        }
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
        const val EXTRA_ACCOUNT = "com.fsck.k9.ChooseFolder_account"
        const val EXTRA_CUR_FOLDER = "com.fsck.k9.ChooseFolder_curfolder"
        const val EXTRA_SEL_FOLDER = "com.fsck.k9.ChooseFolder_selfolder"
        const val EXTRA_NEW_FOLDER = "com.fsck.k9.ChooseFolder_newfolder"
        const val EXTRA_MESSAGE = "com.fsck.k9.ChooseFolder_message"
        const val EXTRA_SHOW_CURRENT = "com.fsck.k9.ChooseFolder_showcurrent"
        const val EXTRA_SHOW_DISPLAYABLE_ONLY = "com.fsck.k9.ChooseFolder_showDisplayableOnly"
        const val RESULT_FOLDER_DISPLAY_NAME = "folderDisplayName"
    }
}
