package com.fsck.k9.mailstore.migrations

import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.Account
import com.fsck.k9.mailstore.StorageManager
import com.fsck.k9.whenever
import org.apache.commons.io.IOUtils
import org.apache.james.mime4j.codec.QuotedPrintableInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream


@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MigrationTo51Test {
    private lateinit var mockMigrationsHelper: MigrationsHelper
    private lateinit var database: SQLiteDatabase


    @Before
    fun setUp() {
        val storageManager = StorageManager.getInstance(RuntimeEnvironment.application)
        storageManager.defaultProviderId

        val account = mock(Account::class.java)
        whenever(account.uuid).thenReturn("001")
        whenever(account.localStorageProviderId).thenReturn(storageManager.defaultProviderId)

        mockMigrationsHelper = mock(MigrationsHelper::class.java)
        whenever(mockMigrationsHelper.context).thenReturn(RuntimeEnvironment.application)
        whenever(mockMigrationsHelper.account).thenReturn(account)

        database = createWithV50Table()
    }

    @Test
    fun db51MigrateMessageFormat_canMigrateEmptyMessagesTable() {
        MigrationTo51.db51MigrateMessageFormat(database, mockMigrationsHelper)
    }

    @Test
    fun db51MigrateMessageFormat_canMigrateTextPlainMessage() {
        addTextPlainMessage()

        MigrationTo51.db51MigrateMessageFormat(database, mockMigrationsHelper)
    }

    @Test
    fun db51MigrateMessageFormat_canMigrateTextHtmlMessage() {
        addTextHtmlMessage()

        MigrationTo51.db51MigrateMessageFormat(database, mockMigrationsHelper)
    }

    @Test
    fun db51MigrateMessageFormat_canMigrateMultipartAlternativeMessage() {
        addMultipartAlternativeMessage()

        MigrationTo51.db51MigrateMessageFormat(database, mockMigrationsHelper)
    }

    @Test
    fun db51MigrateMessageFormat_canMigrateMultipartMixedMessage() {
        addMultipartMixedMessage()

        MigrationTo51.db51MigrateMessageFormat(database, mockMigrationsHelper)
    }

    @Test
    fun db51MigrateMessageFormat_canMigrateMultipartMixedMessageWithAttachment() {
        addMultipartMixedMessageWithAttachment(attachmentContentId = "content*user@host")

        MigrationTo51.db51MigrateMessageFormat(database, mockMigrationsHelper)
    }

    @Test
    fun db51MigrateMessageFormat_withMultipartMixedMessageWithAttachment_storesMessagePart() {
        addMultipartMixedMessageWithAttachment(attachmentContentId = "content*user@host")

        MigrationTo51.db51MigrateMessageFormat(database, mockMigrationsHelper)

        val isNotEmpty = loadHtmlMessagePartCursor().moveToNext()
        assertTrue(isNotEmpty)
    }

    @Test
    fun db51MigrateMessageFormat_withMultipartMixedMessageWithAttachment_updatesContentReference() {
        addMultipartMixedMessageWithAttachment(attachmentContentId = "content*user@host")

        MigrationTo51.db51MigrateMessageFormat(database, mockMigrationsHelper)

        assertEquals("""<html><img src="cid:content*user@host" /></html>""", htmlMessagePartBody())
    }

    @Test
    fun db51MigrateMessageFormat_withMultipartMixedMessageWithAttachmentWithUnusualContentID_updatesContentReference() {
        addMultipartMixedMessageWithAttachment(attachmentContentId = "a\$b@host")

        MigrationTo51.db51MigrateMessageFormat(database, mockMigrationsHelper)

        assertEquals("<html><img src=\"cid:a\$b@host\" /></html>", htmlMessagePartBody())
    }


    private fun createWithV50Table(): SQLiteDatabase {
        val database = SQLiteDatabase.create(null)
        database.execSQL("""
            CREATE TABLE messages (
                id INTEGER PRIMARY KEY,
                deleted INTEGER default 0,
                folder_id INTEGER, uid TEXT,
                subject TEXT,
                date INTEGER,
                sender_list TEXT,
                to_list TEXT,
                cc_list TEXT,
                bcc_list TEXT,
                reply_to_list TEXT,
                attachment_count INTEGER,
                internal_date INTEGER,
                message_id TEXT,
                preview TEXT,
                mime_type TEXT,
                html_content TEXT,
                text_content TEXT,
                flags TEXT,
                normalized_subject_hash INTEGER,
                empty INTEGER default 0,
                read INTEGER default 0,
                flagged INTEGER default 0,
                answered INTEGER default 0
             )
             """.trimIndent()
        )
        database.execSQL("""
            CREATE TABLE headers (
                id INTEGER PRIMARY KEY,
                name TEXT,
                value TEXT,
                message_id INTEGER
            )
            """.trimIndent()
        )
        database.execSQL("""
            CREATE TABLE attachments (
                id INTEGER PRIMARY KEY,
                size INTEGER,
                name TEXT,
                mime_type TEXT,
                store_data TEXT,
                content_uri TEXT,
                content_id TEXT,
                content_disposition TEXT,
                message_id INTEGER
            )
            """.trimIndent()
        )

        return database
    }

    private fun addTextPlainMessage() {
        insertMessage(mimeType = "text/plain", textContent = "Text")
    }

    private fun addTextHtmlMessage() {
        insertMessage(mimeType = "text/html", htmlContent = "<html></html>")
    }

    private fun addMultipartAlternativeMessage() {
        insertMessage(mimeType = "multipart/alternative", htmlContent = "<html></html>")
    }

    private fun addMultipartMixedMessage() {
        insertMessage(mimeType = "multipart/mixed", htmlContent = "<html></html>", textContent = "Text")
    }

    private fun addMultipartMixedMessageWithAttachment(attachmentContentId: String) {
        insertMessage(
                mimeType = "multipart/mixed",
                htmlContent = """<html><img src="testUri" /></html>""",
                attachmentCount = 1
        )
        insertImageAttachment(attachmentContentId)
    }

    private fun insertMessage(
            mimeType: String,
            htmlContent: String? = null,
            textContent: String? = null,
            attachmentCount: Int = 0
    ) {
        database.execSQL(
                "INSERT INTO messages (flags, html_content, text_content, mime_type, attachment_count) " +
                        "VALUES (?, ?, ?, ?, ?)",
                arrayOf("", htmlContent, textContent, mimeType, attachmentCount)
        )
    }

    private fun insertImageAttachment(cid: String) {
        database.execSQL(
                """
                    INSERT INTO attachments
                    (size, name, mime_type, store_data, content_uri, content_id, content_disposition, message_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(1, "a.jpg", "image/jpeg", "a", "testUri", cid, "disposition", 1)
        )
    }

    private fun loadHtmlMessagePartCursor() =
            database.query("message_parts", arrayOf("data"), "mime_type = 'text/html'", null, null, null, null)

    private fun htmlMessagePartBody(): String {
        val cursor = loadHtmlMessagePartCursor()
        if (!cursor.moveToNext()) {
            throw AssertionError("No message part found")
        }

        return IOUtils.toString(
                QuotedPrintableInputStream(
                        ByteArrayInputStream(cursor.getBlob(0))
                )
        )
    }
}
