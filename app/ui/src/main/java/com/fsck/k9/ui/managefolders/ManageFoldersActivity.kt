package com.fsck.k9.ui.managefolders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.Account
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.Preferences
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.activity.setup.FolderSettings
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mailstore.DisplayFolder
import com.fsck.k9.ui.R
import com.fsck.k9.ui.folders.FolderIconProvider
import com.fsck.k9.ui.folders.FolderNameFormatter
import com.fsck.k9.ui.observeNotNull
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import java.util.Locale
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ManageFoldersActivity : K9Activity() {
    private val viewModel: ManageFoldersViewModel by viewModel()
    private val folderNameFormatter: FolderNameFormatter by inject { parametersOf(this) }
    private val messagingController: MessagingController by inject()
    private val preferences: Preferences by inject()
    private val folderIconProvider by lazy { FolderIconProvider(theme) }

    private lateinit var account: Account
    private lateinit var actionBar: ActionBar
    private lateinit var itemAdapter: ItemAdapter<FolderListItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.folder_list)

        if (!decodeArguments()) {
            finish()
            return
        }

        initializeActionBar()
        initializeFolderList()

        viewModel.getFolders(account).observeNotNull(this) { folders ->
            updateFolderList(folders)
        }
    }

    private fun decodeArguments(): Boolean {
        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT) ?: return false
        account = preferences.getAccount(accountUuid) ?: return false
        return true
    }

    private fun initializeActionBar() {
        actionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
    }

    private fun initializeFolderList() {
        itemAdapter = ItemAdapter()
        itemAdapter.itemFilter.filterPredicate = ::folderListFilter

        val folderListAdapter = FastAdapter.with(itemAdapter).apply {
            setHasStableIds(true)
            onClickListener = { _, _, item: FolderListItem, _ ->
                openFolderSettings(item.serverId)
                true
            }
        }

        val recyclerView = findViewById<RecyclerView>(R.id.folderList)
        recyclerView.adapter = folderListAdapter
    }

    private fun updateFolderList(displayFolders: List<DisplayFolder>) {
        val folderListItems = displayFolders.map { displayFolder ->
            val databaseId = displayFolder.folder.id
            val folderIconResource = folderIconProvider.getFolderIcon(displayFolder.folder.type)
            val displayName = folderNameFormatter.displayName(displayFolder.folder)
            val serverId = displayFolder.folder.serverId

            FolderListItem(databaseId, folderIconResource, displayName, serverId)
        }

        itemAdapter.set(folderListItems)
    }

    private fun openFolderSettings(folderServerId: String) {
        FolderSettings.actionSettings(this, account, folderServerId)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.folder_list_option, menu)
        configureFolderSearchView(menu)
        return true
    }

    private fun configureFolderSearchView(menu: Menu) {
        val folderMenuItem = menu.findItem(R.id.filter_folders)
        val folderSearchView = folderMenuItem.actionView as SearchView
        folderSearchView.queryHint = getString(R.string.folder_list_filter_hint)
        folderSearchView.setOnQueryTextListener(object : OnQueryTextListener {
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
            android.R.id.home -> onBackPressed()
            R.id.list_folders -> refreshFolderList()
            R.id.display_1st_class -> setDisplayMode(FolderMode.FIRST_CLASS)
            R.id.display_1st_and_2nd_class -> setDisplayMode(FolderMode.FIRST_AND_SECOND_CLASS)
            R.id.display_not_second_class -> setDisplayMode(FolderMode.NOT_SECOND_CLASS)
            R.id.display_all -> setDisplayMode(FolderMode.ALL)
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    private fun refreshFolderList() {
        messagingController.refreshFolderList(account)
    }

    private fun setDisplayMode(newMode: FolderMode) {
        account.folderDisplayMode = newMode
        preferences.saveAccount(account)

        itemAdapter.filter(null)
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
        private const val EXTRA_ACCOUNT = "account"

        @JvmStatic
        fun launch(context: Context, account: Account) {
            val intent = Intent(context, ManageFoldersActivity::class.java)
            intent.putExtra(EXTRA_ACCOUNT, account.uuid)
            context.startActivity(intent)
        }
    }
}
