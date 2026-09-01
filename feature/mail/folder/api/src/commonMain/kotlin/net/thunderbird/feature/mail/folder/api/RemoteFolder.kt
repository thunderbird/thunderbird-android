package net.thunderbird.feature.mail.folder.api

import net.thunderbird.core.logging.LoggingPii

@LoggingPii.HasPii
data class RemoteFolder(
    val id: Long,
    @get:LoggingPii.Mask
    val serverId: String,
    @get:LoggingPii.Mask
    val name: String,
    val type: FolderType,
)

/*
 * TODO(#11493): The logging compiler plugin will automatically should auto-generate
 *  this method with the correct masking.
 */
fun RemoteFolder.toStringPiiSafe(): String =
    "RemoteFolder(id=$id, serverId='<sensitive>', name='<sensitive>', type=$type)"
