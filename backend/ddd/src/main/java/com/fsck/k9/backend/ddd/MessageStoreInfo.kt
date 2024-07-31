package com.fsck.k9.backend.ddd

import com.fsck.k9.mail.FolderType
import com.squareup.moshi.JsonClass

typealias MessageStoreInfo = Map<String, FolderData>

@JsonClass(generateAdapter = true)
data class FolderData(
    val name: String,
    val type: FolderType,
    val messageServerIds: List<String>,
)
