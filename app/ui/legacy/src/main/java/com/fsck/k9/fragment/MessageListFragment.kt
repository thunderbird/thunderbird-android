package com.fsck.k9.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.fsck.k9.Account
import com.fsck.k9.Account.Expunge
import com.fsck.k9.Account.SortType
import com.fsck.k9.Clock
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.activity.FolderInfoHolder
import com.fsck.k9.activity.misc.ContactPicture
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.controller.SimpleMessagingListener
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener
import com.fsck.k9.fragment.MessageListFragment.MessageListFragmentListener.Companion.MAX_PROGRESS
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchAccount
import com.fsck.k9.search.getAccounts
import com.fsck.k9.ui.R
import com.fsck.k9.ui.choosefolder.ChooseFolderActivity
import com.fsck.k9.ui.folders.FolderNameFormatter
import com.fsck.k9.ui.folders.FolderNameFormatterFactory
import com.fsck.k9.ui.helper.RelativeDateTimeFormatter
import com.fsck.k9.ui.messagelist.MessageListAppearance
import com.fsck.k9.ui.messagelist.MessageListConfig
import com.fsck.k9.ui.messagelist.MessageListInfo
import com.fsck.k9.ui.messagelist.MessageListItem
import com.fsck.k9.ui.messagelist.MessageListViewModel
import java.util.HashSet
import java.util.concurrent.Future
import net.jcip.annotations.GuardedBy
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MessageListFragment :
    Fragment(),
    OnItemClickListener,
    OnItemLongClickListener,
    ConfirmationDialogFragmentListener,
    MessageListItemActionListener {

    private val viewModel: MessageListViewModel by viewModel()
    private val sortTypeToastProvider: SortTypeToastProvider by inject()
    private val folderNameFormatterFactory: FolderNameFormatterFactory by inject()
    private val folderNameFormatter: FolderNameFormatter by lazy { folderNameFormatterFactory.create(requireContext()) }
    private val messagingController: MessagingController by inject()
    private val preferences: Preferences by inject()
    private val clock: Clock by inject()

    private val handler = MessageListHandler(this)
    private val activityListener = MessageListActivityListener()
    private val actionModeCallback = ActionModeCallback()

    private lateinit var fragmentListener: MessageListFragmentListener

    private lateinit var listView: ListView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var adapter: MessageListAdapter
    private var footerView: View? = null

    private var savedListState: Parcelable? = null

    private lateinit var accountUuids: Array<String>
    private var account: Account? = null
    private var currentFolder: FolderInfoHolder? = null
    private var remoteSearchFuture: Future<*>? = null
    private var extraSearchResults: List<String>? = null
    private var threadTitle: String? = null
    private var allAccounts = false
    private var sortType = SortType.SORT_DATE
    private var sortAscending = true
    private var sortDateAscending = false
    private var selectedCount = 0
    private var selected: MutableSet<Long> = HashSet()
    private var actionMode: ActionMode? = null
    private var hasConnectivity: Boolean? = null

    /**
     * Relevant messages for the current context when we have to remember the chosen messages
     * between user interactions (e.g. selecting a folder for move operation).
     */
    private var activeMessages: List<MessageReference>? = null
    private var showingThreadedList = false
    private var isThreadDisplay = false
    private var activeMessage: MessageReference? = null

    var isLoadFinished = false
        private set
    lateinit var localSearch: LocalSearch
        private set
    var isSingleAccountMode = false
        private set
    var isSingleFolderMode = false
        private set
    var isRemoteSearch = false
        private set

    private val isUnifiedInbox: Boolean
        get() = localSearch.id == SearchAccount.UNIFIED_INBOX

    private val isNewMessagesView: Boolean
        get() = localSearch.id == SearchAccount.NEW_MESSAGES

    /**
     * `true` after [.onCreate] was executed. Used in [.updateTitle] to
     * make sure we don't access member variables before initialization is complete.
     */
    var isInitialized = false
        private set

    override fun onAttach(context: Context) {
        super.onAttach(context)

        fragmentListener = try {
            context as MessageListFragmentListener
        } catch (e: ClassCastException) {
            error("${context.javaClass} must implement MessageListFragmentListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        restoreInstanceState(savedInstanceState)
        decodeArguments() ?: return

        viewModel.getMessageListLiveData().observe(this) { messageListInfo: MessageListInfo ->
            setMessageList(messageListInfo)
        }

        isInitialized = true
    }

    private fun restoreInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return

        activeMessages = savedInstanceState.getStringArray(STATE_ACTIVE_MESSAGES)?.map { MessageReference.parse(it)!! }
        restoreSelectedMessages(savedInstanceState)
        isRemoteSearch = savedInstanceState.getBoolean(STATE_REMOTE_SEARCH_PERFORMED)
        savedListState = savedInstanceState.getParcelable(STATE_MESSAGE_LIST)
        val messageReferenceString = savedInstanceState.getString(STATE_ACTIVE_MESSAGE)
        activeMessage = MessageReference.parse(messageReferenceString)
    }

    private fun restoreSelectedMessages(savedInstanceState: Bundle) {
        val selectedIds = savedInstanceState.getLongArray(STATE_SELECTED_MESSAGES) ?: return
        for (id in selectedIds) {
            selected.add(id)
        }
    }

    fun restoreListState(savedListState: Parcelable) {
        listView.onRestoreInstanceState(savedListState)
    }

    private fun decodeArguments(): MessageListFragment? {
        val arguments = requireArguments()
        showingThreadedList = arguments.getBoolean(ARG_THREADED_LIST, false)
        isThreadDisplay = arguments.getBoolean(ARG_IS_THREAD_DISPLAY, false)
        localSearch = arguments.getParcelable(ARG_SEARCH)!!

        allAccounts = localSearch.searchAllAccounts()
        val searchAccounts = localSearch.getAccounts(preferences)
        if (searchAccounts.size == 1) {
            isSingleAccountMode = true
            val singleAccount = searchAccounts[0]
            account = singleAccount
            accountUuids = arrayOf(singleAccount.uuid)
        } else {
            isSingleAccountMode = false
            account = null
            accountUuids = searchAccounts.map { it.uuid }.toTypedArray()
        }

        isSingleFolderMode = false
        if (isSingleAccountMode && localSearch.folderIds.size == 1) {
            try {
                val folderId = localSearch.folderIds[0]
                currentFolder = getFolderInfoHolder(folderId, account!!)
                isSingleFolderMode = true
            } catch (e: MessagingException) {
                fragmentListener.onFolderNotFoundError()
                return null
            }
        }

        return this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.message_list_fragment, container, false).apply {
            initializeSwipeRefreshLayout(this)
            initializeListView(this)
        }
    }

    private fun initializeSwipeRefreshLayout(view: View) {
        swipeRefreshLayout = view.findViewById(R.id.swiperefresh)

        if (isRemoteSearchAllowed) {
            swipeRefreshLayout.setOnRefreshListener { onRemoteSearchRequested() }
        } else if (isCheckMailSupported) {
            swipeRefreshLayout.setOnRefreshListener { checkMail() }
        }

        // Disable pull-to-refresh until the message list has been loaded
        swipeRefreshLayout.isEnabled = false
    }

    private fun initializeListView(view: View) {
        listView = view.findViewById(R.id.message_list)
        with(listView) {
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            isLongClickable = true
            isFastScrollEnabled = true
            isVerticalFadingEdgeEnabled = false
            isScrollingCacheEnabled = false
            onItemClickListener = this@MessageListFragment
            onItemLongClickListener = this@MessageListFragment
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initializeMessageList()

        // This needs to be done before loading the message list below
        initializeSortSettings()
        loadMessageList()
    }

    private fun initializeMessageList() {
        adapter = MessageListAdapter(
            context = requireContext(),
            theme = requireActivity().theme,
            res = resources,
            layoutInflater = layoutInflater,
            contactsPictureLoader = ContactPicture.getContactPictureLoader(),
            listItemListener = this,
            appearance = messageListAppearance,
            relativeDateTimeFormatter = RelativeDateTimeFormatter(requireContext(), clock)
        )

        adapter.activeMessage = activeMessage

        if (isSingleFolderMode) {
            listView.addFooterView(getFooterView(listView))
            updateFooter(null)
        }

        listView.adapter = adapter
    }

    private fun initializeSortSettings() {
        if (isSingleAccountMode) {
            val account = this.account!!
            sortType = account.sortType
            sortAscending = account.isSortAscending(sortType)
            sortDateAscending = account.isSortAscending(SortType.SORT_DATE)
        } else {
            sortType = K9.sortType
            sortAscending = K9.isSortAscending(sortType)
            sortDateAscending = K9.isSortAscending(SortType.SORT_DATE)
        }
    }

    private fun loadMessageList() {
        val config = MessageListConfig(
            localSearch,
            showingThreadedList,
            sortType,
            sortAscending,
            sortDateAscending,
            activeMessage
        )
        viewModel.loadMessageList(config)
    }

    fun folderLoading(folderId: Long, loading: Boolean) {
        currentFolder?.let {
            if (it.databaseId == folderId) {
                it.loading = loading
            }
        }

        updateFooterView()
    }

    fun updateTitle() {
        if (!isInitialized) return

        setWindowTitle()

        if (!localSearch.isManualSearch) {
            setWindowProgress()
        }
    }

    private fun setWindowProgress() {
        var level = 0
        if (currentFolder?.loading == true) {
            val folderTotal = activityListener.getFolderTotal()
            if (folderTotal > 0) {
                level = (MAX_PROGRESS * activityListener.getFolderCompleted() / folderTotal).coerceAtMost(MAX_PROGRESS)
            }
        }

        fragmentListener.setMessageListProgress(level)
    }

    private fun setWindowTitle() {
        val title = when {
            isUnifiedInbox -> getString(R.string.integrated_inbox_title)
            isNewMessagesView -> getString(R.string.new_messages_title)
            isManualSearch -> getString(R.string.search_results)
            isThreadDisplay -> threadTitle ?: ""
            isSingleFolderMode -> currentFolder!!.displayName
            else -> ""
        }

        val subtitle = account.let { account ->
            if (account == null || isUnifiedInbox || preferences.accounts.size == 1) {
                null
            } else {
                account.displayName
            }
        }

        fragmentListener.setMessageListTitle(title, subtitle)
    }

    fun progress(progress: Boolean) {
        if (!progress) {
            swipeRefreshLayout.isRefreshing = false
        }

        fragmentListener.setMessageListProgressEnabled(progress)
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        if (view === footerView) {
            handleFooterClick()
        } else {
            handleListItemClick(position)
        }
    }

    private fun handleFooterClick() {
        val currentFolder = this.currentFolder ?: return

        if (currentFolder.moreMessages && !localSearch.isManualSearch) {
            val folderId = currentFolder.databaseId
            messagingController.loadMoreMessages(account, folderId, null)
        } else if (isRemoteSearch) {
            val additionalSearchResults = extraSearchResults ?: return
            if (additionalSearchResults.isEmpty()) return

            val loadSearchResults: List<String>

            val limit = account!!.remoteSearchNumResults
            if (limit in 1 until additionalSearchResults.size) {
                extraSearchResults = additionalSearchResults.subList(limit, additionalSearchResults.size)
                loadSearchResults = additionalSearchResults.subList(0, limit)
            } else {
                extraSearchResults = null
                loadSearchResults = additionalSearchResults
                updateFooter(null)
            }

            messagingController.loadSearchResults(
                account,
                currentFolder.databaseId,
                loadSearchResults,
                activityListener
            )
        }
    }

    private fun handleListItemClick(position: Int) {
        if (selectedCount > 0) {
            toggleMessageSelect(position)
        } else {
            val adapterPosition = listViewToAdapterPosition(position)
            val messageListItem = adapter.getItem(adapterPosition)

            if (showingThreadedList && messageListItem.threadCount > 1) {
                fragmentListener.showThread(messageListItem.account, messageListItem.threadRoot)
            } else {
                openMessageAtPosition(adapterPosition)
            }
        }
    }

    override fun onItemLongClick(parent: AdapterView<*>?, view: View, position: Int, id: Long): Boolean {
        if (view === footerView) return false

        toggleMessageSelect(position)
        return true
    }

    override fun onDestroyView() {
        if (isNewMessagesView && !requireActivity().isChangingConfigurations) {
            messagingController.clearNewMessages(account)
        }

        savedListState = listView.onSaveInstanceState()
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        saveListState(outState)
        outState.putLongArray(STATE_SELECTED_MESSAGES, selected.toLongArray())
        outState.putBoolean(STATE_REMOTE_SEARCH_PERFORMED, isRemoteSearch)
        outState.putStringArray(
            STATE_ACTIVE_MESSAGES,
            activeMessages?.map(MessageReference::toIdentityString)?.toTypedArray()
        )
        if (activeMessage != null) {
            outState.putString(STATE_ACTIVE_MESSAGE, activeMessage!!.toIdentityString())
        }
    }

    private fun saveListState(outState: Bundle) {
        if (savedListState != null) {
            // The previously saved state was never restored, so just use that.
            outState.putParcelable(STATE_MESSAGE_LIST, savedListState)
        } else {
            outState.putParcelable(STATE_MESSAGE_LIST, listView.onSaveInstanceState())
        }
    }

    private val messageListAppearance: MessageListAppearance
        get() = MessageListAppearance(
            fontSizes = K9.fontSizes,
            previewLines = K9.messageListPreviewLines,
            stars = !isOutbox && K9.isShowMessageListStars,
            senderAboveSubject = K9.isMessageListSenderAboveSubject,
            showContactPicture = K9.isShowContactPicture,
            showingThreadedList = showingThreadedList,
            backGroundAsReadIndicator = K9.isUseBackgroundAsUnreadIndicator,
            showAccountChip = !isSingleAccountMode
        )

    private fun getFolderInfoHolder(folderId: Long, account: Account): FolderInfoHolder {
        val localFolder = MlfUtils.getOpenFolder(folderId, account)
        return FolderInfoHolder(folderNameFormatter, localFolder, account)
    }

    override fun onResume() {
        super.onResume()

        if (hasConnectivity == null) {
            hasConnectivity = Utility.hasConnectivity(requireActivity().application)
        }

        messagingController.addListener(activityListener)

        updateTitle()
    }

    override fun onPause() {
        super.onPause()

        messagingController.removeListener(activityListener)
    }

    fun goBack() {
        fragmentListener.goBack()
    }

    fun onCompose() {
        if (!isSingleAccountMode) {
            fragmentListener.onCompose(null)
        } else {
            fragmentListener.onCompose(account)
        }
    }

    fun changeSort(sortType: SortType) {
        val sortAscending = if (this.sortType == sortType) !sortAscending else null
        changeSort(sortType, sortAscending)
    }

    private fun onRemoteSearchRequested() {
        val searchAccount = account!!.uuid
        val folderId = currentFolder!!.databaseId
        val queryString = localSearch.remoteSearchArguments

        isRemoteSearch = true
        swipeRefreshLayout.isEnabled = false

        remoteSearchFuture = messagingController.searchRemoteMessages(
            searchAccount,
            folderId,
            queryString,
            null,
            null,
            activityListener
        )

        fragmentListener.remoteSearchStarted()
    }

    /**
     * Change the sort type and sort order used for the message list.
     *
     * @param sortType Specifies which field to use for sorting the message list.
     * @param sortAscending Specifies the sort order. If this argument is `null` the default search order for the
     *   sort type is used.
     */
    // FIXME: Don't save the changes in the UI thread
    private fun changeSort(sortType: SortType, sortAscending: Boolean?) {
        this.sortType = sortType

        val account = account
        if (account != null) {
            account.sortType = this.sortType
            if (sortAscending == null) {
                this.sortAscending = account.isSortAscending(this.sortType)
            } else {
                this.sortAscending = sortAscending
            }
            account.setSortAscending(this.sortType, this.sortAscending)
            sortDateAscending = account.isSortAscending(SortType.SORT_DATE)

            preferences.saveAccount(account)
        } else {
            K9.sortType = this.sortType
            if (sortAscending == null) {
                this.sortAscending = K9.isSortAscending(this.sortType)
            } else {
                this.sortAscending = sortAscending
            }
            K9.setSortAscending(this.sortType, this.sortAscending)
            sortDateAscending = K9.isSortAscending(SortType.SORT_DATE)

            K9.saveSettingsAsync()
        }

        reSort()
    }

    private fun reSort() {
        val toastString = sortTypeToastProvider.getToast(sortType, sortAscending)
        Toast.makeText(activity, toastString, Toast.LENGTH_SHORT).show()
        loadMessageList()
    }

    fun onCycleSort() {
        val sortTypes = SortType.values()
        val currentIndex = sortTypes.indexOf(sortType)
        val newIndex = if (currentIndex == sortTypes.lastIndex) 0 else currentIndex + 1
        val nextSortType = sortTypes[newIndex]
        changeSort(nextSortType)
    }

    private fun onDelete(messages: List<MessageReference>) {
        if (K9.isConfirmDelete) {
            // remember the message selection for #onCreateDialog(int)
            activeMessages = messages
            showDialog(R.id.dialog_confirm_delete)
        } else {
            onDeleteConfirmed(messages)
        }
    }

    private fun onDeleteConfirmed(messages: List<MessageReference>) {
        if (showingThreadedList) {
            messagingController.deleteThreads(messages)
        } else {
            messagingController.deleteMessages(messages)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            ACTIVITY_CHOOSE_FOLDER_MOVE,
            ACTIVITY_CHOOSE_FOLDER_COPY -> {
                if (data == null) return

                val destinationFolderId = data.getLongExtra(ChooseFolderActivity.RESULT_SELECTED_FOLDER_ID, -1L)
                val messages = activeMessages!!
                if (destinationFolderId != -1L) {
                    activeMessages = null

                    if (messages.isNotEmpty()) {
                        MlfUtils.setLastSelectedFolder(preferences, messages, destinationFolderId)
                    }

                    when (requestCode) {
                        ACTIVITY_CHOOSE_FOLDER_MOVE -> move(messages, destinationFolderId)
                        ACTIVITY_CHOOSE_FOLDER_COPY -> copy(messages, destinationFolderId)
                    }
                }
            }
        }
    }

    fun onExpunge() {
        currentFolder?.let { folderInfoHolder ->
            onExpunge(account, folderInfoHolder.databaseId)
        }
    }

    private fun onExpunge(account: Account?, folderId: Long) {
        messagingController.expunge(account, folderId)
    }

    fun onEmptyTrash() {
        if (isShowingTrashFolder) {
            showDialog(R.id.dialog_confirm_empty_trash)
        }
    }

    val isShowingTrashFolder: Boolean
        get() {
            if (!isSingleFolderMode) return false
            return currentFolder!!.databaseId == account!!.trashFolderId
        }

    private fun showDialog(dialogId: Int) {
        val dialogFragment = when (dialogId) {
            R.id.dialog_confirm_spam -> {
                val title = getString(R.string.dialog_confirm_spam_title)
                val selectionSize = activeMessages!!.size
                val message = resources.getQuantityString(
                    R.plurals.dialog_confirm_spam_message,
                    selectionSize,
                    selectionSize
                )
                val confirmText = getString(R.string.dialog_confirm_spam_confirm_button)
                val cancelText = getString(R.string.dialog_confirm_spam_cancel_button)
                ConfirmationDialogFragment.newInstance(dialogId, title, message, confirmText, cancelText)
            }
            R.id.dialog_confirm_delete -> {
                val title = getString(R.string.dialog_confirm_delete_title)
                val selectionSize = activeMessages!!.size
                val message = resources.getQuantityString(
                    R.plurals.dialog_confirm_delete_messages,
                    selectionSize,
                    selectionSize
                )
                val confirmText = getString(R.string.dialog_confirm_delete_confirm_button)
                val cancelText = getString(R.string.dialog_confirm_delete_cancel_button)
                ConfirmationDialogFragment.newInstance(dialogId, title, message, confirmText, cancelText)
            }
            R.id.dialog_confirm_mark_all_as_read -> {
                val title = getString(R.string.dialog_confirm_mark_all_as_read_title)
                val message = getString(R.string.dialog_confirm_mark_all_as_read_message)
                val confirmText = getString(R.string.dialog_confirm_mark_all_as_read_confirm_button)
                val cancelText = getString(R.string.dialog_confirm_mark_all_as_read_cancel_button)
                ConfirmationDialogFragment.newInstance(dialogId, title, message, confirmText, cancelText)
            }
            R.id.dialog_confirm_empty_trash -> {
                val title = getString(R.string.dialog_confirm_empty_trash_title)
                val message = getString(R.string.dialog_confirm_empty_trash_message)
                val confirmText = getString(R.string.dialog_confirm_delete_confirm_button)
                val cancelText = getString(R.string.dialog_confirm_delete_cancel_button)
                ConfirmationDialogFragment.newInstance(dialogId, title, message, confirmText, cancelText)
            }
            else -> {
                throw RuntimeException("Called showDialog(int) with unknown dialog id.")
            }
        }

        dialogFragment.setTargetFragment(this, dialogId)
        dialogFragment.show(parentFragmentManager, getDialogTag(dialogId))
    }

    private fun getDialogTag(dialogId: Int): String {
        return "dialog-$dialogId"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.set_sort_date) {
            changeSort(SortType.SORT_DATE)
            return true
        } else if (id == R.id.set_sort_arrival) {
            changeSort(SortType.SORT_ARRIVAL)
            return true
        } else if (id == R.id.set_sort_subject) {
            changeSort(SortType.SORT_SUBJECT)
            return true
        } else if (id == R.id.set_sort_sender) {
            changeSort(SortType.SORT_SENDER)
            return true
        } else if (id == R.id.set_sort_flag) {
            changeSort(SortType.SORT_FLAGGED)
            return true
        } else if (id == R.id.set_sort_unread) {
            changeSort(SortType.SORT_UNREAD)
            return true
        } else if (id == R.id.set_sort_attach) {
            changeSort(SortType.SORT_ATTACHMENT)
            return true
        } else if (id == R.id.select_all) {
            selectAll()
            return true
        }

        if (!isSingleAccountMode) {
            // None of the options after this point are "safe" for search results
            // TODO: This is not true for "unread" and "starred" searches in regular folders
            return false
        }

        if (id == R.id.send_messages) {
            onSendPendingMessages()
            return true
        } else if (id == R.id.expunge) {
            currentFolder?.let { folderInfoHolder ->
                onExpunge(account, folderInfoHolder.databaseId)
            }
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    fun onSendPendingMessages() {
        messagingController.sendPendingMessages(account, null)
    }

    private fun listViewToAdapterPosition(position: Int): Int {
        return if (position in 0 until adapter.count) position else AdapterView.INVALID_POSITION
    }

    private fun adapterToListViewPosition(position: Int): Int {
        return if (position in 0 until adapter.count) position else AdapterView.INVALID_POSITION
    }

    private fun getFooterView(parent: ViewGroup?): View? {
        return footerView ?: createFooterView(parent).also { footerView = it }
    }

    private fun createFooterView(parent: ViewGroup?): View {
        return layoutInflater.inflate(R.layout.message_list_item_footer, parent, false).apply {
            tag = FooterViewHolder(this)
        }
    }

    private fun updateFooterView() {
        val currentFolder = this.currentFolder
        val account = this.account

        if (localSearch.isManualSearch || currentFolder == null || account == null) {
            updateFooter(null)
            return
        }

        val footerText = if (currentFolder.loading) {
            getString(R.string.status_loading_more)
        } else if (!currentFolder.moreMessages) {
            null
        } else if (account.displayCount == 0) {
            getString(R.string.message_list_load_more_messages_action)
        } else {
            getString(R.string.load_more_messages_fmt, account.displayCount)
        }

        updateFooter(footerText)
    }

    fun updateFooter(text: String?) {
        val footerView = this.footerView ?: return

        val shouldHideFooter = text == null
        if (shouldHideFooter) {
            listView.removeFooterView(footerView)
        } else {
            val isFooterViewAddedToListView = listView.footerViewsCount > 0
            if (!isFooterViewAddedToListView) {
                listView.addFooterView(footerView)
            }
        }

        val holder = footerView.tag as FooterViewHolder
        holder.main.text = text
    }

    private fun setSelectionState(selected: Boolean) {
        if (selected) {
            if (adapter.count == 0) {
                // Nothing to do if there are no messages
                return
            }

            selectedCount = 0
            for (i in 0 until adapter.count) {
                val messageListItem = adapter.getItem(i)
                this.selected.add(messageListItem.uniqueId)

                if (showingThreadedList) {
                    selectedCount += messageListItem.threadCount.coerceAtLeast(1)
                } else {
                    selectedCount++
                }
            }

            if (actionMode == null) {
                startAndPrepareActionMode()
            }

            computeBatchDirection()
            updateActionMode()
            computeSelectAllVisibility()
        } else {
            this.selected.clear()
            selectedCount = 0

            actionMode?.finish()
            actionMode = null
        }

        adapter.notifyDataSetChanged()
    }

    private fun toggleMessageSelect(listViewPosition: Int) {
        val adapterPosition = listViewToAdapterPosition(listViewPosition)
        if (adapterPosition == AdapterView.INVALID_POSITION) return

        val messageListItem = adapter.getItem(adapterPosition)
        toggleMessageSelect(messageListItem)
    }

    private fun toggleMessageSelect(messageListItem: MessageListItem) {
        val uniqueId = messageListItem.uniqueId
        val selected = selected.contains(uniqueId)
        if (!selected) {
            this.selected.add(uniqueId)
        } else {
            this.selected.remove(uniqueId)
        }

        var selectedCountDelta = 1
        if (showingThreadedList) {
            val threadCount = messageListItem.threadCount
            if (threadCount > 1) {
                selectedCountDelta = threadCount
            }
        }

        if (actionMode != null) {
            if (selected && selectedCount - selectedCountDelta == 0) {
                actionMode?.finish()
                actionMode = null
                return
            }
        } else {
            startAndPrepareActionMode()
        }

        if (selected) {
            selectedCount -= selectedCountDelta
        } else {
            selectedCount += selectedCountDelta
        }

        computeBatchDirection()
        updateActionMode()
        computeSelectAllVisibility()

        adapter.notifyDataSetChanged()
    }

    override fun onToggleMessageSelection(item: MessageListItem) {
        toggleMessageSelect(item)
    }

    override fun onToggleMessageFlag(item: MessageListItem) {
        setFlag(item, Flag.FLAGGED, !item.isStarred)
    }

    private fun updateActionMode() {
        val actionMode = actionMode ?: error("actionMode == null")
        actionMode.title = getString(R.string.actionbar_selected, selectedCount)
        actionMode.invalidate()
    }

    private fun computeSelectAllVisibility() {
        actionModeCallback.showSelectAll(selected.size != adapter.count)
    }

    private fun computeBatchDirection() {
        var isBatchFlag = false
        var isBatchRead = false
        for (i in 0 until adapter.count) {
            val messageListItem = adapter.getItem(i)
            if (selected.contains(messageListItem.uniqueId)) {
                if (!messageListItem.isStarred) {
                    isBatchFlag = true
                }

                if (!messageListItem.isRead) {
                    isBatchRead = true
                }

                if (isBatchFlag && isBatchRead) {
                    break
                }
            }
        }

        actionModeCallback.showMarkAsRead(isBatchRead)
        actionModeCallback.showFlag(isBatchFlag)
    }

    private fun setFlag(messageListItem: MessageListItem, flag: Flag, newState: Boolean) {
        val account = messageListItem.account
        if (showingThreadedList && messageListItem.threadCount > 1) {
            val threadRootId = messageListItem.threadRoot
            messagingController.setFlagForThreads(account, listOf(threadRootId), flag, newState)
        } else {
            val messageId = messageListItem.databaseId
            messagingController.setFlag(account, listOf(messageId), flag, newState)
        }

        computeBatchDirection()
    }

    private fun setFlagForSelected(flag: Flag, newState: Boolean) {
        if (selected.isEmpty()) return

        val messageMap: MutableMap<Account, MutableList<Long>> = mutableMapOf()
        val threadMap: MutableMap<Account, MutableList<Long>> = mutableMapOf()
        val accounts: MutableSet<Account> = mutableSetOf()

        for (position in 0 until adapter.count) {
            val messageListItem = adapter.getItem(position)
            val account = messageListItem.account
            if (messageListItem.uniqueId in selected) {
                accounts.add(account)

                if (showingThreadedList && messageListItem.threadCount > 1) {
                    val threadRootIdList = threadMap.getOrPut(account) { mutableListOf() }
                    threadRootIdList.add(messageListItem.threadRoot)
                } else {
                    val messageIdList = messageMap.getOrPut(account) { mutableListOf() }
                    messageIdList.add(messageListItem.databaseId)
                }
            }
        }

        for (account in accounts) {
            messageMap[account]?.let { messageIds ->
                messagingController.setFlag(account, messageIds, flag, newState)
            }

            threadMap[account]?.let { threadRootIds ->
                messagingController.setFlagForThreads(account, threadRootIds, flag, newState)
            }
        }

        computeBatchDirection()
    }

    private fun onMove(message: MessageReference) {
        onMove(listOf(message))
    }

    private fun onMove(messages: List<MessageReference>) {
        if (!checkCopyOrMovePossible(messages, FolderOperation.MOVE)) return

        val folderId = when {
            isThreadDisplay -> messages.first().folderId
            isSingleFolderMode -> currentFolder!!.databaseId
            else -> null
        }

        displayFolderChoice(
            operation = FolderOperation.MOVE,
            requestCode = ACTIVITY_CHOOSE_FOLDER_MOVE,
            sourceFolderId = folderId,
            accountUuid = messages.first().accountUuid,
            lastSelectedFolderId = null,
            messages = messages
        )
    }

    private fun onCopy(message: MessageReference) {
        onCopy(listOf(message))
    }

    private fun onCopy(messages: List<MessageReference>) {
        if (!checkCopyOrMovePossible(messages, FolderOperation.COPY)) return

        val folderId = when {
            isThreadDisplay -> messages.first().folderId
            isSingleFolderMode -> currentFolder!!.databaseId
            else -> null
        }

        displayFolderChoice(
            operation = FolderOperation.COPY,
            requestCode = ACTIVITY_CHOOSE_FOLDER_COPY,
            sourceFolderId = folderId,
            accountUuid = messages.first().accountUuid,
            lastSelectedFolderId = null,
            messages = messages
        )
    }

    private fun displayFolderChoice(
        operation: FolderOperation,
        requestCode: Int,
        sourceFolderId: Long?,
        accountUuid: String,
        lastSelectedFolderId: Long?,
        messages: List<MessageReference>
    ) {
        val action = when (operation) {
            FolderOperation.COPY -> ChooseFolderActivity.Action.COPY
            FolderOperation.MOVE -> ChooseFolderActivity.Action.MOVE
        }
        val intent = ChooseFolderActivity.buildLaunchIntent(
            context = requireContext(),
            action = action,
            accountUuid = accountUuid,
            currentFolderId = sourceFolderId,
            scrollToFolderId = lastSelectedFolderId,
            showDisplayableOnly = false,
            messageReference = null
        )

        // remember the selected messages for #onActivityResult
        activeMessages = messages

        startActivityForResult(intent, requestCode)
    }

    private fun onArchive(message: MessageReference) {
        onArchive(listOf(message))
    }

    private fun onArchive(messages: List<MessageReference>) {
        for ((account, messagesInAccount) in groupMessagesByAccount(messages)) {
            account.archiveFolderId?.let { archiveFolderId ->
                move(messagesInAccount, archiveFolderId)
            }
        }
    }

    private fun groupMessagesByAccount(messages: List<MessageReference>): Map<Account, List<MessageReference>> {
        return messages.groupBy { preferences.getAccount(it.accountUuid)!! }
    }

    private fun onSpam(messages: List<MessageReference>) {
        if (K9.isConfirmSpam) {
            // remember the message selection for #onCreateDialog(int)
            activeMessages = messages
            showDialog(R.id.dialog_confirm_spam)
        } else {
            onSpamConfirmed(messages)
        }
    }

    private fun onSpamConfirmed(messages: List<MessageReference>) {
        for ((account, messagesInAccount) in groupMessagesByAccount(messages)) {
            account.spamFolderId?.let { spamFolderId ->
                move(messagesInAccount, spamFolderId)
            }
        }
    }

    private fun checkCopyOrMovePossible(messages: List<MessageReference>, operation: FolderOperation): Boolean {
        if (messages.isEmpty()) return false

        val account = preferences.getAccount(messages.first().accountUuid)
        if (operation == FolderOperation.MOVE && !messagingController.isMoveCapable(account) ||
            operation == FolderOperation.COPY && !messagingController.isCopyCapable(account)
        ) {
            return false
        }

        for (message in messages) {
            if (operation == FolderOperation.MOVE && !messagingController.isMoveCapable(message) ||
                operation == FolderOperation.COPY && !messagingController.isCopyCapable(message)
            ) {
                val toast = Toast.makeText(
                    activity, R.string.move_copy_cannot_copy_unsynced_message,
                    Toast.LENGTH_LONG
                )
                toast.show()
                return false
            }
        }

        return true
    }

    private fun copy(messages: List<MessageReference>, folderId: Long) {
        copyOrMove(messages, folderId, FolderOperation.COPY)
    }

    private fun move(messages: List<MessageReference>, folderId: Long) {
        copyOrMove(messages, folderId, FolderOperation.MOVE)
    }

    private fun copyOrMove(messages: List<MessageReference>, destinationFolderId: Long, operation: FolderOperation) {
        if (!checkCopyOrMovePossible(messages, operation)) return

        val folderMap = messages.asSequence()
            .filterNot { it.folderId == destinationFolderId }
            .groupBy { it.folderId }

        for ((folderId, messagesInFolder) in folderMap) {
            val account = preferences.getAccount(messagesInFolder.first().accountUuid)

            if (operation == FolderOperation.MOVE) {
                if (showingThreadedList) {
                    messagingController.moveMessagesInThread(account, folderId, messagesInFolder, destinationFolderId)
                } else {
                    messagingController.moveMessages(account, folderId, messagesInFolder, destinationFolderId)
                }
            } else {
                if (showingThreadedList) {
                    messagingController.copyMessagesInThread(account, folderId, messagesInFolder, destinationFolderId)
                } else {
                    messagingController.copyMessages(account, folderId, messagesInFolder, destinationFolderId)
                }
            }
        }
    }

    private fun onMoveToDraftsFolder(messages: List<MessageReference>) {
        messagingController.moveToDraftsFolder(account, currentFolder!!.databaseId, messages)
        activeMessages = null
    }

    override fun doPositiveClick(dialogId: Int) {
        when (dialogId) {
            R.id.dialog_confirm_spam -> {
                onSpamConfirmed(activeMessages!!)
                activeMessages = null
            }
            R.id.dialog_confirm_delete -> {
                onDeleteConfirmed(activeMessages!!)
                activeMessage = null
                adapter.activeMessage = null
            }
            R.id.dialog_confirm_mark_all_as_read -> {
                markAllAsRead()
            }
            R.id.dialog_confirm_empty_trash -> {
                messagingController.emptyTrash(account, null)
            }
        }
    }

    override fun doNegativeClick(dialogId: Int) {
        if (dialogId == R.id.dialog_confirm_spam || dialogId == R.id.dialog_confirm_delete) {
            // No further need for this reference
            activeMessages = null
        }
    }

    override fun dialogCancelled(dialogId: Int) {
        doNegativeClick(dialogId)
    }

    private fun checkMail() {
        if (isSingleAccountMode && isSingleFolderMode) {
            val folderId = currentFolder!!.databaseId
            messagingController.synchronizeMailbox(account, folderId, false, activityListener)
            messagingController.sendPendingMessages(account, activityListener)
        } else if (allAccounts) {
            messagingController.checkMail(null, true, true, false, activityListener)
        } else {
            for (accountUuid in accountUuids) {
                val account = preferences.getAccount(accountUuid)
                messagingController.checkMail(account, true, true, false, activityListener)
            }
        }
    }

    override fun onStop() {
        // If we represent a remote search, then kill that before going back.
        if (isRemoteSearch && remoteSearchFuture != null) {
            try {
                Timber.i("Remote search in progress, attempting to abort...")

                // Canceling the future stops any message fetches in progress.
                val cancelSuccess = remoteSearchFuture!!.cancel(true) // mayInterruptIfRunning = true
                if (!cancelSuccess) {
                    Timber.e("Could not cancel remote search future.")
                }

                // Closing the folder will kill off the connection if we're mid-search.
                val searchAccount = account!!

                // Send a remoteSearchFinished() message for good measure.
                activityListener.remoteSearchFinished(
                    currentFolder!!.databaseId,
                    0,
                    searchAccount.remoteSearchNumResults,
                    null
                )
            } catch (e: Exception) {
                // Since the user is going back, log and squash any exceptions.
                Timber.e(e, "Could not abort remote search before going back")
            }
        }

        super.onStop()
    }

    fun selectAll() {
        setSelectionState(true)
    }

    fun onMoveUp() {
        var currentPosition = listView.selectedItemPosition
        if (currentPosition == AdapterView.INVALID_POSITION || listView.isInTouchMode) {
            currentPosition = listView.firstVisiblePosition
        }

        if (currentPosition > 0) {
            listView.setSelection(currentPosition - 1)
        }
    }

    fun onMoveDown() {
        var currentPosition = listView.selectedItemPosition
        if (currentPosition == AdapterView.INVALID_POSITION || listView.isInTouchMode) {
            currentPosition = listView.firstVisiblePosition
        }

        if (currentPosition < listView.count) {
            listView.setSelection(currentPosition + 1)
        }
    }

    fun openPrevious(messageReference: MessageReference): Boolean {
        val position = getPosition(messageReference)
        if (position <= 0) return false

        openMessageAtPosition(position - 1)
        return true
    }

    fun openNext(messageReference: MessageReference): Boolean {
        val position = getPosition(messageReference)
        if (position < 0 || position == adapter.count - 1) return false

        openMessageAtPosition(position + 1)
        return true
    }

    fun isFirst(messageReference: MessageReference): Boolean {
        return adapter.isEmpty || messageReference == getReferenceForPosition(0)
    }

    fun isLast(messageReference: MessageReference): Boolean {
        return adapter.isEmpty || messageReference == getReferenceForPosition(adapter.count - 1)
    }

    private fun getReferenceForPosition(position: Int): MessageReference {
        val item = adapter.getItem(position)
        return MessageReference(item.account.uuid, item.folderId, item.messageUid)
    }

    private fun openMessageAtPosition(position: Int) {
        // Scroll message into view if necessary
        val listViewPosition = adapterToListViewPosition(position)
        if (listViewPosition != AdapterView.INVALID_POSITION &&
            (listViewPosition < listView.firstVisiblePosition || listViewPosition > listView.lastVisiblePosition)
        ) {
            listView.setSelection(listViewPosition)
        }

        val messageReference = getReferenceForPosition(position)

        // For some reason the listView.setSelection() above won't do anything when we call
        // onOpenMessage() (and consequently adapter.notifyDataSetChanged()) right away. So we
        // defer the call using MessageListHandler.
        handler.openMessage(messageReference)
    }

    fun openMessage(messageReference: MessageReference) {
        fragmentListener.openMessage(messageReference)
    }

    private fun getPosition(messageReference: MessageReference): Int {
        return adapter.messages.indexOfFirst { messageListItem ->
            messageListItem.account.uuid == messageReference.accountUuid &&
                messageListItem.folderId == messageReference.folderId &&
                messageListItem.messageUid == messageReference.uid
        }
    }

    fun onReverseSort() {
        changeSort(sortType)
    }

    private val selectedMessage: MessageReference?
        get() {
            val listViewPosition = listView.selectedItemPosition
            val adapterPosition = listViewToAdapterPosition(listViewPosition)
            if (adapterPosition == AdapterView.INVALID_POSITION) return null
            return getReferenceForPosition(adapterPosition)
        }

    private val adapterPositionForSelectedMessage: Int
        get() {
            val listViewPosition = listView.selectedItemPosition
            return listViewToAdapterPosition(listViewPosition)
        }

    private val checkedMessages: List<MessageReference>
        get() {
            return adapter.messages
                .asSequence()
                .filter { it.uniqueId in selected }
                .map { MessageReference(it.account.uuid, it.folderId, it.messageUid) }
                .toList()
        }

    fun onDelete() {
        selectedMessage?.let { message ->
            onDelete(listOf(message))
        }
    }

    fun toggleMessageSelect() {
        toggleMessageSelect(listView.selectedItemPosition)
    }

    fun onToggleFlagged() {
        toggleFlag(Flag.FLAGGED)
    }

    fun onToggleRead() {
        toggleFlag(Flag.SEEN)
    }

    private fun toggleFlag(flag: Flag) {
        val adapterPosition = adapterPositionForSelectedMessage
        if (adapterPosition == ListView.INVALID_POSITION) return

        val messageListItem = adapter.getItem(adapterPosition)
        val flagState = when (flag) {
            Flag.SEEN -> messageListItem.isRead
            Flag.FLAGGED -> messageListItem.isStarred
            else -> false
        }

        setFlag(messageListItem, flag, !flagState)
    }

    fun onMove() {
        selectedMessage?.let { message ->
            onMove(message)
        }
    }

    fun onArchive() {
        selectedMessage?.let { message ->
            onArchive(message)
        }
    }

    fun onCopy() {
        selectedMessage?.let { message ->
            onCopy(message)
        }
    }

    val isOutbox: Boolean
        get() = isSpecialFolder(account?.outboxFolderId)

    private val isInbox: Boolean
        get() = isSpecialFolder(account?.inboxFolderId)

    private val isArchiveFolder: Boolean
        get() = isSpecialFolder(account?.archiveFolderId)

    private val isSpamFolder: Boolean
        get() = isSpecialFolder(account?.spamFolderId)

    private fun isSpecialFolder(specialFolderId: Long?): Boolean {
        val folderId = specialFolderId ?: return false
        val currentFolder = currentFolder ?: return false
        return currentFolder.databaseId == folderId
    }

    val isRemoteFolder: Boolean
        get() {
            if (localSearch.isManualSearch || isOutbox) return false

            return if (!messagingController.isMoveCapable(account)) {
                // For POP3 accounts only the Inbox is a remote folder.
                isInbox
            } else {
                true
            }
        }

    val isManualSearch: Boolean
        get() = localSearch.isManualSearch

    fun shouldShowExpungeAction(): Boolean {
        val account = this.account ?: return false
        return account.expungePolicy == Expunge.EXPUNGE_MANUALLY && messagingController.supportsExpunge(account)
    }

    fun onRemoteSearch() {
        // Remote search is useless without the network.
        if (hasConnectivity == true) {
            onRemoteSearchRequested()
        } else {
            Toast.makeText(activity, getText(R.string.remote_search_unavailable_no_network), Toast.LENGTH_SHORT).show()
        }
    }

    val isRemoteSearchAllowed: Boolean
        get() = isManualSearch && !isRemoteSearch && isSingleFolderMode && messagingController.isPushCapable(account)

    fun onSearchRequested(query: String): Boolean {
        val folderId = currentFolder?.databaseId
        return fragmentListener.startSearch(query, account, folderId)
    }

    fun setMessageList(messageListInfo: MessageListInfo) {
        val messageListItems = messageListInfo.messageListItems
        if (isThreadDisplay && messageListItems.isEmpty()) {
            handler.goBack()
            return
        }

        swipeRefreshLayout.isRefreshing = false
        swipeRefreshLayout.isEnabled = isPullToRefreshAllowed

        if (isThreadDisplay) {
            if (messageListItems.isNotEmpty()) {
                val strippedSubject = messageListItems.first().subject?.let { Utility.stripSubject(it) }
                threadTitle = if (strippedSubject.isNullOrEmpty()) {
                    getString(R.string.general_no_subject)
                } else {
                    strippedSubject
                }
                updateTitle()
            } else {
                // TODO: empty thread view -> return to full message list
            }
        }

        cleanupSelected(messageListItems)
        adapter.selected = selected

        adapter.messages = messageListItems

        resetActionMode()
        computeBatchDirection()

        isLoadFinished = true

        if (savedListState != null) {
            handler.restoreListPosition(savedListState)
            savedListState = null
        }

        fragmentListener.updateMenu()

        currentFolder?.let { currentFolder ->
            currentFolder.moreMessages = messageListInfo.hasMoreMessages
            updateFooterView()
        }
    }

    private fun cleanupSelected(messageListItems: List<MessageListItem>) {
        if (selected.isEmpty()) return

        selected = messageListItems.asSequence()
            .map { it.uniqueId }
            .filter { it in selected }
            .toMutableSet()
    }

    private fun resetActionMode() {
        if (selected.isEmpty()) {
            actionMode?.finish()
            return
        }

        if (actionMode == null) {
            startAndPrepareActionMode()
        }

        recalculateSelectionCount()
        updateActionMode()
    }

    private fun startAndPrepareActionMode() {
        val activity = requireActivity() as AppCompatActivity
        actionMode = activity.startSupportActionMode(actionModeCallback)
        actionMode?.invalidate()
    }

    private fun recalculateSelectionCount() {
        if (!showingThreadedList) {
            selectedCount = selected.size
            return
        }

        selectedCount = adapter.messages
            .asSequence()
            .filter { it.uniqueId in selected }
            .sumOf { it.threadCount.coerceAtLeast(1) }
    }

    fun remoteSearchFinished() {
        remoteSearchFuture = null
    }

    fun setActiveMessage(messageReference: MessageReference?) {
        activeMessage = messageReference

        // Reload message list with modified query that always includes the active message
        if (isAdded) {
            loadMessageList()
        }

        // Redraw list immediately
        if (::adapter.isInitialized) {
            adapter.activeMessage = activeMessage
            adapter.notifyDataSetChanged()
        }
    }

    val isMarkAllAsReadSupported: Boolean
        get() = isSingleAccountMode && isSingleFolderMode && !isOutbox

    fun confirmMarkAllAsRead() {
        if (K9.isConfirmMarkAllRead) {
            showDialog(R.id.dialog_confirm_mark_all_as_read)
        } else {
            markAllAsRead()
        }
    }

    private fun markAllAsRead() {
        if (isMarkAllAsReadSupported) {
            messagingController.markAllMessagesRead(account, currentFolder!!.databaseId)
        }
    }

    val isCheckMailSupported: Boolean
        get() = allAccounts || !isSingleAccountMode || !isSingleFolderMode || isRemoteFolder

    private val isCheckMailAllowed: Boolean
        get() = !isManualSearch && isCheckMailSupported

    private val isPullToRefreshAllowed: Boolean
        get() = isRemoteSearchAllowed || isCheckMailAllowed

    internal inner class MessageListActivityListener : SimpleMessagingListener() {
        private val lock = Any()

        @GuardedBy("lock")
        private var folderCompleted = 0

        @GuardedBy("lock")
        private var folderTotal = 0

        override fun remoteSearchFailed(folderServerId: String?, err: String?) {
            handler.post {
                activity?.let { activity ->
                    Toast.makeText(activity, R.string.remote_search_error, Toast.LENGTH_LONG).show()
                }
            }
        }

        override fun remoteSearchStarted(folderId: Long) {
            handler.progress(true)
            handler.updateFooter(getString(R.string.remote_search_sending_query))
        }

        override fun enableProgressIndicator(enable: Boolean) {
            handler.progress(enable)
        }

        override fun remoteSearchFinished(
            folderId: Long,
            numResults: Int,
            maxResults: Int,
            extraResults: List<String>?
        ) {
            handler.progress(false)
            handler.remoteSearchFinished()

            extraSearchResults = extraResults
            if (extraResults != null && extraResults.isNotEmpty()) {
                handler.updateFooter(String.format(getString(R.string.load_more_messages_fmt), maxResults))
            } else {
                handler.updateFooter(null)
            }
        }

        override fun remoteSearchServerQueryComplete(folderId: Long, numResults: Int, maxResults: Int) {
            handler.progress(true)

            val footerText = if (maxResults != 0 && numResults > maxResults) {
                resources.getQuantityString(
                    R.plurals.remote_search_downloading_limited,
                    maxResults,
                    maxResults,
                    numResults
                )
            } else {
                resources.getQuantityString(R.plurals.remote_search_downloading, numResults, numResults)
            }

            handler.updateFooter(footerText)
            informUserOfStatus()
        }

        private fun informUserOfStatus() {
            handler.refreshTitle()
        }

        override fun synchronizeMailboxStarted(account: Account, folderId: Long) {
            if (updateForMe(account, folderId)) {
                handler.progress(true)
                handler.folderLoading(folderId, true)

                synchronized(lock) {
                    folderCompleted = 0
                    folderTotal = 0
                }

                informUserOfStatus()
            }
        }

        override fun synchronizeMailboxHeadersProgress(
            account: Account,
            folderServerId: String,
            completed: Int,
            total: Int
        ) {
            synchronized(lock) {
                folderCompleted = completed
                folderTotal = total
            }

            informUserOfStatus()
        }

        override fun synchronizeMailboxHeadersFinished(
            account: Account,
            folderServerId: String,
            total: Int,
            completed: Int
        ) {
            synchronized(lock) {
                folderCompleted = 0
                folderTotal = 0
            }

            informUserOfStatus()
        }

        override fun synchronizeMailboxProgress(account: Account, folderId: Long, completed: Int, total: Int) {
            synchronized(lock) {
                folderCompleted = completed
                folderTotal = total
            }

            informUserOfStatus()
        }

        override fun synchronizeMailboxFinished(account: Account, folderId: Long) {
            if (updateForMe(account, folderId)) {
                handler.progress(false)
                handler.folderLoading(folderId, false)
            }
        }

        override fun synchronizeMailboxFailed(account: Account, folderId: Long, message: String) {
            if (updateForMe(account, folderId)) {
                handler.progress(false)
                handler.folderLoading(folderId, false)
            }
        }

        override fun checkMailFinished(context: Context?, account: Account?) {
            handler.progress(false)
        }

        private fun updateForMe(account: Account?, folderId: Long): Boolean {
            if (account == null || account.uuid !in accountUuids) return false

            val folderIds = localSearch.folderIds
            return folderIds.isEmpty() || folderId in folderIds
        }

        fun getFolderCompleted(): Int {
            synchronized(lock) {
                return folderCompleted
            }
        }

        fun getFolderTotal(): Int {
            synchronized(lock) {
                return folderTotal
            }
        }
    }

    internal inner class ActionModeCallback : ActionMode.Callback {
        private var selectAll: MenuItem? = null
        private var markAsRead: MenuItem? = null
        private var markAsUnread: MenuItem? = null
        private var flag: MenuItem? = null
        private var unflag: MenuItem? = null
        private var disableMarkAsRead = false
        private var disableFlag = false

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            selectAll = menu.findItem(R.id.select_all)
            markAsRead = menu.findItem(R.id.mark_as_read)
            markAsUnread = menu.findItem(R.id.mark_as_unread)
            flag = menu.findItem(R.id.flag)
            unflag = menu.findItem(R.id.unflag)

            // we don't support cross account actions atm
            if (!isSingleAccountMode) {
                val accounts = accountUuidsForSelected.mapNotNull { accountUuid ->
                    preferences.getAccount(accountUuid)
                }

                menu.findItem(R.id.move).isVisible = true
                menu.findItem(R.id.copy).isVisible = true

                // Disable archive/spam options here and maybe enable below when checking account capabilities
                menu.findItem(R.id.archive).isVisible = false
                menu.findItem(R.id.spam).isVisible = false

                for (account in accounts) {
                    setContextCapabilities(account, menu)
                }
            }

            return true
        }

        private val accountUuidsForSelected: Set<String>
            get() {
                return adapter.messages.asSequence()
                    .filter { it.uniqueId in selected }
                    .map { it.account.uuid }
                    .toSet()
            }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
            selectAll = null
            markAsRead = null
            markAsUnread = null
            flag = null
            unflag = null

            setSelectionState(false)
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.message_list_context, menu)

            setContextCapabilities(account, menu)
            return true
        }

        private fun setContextCapabilities(account: Account?, menu: Menu) {
            if (!isSingleAccountMode || account == null) {
                // We don't support cross-account copy/move operations right now
                menu.findItem(R.id.move).isVisible = false
                menu.findItem(R.id.copy).isVisible = false

                if (account?.hasArchiveFolder() == true) {
                    menu.findItem(R.id.archive).isVisible = true
                }

                if (account?.hasSpamFolder() == true) {
                    menu.findItem(R.id.spam).isVisible = true
                }
            } else if (isOutbox) {
                menu.findItem(R.id.mark_as_read).isVisible = false
                menu.findItem(R.id.mark_as_unread).isVisible = false
                menu.findItem(R.id.archive).isVisible = false
                menu.findItem(R.id.copy).isVisible = false
                menu.findItem(R.id.flag).isVisible = false
                menu.findItem(R.id.unflag).isVisible = false
                menu.findItem(R.id.spam).isVisible = false
                menu.findItem(R.id.move).isVisible = false

                disableMarkAsRead = true
                disableFlag = true

                if (account.hasDraftsFolder()) {
                    menu.findItem(R.id.move_to_drafts).isVisible = true
                }
            } else {
                if (!messagingController.isCopyCapable(account)) {
                    menu.findItem(R.id.copy).isVisible = false
                }

                if (!messagingController.isMoveCapable(account)) {
                    menu.findItem(R.id.move).isVisible = false
                    menu.findItem(R.id.archive).isVisible = false
                    menu.findItem(R.id.spam).isVisible = false
                } else {
                    if (!account.hasArchiveFolder() || isArchiveFolder) {
                        menu.findItem(R.id.archive).isVisible = false
                    }

                    if (!account.hasSpamFolder() || isSpamFolder) {
                        menu.findItem(R.id.spam).isVisible = false
                    }
                }
            }
        }

        fun showSelectAll(show: Boolean) {
            selectAll?.isVisible = show
        }

        fun showMarkAsRead(show: Boolean) {
            if (!disableMarkAsRead) {
                markAsRead?.isVisible = show
                markAsUnread?.isVisible = !show
            }
        }

        fun showFlag(show: Boolean) {
            if (!disableFlag) {
                flag?.isVisible = show
                unflag?.isVisible = !show
            }
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            // In the following we assume that we can't move or copy mails to the same folder. Also that spam isn't
            // available if we are in the spam folder, same for archive.
            when (item.itemId) {
                R.id.delete -> {
                    val messages = checkedMessages
                    onDelete(messages)
                    selectedCount = 0
                }
                R.id.mark_as_read -> setFlagForSelected(Flag.SEEN, true)
                R.id.mark_as_unread -> setFlagForSelected(Flag.SEEN, false)
                R.id.flag -> setFlagForSelected(Flag.FLAGGED, true)
                R.id.unflag -> setFlagForSelected(Flag.FLAGGED, false)
                R.id.select_all -> selectAll()
                R.id.archive -> {
                    onArchive(checkedMessages)
                    // TODO: Only finish action mode if all messages have been moved.
                    selectedCount = 0
                }
                R.id.spam -> {
                    onSpam(checkedMessages)
                    // TODO: Only finish action mode if all messages have been moved.
                    selectedCount = 0
                }
                R.id.move -> {
                    onMove(checkedMessages)
                    selectedCount = 0
                }
                R.id.move_to_drafts -> {
                    onMoveToDraftsFolder(checkedMessages)
                    selectedCount = 0
                }
                R.id.copy -> {
                    onCopy(checkedMessages)
                    selectedCount = 0
                }
            }

            if (selectedCount == 0) {
                mode.finish()
            }

            return true
        }
    }

    internal class FooterViewHolder(view: View) {
        val main: TextView = view.findViewById(R.id.main_text)
    }

    private enum class FolderOperation {
        COPY, MOVE
    }

    interface MessageListFragmentListener {
        fun setMessageListProgressEnabled(enable: Boolean)
        fun setMessageListProgress(level: Int)
        fun showThread(account: Account, threadRootId: Long)
        fun openMessage(messageReference: MessageReference)
        fun setMessageListTitle(title: String, subtitle: String?)
        fun onCompose(account: Account?)
        fun startSearch(query: String, account: Account?, folderId: Long?): Boolean
        fun remoteSearchStarted()
        fun goBack()
        fun updateMenu()
        fun onFolderNotFoundError()

        companion object {
            const val MAX_PROGRESS = 10000
        }
    }

    companion object {
        private const val ACTIVITY_CHOOSE_FOLDER_MOVE = 1
        private const val ACTIVITY_CHOOSE_FOLDER_COPY = 2

        private const val ARG_SEARCH = "searchObject"
        private const val ARG_THREADED_LIST = "showingThreadedList"
        private const val ARG_IS_THREAD_DISPLAY = "isThreadedDisplay"

        private const val STATE_SELECTED_MESSAGES = "selectedMessages"
        private const val STATE_ACTIVE_MESSAGES = "activeMessages"
        private const val STATE_ACTIVE_MESSAGE = "activeMessage"
        private const val STATE_REMOTE_SEARCH_PERFORMED = "remoteSearchPerformed"
        private const val STATE_MESSAGE_LIST = "listState"

        fun newInstance(search: LocalSearch, isThreadDisplay: Boolean, threadedList: Boolean): MessageListFragment {
            return MessageListFragment().apply {
                arguments = bundleOf(
                    ARG_SEARCH to search,
                    ARG_IS_THREAD_DISPLAY to isThreadDisplay,
                    ARG_THREADED_LIST to threadedList,
                )
            }
        }
    }
}
