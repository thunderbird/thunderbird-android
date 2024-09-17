package app.k9mail.feature.navigation.drawer.domain.entity

import app.k9mail.core.mail.folder.api.Folder

data class DisplayAccountFolder(
    val accountUuid: String,
    val folder: Folder,
    val isInTopGroup: Boolean,
    val unreadMessageCount: Int,
    val starredMessageCount: Int,
)
