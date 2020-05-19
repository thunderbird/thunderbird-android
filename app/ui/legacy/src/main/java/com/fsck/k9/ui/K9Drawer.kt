package com.fsck.k9.ui

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.activity.MessageList
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.controller.SimpleMessagingListener
import com.fsck.k9.helper.Contacts
import com.fsck.k9.mailstore.DisplayFolder
import com.fsck.k9.mailstore.Folder
import com.fsck.k9.ui.base.Theme
import com.fsck.k9.ui.base.ThemeManager
import com.fsck.k9.ui.folders.FolderIconProvider
import com.fsck.k9.ui.folders.FolderNameFormatter
import com.fsck.k9.ui.folders.FoldersViewModel
import com.fsck.k9.ui.settings.SettingsActivity
import com.mikepenz.iconics.IconicsColor
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.IconicsSize
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import com.mikepenz.iconics.utils.colorRes
import com.mikepenz.materialdrawer.AccountHeader
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.Drawer.OnDrawerItemClickListener
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IProfile
import java.util.ArrayList
import java.util.HashSet
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.parameter.parametersOf

class K9Drawer(private val parent: MessageList, savedInstanceState: Bundle?) : KoinComponent {
    private val viewModel: FoldersViewModel by parent.viewModel()
    private val folderNameFormatter: FolderNameFormatter by inject { parametersOf(parent) }
    private val preferences: Preferences by inject()
    private val themeManager: ThemeManager by inject()
    private val resources: Resources by inject()
    private val messagingController: MessagingController by inject()

    private val drawer: Drawer
    private val accountHeader: AccountHeader
    private val folderIconProvider: FolderIconProvider = FolderIconProvider(parent.theme)
    private val swipeRefreshLayout: SwipeRefreshLayout

    private val userFolderDrawerIds = ArrayList<Long>()
    private var unifiedInboxSelected: Boolean = false
    private var accentColor: Int = 0
    private var selectedColor: Int = 0
    private var openedFolderId: Long? = null

    val layout: DrawerLayout
        get() = drawer.drawerLayout

    val isOpen: Boolean
        get() = drawer.isDrawerOpen

    init {
        accountHeader = buildAccountHeader()

        drawer = DrawerBuilder()
                .withActivity(parent)
                .withOnDrawerItemClickListener(createItemClickListener())
                .withOnDrawerListener(parent.createOnDrawerListener())
                .withSavedInstance(savedInstanceState)
                .withAccountHeader(accountHeader)
                .build()

        swipeRefreshLayout = drawer.slider.findViewById(R.id.material_drawer_swipe_refresh)
        accountHeader.view.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            val densityMultiplier = view.resources.displayMetrics.density

            val progressViewStart = view.measuredHeight
            val progressViewEnd = progressViewStart + (PROGRESS_VIEW_END_OFFSET * densityMultiplier).toInt()
            swipeRefreshLayout.setProgressViewOffset(true, progressViewStart, progressViewEnd)

            val slingshotDistance = (PROGRESS_VIEW_SLINGSHOT_DISTANCE * densityMultiplier).toInt()
            swipeRefreshLayout.setSlingshotDistance(slingshotDistance)
        }

        addFooterItems()

