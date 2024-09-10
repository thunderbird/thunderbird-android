package app.k9mail.feature.navigation.drawer

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons
import app.k9mail.core.ui.theme.api.Theme
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount
import app.k9mail.feature.navigation.drawer.legacy.AccountsViewModel
import app.k9mail.feature.navigation.drawer.legacy.FoldersViewModel
import app.k9mail.legacy.account.Account
import app.k9mail.legacy.folder.DisplayFolder
import app.k9mail.legacy.folder.Folder
import app.k9mail.legacy.message.controller.MessagingControllerMailChecker
import app.k9mail.legacy.message.controller.SimpleMessagingListener
import app.k9mail.legacy.ui.account.AccountImageLoader
import app.k9mail.legacy.ui.folder.DisplayUnifiedInbox
import app.k9mail.legacy.ui.folder.FolderIconProvider
import app.k9mail.legacy.ui.folder.FolderList
import app.k9mail.legacy.ui.folder.FolderNameFormatter
import app.k9mail.legacy.ui.theme.ThemeManager
import com.fsck.k9.K9
import com.fsck.k9.ui.base.livedata.observeNotNull
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.holder.ImageHolder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IProfile
import com.mikepenz.materialdrawer.model.interfaces.badgeText
import com.mikepenz.materialdrawer.model.interfaces.descriptionText
import com.mikepenz.materialdrawer.model.interfaces.iconRes
import com.mikepenz.materialdrawer.model.interfaces.nameRes
import com.mikepenz.materialdrawer.model.interfaces.nameText
import com.mikepenz.materialdrawer.model.interfaces.selectedColorInt
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.mikepenz.materialdrawer.util.addItems
import com.mikepenz.materialdrawer.util.addStickyFooterItem
import com.mikepenz.materialdrawer.util.getDrawerItem
import com.mikepenz.materialdrawer.util.removeAllItems
import com.mikepenz.materialdrawer.widget.AccountHeaderView
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.fsck.k9.core.R as CoreR
import com.mikepenz.materialdrawer.R as MaterialDrawerR

private const val UNREAD_SYMBOL = "\u2B24"
private const val STARRED_SYMBOL = "\u2605"
private const val THIN_SPACE = "\u2009"
private const val EN_SPACE = "\u2000"

