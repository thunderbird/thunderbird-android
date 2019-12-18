package com.fsck.k9.ui.choosefolder

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SearchView
import android.widget.TextView
import com.fsck.k9.Account
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.activity.K9ListActivity
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mailstore.DisplayFolder
import com.fsck.k9.ui.R
import com.fsck.k9.ui.folders.FolderIconProvider
import com.fsck.k9.ui.folders.FolderNameFormatter
import com.fsck.k9.ui.observeNotNull
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChooseFolderActivity : K9ListActivity() {
    private val viewModel: ChooseFolderViewModel by viewModel()
    private val folderNameFormatter: FolderNameFormatter by inject()

    private lateinit var listAdapter: FolderListAdapter
    private lateinit var account: Account
    private var currentFolder: String? = null
    private var selectFolder: String? = null
    private var messageReference: MessageReference? = null
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

        this.listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val displayFolder = listAdapter.getItem(position)
            val result = Intent()
            result.putExtra(EXTRA_ACCOUNT, account.getUuid())
            result.putExtra(EXTRA_CUR_FOLDER, currentFolder)
            val targetFolder = displayFolder.folder.serverId
            result.putExtra(EXTRA_NEW_FOLDER, targetFolder)
            if (messageReference != null) {
                result.putExtra(EXTRA_MESSAGE, messageReference!!.toIdentityString())
            }
            val displayName = folderNameFormatter.displayName(displayFolder.folder)
            result.putExtra(RESULT_FOLDER_DISPLAY_NAME, displayName)
            setResult(Activity.RESULT_OK, result)
            finish()
        }

        viewModel.getFolders(account).observeNotNull(this) { folders ->
            populateFolderList(folders)
        }
    }

    private fun populateFolderList(folders: List<DisplayFolder>) {
        val foldersToHide = if (hideCurrentFolder) setOf(currentFolder, Account.OUTBOX) else setOf(Account.OUTBOX)
        listAdapter.folders = folders.filterNot { it.folder.serverId in foldersToHide }
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
        // TODO: implement
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

    private fun setDisplayMode(aMode: FolderMode) {
        mode = aMode
        // TODO: implement
    }

    internal inner class FolderListAdapter : BaseAdapter() {
        private val folderIconProvider = FolderIconProvider(theme)

        var folders: List<DisplayFolder> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun getItem(position: Int): DisplayFolder {
            return folders[position]
        }

        override fun getItemId(position: Int): Long {
            return folders[position].folder.id
        }

        override fun getCount(): Int {
            return folders.size
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val displayFolder = getItem(position)
            val view = convertView ?: View.inflate(this@ChooseFolderActivity, R.layout.choose_folder_list_item, null)
            var holder = view.tag as? FolderViewHolder
            if (holder == null) {
                holder = FolderViewHolder()
                holder.folderName = view.findViewById(R.id.folder_name)
                holder.folderIcon = view.findViewById(R.id.folder_icon)
                holder.folderListItemLayout = view.findViewById(R.id.folder_list_item_layout)
                view.tag = holder
            }

            holder.folderName!!.text = folderNameFormatter.displayName(displayFolder.folder)
            holder.folderIcon!!.setImageResource(folderIconProvider.getFolderIcon(displayFolder.folder.type))
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
