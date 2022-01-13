package com.fsck.k9.storage.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.helper.map
import com.fsck.k9.mailstore.MigrationsHelper

/**
 * Write the address fields to use ASCII 1 instead of ASCII 0 as separator.
 * Separator was previously ASCII 0 but this caused problems with LIKE and searching.
 */
internal class MigrationTo84(private val db: SQLiteDatabase, private val migrationsHelper: MigrationsHelper) {

    fun rewriteAddresses() {
        val addressSets = db.rawQuery(
            "SELECT id, to_list, cc_list, bcc_list, reply_to_list, sender_list FROM messages",
            null
        ).use { cursor ->
            cursor.map {

                val colVals = ArrayList<String?>(5)
                for (i in 1..5) {
                    colVals.add(if (it.isNull(i)) null else it.getString(i))
                }
                it.getLong(0) to AddressSet(colVals[0], colVals[1], colVals[2], colVals[3], colVals[4])
            }.toMap()
        }

        var i = 0
        for ((messageId, addressSet) in addressSets) {
            rewriteAddresses(messageId, addressSet)
            ++i
        }
    }

    private fun rewriteAddress(inAddress: String?): String? {
        val oldSeparator = '\u0000'
        val newSeparator = '\u0001'
        return inAddress?.replace(oldSeparator, newSeparator)
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

private data class AddressSet(val toList: String?, val ccList: String?, val bccList: String?, val replyToList: String?, val senderList: String?)
