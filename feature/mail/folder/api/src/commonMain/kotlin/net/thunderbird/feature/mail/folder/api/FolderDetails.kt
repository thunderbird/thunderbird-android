package net.thunderbird.feature.mail.folder.api

import net.thunderbird.core.logging.LoggingPii

@LoggingPii.HasPii
data class FolderDetails(
    val folder: Folder,
    val isInTopGroup: Boolean,
    val isIntegrate: Boolean,
    val isSyncEnabled: Boolean,
    val isVisible: Boolean,
    val isNotificationsEnabled: Boolean,
    val isPushEnabled: Boolean,
)

/*
 * TODO(#11493): The logging compiler plugin will automatically should auto-generate
 *  this method with the correct masking.
 */
fun FolderDetails.toStringPiiSafe(): String = "FolderDetails(folder=${folder.toStringPiiSafe()}, " +
    "isInTopGroup=$isInTopGroup, isIntegrate=$isIntegrate, isSyncEnabled=$isSyncEnabled, " +
    "isVisible=$isVisible, isNotificationsEnabled=$isNotificationsEnabled, isPushEnabled=$isPushEnabled)"
