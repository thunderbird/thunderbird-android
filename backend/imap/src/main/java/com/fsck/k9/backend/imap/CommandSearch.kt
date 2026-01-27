package com.fsck.k9.backend.imap

import com.fsck.k9.mail.store.imap.ImapStore
import com.fsck.k9.mail.store.imap.OpenMode
import net.thunderbird.core.common.mail.Flag

internal class CommandSearch(private val imapStore: ImapStore) {

    fun search(
        folderServerId: String,
        query: String?,
        requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?,
        performFullTextSearch: Boolean,
    ): List<String> {
        val folder = imapStore.getFolder(folderServerId)
        try {
            folder.open(OpenMode.READ_ONLY)

            return folder.search(query, requiredFlags, forbiddenFlags, performFullTextSearch)
                .sortedWith(UidReverseComparator())
                .map { it.uid }
        } finally {
            folder.close()
        }
    }
}
