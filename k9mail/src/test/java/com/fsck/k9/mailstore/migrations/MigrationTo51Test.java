package com.fsck.k9.mailstore.migrations;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.fsck.k9.Account;
import com.fsck.k9.mailstore.StorageManager;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.codec.QuotedPrintableInputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class MigrationTo51Test {
    private MigrationsHelper mockMigrationsHelper;
    private Account account;
    private StorageManager storageManager;
    private SQLiteDatabase database;

    @Before
    public void before() {
        mockMigrationsHelper = mock(MigrationsHelper.class);
        account = mock(Account.class);
        storageManager = StorageManager.getInstance(RuntimeEnvironment.application);
        storageManager.getDefaultProviderId();
        when(account.getUuid()).thenReturn("001");
        when(account.getLocalStorageProviderId()).thenReturn(storageManager.getDefaultProviderId());
        when(mockMigrationsHelper.getContext()).thenReturn(RuntimeEnvironment.application);
        when(mockMigrationsHelper.getAccount()).thenReturn(account);

        database = createWithV50Table();
    }

    private SQLiteDatabase createWithV50Table() {
        SQLiteDatabase database = SQLiteDatabase.create(null);
        database.execSQL("CREATE TABLE messages (" +
                "id INTEGER PRIMARY KEY, " +
                "deleted INTEGER default 0, " +
                "folder_id INTEGER, " +
                "uid TEXT, " +
                "subject TEXT, " +
                "date INTEGER, " +
                "sender_list TEXT, " +
                "to_list TEXT, " +
                "cc_list TEXT, " +
                "bcc_list TEXT, " +
                "reply_to_list TEXT, " +
                "attachment_count INTEGER, " +
                "internal_date INTEGER, " +
                "message_id TEXT, " +
                "preview TEXT, " +
                "mime_type TEXT, " +
                "html_content TEXT, " +
                "text_content TEXT, " +
                "flags TEXT, " +
                "normalized_subject_hash INTEGER, " +
                "empty INTEGER default 0, " +
                "read INTEGER default 0, " +
                "flagged INTEGER default 0, " +
                "answered INTEGER default 0 )");
        database.execSQL("CREATE TABLE headers ("+
                "id INTEGER PRIMARY KEY," +
                "name TEXT," +
                "value TEXT," +
                "message_id INTEGER)");
        database.execSQL("CREATE TABLE attachments ("+
                "id INTEGER PRIMARY KEY," +
                "size INTEGER, " +
                "name TEXT, " +
                "mime_type TEXT, " +
                "store_data TEXT, " +
                "content_uri TEXT, " +
                "content_id TEXT, " +
                "content_disposition TEXT," +
                "message_id INTEGER)");
        return database;
    }

    private void addTextPlainMessage() {
        database.execSQL(
                "INSERT INTO messages (flags, html_content, text_content, " +
                        "mime_type, attachment_count) " +
                "VALUES(?,?,?,?,?)", new Object[]{"",null,"Text","text/plain",0});
    }

    private void addTextHtmlMessage() {
        database.execSQL(
                "INSERT INTO messages (flags, html_content, text_content, " +
                        "mime_type, attachment_count) " +
                        "VALUES(?,?,?,?,?)", new Object[]{"","<html></html>","Text","text/html",0});
    }

    private void addMultipartAlternativeMessage() {
        database.execSQL(
                "INSERT INTO messages (flags, html_content, text_content, " +
                        "mime_type, attachment_count) " +
                        "VALUES(?,?,?,?,?)", new Object[]{"","<html></html>",null,"multipart/alternative",0});
    }

    private void addMultipartMixedMessage() {
        database.execSQL(
                "INSERT INTO messages (flags, html_content, text_content, " +
                        "mime_type, attachment_count) " +
                        "VALUES(?,?,?,?,?)", new Object[]{"","<html></html>","Text","multipart/mixed",0});
    }

    private void addMultipartMixedMessageWithAttachment() {
        database.execSQL(
                "INSERT INTO messages (flags, html_content, text_content, " +
                        "mime_type, attachment_count) " +
                        "VALUES(?,?,?,?,?)",
                new Object[]{"","<html><img src=\"testUri\" /></html>",null,"multipart/mixed",1});
        database.execSQL(
                "INSERT INTO attachments (size, name, mime_type, store_data, " +
                        "content_uri, content_id, content_disposition, message_id) VALUES(?,?,?,?, ?,?,?,?)",
                new Object[]{1, "a.jpg", "image/jpg", "a", "testUri", "content*user@host", "disposition", 1}
        );
    }

    private void addMultipartMixedMessageWithAttachmentWithUnusualContentId() {
        database.execSQL(
                "INSERT INTO messages (flags, html_content, text_content, " +
                        "mime_type, attachment_count) " +
                        "VALUES(?,?,?,?,?)",
                new Object[]{"","<html><img src=\"testUri\" /></html>",null,"multipart/mixed",1});
        database.execSQL(
                "INSERT INTO attachments (size, name, mime_type, store_data, " +
                        "content_uri, content_id, content_disposition, message_id) VALUES(?,?,?,?, ?,?,?,?)",
                new Object[]{1, "a.jpg", "image/jpg", "a", "testUri", "a$b@host", "disposition", 1}
        );
    }

    @Test
    public void db51MigrateMessageFormat_canMigrateEmptyMessagesTable() {
        MigrationTo51.db51MigrateMessageFormat(database, mockMigrationsHelper);
    }

    @Test
    public void db51MigrateMessageFormat_canMigrateTextPlainMessage() {
        addTextPlainMessage();
        MigrationTo51.db51MigrateMessageFormat(database, mockMigrationsHelper);
    }

    @Test
    public void db51MigrateMessageFormat_canMigrateTextHtmlMessage() {
        addTextHtmlMessage();
        MigrationTo51.db51MigrateMessageFormat(database, mockMigrationsHelper);
    }

    @Test
    public void db51MigrateMessageFormat_canMigrateMultipartAlternativeMessage() {
        addMultipartAlternativeMessage();
        MigrationTo51.db51MigrateMessageFormat(database, mockMigrationsHelper);
    }

    @Test
    public void db51MigrateMessageFormat_canMigrateMultipartMixedMessage() {
        addMultipartMixedMessage();
        MigrationTo51.db51MigrateMessageFormat(database, mockMigrationsHelper);
    }

    @Test
    public void db51MigrateMessageFormat_canMigrateMultipartMixedMessageWithAttachment() {
        addMultipartMixedMessageWithAttachment();
        MigrationTo51.db51MigrateMessageFormat(database, mockMigrationsHelper);
    }

    @Test
    public void db51MigrateMessageFormat_withMultipartMixedMessageWithAttachment_stores_messagePart() {
        addMultipartMixedMessageWithAttachment();
        MigrationTo51.db51MigrateMessageFormat(database, mockMigrationsHelper);

        Cursor cursor = database.query("message_parts", new String[]{"data"}, "mime_type = 'text/html'", null, null, null, null);
        boolean isNotEmpty = cursor.moveToNext();

        assertTrue(isNotEmpty);
    }

    @Test
    public void db51MigrateMessageFormat_withMultipartMixedMessageWithAttachment_updatesContentReference()
            throws IOException {
        addMultipartMixedMessageWithAttachment();
        MigrationTo51.db51MigrateMessageFormat(database, mockMigrationsHelper);
        Cursor cursor = database.query("message_parts", new String[]{"data"}, "mime_type = 'text/html'", null, null, null, null);
        boolean isNotEmpty = cursor.moveToNext();
        assertTrue(isNotEmpty);
        String partDataAsString = IOUtils.toString(
                new QuotedPrintableInputStream(new ByteArrayInputStream(cursor.getBlob(0))));

        assertEquals("<html><img src=\"cid:content*user@host\" /></html>", partDataAsString);
    }

    @Test
    public void db51MigrateMessageFormat_withMultipartMixedMessageWithAttachmentWithUnusualContentID_updatesContentReference()
            throws IOException {
        addMultipartMixedMessageWithAttachmentWithUnusualContentId();
        MigrationTo51.db51MigrateMessageFormat(database, mockMigrationsHelper);
        Cursor cursor = database.query("message_parts", new String[]{"data"}, "mime_type = 'text/html'", null, null, null, null);
        boolean isNotEmpty = cursor.moveToNext();
        assertTrue(isNotEmpty);
        String partDataAsString = IOUtils.toString(
                new QuotedPrintableInputStream(new ByteArrayInputStream(cursor.getBlob(0))));

        assertEquals("<html><img src=\"cid:a$b@host\" /></html>", partDataAsString);
    }
}
