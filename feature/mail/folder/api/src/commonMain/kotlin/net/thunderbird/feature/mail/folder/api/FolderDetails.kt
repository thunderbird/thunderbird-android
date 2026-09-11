package net.thunderbird.feature.mail.folder.api

import net.thunderbird.core.logging.LoggingPii

@LoggingPii.HasPii
data class FolderDetails(
    override val folder: Folder,
    override val isInTopGroup: Boolean,
    override val isIntegrate: Boolean,
    override val isSyncEnabled: Boolean,
    override val isVisible: Boolean,
    override val isNotificationsEnabled: Boolean,
    override val isPushEnabled: Boolean,
) : BaseFolderDetails<Folder>

@LoggingPii.HasPii
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

/*
 * TODO(#11493): The logging compiler plugin will automatically should auto-generate
 *  this method with the correct masking.
 */
fun FolderDetails.toStringPiiSafe(): String = "FolderDetails(folder=${folder.toStringPiiSafe()}, " +
    "isInTopGroup=$isInTopGroup, isIntegrate=$isIntegrate, isSyncEnabled=$isSyncEnabled, " +
    "isVisible=$isVisible, isNotificationsEnabled=$isNotificationsEnabled, isPushEnabled=$isPushEnabled)"

/*
 * TODO(#11493): The logging compiler plugin will automatically should auto-generate
 *  this method with the correct masking.
 */
fun RemoteFolderDetails.toStringPiiSafe(): String = "FolderDetails(folder=${folder.toStringPiiSafe()}, " +
    "isInTopGroup=$isInTopGroup, isIntegrate=$isIntegrate, isSyncEnabled=$isSyncEnabled, " +
    "isVisible=$isVisible, isNotificationsEnabled=$isNotificationsEnabled, isPushEnabled=$isPushEnabled)"
