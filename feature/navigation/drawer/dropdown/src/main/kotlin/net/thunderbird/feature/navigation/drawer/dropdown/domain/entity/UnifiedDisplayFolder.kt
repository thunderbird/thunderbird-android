package net.thunderbird.feature.navigation.drawer.dropdown.domain.entity

import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.UnifiedAccountId
import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter

internal data class UnifiedDisplayFolder(
    override val folderId: String,
    val unifiedType: UnifiedDisplayFolderType,
    override val unreadMessageCount: Int,
    override val starredMessageCount: Int,
) : DisplayFolder {
    override val accountId: AccountId = UnifiedAccountId
    override val pathDelimiter: FolderPathDelimiter = "/"
}
