package app.k9mail.feature.navigation.drawer

import androidx.appcompat.app.AppCompatActivity
import app.k9mail.legacy.account.Account

interface NavigationDrawer {
    val parent: AppCompatActivity
    val isOpen: Boolean

    // TODO: remove once LegacyDrawer is removed
    fun updateUserAccountsAndFolders(account: Account?)

    fun selectAccount(accountUuid: String)

    fun selectFolder(accountUuid: String, folderId: Long)

    fun selectUnifiedInbox()

    fun deselect()

    fun open()

    fun close()

    fun lock()

    fun unlock()
}
