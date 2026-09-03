package net.thunderbird.feature.mail.folder.api

import net.thunderbird.core.logging.LoggingPii

@LoggingPii.HasPii
data class Folder(
    val id: Long,
    @get:LoggingPii.Mask
    val name: String,
    val type: FolderType,
    val isLocalOnly: Boolean,
)

/*
 * TODO(#11493): The logging compiler plugin will automatically should auto-generate
 *  this method with the correct masking.
 */
fun Folder.toStringPiiSafe(): String = "Folder(id=$id, name='<sensitive>', type=$type, isLocalOnly=$isLocalOnly)"
