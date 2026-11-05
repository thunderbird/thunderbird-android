package net.thunderbird.feature.navigation.drawer.dropdown.domain.entity

import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.UnifiedAccountId
import net.thunderbird.feature.mail.folder.FolderRole
import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter

/**
 * A unified folder in the drawer, showing the folder with the given [role] in every account.
 */
internal data class UnifiedDisplayFolder(
    val role: FolderRole,
    override val unreadMessageCount: Int,
    override val starredMessageCount: Int,
) : DisplayFolder {
    override val folderId: String = idOf(role)
    override val accountId: AccountId = UnifiedAccountId
    override val pathDelimiter: FolderPathDelimiter = "/"

    companion object {
        fun idOf(role: FolderRole): String = "unified_${role.name.lowercase()}"
    }
}
