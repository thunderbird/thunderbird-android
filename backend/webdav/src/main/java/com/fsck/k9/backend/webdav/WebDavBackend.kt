package com.fsck.k9.backend.webdav

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Folder
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.store.webdav.WebDavStore

class WebDavBackend(accountName: String, backendStorage: BackendStorage, webDavStore: WebDavStore) : Backend {
    private val webDavSync: WebDavSync = WebDavSync(accountName, backendStorage, webDavStore)
    private val commandGetFolders = CommandGetFolders(webDavStore)
    private val commandSetFlag = CommandSetFlag(webDavStore)
    private val commandMarkAllAsRead = CommandMarkAllAsRead(webDavStore)
    private val commandMoveOrCopyMessages = CommandMoveOrCopyMessages(webDavStore)

    override val supportsSeenFlag: Boolean = true
    override val supportsExpunge: Boolean = true


    override fun getFolders(forceListAll: Boolean): List<FolderInfo> {
        return commandGetFolders.getFolders(forceListAll)
    }

    override fun sync(folder: String, syncConfig: SyncConfig, listener: SyncListener, providedRemoteFolder: Folder<*>?) {
        webDavSync.sync(folder, syncConfig, listener)
    }

    @Throws(MessagingException::class)
    override fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) {
        commandSetFlag.setFlag(folderServerId, messageServerIds, flag, newState)
    }

    override fun markAllAsRead(folderServerId: String) {
        commandMarkAllAsRead.markAllAsRead(folderServerId)
    }

    override fun expunge(folderServerId: String) {
        throw UnsupportedOperationException("not supported")
    }

    override fun expungeMessages(folderServerId: String, messageServerIds: List<String>) {
        throw UnsupportedOperationException("not supported")
    }

    override fun moveMessages(
            sourceFolderServerId: String,
            targetFolderServerId: String,
            messageServerIds: List<String>
    ): Map<String, String>? {
        return commandMoveOrCopyMessages.moveMessages(sourceFolderServerId, targetFolderServerId, messageServerIds)
    }

    override fun copyMessages(
            sourceFolderServerId: String,
            targetFolderServerId: String,
            messageServerIds: List<String>
    ): Map<String, String>? {
        return commandMoveOrCopyMessages.copyMessages(sourceFolderServerId, targetFolderServerId, messageServerIds)
    }
}
