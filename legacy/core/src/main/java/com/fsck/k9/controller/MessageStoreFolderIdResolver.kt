package com.fsck.k9.controller

import app.k9mail.legacy.mailstore.MessageStoreManager
import net.thunderbird.core.android.account.LegacyAccountDto

internal class MessageStoreFolderIdResolver(
    private val messageStoreManager: MessageStoreManager,
) : FolderIdResolver {
    override fun getFolderServerId(account: LegacyAccountDto, folderId: Long): String? {
        return messageStoreManager.getMessageStore(account).getFolderServerId(folderId)
    }

    override fun getFolderId(account: LegacyAccountDto, folderServerId: String): Long? {
        return messageStoreManager.getMessageStore(account).getFolderId(folderServerId)
    }
}
