package com.fsck.k9.controller

import com.fsck.k9.backend.api.FolderInfo
import net.thunderbird.core.android.account.LegacyAccountDto

internal class FakeArchiveFolderCreator(
    private val createdFolderIds: Map<String, Long?> = emptyMap(),
    private val failAfterCalls: Int = Int.MAX_VALUE,
) : ArchiveFolderCreator {
    private var callCount = 0

    override fun createFolder(account: LegacyAccountDto, folderInfo: FolderInfo): Long? {
        callCount++
        if (callCount > failAfterCalls) {
            return null
        }
        return createdFolderIds[folderInfo.serverId]
    }
}
