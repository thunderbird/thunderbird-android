package net.thunderbird.feature.navigation.drawer.dropdown.domain.entity

import net.thunderbird.feature.mail.folder.api.Folder

internal data class MailDisplayFolder(
    val accountId: String,
    val folder: Folder,
    val isInTopGroup: Boolean,
    override val unreadMessageCount: Int,
    override val starredMessageCount: Int,
) : DisplayFolder {
    override val id: String = createMailDisplayAccountFolderId(accountId, folder.id)
}

fun createMailDisplayAccountFolderId(accountId: String, folderId: Long): String {
    return "${accountId}_$folderId"
}
