package com.fsck.k9.ui.folders

import com.fsck.k9.Account
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mailstore.FolderRepository

class FoldersLiveDataFactory(
    private val folderRepository: FolderRepository,
    private val messagingController: MessagingController,
    private val preferences: Preferences
) {
    fun create(account: Account, displayMode: FolderMode? = null): FoldersLiveData {
        return FoldersLiveData(folderRepository, messagingController, preferences, account, displayMode)
    }
}
