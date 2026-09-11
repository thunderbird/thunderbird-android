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
