package net.thunderbird.feature.mail.folder.api

import net.thunderbird.piisafe.annotation.PiiSafe

@PiiSafe.HasPii
public data class FolderDetails(
    override val folder: Folder,
    override val isInTopGroup: Boolean,
    override val isIntegrate: Boolean,
    override val isSyncEnabled: Boolean,
    override val isVisible: Boolean,
    override val isNotificationsEnabled: Boolean,
    override val isPushEnabled: Boolean,
) : BaseFolderDetails<Folder>

@PiiSafe.HasPii
public data class RemoteFolderDetails(
    override val folder: RemoteFolder,
    override val isInTopGroup: Boolean,
    override val isIntegrate: Boolean,
    override val isSyncEnabled: Boolean,
    override val isVisible: Boolean,
    override val isNotificationsEnabled: Boolean,
    override val isPushEnabled: Boolean,
) : BaseFolderDetails<RemoteFolder>

public sealed interface BaseFolderDetails<TFolder> {
    public val folder: TFolder
    public val isInTopGroup: Boolean
    public val isIntegrate: Boolean
    public val isSyncEnabled: Boolean
    public val isVisible: Boolean
    public val isNotificationsEnabled: Boolean
    public val isPushEnabled: Boolean
}
