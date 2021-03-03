package com.fsck.k9.ui

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.activity.MessageList
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.controller.SimpleMessagingListener
import com.fsck.k9.helper.Contacts
import com.fsck.k9.mailstore.DisplayFolder
import com.fsck.k9.mailstore.Folder
import com.fsck.k9.ui.account.AccountsViewModel
import com.fsck.k9.ui.base.Theme
import com.fsck.k9.ui.base.ThemeManager
import com.fsck.k9.ui.folders.FolderIconProvider
import com.fsck.k9.ui.folders.FolderNameFormatter
import com.fsck.k9.ui.folders.FoldersViewModel
import com.fsck.k9.ui.settings.SettingsActivity
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import com.mikepenz.iconics.utils.backgroundColorInt
import com.mikepenz.iconics.utils.colorRes
import com.mikepenz.iconics.utils.paddingDp
import com.mikepenz.iconics.utils.sizeDp
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.holder.ImageHolder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.badgeText
import com.mikepenz.materialdrawer.model.interfaces.descriptionText
import com.mikepenz.materialdrawer.model.interfaces.iconDrawable
import com.mikepenz.materialdrawer.model.interfaces.iconRes
import com.mikepenz.materialdrawer.model.interfaces.nameRes
import com.mikepenz.materialdrawer.model.interfaces.nameText
import com.mikepenz.materialdrawer.model.interfaces.selectedColorInt
import com.mikepenz.materialdrawer.util.addItems
import com.mikepenz.materialdrawer.util.addStickyFooterItem
import com.mikepenz.materialdrawer.util.getDrawerItem
import com.mikepenz.materialdrawer.util.removeAllItems
import com.mikepenz.materialdrawer.widget.AccountHeaderView
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView
import java.util.ArrayList
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.parameter.parametersOf

class K9Drawer(private val parent: MessageList, savedInstanceState: Bundle?) : KoinComponent {
    private val foldersViewModel: FoldersViewModel by parent.viewModel()
    private val accountsViewModel: AccountsViewModel by parent.viewModel()
    private val folderNameFormatter: FolderNameFormatter by inject { parametersOf(parent) }
    private val themeManager: ThemeManager by inject()
    private val resources: Resources by inject()
    private val messagingController: MessagingController by inject()

    private val drawer: DrawerLayout = parent.findViewById(R.id.drawerLayout)
    private val sliderView: MaterialDrawerSliderView = parent.findViewById(R.id.material_drawer_slider)
    private val headerView: AccountHeaderView = AccountHeaderView(parent).apply {
        attachToSliderView(this@K9Drawer.sliderView)
        dividerBelowHeader = false
    }
    private val folderIconProvider: FolderIconProvider = FolderIconProvider(parent.theme)
    private val swipeRefreshLayout: SwipeRefreshLayout

    private val userFolderDrawerIds = ArrayList<Long>()
    private var unifiedInboxSelected: Boolean = false
    private val textColor: Int
    private var selectedTextColor: ColorStateList? = null
    private var selectedBackgroundColor: Int = 0
    private var folderBadgeStyle: BadgeStyle? = null
    private var openedFolderId: Long? = null

    val layout: DrawerLayout
        get() = drawer

    val isOpen: Boolean
        get() = drawer.isOpen

    init {
        textColor = parent.obtainDrawerTextColor()

        configureAccountHeader()

        drawer.addDrawerListener(parent.createDrawerListener())
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

        accountsViewModel.accountsLiveData.observeNotNull(parent) { accounts ->
            setAccounts(accounts)
        }

        foldersViewModel.getFolderListLiveData().observe(parent) { folders ->
            setUserFolders(folders)
        }
    }

    private fun configureAccountHeader() {
        headerView.headerBackground = ImageHolder(R.drawable.drawer_header_background)

        headerView.onAccountHeaderListener = { _, profile, _ ->
            val account = (profile as ProfileDrawerItem).tag as Account
            parent.openRealAccount(account)
            updateUserAccountsAndFolders(account)
            true
        }
    }

