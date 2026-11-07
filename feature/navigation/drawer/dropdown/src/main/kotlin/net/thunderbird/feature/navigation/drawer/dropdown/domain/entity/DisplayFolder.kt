package net.thunderbird.feature.navigation.drawer.dropdown.domain.entity

import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter

internal interface DisplayFolder {
    val accountId: AccountId
    val folderId: String
    val unreadMessageCount: Int
    val starredMessageCount: Int
    val pathDelimiter: FolderPathDelimiter
}
