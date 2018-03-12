package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.MessagingException

class FolderNotFoundException(val folderServerId: String)
    : MessagingException("Folder not found: $folderServerId")