    private fun setAccounts(accounts: List<Account>) {
        val photoUris = mutableSetOf<Uri>()

        val selectedAccountUuid = (headerView.activeProfile?.tag as? Account)?.uuid
        val oldSelectedBackgroundColor = selectedBackgroundColor

        val accountItems = accounts.map { account ->
            val drawerId = (account.accountNumber + 1 shl DRAWER_ACCOUNT_SHIFT).toLong()

            val drawerColors = getDrawerColorsForAccount(account)
            val selectedTextColor = drawerColors.accentColor.toSelectedColorStateList()

            val accountItem = ProfileDrawerItem().apply {
                isNameShown = true
                nameText = account.description
                descriptionText = account.email
                identifier = drawerId
                tag = account
                textColor = selectedTextColor
                descriptionTextColor = selectedTextColor
                selectedColorInt = drawerColors.selectedColor
            }

            // TODO: Use DrawerImageLoader to load contact image in background
            val photoUri = Contacts.getInstance(parent).getPhotoUri(account.email)
            if (photoUri != null && photoUri !in photoUris) {
                photoUris.add(photoUri)
                accountItem.icon = ImageHolder(photoUri)
            } else {
                accountItem.iconDrawable = IconicsDrawable(parent, FontAwesome.Icon.faw_user_alt).apply {
                    colorRes = R.color.material_drawer_profile_icon
                    backgroundColorInt = account.chipColor
                    sizeDp = 56
                    paddingDp = 12
                }
            }

            if (account.uuid == selectedAccountUuid) {
                initializeWithAccountColor(account)
            }

            accountItem
        }.toTypedArray()

        headerView.clear()
        headerView.addProfiles(*accountItems)

        if (oldSelectedBackgroundColor != selectedBackgroundColor) {
            // Recreate list of folders with updated account color
            setUserFolders(foldersViewModel.getFolderListLiveData().value)
        }
    }

    private fun addFooterItems() {
        sliderView.addStickyFooterItem(
            PrimaryDrawerItem().apply {
                nameRes = R.string.folders_action
                iconRes = folderIconProvider.iconFolderResId
                identifier = DRAWER_ID_FOLDERS
                isSelectable = false
            }
        )

        sliderView.addStickyFooterItem(
            PrimaryDrawerItem().apply {
                nameRes = R.string.preferences_action
                iconRes = getResId(R.attr.iconActionSettings)
                identifier = DRAWER_ID_PREFERENCES
                isSelectable = false
            }
        )
    }

    private fun getResId(resAttribute: Int): Int {
        val typedValue = TypedValue()
        val found = parent.theme.resolveAttribute(resAttribute, typedValue, true)
        if (!found) {
            throw AssertionError("Couldn't find resource with attribute $resAttribute")
        }
        return typedValue.resourceId
    }

    private fun getFolderDisplayName(folder: Folder): String {
        return folderNameFormatter.displayName(folder)
    }

