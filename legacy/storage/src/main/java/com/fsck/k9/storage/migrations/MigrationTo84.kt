package com.fsck.k9.storage.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getStringOrNull
import app.k9mail.core.android.common.database.map

/**
 * Write the address fields to use ASCII 1 instead of ASCII 0 as separator.
 * Separator was previously ASCII 0 but this caused problems with LIKE and searching.
 */
internal class MigrationTo84(private val db: SQLiteDatabase) {

    fun rewriteAddresses() {
        val addressSets = db.rawQuery(
            "SELECT id, to_list, cc_list, bcc_list, reply_to_list, sender_list " +
                "FROM messages WHERE empty = 0 AND deleted = 0",
            null,
        ).use { cursor ->
            cursor.map {
                val messageId = it.getLong(0)

                messageId to AddressSet(
                    toList = it.getStringOrNull(1),
                    ccList = it.getStringOrNull(2),
                    bccList = it.getStringOrNull(3),
                    replyToList = it.getStringOrNull(4),
                    senderList = it.getStringOrNull(5),
                )
            }.toMap()
        }

        for ((messageId, addressSet) in addressSets) {
            rewriteAddresses(messageId, addressSet)
        }
    }

    private fun rewriteAddress(inAddress: String?): String? {
        return inAddress?.replace(oldChar = '\u0000', newChar = '\u0001')
    }

    private fun rewriteAddresses(messageId: Long, addressSet: AddressSet) {
        val cv = ContentValues().apply {
            put("to_list", rewriteAddress(addressSet.toList))
            put("cc_list", rewriteAddress(addressSet.ccList))
            put("bcc_list", rewriteAddress(addressSet.bccList))
            put("reply_to_list", rewriteAddress(addressSet.replyToList))
            put("sender_list", rewriteAddress(addressSet.senderList))
        }
        db.update("messages", cv, "id = ?", arrayOf(messageId.toString()))
    }
}

private data class AddressSet(
    val toList: String?,
    val ccList: String?,
    val bccList: String?,
    val replyToList: String?,
    val senderList: String?,
)
