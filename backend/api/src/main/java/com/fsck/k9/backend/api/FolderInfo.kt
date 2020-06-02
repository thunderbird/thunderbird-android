package com.fsck.k9.backend.api

import com.fsck.k9.mail.FolderType

data class FolderInfo(val serverId: String, val name: String, val type: FolderType)
