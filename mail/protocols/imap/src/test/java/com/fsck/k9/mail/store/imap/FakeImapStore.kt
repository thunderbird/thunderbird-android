package com.fsck.k9.mail.store.imap

import kotlin.test.fail

class FakeImapStore : ImapStore {
    private var openConnectionCount = 0

    var getFoldersAction: () -> List<FolderListItem> = { fail("getFoldersAction not set") }
    val hasOpenConnections: Boolean
        get() = openConnectionCount != 0

    override val combinedPrefix: String?
        get() = null

    override fun checkSettings() {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getFolder(name: String): ImapFolder {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getFolders(): List<FolderListItem> {
        openConnectionCount++
        return getFoldersAction()
    }

    override fun closeAllConnections() {
        openConnectionCount = 0
    }
}
