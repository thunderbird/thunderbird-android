package com.fsck.k9.backend.pop3

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Folder
import com.fsck.k9.mail.store.pop3.Pop3Store

class Pop3Backend(accountName: String, backendStorage: BackendStorage, pop3Store: Pop3Store) : Backend {
    private val pop3Sync: Pop3Sync = Pop3Sync(accountName, backendStorage, pop3Store)
    private val commandGetFolders = CommandGetFolders()
    private val commandSetFlag = CommandSetFlag(pop3Store)
    private val commandDeleteAll = CommandDeleteAll(pop3Store)

    override val supportsSeenFlag: Boolean = false
    override val supportsExpunge: Boolean = false

    override fun getFolders(forceListAll: Boolean): List<FolderInfo> {
        return commandGetFolders.getFolders()
    }

    override fun sync(folder: String, syncConfig: SyncConfig, listener: SyncListener, providedRemoteFolder: Folder<*>?) {
        pop3Sync.sync(folder, syncConfig, listener)
    }

    override fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) {
        commandSetFlag.setFlag(folderServerId, messageServerIds, flag, newState)
    }

    override fun markAllAsRead(folderServerId: String) {
        throw UnsupportedOperationException("not supported")
    }

    override fun expunge(folderServerId: String) {
        throw UnsupportedOperationException("not supported")
    }

    override fun expungeMessages(folderServerId: String, messageServerIds: List<String>) {
        throw UnsupportedOperationException("not supported")
    }

    override fun deleteAllMessages(folderServerId: String) {
        commandDeleteAll.deleteAll(folderServerId)
    }

    override fun moveMessages(
            sourceFolderServerId: String,
            targetFolderServerId: String,
            messageServerIds: List<String>
    ): Map<String, String>? {
        throw UnsupportedOperationException("not supported")
    }

    override fun copyMessages(
            sourceFolderServerId: String,
            targetFolderServerId: String,
            messageServerIds: List<String>
    ): Map<String, String>? {
        throw UnsupportedOperationException("not supported")
    }
}
