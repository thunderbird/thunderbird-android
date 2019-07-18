package com.fsck.k9.ui.folders

import androidx.lifecycle.LiveData
import com.fsck.k9.Account
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.controller.SimpleMessagingListener
import com.fsck.k9.mailstore.DisplayFolder
import com.fsck.k9.mailstore.FolderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FoldersLiveData(
        private val folderRepository: FolderRepository,
        private val messagingController: MessagingController,
        val accountUuid: String
) : LiveData<List<DisplayFolder>>() {

    private val listener = object : SimpleMessagingListener() {
        override fun folderStatusChanged(
                account: Account?,
                folderServerId: String?,
                unreadMessageCount: Int
        ) {
            if (account?.uuid == accountUuid) {
                loadFoldersAsync()
            }
        }
    }

    private fun loadFoldersAsync() {
        GlobalScope.launch(Dispatchers.Main) {
            value = withContext(Dispatchers.IO) { folderRepository.getDisplayFolders() }
        }
    }

    override fun onActive() {
        super.onActive()
        messagingController.addListener(listener)
        loadFoldersAsync()
    }

    override fun onInactive() {
        super.onInactive()
        messagingController.removeListener(listener)
    }
}
