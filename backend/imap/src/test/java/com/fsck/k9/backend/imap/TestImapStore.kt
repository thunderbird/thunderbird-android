package com.fsck.k9.backend.imap

import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.store.imap.FolderListItem
import com.fsck.k9.mail.store.imap.ImapFolder
import com.fsck.k9.mail.store.imap.ImapStore

class TestImapStore : ImapStore {
    private val folders = mutableMapOf<String, TestImapFolder>()

    fun addFolder(name: String): TestImapFolder {
        require(!folders.containsKey(name)) { "Folder '$name' already exists" }

        return TestImapFolder(name).also { folder ->
            folders[name] = folder
        }
    }

    override fun getFolder(name: String): ImapFolder {
        return folders[name] ?: error("Folder '$name' not found")
    }

    override fun getFolders(): List<FolderListItem> {
        return folders.values.map { folder ->
            FolderListItem(
                serverId = folder.serverId,
                name = "irrelevant",
                type = FolderType.REGULAR,
                oldServerId = null
            )
        }
    }

    override fun checkSettings() {
        throw UnsupportedOperationException("not implemented")
    }

    override fun closeAllConnections() {
        throw UnsupportedOperationException("not implemented")
    }
}
