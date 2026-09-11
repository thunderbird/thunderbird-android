package com.fsck.k9.controller

import com.fsck.k9.backend.api.FolderInfo
import net.thunderbird.core.android.account.LegacyAccountDto

internal interface ArchiveFolderCreator {
    fun createFolder(account: LegacyAccountDto, folderInfo: FolderInfo): Long?
}
