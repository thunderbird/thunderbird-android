package com.fsck.k9.ui


import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.fsck.k9.Account
import com.fsck.k9.DI
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.activity.MessageList
import com.fsck.k9.helper.Contacts
import com.fsck.k9.mailstore.DisplayFolder
import com.fsck.k9.mailstore.Folder
import com.fsck.k9.ui.folders.FolderIconProvider
import com.fsck.k9.ui.folders.FolderNameFormatter
import com.fsck.k9.ui.folders.FoldersLiveData
import com.fsck.k9.ui.messagelist.MessageListViewModel
import com.fsck.k9.ui.messagelist.MessageListViewModelFactory
import com.fsck.k9.ui.settings.SettingsActivity
import com.mikepenz.fontawesome_typeface_library.FontAwesome
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.materialdrawer.AccountHeader
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.Drawer.OnDrawerItemClickListener
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import java.util.ArrayList
import java.util.HashSet


class K9Drawer(private val parent: MessageList, savedInstanceState: Bundle?) {
    private val folderNameFormatter = DI.get<FolderNameFormatter>()
    private val preferences = DI.get<Preferences>()

    private val drawer: Drawer
    private val accountHeader: AccountHeader
    private val headerItemCount = 1
    private val folderIconProvider: FolderIconProvider = FolderIconProvider(parent.theme)

    private val userFolderDrawerIds = ArrayList<Long>()
    private var unifiedInboxSelected: Boolean = false
    private var openedFolderServerId: String? = null

    private var foldersLiveData: FoldersLiveData? = null
    private val foldersObserver = Observer<List<DisplayFolder>> { folders ->
        setUserFolders(folders)
    }


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

        addFooterItems()
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
                            .backgroundColor(Color.GRAY)
                            .sizeDp(56)
                            .paddingDp(8))
                    .withSetSelected(unifiedInboxSelected)
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
                    .withSetSelected(false)
                    .withTag(account)

            val photoUri = Contacts.getInstance(parent).getPhotoUri(account.email)
            if (photoUri != null && !photoUris.contains(photoUri)) {
                photoUris.add(photoUri)
                pdi.withIcon(photoUri)
            } else {
                pdi.withIcon(IconicsDrawable(parent, FontAwesome.Icon.faw_user_alt)
                        .colorRes(R.color.material_drawer_background)
                        .backgroundColor(account.chipColor)
                        .sizeDp(56)
                        .paddingDp(14)
                )
            }
            headerBuilder.addProfiles(pdi)
        }

        return headerBuilder
                .withOnAccountHeaderListener { _, profile, _ ->
                    if (profile.identifier == DRAWER_ID_UNIFIED_INBOX) {
                        parent.openUnifiedInbox()
                        false
                    } else {
                        val account = (profile as ProfileDrawerItem).tag as Account
                        parent.openRealAccount(account)
                        updateUserAccountsAndFolders(account)
                        false
                    }
                }
                .build()
    }

    private fun addFooterItems() {
        drawer.addItems(DividerDrawerItem(),
                PrimaryDrawerItem()
                        .withName(R.string.folders_action)
                        .withIcon(folderIconProvider.iconFolderResId)
                        .withIdentifier(DRAWER_ID_FOLDERS)
                        .withSelectable(false),
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
            accountHeader.setActiveProfile((account.accountNumber + 1 shl DRAWER_ACCOUNT_SHIFT).toLong())
            accountHeader.headerBackgroundView.setColorFilter(account.chipColor, PorterDuff.Mode.MULTIPLY)
            val viewModelProvider = ViewModelProviders.of(parent, MessageListViewModelFactory())
            val viewModel = viewModelProvider.get(MessageListViewModel::class.java)

            foldersLiveData?.removeObserver(foldersObserver)
            foldersLiveData = viewModel.getFolders(account).apply {
                observe(parent, foldersObserver)
            }

            updateFolderSettingsItem()
        }
    }

    private fun updateFolderSettingsItem() {
        val drawerItem = drawer.getDrawerItem(DRAWER_ID_FOLDERS)
        drawerItem.withEnabled(!unifiedInboxSelected)
        drawer.updateItem(drawerItem)
    }

    private fun createItemClickListener(): OnDrawerItemClickListener {
        return OnDrawerItemClickListener { _, _, drawerItem ->
            when (drawerItem.identifier) {
                DRAWER_ID_PREFERENCES -> SettingsActivity.launch(parent)
                DRAWER_ID_FOLDERS -> parent.launchManageFoldersScreen()
                else -> {
                    val folder = drawerItem.tag as Folder
                    parent.openFolder(folder.serverId)
                }
            }
            false
        }
    }

    private fun setUserFolders(folders: List<DisplayFolder>?) {
        clearUserFolders()

        if (folders == null) {
            return
        }

        var openedFolderDrawerId: Long = -1
        for (i in folders.indices.reversed()) {
            val displayFolder = folders[i]
            val folder = displayFolder.folder
            val drawerId = folder.id shl DRAWER_FOLDER_SHIFT

            val drawerItem = PrimaryDrawerItem()
                    .withIcon(folderIconProvider.getFolderIcon(folder.type))
                    .withIdentifier(drawerId)
                    .withTag(folder)
                    .withName(getFolderDisplayName(folder))

            val unreadCount = displayFolder.unreadCount
            if (unreadCount > 0) {
                drawerItem.withBadge(unreadCount.toString())
            }

            drawer.addItemAtPosition(drawerItem, headerItemCount)

            userFolderDrawerIds.add(drawerId)

            if (folder.serverId == openedFolderServerId) {
                openedFolderDrawerId = drawerId
            }
        }

        if (openedFolderDrawerId != -1L) {
            drawer.setSelection(openedFolderDrawerId, false)
        }
    }

    private fun clearUserFolders() {
        for (drawerId in userFolderDrawerIds) {
            drawer.removeItem(drawerId)
        }
        userFolderDrawerIds.clear()
    }

    fun selectFolder(folderServerId: String) {
        unifiedInboxSelected = false
        openedFolderServerId = folderServerId
        for (drawerId in userFolderDrawerIds) {
            val folder = drawer.getDrawerItem(drawerId).tag as Folder
            if (folder.serverId == folderServerId) {
                drawer.setSelection(drawerId, false)
                return
            }
        }
        updateFolderSettingsItem()
    }

    fun deselect() {
        unifiedInboxSelected = false
        openedFolderServerId = null
        drawer.deselect()
    }

    fun selectUnifiedInbox() {
        unifiedInboxSelected = true
        openedFolderServerId = null
        accountHeader.setActiveProfile(DRAWER_ID_UNIFIED_INBOX)
        accountHeader.headerBackgroundView.setColorFilter(0xFFFFFFFFL.toInt(), PorterDuff.Mode.MULTIPLY)
        clearUserFolders()
        updateFolderSettingsItem()
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
    }
}
