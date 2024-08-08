package app.k9mail.feature.navigation.drawer

import androidx.appcompat.app.AppCompatActivity
import app.k9mail.legacy.account.Account

interface NavigationDrawer {
    val parent: AppCompatActivity
    val isOpen: Boolean

    fun updateUserAccountsAndFolders(account: Account?)

    fun selectAccount(accountUuid: String)

    fun selectFolder(folderId: Long)

    fun selectUnifiedInbox()

    fun deselect()

    fun open()

    fun close()

    fun lock()

    fun unlock()
}
