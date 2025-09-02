package net.thunderbird.feature.navigation.drawer.dropdown.domain.entity

import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter

internal data class UnifiedDisplayFolder(
    override val id: String,
    val unifiedType: UnifiedDisplayFolderType,
    override val unreadMessageCount: Int,
    override val starredMessageCount: Int,
) : DisplayFolder {
    override val pathDelimiter: FolderPathDelimiter = "/"
}
