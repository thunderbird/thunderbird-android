package net.thunderbird.feature.navigation.drawer.dropdown.domain.entity

import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter

internal interface DisplayFolder {
    val id: String
    val unreadMessageCount: Int
    val starredMessageCount: Int
    val pathDelimiter: FolderPathDelimiter
}
