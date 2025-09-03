package net.thunderbird.feature.navigation.drawer.dropdown.domain.entity

import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter

internal data class MailDisplayFolder(
    val accountId: String?,
    val folder: Folder,
    val isInTopGroup: Boolean,
    override val unreadMessageCount: Int,
    override val starredMessageCount: Int,
    override val pathDelimiter: FolderPathDelimiter,
) : DisplayFolder {
    override val id: String = createMailDisplayAccountFolderId(accountId.orEmpty(), folder.id)
}

fun createMailDisplayAccountFolderId(accountId: String, folderId: Long): String {
    return "${accountId}_$folderId"
}
