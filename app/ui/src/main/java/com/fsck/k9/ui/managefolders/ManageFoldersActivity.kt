package com.fsck.k9.ui.managefolders

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import com.fsck.k9.activity.K9ListActivity
import com.fsck.k9.activity.setup.FolderSettings
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.job.K9JobManager
import com.fsck.k9.mailstore.DisplayFolder
import com.fsck.k9.ui.R
import com.fsck.k9.ui.folders.FolderIconProvider
import com.fsck.k9.ui.folders.FolderNameFormatter
import com.fsck.k9.ui.helper.SizeFormatter
import com.fsck.k9.ui.observeNotNull
import java.util.Locale
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ManageFoldersActivity : K9ListActivity() {
    private val viewModel: ManageFoldersViewModel by viewModel()
    private val folderNameFormatter: FolderNameFormatter by inject()
    private val jobManager = DI.get(K9JobManager::class.java)
    private var folderListAdapter: FolderListAdapter? = null
    private var inflater: LayoutInflater? = null
    private var account: Account? = null
    private val fontSizes = K9.fontSizes
    private var context: Context? = null
    private var actionBar: ActionBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.folder_list)

        initializeActionBar()
        val listView = listView
        listView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        listView.isFastScrollEnabled = true
        listView.isScrollingCacheEnabled = false
        listView.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            val folderServerId = (folderListAdapter!!.getItem(position) as DisplayFolder).folder.serverId
            FolderSettings.actionSettings(this@ManageFoldersActivity, account, folderServerId)
        }
        listView.isSaveEnabled = true
        inflater = layoutInflater
        context = this

        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT)
        val account = Preferences.getPreferences(this).getAccount(accountUuid)
        if (account == null) {
            finish()
            return
        }
        this.account = account

        initializeActivityView()

        viewModel.getFolders(account).observeNotNull(this) { folders ->
            updateFolderList(folders)
        }
    }

    private fun updateFolderList(displayFolders: List<DisplayFolder>) {
        folderListAdapter!!.apply {
            folders.clear()
            folders.addAll(displayFolders)
            filteredFolders = folders
            notifyDataSetChanged()
        }
    }

    private fun initializeActionBar() {
        actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    private fun initializeActivityView() {
        folderListAdapter = FolderListAdapter()
        setListAdapter(folderListAdapter)
        listView.isTextFilterEnabled =
            folderListAdapter!!.filter != null // should never be false but better safe then sorry
    }

    public override fun onPause() {
        MessagingController.getInstance(application).removeListener(folderListAdapter!!.activityListener)
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        MessagingController.getInstance(application).addListener(folderListAdapter!!.activityListener)
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
        var folders: MutableList<DisplayFolder> = mutableListOf()
        var filteredFolders = folders.toList()
        private val filter: Filter = FolderListFilter()
        private val folderIconProvider = FolderIconProvider(theme)
        override fun getItem(position: Int): Any {
            return filteredFolders[position]
        }

        override fun getItemId(position: Int): Long {
            return filteredFolders[position].folder.id
        }

        override fun getCount(): Int {
            return filteredFolders.size
        }

        val activityListener: ActivityListener = object : ActivityListener() {
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

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
            return if (position <= count) {
                getItemView(position, convertView, parent)
            } else {
                Timber.e("getView with illegal position=%d called! count is only %d", position, count)
                null
            }
        }

        fun getItemView(itemPosition: Int, convertView: View?, parent: ViewGroup?): View {
            val displayFolder = getItem(itemPosition) as DisplayFolder
            val view: View
            view = convertView ?: inflater!!.inflate(R.layout.folder_list_item, parent, false)
            var holder = view.tag as? FolderViewHolder
            if (holder == null) {
                holder = FolderViewHolder()
                holder.folderName = view.findViewById(R.id.folder_name)
                holder.folderIcon = view.findViewById(R.id.folder_icon)
                holder.folderListItemLayout = view.findViewById(R.id.folder_list_item_layout)
                holder.folderServerId = displayFolder.folder.serverId
                view.tag = holder
            }

            holder.folderName!!.text = folderNameFormatter.displayName(displayFolder.folder)
            holder.folderIcon!!.setImageResource(folderIconProvider.getFolderIcon(displayFolder.folder.type))
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
                    val list: List<DisplayFolder> = folders.toList()
                    results.values = list
                    results.count = list.size
                } else {
                    val searchTermString = searchTerm.toString().toLowerCase(locale)
                    val words = searchTermString.split(" ").toTypedArray()
                    val wordCount = words.size
                    val newValues: MutableList<DisplayFolder> = mutableListOf()
                    for (displayFolder in folders) {
                        val displayName = folderNameFormatter.displayName(displayFolder.folder)
                        val valueText = displayName.toLowerCase(locale)
                        for (k in 0 until wordCount) {
                            if (valueText.contains(words[k])) {
                                newValues.add(displayFolder)
                                break
                            }
                        }
                    }
                    results.values = newValues.toList()
                    results.count = newValues.size
                }
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                @Suppress("UNCHECKED_CAST")
                filteredFolders = results.values as List<DisplayFolder>
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
