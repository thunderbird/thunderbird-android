package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.internet.MimeMessage

class ImapMessage(uid: String) : MimeMessage() {
    init {
        this.mUid = uid
    }

    fun setSize(size: Int) {
        this.mSize = size
    }
}
