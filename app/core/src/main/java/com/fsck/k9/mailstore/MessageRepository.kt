package com.fsck.k9.mailstore

import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.mail.Header
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.message.MessageHeaderParser

class MessageRepository(
    private val preferences: Preferences,
    private val localStoreProvider: LocalStoreProvider
) {
    fun getHeaders(messageReference: MessageReference): List<Header> {
        val accountUuid = messageReference.accountUuid
        val account = preferences.getAccount(accountUuid) ?: error("Account not found: $accountUuid")
        return account.database.execute(false) { db ->
            db.rawQuery(
                "SELECT message_parts.header FROM messages" +
                    " LEFT JOIN message_parts ON (messages.message_part_id = message_parts.id)" +
                    " WHERE messages.folder_id = ? AND messages.uid = ?",
                arrayOf(messageReference.folderId.toString(), messageReference.uid),
            ).use { cursor ->
                if (!cursor.moveToFirst()) throw MessageNotFoundException(messageReference)

                val headerBytes = cursor.getBlob(0)

                val header = MimeHeader()
                MessageHeaderParser.parse(headerBytes.inputStream()) { name, value ->
                    header.addRawHeader(name, value)
                }

                header.headers
            }
        }
    }

    private val Account.database: LockableDatabase
        get() = localStoreProvider.getInstance(this).database
}
