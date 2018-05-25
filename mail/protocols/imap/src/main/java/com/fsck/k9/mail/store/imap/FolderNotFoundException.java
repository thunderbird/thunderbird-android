package com.fsck.k9.mail.store.imap;


import com.fsck.k9.mail.MessagingException;


public class FolderNotFoundException extends MessagingException {
    private final String folderServerId;


    public FolderNotFoundException(String folderServerId) {
        super("Folder not found: " + folderServerId);
        this.folderServerId = folderServerId;
    }

    public String getFolderServerId() {
        return folderServerId;
    }
}