@Suppress("MagicNumber", "TooManyFunctions", "LongParameterList")
class LegacyDrawer(
    override val parent: AppCompatActivity,
    private val openFolders: () -> Unit,
    private val openUnifiedInbox: () -> Unit,
    private val openFolder: (folderId: Long) -> Unit,
    private val openAccount: (account: Account) -> Boolean,
    private val openSettings: () -> Unit,
    createDrawerListener: () -> DrawerLayout.DrawerListener,
    savedInstanceState: Bundle?,
) : NavigationDrawer, KoinComponent {
    private val foldersViewModel: FoldersViewModel by parent.viewModel()
    private val accountsViewModel: AccountsViewModel by parent.viewModel()
    private val folderNameFormatter: FolderNameFormatter by inject()
    private val themeManager: ThemeManager by inject()
    private val resources: Resources by inject()
    private val messagingController: MessagingControllerMailChecker by inject()
    private val accountImageLoader: AccountImageLoader by inject()
    private val folderIconProvider: FolderIconProvider by inject()

    private val drawer: DrawerLayout = parent.findViewById(R.id.navigation_drawer_layout)
    private val sliderView: MaterialDrawerSliderView = parent.findViewById(R.id.material_drawer_slider)
    private val composeView: View = parent.findViewById(R.id.material_drawer_compose_view)
    private val headerView: AccountHeaderView = AccountHeaderView(parent).apply {
        attachToSliderView(this@LegacyDrawer.sliderView)
        dividerBelowHeader = false
        displayBadgesOnCurrentProfileImage = false
    }
    private val swipeRefreshLayout: SwipeRefreshLayout

    private val userFolderDrawerIds = ArrayList<Long>()
    private var unifiedInboxSelected: Boolean = false
    private val textColor: Int
    private var selectedTextColor: ColorStateList? = null
    private var selectedBackgroundColor: Int = 0
    private var folderBadgeStyle: BadgeStyle? = null
    private var openedAccountUuid: String? = null
    private var openedFolderId: Long? = null
    private var latestFolderList: FolderList? = null

    override val isOpen: Boolean
        get() = drawer.isOpen

    init {
        composeView.visibility = View.GONE
        sliderView.visibility = View.VISIBLE

        textColor = parent.obtainDrawerTextColor()

        initializeImageLoader()
        configureAccountHeader()

        drawer.addDrawerListener(createDrawerListener())
        sliderView.tintStatusBar = true
        sliderView.onDrawerItemClickListener = { _, item, _ ->
            handleItemClickListener(item)
            false
        }
        sliderView.setSavedInstance(savedInstanceState)
        headerView.withSavedInstance(savedInstanceState)

        swipeRefreshLayout = parent.findViewById(R.id.material_drawer_swipe_refresh)
        headerView.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            val densityMultiplier = view.resources.displayMetrics.density
            val progressViewStart = view.measuredHeight
            val progressViewEnd = progressViewStart + (PROGRESS_VIEW_END_OFFSET * densityMultiplier).toInt()

            val progressViewStartOld = swipeRefreshLayout.progressViewStartOffset
            val progressViewEndOld = swipeRefreshLayout.progressViewEndOffset
            if (progressViewStart != progressViewStartOld || progressViewEnd != progressViewEndOld) {
                swipeRefreshLayout.setProgressViewOffset(true, progressViewStart, progressViewEnd)

                val slingshotDistance = (PROGRESS_VIEW_SLINGSHOT_DISTANCE * densityMultiplier).toInt()
                swipeRefreshLayout.setSlingshotDistance(slingshotDistance)
            }
        }

        addFooterItems()

        accountsViewModel.displayAccountsLiveData.observeNotNull(parent) { accounts ->
            setAccounts(accounts)
        }

        foldersViewModel.getFolderListLiveData().observe(parent) { folderList ->
            setUserFolders(folderList)
        }
    }

    private fun initializeImageLoader() {
        DrawerImageLoader.init(
            object : AbstractDrawerImageLoader() {
                override fun set(imageView: ImageView, uri: Uri, placeholder: Drawable, tag: String?) {
                    val email = uri.getQueryParameter(QUERY_EMAIL) ?: error("Missing '$QUERY_EMAIL' parameter in $uri")
                    val color = uri.getQueryParameter(QUERY_COLOR)?.toInt()
                        ?: error("Missing '$QUERY_COLOR' parameter in $uri")

                    accountImageLoader.setAccountImage(imageView, email, color)
                }

                override fun cancel(imageView: ImageView) {
                    accountImageLoader.cancel(imageView)
                }
            },
        ).apply {
            handledProtocols = listOf(INTERNAL_URI_SCHEME)
        }
    }

    private fun configureAccountHeader() {
        headerView.headerBackground = ImageHolder(R.drawable.navigation_drawer_header_background)

        headerView.onAccountHeaderListener = { _, profile, _ ->
            val account = (profile as ProfileDrawerItem).tag as Account
            openedAccountUuid = account.uuid
            val eventHandled = openAccount(account)
            updateUserAccountsAndFolders(account)

            eventHandled
        }
    }

    private fun buildBadgeText(displayAccount: DisplayAccount): String? {
        return buildBadgeText(displayAccount.unreadMessageCount, displayAccount.starredMessageCount)
    }

    private fun buildBadgeText(displayFolder: DisplayFolder): String? {
        return buildBadgeText(displayFolder.unreadMessageCount, displayFolder.starredMessageCount)
    }

    private fun buildBadgeText(unifiedInbox: DisplayUnifiedInbox): String? {
        return buildBadgeText(unifiedInbox.unreadMessageCount, unifiedInbox.starredMessageCount)
    }

    private fun buildBadgeText(unreadCount: Int, starredCount: Int): String? {
        return if (K9.isShowStarredCount) {
            buildBadgeTextWithStarredCount(unreadCount, starredCount)
        } else {
            buildBadgeTextWithUnreadCount(unreadCount)
        }
    }

    private fun buildBadgeTextWithStarredCount(unreadCount: Int, starredCount: Int): String? {
        if (unreadCount == 0 && starredCount == 0) return null

        return buildString {
            val hasUnreadCount = unreadCount > 0
            if (hasUnreadCount) {
                append(UNREAD_SYMBOL)
                append(THIN_SPACE)
                append(unreadCount)
            }

            if (starredCount > 0) {
                if (hasUnreadCount) {
                    append(EN_SPACE)
                }
                append(STARRED_SYMBOL)
                append(THIN_SPACE)
                append(starredCount)
            }
        }
    }

    private fun buildBadgeTextWithUnreadCount(unreadCount: Int): String? {
        return if (unreadCount > 0) unreadCount.toString() else null
    }

    @Suppress("SpreadOperator")
    private fun setAccounts(displayAccounts: List<DisplayAccount>) {
        val oldSelectedBackgroundColor = selectedBackgroundColor

        val isOpenedAccountStillAvailable = displayAccounts.any { it.account.uuid == openedAccountUuid }
        if (!isOpenedAccountStillAvailable) {
            openedAccountUuid = displayAccounts.first().account.uuid
        }

        var newActiveProfile: IProfile? = null
        val accountItems = displayAccounts.map { displayAccount ->
            val account = displayAccount.account

            val drawerColors = getDrawerColorsForAccount(account)
            val selectedTextColor = drawerColors.accentColor.toSelectedColorStateList()

            val accountItem = ProfileDrawerItem().apply {
                account.name.let { accountName ->
                    isNameShown = accountName != null
                    nameText = accountName.orEmpty()
                }
                descriptionText = account.email
                identifier = account.drawerId
                tag = account
                textColor = selectedTextColor
                descriptionTextColor = selectedTextColor
                selectedColorInt = drawerColors.selectedColor
                icon = ImageHolder(createAccountImageUri(account))
                buildBadgeText(displayAccount)?.let { text ->
                    badgeText = text
                    badgeStyle = BadgeStyle().apply {
                        textColorStateList = selectedTextColor
                    }
                }
            }

            if (account.uuid == openedAccountUuid) {
                initializeWithAccountColor(account)
                newActiveProfile = accountItem
            }

            accountItem
        }.toTypedArray()

        headerView.clear()
        headerView.addProfiles(*accountItems)

        newActiveProfile?.let { profile ->
            headerView.activeProfile = profile
        }

        if (oldSelectedBackgroundColor != selectedBackgroundColor) {
            // Recreate list of folders with updated account color
            setUserFolders(latestFolderList)
        }
    }

    private fun addFooterItems() {
        sliderView.addStickyFooterItem(
            PrimaryDrawerItem().apply {
                nameRes = R.string.navigation_drawer_action_folders
                iconRes = Icons.Outlined.Folder
                identifier = DRAWER_ID_FOLDERS
                isSelectable = false
            },
        )

        sliderView.addStickyFooterItem(
            PrimaryDrawerItem().apply {
                nameRes = R.string.navigation_drawer_action_settings
                iconRes = Icons.Outlined.Settings
                identifier = DRAWER_ID_PREFERENCES
                isSelectable = false
            },
        )
    }

    private fun getFolderDisplayName(folder: Folder): String {
        return folderNameFormatter.displayName(folder)
    }

    override fun updateUserAccountsAndFolders(account: Account?) {
        if (account != null) {
            initializeWithAccountColor(account)
            headerView.setActiveProfile(account.drawerId)
            foldersViewModel.loadFolders(account)
        }

        // Account can be null to refresh all (unified inbox or account list).
        swipeRefreshLayout.setOnRefreshListener {
            val accountToRefresh = if (headerView.selectionListShown) null else account
            messagingController.checkMail(
                account = accountToRefresh,
                ignoreLastCheckedTime = true,
                useManualWakeLock = true,
                notify = true,
                listener = object : SimpleMessagingListener() {
                    override fun checkMailFinished(context: Context?, account: Account?) {
                        swipeRefreshLayout.post {
                            swipeRefreshLayout.isRefreshing = false
                        }
                    }
                },
            )
        }
    }

    private fun initializeWithAccountColor(account: Account) {
        getDrawerColorsForAccount(account).let { drawerColors ->
            selectedBackgroundColor = drawerColors.selectedColor
            val selectedTextColor = drawerColors.accentColor.toSelectedColorStateList()
            this.selectedTextColor = selectedTextColor
            folderBadgeStyle = BadgeStyle().apply {
                textColorStateList = selectedTextColor
            }
        }
        headerView.accountHeaderBackground.setColorFilter(account.chipColor, PorterDuff.Mode.MULTIPLY)
    }

    private fun handleItemClickListener(drawerItem: IDrawerItem<*>) {
        when (drawerItem.identifier) {
            DRAWER_ID_PREFERENCES -> openSettings()
            DRAWER_ID_FOLDERS -> openFolders()
            DRAWER_ID_UNIFIED_INBOX -> openUnifiedInbox()
            else -> {
                val folder = drawerItem.tag as Folder
                openFolder(folder.id)
            }
        }
    }

    private fun setUserFolders(folderList: FolderList?) {
        this.latestFolderList = folderList
        clearUserFolders()

        var openedFolderDrawerId: Long = -1

        if (folderList == null) {
            return
        }

        folderList.unifiedInbox?.let { unifiedInbox ->
            val unifiedInboxItem = PrimaryDrawerItem().apply {
                iconRes = Icons.Outlined.AllInbox
                identifier = DRAWER_ID_UNIFIED_INBOX
                nameRes = R.string.navigation_drawer_unified_inbox_title
                selectedColorInt = selectedBackgroundColor
                textColor = selectedTextColor
                isSelected = unifiedInboxSelected
                buildBadgeText(unifiedInbox)?.let { text ->
                    badgeText = text
                    badgeStyle = folderBadgeStyle
                }
            }

            sliderView.addItems(unifiedInboxItem)
            sliderView.addItems(FixedDividerDrawerItem(identifier = DRAWER_ID_DIVIDER))

            if (unifiedInboxSelected) {
                openedFolderDrawerId = DRAWER_ID_UNIFIED_INBOX
            }
        }

        val accountOffset = folderList.accountId.toLong() shl DRAWER_ACCOUNT_SHIFT
        for (displayFolder in folderList.folders) {
            val folder = displayFolder.folder
            val drawerId = accountOffset + folder.id

            val drawerItem = FolderDrawerItem().apply {
                iconRes = folderIconProvider.getFolderIcon(folder.type)
                identifier = drawerId
                tag = folder
                nameText = getFolderDisplayName(folder)
                buildBadgeText(displayFolder)?.let { text ->
                    badgeText = text
                    badgeStyle = folderBadgeStyle
                }
                selectedColorInt = selectedBackgroundColor
                textColor = selectedTextColor
            }

            sliderView.addItems(drawerItem)
            userFolderDrawerIds.add(drawerId)
            if (folder.id == openedFolderId) {
                openedFolderDrawerId = drawerId
            }
        }

        if (openedFolderDrawerId != -1L) {
            sliderView.setSelection(openedFolderDrawerId, false)
        }
    }

    private fun clearUserFolders() {
        // remove old items first
        sliderView.selectExtension.deselect()
        sliderView.removeAllItems()
        userFolderDrawerIds.clear()
    }

    override fun selectAccount(accountUuid: String) {
        openedAccountUuid = accountUuid
        headerView.profiles?.firstOrNull { it.accountUuid == accountUuid }?.let { profile ->
            headerView.activeProfile = profile
        }
    }

    override fun selectFolder(folderId: Long) {
        deselect()
        openedFolderId = folderId
        for (drawerId in userFolderDrawerIds) {
            val folder = sliderView.getDrawerItem(drawerId)?.tag as? Folder
            if (folder?.id == folderId) {
                sliderView.setSelection(drawerId, false)
                return
            }
        }
    }

    override fun deselect() {
        unifiedInboxSelected = false
        openedFolderId = null
        sliderView.selectExtension.deselect()
    }

    override fun selectUnifiedInbox() {
        headerView.selectionListShown = false
        deselect()
        unifiedInboxSelected = true
        sliderView.setSelection(DRAWER_ID_UNIFIED_INBOX, false)
    }

    private data class DrawerColors(
        val accentColor: Int,
        val selectedColor: Int,
    )

    private fun getDrawerColorsForAccount(account: Account): DrawerColors {
        val baseColor = if (themeManager.appTheme == Theme.DARK) {
            getDarkThemeAccentColor(account.chipColor)
        } else {
            account.chipColor
        }
        return DrawerColors(
            accentColor = baseColor,
            selectedColor = baseColor.and(0xffffff).or(0x22000000),
        )
    }

    private fun getDarkThemeAccentColor(color: Int): Int {
        val lightColors = resources.getIntArray(CoreR.array.account_colors)
        val darkColors = resources.getIntArray(CoreR.array.drawer_account_accent_color_dark_theme)
        val index = lightColors.indexOf(color)
        return if (index == -1) color else darkColors[index]
    }

    override fun open() {
        drawer.openDrawer(GravityCompat.START)
    }

    override fun close() {
        drawer.closeDrawer(GravityCompat.START)
    }

    override fun lock() {
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    override fun unlock() {
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    private fun createAccountImageUri(account: Account): Uri {
        return Uri.parse("$INTERNAL_URI_SCHEME://account-image/")
            .buildUpon()
            .appendQueryParameter(QUERY_EMAIL, account.email)
            .appendQueryParameter(QUERY_COLOR, account.chipColor.toString())
            .build()
    }

    private fun Int.toSelectedColorStateList(): ColorStateList {
        val states = arrayOf(
            intArrayOf(android.R.attr.state_selected),
            intArrayOf(),
        )

        val colors = intArrayOf(
            this,
            textColor,
        )

        return ColorStateList(states, colors)
    }

    private val IProfile.accountUuid: String?
        get() = (this.tag as? Account)?.uuid

    private val Account.drawerId: Long
        get() = (accountNumber + 1).toLong()

    companion object {
        // Use the lower 48 bits for the folder ID, the upper bits for the account's drawer ID
        private const val DRAWER_ACCOUNT_SHIFT: Int = 48

        private const val DRAWER_ID_UNIFIED_INBOX: Long = 0
        private const val DRAWER_ID_DIVIDER: Long = 1
        private const val DRAWER_ID_PREFERENCES: Long = 2
        private const val DRAWER_ID_FOLDERS: Long = 3

        private const val PROGRESS_VIEW_END_OFFSET = 32
        private const val PROGRESS_VIEW_SLINGSHOT_DISTANCE = 48

        private const val INTERNAL_URI_SCHEME = "app-internal"
        private const val QUERY_EMAIL = "email"
        private const val QUERY_COLOR = "color"
    }
}

private fun Context.obtainDrawerTextColor(): Int {
    val styledAttributes = obtainStyledAttributes(
        null,
        MaterialDrawerR.styleable.MaterialDrawerSliderView,
        MaterialDrawerR.attr.materialDrawerStyle,
        MaterialDrawerR.style.Widget_MaterialDrawerStyle,
    )
    val textColor = styledAttributes.getColor(
        MaterialDrawerR.styleable.MaterialDrawerSliderView_materialDrawerPrimaryText,
        0,
    )
    styledAttributes.recycle()

    return textColor
}

private class FixedDividerDrawerItem(override var identifier: Long) : DividerDrawerItem()

// We ellipsize long folder names in the middle for better readability
private class FolderDrawerItem : PrimaryDrawerItem() {
    override val type: Int = R.id.navigation_drawer_legacy_list_folder_item
    override val layoutRes: Int = R.layout.navigation_drawer_legacy_list_folder_item
}
