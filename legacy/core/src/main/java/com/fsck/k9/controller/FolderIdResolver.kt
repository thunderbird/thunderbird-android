package com.fsck.k9.controller

import net.thunderbird.core.android.account.LegacyAccountDto

internal interface FolderIdResolver {
    fun getFolderServerId(account: LegacyAccountDto, folderId: Long): String?
    fun getFolderId(account: LegacyAccountDto, folderServerId: String): Long?
}
