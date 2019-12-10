package com.fsck.k9.ui.managefolders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils.TruncateAt
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.SearchView.OnQueryTextListener
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import com.fsck.k9.Account
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.DI
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.activity.ActivityListener
import com.fsck.k9.activity.FolderInfoHolder
import com.fsck.k9.activity.K9ListActivity
import com.fsck.k9.activity.setup.FolderSettings
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.job.K9JobManager
import com.fsck.k9.mail.Folder.FolderClass
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.ui.R
import com.fsck.k9.ui.folders.FolderIconProvider
import com.fsck.k9.ui.helper.SizeFormatter
import java.util.Collections
import java.util.LinkedList
import java.util.Locale
import timber.log.Timber

class ManageFoldersActivity : K9ListActivity() {
    private val jobManager = DI.get(K9JobManager::class.java)
    private var folderListAdapter: FolderListAdapter? = null
    private var inflater: LayoutInflater? = null
    private var account: Account? = null
    private val handler = FolderListHandler()
    private val fontSizes = K9.fontSizes
    private var context: Context? = null
    private var actionBar: ActionBar? = null

    internal inner class FolderListHandler : Handler() {
        fun refreshTitle() {
            runOnUiThread {
                actionBar!!.setTitle(R.string.folders_action)
                val operation = folderListAdapter!!.activityListener.getOperation(this@ManageFoldersActivity)
                if (operation.length < 1) {
                    actionBar!!.subtitle = account!!.email
                } else {
                    actionBar!!.subtitle = operation
                }
            }
        }

        fun newFolders(newFolders: List<FolderInfoHolder>?) {
            runOnUiThread {
                folderListAdapter!!.folders.clear()
                folderListAdapter!!.folders.addAll(newFolders!!)
                folderListAdapter!!.filteredFolders = folderListAdapter!!.folders
                handler.dataChanged()
            }
        }

        fun workingAccount(res: Int) {
            runOnUiThread {
                val toastText = getString(res, account!!.description)
                val toast = Toast.makeText(application, toastText, Toast.LENGTH_SHORT)
                toast.show()
            }
        }

        fun accountSizeChanged(oldSize: Long, newSize: Long) {
            runOnUiThread {
                val toastText =
                    getString(R.string.account_size_changed, account!!.description, SizeFormatter.formatSize(application, oldSize), SizeFormatter.formatSize(application, newSize))
                val toast = Toast.makeText(application, toastText, Toast.LENGTH_LONG)
                toast.show()
            }
        }

        fun progress(progress: Boolean) { // TODO: Display progress indicator
        }

        fun dataChanged() {
            runOnUiThread { folderListAdapter!!.notifyDataSetChanged() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.folder_list)

        initializeActionBar()
        val listView = listView
        listView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        listView.isFastScrollEnabled = true
        listView.isScrollingCacheEnabled = false
        listView.onItemClickListener =
            OnItemClickListener { parent, view, position, id -> FolderSettings.actionSettings(this@ManageFoldersActivity, account, (folderListAdapter!!.getItem(position) as FolderInfoHolder).serverId) }
        listView.isSaveEnabled = true
        inflater = layoutInflater
        context = this

        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT)
        account = Preferences.getPreferences(this).getAccount(accountUuid)
        if (account == null) {
            finish()
            return
        }

        initializeActivityView()
    }