        viewModel.getFolderListLiveData().observe(parent) { folders ->
            setUserFolders(folders)
        }
    }

    private fun buildAccountHeader(): AccountHeader {
        val headerBuilder = AccountHeaderBuilder()
                .withActivity(parent)
                .withHeaderBackground(R.drawable.drawer_header_background)

        if (!K9.isHideSpecialAccounts) {
            headerBuilder.addProfiles(ProfileDrawerItem()
                    .withNameShown(true)
                    .withName(R.string.integrated_inbox_title)
                    .withEmail(parent.getString(R.string.integrated_inbox_detail))
                    .withIcon(IconicsDrawable(parent, FontAwesome.Icon.faw_users)
                            .colorRes(R.color.material_drawer_background)
                            .backgroundColor(IconicsColor.colorInt(Color.GRAY))
                            .size(IconicsSize.dp(56))
                            .padding(IconicsSize.dp(8)))
                    .withSelected(unifiedInboxSelected)
                    .withIdentifier(DRAWER_ID_UNIFIED_INBOX)
            )
        }

        val photoUris = HashSet<Uri>()

        for (account in preferences.accounts) {
            val drawerId = (account.accountNumber + 1 shl DRAWER_ACCOUNT_SHIFT).toLong()

            val pdi = ProfileDrawerItem()
                    .withNameShown(true)
                    .withName(account.description)
                    .withEmail(account.email)
                    .withIdentifier(drawerId)
                    .withSelected(false)
                    .withTag(account)

            val photoUri = Contacts.getInstance(parent).getPhotoUri(account.email)
            if (photoUri != null && !photoUris.contains(photoUri)) {
                photoUris.add(photoUri)
                pdi.withIcon(photoUri)
            } else {
                pdi.withIcon(IconicsDrawable(parent, FontAwesome.Icon.faw_user_alt)
                        .colorRes(R.color.material_drawer_background)
                        .backgroundColor(IconicsColor.colorInt(account.chipColor))
                        .size(IconicsSize.dp(56))
                        .padding(IconicsSize.dp(14))
                )
            }
            headerBuilder.addProfiles(pdi)
        }

        return headerBuilder
                .withOnAccountHeaderListener(object : AccountHeader.OnAccountHeaderListener {
                    override fun onProfileChanged(view: View?, profile: IProfile<*>, current: Boolean): Boolean {
                        if (profile.identifier == DRAWER_ID_UNIFIED_INBOX) {
                            parent.openUnifiedInbox()
                            return false
                        } else {
                            val account = (profile as ProfileDrawerItem).tag as Account
                            parent.openRealAccount(account)
                            updateUserAccountsAndFolders(account)
                            return true
                        }
                    }
                })
                .build()
    }

    private fun addFooterItems() {
        if (!unifiedInboxSelected) {
            drawer.addStickyFooterItem(
                PrimaryDrawerItem()
                    .withName(R.string.folders_action)
                    .withIcon(folderIconProvider.iconFolderResId)
                    .withIdentifier(DRAWER_ID_FOLDERS)
                    .withSelectable(false)
            )
        }

        drawer.addStickyFooterItem(PrimaryDrawerItem()
            .withName(R.string.preferences_action)
            .withIcon(getResId(R.attr.iconActionSettings))
            .withIdentifier(DRAWER_ID_PREFERENCES)
            .withSelectable(false)
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
        if (account == null) {
            selectUnifiedInbox()
        } else {
            unifiedInboxSelected = false
            getDrawerColorsForAccount(account).let { drawerColors ->
                accentColor = drawerColors.accentColor
                selectedColor = drawerColors.selectedColor
            }

            accountHeader.setActiveProfile((account.accountNumber + 1 shl DRAWER_ACCOUNT_SHIFT).toLong())
            accountHeader.headerBackgroundView.setColorFilter(account.chipColor, PorterDuff.Mode.MULTIPLY)
            viewModel.loadFolders(account)

            updateFooterItems()
        }

        // Account can be null to refresh all (unified inbox or account list).
        swipeRefreshLayout.setOnRefreshListener {
            val accountToRefresh = if (accountHeader.isSelectionListShown) null else account
            messagingController.checkMail(parent, accountToRefresh, true, true, object : SimpleMessagingListener() {
                override fun checkMailFinished(context: Context?, account: Account?) {
                    swipeRefreshLayout.isRefreshing = false
                }
            })
        }
    }

    private fun updateFooterItems() {
        drawer.removeAllStickyFooterItems()
        addFooterItems()
    }

    private fun createItemClickListener(): OnDrawerItemClickListener {
        return object : OnDrawerItemClickListener {
            override fun onItemClick(view: View?, position: Int, drawerItem: IDrawerItem<*>): Boolean {
                when (drawerItem.identifier) {
                    DRAWER_ID_PREFERENCES -> SettingsActivity.launch(parent)
                    DRAWER_ID_FOLDERS -> parent.launchManageFoldersScreen()
                    else -> {
                        val folder = drawerItem.tag as Folder
                        parent.openFolder(folder.id)
                    }
                }
                return false
            }
        }
    }

    private fun setUserFolders(folders: List<DisplayFolder>?) {
        clearUserFolders()

        if (folders == null) {
            return
        }

        var openedFolderDrawerId: Long = -1
        for (displayFolder in folders) {
            val folder = displayFolder.folder
            val drawerId = folder.id shl DRAWER_FOLDER_SHIFT

            val drawerItem = PrimaryDrawerItem()
                    .withIcon(folderIconProvider.getFolderIcon(folder.type))
                    .withIdentifier(drawerId)
                    .withTag(folder)
                    .withSelectedColor(selectedColor)
                    .withSelectedTextColor(accentColor)
                    .withName(getFolderDisplayName(folder))

            val unreadCount = displayFolder.unreadCount
            if (unreadCount > 0) {
                drawerItem.withBadge(unreadCount.toString())
            }

            drawer.addItem(drawerItem)

            userFolderDrawerIds.add(drawerId)

            if (folder.id == openedFolderId) {
                openedFolderDrawerId = drawerId
            }
        }

        if (openedFolderDrawerId != -1L) {
            drawer.setSelection(openedFolderDrawerId, false)
        }
    }

    private fun clearUserFolders() {
        drawer.removeAllItems()
        userFolderDrawerIds.clear()
    }

    fun selectFolder(folderId: Long) {
        unifiedInboxSelected = false
        openedFolderId = folderId
        for (drawerId in userFolderDrawerIds) {
            val folder = drawer.getDrawerItem(drawerId)!!.tag as Folder
            if (folder.id == folderId) {
                drawer.setSelection(drawerId, false)
                return
            }
        }
        updateFooterItems()
    }

    fun deselect() {
        unifiedInboxSelected = false
        openedFolderId = null
        drawer.deselect()
    }

    fun selectUnifiedInbox() {
        unifiedInboxSelected = true
        openedFolderId = null
        accentColor = 0 // Unified inbox does not have folders, so color does not matter
        selectedColor = 0
        accountHeader.setActiveProfile(DRAWER_ID_UNIFIED_INBOX)
        accountHeader.headerBackgroundView.setColorFilter(0xFFFFFFFFL.toInt(), PorterDuff.Mode.MULTIPLY)
        viewModel.stopLoadingFolders()
        clearUserFolders()
        updateFooterItems()
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
        drawer.openDrawer()
    }

    fun close() {
        drawer.closeDrawer()
    }

    fun lock() {
        drawer.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    fun unlock() {
        drawer.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    companion object {
        // Bit shift for identifiers of user folders items, to leave space for other items
        private const val DRAWER_FOLDER_SHIFT: Int = 2
        private const val DRAWER_ACCOUNT_SHIFT: Int = 16

        private const val DRAWER_ID_UNIFIED_INBOX: Long = 0
        private const val DRAWER_ID_PREFERENCES: Long = 1
        private const val DRAWER_ID_FOLDERS: Long = 2

        private const val PROGRESS_VIEW_END_OFFSET = 32
        private const val PROGRESS_VIEW_SLINGSHOT_DISTANCE = 48
    }
}
