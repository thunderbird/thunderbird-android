package net.thunderbird.feature.navigation.drawer.dropdown.domain.entity

import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter

internal data class MailDisplayFolder(
    override val accountId: AccountId,
    val folder: Folder,
    val isInTopGroup: Boolean,
    override val unreadMessageCount: Int,
    override val starredMessageCount: Int,
    override val pathDelimiter: FolderPathDelimiter,
) : DisplayFolder {
    override val folderId: String = createMailDisplayAccountFolderId(accountId, folder.id)
}

fun createMailDisplayAccountFolderId(accountId: AccountId, folderId: Long): String {
    return "${accountId}_$folderId"
}