    private fun initializeActionBar() {
        actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    private fun initializeActivityView() {
        folderListAdapter = FolderListAdapter()
        restorePreviousData()
        setListAdapter(folderListAdapter)
        listView.isTextFilterEnabled =
            folderListAdapter!!.filter != null // should never be false but better safe then sorry
    }

    private fun restorePreviousData() {
        val previousData = lastCustomNonConfigurationInstance
        if (previousData != null) {
            folderListAdapter!!.folders = previousData as ArrayList<FolderInfoHolder>
            folderListAdapter!!.filteredFolders = Collections.unmodifiableList(folderListAdapter!!.folders)
        }
    }

    override fun onRetainCustomNonConfigurationInstance(): Any? {
        return if (folderListAdapter == null) null else folderListAdapter!!.folders
    }

    public override fun onPause() {
        super.onPause()
        MessagingController.getInstance(application).removeListener(folderListAdapter!!.activityListener)
        folderListAdapter!!.activityListener.onPause(this)
    }

    /**
     * On resume we refresh the folder list (in the background) and we refresh the
     * messages for any folder that is currently open. This guarantees that things
     * like unread message count and read status are updated.
     */
    public override fun onResume() {
        super.onResume()
        if (!account!!.isAvailable(this)) {
            Timber.i("Account is unavailable right now: $account")
            finish()
            return
        }
        if (folderListAdapter == null) initializeActivityView()
        handler.refreshTitle()
        MessagingController.getInstance(application).addListener(folderListAdapter!!.activityListener)
        // account.refresh(Preferences.getPreferences(this));
        onRefresh(!REFRESH_REMOTE)
        MessagingController.getInstance(application).cancelNotificationsForAccount(account)
        folderListAdapter!!.activityListener.onResume(this)
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
        folderListAdapter!!.filter.filter(null)
        onRefresh(false)
    }

    private fun onRefresh(forceRemote: Boolean) {
        MessagingController.getInstance(application).listFolders(account, forceRemote, folderListAdapter!!.activityListener)
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
        handler.workingAccount(R.string.compacting_account)
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
                folderMenuItem.collapseActionView()
                actionBar!!.setTitle(R.string.filter_folders_action)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                folderListAdapter!!.filter.filter(newText)
                return true
            }
        })
        folderSearchView.setOnCloseListener {
            actionBar!!.setTitle(R.string.folders_title)
            false
        }
    }

    internal inner class FolderListAdapter : BaseAdapter(), Filterable {
        var folders: MutableList<FolderInfoHolder> = ArrayList()
        var filteredFolders = Collections.unmodifiableList(folders)
        private val filter: Filter = FolderListFilter()
        private val folderIconProvider = FolderIconProvider(theme)
        override fun getItem(position: Int): Any {
            return filteredFolders[position]
        }

        override fun getItemId(position: Int): Long {
            return filteredFolders[position].folder.databaseId
        }

        override fun getCount(): Int {
            return filteredFolders.size
        }

        val activityListener: ActivityListener = object : ActivityListener() {
            override fun listFoldersStarted(account: Account) {
                if (account == this@ManageFoldersActivity.account) {
                    handler.progress(true)
                }
                super.listFoldersStarted(account)
            }

            override fun listFoldersFailed(account: Account, message: String) {
                if (account == this@ManageFoldersActivity.account) {
                    handler.progress(false)
                    runOnUiThread { Toast.makeText(context, R.string.fetching_folders_failed, Toast.LENGTH_SHORT).show() }
                }
                super.listFoldersFailed(account, message)
            }

            override fun listFoldersFinished(account: Account) {
                if (account == this@ManageFoldersActivity.account) {
                    handler.progress(false)
                    MessagingController.getInstance(application).refreshListener(this)
                    handler.dataChanged()
                }
                super.listFoldersFinished(account)
            }

            override fun listFolders(account: Account, folders: List<LocalFolder>) {
                if (account == this@ManageFoldersActivity.account) {
                    val newFolders: MutableList<FolderInfoHolder> = LinkedList()
                    val topFolders: MutableList<FolderInfoHolder> = LinkedList()
                    val aMode = account.folderDisplayMode
                    for (folder in folders) {
                        val fMode = folder.displayClass
                        if (aMode == FolderMode.FIRST_CLASS && fMode != FolderClass.FIRST_CLASS ||
                            aMode == FolderMode.FIRST_AND_SECOND_CLASS && fMode != FolderClass.FIRST_CLASS && fMode != FolderClass.SECOND_CLASS ||
                            aMode == FolderMode.NOT_SECOND_CLASS && fMode == FolderClass.SECOND_CLASS) {
                            continue
                        }
                        var holder: FolderInfoHolder? = null
                        val folderIndex = getFolderIndex(folder.serverId)
                        if (folderIndex >= 0) {
                            holder = getItem(folderIndex) as FolderInfoHolder
                        }
                        if (holder == null) {
                            holder = FolderInfoHolder(folder, this@ManageFoldersActivity.account, -1)
                        } else {
                            holder.populate(folder, this@ManageFoldersActivity.account, -1)
                        }
                        if (folder.isInTopGroup) {
                            topFolders.add(holder)
                        } else {
                            newFolders.add(holder)
                        }
                    }
                    Collections.sort(newFolders)
                    Collections.sort(topFolders)
                    topFolders.addAll(newFolders)
                    handler.newFolders(topFolders)
                }
                super.listFolders(account, folders)
            }

            override fun accountSizeChanged(account: Account, oldSize: Long, newSize: Long) {
                if (account == this@ManageFoldersActivity.account) {
                    handler.accountSizeChanged(oldSize, newSize)
                }
            }
        }

        fun getFolderIndex(folder: String?): Int {
            val searchHolder = FolderInfoHolder()
            searchHolder.serverId = folder
            return filteredFolders.indexOf(searchHolder)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
            return if (position <= count) {
                getItemView(position, convertView, parent)
            } else {
                Timber.e("getView with illegal position=%d called! count is only %d", position, count)
                null
            }
        }

        fun getItemView(itemPosition: Int, convertView: View?, parent: ViewGroup?): View {
            val folder = getItem(itemPosition) as? FolderInfoHolder
            val view: View
            view = convertView ?: inflater!!.inflate(R.layout.folder_list_item, parent, false)
            var holder = view.tag as? FolderViewHolder
            if (holder == null) {
                holder = FolderViewHolder()
                holder.folderName = view.findViewById(R.id.folder_name)
                holder.folderIcon = view.findViewById(R.id.folder_icon)
                holder.folderListItemLayout = view.findViewById(R.id.folder_list_item_layout)
                holder.folderServerId = folder!!.serverId
                view.tag = holder
            }
            if (folder == null) {
                return view
            }
            holder.folderName!!.text = folder.displayName
            holder.folderIcon!!.setImageResource(folderIconProvider.getFolderIcon(folder.folder.type))
            fontSizes.setViewTextSize(holder.folderName, fontSizes.folderName)
            if (K9.isWrapFolderNames) {
                holder.folderName!!.ellipsize = null
                holder.folderName!!.isSingleLine = false
            } else {
                holder.folderName!!.ellipsize = TruncateAt.START
                holder.folderName!!.isSingleLine = true
            }
            return view
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun getFilter(): Filter {
            return filter
        }

        /**
         * Filter to search for occurrences of the search-expression in any place of the
         * folder-name instead of doing just a prefix-search.
         */
        inner class FolderListFilter : Filter() {
            override fun performFiltering(searchTerm: CharSequence?): FilterResults {
                val results = FilterResults()
                val locale = Locale.getDefault()
                if (searchTerm == null || searchTerm.length == 0) {
                    val list: List<FolderInfoHolder> = ArrayList(folders)
                    results.values = list
                    results.count = list.size
                } else {
                    val searchTermString = searchTerm.toString().toLowerCase(locale)
                    val words = searchTermString.split(" ").toTypedArray()
                    val wordCount = words.size
                    val newValues: MutableList<FolderInfoHolder> = ArrayList()
                    for (value in folders) {
                        if (value.displayName == null) {
                            continue
                        }
                        val valueText = value.displayName.toLowerCase(locale)
                        for (k in 0 until wordCount) {
                            if (valueText.contains(words[k])) {
                                newValues.add(value)
                                break
                            }
                        }
                    }
                    results.values = newValues
                    results.count = newValues.size
                }
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                filteredFolders = Collections.unmodifiableList(results.values as ArrayList<FolderInfoHolder?>)
                // Send notification that the data set changed now
                notifyDataSetChanged()
            }
        }
    }

    class FolderViewHolder {
        var folderName: TextView? = null
        var folderServerId: String? = null
        var folderIcon: ImageView? = null
        var folderListItemLayout: LinearLayout? = null
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
