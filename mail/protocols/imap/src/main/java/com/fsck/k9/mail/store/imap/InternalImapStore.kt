package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.Flag

internal interface InternalImapStore {
    val logLabel: String

    fun getCombinedPrefix(): String

    fun getPermanentFlagsIndex(): MutableSet<Flag>
}
