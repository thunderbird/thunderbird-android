package app.k9mail.feature.navigation.drawer.domain.entity

import app.k9mail.core.mail.folder.api.Folder

internal data class DisplayAccountFolder(
    val accountUuid: String,
    val folder: Folder,
    val isInTopGroup: Boolean,
    override val unreadMessageCount: Int,
    override val starredMessageCount: Int,
) : DisplayFolder {
    override val id: String = createDisplayAccountFolderId(accountUuid, folder.id)
}

fun createDisplayAccountFolderId(accountUuid: String, folderId: Long): String {
    return "${accountUuid}_$folderId"
}
