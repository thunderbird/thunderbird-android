package com.fsck.k9.activity

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import com.fsck.k9.Account
import com.fsck.k9.Account.SortType
import com.fsck.k9.K9
import com.fsck.k9.K9.SplitViewMode
import com.fsck.k9.Preferences
import com.fsck.k9.account.BackgroundAccountRemover
import com.fsck.k9.activity.compose.MessageActions
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.fragment.MessageListFragment
import com.fsck.k9.fragment.MessageListFragment.MessageListFragmentListener
import com.fsck.k9.helper.Contacts
import com.fsck.k9.helper.ParcelableUtil
import com.fsck.k9.mailstore.SearchStatusManager
import com.fsck.k9.mailstore.StorageManager
import com.fsck.k9.mailstore.StorageManager.StorageListener
import com.fsck.k9.notification.NotificationChannelManager
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchAccount
import com.fsck.k9.search.SearchSpecification
import com.fsck.k9.search.SearchSpecification.SearchCondition
import com.fsck.k9.search.SearchSpecification.SearchField
import com.fsck.k9.ui.BuildConfig
import com.fsck.k9.ui.K9Drawer
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.K9Activity
import com.fsck.k9.ui.base.Theme
import com.fsck.k9.ui.changelog.RecentChangesActivity
import com.fsck.k9.ui.changelog.RecentChangesViewModel
import com.fsck.k9.ui.managefolders.ManageFoldersActivity
import com.fsck.k9.ui.messagelist.DefaultFolderProvider
import com.fsck.k9.ui.messagesource.MessageSourceActivity
import com.fsck.k9.ui.messageview.MessageViewFragment
import com.fsck.k9.ui.messageview.MessageViewFragment.MessageViewFragmentListener
import com.fsck.k9.ui.messageview.MessageViewPagerFragment
import com.fsck.k9.ui.messageview.PlaceholderFragment
import com.fsck.k9.ui.onboarding.OnboardingActivity
import com.fsck.k9.ui.permissions.K9PermissionUiHelper
import com.fsck.k9.ui.permissions.Permission
import com.fsck.k9.ui.permissions.PermissionUiHelper
import com.fsck.k9.view.ViewSwitcher
import com.fsck.k9.view.ViewSwitcher.OnSwitchCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.materialdrawer.util.getOptimalDrawerWidth
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * MessageList is the primary user interface for the program. This Activity shows a list of messages.
 *
 * From this Activity the user can perform all standard message operations.
 */
