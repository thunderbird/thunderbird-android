package com.fsck.k9.controller

import net.thunderbird.core.android.account.LegacyAccountDto

internal class FakeFolderIdResolver(
    private val folderServerIds: Map<Long, String?> = emptyMap(),
    private val folderIds: Map<String, Long?> = emptyMap(),
) : FolderIdResolver {
    override fun getFolderServerId(account: LegacyAccountDto, folderId: Long): String? {
        return folderServerIds[folderId]
    }

    override fun getFolderId(account: LegacyAccountDto, folderServerId: String): Long? {
        return folderIds[folderServerId]
    }
}
