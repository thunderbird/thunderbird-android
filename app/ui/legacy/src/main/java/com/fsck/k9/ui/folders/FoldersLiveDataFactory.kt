package com.fsck.k9.ui.folders

import com.fsck.k9.Account
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mailstore.FolderRepositoryManager

class FoldersLiveDataFactory(
    private val folderRepositoryManager: FolderRepositoryManager,
    private val messagingController: MessagingController,
    private val preferences: Preferences
) {
    fun create(account: Account, displayMode: FolderMode? = null): FoldersLiveData {
        val folderRepository = folderRepositoryManager.getFolderRepository(account)
        return FoldersLiveData(folderRepository, messagingController, preferences, account.uuid, displayMode)
    }
}