    fun updateUserAccountsAndFolders(account: Account?) {
        if (account != null) {
            initializeWithAccountColor(account)
            headerView.setActiveProfile((account.accountNumber + 1 shl DRAWER_ACCOUNT_SHIFT).toLong())
            foldersViewModel.loadFolders(account)
        }

        // Account can be null to refresh all (unified inbox or account list).
        swipeRefreshLayout.setOnRefreshListener {
            val accountToRefresh = if (headerView.selectionListShown) null else account
            messagingController.checkMail(
                accountToRefresh, true, true,
                object : SimpleMessagingListener() {
                    override fun checkMailFinished(context: Context?, account: Account?) {
                        swipeRefreshLayout.post {
                            swipeRefreshLayout.isRefreshing = false
                        }
                    }
                }
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
            DRAWER_ID_PREFERENCES -> SettingsActivity.launch(parent)
            DRAWER_ID_FOLDERS -> parent.launchManageFoldersScreen()
            DRAWER_ID_UNIFIED_INBOX -> parent.openUnifiedInbox()
            else -> {
                val folder = drawerItem.tag as Folder
                parent.openFolder(folder.id)
            }
        }
    }

    private fun setUserFolders(folders: List<DisplayFolder>?) {
        clearUserFolders()

        var openedFolderDrawerId: Long = -1

        if (K9.isShowUnifiedInbox) {
            val unifiedInboxItem = PrimaryDrawerItem().apply {
                iconRes = R.drawable.ic_inbox_multiple
                identifier = DRAWER_ID_UNIFIED_INBOX
                nameRes = R.string.integrated_inbox_title
                selectedColorInt = selectedBackgroundColor
                textColor = selectedTextColor
                isSelected = unifiedInboxSelected
            }

            sliderView.addItems(unifiedInboxItem)
            sliderView.addItems(FixedDividerDrawerItem(identifier = DRAWER_ID_DIVIDER))

            if (unifiedInboxSelected) {
                openedFolderDrawerId = DRAWER_ID_UNIFIED_INBOX
            }
        }

        if (folders == null) {
            return
        }

        for (displayFolder in folders) {
            val folder = displayFolder.folder
            val drawerId = folder.id shl DRAWER_FOLDER_SHIFT

            val drawerItem = PrimaryDrawerItem().apply {
                iconRes = folderIconProvider.getFolderIcon(folder.type)
                identifier = drawerId
                tag = folder
                nameText = getFolderDisplayName(folder)
                displayFolder.unreadCount.takeIf { it > 0 }?.let {
                    badgeText = it.toString()
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

    fun selectFolder(folderId: Long) {
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

    fun deselect() {
        unifiedInboxSelected = false
        openedFolderId = null
        sliderView.selectExtension.deselect()
    }

    fun selectUnifiedInbox() {
        headerView.selectionListShown = false
        deselect()
        unifiedInboxSelected = true
        sliderView.setSelection(DRAWER_ID_UNIFIED_INBOX, false)
    }

    private data class DrawerColors(
        val accentColor: Int,
        val selectedColor: Int
    )

    private fun getDrawerColorsForAccount(account: Account): DrawerColors {
        val baseColor = if (themeManager.appTheme == Theme.DARK) {
            getDarkThemeAccentColor(account.chipColor)
        } else {
            account.chipColor
        }
        return DrawerColors(
            accentColor = baseColor,
            selectedColor = baseColor.and(0xffffff).or(0x22000000)
        )
    }

    private fun getDarkThemeAccentColor(color: Int): Int {
        val lightColors = resources.getIntArray(R.array.account_colors)
        val darkColors = resources.getIntArray(R.array.drawer_account_accent_color_dark_theme)
        val index = lightColors.indexOf(color)
        return if (index == -1) color else darkColors[index]
    }

    fun open() {
        drawer.openDrawer(GravityCompat.START)
    }

    fun close() {
        drawer.closeDrawer(GravityCompat.START)
    }

    fun lock() {
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    fun unlock() {
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    private fun Int.toSelectedColorStateList(): ColorStateList {
        val states = arrayOf(
            intArrayOf(android.R.attr.state_selected),
            intArrayOf()
        )

        val colors = intArrayOf(
            this,
            textColor
        )

        return ColorStateList(states, colors)
    }

    companion object {
        // Bit shift for identifiers of user folders items, to leave space for other items
        private const val DRAWER_FOLDER_SHIFT: Int = 20
        private const val DRAWER_ACCOUNT_SHIFT: Int = 3

        private const val DRAWER_ID_UNIFIED_INBOX: Long = 0
        private const val DRAWER_ID_DIVIDER: Long = 1
        private const val DRAWER_ID_PREFERENCES: Long = 2
        private const val DRAWER_ID_FOLDERS: Long = 3

        private const val PROGRESS_VIEW_END_OFFSET = 32
        private const val PROGRESS_VIEW_SLINGSHOT_DISTANCE = 48
    }
}

private fun Context.obtainDrawerTextColor(): Int {
    val styledAttributes = obtainStyledAttributes(
        null,
        R.styleable.MaterialDrawerSliderView,
        R.attr.materialDrawerStyle,
        R.style.Widget_MaterialDrawerStyle
    )
    val textColor = styledAttributes.getColor(R.styleable.MaterialDrawerSliderView_materialDrawerPrimaryText, 0)
    styledAttributes.recycle()

    return textColor
}

private class FixedDividerDrawerItem(override var identifier: Long) : DividerDrawerItem()
