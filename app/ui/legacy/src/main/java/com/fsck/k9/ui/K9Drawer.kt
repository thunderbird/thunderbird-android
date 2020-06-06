package com.fsck.k9.ui

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import androidx.core.view.GravityCompat
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
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import com.mikepenz.iconics.utils.backgroundColorInt
import com.mikepenz.iconics.utils.paddingDp
import com.mikepenz.iconics.utils.sizeDp
import com.mikepenz.materialdrawer.holder.ImageHolder
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.withBadge
import com.mikepenz.materialdrawer.model.interfaces.withEmail
import com.mikepenz.materialdrawer.model.interfaces.withIcon
import com.mikepenz.materialdrawer.model.interfaces.withIdentifier
import com.mikepenz.materialdrawer.model.interfaces.withName
import com.mikepenz.materialdrawer.model.interfaces.withSelectable
import com.mikepenz.materialdrawer.model.interfaces.withSelected
import com.mikepenz.materialdrawer.model.interfaces.withTag
import com.mikepenz.materialdrawer.util.addItems
import com.mikepenz.materialdrawer.util.addStickyFooterItem
import com.mikepenz.materialdrawer.util.getDrawerItem
import com.mikepenz.materialdrawer.util.removeAllItems
import com.mikepenz.materialdrawer.util.removeAllStickyFooterItems
import com.mikepenz.materialdrawer.widget.AccountHeaderView
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.parameter.parametersOf
import java.util.ArrayList
import java.util.HashSet

class K9Drawer(private val parent: MessageList, savedInstanceState: Bundle?) : KoinComponent {
    private val viewModel: FoldersViewModel by parent.viewModel()
    private val folderNameFormatter: FolderNameFormatter by inject { parametersOf(parent) }
    private val preferences: Preferences by inject()
    private val themeManager: ThemeManager by inject()
    private val resources: Resources by inject()
    private val messagingController: MessagingController by inject()

    private val drawer: DrawerLayout = parent.findViewById(R.id.drawerLayout)
    private val sliderView: MaterialDrawerSliderView = parent.findViewById(R.id.material_drawer_slider)
    private val headerView: AccountHeaderView = AccountHeaderView(parent).apply {
        attachToSliderView(this@K9Drawer.sliderView)
    }
    private val folderIconProvider: FolderIconProvider = FolderIconProvider(parent.theme)
    private val swipeRefreshLayout: SwipeRefreshLayout

    private val userFolderDrawerIds = ArrayList<Long>()
    private var unifiedInboxSelected: Boolean = false
    private var accentColor: Int = 0
    private var selectedColor: Int = 0
    private var openedFolderId: Long? = null

    val layout: DrawerLayout
        get() = drawer

    val isOpen: Boolean
        get() = drawer.isOpen

    init {
        configureAccountHeader()

        drawer.setDrawerListener(parent.createOnDrawerListener())
        sliderView.onDrawerItemClickListener = { view, item, position ->
            when (item.identifier) {
                DRAWER_ID_PREFERENCES -> SettingsActivity.launch(parent)
                DRAWER_ID_FOLDERS -> parent.launchManageFoldersScreen()
                else -> {
                    val folder = item.tag as Folder
                    parent.openFolder(folder.id)
                }
            }
            false
        }
        sliderView.setSavedInstance(savedInstanceState)
        headerView.withSavedInstance(savedInstanceState)

        swipeRefreshLayout = parent.findViewById(R.id.material_drawer_swipe_refresh)
        headerView.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
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

    private fun configureAccountHeader() {
        headerView.headerBackground = ImageHolder(R.drawable.drawer_header_background)

        if (!K9.isHideSpecialAccounts) {
            headerView.addProfiles(
                ProfileDrawerItem()
                    .withNameShown(true)
                    .withName(R.string.integrated_inbox_title)
                    .withEmail(parent.getString(R.string.integrated_inbox_detail))
                    .withIcon(IconicsDrawable(parent, FontAwesome.Icon.faw_users).apply {
                        //colorRes = R.color.material_drawer_background
                        backgroundColorInt = Color.GRAY
                        sizeDp = 56
                        paddingDp = 8
                    })
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
                pdi.withIcon(IconicsDrawable(parent, FontAwesome.Icon.faw_user_alt).apply {
                    // TODO colorRes = R.color.material_drawer_background
                    backgroundColorInt = account.chipColor
                    sizeDp = 56
                    paddingDp = 12
                })
            }
            headerView.addProfiles(pdi)
        }

        headerView.onAccountHeaderListener = { view, profile, current ->
            if (profile.identifier == DRAWER_ID_UNIFIED_INBOX) {
                parent.openUnifiedInbox()
                false
            } else {
                val account = (profile as ProfileDrawerItem).tag as Account
                parent.openRealAccount(account)
                updateUserAccountsAndFolders(account)
                true
            }
        }
    }

    private fun addFooterItems() {
        if (!unifiedInboxSelected) {
            sliderView.addStickyFooterItem(
                PrimaryDrawerItem()
                    .withName(R.string.folders_action)
                    .withIcon(folderIconProvider.iconFolderResId)
                    .withIdentifier(DRAWER_ID_FOLDERS)
                    .withSelectable(false)
            )
        }

        sliderView.addStickyFooterItem(
            PrimaryDrawerItem()
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

            headerView.setActiveProfile((account.accountNumber + 1 shl DRAWER_ACCOUNT_SHIFT).toLong())
            headerView.accountHeaderBackground.setColorFilter(account.chipColor, PorterDuff.Mode.MULTIPLY)
            viewModel.loadFolders(account)

            updateFooterItems()
        }

        // Account can be null to refresh all (unified inbox or account list).
        swipeRefreshLayout.setOnRefreshListener {
            val accountToRefresh = if (headerView.selectionListShown) null else account
            messagingController.checkMail(parent, accountToRefresh, true, true, object : SimpleMessagingListener() {
                override fun checkMailFinished(context: Context?, account: Account?) {
                    swipeRefreshLayout.isRefreshing = false
                }
            })
        }
    }

    private fun updateFooterItems() {
        sliderView.removeAllStickyFooterItems()
        addFooterItems()
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
                //.withSelectedColor(selectedColor)
                //.apply {
                //    textColorInt = accentColor
                //}
                .withName(getFolderDisplayName(folder))

            val unreadCount = displayFolder.unreadCount
            if (unreadCount > 0) {
                drawerItem.withBadge(unreadCount.toString())
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
        sliderView.itemAdapter.clear()
        userFolderDrawerIds.clear()
    }

    fun selectFolder(folderId: Long) {
        unifiedInboxSelected = false
        openedFolderId = folderId
        for (drawerId in userFolderDrawerIds) {
            val folder = sliderView.getDrawerItem(drawerId)!!.tag as Folder
            if (folder.id == folderId) {
                sliderView.setSelection(drawerId, false)
                return
            }
        }
        updateFooterItems()
    }

    fun deselect() {
        unifiedInboxSelected = false
        openedFolderId = null
        sliderView.selectExtension.deselect()
    }

    fun selectUnifiedInbox() {
        unifiedInboxSelected = true
        openedFolderId = null
        accentColor = 0 // Unified inbox does not have folders, so color does not matter
        selectedColor = 0
        headerView.setActiveProfile(DRAWER_ID_UNIFIED_INBOX)
        headerView.accountHeaderBackground.setColorFilter(0xFFFFFFFFL.toInt(), PorterDuff.Mode.MULTIPLY)
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
