package com.fsck.k9.storage.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalStore
import timber.log.Timber

internal class FullTextIndexer(val localStore: LocalStore, val database: SQLiteDatabase) {
    private val fulltextCreator = localStore.messageFulltextCreator
    private val fetchProfile = FetchProfile().apply { add(FetchProfile.Item.BODY) }

    fun indexAllMessages() {
        try {
            val folders = localStore.getPersonalNamespaces(true)
            for (folder in folders) {
                indexFolder(folder)
            }
        } catch (e: MessagingException) {
            Timber.e(e, "error indexing fulltext - skipping rest, fts index is incomplete!")
        }
    }

    private fun indexFolder(folder: LocalFolder) {
        val messageUids = folder.allMessageUids
        for (messageUid in messageUids) {
            indexMessage(folder, messageUid)
        }
    }

    private fun indexMessage(folder: LocalFolder, messageUid: String?) {
        val localMessage = folder.getMessage(messageUid)
        folder.fetch(listOf(localMessage), fetchProfile, null)

        val fulltext = fulltextCreator.createFulltext(localMessage)
        if (fulltext.isNullOrEmpty()) {
            Timber.d("no fulltext for msg id %d :(", localMessage.databaseId)
        } else {
            Timber.d("fulltext for msg id %d is %d chars long", localMessage.databaseId, fulltext.length)

            val values = ContentValues().apply {
                put("docid", localMessage.databaseId)
                put("fulltext", fulltext)
            }
            database.insert("messages_fulltext", null, values)
        }
    }
}
