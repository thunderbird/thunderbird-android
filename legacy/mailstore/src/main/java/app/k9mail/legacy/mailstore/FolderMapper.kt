package app.k9mail.legacy.mailstore

import com.fsck.k9.mail.FolderClass
import com.fsck.k9.mail.FolderType

fun interface FolderMapper<T> {
    fun map(folder: FolderDetailsAccessor): T
}

interface FolderDetailsAccessor {
    val id: Long
    val serverId: String?
    val name: String
    val type: FolderType
    val isLocalOnly: Boolean
    val isInTopGroup: Boolean
    val isIntegrate: Boolean
    val syncClass: FolderClass
    val displayClass: FolderClass
    val isNotificationsEnabled: Boolean
    val isPushEnabled: Boolean
    val visibleLimit: Int
    val moreMessages: MoreMessages
    val lastChecked: Long?
    val unreadMessageCount: Int
    val starredMessageCount: Int

    fun serverIdOrThrow(): String
}
