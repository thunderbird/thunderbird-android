package com.fsck.k9.backend.pop3


import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.store.pop3.Pop3Folder


internal class CommandGetFolders {
    fun getFolders(): List<FolderInfo> {
        return listOf(FolderInfo(Pop3Folder.INBOX, Pop3Folder.INBOX))
    }
}
