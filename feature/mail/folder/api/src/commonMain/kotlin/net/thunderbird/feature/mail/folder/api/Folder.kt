package net.thunderbird.feature.mail.folder.api

import net.thunderbird.piisafe.annotation.PiiSafe

@PiiSafe.HasPii
data class Folder(
    val id: Long,
    @get:PiiSafe.Mask
    val name: String,
    val type: FolderType,
    val isLocalOnly: Boolean,
)

/*
 * TODO(#11493): The logging compiler plugin will automatically should auto-generate
 *  this method with the correct masking.
 */
fun Folder.toStringPiiSafe(): String = "Folder(id=$id, name='<sensitive>', type=$type, isLocalOnly=$isLocalOnly)"
