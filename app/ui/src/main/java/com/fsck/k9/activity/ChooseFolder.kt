package com.fsck.k9.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SearchView
import android.widget.TextView
import com.fsck.k9.Account
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.activity.FolderListFilter.FolderAdapter
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.controller.MessagingListener
import com.fsck.k9.controller.SimpleMessagingListener
import com.fsck.k9.mail.Folder.FolderClass
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.ui.R
import com.fsck.k9.ui.folders.FolderIconProvider
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator

class ChooseFolder : K9ListActivity() {
    private lateinit var account: Account
    private var currentFolder: String? = null
    private var selectFolder: String? = null
    private var messageReference: MessageReference? = null
    private var listAdapter: FolderListAdapter? = null
    private val handler = ChooseFolderHandler()
    private var hideCurrentFolder = true
    private var showDisplayableOnly = false
    private var mode: FolderMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        setLayout(R.layout.list_content_simple)

        listView.isFastScrollEnabled = true
        listView.itemsCanFocus = false
        listView.choiceMode = ListView.CHOICE_MODE_NONE

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
        listAdapter = FolderListAdapter()
        setListAdapter(listAdapter)
        mode = account.getFolderTargetMode()
        MessagingController.getInstance(application).listFolders(account, false, mListener)
        this.listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val folder = listAdapter!!.getItem(position)
            val result = Intent()
            result.putExtra(EXTRA_ACCOUNT, account.getUuid())
            result.putExtra(EXTRA_CUR_FOLDER, currentFolder)
            val targetFolder = folder.serverId
            result.putExtra(EXTRA_NEW_FOLDER, targetFolder)
            if (messageReference != null) {
                result.putExtra(EXTRA_MESSAGE, messageReference!!.toIdentityString())
            }
            result.putExtra(RESULT_FOLDER_DISPLAY_NAME, folder.displayName)
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    internal inner class ChooseFolderHandler : Handler() {
        private val MSG_PROGRESS = 1
        private val MSG_SET_SELECTED_FOLDER = 2

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_PROGRESS -> {
                    setProgressBarIndeterminateVisibility(msg.arg1 != 0)
                }
                MSG_SET_SELECTED_FOLDER -> {
                    listView.setSelection(msg.arg1)
                }
            }
        }

        fun progress(progress: Boolean) {
            val msg = Message()
            msg.what = MSG_PROGRESS
            msg.arg1 = if (progress) 1 else 0
            sendMessage(msg)
        }

        fun setSelectedFolder(position: Int) {
            val msg = Message()
            msg.what = MSG_SET_SELECTED_FOLDER
            msg.arg1 = position
            sendMessage(msg)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.folder_select_option, menu)
        configureFolderSearchView(menu)
        return true
    }

    private fun configureFolderSearchView(menu: Menu) {
        val folderMenuItem = menu.findItem(R.id.filter_folders)
        val folderSearchView = folderMenuItem.actionView as SearchView
        folderSearchView.queryHint = getString(R.string.folder_list_filter_hint)
        folderSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                folderMenuItem.collapseActionView()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                listAdapter!!.filter.filter(newText)
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
        MessagingController.getInstance(application).listFolders(account, true, mListener)
    }

    private fun setDisplayMode(aMode: FolderMode) {
        mode = aMode
        //re-populate the list
        MessagingController.getInstance(application).listFolders(account, false, mListener)
    }

    private val mListener: MessagingListener = object : SimpleMessagingListener() {
        override fun listFoldersStarted(account: Account) {
            if (account != this@ChooseFolder.account) {
                return
            }
            handler.progress(true)
        }

        override fun listFoldersFailed(account: Account, message: String) {
            if (account != this@ChooseFolder.account) {
                return
            }
            handler.progress(false)
        }

        override fun listFoldersFinished(account: Account) {
            if (account != this@ChooseFolder.account) {
                return
            }
            handler.progress(false)
        }

        override fun listFolders(account: Account, folders: List<LocalFolder>) {
            if (account != this@ChooseFolder.account) {
                return
            }
            val aMode = mode
            val newFolders: MutableList<FolderInfoHolder> = ArrayList()
            val topFolders: MutableList<FolderInfoHolder> = ArrayList()
            for (folder in folders) {
                val serverId = folder.serverId
                if (hideCurrentFolder && serverId == currentFolder) {
                    continue
                }
                if (account.outboxFolder == serverId) {
                    continue
                }
                val fMode = folder.displayClass
                if (aMode == FolderMode.FIRST_CLASS &&
                    fMode != FolderClass.FIRST_CLASS || aMode == FolderMode.FIRST_AND_SECOND_CLASS && fMode != FolderClass.FIRST_CLASS && fMode != FolderClass.SECOND_CLASS || aMode == FolderMode.NOT_SECOND_CLASS &&
                    fMode == FolderClass.SECOND_CLASS) {
                    continue
                }
                val folderDisplayData = FolderInfoHolder(folder, account)
                if (folder.isInTopGroup) {
                    topFolders.add(folderDisplayData)
                } else {
                    newFolders.add(folderDisplayData)
                }
            }
            val comparator = Comparator<FolderInfoHolder> { lhs, rhs ->
                val result = lhs.displayName.compareTo(rhs.displayName, ignoreCase = true)
                if (result != 0) result else lhs.displayName.compareTo(rhs.displayName)
            }
            Collections.sort(topFolders, comparator)
            Collections.sort(newFolders, comparator)
            val folderList: MutableList<FolderInfoHolder> =
                ArrayList(newFolders.size + topFolders.size)
            folderList.addAll(topFolders)
            folderList.addAll(newFolders)
            var selectedFolder = -1
            /*
             * We're not allowed to change the adapter from a background thread, so we collect the
             * folder names and update the adapter in the UI thread (see finally block).
             */try {
                var position = 0
                for (folder in folderList) {
                    if (selectFolder != null) { /*
                         * Never select EXTRA_CUR_FOLDER (mFolder) if EXTRA_SEL_FOLDER
                         * (mSelectedFolder) was provided.
                         */
                        if (folder.serverId == selectFolder) {
                            selectedFolder = position
                            break
                        }
                    } else if (folder.serverId == currentFolder) {
                        selectedFolder = position
                        break
                    }
                    position++
                }
            } finally {
                runOnUiThread {
                    // Now we're in the UI-thread, we can safely change the contents of the adapter.
                    listAdapter!!.setFolders(folderList)
                }
            }
            if (selectedFolder != -1) {
                handler.setSelectedFolder(selectedFolder)
            }
        }
    }

    internal inner class FolderListAdapter : BaseAdapter(), Filterable, FolderAdapter {
        private var mFolders: List<FolderInfoHolder> = emptyList()
        private var mFilteredFolders: List<FolderInfoHolder> = emptyList()
        private var mFilter: Filter = FolderListFilter(this, mFolders)
        private val folderIconProvider = FolderIconProvider(theme)
        private var filterText: CharSequence? = null
        fun getItem(position: Long): FolderInfoHolder {
            return getItem(position.toInt())
        }

        override fun getItem(position: Int): FolderInfoHolder {
            return mFilteredFolders[position]
        }

        override fun getItemId(position: Int): Long {
            return mFilteredFolders[position].folder.databaseId
        }

        override fun getCount(): Int {
            return mFilteredFolders.size
        }

        override fun isEnabled(item: Int): Boolean {
            return true
        }

        override fun areAllItemsEnabled(): Boolean {
            return true
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val folder = getItem(position)
            val view = convertView ?: View.inflate(this@ChooseFolder, R.layout.choose_folder_list_item, null)
            var holder = view.tag as? FolderViewHolder
            if (holder == null) {
                holder = FolderViewHolder()
                holder.folderName = view.findViewById(R.id.folder_name)
                holder.folderIcon = view.findViewById(R.id.folder_icon)
                holder.folderListItemLayout = view.findViewById(R.id.folder_list_item_layout)
                view.tag = holder
            }

            holder.folderName!!.text = folder.displayName
            holder.folderIcon!!.setImageResource(folderIconProvider.getFolderIcon(folder.folder.type))
            if (K9.isWrapFolderNames) {
                holder.folderName!!.ellipsize = null
                holder.folderName!!.isSingleLine = false
            } else {
                holder.folderName!!.ellipsize = TextUtils.TruncateAt.START
                holder.folderName!!.isSingleLine = true
            }
            return view
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun getFilter(): Filter {
            return mFilter
        }

        override fun setFilteredFolders(filterText: CharSequence?, folders: List<FolderInfoHolder>) {
            this.filterText = filterText
            mFilteredFolders = folders
            notifyDataSetChanged()
        }

        fun setFolders(folders: List<FolderInfoHolder>) {
            mFolders = folders
            mFilter = FolderListFilter(this, folders)
            mFilter.filter(filterText)
        }
    }

    internal class FolderViewHolder {
        var folderName: TextView? = null
        var folderIcon: ImageView? = null
        var folderListItemLayout: LinearLayout? = null
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
