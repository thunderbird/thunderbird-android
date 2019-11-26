package com.fsck.k9.ui.folders

import com.fsck.k9.Account
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mailstore.FolderRepositoryManager

class FoldersLiveDataFactory(
        private val folderRepositoryManager: FolderRepositoryManager,
        private val messagingController: MessagingController
) {
    fun create(account: Account): FoldersLiveData {
        val folderRepository = folderRepositoryManager.getFolderRepository(account)
        return FoldersLiveData(folderRepository, messagingController, account.uuid)
    }
}
