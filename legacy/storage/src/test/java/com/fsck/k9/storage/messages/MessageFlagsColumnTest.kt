package com.fsck.k9.storage.messages

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.fsck.k9.storage.RobolectricTest
import net.thunderbird.core.common.mail.Flag
import org.junit.After
import org.junit.Before
import org.junit.Test

class MessageFlagsColumnTest : RobolectricTest() {
    private lateinit var sqliteDatabase: SQLiteDatabase

    @Before
    fun setUp() {
        sqliteDatabase = createDatabase()
    }

    @After
    fun tearDown() {
        sqliteDatabase.close()
    }

    @Test
    fun `toDatabaseValue should omit flags stored in dedicated columns`() {
        val flags = setOf(Flag.SEEN, Flag.X_DOWNLOADED_FULL, Flag.FLAGGED, Flag.X_SUBJECT_DECRYPTED)

        val result = flags.toDatabaseValue()

        assertThat(result.split(',')).containsExactly(Flag.X_DOWNLOADED_FULL.name, Flag.X_SUBJECT_DECRYPTED.name)
    }

    @Test
    fun `updateMessageFlagsById should treat null flags as empty`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId = sqliteDatabase.createMessage(folderId = folderId, uid = "uid1")
        sqliteDatabase.update(
            "messages",
            ContentValues().apply {
                putNull("flags")
            },
            "id = ?",
            arrayOf(messageId.toString()),
        )

        sqliteDatabase.updateMessageFlagsById(listOf(messageId)) { flags ->
            flags + Flag.X_DOWNLOADED_PARTIAL
        }

        val message = sqliteDatabase.readMessages().single()
        assertThat(message.flags).isEqualTo(Flag.X_DOWNLOADED_PARTIAL.name)
    }

    @Test
    fun `updateMessageFlagsByServerId should preserve non-special flags and ignore blank entries`() {
        val folderId = sqliteDatabase.createFolder()
        sqliteDatabase.createMessage(
            folderId = folderId,
            uid = "uid1",
            flags = "X_DOWNLOADED_FULL,,X_SUBJECT_DECRYPTED",
        )

        sqliteDatabase.updateMessageFlagsByServerId(folderId, "uid1") { flags ->
            flags - Flag.X_DOWNLOADED_FULL + Flag.X_DOWNLOADED_PARTIAL
        }

        val message = sqliteDatabase.readMessages().single()
        assertThat(message.flags?.split(',').orEmpty()).containsExactly(
            Flag.X_SUBJECT_DECRYPTED.name,
            Flag.X_DOWNLOADED_PARTIAL.name,
        )
    }
}
