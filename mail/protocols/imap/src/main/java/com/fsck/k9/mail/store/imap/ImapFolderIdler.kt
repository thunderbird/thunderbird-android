package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.power.WakeLock

interface ImapFolderIdler {
    fun idle(): IdleResult
    fun refresh()
    fun stop()

    companion object {
        private val connectionProvider = object : ImapConnectionProvider {
            override fun getConnection(folder: ImapFolder): ImapConnection? {
                require(folder is RealImapFolder)
                return folder.connection
            }
        }

        fun create(
            idleRefreshManager: IdleRefreshManager,
            wakeLock: WakeLock,
            imapStore: ImapStore,
            folderServerId: String,
            idleRefreshTimeoutProvider: IdleRefreshTimeoutProvider,
        ): ImapFolderIdler {
            return RealImapFolderIdler(
                idleRefreshManager,
                wakeLock,
                imapStore,
                connectionProvider,
                folderServerId,
                idleRefreshTimeoutProvider,
            )
        }
    }
}

enum class IdleResult {
    SYNC,
    STOPPED,
    NOT_SUPPORTED,
}
