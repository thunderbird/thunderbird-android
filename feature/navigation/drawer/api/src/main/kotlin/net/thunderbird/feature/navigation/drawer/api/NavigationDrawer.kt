package net.thunderbird.feature.navigation.drawer.api

import androidx.appcompat.app.AppCompatActivity
import net.thunderbird.feature.account.AccountId

interface NavigationDrawer {
    val parent: AppCompatActivity
    val isOpen: Boolean

    fun selectAccount(accountId: AccountId)

    fun selectFolder(accountId: AccountId, folderId: Long)

    fun selectUnifiedInbox()

    fun deselect()

    fun open()

    fun close()

    fun lock()

    fun unlock()
}
