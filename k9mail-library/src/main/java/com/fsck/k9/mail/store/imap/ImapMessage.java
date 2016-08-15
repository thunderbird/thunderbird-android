package com.fsck.k9.mail.store.imap;


import java.util.Collections;

import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;


class ImapMessage extends MimeMessage {
    ImapMessage(String uid, Folder folder) {
        this.mUid = uid;
        this.mFolder = folder;
    }

    public void setSize(int size) {
        this.mSize = size;
    }

    public void setFlagInternal(Flag flag, boolean set) throws MessagingException {
        super.setFlag(flag, set);
    }

    @Override
    public void setFlag(Flag flag, boolean set) throws MessagingException {
        super.setFlag(flag, set);
        mFolder.setFlags(Collections.singletonList(this), Collections.singleton(flag), set);
    }

    @Override
    public void delete(String trashFolderName) throws MessagingException {
        getFolder().delete(Collections.singletonList(this), trashFolderName);
    }
}