open class MessageList :
    K9Activity(),
    MessageListFragmentListener,
    MessageViewFragmentListener,
    FragmentManager.OnBackStackChangedListener,
    OnSwitchCompleteListener,
    PermissionUiHelper {

    private val recentChangesViewModel: RecentChangesViewModel by viewModel()

    protected val searchStatusManager: SearchStatusManager by inject()
    private val preferences: Preferences by inject()
    private val channelUtils: NotificationChannelManager by inject()
    private val defaultFolderProvider: DefaultFolderProvider by inject()
    private val accountRemover: BackgroundAccountRemover by inject()

    private val storageListener: StorageListener = StorageListenerImplementation()
    private val permissionUiHelper: PermissionUiHelper = K9PermissionUiHelper(this)

    private lateinit var actionBar: ActionBar
    private lateinit var searchView: SearchView
    private var drawer: K9Drawer? = null
    private var openFolderTransaction: FragmentTransaction? = null
    private var menu: Menu? = null
    private var progressBar: ProgressBar? = null
    private var messageViewPlaceHolder: PlaceholderFragment? = null
    private var messageListFragment: MessageListFragment? = null
        set(value) {
            val changing = (field != value)
            field = value
            if (changing) {
                messageViewPagerFragment?.notifyMessageListFragmentChanged()
            }
        }
    private var messageViewPagerFragment: MessageViewPagerFragment? = null
    private var firstBackStackId = -1
    private var account: Account? = null
    private var search: LocalSearch? = null
    private var singleFolderMode = false
    private var lastDirection = if (K9.isMessageViewShowNext) NEXT else PREVIOUS

    private var messageListActivityAppearance: MessageListActivityAppearance? = null

    /**
     * `true` if the message list should be displayed as flat list (i.e. no threading)
     * regardless whether or not message threading was enabled in the settings. This is used for
     * filtered views, e.g. when only displaying the unread messages in a folder.
     */
    private var noThreading = false
    private var displayMode: DisplayMode? = null
    private var messageReference: MessageReference? = null

    /**
     * `true` when the message list was displayed once. This is used in
     * [.onBackPressed] to decide whether to go from the message view to the message list or
     * finish the activity.
     */
    private var messageListWasDisplayed = false
    private var viewSwitcher: ViewSwitcher? = null
    private lateinit var recentChangesSnackbar: Snackbar

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If the app's main task was not created using the default launch intent (e.g. from a notification, a widget,
        // or a shortcut), using the app icon to "launch" the app will create a new MessageList instance instead of only
        // bringing the app's task to the foreground. We catch this situation here and simply finish the activity. This
        // will bring the task to the foreground, showing the last active screen.
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER) && !isTaskRoot) {
            Timber.v("Not displaying MessageList. Only bringing the app task to the foreground.")
            finish()
            return
        }

        val accounts = preferences.accounts
        deleteIncompleteAccounts(accounts)
        val hasAccountSetup = accounts.any { it.isFinishedSetup }
        if (!hasAccountSetup) {
            OnboardingActivity.launch(this)
            finish()
            return
        }

        if (UpgradeDatabases.actionUpgradeDatabases(this, intent)) {
            finish()
            return
        }

        if (useSplitView()) {
            setLayout(R.layout.split_message_list)
        } else {
            setLayout(R.layout.message_list)
            viewSwitcher = findViewById<ViewSwitcher>(R.id.container).apply {
                firstInAnimation = AnimationUtils.loadAnimation(this@MessageList, R.anim.slide_in_left)
                firstOutAnimation = AnimationUtils.loadAnimation(this@MessageList, R.anim.slide_out_right)
                secondInAnimation = AnimationUtils.loadAnimation(this@MessageList, R.anim.slide_in_right)
                secondOutAnimation = AnimationUtils.loadAnimation(this@MessageList, R.anim.slide_out_left)
                setOnSwitchCompleteListener(this@MessageList)
            }
        }

        window.statusBarColor = Color.TRANSPARENT

        val rootLayout = findViewById<View>(R.id.drawerLayout)
        rootLayout.systemUiVisibility = rootLayout.systemUiVisibility or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(view.paddingLeft, insets.systemWindowInsetTop, view.paddingRight, view.paddingBottom)
            insets
        }

        val swipeRefreshLayout = findViewById<View>(R.id.material_drawer_swipe_refresh)
        swipeRefreshLayout.layoutParams.width = getOptimalDrawerWidth(this)

        initializeActionBar()
        initializeDrawer(savedInstanceState)

        if (!decodeExtras(intent)) {
            return
        }

        if (isDrawerEnabled) {
            configureDrawer()
            drawer!!.updateUserAccountsAndFolders(account)
        }

        findFragments()
        initializeDisplayMode(savedInstanceState)
        initializeLayout()
        initializeFragments()
        displayViews()
        initializeRecentChangesSnackbar()
        channelUtils.updateChannels()

        if (savedInstanceState == null) {
            checkAndRequestPermissions()
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (isFinishing) {
            return
        }

        setIntent(intent)

        if (firstBackStackId >= 0) {
            supportFragmentManager.popBackStackImmediate(firstBackStackId, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            firstBackStackId = -1
        }

        removeMessageListFragment()
        removeMessageViewPagerFragment()

        messageReference = null
        search = null

        if (!decodeExtras(intent)) {
            return
        }

        if (isDrawerEnabled) {
            configureDrawer()
            drawer!!.updateUserAccountsAndFolders(account)
        }

        initializeDisplayMode(null)
        initializeFragments()
        displayViews()
    }

    private fun deleteIncompleteAccounts(accounts: List<Account>) {
        accounts.filter { !it.isFinishedSetup }.forEach {
            accountRemover.removeAccountAsync(it.uuid)
        }
    }

    private fun findFragments() {
        val fragmentManager = supportFragmentManager
        messageListFragment = fragmentManager.findFragmentById(R.id.message_list_container) as MessageListFragment?
        messageViewPagerFragment = fragmentManager.findFragmentById(R.id.message_viewpager_container) as MessageViewPagerFragment?
        messageListFragment?.let { messageListFragment ->
            initializeFromLocalSearch(messageListFragment.localSearch)
        }
    }

    private fun initializeFragments() {
        val fragmentManager = supportFragmentManager
        fragmentManager.addOnBackStackChangedListener(this)

        val hasMessageListFragment = messageListFragment != null
        if (!hasMessageListFragment) {
            val fragmentTransaction = fragmentManager.beginTransaction()
            val messageListFragment = MessageListFragment.newInstance(
                search!!, false, K9.isThreadedViewEnabled && !noThreading
            )
            fragmentTransaction.add(R.id.message_list_container, messageListFragment)
            fragmentTransaction.commit()

            this.messageListFragment = messageListFragment
        }

        val hasMessageViewPagerFragment = messageViewPagerFragment != null
        if (!hasMessageViewPagerFragment) {
            val fragmentTransaction = fragmentManager.beginTransaction()
            val messageViewPagerFragment1 = MessageViewPagerFragment(this)
            fragmentTransaction.add(R.id.message_viewpager_container, messageViewPagerFragment1)
            fragmentTransaction.commit()

            messageViewPagerFragment = messageViewPagerFragment1
        }

        // Check if the fragment wasn't restarted and has a MessageReference in the arguments.
        // If so, open the referenced message.
        if (!hasMessageListFragment && messageReference != null) {
            openMessage(messageReference!!)
        }
    }

    /**
     * Set the initial display mode (message list, message view, or split view).
     *
     * **Note:**
     * This method has to be called after [.findFragments] because the result depends on
     * the availability of a [MessageViewFragment] instance.
     */
    private fun initializeDisplayMode(savedInstanceState: Bundle?) {
        if (useSplitView()) {
            displayMode = DisplayMode.SPLIT_VIEW
            return
        }

        if (savedInstanceState != null) {
            val savedDisplayMode = savedInstanceState.getSerializable(STATE_DISPLAY_MODE) as DisplayMode?
            if (savedDisplayMode != DisplayMode.SPLIT_VIEW) {
                displayMode = savedDisplayMode
                return
            }
        }

        displayMode = if (messageViewPagerFragment != null || messageReference != null) {
            DisplayMode.MESSAGE_VIEW
        } else {
            DisplayMode.MESSAGE_LIST
        }
    }

    private fun useSplitView(): Boolean {
        val splitViewMode = K9.splitViewMode
        val orientation = resources.configuration.orientation
        return splitViewMode === SplitViewMode.ALWAYS ||
            splitViewMode === SplitViewMode.WHEN_IN_LANDSCAPE && orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    private fun initializeLayout() {
        progressBar = findViewById(R.id.message_list_progress)
        messageViewPlaceHolder = PlaceholderFragment()
    }

    private fun displayViews() {
        when (displayMode) {
            DisplayMode.MESSAGE_LIST -> {
                showMessageList()
            }
            DisplayMode.MESSAGE_VIEW -> {
                showMessageView()
            }
            DisplayMode.SPLIT_VIEW -> {
                messageListWasDisplayed = true
                if (messageViewPagerFragment == null) {
                    showMessageViewPlaceHolder()
                } else {
                    val activeMessage = messageViewPagerFragment!!.activeMessageViewFragment?.messageReference
                    if (activeMessage != null) {
                        messageListFragment!!.setActiveMessage(activeMessage)
                    }
                }
                setDrawerLockState()
            }
        }
    }

    private val shouldShowRecentChangesHintObserver = Observer<Boolean> { showRecentChangesHint ->
        val recentChangesSnackbarVisible = recentChangesSnackbar.isShown
        if (showRecentChangesHint && !recentChangesSnackbarVisible) {
            recentChangesSnackbar.show()
        } else if (!showRecentChangesHint && recentChangesSnackbarVisible) {
            recentChangesSnackbar.dismiss()
        }
    }

    @SuppressLint("ShowToast")
    private fun initializeRecentChangesSnackbar() {
        recentChangesSnackbar = Snackbar
            .make(findViewById(R.id.container), R.string.changelog_snackbar_text, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.okay_action) { launchRecentChangesActivity() }

        recentChangesViewModel.shouldShowRecentChangesHint.observe(this, shouldShowRecentChangesHintObserver)
    }

    private fun launchRecentChangesActivity() {
        recentChangesViewModel.shouldShowRecentChangesHint.removeObserver(shouldShowRecentChangesHintObserver)

        val intent = Intent(this, RecentChangesActivity::class.java)
        startActivity(intent)
    }

    private fun decodeExtras(intent: Intent): Boolean {
        val launchData = decodeExtrasToLaunchData(intent)

        // If Unified Inbox was disabled show default account instead
        val search = if (launchData.search.isUnifiedInbox && !K9.isShowUnifiedInbox) {
            createDefaultLocalSearch()
        } else {
            launchData.search
        }

        // Don't switch the currently active account when opening the Unified Inbox
        val account = account?.takeIf { launchData.search.isUnifiedInbox } ?: search.firstAccount()
        if (account == null) {
            finish()
            return false
        }

        this.account = account
        this.search = search
        singleFolderMode = search.folderIds.size == 1
        noThreading = launchData.noThreading
        messageReference = launchData.messageReference

        if (!account.isAvailable(this)) {
            onAccountUnavailable()
            return false
        }

        return true
    }

    private fun decodeExtrasToLaunchData(intent: Intent): LaunchData {
        val action = intent.action
        val data = intent.data
        val queryString = intent.getStringExtra(SearchManager.QUERY)

        if (action == Intent.ACTION_VIEW && data != null && data.pathSegments.size >= 3) {
            val segmentList = data.pathSegments
            val accountId = segmentList[0]
            for (account in preferences.accounts) {
                if (account.accountNumber.toString() == accountId) {
                    val folderId = segmentList[1].toLong()
                    val messageUid = segmentList[2]
                    val messageReference = MessageReference(account.uuid, folderId, messageUid, null)

                    return LaunchData(
                        search = messageReference.toLocalSearch(),
                        messageReference = messageReference
                    )
                }
            }
        } else if (action == ACTION_SHORTCUT) {
            // Handle shortcut intents
            val specialFolder = intent.getStringExtra(EXTRA_SPECIAL_FOLDER)
            if (SearchAccount.UNIFIED_INBOX == specialFolder) {
                return LaunchData(search = SearchAccount.createUnifiedInboxAccount().relatedSearch)
            }
        } else if (action == Intent.ACTION_SEARCH && queryString != null) {
            // Query was received from Search Dialog
            val query = queryString.trim()

            val search = LocalSearch().apply {
                isManualSearch = true
                or(SearchCondition(SearchField.SENDER, SearchSpecification.Attribute.CONTAINS, query))
                or(SearchCondition(SearchField.SUBJECT, SearchSpecification.Attribute.CONTAINS, query))
                or(SearchCondition(SearchField.MESSAGE_CONTENTS, SearchSpecification.Attribute.CONTAINS, query))
            }

            val appData = intent.getBundleExtra(SearchManager.APP_DATA)
            if (appData != null) {
                val searchAccountUuid = appData.getString(EXTRA_SEARCH_ACCOUNT)
                if (searchAccountUuid != null) {
                    search.addAccountUuid(searchAccountUuid)
                    // searches started from a folder list activity will provide an account, but no folder
                    if (appData.containsKey(EXTRA_SEARCH_FOLDER)) {
                        val folderId = appData.getLong(EXTRA_SEARCH_FOLDER)
                        search.addAllowedFolder(folderId)
                    }
                } else if (BuildConfig.DEBUG) {
                    throw AssertionError("Invalid app data in search intent")
                }
            }

            return LaunchData(
                search = search,
                noThreading = true
            )
        } else if (intent.hasExtra(EXTRA_SEARCH)) {
            // regular LocalSearch object was passed
            val search = ParcelableUtil.unmarshall(intent.getByteArrayExtra(EXTRA_SEARCH), LocalSearch.CREATOR)
            val noThreading = intent.getBooleanExtra(EXTRA_NO_THREADING, false)

            return LaunchData(search = search, noThreading = noThreading)
        } else if (intent.hasExtra(EXTRA_MESSAGE_REFERENCE)) {
            val messageReferenceString = intent.getStringExtra(EXTRA_MESSAGE_REFERENCE)
            val messageReference = MessageReference.parse(messageReferenceString)

            if (messageReference != null) {
                return LaunchData(
                    search = messageReference.toLocalSearch(),
                    messageReference = messageReference
                )
            }
        } else if (intent.hasExtra("account")) {
            val accountUuid = intent.getStringExtra("account")
            if (accountUuid != null) {
                // We've most likely been started by an old unread widget or accounts shortcut
                val account = preferences.getAccount(accountUuid)
                if (account == null) {
                    Timber.d("Account %s not found.", accountUuid)
                    return LaunchData(createDefaultLocalSearch())
                }

                val folderId = defaultFolderProvider.getDefaultFolder(account)
                val search = LocalSearch().apply {
                    addAccountUuid(accountUuid)
                    addAllowedFolder(folderId)
                }

                return LaunchData(search = search)
            }
        }

        // Default action
        val search = if (K9.isShowUnifiedInbox) {
            SearchAccount.createUnifiedInboxAccount().relatedSearch
        } else {
            createDefaultLocalSearch()
        }

        return LaunchData(search)
    }

    private fun createDefaultLocalSearch(): LocalSearch {
        val account = preferences.defaultAccount ?: error("No default account available")
        return LocalSearch().apply {
            addAccountUuid(account.uuid)
            addAllowedFolder(defaultFolderProvider.getDefaultFolder(account))
        }
    }

    private fun checkAndRequestPermissions() {
        if (!hasPermission(Permission.READ_CONTACTS)) {
            requestPermissionOrShowRationale(Permission.READ_CONTACTS)
        }
    }

    public override fun onPause() {
        super.onPause()
        StorageManager.getInstance(application).removeListener(storageListener)
    }

    public override fun onResume() {
        super.onResume()

        if (messageListActivityAppearance == null) {
            messageListActivityAppearance = MessageListActivityAppearance.create()
        } else if (messageListActivityAppearance != MessageListActivityAppearance.create()) {
            recreate()
        }

        if (this !is Search) {
            // necessary b/c no guarantee Search.onStop will be called before MessageList.onResume
            // when returning from search results
            searchStatusManager.isActive = false
        }

        if (account != null && !account!!.isAvailable(this)) {
            onAccountUnavailable()
            return
        }

        StorageManager.getInstance(application).addListener(storageListener)
    }

    override fun onStart() {
        super.onStart()
        Contacts.clearCache()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putSerializable(STATE_DISPLAY_MODE, displayMode)
        outState.putBoolean(STATE_MESSAGE_LIST_WAS_DISPLAYED, messageListWasDisplayed)
        outState.putInt(STATE_FIRST_BACK_STACK_ID, firstBackStackId)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        messageListWasDisplayed = savedInstanceState.getBoolean(STATE_MESSAGE_LIST_WAS_DISPLAYED)
        firstBackStackId = savedInstanceState.getInt(STATE_FIRST_BACK_STACK_ID)
    }

    private fun initializeActionBar() {
        actionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
    }

    private fun initializeDrawer(savedInstanceState: Bundle?) {
        if (!isDrawerEnabled) {
            val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            return
        }

        drawer = K9Drawer(this, savedInstanceState)
    }

    fun createDrawerListener(): DrawerListener {
        return object : DrawerListener {
            override fun onDrawerClosed(drawerView: View) {
                if (openFolderTransaction != null) {
                    openFolderTransaction!!.commit()
                    openFolderTransaction = null
                }
            }

            override fun onDrawerStateChanged(newState: Int) = Unit

            override fun onDrawerOpened(drawerView: View) = Unit

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit
        }
    }

    fun openFolder(folderId: Long) {
        if (displayMode == DisplayMode.SPLIT_VIEW) {
            removeMessageViewPagerFragment()
            showMessageViewPlaceHolder()
        }

        val search = LocalSearch()
        search.addAccountUuid(account!!.uuid)
        search.addAllowedFolder(folderId)

        performSearch(search)
    }

    private fun openFolderImmediately(folderId: Long) {
        openFolder(folderId)
        openFolderTransaction!!.commit()
        openFolderTransaction = null
    }

    fun openUnifiedInbox() {
        actionDisplaySearch(this, SearchAccount.createUnifiedInboxAccount().relatedSearch, false, false)
    }

    fun launchManageFoldersScreen() {
        if (account == null) {
            Timber.e("Tried to open \"Manage folders\", but no account selected!")
            return
        }

        ManageFoldersActivity.launch(this, account!!)
    }

    fun openRealAccount(account: Account): Boolean {
        val shouldCloseDrawer = account.autoExpandFolderId != null

        val folderId = defaultFolderProvider.getDefaultFolder(account)

        val search = LocalSearch()
        search.addAllowedFolder(folderId)
        search.addAccountUuid(account.uuid)
        actionDisplaySearch(this, search, noThreading = false, newTask = false)

        return shouldCloseDrawer
    }

    private fun performSearch(search: LocalSearch) {
        initializeFromLocalSearch(search)

        val fragmentManager = supportFragmentManager

        check(!(BuildConfig.DEBUG && fragmentManager.backStackEntryCount > 0)) {
            "Don't call performSearch() while there are fragments on the back stack"
        }

        val openFolderTransaction = fragmentManager.beginTransaction()
        val messageListFragment = MessageListFragment.newInstance(search, false, K9.isThreadedViewEnabled)
        openFolderTransaction.replace(R.id.message_list_container, messageListFragment)

        this.messageListFragment = messageListFragment

        this.openFolderTransaction = openFolderTransaction
    }

    protected open val isDrawerEnabled: Boolean = true

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        var eventHandled = false
        if (KeyEvent.ACTION_DOWN == event.action) {
            eventHandled = onCustomKeyDown(event.keyCode, event)
        }

        if (!eventHandled) {
            eventHandled = super.dispatchKeyEvent(event)
        }

        return eventHandled
    }

    override fun onBackPressed() {
        if (isDrawerEnabled && drawer!!.isOpen) {
            drawer!!.close()
        } else if (displayMode == DisplayMode.MESSAGE_VIEW && messageListWasDisplayed) {
            showMessageList()
        } else if (this::searchView.isInitialized && !searchView.isIconified) {
            searchView.isIconified = true
        } else {
            if (isDrawerEnabled && account != null && supportFragmentManager.backStackEntryCount == 0) {
                if (K9.isShowUnifiedInbox) {
                    if (search!!.id != SearchAccount.UNIFIED_INBOX) {
                        openUnifiedInbox()
                    } else {
                        super.onBackPressed()
                    }
                } else {
                    val defaultFolderId = defaultFolderProvider.getDefaultFolder(account!!)
                    val currentFolder = if (singleFolderMode) search!!.folderIds[0] else null
                    if (currentFolder == null || defaultFolderId != currentFolder) {
                        openFolderImmediately(defaultFolderId)
                    } else {
                        super.onBackPressed()
                    }
                }
            } else {
                super.onBackPressed()
            }
        }
    }

    /**
     * Handle hotkeys
     *
     * This method is called by [.dispatchKeyEvent] before any view had the chance to consume this key event.
     *
     * @return `true` if this event was consumed.
     */
    private fun onCustomKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!event.hasNoModifiers()) return false

        val messageViewFragment = messageViewPagerFragment!!.activeMessageViewFragment
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (displayMode != DisplayMode.MESSAGE_LIST && K9.isUseVolumeKeysForNavigation) {
                    messageViewPagerFragment!!.showPreviousMessage()
                    return true
                } else if (displayMode != DisplayMode.MESSAGE_VIEW && K9.isUseVolumeKeysForListNavigation) {
                    messageListFragment!!.onMoveUp()
                    return true
                }
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (displayMode != DisplayMode.MESSAGE_LIST && K9.isUseVolumeKeysForNavigation) {
                    messageViewPagerFragment!!.showNextMessage()
                    return true
                } else if (displayMode != DisplayMode.MESSAGE_VIEW && K9.isUseVolumeKeysForListNavigation) {
                    messageListFragment!!.onMoveDown()
                    return true
                }
            }
            KeyEvent.KEYCODE_C -> {
                messageListFragment!!.onCompose()
                return true
            }
            KeyEvent.KEYCODE_O -> {
                messageListFragment!!.onCycleSort()
                return true
            }
            KeyEvent.KEYCODE_I -> {
                messageListFragment!!.onReverseSort()
                return true
            }
            KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_D -> {
                if (displayMode == DisplayMode.MESSAGE_LIST) {
                    messageListFragment!!.onDelete()
                } else {
                    messageViewFragment?.onDelete()
                }
                return true
            }
            KeyEvent.KEYCODE_S -> {
                messageListFragment!!.toggleMessageSelect()
                return true
            }
            KeyEvent.KEYCODE_G -> {
                if (displayMode == DisplayMode.MESSAGE_LIST) {
                    messageListFragment!!.onToggleFlagged()
                } else {
                    messageViewFragment?.onToggleFlagged()
                }
                return true
            }
            KeyEvent.KEYCODE_M -> {
                if (displayMode == DisplayMode.MESSAGE_LIST) {
                    messageListFragment!!.onMove()
                } else {
                    messageViewFragment?.onMove()
                }
                return true
            }
            KeyEvent.KEYCODE_V -> {
                if (displayMode == DisplayMode.MESSAGE_LIST) {
                    messageListFragment!!.onArchive()
                } else {
                    messageViewFragment?.onArchive()
                }
                return true
            }
            KeyEvent.KEYCODE_Y -> {
                if (displayMode == DisplayMode.MESSAGE_LIST) {
                    messageListFragment!!.onCopy()
                } else {
                    messageViewFragment?.onCopy()
                }
                return true
            }
            KeyEvent.KEYCODE_Z -> {
                if (displayMode == DisplayMode.MESSAGE_LIST) {
                    messageListFragment!!.onToggleRead()
                } else {
                    messageViewFragment?.onToggleRead()
                }
                return true
            }
            KeyEvent.KEYCODE_F -> {
                messageViewFragment?.onForward()
                return true
            }
            KeyEvent.KEYCODE_A -> {
                messageViewFragment?.onReplyAll()
                return true
            }
            KeyEvent.KEYCODE_R -> {
                messageViewFragment?.onReply()
                return true
            }
            KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_P -> {
                messageViewPagerFragment!!.showPreviousMessage()
                return true
            }
            KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_K -> {
                messageViewPagerFragment!!.showNextMessage()
                return true
            }
            KeyEvent.KEYCODE_H -> {
                val toast = if (displayMode == DisplayMode.MESSAGE_LIST) {
                    Toast.makeText(this, R.string.message_list_help_key, Toast.LENGTH_LONG)
                } else {
                    Toast.makeText(this, R.string.message_view_help_key, Toast.LENGTH_LONG)
                }
                toast.show()
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                return if (displayMode == DisplayMode.MESSAGE_VIEW) {
                    messageViewPagerFragment!!.showPreviousMessage()
                } else {
                    false
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                return if (displayMode == DisplayMode.MESSAGE_VIEW) {
                    messageViewPagerFragment!!.showNextMessage()
                } else {
                    false
                }
            }
        }
        return false
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        // Swallow these events too to avoid the audible notification of a volume change
        if (K9.isUseVolumeKeysForListNavigation) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                Timber.v("Swallowed key up.")
                return true
            }
        }

        return super.onKeyUp(keyCode, event)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val messageViewFragment = messageViewPagerFragment!!.activeMessageViewFragment
        if (id == android.R.id.home) {
            if (displayMode != DisplayMode.MESSAGE_VIEW && !isAdditionalMessageListDisplayed) {
                if (isDrawerEnabled) {
                    if (drawer!!.isOpen) {
                        drawer!!.close()
                    } else {
                        drawer!!.open()
                    }
                } else {
                    finish()
                }
            } else {
                goBack()
            }
            return true
        } else if (id == R.id.compose) {
            messageListFragment!!.onCompose()
            return true
        } else if (id == R.id.toggle_message_view_theme) {
            onToggleTheme()
            return true
        } else if (id == R.id.set_sort_date) { // MessageList
            messageListFragment!!.changeSort(SortType.SORT_DATE)
            return true
        } else if (id == R.id.set_sort_arrival) {
            messageListFragment!!.changeSort(SortType.SORT_ARRIVAL)
            return true
        } else if (id == R.id.set_sort_subject) {
            messageListFragment!!.changeSort(SortType.SORT_SUBJECT)
            return true
        } else if (id == R.id.set_sort_sender) {
            messageListFragment!!.changeSort(SortType.SORT_SENDER)
            return true
        } else if (id == R.id.set_sort_flag) {
            messageListFragment!!.changeSort(SortType.SORT_FLAGGED)
            return true
        } else if (id == R.id.set_sort_unread) {
            messageListFragment!!.changeSort(SortType.SORT_UNREAD)
            return true
        } else if (id == R.id.set_sort_attach) {
            messageListFragment!!.changeSort(SortType.SORT_ATTACHMENT)
            return true
        } else if (id == R.id.select_all) {
            messageListFragment!!.selectAll()
            return true
        } else if (id == R.id.search_remote) {
            messageListFragment!!.onRemoteSearch()
            return true
        } else if (id == R.id.search_everywhere) {
            searchEverywhere()
            return true
        } else if (id == R.id.mark_all_as_read) {
            messageListFragment!!.confirmMarkAllAsRead()
            return true
        } else if (id == R.id.next_message) { // MessageViewPager
            messageViewPagerFragment!!.showNextMessage()
            return true
        } else if (id == R.id.previous_message) {
            messageViewPagerFragment!!.showPreviousMessage()
            return true
        } else if (id == R.id.delete) {
            messageViewFragment?.onDelete()
            return true
        } else if (id == R.id.reply) {
            messageViewFragment?.onReply()
            return true
        } else if (id == R.id.reply_all) {
            messageViewFragment?.onReplyAll()
            return true
        } else if (id == R.id.forward) {
            messageViewFragment?.onForward()
            return true
        } else if (id == R.id.forward_as_attachment) {
            messageViewFragment?.onForwardAsAttachment()
            return true
        } else if (id == R.id.edit_as_new_message) {
            messageViewFragment?.onEditAsNewMessage()
            return true
        } else if (id == R.id.share) {
            messageViewFragment?.onSendAlternate()
            return true
        } else if (id == R.id.toggle_unread) {
            messageViewFragment?.onToggleRead()
            return true
        } else if (id == R.id.archive || id == R.id.refile_archive) {
            messageViewFragment?.onArchive()
            return true
        } else if (id == R.id.spam || id == R.id.refile_spam) {
            messageViewFragment?.onSpam()
            return true
        } else if (id == R.id.move || id == R.id.refile_move) {
            messageViewFragment?.onMove()
            return true
        } else if (id == R.id.copy || id == R.id.refile_copy) {
            messageViewFragment?.onCopy()
            return true
        } else if (id == R.id.move_to_drafts) {
            messageViewFragment?.onMoveToDrafts()
            return true
        } else if (id == R.id.show_headers) {
            startActivity(
                MessageSourceActivity.createLaunchIntent(
                    this,
                    messageViewFragment!!.messageReference
                )
            ) // TODO: !!
            return true
        }

        if (!singleFolderMode) {
            // None of the options after this point are "safe" for search results
            // TODO: This is not true for "unread" and "starred" searches in regular folders
            return false
        }

        return when (id) {
            R.id.send_messages -> {
                messageListFragment!!.onSendPendingMessages()
                true
            }
            R.id.expunge -> {
                messageListFragment!!.onExpunge()
                true
            }
            R.id.empty_trash -> {
                messageListFragment!!.onEmptyTrash()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun searchEverywhere() {
        val searchIntent = Intent(this, Search::class.java).apply {
            action = Intent.ACTION_SEARCH
            putExtra(SearchManager.QUERY, intent.getStringExtra(SearchManager.QUERY))
        }
        onNewIntent(searchIntent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.message_list_option, menu)
        this.menu = menu

        // setup search view
        val searchItem = menu.findItem(R.id.search)
        searchView = searchItem.actionView as SearchView
        searchView.maxWidth = Int.MAX_VALUE
        searchView.queryHint = resources.getString(R.string.search_action)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                messageListFragment?.onSearchRequested(query)
                return true
            }

            override fun onQueryTextChange(s: String): Boolean {
                return false
            }
        })

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        configureMenu(menu)
        return true
    }

    /**
     * Hide menu items not appropriate for the current context.
     *
     * **Note:**
     * Please adjust the comments in `res/menu/message_list_option.xml` if you change the  visibility of a menu item
     * in this method.
     */
    private fun configureMenu(menu: Menu?) {
        if (menu == null) return

        // Set visibility of menu items related to the message view
        if (displayMode == DisplayMode.MESSAGE_LIST || messageViewPagerFragment == null ||
            messageViewPagerFragment!!.activeMessageViewFragment == null ||
            !messageViewPagerFragment!!.activeMessageViewFragment!!.isInitialized
        ) {
            menu.findItem(R.id.next_message).isVisible = false
            menu.findItem(R.id.previous_message).isVisible = false
            menu.findItem(R.id.single_message_options).isVisible = false
            menu.findItem(R.id.delete).isVisible = false
            menu.findItem(R.id.compose).isVisible = false
            menu.findItem(R.id.archive).isVisible = false
            menu.findItem(R.id.move).isVisible = false
            menu.findItem(R.id.copy).isVisible = false
            menu.findItem(R.id.spam).isVisible = false
            menu.findItem(R.id.refile).isVisible = false
            menu.findItem(R.id.toggle_unread).isVisible = false
            menu.findItem(R.id.toggle_message_view_theme).isVisible = false
            menu.findItem(R.id.show_headers).isVisible = false
        } else {
            // hide prev/next buttons in split mode
            if (displayMode != DisplayMode.MESSAGE_VIEW) {
                menu.findItem(R.id.next_message).isVisible = false
                menu.findItem(R.id.previous_message).isVisible = false
            } else {
                val ref = messageViewPagerFragment!!.activeMessageViewFragment!!.messageReference
                val initialized = messageListFragment != null &&
                    messageListFragment!!.isLoadFinished
                val canDoPrev = initialized && !messageListFragment!!.isFirst(ref)
                val canDoNext = initialized && !messageListFragment!!.isLast(ref)
                val prev = menu.findItem(R.id.previous_message)
                prev.isEnabled = canDoPrev
                prev.icon.alpha = if (canDoPrev) 255 else 127
                val next = menu.findItem(R.id.next_message)
                next.isEnabled = canDoNext
                next.icon.alpha = if (canDoNext) 255 else 127
            }

            val toggleTheme = menu.findItem(R.id.toggle_message_view_theme)
            if (K9.isFixedMessageViewTheme) {
                toggleTheme.isVisible = false
            } else {
                // Set title of menu item to switch to dark/light theme
                if (themeManager.messageViewTheme === Theme.DARK) {
                    toggleTheme.setTitle(R.string.message_view_theme_action_light)
                } else {
                    toggleTheme.setTitle(R.string.message_view_theme_action_dark)
                }
                toggleTheme.isVisible = true
            }

            if (messageViewPagerFragment!!.activeMessageViewFragment!!.isOutbox) {
                menu.findItem(R.id.toggle_unread).isVisible = false
            } else {
                // Set title of menu item to toggle the read state of the currently displayed message
                val drawableAttr = if (messageViewPagerFragment!!.activeMessageViewFragment!!.isMessageRead) {
                    menu.findItem(R.id.toggle_unread).setTitle(R.string.mark_as_unread_action)
                    intArrayOf(R.attr.iconActionMarkAsUnread)
                } else {
                    menu.findItem(R.id.toggle_unread).setTitle(R.string.mark_as_read_action)
                    intArrayOf(R.attr.iconActionMarkAsRead)
                }
                val typedArray = obtainStyledAttributes(drawableAttr)
                menu.findItem(R.id.toggle_unread).icon = typedArray.getDrawable(0)
                typedArray.recycle()
            }

            menu.findItem(R.id.delete).isVisible = K9.isMessageViewDeleteActionVisible

            // Set visibility of copy, move, archive, spam in action bar and refile submenu
            if (messageViewPagerFragment!!.activeMessageViewFragment!!.isCopyCapable) {
                menu.findItem(R.id.copy).isVisible = K9.isMessageViewCopyActionVisible
                menu.findItem(R.id.refile_copy).isVisible = true
            } else {
                menu.findItem(R.id.copy).isVisible = false
                menu.findItem(R.id.refile_copy).isVisible = false
            }

            if (messageViewPagerFragment!!.activeMessageViewFragment!!.isMoveCapable) {
                val canMessageBeArchived = messageViewPagerFragment!!.activeMessageViewFragment!!.canMessageBeArchived()
                val canMessageBeMovedToSpam = messageViewPagerFragment!!.activeMessageViewFragment!!.canMessageBeMovedToSpam()

                menu.findItem(R.id.move).isVisible = K9.isMessageViewMoveActionVisible
                menu.findItem(R.id.archive).isVisible = canMessageBeArchived && K9.isMessageViewArchiveActionVisible
                menu.findItem(R.id.spam).isVisible = canMessageBeMovedToSpam && K9.isMessageViewSpamActionVisible

                menu.findItem(R.id.refile_move).isVisible = true
                menu.findItem(R.id.refile_archive).isVisible = canMessageBeArchived
                menu.findItem(R.id.refile_spam).isVisible = canMessageBeMovedToSpam
            } else {
                menu.findItem(R.id.move).isVisible = false
                menu.findItem(R.id.archive).isVisible = false
                menu.findItem(R.id.spam).isVisible = false

                menu.findItem(R.id.refile).isVisible = false
            }

            if (messageViewPagerFragment!!.activeMessageViewFragment!!.isOutbox) {
                menu.findItem(R.id.move_to_drafts).isVisible = true
            }
        }

        // Set visibility of menu items related to the message list

        // Hide search menu items by default and enable one when appropriate
        menu.findItem(R.id.search).isVisible = false
        menu.findItem(R.id.search_remote).isVisible = false
        menu.findItem(R.id.search_everywhere).isVisible = false

        if (displayMode == DisplayMode.MESSAGE_VIEW || messageListFragment == null ||
            !messageListFragment!!.isInitialized
        ) {
            menu.findItem(R.id.set_sort).isVisible = false
            menu.findItem(R.id.select_all).isVisible = false
            menu.findItem(R.id.send_messages).isVisible = false
            menu.findItem(R.id.expunge).isVisible = false
            menu.findItem(R.id.empty_trash).isVisible = false
            menu.findItem(R.id.mark_all_as_read).isVisible = false
        } else {
            menu.findItem(R.id.set_sort).isVisible = true
            menu.findItem(R.id.select_all).isVisible = true
            menu.findItem(R.id.compose).isVisible = true
            menu.findItem(R.id.mark_all_as_read).isVisible = messageListFragment!!.isMarkAllAsReadSupported

            if (!messageListFragment!!.isSingleAccountMode) {
                menu.findItem(R.id.expunge).isVisible = false
                menu.findItem(R.id.send_messages).isVisible = false
            } else {
                menu.findItem(R.id.send_messages).isVisible = messageListFragment!!.isOutbox
                menu.findItem(R.id.expunge).isVisible = messageListFragment!!.isRemoteFolder &&
                    messageListFragment!!.shouldShowExpungeAction()
            }
            menu.findItem(R.id.empty_trash).isVisible = messageListFragment!!.isShowingTrashFolder

            // If this is an explicit local search, show the option to search on the server
            if (!messageListFragment!!.isRemoteSearch && messageListFragment!!.isRemoteSearchAllowed) {
                menu.findItem(R.id.search_remote).isVisible = true
            } else if (!messageListFragment!!.isManualSearch) {
                menu.findItem(R.id.search).isVisible = true
            }

            val messageListFragment = messageListFragment!!
            if (messageListFragment.isManualSearch && !messageListFragment.localSearch.searchAllAccounts()) {
                menu.findItem(R.id.search_everywhere).isVisible = true
            }
        }
    }

    fun configureMenu() {
        configureMenu(menu)
    }

    protected fun onAccountUnavailable() {
        // TODO: Find better way to handle this case.
        Timber.i("Account is unavailable right now: $account")
        finish()
    }

    fun setActionBarTitle(title: String, subtitle: String? = null) {
        actionBar.title = title
        actionBar.subtitle = subtitle
    }

    override fun setMessageListTitle(title: String, subtitle: String?) {
        if (displayMode != DisplayMode.MESSAGE_VIEW) {
            setActionBarTitle(title, subtitle)
        }
    }

    override fun setMessageListProgressEnabled(enable: Boolean) {
        progressBar!!.visibility = if (enable) View.VISIBLE else View.INVISIBLE
    }

    override fun setMessageListProgress(progress: Int) {
        progressBar!!.progress = progress
    }

    override fun openMessage(messageReference: MessageReference) {
        val account = preferences.getAccount(messageReference.accountUuid) ?: error("Account not found")
        val folderId = messageReference.folderId

        val draftsFolderId = account.draftsFolderId
        if (draftsFolderId != null && folderId == draftsFolderId) {
            MessageActions.actionEditDraft(this, messageReference)
        } else {
            if (messageListFragment != null) {
                messageListFragment!!.setActiveMessage(messageReference)
            }

            // TODO: messageViewPagerFragment should never be null here!!
            if (messageViewPagerFragment != null) {
                messageViewPagerFragment!!.showMessage(messageReference)
            }

            if (displayMode != DisplayMode.SPLIT_VIEW) {
                showMessageView()
            }
        }
    }

    override fun onForward(messageReference: MessageReference, decryptionResultForReply: Parcelable?) {
        MessageActions.actionForward(this, messageReference, decryptionResultForReply)
    }

    override fun onForwardAsAttachment(messageReference: MessageReference, decryptionResultForReply: Parcelable?) {
        MessageActions.actionForwardAsAttachment(this, messageReference, decryptionResultForReply)
    }

    override fun onEditAsNewMessage(messageReference: MessageReference) {
        MessageActions.actionEditDraft(this, messageReference)
    }

    override fun onReply(messageReference: MessageReference, decryptionResultForReply: Parcelable?) {
        MessageActions.actionReply(this, messageReference, false, decryptionResultForReply)
    }

    override fun onReplyAll(messageReference: MessageReference, decryptionResultForReply: Parcelable?) {
        MessageActions.actionReply(this, messageReference, true, decryptionResultForReply)
    }

    override fun onCompose(account: Account?) {
        MessageActions.actionCompose(this, account)
    }

    override fun onBackStackChanged() {
        findFragments()
        if (isDrawerEnabled && !isAdditionalMessageListDisplayed) {
            unlockDrawer()
        }

        if (displayMode == DisplayMode.SPLIT_VIEW) {
            showMessageViewPlaceHolder()
        }

        configureMenu(menu)
    }

    private fun addMessageListFragment(fragment: MessageListFragment, addToBackStack: Boolean) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        fragmentTransaction.replace(R.id.message_list_container, fragment)
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(null)
        }

        messageListFragment = fragment

        if (isDrawerEnabled) {
            lockDrawer()
        }

        val transactionId = fragmentTransaction.commit()
        if (transactionId >= 0 && firstBackStackId < 0) {
            firstBackStackId = transactionId
        }
    }

    override fun startSearch(query: String, account: Account?, folderId: Long?): Boolean {
        // If this search was started from a MessageList of a single folder, pass along that folder info
        // so that we can enable remote search.
        val appData = if (account != null && folderId != null) {
            Bundle().apply {
                putString(EXTRA_SEARCH_ACCOUNT, account.uuid)
                putLong(EXTRA_SEARCH_FOLDER, folderId)
            }
        } else {
            // TODO Handle the case where we're searching from within a search result.
            null
        }
        val searchIntent = Intent(this, Search::class.java).apply {
            action = Intent.ACTION_SEARCH
            putExtra(SearchManager.QUERY, query)
            putExtra(SearchManager.APP_DATA, appData)
        }
        startActivity(searchIntent)

        return true
    }

    override fun showThread(account: Account, threadRootId: Long) {
        showMessageViewPlaceHolder()

        val tmpSearch = LocalSearch().apply {
            addAccountUuid(account.uuid)
            and(SearchField.THREAD_ID, threadRootId.toString(), SearchSpecification.Attribute.EQUALS)
        }

        initializeFromLocalSearch(tmpSearch)

        val fragment = MessageListFragment.newInstance(tmpSearch, true, false)
        addMessageListFragment(fragment, true)
    }

    private fun showMessageViewPlaceHolder() {
        removeMessageViewPagerFragment()

        // Add placeholder fragment if necessary
        val fragmentManager = supportFragmentManager
        if (fragmentManager.findFragmentByTag(FRAGMENT_TAG_PLACEHOLDER) == null) {
            val fragmentTransaction = fragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.message_view_container, messageViewPlaceHolder!!, FRAGMENT_TAG_PLACEHOLDER)
            fragmentTransaction.commit()
        }

        messageListFragment!!.setActiveMessage(null)
    }

    private fun removeMessageViewPagerFragment() {
        if (messageViewPagerFragment != null) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.remove(messageViewPagerFragment!!)
            messageViewPagerFragment = null
            fragmentTransaction.commit()

            showDefaultTitleView()
        }
    }

    private fun removeMessageListFragment() {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.remove(messageListFragment!!)
        messageListFragment = null
        fragmentTransaction.commit()
    }

    override fun remoteSearchStarted() {
        // Remove action button for remote search
        configureMenu(menu)
    }

    override fun goBack() {
        val fragmentManager = supportFragmentManager
        when {
            displayMode == DisplayMode.MESSAGE_VIEW -> showMessageList()
            fragmentManager.backStackEntryCount > 0 -> fragmentManager.popBackStack()
            else -> finish()
        }
    }

    override fun showNextMessageOrReturn() {
        if (K9.isMessageViewReturnToList || !showLogicalNextMessage()) {
            if (displayMode == DisplayMode.SPLIT_VIEW) {
                showMessageViewPlaceHolder()
            } else {
                showMessageList()
            }
        }
    }

    private fun showLogicalNextMessage(): Boolean {
        var result = false
        if (lastDirection == NEXT) {
            result = showNextMessage()
        } else if (lastDirection == PREVIOUS) {
            result = showPreviousMessage()
        }

        if (!result) {
            result = showNextMessage() || showPreviousMessage()
        }

        return result
    }

    override fun setProgress(enable: Boolean) {
        setProgressBarIndeterminateVisibility(enable)
    }

    private fun showNextMessage(): Boolean {
        val ref = messageViewPagerFragment!!.activeMessageViewFragment?.messageReference
        if (ref != null) {
            if (messageListFragment!!.openNext(ref)) {
                lastDirection = NEXT
                return true
            }
        }
        return false
    }

    private fun showPreviousMessage(): Boolean {
        val ref = messageViewPagerFragment!!.activeMessageViewFragment?.messageReference
        if (ref != null) {
            if (messageListFragment!!.openPrevious(ref)) {
                lastDirection = PREVIOUS
                return true
            }
        }
        return false
    }

    private fun showMessageList() {
        messageListWasDisplayed = true
        displayMode = DisplayMode.MESSAGE_LIST
        viewSwitcher!!.showFirstView()

        messageListFragment!!.setActiveMessage(null)

        setDrawerLockState()

        showDefaultTitleView()
        configureMenu(menu)
    }

    private fun setDrawerLockState() {
        if (!isDrawerEnabled) return

        if (isAdditionalMessageListDisplayed) {
            lockDrawer()
        } else {
            unlockDrawer()
        }
    }

    private fun showMessageView() {
        displayMode = DisplayMode.MESSAGE_VIEW

        if (!messageListWasDisplayed) {
            viewSwitcher!!.animateFirstView = false
        }
        viewSwitcher!!.showSecondView()

        if (isDrawerEnabled) {
            lockDrawer()
        }

        showMessageTitleView()
        configureMenu(menu)
    }

    override fun updateMenu() {
        invalidateOptionsMenu()
    }

    override fun disableDeleteAction() {
        menu!!.findItem(R.id.delete).isEnabled = false
    }

    private fun onToggleTheme() {
        themeManager.toggleMessageViewTheme()
        recreate()
    }

    private fun showDefaultTitleView() {
        if (messageListFragment != null) {
            messageListFragment!!.updateTitle()
        }
    }

    private fun showMessageTitleView() {
        setActionBarTitle("")
    }

    override fun onSwitchComplete(displayedChild: Int) {
        if (displayedChild == 0) {
//            removeMessageViewPagerFragment()
        }
    }

    override fun startIntentSenderForResult(
        intent: IntentSender,
        requestCode: Int,
        fillInIntent: Intent?,
        flagsMask: Int,
        flagsValues: Int,
        extraFlags: Int
    ) {
        // If any of the high 16 bits are set it is not one of our request codes
        if (requestCode and REQUEST_CODE_MASK != 0) {
            super.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags)
            return
        }

        val modifiedRequestCode = requestCode or REQUEST_FLAG_PENDING_INTENT
        super.startIntentSenderForResult(intent, modifiedRequestCode, fillInIntent, flagsMask, flagsValues, extraFlags)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // If any of the high 16 bits are set it is not one of our request codes
        if (requestCode and REQUEST_CODE_MASK != 0) return

        if (requestCode and REQUEST_FLAG_PENDING_INTENT != 0) {
            val originalRequestCode = requestCode xor REQUEST_FLAG_PENDING_INTENT
            if (messageViewPagerFragment != null) {
                messageViewPagerFragment!!.activeMessageViewFragment
                    ?.onPendingIntentResult(originalRequestCode, resultCode, data) // TODO ?.
            }
        }
    }

    private val isAdditionalMessageListDisplayed: Boolean
        get() = supportFragmentManager.backStackEntryCount > 0

    private fun lockDrawer() {
        drawer!!.lock()
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
    }

    private fun unlockDrawer() {
        drawer!!.unlock()
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu)
    }

    private fun initializeFromLocalSearch(search: LocalSearch?) {
        this.search = search
        singleFolderMode = false

        if (!search!!.searchAllAccounts()) {
            val accountUuids = search.accountUuids
            if (accountUuids.size == 1) {
                account = preferences.getAccount(accountUuids[0])
                val folderIds = search.folderIds
                singleFolderMode = folderIds.size == 1
            } else {
                account = null
            }
        }

        configureDrawer()
    }

    private fun LocalSearch.firstAccount(): Account? {
        return if (searchAllAccounts()) {
            preferences.defaultAccount
        } else {
            val accountUuid = accountUuids.first()
            preferences.getAccount(accountUuid)
        }
    }

    private val LocalSearch.isUnifiedInbox: Boolean
        get() = id == SearchAccount.UNIFIED_INBOX

    private fun MessageReference.toLocalSearch(): LocalSearch {
        return LocalSearch().apply {
            addAccountUuid(accountUuid)
            addAllowedFolder(folderId)
        }
    }

    private fun configureDrawer() {
        val drawer = drawer ?: return
        drawer.selectAccount(account!!.uuid)
        when {
            singleFolderMode -> drawer.selectFolder(search!!.folderIds[0])
            // Don't select any item in the drawer because the Unified Inbox is displayed, but not listed in the drawer
            search!!.id == SearchAccount.UNIFIED_INBOX && !K9.isShowUnifiedInbox -> drawer.deselect()
            search!!.id == SearchAccount.UNIFIED_INBOX -> drawer.selectUnifiedInbox()
            else -> drawer.deselect()
        }
    }

    override fun hasPermission(permission: Permission): Boolean {
        return permissionUiHelper.hasPermission(permission)
    }

    override fun requestPermissionOrShowRationale(permission: Permission) {
        permissionUiHelper.requestPermissionOrShowRationale(permission)
    }

    override fun requestPermission(permission: Permission) {
        permissionUiHelper.requestPermission(permission)
    }

    fun getMessageCount(): Int {
        if (messageListFragment != null) {
            return messageListFragment!!.getCount()
        }
        throw UninitializedPropertyAccessException()
    }

    fun getMessagePosition(reference: MessageReference): Int {
        if (messageListFragment != null) {
            return messageListFragment!!.getPosition(reference)
        }
        throw UninitializedPropertyAccessException()
    }

    fun getMessageReference(position: Int): MessageReference {
        if (messageListFragment != null) {
            return messageListFragment!!.getReferenceForPosition(position)
        }
        throw UninitializedPropertyAccessException()
    }

    private inner class StorageListenerImplementation : StorageListener {
        override fun onUnmount(providerId: String) {
            if (account?.localStorageProviderId == providerId) {
                runOnUiThread { onAccountUnavailable() }
            }
        }

        override fun onMount(providerId: String) = Unit
    }

    private enum class DisplayMode {
        MESSAGE_LIST, MESSAGE_VIEW, SPLIT_VIEW
    }

    private class LaunchData(
        val search: LocalSearch,
        val messageReference: MessageReference? = null,
        val noThreading: Boolean = false
    )

    companion object : KoinComponent {
        private const val EXTRA_SEARCH = "search_bytes"
        private const val EXTRA_NO_THREADING = "no_threading"

        private const val ACTION_SHORTCUT = "shortcut"
        private const val EXTRA_SPECIAL_FOLDER = "special_folder"

        private const val EXTRA_MESSAGE_REFERENCE = "message_reference"

        // used for remote search
        const val EXTRA_SEARCH_ACCOUNT = "com.fsck.k9.search_account"
        private const val EXTRA_SEARCH_FOLDER = "com.fsck.k9.search_folder"

        private const val STATE_DISPLAY_MODE = "displayMode"
        private const val STATE_MESSAGE_LIST_WAS_DISPLAYED = "messageListWasDisplayed"
        private const val STATE_FIRST_BACK_STACK_ID = "firstBackstackId"

        private const val FRAGMENT_TAG_MESSAGE_VIEW = "MessageViewFragment"
        private const val FRAGMENT_TAG_PLACEHOLDER = "MessageViewPlaceholder"

        // Used for navigating to next/previous message
        private const val PREVIOUS = 1
        private const val NEXT = 2

        private const val REQUEST_CODE_MASK = 0xFFFF0000.toInt()
        private const val REQUEST_FLAG_PENDING_INTENT = 1 shl 15

        private val defaultFolderProvider: DefaultFolderProvider by inject()

        @JvmStatic
        @JvmOverloads
        fun actionDisplaySearch(
            context: Context,
            search: SearchSpecification?,
            noThreading: Boolean,
            newTask: Boolean,
            clearTop: Boolean = true
        ) {
            context.startActivity(intentDisplaySearch(context, search, noThreading, newTask, clearTop))
        }

        @JvmStatic
        fun intentDisplaySearch(
            context: Context?,
            search: SearchSpecification?,
            noThreading: Boolean,
            newTask: Boolean,
            clearTop: Boolean
        ): Intent {
            return Intent(context, MessageList::class.java).apply {
                putExtra(EXTRA_SEARCH, ParcelableUtil.marshall(search))
                putExtra(EXTRA_NO_THREADING, noThreading)

                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

                if (clearTop) addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                if (newTask) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        @JvmStatic
        fun shortcutIntent(context: Context?, specialFolder: String?): Intent {
            return Intent(context, MessageList::class.java).apply {
                action = ACTION_SHORTCUT
                putExtra(EXTRA_SPECIAL_FOLDER, specialFolder)

                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        @JvmStatic
        fun shortcutIntentForAccount(context: Context?, account: Account): Intent {
            val folderId = defaultFolderProvider.getDefaultFolder(account)

            val search = LocalSearch().apply {
                addAccountUuid(account.uuid)
                addAllowedFolder(folderId)
            }

            return intentDisplaySearch(context, search, noThreading = false, newTask = true, clearTop = true)
        }

        @JvmStatic
        fun actionDisplayMessageIntent(context: Context?, messageReference: MessageReference): Intent {
            return Intent(context, MessageList::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EXTRA_MESSAGE_REFERENCE, messageReference.toIdentityString())
            }
        }

        @JvmStatic
        fun launch(context: Context) {
            val intent = Intent(context, MessageList::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }

            context.startActivity(intent)
        }

        @JvmStatic
        fun launch(context: Context, account: Account) {
            val folderId = defaultFolderProvider.getDefaultFolder(account)

            val search = LocalSearch().apply {
                addAllowedFolder(folderId)
                addAccountUuid(account.uuid)
            }

            actionDisplaySearch(context, search, noThreading = false, newTask = false)
        }
    }
}
