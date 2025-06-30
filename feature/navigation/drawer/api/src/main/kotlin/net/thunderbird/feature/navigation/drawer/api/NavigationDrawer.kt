package net.thunderbird.feature.navigation.drawer.api

import androidx.appcompat.app.AppCompatActivity

interface NavigationDrawer {
    val parent: AppCompatActivity
    val isOpen: Boolean

    fun selectAccount(accountUuid: String)

    fun selectFolder(accountUuid: String, folderId: Long)

    fun selectUnifiedInbox()

    fun deselect()

    fun open()

    fun close()

    fun lock()

    fun unlock()
}
