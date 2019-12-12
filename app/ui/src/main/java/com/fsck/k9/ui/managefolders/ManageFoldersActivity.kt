package com.fsck.k9.ui.managefolders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.Account
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.DI
import com.fsck.k9.Preferences
import com.fsck.k9.activity.ActivityListener
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.activity.setup.FolderSettings
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.job.K9JobManager
import com.fsck.k9.mailstore.DisplayFolder
import com.fsck.k9.ui.R
import com.fsck.k9.ui.folders.FolderIconProvider
import com.fsck.k9.ui.folders.FolderNameFormatter
import com.fsck.k9.ui.helper.SizeFormatter
import com.fsck.k9.ui.observeNotNull
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import java.util.Locale
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ManageFoldersActivity : K9Activity() {
    private val viewModel: ManageFoldersViewModel by viewModel()
    private val folderNameFormatter: FolderNameFormatter by inject()
    private val jobManager = DI.get(K9JobManager::class.java)
    private val folderIconProvider by lazy { FolderIconProvider(theme) }

    private lateinit var actionBar: ActionBar
    private lateinit var itemAdapter: ItemAdapter<FolderListItem>

    private var account: Account? = null
    private val activityListener = object : ActivityListener() {
        override fun accountSizeChanged(account: Account, oldSize: Long, newSize: Long) {
            if (account == this@ManageFoldersActivity.account) {
                runOnUiThread {
                    val toastText = getString(
                        R.string.account_size_changed,
                        account.description,
                        SizeFormatter.formatSize(application, oldSize),
                        SizeFormatter.formatSize(application, newSize)
                    )
                    val toast = Toast.makeText(application, toastText, Toast.LENGTH_LONG)
                    toast.show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.folder_list)

        initializeActionBar()
        initializeFolderList()

        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT)
        val account = Preferences.getPreferences(this).getAccount(accountUuid)
        if (account == null) {
            finish()
            return
        }
        this.account = account

        viewModel.getFolders(account).observeNotNull(this) { folders ->
            updateFolderList(folders)
        }
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
                onFolderListItemClicked(item.serverId)
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

    private fun onFolderListItemClicked(folderServerId: String) {
        FolderSettings.actionSettings(this@ManageFoldersActivity, account, folderServerId)
    }

    public override fun onPause() {
        MessagingController.getInstance(application).removeListener(activityListener)
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        MessagingController.getInstance(application).addListener(activityListener)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean { // Shortcuts that work no matter what is selected
        when (keyCode) {
            KeyEvent.KEYCODE_H -> {
                val toast = Toast.makeText(this, R.string.folder_list_help_key, Toast.LENGTH_LONG)
                toast.show()
                return true
            }
            KeyEvent.KEYCODE_1 -> {
                setDisplayMode(FolderMode.FIRST_CLASS)
                return true
            }
            KeyEvent.KEYCODE_2 -> {
                setDisplayMode(FolderMode.FIRST_AND_SECOND_CLASS)
                return true
            }
            KeyEvent.KEYCODE_3 -> {
                setDisplayMode(FolderMode.NOT_SECOND_CLASS)
                return true
            }
            KeyEvent.KEYCODE_4 -> {
                setDisplayMode(FolderMode.ALL)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    } // onKeyDown

    private fun setDisplayMode(newMode: FolderMode) {
        account!!.folderDisplayMode = newMode
        Preferences.getPreferences(applicationContext).saveAccount(account)
        if (account!!.folderPushMode != FolderMode.NONE) {
            jobManager.schedulePusherRefresh()
        }

        itemAdapter.filter(null)
    }

    private fun onRefresh(forceRemote: Boolean) {
        MessagingController.getInstance(application).listFolders(account, forceRemote, activityListener)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return if (id == android.R.id.home) {
            onBackPressed()
            true
        } else if (id == R.id.list_folders) {
            onRefresh(REFRESH_REMOTE)
            true
        } else if (id == R.id.compact) {
            onCompact(account)
            true
        } else if (id == R.id.display_1st_class) {
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
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun onCompact(account: Account?) {
        val toastText = getString(R.string.compacting_account, account!!.description)
        val toast = Toast.makeText(application, toastText, Toast.LENGTH_SHORT)
        toast.show()

        MessagingController.getInstance(application).compact(account, null)
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
        private const val REFRESH_REMOTE = true

        @JvmStatic
        fun launch(context: Context, account: Account) {
            val intent = Intent(context, ManageFoldersActivity::class.java)
            intent.putExtra(EXTRA_ACCOUNT, account.uuid)
            context.startActivity(intent)
        }
    }
}
