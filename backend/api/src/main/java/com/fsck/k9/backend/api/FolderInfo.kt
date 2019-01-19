package com.fsck.k9.backend.api

import com.fsck.k9.mail.Folder

data class FolderInfo(val serverId: String, val name: String, val type: Folder.FolderType)
