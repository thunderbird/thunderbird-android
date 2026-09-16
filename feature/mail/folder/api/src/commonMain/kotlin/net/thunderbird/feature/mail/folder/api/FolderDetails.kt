package net.thunderbird.feature.mail.folder.api

import net.thunderbird.piisafe.annotation.PiiSafe

@PiiSafe.HasPii
data class FolderDetails(
    override val folder: Folder,
    override val isInTopGroup: Boolean,
    override val isIntegrate: Boolean,
    override val isSyncEnabled: Boolean,
    override val isVisible: Boolean,
    override val isNotificationsEnabled: Boolean,
    override val isPushEnabled: Boolean,
) : BaseFolderDetails<Folder>

@PiiSafe.HasPii
data class RemoteFolderDetails(
    override val folder: RemoteFolder,
    override val isInTopGroup: Boolean,
    override val isIntegrate: Boolean,
    override val isSyncEnabled: Boolean,
    override val isVisible: Boolean,
    override val isNotificationsEnabled: Boolean,
    override val isPushEnabled: Boolean,
) : BaseFolderDetails<RemoteFolder>

sealed interface BaseFolderDetails<TFolder> {
    val folder: TFolder
    val isInTopGroup: Boolean
    val isIntegrate: Boolean
    val isSyncEnabled: Boolean
    val isVisible: Boolean
    val isNotificationsEnabled: Boolean
    val isPushEnabled: Boolean
}
