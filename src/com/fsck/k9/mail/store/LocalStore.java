
package com.fsck.k9.mail.store;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.codec.QuotedPrintableOutputStream;
import org.apache.james.mime4j.util.MimeUtil;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.Account.MessageFormat;
import com.fsck.k9.activity.Search;
import com.fsck.k9.controller.MessageRemovalListener;
import com.fsck.k9.controller.MessageRetrievalListener;
import com.fsck.k9.helper.HtmlConverter;
import com.fsck.k9.helper.StringUtils;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.CompositeBody;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.filter.Base64OutputStream;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.internet.MimeUtility.ViewableContainer;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mail.store.LockableDatabase.DbCallback;
import com.fsck.k9.mail.store.LockableDatabase.WrappedException;
import com.fsck.k9.mail.store.StorageManager.StorageProvider;
import com.fsck.k9.provider.AttachmentProvider;
import com.fsck.k9.provider.EmailProvider;
import com.fsck.k9.provider.EmailProvider.MessageColumns;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchSpecification.Attribute;
import com.fsck.k9.search.SearchSpecification.Searchfield;
import com.fsck.k9.search.SqlQueryBuilder;

/**
 * <pre>
 * Implements a SQLite database backed local store for Messages.
 * </pre>
 */
public class LocalStore extends Store implements Serializable {

    private static final long serialVersionUID = -5142141896809423072L;

    private static final Message[] EMPTY_MESSAGE_ARRAY = new Message[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final Flag[] EMPTY_FLAG_ARRAY = new Flag[0];
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /*
     * a String containing the columns getMessages expects to work with
     * in the correct order.
     */
    static private String GET_MESSAGES_COLS =
        "subject, sender_list, date, uid, flags, messages.id, to_list, cc_list, " +
        "bcc_list, reply_to_list, attachment_count, internal_date, messages.message_id, " +
        "folder_id, preview, threads.id, threads.root, deleted, read, flagged, answered, " +
        "forwarded ";

    private static final String GET_FOLDER_COLS =
        "folders.id, name, visible_limit, last_updated, status, push_state, last_pushed, " +
        "integrate, top_group, poll_class, push_class, display_class";

    private static final int FOLDER_ID_INDEX = 0;
    private static final int FOLDER_NAME_INDEX = 1;
    private static final int FOLDER_VISIBLE_LIMIT_INDEX = 2;
    private static final int FOLDER_LAST_CHECKED_INDEX = 3;
    private static final int FOLDER_STATUS_INDEX = 4;
    private static final int FOLDER_PUSH_STATE_INDEX = 5;
    private static final int FOLDER_LAST_PUSHED_INDEX = 6;
    private static final int FOLDER_INTEGRATE_INDEX = 7;
    private static final int FOLDER_TOP_GROUP_INDEX = 8;
    private static final int FOLDER_SYNC_CLASS_INDEX = 9;
    private static final int FOLDER_PUSH_CLASS_INDEX = 10;
    private static final int FOLDER_DISPLAY_CLASS_INDEX = 11;

    private static final String[] UID_CHECK_PROJECTION = { "uid" };

    /**
     * Maximum number of UIDs to check for existence at once.
     *
     * @see LocalFolder#extractNewMessages(List)
     */
    private static final int UID_CHECK_BATCH_SIZE = 500;

    /**
     * Maximum number of messages to perform flag updates on at once.
     *
     * @see #setFlag(List, Flag, boolean, boolean)
     */
    private static final int FLAG_UPDATE_BATCH_SIZE = 500;

    /**
     * Maximum number of threads to perform flag updates on at once.
     *
     * @see #setFlagForThreads(List, Flag, boolean)
     */
    private static final int THREAD_FLAG_UPDATE_BATCH_SIZE = 500;

    public static final int DB_VERSION = 49;


    public static String getColumnNameForFlag(Flag flag) {
        switch (flag) {
            case SEEN: {
                return MessageColumns.READ;
            }
            case FLAGGED: {
                return MessageColumns.FLAGGED;
            }
            case ANSWERED: {
                return MessageColumns.ANSWERED;
            }
            case FORWARDED: {
                return MessageColumns.FORWARDED;
            }
            default: {
                throw new IllegalArgumentException("Flag must be a special column flag");
            }
        }
    }


    protected String uUid = null;

    private final Application mApplication;

    private LockableDatabase database;

    private ContentResolver mContentResolver;

    /**
     * local://localhost/path/to/database/uuid.db
     * This constructor is only used by {@link Store#getLocalInstance(Account, Application)}
     * @param account
     * @param application
     * @throws UnavailableStorageException if not {@link StorageProvider#isReady(Context)}
     */
    public LocalStore(final Account account, final Application application) throws MessagingException {
        super(account);
        database = new LockableDatabase(application, account.getUuid(), new StoreSchemaDefinition());

        mApplication = application;
        mContentResolver = application.getContentResolver();
        database.setStorageProviderId(account.getLocalStorageProviderId());
        uUid = account.getUuid();

        database.open();
    }

    public void switchLocalStorage(final String newStorageProviderId) throws MessagingException {
        database.switchProvider(newStorageProviderId);
    }

    protected SharedPreferences getPreferences() {
        return Preferences.getPreferences(mApplication).getPreferences();
    }

    private class StoreSchemaDefinition implements LockableDatabase.SchemaDefinition {
        @Override
        public int getVersion() {
            return DB_VERSION;
        }

        @Override
        public void doDbUpgrade(final SQLiteDatabase db) {
            try {
                upgradeDatabase(db);
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Exception while upgrading database. Resetting the DB to v0", e);
                db.setVersion(0);
                upgradeDatabase(db);
            }
        }

        private void upgradeDatabase(final SQLiteDatabase db) {
            Log.i(K9.LOG_TAG, String.format("Upgrading database from version %d to version %d",
                                            db.getVersion(), DB_VERSION));

            AttachmentProvider.clear(mApplication);

            db.beginTransaction();
            try {
                // schema version 29 was when we moved to incremental updates
                // in the case of a new db or a < v29 db, we blow away and start from scratch
                if (db.getVersion() < 29) {

                    db.execSQL("DROP TABLE IF EXISTS folders");
                    db.execSQL("CREATE TABLE folders (id INTEGER PRIMARY KEY, name TEXT, "
                               + "last_updated INTEGER, unread_count INTEGER, visible_limit INTEGER, status TEXT, "
                               + "push_state TEXT, last_pushed INTEGER, flagged_count INTEGER default 0, "
                               + "integrate INTEGER, top_group INTEGER, poll_class TEXT, push_class TEXT, display_class TEXT"
                               + ")");

                    db.execSQL("CREATE INDEX IF NOT EXISTS folder_name ON folders (name)");
                    db.execSQL("DROP TABLE IF EXISTS messages");
                    db.execSQL("CREATE TABLE messages (" +
                            "id INTEGER PRIMARY KEY, " +
                            "deleted INTEGER default 0, " +
                            "folder_id INTEGER, " +
                            "uid TEXT, " +
                            "subject TEXT, " +
                            "date INTEGER, " +
                            "flags TEXT, " +
                            "sender_list TEXT, " +
                            "to_list TEXT, " +
                            "cc_list TEXT, " +
                            "bcc_list TEXT, " +
                            "reply_to_list TEXT, " +
                            "html_content TEXT, " +
                            "text_content TEXT, " +
                            "attachment_count INTEGER, " +
                            "internal_date INTEGER, " +
                            "message_id TEXT, " +
                            "preview TEXT, " +
                            "mime_type TEXT, "+
                            "normalized_subject_hash INTEGER, " +
                            "empty INTEGER, " +
                            "read INTEGER default 0, " +
                            "flagged INTEGER default 0, " +
                            "answered INTEGER default 0, " +
                            "forwarded INTEGER default 0" +
                            ")");

                    db.execSQL("DROP TABLE IF EXISTS headers");
                    db.execSQL("CREATE TABLE headers (id INTEGER PRIMARY KEY, message_id INTEGER, name TEXT, value TEXT)");
                    db.execSQL("CREATE INDEX IF NOT EXISTS header_folder ON headers (message_id)");

                    db.execSQL("CREATE INDEX IF NOT EXISTS msg_uid ON messages (uid, folder_id)");
                    db.execSQL("DROP INDEX IF EXISTS msg_folder_id");
                    db.execSQL("DROP INDEX IF EXISTS msg_folder_id_date");
                    db.execSQL("CREATE INDEX IF NOT EXISTS msg_folder_id_deleted_date ON messages (folder_id,deleted,internal_date)");

                    db.execSQL("DROP INDEX IF EXISTS msg_empty");
                    db.execSQL("CREATE INDEX IF NOT EXISTS msg_empty ON messages (empty)");

                    db.execSQL("DROP INDEX IF EXISTS msg_read");
                    db.execSQL("CREATE INDEX IF NOT EXISTS msg_read ON messages (read)");

                    db.execSQL("DROP INDEX IF EXISTS msg_flagged");
                    db.execSQL("CREATE INDEX IF NOT EXISTS msg_flagged ON messages (flagged)");

                    db.execSQL("DROP INDEX IF EXISTS msg_composite");
                    db.execSQL("CREATE INDEX IF NOT EXISTS msg_composite ON messages (deleted, empty,folder_id,flagged,read)");



                    db.execSQL("DROP TABLE IF EXISTS threads");
                    db.execSQL("CREATE TABLE threads (" +
                            "id INTEGER PRIMARY KEY, " +
                            "message_id INTEGER, " +
                            "root INTEGER, " +
                            "parent INTEGER" +
                            ")");

                    db.execSQL("DROP INDEX IF EXISTS threads_message_id");
                    db.execSQL("CREATE INDEX IF NOT EXISTS threads_message_id ON threads (message_id)");

                    db.execSQL("DROP INDEX IF EXISTS threads_root");
                    db.execSQL("CREATE INDEX IF NOT EXISTS threads_root ON threads (root)");

                    db.execSQL("DROP INDEX IF EXISTS threads_parent");
                    db.execSQL("CREATE INDEX IF NOT EXISTS threads_parent ON threads (parent)");

                    db.execSQL("DROP TRIGGER IF EXISTS set_thread_root");
                    db.execSQL("CREATE TRIGGER set_thread_root " +
                            "AFTER INSERT ON threads " +
                            "BEGIN " +
                            "UPDATE threads SET root=id WHERE root IS NULL AND ROWID = NEW.ROWID; " +
                            "END");

                    db.execSQL("DROP TABLE IF EXISTS attachments");
                    db.execSQL("CREATE TABLE attachments (id INTEGER PRIMARY KEY, message_id INTEGER,"
                               + "store_data TEXT, content_uri TEXT, size INTEGER, name TEXT,"
                               + "mime_type TEXT, content_id TEXT, content_disposition TEXT)");

                    db.execSQL("DROP TABLE IF EXISTS pending_commands");
                    db.execSQL("CREATE TABLE pending_commands " +
                               "(id INTEGER PRIMARY KEY, command TEXT, arguments TEXT)");

                    db.execSQL("DROP TRIGGER IF EXISTS delete_folder");
                    db.execSQL("CREATE TRIGGER delete_folder BEFORE DELETE ON folders BEGIN DELETE FROM messages WHERE old.id = folder_id; END;");

                    db.execSQL("DROP TRIGGER IF EXISTS delete_message");
                    db.execSQL("CREATE TRIGGER delete_message BEFORE DELETE ON messages BEGIN DELETE FROM attachments WHERE old.id = message_id; "
                               + "DELETE FROM headers where old.id = message_id; END;");
                } else {
                    // in the case that we're starting out at 29 or newer, run all the needed updates

                    if (db.getVersion() < 30) {
                        try {
                            db.execSQL("ALTER TABLE messages ADD deleted INTEGER default 0");
                        } catch (SQLiteException e) {
                            if (! e.toString().startsWith("duplicate column name: deleted")) {
                                throw e;
                            }
                        }
                    }
                    if (db.getVersion() < 31) {
                        db.execSQL("DROP INDEX IF EXISTS msg_folder_id_date");
                        db.execSQL("CREATE INDEX IF NOT EXISTS msg_folder_id_deleted_date ON messages (folder_id,deleted,internal_date)");
                    }
                    if (db.getVersion() < 32) {
                        db.execSQL("UPDATE messages SET deleted = 1 WHERE flags LIKE '%DELETED%'");
                    }
                    if (db.getVersion() < 33) {

                        try {
                            db.execSQL("ALTER TABLE messages ADD preview TEXT");
                        } catch (SQLiteException e) {
                            if (! e.toString().startsWith("duplicate column name: preview")) {
                                throw e;
                            }
                        }

                    }
                    if (db.getVersion() < 34) {
                        try {
                            db.execSQL("ALTER TABLE folders ADD flagged_count INTEGER default 0");
                        } catch (SQLiteException e) {
                            if (! e.getMessage().startsWith("duplicate column name: flagged_count")) {
                                throw e;
                            }
                        }
                    }
                    if (db.getVersion() < 35) {
                        try {
                            db.execSQL("update messages set flags = replace(flags, 'X_NO_SEEN_INFO', 'X_BAD_FLAG')");
                        } catch (SQLiteException e) {
                            Log.e(K9.LOG_TAG, "Unable to get rid of obsolete flag X_NO_SEEN_INFO", e);
                        }
                    }
                    if (db.getVersion() < 36) {
                        try {
                            db.execSQL("ALTER TABLE attachments ADD content_id TEXT");
                        } catch (SQLiteException e) {
                            Log.e(K9.LOG_TAG, "Unable to add content_id column to attachments");
                        }
                    }
                    if (db.getVersion() < 37) {
                        try {
                            db.execSQL("ALTER TABLE attachments ADD content_disposition TEXT");
                        } catch (SQLiteException e) {
                            Log.e(K9.LOG_TAG, "Unable to add content_disposition column to attachments");
                        }
                    }

                    // Database version 38 is solely to prune cached attachments now that we clear them better
                    if (db.getVersion() < 39) {
                        try {
                            db.execSQL("DELETE FROM headers WHERE id in (SELECT headers.id FROM headers LEFT JOIN messages ON headers.message_id = messages.id WHERE messages.id IS NULL)");
                        } catch (SQLiteException e) {
                            Log.e(K9.LOG_TAG, "Unable to remove extra header data from the database");
                        }
                    }

                    // V40: Store the MIME type for a message.
                    if (db.getVersion() < 40) {
                        try {
                            db.execSQL("ALTER TABLE messages ADD mime_type TEXT");
                        } catch (SQLiteException e) {
                            Log.e(K9.LOG_TAG, "Unable to add mime_type column to messages");
                        }
                    }

                    if (db.getVersion() < 41) {
                        try {
                            db.execSQL("ALTER TABLE folders ADD integrate INTEGER");
                            db.execSQL("ALTER TABLE folders ADD top_group INTEGER");
                            db.execSQL("ALTER TABLE folders ADD poll_class TEXT");
                            db.execSQL("ALTER TABLE folders ADD push_class TEXT");
                            db.execSQL("ALTER TABLE folders ADD display_class TEXT");
                        } catch (SQLiteException e) {
                            if (! e.getMessage().startsWith("duplicate column name:")) {
                                throw e;
                            }
                        }
                        Cursor cursor = null;

                        try {

                            SharedPreferences prefs = getPreferences();
                            cursor = db.rawQuery("SELECT id, name FROM folders", null);
                            while (cursor.moveToNext()) {
                                try {
                                    int id = cursor.getInt(0);
                                    String name = cursor.getString(1);
                                    update41Metadata(db, prefs, id, name);
                                } catch (Exception e) {
                                    Log.e(K9.LOG_TAG, " error trying to ugpgrade a folder class", e);
                                }
                            }
                        }


                        catch (SQLiteException e) {
                            Log.e(K9.LOG_TAG, "Exception while upgrading database to v41. folder classes may have vanished", e);

                        } finally {
                            Utility.closeQuietly(cursor);
                        }
                    }
                    if (db.getVersion() == 41) {
                        try {
                            long startTime = System.currentTimeMillis();
                            SharedPreferences.Editor editor = getPreferences().edit();

                            List <? extends Folder >  folders = getPersonalNamespaces(true);
                            for (Folder folder : folders) {
                                if (folder instanceof LocalFolder) {
                                    LocalFolder lFolder = (LocalFolder)folder;
                                    lFolder.save(editor);
                                }
                            }

                            editor.commit();
                            long endTime = System.currentTimeMillis();
                            Log.i(K9.LOG_TAG, "Putting folder preferences for " + folders.size() + " folders back into Preferences took " + (endTime - startTime) + " ms");
                        } catch (Exception e) {
                            Log.e(K9.LOG_TAG, "Could not replace Preferences in upgrade from DB_VERSION 41", e);
                        }
                    }
                    if (db.getVersion() < 43) {
                        try {
                            // If folder "OUTBOX" (old, v3.800 - v3.802) exists, rename it to
                            // "K9MAIL_INTERNAL_OUTBOX" (new)
                            LocalFolder oldOutbox = new LocalFolder("OUTBOX");
                            if (oldOutbox.exists()) {
                                ContentValues cv = new ContentValues();
                                cv.put("name", Account.OUTBOX);
                                db.update("folders", cv, "name = ?", new String[] { "OUTBOX" });
                                Log.i(K9.LOG_TAG, "Renamed folder OUTBOX to " + Account.OUTBOX);
                            }

                            // Check if old (pre v3.800) localized outbox folder exists
                            String localizedOutbox = K9.app.getString(R.string.special_mailbox_name_outbox);
                            LocalFolder obsoleteOutbox = new LocalFolder(localizedOutbox);
                            if (obsoleteOutbox.exists()) {
                                // Get all messages from the localized outbox ...
                                Message[] messages = obsoleteOutbox.getMessages(null, false);

                                if (messages.length > 0) {
                                    // ... and move them to the drafts folder (we don't want to
                                    // surprise the user by sending potentially very old messages)
                                    LocalFolder drafts = new LocalFolder(mAccount.getDraftsFolderName());
                                    obsoleteOutbox.moveMessages(messages, drafts);
                                }

                                // Now get rid of the localized outbox
                                obsoleteOutbox.delete();
                                obsoleteOutbox.delete(true);
                            }
                        } catch (Exception e) {
                            Log.e(K9.LOG_TAG, "Error trying to fix the outbox folders", e);
                        }
                    }
                    if (db.getVersion() < 44) {
                        try {
                            db.execSQL("ALTER TABLE messages ADD thread_root INTEGER");
                            db.execSQL("ALTER TABLE messages ADD thread_parent INTEGER");
                            db.execSQL("ALTER TABLE messages ADD normalized_subject_hash INTEGER");
                            db.execSQL("ALTER TABLE messages ADD empty INTEGER");
                        } catch (SQLiteException e) {
                            if (! e.getMessage().startsWith("duplicate column name:")) {
                                throw e;
                            }
                        }
                    }
                    if (db.getVersion() < 45) {
                        try {
                            db.execSQL("DROP INDEX IF EXISTS msg_empty");
                            db.execSQL("CREATE INDEX IF NOT EXISTS msg_empty ON messages (empty)");

                            db.execSQL("DROP INDEX IF EXISTS msg_thread_root");
                            db.execSQL("CREATE INDEX IF NOT EXISTS msg_thread_root ON messages (thread_root)");

                            db.execSQL("DROP INDEX IF EXISTS msg_thread_parent");
                            db.execSQL("CREATE INDEX IF NOT EXISTS msg_thread_parent ON messages (thread_parent)");
                        } catch (SQLiteException e) {
                            if (! e.getMessage().startsWith("duplicate column name:")) {
                                throw e;
                            }
                        }
                    }
                    if (db.getVersion() < 46) {
                        db.execSQL("ALTER TABLE messages ADD read INTEGER default 0");
                        db.execSQL("ALTER TABLE messages ADD flagged INTEGER default 0");
                        db.execSQL("ALTER TABLE messages ADD answered INTEGER default 0");
                        db.execSQL("ALTER TABLE messages ADD forwarded INTEGER default 0");

                        String[] projection = { "id", "flags" };

                        ContentValues cv = new ContentValues();
                        List<Flag> extraFlags = new ArrayList<Flag>();

                        Cursor cursor = db.query("messages", projection, null, null, null, null, null);
                        try {
                            while (cursor.moveToNext()) {
                                long id = cursor.getLong(0);
                                String flagList = cursor.getString(1);

                                boolean read = false;
                                boolean flagged = false;
                                boolean answered = false;
                                boolean forwarded = false;

                                if (flagList != null && flagList.length() > 0) {
                                    String[] flags = flagList.split(",");

                                    for (String flagStr : flags) {
                                        try {
                                            Flag flag = Flag.valueOf(flagStr);

                                            switch (flag) {
                                                case ANSWERED: {
                                                    answered = true;
                                                    break;
                                                }
                                                case DELETED: {
                                                    // Don't store this in column 'flags'
                                                    break;
                                                }
                                                case FLAGGED: {
                                                    flagged = true;
                                                    break;
                                                }
                                                case FORWARDED: {
                                                    forwarded = true;
                                                    break;
                                                }
                                                case SEEN: {
                                                    read = true;
                                                    break;
                                                }
                                                case DRAFT:
                                                case RECENT:
                                                case X_DESTROYED:
                                                case X_DOWNLOADED_FULL:
                                                case X_DOWNLOADED_PARTIAL:
                                                case X_GOT_ALL_HEADERS:
                                                case X_REMOTE_COPY_STARTED:
                                                case X_SEND_FAILED:
                                                case X_SEND_IN_PROGRESS: {
                                                    extraFlags.add(flag);
                                                    break;
                                                }
                                            }
                                        } catch (Exception e) {
                                            // Ignore bad flags
                                        }
                                    }
                                }


                                cv.put("flags", serializeFlags(extraFlags.toArray(EMPTY_FLAG_ARRAY)));
                                cv.put("read", read);
                                cv.put("flagged", flagged);
                                cv.put("answered", answered);
                                cv.put("forwarded", forwarded);

                                db.update("messages", cv, "id = ?", new String[] { Long.toString(id) });

                                cv.clear();
                                extraFlags.clear();
                            }
                        } finally {
                            cursor.close();
                        }

                        db.execSQL("CREATE INDEX IF NOT EXISTS msg_read ON messages (read)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS msg_flagged ON messages (flagged)");
                    }

                    if (db.getVersion() < 47) {
                        // Create new 'threads' table
                        db.execSQL("DROP TABLE IF EXISTS threads");
                        db.execSQL("CREATE TABLE threads (" +
                                "id INTEGER PRIMARY KEY, " +
                                "message_id INTEGER, " +
                                "root INTEGER, " +
                                "parent INTEGER" +
                                ")");

                        // Create indices for new table
                        db.execSQL("DROP INDEX IF EXISTS threads_message_id");
                        db.execSQL("CREATE INDEX IF NOT EXISTS threads_message_id ON threads (message_id)");

                        db.execSQL("DROP INDEX IF EXISTS threads_root");
                        db.execSQL("CREATE INDEX IF NOT EXISTS threads_root ON threads (root)");

                        db.execSQL("DROP INDEX IF EXISTS threads_parent");
                        db.execSQL("CREATE INDEX IF NOT EXISTS threads_parent ON threads (parent)");

                        // Create entries for all messages in 'threads' table
                        db.execSQL("INSERT INTO threads (message_id) SELECT id FROM messages");

                        // Copy thread structure from 'messages' table to 'threads'
                        Cursor cursor = db.query("messages",
                                new String[] { "id", "thread_root", "thread_parent" },
                                null, null, null, null, null);
                        try {
                            ContentValues cv = new ContentValues();
                            while (cursor.moveToNext()) {
                                cv.clear();
                                long messageId = cursor.getLong(0);

                                if (!cursor.isNull(1)) {
                                    long threadRootMessageId = cursor.getLong(1);
                                    db.execSQL("UPDATE threads SET root = (SELECT t.id FROM " +
                                            "threads t WHERE t.message_id = ?) " +
                                            "WHERE message_id = ?",
                                            new String[] {
                                                Long.toString(threadRootMessageId),
                                                Long.toString(messageId)
                                            });
                                }

                                if (!cursor.isNull(2)) {
                                    long threadParentMessageId = cursor.getLong(2);
                                    db.execSQL("UPDATE threads SET parent = (SELECT t.id FROM " +
                                            "threads t WHERE t.message_id = ?) " +
                                            "WHERE message_id = ?",
                                            new String[] {
                                                Long.toString(threadParentMessageId),
                                                Long.toString(messageId)
                                            });
                                }
                            }
                        } finally {
                            cursor.close();
                        }

                        // Remove indices for old thread-related columns in 'messages' table
                        db.execSQL("DROP INDEX IF EXISTS msg_thread_root");
                        db.execSQL("DROP INDEX IF EXISTS msg_thread_parent");

                        // Clear out old thread-related columns in 'messages'
                        ContentValues cv = new ContentValues();
                        cv.putNull("thread_root");
                        cv.putNull("thread_parent");
                        db.update("messages", cv, null, null);
                    }

                    if (db.getVersion() < 48) {
                        db.execSQL("UPDATE threads SET root=id WHERE root IS NULL");

                        db.execSQL("CREATE TRIGGER set_thread_root " +
                                "AFTER INSERT ON threads " +
                                "BEGIN " +
                                "UPDATE threads SET root=id WHERE root IS NULL AND ROWID = NEW.ROWID; " +
                                "END");
                    }
                    if (db.getVersion() < 49) {
                        db.execSQL("CREATE INDEX IF NOT EXISTS msg_composite ON messages (deleted, empty,folder_id,flagged,read)");

                    }
                }

                db.setVersion(DB_VERSION);

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            if (db.getVersion() != DB_VERSION) {
                throw new RuntimeException("Database upgrade failed!");
            }
        }

        private void update41Metadata(final SQLiteDatabase  db, SharedPreferences prefs, int id, String name) {


            Folder.FolderClass displayClass = Folder.FolderClass.NO_CLASS;
            Folder.FolderClass syncClass = Folder.FolderClass.INHERITED;
            Folder.FolderClass pushClass = Folder.FolderClass.SECOND_CLASS;
            boolean inTopGroup = false;
            boolean integrate = false;
            if (mAccount.getInboxFolderName().equals(name)) {
                displayClass = Folder.FolderClass.FIRST_CLASS;
                syncClass =  Folder.FolderClass.FIRST_CLASS;
                pushClass =  Folder.FolderClass.FIRST_CLASS;
                inTopGroup = true;
                integrate = true;
            }

            try {
                displayClass = Folder.FolderClass.valueOf(prefs.getString(uUid + "." + name + ".displayMode", displayClass.name()));
                syncClass = Folder.FolderClass.valueOf(prefs.getString(uUid + "." + name + ".syncMode", syncClass.name()));
                pushClass = Folder.FolderClass.valueOf(prefs.getString(uUid + "." + name + ".pushMode", pushClass.name()));
                inTopGroup = prefs.getBoolean(uUid + "." + name + ".inTopGroup", inTopGroup);
                integrate = prefs.getBoolean(uUid + "." + name + ".integrate", integrate);
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, " Throwing away an error while trying to upgrade folder metadata", e);
            }

            if (displayClass == Folder.FolderClass.NONE) {
                displayClass = Folder.FolderClass.NO_CLASS;
            }
            if (syncClass == Folder.FolderClass.NONE) {
                syncClass = Folder.FolderClass.INHERITED;
            }
            if (pushClass == Folder.FolderClass.NONE) {
                pushClass = Folder.FolderClass.INHERITED;
            }

            db.execSQL("UPDATE folders SET integrate = ?, top_group = ?, poll_class=?, push_class =?, display_class = ? WHERE id = ?",
                       new Object[] { integrate, inTopGroup, syncClass, pushClass, displayClass, id });

        }
    }


    public long getSize() throws UnavailableStorageException {

        final StorageManager storageManager = StorageManager.getInstance(mApplication);

        final File attachmentDirectory = storageManager.getAttachmentDirectory(uUid,
                                         database.getStorageProviderId());

        return database.execute(false, new DbCallback<Long>() {
            @Override
            public Long doDbWork(final SQLiteDatabase db) {
                final File[] files = attachmentDirectory.listFiles();
                long attachmentLength = 0;
                for (File file : files) {
                    if (file.exists()) {
                        attachmentLength += file.length();
                    }
                }

                final File dbFile = storageManager.getDatabase(uUid, database.getStorageProviderId());
                return dbFile.length() + attachmentLength;
            }
        });
    }

    public void compact() throws MessagingException {
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "Before compaction size = " + getSize());

        database.execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws WrappedException {
                db.execSQL("VACUUM");
                return null;
            }
        });
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "After compaction size = " + getSize());
    }


    public void clear() throws MessagingException {
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "Before prune size = " + getSize());

        pruneCachedAttachments(true);
        if (K9.DEBUG) {
            Log.i(K9.LOG_TAG, "After prune / before compaction size = " + getSize());

            Log.i(K9.LOG_TAG, "Before clear folder count = " + getFolderCount());
            Log.i(K9.LOG_TAG, "Before clear message count = " + getMessageCount());

            Log.i(K9.LOG_TAG, "After prune / before clear size = " + getSize());
        }
        // don't delete messages that are Local, since there is no copy on the server.
        // Don't delete deleted messages.  They are essentially placeholders for UIDs of messages that have
        // been deleted locally.  They take up insignificant space
        database.execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) {
                // Delete entries from 'threads' table
                db.execSQL("DELETE FROM threads WHERE message_id IN " +
                        "(SELECT id FROM messages WHERE deleted = 0 AND uid NOT LIKE 'Local%')");

                // Set 'root' and 'parent' of remaining entries in 'thread' table to 'NULL' to make
                // sure the thread structure is in a valid state (this may destroy existing valid
                // thread trees, but is much faster than adjusting the tree by removing messages
                // one by one).
                db.execSQL("UPDATE threads SET root=id, parent=NULL");

                // Delete entries from 'messages' table
                db.execSQL("DELETE FROM messages WHERE deleted = 0 AND uid NOT LIKE 'Local%'");
                return null;
            }
        });

        compact();

        if (K9.DEBUG) {
            Log.i(K9.LOG_TAG, "After clear message count = " + getMessageCount());

            Log.i(K9.LOG_TAG, "After clear size = " + getSize());
        }
    }

    public int getMessageCount() throws MessagingException {
        return database.execute(false, new DbCallback<Integer>() {
            @Override
            public Integer doDbWork(final SQLiteDatabase db) {
                Cursor cursor = null;
                try {
                    cursor = db.rawQuery("SELECT COUNT(*) FROM messages", null);
                    cursor.moveToFirst();
                    return cursor.getInt(0);   // message count
                } finally {
                    Utility.closeQuietly(cursor);
                }
            }
        });
    }

    public int getFolderCount() throws MessagingException {
        return database.execute(false, new DbCallback<Integer>() {
            @Override
            public Integer doDbWork(final SQLiteDatabase db) {
                Cursor cursor = null;
                try {
                    cursor = db.rawQuery("SELECT COUNT(*) FROM folders", null);
                    cursor.moveToFirst();
                    return cursor.getInt(0);        // folder count
                } finally {
                    Utility.closeQuietly(cursor);
                }
            }
        });
    }

    @Override
    public LocalFolder getFolder(String name) {
        return new LocalFolder(name);
    }

    public LocalFolder getFolderById(long folderId) {
        return new LocalFolder(folderId);
    }

    // TODO this takes about 260-300ms, seems slow.
    @Override
    public List <? extends Folder > getPersonalNamespaces(boolean forceListAll) throws MessagingException {
        final List<LocalFolder> folders = new LinkedList<LocalFolder>();
        try {
            database.execute(false, new DbCallback < List <? extends Folder >> () {
                @Override
                public List <? extends Folder > doDbWork(final SQLiteDatabase db) throws WrappedException {
                    Cursor cursor = null;

                    try {
                        cursor = db.rawQuery("SELECT " + GET_FOLDER_COLS + " FROM folders " +
                                "ORDER BY name ASC", null);
                        while (cursor.moveToNext()) {
                            if (cursor.isNull(FOLDER_ID_INDEX)) {
                                continue;
                            }
                            String folderName = cursor.getString(FOLDER_NAME_INDEX);
                            LocalFolder folder = new LocalFolder(folderName);
                            folder.open(cursor);

                            folders.add(folder);
                        }
                        return folders;
                    } catch (MessagingException e) {
                        throw new WrappedException(e);
                    } finally {
                        Utility.closeQuietly(cursor);
                    }
                }
            });
        } catch (WrappedException e) {
            throw(MessagingException) e.getCause();
        }
        return folders;
    }

    @Override
    public void checkSettings() throws MessagingException {
    }

    public void delete() throws UnavailableStorageException {
        database.delete();
    }

    public void recreate() throws UnavailableStorageException {
        database.recreate();
    }

    public void pruneCachedAttachments() throws MessagingException {
        pruneCachedAttachments(false);
    }

    /**
     * Deletes all cached attachments for the entire store.
     * @param force
     * @throws com.fsck.k9.mail.MessagingException
     */
    private void pruneCachedAttachments(final boolean force) throws MessagingException {
        database.execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws WrappedException {
                if (force) {
                    ContentValues cv = new ContentValues();
                    cv.putNull("content_uri");
                    db.update("attachments", cv, null, null);
                }
                final StorageManager storageManager = StorageManager.getInstance(mApplication);
                File[] files = storageManager.getAttachmentDirectory(uUid, database.getStorageProviderId()).listFiles();
                for (File file : files) {
                    if (file.exists()) {
                        if (!force) {
                            Cursor cursor = null;
                            try {
                                cursor = db.query(
                                             "attachments",
                                             new String[] { "store_data" },
                                             "id = ?",
                                             new String[] { file.getName() },
                                             null,
                                             null,
                                             null);
                                if (cursor.moveToNext()) {
                                    if (cursor.getString(0) == null) {
                                        if (K9.DEBUG)
                                            Log.d(K9.LOG_TAG, "Attachment " + file.getAbsolutePath() + " has no store data, not deleting");
                                        /*
                                         * If the attachment has no store data it is not recoverable, so
                                         * we won't delete it.
                                         */
                                        continue;
                                    }
                                }
                            } finally {
                                Utility.closeQuietly(cursor);
                            }
                        }
                        if (!force) {
                            try {
                                ContentValues cv = new ContentValues();
                                cv.putNull("content_uri");
                                db.update("attachments", cv, "id = ?", new String[] { file.getName() });
                            } catch (Exception e) {
                                /*
                                 * If the row has gone away before we got to mark it not-downloaded that's
                                 * okay.
                                 */
                            }
                        }
                        if (!file.delete()) {
                            file.deleteOnExit();
                        }
                    }
                }
                return null;
            }
        });
    }

    public void resetVisibleLimits() throws UnavailableStorageException {
        resetVisibleLimits(mAccount.getDisplayCount());
    }

    public void resetVisibleLimits(int visibleLimit) throws UnavailableStorageException {
        final ContentValues cv = new ContentValues();
        cv.put("visible_limit", Integer.toString(visibleLimit));
        database.execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws WrappedException {
                db.update("folders", cv, null, null);
                return null;
            }
        });
    }

    public ArrayList<PendingCommand> getPendingCommands() throws UnavailableStorageException {
        return database.execute(false, new DbCallback<ArrayList<PendingCommand>>() {
            @Override
            public ArrayList<PendingCommand> doDbWork(final SQLiteDatabase db) throws WrappedException {
                Cursor cursor = null;
                try {
                    cursor = db.query("pending_commands",
                                      new String[] { "id", "command", "arguments" },
                                      null,
                                      null,
                                      null,
                                      null,
                                      "id ASC");
                    ArrayList<PendingCommand> commands = new ArrayList<PendingCommand>();
                    while (cursor.moveToNext()) {
                        PendingCommand command = new PendingCommand();
                        command.mId = cursor.getLong(0);
                        command.command = cursor.getString(1);
                        String arguments = cursor.getString(2);
                        command.arguments = arguments.split(",");
                        for (int i = 0; i < command.arguments.length; i++) {
                            command.arguments[i] = Utility.fastUrlDecode(command.arguments[i]);
                        }
                        commands.add(command);
                    }
                    return commands;
                } finally {
                    Utility.closeQuietly(cursor);
                }
            }
        });
    }

    public void addPendingCommand(PendingCommand command) throws UnavailableStorageException {
        try {
            for (int i = 0; i < command.arguments.length; i++) {
                command.arguments[i] = URLEncoder.encode(command.arguments[i], "UTF-8");
            }
            final ContentValues cv = new ContentValues();
            cv.put("command", command.command);
            cv.put("arguments", Utility.combine(command.arguments, ','));
            database.execute(false, new DbCallback<Void>() {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws WrappedException {
                    db.insert("pending_commands", "command", cv);
                    return null;
                }
            });
        } catch (UnsupportedEncodingException usee) {
            throw new Error("Aparently UTF-8 has been lost to the annals of history.");
        }
    }

    public void removePendingCommand(final PendingCommand command) throws UnavailableStorageException {
        database.execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws WrappedException {
                db.delete("pending_commands", "id = ?", new String[] { Long.toString(command.mId) });
                return null;
            }
        });
    }

    public void removePendingCommands() throws UnavailableStorageException {
        database.execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws WrappedException {
                db.delete("pending_commands", null, null);
                return null;
            }
        });
    }

    public static class PendingCommand {
        private long mId;
        public String command;
        public String[] arguments;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(command);
            sb.append(": ");
            for (String argument : arguments) {
                sb.append(", ");
                sb.append(argument);
                //sb.append("\n");
            }
            return sb.toString();
        }
    }

    @Override
    public boolean isMoveCapable() {
        return true;
    }

    @Override
    public boolean isCopyCapable() {
        return true;
    }

    public Message[] searchForMessages(MessageRetrievalListener retrievalListener,
                                        LocalSearch search) throws MessagingException {

        StringBuilder query = new StringBuilder();
        List<String> queryArgs = new ArrayList<String>();
        SqlQueryBuilder.buildWhereClause(mAccount, search.getConditions(), query, queryArgs);

        // Avoid "ambiguous column name" error by prefixing "id" with the message table name
        String where = SqlQueryBuilder.addPrefixToSelection(new String[] { "id" },
                "messages.", query.toString());

        String[] selectionArgs = queryArgs.toArray(EMPTY_STRING_ARRAY);

        String sqlQuery = "SELECT " + GET_MESSAGES_COLS + "FROM messages " +
                "LEFT JOIN threads ON (threads.message_id = messages.id) " +
                "LEFT JOIN folders ON (folders.id = messages.folder_id) WHERE " +
                "((empty IS NULL OR empty != 1) AND deleted = 0)" +
                ((!StringUtils.isNullOrEmpty(where)) ? " AND (" + where + ")" : "") +
                " ORDER BY date DESC";

        if (K9.DEBUG) {
            Log.d(K9.LOG_TAG, "Query = " + sqlQuery);
        }

        return getMessages(retrievalListener, null, sqlQuery, selectionArgs);
    }

    /*
     * Given a query string, actually do the query for the messages and
     * call the MessageRetrievalListener for each one
     */
    private Message[] getMessages(
        final MessageRetrievalListener listener,
        final LocalFolder folder,
        final String queryString, final String[] placeHolders
    ) throws MessagingException {
        final ArrayList<LocalMessage> messages = new ArrayList<LocalMessage>();
        final int j = database.execute(false, new DbCallback<Integer>() {
            @Override
            public Integer doDbWork(final SQLiteDatabase db) throws WrappedException {
                Cursor cursor = null;
                int i = 0;
                try {
                    cursor = db.rawQuery(queryString + " LIMIT 10", placeHolders);

                    while (cursor.moveToNext()) {
                        LocalMessage message = new LocalMessage(null, folder);
                        message.populateFromGetMessageCursor(cursor);

                        messages.add(message);
                        if (listener != null) {
                            listener.messageFinished(message, i, -1);
                        }
                        i++;
                    }
                    cursor.close();
                    cursor = db.rawQuery(queryString + " LIMIT -1 OFFSET 10", placeHolders);

                    while (cursor.moveToNext()) {
                        LocalMessage message = new LocalMessage(null, folder);
                        message.populateFromGetMessageCursor(cursor);

                        messages.add(message);
                        if (listener != null) {
                            listener.messageFinished(message, i, -1);
                        }
                        i++;
                    }
                } catch (Exception e) {
                    Log.d(K9.LOG_TAG, "Got an exception", e);
                } finally {
                    Utility.closeQuietly(cursor);
                }
                return i;
            }
        });
        if (listener != null) {
            listener.messagesFinished(j);
        }

        return messages.toArray(EMPTY_MESSAGE_ARRAY);

    }

    public Message[] getMessagesInThread(final long rootId) throws MessagingException {
        String rootIdString = Long.toString(rootId);

        LocalSearch search = new LocalSearch();
        search.and(Searchfield.THREAD_ID, rootIdString, Attribute.EQUALS);

        return searchForMessages(null, search);
    }

    public AttachmentInfo getAttachmentInfo(final String attachmentId) throws UnavailableStorageException {
        return database.execute(false, new DbCallback<AttachmentInfo>() {
            @Override
            public AttachmentInfo doDbWork(final SQLiteDatabase db) throws WrappedException {
                String name;
                String type;
                int size;
                Cursor cursor = null;
                try {
                    cursor = db.query(
                                 "attachments",
                                 new String[] { "name", "size", "mime_type" },
                                 "id = ?",
                                 new String[] { attachmentId },
                                 null,
                                 null,
                                 null);
                    if (!cursor.moveToFirst()) {
                        return null;
                    }
                    name = cursor.getString(0);
                    size = cursor.getInt(1);
                    type = cursor.getString(2);
                    final AttachmentInfo attachmentInfo = new AttachmentInfo();
                    attachmentInfo.name = name;
                    attachmentInfo.size = size;
                    attachmentInfo.type = type;
                    return attachmentInfo;
                } finally {
                    Utility.closeQuietly(cursor);
                }
            }
        });
    }

    public static class AttachmentInfo {
        public String name;
        public int size;
        public String type;
    }

    public void createFolders(final List<LocalFolder> foldersToCreate, final int visibleLimit) throws UnavailableStorageException {
        database.execute(true, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws WrappedException {
                for (LocalFolder folder : foldersToCreate) {
                    String name = folder.getName();
                    final  LocalFolder.PreferencesHolder prefHolder = folder.new PreferencesHolder();

                    // When created, special folders should always be displayed
                    // inbox should be integrated
                    // and the inbox and drafts folders should be syncced by default
                    if (mAccount.isSpecialFolder(name)) {
                        prefHolder.inTopGroup = true;
                        prefHolder.displayClass = LocalFolder.FolderClass.FIRST_CLASS;
                        if (name.equalsIgnoreCase(mAccount.getInboxFolderName())) {
                            prefHolder.integrate = true;
                            prefHolder.pushClass = LocalFolder.FolderClass.FIRST_CLASS;
                        } else {
                            prefHolder.pushClass = LocalFolder.FolderClass.INHERITED;

                        }
                        if (name.equalsIgnoreCase(mAccount.getInboxFolderName()) ||
                                name.equalsIgnoreCase(mAccount.getDraftsFolderName())) {
                            prefHolder.syncClass = LocalFolder.FolderClass.FIRST_CLASS;
                        } else {
                            prefHolder.syncClass = LocalFolder.FolderClass.NO_CLASS;
                        }
                    }
                    folder.refresh(name, prefHolder);   // Recover settings from Preferences

                    db.execSQL("INSERT INTO folders (name, visible_limit, top_group, display_class, poll_class, push_class, integrate) VALUES (?, ?, ?, ?, ?, ?, ?)", new Object[] {
                                   name,
                                   visibleLimit,
                                   prefHolder.inTopGroup ? 1 : 0,
                                   prefHolder.displayClass.name(),
                                   prefHolder.syncClass.name(),
                                   prefHolder.pushClass.name(),
                                   prefHolder.integrate ? 1 : 0,
                               });

                }
                return null;
            }
        });
    }


    private String serializeFlags(Flag[] flags) {
        List<Flag> extraFlags = new ArrayList<Flag>();

        for (Flag flag : flags) {
            switch (flag) {
                case DELETED:
                case SEEN:
                case FLAGGED:
                case ANSWERED:
                case FORWARDED: {
                    break;
                }
                default: {
                    extraFlags.add(flag);
                }
            }
        }

        return Utility.combine(extraFlags.toArray(EMPTY_FLAG_ARRAY), ',').toUpperCase(Locale.US);
    }

    public class LocalFolder extends Folder implements Serializable {
        /**
         *
         */
        private static final long serialVersionUID = -1973296520918624767L;
        private String mName = null;
        private long mFolderId = -1;
        private int mVisibleLimit = -1;
        private String prefId = null;
        private FolderClass mDisplayClass = FolderClass.NO_CLASS;
        private FolderClass mSyncClass = FolderClass.INHERITED;
        private FolderClass mPushClass = FolderClass.SECOND_CLASS;
        private boolean mInTopGroup = false;
        private String mPushState = null;
        private boolean mIntegrate = false;
        // mLastUid is used during syncs. It holds the highest UID within the local folder so we
        // know whether or not an unread message added to the local folder is actually "new" or not.
        private Integer mLastUid = null;

        public LocalFolder(String name) {
            super(LocalStore.this.mAccount);
            this.mName = name;

            if (LocalStore.this.mAccount.getInboxFolderName().equals(getName())) {

                mSyncClass =  FolderClass.FIRST_CLASS;
                mPushClass =  FolderClass.FIRST_CLASS;
                mInTopGroup = true;
            }


        }

        public LocalFolder(long id) {
            super(LocalStore.this.mAccount);
            this.mFolderId = id;
        }

        public long getId() {
            return mFolderId;
        }

        @Override
        public void open(final int mode) throws MessagingException {

            if (isOpen() && (getMode() == mode || mode == OPEN_MODE_RO)) {
                return;
            } else if (isOpen()) {
                //previously opened in READ_ONLY and now requesting READ_WRITE
                //so close connection and reopen
                close();
            }

            try {
                database.execute(false, new DbCallback<Void>() {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException {
                        Cursor cursor = null;
                        try {
                            String baseQuery = "SELECT " + GET_FOLDER_COLS + " FROM folders ";

                            if (mName != null) {
                                cursor = db.rawQuery(baseQuery + "where folders.name = ?", new String[] { mName });
                            } else {
                                cursor = db.rawQuery(baseQuery + "where folders.id = ?", new String[] { Long.toString(mFolderId) });
                            }

                            if (cursor.moveToFirst() && !cursor.isNull(FOLDER_ID_INDEX)) {
                                int folderId = cursor.getInt(FOLDER_ID_INDEX);
                                if (folderId > 0) {
                                    open(cursor);
                                }
                            } else {
                                Log.w(K9.LOG_TAG, "Creating folder " + getName() + " with existing id " + getId());
                                create(FolderType.HOLDS_MESSAGES);
                                open(mode);
                            }
                        } catch (MessagingException e) {
                            throw new WrappedException(e);
                        } finally {
                            Utility.closeQuietly(cursor);
                        }
                        return null;
                    }
                });
            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }
        }

        private void open(Cursor cursor) throws MessagingException {
            mFolderId = cursor.getInt(FOLDER_ID_INDEX);
            mName = cursor.getString(FOLDER_NAME_INDEX);
            mVisibleLimit = cursor.getInt(FOLDER_VISIBLE_LIMIT_INDEX);
            mPushState = cursor.getString(FOLDER_PUSH_STATE_INDEX);
            super.setStatus(cursor.getString(FOLDER_STATUS_INDEX));
            // Only want to set the local variable stored in the super class.  This class
            // does a DB update on setLastChecked
            super.setLastChecked(cursor.getLong(FOLDER_LAST_CHECKED_INDEX));
            super.setLastPush(cursor.getLong(FOLDER_LAST_PUSHED_INDEX));
            mInTopGroup = (cursor.getInt(FOLDER_TOP_GROUP_INDEX)) == 1  ? true : false;
            mIntegrate = (cursor.getInt(FOLDER_INTEGRATE_INDEX) == 1) ? true : false;
            String noClass = FolderClass.NO_CLASS.toString();
            String displayClass = cursor.getString(FOLDER_DISPLAY_CLASS_INDEX);
            mDisplayClass = Folder.FolderClass.valueOf((displayClass == null) ? noClass : displayClass);
            String pushClass = cursor.getString(FOLDER_PUSH_CLASS_INDEX);
            mPushClass = Folder.FolderClass.valueOf((pushClass == null) ? noClass : pushClass);
            String syncClass = cursor.getString(FOLDER_SYNC_CLASS_INDEX);
            mSyncClass = Folder.FolderClass.valueOf((syncClass == null) ? noClass : syncClass);
        }

        @Override
        public boolean isOpen() {
            return (mFolderId != -1 && mName != null);
        }

        @Override
        public int getMode() {
            return OPEN_MODE_RW;
        }

        @Override
        public String getName() {
            return mName;
        }

        @Override
        public boolean exists() throws MessagingException {
            return database.execute(false, new DbCallback<Boolean>() {
                @Override
                public Boolean doDbWork(final SQLiteDatabase db) throws WrappedException {
                    Cursor cursor = null;
                    try {
                        cursor = db.rawQuery("SELECT id FROM folders "
                                             + "where folders.name = ?", new String[] { LocalFolder.this
                                                     .getName()
                                                                                      });
                        if (cursor.moveToFirst()) {
                            int folderId = cursor.getInt(0);
                            return (folderId > 0);
                        }

                        return false;
                    } finally {
                        Utility.closeQuietly(cursor);
                    }
                }
            });
        }

        @Override
        public boolean create(FolderType type) throws MessagingException {
            return create(type, mAccount.getDisplayCount());
        }

        @Override
        public boolean create(FolderType type, final int visibleLimit) throws MessagingException {
            if (exists()) {
                throw new MessagingException("Folder " + mName + " already exists.");
            }
            List<LocalFolder> foldersToCreate = new ArrayList<LocalFolder>(1);
            foldersToCreate.add(this);
            LocalStore.this.createFolders(foldersToCreate, visibleLimit);

            return true;
        }

        private class PreferencesHolder {
            FolderClass displayClass = mDisplayClass;
            FolderClass syncClass = mSyncClass;
            FolderClass pushClass = mPushClass;
            boolean inTopGroup = mInTopGroup;
            boolean integrate = mIntegrate;
        }

        @Override
        public void close() {
            mFolderId = -1;
        }

        @Override
        public int getMessageCount() throws MessagingException {
            try {
                return database.execute(false, new DbCallback<Integer>() {
                    @Override
                    public Integer doDbWork(final SQLiteDatabase db) throws WrappedException {
                        try {
                            open(OPEN_MODE_RW);
                        } catch (MessagingException e) {
                            throw new WrappedException(e);
                        }
                        Cursor cursor = null;
                        try {
                            cursor = db.rawQuery("SELECT COUNT(id) FROM messages WHERE (empty IS NULL OR empty != 1) AND deleted = 0 and folder_id = ?",
                                                 new String[] {
                                                     Long.toString(mFolderId)
                                                 });
                            cursor.moveToFirst();
                            return cursor.getInt(0);   //messagecount
                        } finally {
                            Utility.closeQuietly(cursor);
                        }
                    }
                });
            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }
        }

        @Override
        public int getUnreadMessageCount() throws MessagingException {
            if (mFolderId == -1) {
                open(OPEN_MODE_RW);
            }

            try {
                return database.execute(false, new DbCallback<Integer>() {
                    @Override
                    public Integer doDbWork(final SQLiteDatabase db) throws WrappedException {
                        int unreadMessageCount = 0;
                        Cursor cursor = db.query("messages", new String[] { "COUNT(id)" },
                                "folder_id = ? AND (empty IS NULL OR empty != 1) AND deleted = 0 AND read=0",
                                new String[] { Long.toString(mFolderId) }, null, null, null);

                        try {
                            if (cursor.moveToFirst()) {
                                unreadMessageCount = cursor.getInt(0);
                            }
                        } finally {
                            cursor.close();
                        }

                        return unreadMessageCount;
                    }
                });
            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }
        }

        @Override
        public int getFlaggedMessageCount() throws MessagingException {
            if (mFolderId == -1) {
                open(OPEN_MODE_RW);
            }

            try {
                return database.execute(false, new DbCallback<Integer>() {
                    @Override
                    public Integer doDbWork(final SQLiteDatabase db) throws WrappedException {
                        int flaggedMessageCount = 0;
                        Cursor cursor = db.query("messages", new String[] { "COUNT(id)" },
                                "folder_id = ? AND (empty IS NULL OR empty != 1) AND deleted = 0 AND flagged = 1",
                                new String[] { Long.toString(mFolderId) }, null, null, null);

                        try {
                            if (cursor.moveToFirst()) {
                                flaggedMessageCount = cursor.getInt(0);
                            }
                        } finally {
                            cursor.close();
                        }

                        return flaggedMessageCount;
                    }
                });
            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }
        }

        @Override
        public void setLastChecked(final long lastChecked) throws MessagingException {
            try {
                open(OPEN_MODE_RW);
                LocalFolder.super.setLastChecked(lastChecked);
            } catch (MessagingException e) {
                throw new WrappedException(e);
            }
            updateFolderColumn("last_updated", lastChecked);
        }

        @Override
        public void setLastPush(final long lastChecked) throws MessagingException {
            try {
                open(OPEN_MODE_RW);
                LocalFolder.super.setLastPush(lastChecked);
            } catch (MessagingException e) {
                throw new WrappedException(e);
            }
            updateFolderColumn("last_pushed", lastChecked);
        }

        public int getVisibleLimit() throws MessagingException {
            open(OPEN_MODE_RW);
            return mVisibleLimit;
        }

        public void purgeToVisibleLimit(MessageRemovalListener listener) throws MessagingException {
            //don't purge messages while a Search is active since it might throw away search results
            if (!Search.isActive()) {
                if (mVisibleLimit == 0) {
                    return ;
                }
                open(OPEN_MODE_RW);
                Message[] messages = getMessages(null, false);
                for (int i = mVisibleLimit; i < messages.length; i++) {
                    if (listener != null) {
                        listener.messageRemoved(messages[i]);
                    }
                    messages[i].destroy();
                }
            }
        }


        public void setVisibleLimit(final int visibleLimit) throws MessagingException {
            mVisibleLimit = visibleLimit;
            updateFolderColumn("visible_limit", mVisibleLimit);
        }

        @Override
        public void setStatus(final String status) throws MessagingException {
            updateFolderColumn("status", status);
        }
        public void setPushState(final String pushState) throws MessagingException {
            mPushState = pushState;
            updateFolderColumn("push_state", pushState);
        }

        private void updateFolderColumn(final String column, final Object value) throws MessagingException {
            try {
                database.execute(false, new DbCallback<Void>() {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException {
                        try {
                            open(OPEN_MODE_RW);
                        } catch (MessagingException e) {
                            throw new WrappedException(e);
                        }
                        db.execSQL("UPDATE folders SET " + column + " = ? WHERE id = ?", new Object[] { value, mFolderId });
                        return null;
                    }
                });
            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }
        }

        public String getPushState() {
            return mPushState;
        }
        @Override
        public FolderClass getDisplayClass() {
            return mDisplayClass;
        }

        @Override
        public FolderClass getSyncClass() {
            return (FolderClass.INHERITED == mSyncClass) ? getDisplayClass() : mSyncClass;
        }

        public FolderClass getRawSyncClass() {
            return mSyncClass;
        }

        @Override
        public FolderClass getPushClass() {
            return (FolderClass.INHERITED == mPushClass) ? getSyncClass() : mPushClass;
        }

        public FolderClass getRawPushClass() {
            return mPushClass;
        }

        public void setDisplayClass(FolderClass displayClass) throws MessagingException {
            mDisplayClass = displayClass;
            updateFolderColumn("display_class", mDisplayClass.name());

        }

        public void setSyncClass(FolderClass syncClass) throws MessagingException {
            mSyncClass = syncClass;
            updateFolderColumn("poll_class", mSyncClass.name());
        }
        public void setPushClass(FolderClass pushClass) throws MessagingException {
            mPushClass = pushClass;
            updateFolderColumn("push_class", mPushClass.name());
        }

        public boolean isIntegrate() {
            return mIntegrate;
        }
        public void setIntegrate(boolean integrate) throws MessagingException {
            mIntegrate = integrate;
            updateFolderColumn("integrate", mIntegrate ? 1 : 0);
        }

        private String getPrefId(String name) {
            if (prefId == null) {
                prefId = uUid + "." + name;
            }

            return prefId;
        }

        private String getPrefId() throws MessagingException {
            open(OPEN_MODE_RW);
            return getPrefId(mName);

        }

        public void delete() throws MessagingException {
            String id = getPrefId();

            SharedPreferences.Editor editor = LocalStore.this.getPreferences().edit();

            editor.remove(id + ".displayMode");
            editor.remove(id + ".syncMode");
            editor.remove(id + ".pushMode");
            editor.remove(id + ".inTopGroup");
            editor.remove(id + ".integrate");

            editor.commit();
        }

        public void save() throws MessagingException {
            SharedPreferences.Editor editor = LocalStore.this.getPreferences().edit();
            save(editor);
            editor.commit();
        }

        public void save(SharedPreferences.Editor editor) throws MessagingException {
            String id = getPrefId();

            // there can be a lot of folders.  For the defaults, let's not save prefs, saving space, except for INBOX
            if (mDisplayClass == FolderClass.NO_CLASS && !mAccount.getInboxFolderName().equals(getName())) {
                editor.remove(id + ".displayMode");
            } else {
                editor.putString(id + ".displayMode", mDisplayClass.name());
            }

            if (mSyncClass == FolderClass.INHERITED && !mAccount.getInboxFolderName().equals(getName())) {
                editor.remove(id + ".syncMode");
            } else {
                editor.putString(id + ".syncMode", mSyncClass.name());
            }

            if (mPushClass == FolderClass.SECOND_CLASS && !mAccount.getInboxFolderName().equals(getName())) {
                editor.remove(id + ".pushMode");
            } else {
                editor.putString(id + ".pushMode", mPushClass.name());
            }
            editor.putBoolean(id + ".inTopGroup", mInTopGroup);

            editor.putBoolean(id + ".integrate", mIntegrate);

        }

        public void refresh(String name, PreferencesHolder prefHolder) {
            String id = getPrefId(name);

            SharedPreferences preferences = LocalStore.this.getPreferences();

            try {
                prefHolder.displayClass = FolderClass.valueOf(preferences.getString(id + ".displayMode",
                                          prefHolder.displayClass.name()));
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Unable to load displayMode for " + getName(), e);
            }
            if (prefHolder.displayClass == FolderClass.NONE) {
                prefHolder.displayClass = FolderClass.NO_CLASS;
            }

            try {
                prefHolder.syncClass = FolderClass.valueOf(preferences.getString(id  + ".syncMode",
                                       prefHolder.syncClass.name()));
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Unable to load syncMode for " + getName(), e);

            }
            if (prefHolder.syncClass == FolderClass.NONE) {
                prefHolder.syncClass = FolderClass.INHERITED;
            }

            try {
                prefHolder.pushClass = FolderClass.valueOf(preferences.getString(id  + ".pushMode",
                                       prefHolder.pushClass.name()));
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Unable to load pushMode for " + getName(), e);
            }
            if (prefHolder.pushClass == FolderClass.NONE) {
                prefHolder.pushClass = FolderClass.INHERITED;
            }
            prefHolder.inTopGroup = preferences.getBoolean(id + ".inTopGroup", prefHolder.inTopGroup);
            prefHolder.integrate = preferences.getBoolean(id + ".integrate", prefHolder.integrate);

        }

        @Override
        public void fetch(final Message[] messages, final FetchProfile fp, final MessageRetrievalListener listener)
        throws MessagingException {
            try {
                database.execute(false, new DbCallback<Void>() {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException {
                        try {
                            open(OPEN_MODE_RW);
                            if (fp.contains(FetchProfile.Item.BODY)) {
                                for (Message message : messages) {
                                    LocalMessage localMessage = (LocalMessage)message;
                                    Cursor cursor = null;
                                    MimeMultipart mp = new MimeMultipart();
                                    mp.setSubType("mixed");
                                    try {
                                        cursor = db.rawQuery("SELECT html_content, text_content, mime_type FROM messages "
                                                             + "WHERE id = ?",
                                                             new String[] { Long.toString(localMessage.mId) });
                                        cursor.moveToNext();
                                        String htmlContent = cursor.getString(0);
                                        String textContent = cursor.getString(1);
                                        String mimeType = cursor.getString(2);
                                        if (mimeType != null && mimeType.toLowerCase(Locale.US).startsWith("multipart/")) {
                                            // If this is a multipart message, preserve both text
                                            // and html parts, as well as the subtype.
                                            mp.setSubType(mimeType.toLowerCase(Locale.US).replaceFirst("^multipart/", ""));
                                            if (textContent != null) {
                                                LocalTextBody body = new LocalTextBody(textContent, htmlContent);
                                                MimeBodyPart bp = new MimeBodyPart(body, "text/plain");
                                                mp.addBodyPart(bp);
                                            }

                                            if (mAccount.getMessageFormat() != MessageFormat.TEXT) {
                                                if (htmlContent != null) {
                                                    TextBody body = new TextBody(htmlContent);
                                                    MimeBodyPart bp = new MimeBodyPart(body, "text/html");
                                                    mp.addBodyPart(bp);
                                                }

                                                // If we have both text and html content and our MIME type
                                                // isn't multipart/alternative, then corral them into a new
                                                // multipart/alternative part and put that into the parent.
                                                // If it turns out that this is the only part in the parent
                                                // MimeMultipart, it'll get fixed below before we attach to
                                                // the message.
                                                if (textContent != null && htmlContent != null && !mimeType.equalsIgnoreCase("multipart/alternative")) {
                                                    MimeMultipart alternativeParts = mp;
                                                    alternativeParts.setSubType("alternative");
                                                    mp = new MimeMultipart();
                                                    mp.addBodyPart(new MimeBodyPart(alternativeParts));
                                                }
                                            }
                                        } else if (mimeType != null && mimeType.equalsIgnoreCase("text/plain")) {
                                            // If it's text, add only the plain part. The MIME
                                            // container will drop away below.
                                            if (textContent != null) {
                                                LocalTextBody body = new LocalTextBody(textContent, htmlContent);
                                                MimeBodyPart bp = new MimeBodyPart(body, "text/plain");
                                                mp.addBodyPart(bp);
                                            }
                                        } else if (mimeType != null && mimeType.equalsIgnoreCase("text/html")) {
                                            // If it's html, add only the html part. The MIME
                                            // container will drop away below.
                                            if (htmlContent != null) {
                                                TextBody body = new TextBody(htmlContent);
                                                MimeBodyPart bp = new MimeBodyPart(body, "text/html");
                                                mp.addBodyPart(bp);
                                            }
                                        } else {
                                            // MIME type not set. Grab whatever part we can get,
                                            // with Text taking precedence. This preserves pre-HTML
                                            // composition behaviour.
                                            if (textContent != null) {
                                                LocalTextBody body = new LocalTextBody(textContent, htmlContent);
                                                MimeBodyPart bp = new MimeBodyPart(body, "text/plain");
                                                mp.addBodyPart(bp);
                                            } else if (htmlContent != null) {
                                                TextBody body = new TextBody(htmlContent);
                                                MimeBodyPart bp = new MimeBodyPart(body, "text/html");
                                                mp.addBodyPart(bp);
                                            }
                                        }

                                    } catch (Exception e) {
                                        Log.e(K9.LOG_TAG, "Exception fetching message:", e);
                                    } finally {
                                        Utility.closeQuietly(cursor);
                                    }

                                    try {
                                        cursor = db.query(
                                                     "attachments",
                                                     new String[] {
                                                         "id",
                                                         "size",
                                                         "name",
                                                         "mime_type",
                                                         "store_data",
                                                         "content_uri",
                                                         "content_id",
                                                         "content_disposition"
                                                     },
                                                     "message_id = ?",
                                                     new String[] { Long.toString(localMessage.mId) },
                                                     null,
                                                     null,
                                                     null);

                                        while (cursor.moveToNext()) {
                                            long id = cursor.getLong(0);
                                            int size = cursor.getInt(1);
                                            String name = cursor.getString(2);
                                            String type = cursor.getString(3);
                                            String storeData = cursor.getString(4);
                                            String contentUri = cursor.getString(5);
                                            String contentId = cursor.getString(6);
                                            String contentDisposition = cursor.getString(7);
                                            String encoding = MimeUtility.getEncodingforType(type);
                                            Body body = null;

                                            if (contentDisposition == null) {
                                                contentDisposition = "attachment";
                                            }

                                            if (contentUri != null) {
                                                if (MimeUtil.isMessage(type)) {
                                                    body = new LocalAttachmentMessageBody(
                                                            Uri.parse(contentUri),
                                                            mApplication);
                                                } else {
                                                    body = new LocalAttachmentBody(
                                                            Uri.parse(contentUri),
                                                            mApplication);
                                                }
                                            }

                                            MimeBodyPart bp = new LocalAttachmentBodyPart(body, id);
                                            bp.setEncoding(encoding);
                                            if (name != null) {
                                                bp.setHeader(MimeHeader.HEADER_CONTENT_TYPE,
                                                             String.format("%s;\r\n name=\"%s\"",
                                                                           type,
                                                                           name));
                                                bp.setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION,
                                                             String.format("%s;\r\n filename=\"%s\";\r\n size=%d",
                                                                           contentDisposition,
                                                                           name, // TODO: Should use encoded word defined in RFC 2231.
                                                                           size));
                                            } else {
                                                bp.setHeader(MimeHeader.HEADER_CONTENT_TYPE, type);
                                                bp.setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION,
                                                        String.format("%s;\r\n size=%d",
                                                                      contentDisposition,
                                                                      size));
                                            }

                                            bp.setHeader(MimeHeader.HEADER_CONTENT_ID, contentId);
                                            /*
                                             * HEADER_ANDROID_ATTACHMENT_STORE_DATA is a custom header we add to that
                                             * we can later pull the attachment from the remote store if necessary.
                                             */
                                            bp.setHeader(MimeHeader.HEADER_ANDROID_ATTACHMENT_STORE_DATA, storeData);

                                            mp.addBodyPart(bp);
                                        }
                                    } finally {
                                        Utility.closeQuietly(cursor);
                                    }

                                    if (mp.getCount() == 0) {
                                        // If we have no body, remove the container and create a
                                        // dummy plain text body. This check helps prevents us from
                                        // triggering T_MIME_NO_TEXT and T_TVD_MIME_NO_HEADERS
                                        // SpamAssassin rules.
                                        localMessage.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/plain");
                                        localMessage.setBody(new TextBody(""));
                                    } else if (mp.getCount() == 1 && (mp.getBodyPart(0) instanceof LocalAttachmentBodyPart) == false)

                                    {
                                        // If we have only one part, drop the MimeMultipart container.
                                        BodyPart part = mp.getBodyPart(0);
                                        localMessage.setHeader(MimeHeader.HEADER_CONTENT_TYPE, part.getContentType());
                                        localMessage.setBody(part.getBody());
                                    } else {
                                        // Otherwise, attach the MimeMultipart to the message.
                                        localMessage.setBody(mp);
                                    }
                                }
                            }
                        } catch (MessagingException e) {
                            throw new WrappedException(e);
                        }
                        return null;
                    }
                });
            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }
        }

        @Override
        public Message[] getMessages(int start, int end, Date earliestDate, MessageRetrievalListener listener)
        throws MessagingException {
            open(OPEN_MODE_RW);
            throw new MessagingException(
                "LocalStore.getMessages(int, int, MessageRetrievalListener) not yet implemented");
        }

        /**
         * Populate the header fields of the given list of messages by reading
         * the saved header data from the database.
         *
         * @param messages
         *            The messages whose headers should be loaded.
         * @throws UnavailableStorageException
         */
        private void populateHeaders(final List<LocalMessage> messages) throws UnavailableStorageException {
            database.execute(false, new DbCallback<Void>() {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException {
                    Cursor cursor = null;
                    if (messages.isEmpty()) {
                        return null;
                    }
                    try {
                        Map<Long, LocalMessage> popMessages = new HashMap<Long, LocalMessage>();
                        List<String> ids = new ArrayList<String>();
                        StringBuilder questions = new StringBuilder();

                        for (int i = 0; i < messages.size(); i++) {
                            if (i != 0) {
                                questions.append(", ");
                            }
                            questions.append("?");
                            LocalMessage message = messages.get(i);
                            Long id = message.getId();
                            ids.add(Long.toString(id));
                            popMessages.put(id, message);

                        }

                        cursor = db.rawQuery(
                                     "SELECT message_id, name, value FROM headers " + "WHERE message_id in ( " + questions + ") ORDER BY id ASC",
                                     ids.toArray(EMPTY_STRING_ARRAY));


                        while (cursor.moveToNext()) {
                            Long id = cursor.getLong(0);
                            String name = cursor.getString(1);
                            String value = cursor.getString(2);
                            //Log.i(K9.LOG_TAG, "Retrieved header name= " + name + ", value = " + value + " for message " + id);
                            popMessages.get(id).addHeader(name, value);
                        }
                    } finally {
                        Utility.closeQuietly(cursor);
                    }
                    return null;
                }
            });
        }

        public String getMessageUidById(final long id) throws MessagingException {
            try {
                return database.execute(false, new DbCallback<String>() {
                    @Override
                    public String doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException {
                        try {
                            open(OPEN_MODE_RW);
                            Cursor cursor = null;

                            try {
                                cursor = db.rawQuery(
                                             "SELECT uid FROM messages " +
                                              "WHERE id = ? AND folder_id = ?",
                                             new String[] {
                                                 Long.toString(id), Long.toString(mFolderId)
                                             });
                                if (!cursor.moveToNext()) {
                                    return null;
                                }
                                return cursor.getString(0);
                            } finally {
                                Utility.closeQuietly(cursor);
                            }
                        } catch (MessagingException e) {
                            throw new WrappedException(e);
                        }
                    }
                });
            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }
        }

        @Override
        public LocalMessage getMessage(final String uid) throws MessagingException {
            try {
                return database.execute(false, new DbCallback<LocalMessage>() {
                    @Override
                    public LocalMessage doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException {
                        try {
                            open(OPEN_MODE_RW);
                            LocalMessage message = new LocalMessage(uid, LocalFolder.this);
                            Cursor cursor = null;

                            try {
                                cursor = db.rawQuery(
                                             "SELECT " +
                                             GET_MESSAGES_COLS +
                                             "FROM messages " +
                                             "LEFT JOIN threads ON (threads.message_id = messages.id) " +
                                             "WHERE uid = ? AND folder_id = ?",
                                             new String[] {
                                                 message.getUid(), Long.toString(mFolderId)
                                             });
                                if (!cursor.moveToNext()) {
                                    return null;
                                }
                                message.populateFromGetMessageCursor(cursor);
                            } finally {
                                Utility.closeQuietly(cursor);
                            }
                            return message;
                        } catch (MessagingException e) {
                            throw new WrappedException(e);
                        }
                    }
                });
            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }
        }

        @Override
        public Message[] getMessages(MessageRetrievalListener listener) throws MessagingException {
            return getMessages(listener, true);
        }

        @Override
        public Message[] getMessages(final MessageRetrievalListener listener, final boolean includeDeleted) throws MessagingException {
            try {
                return database.execute(false, new DbCallback<Message[]>() {
                    @Override
                    public Message[] doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException {
                        try {
                            open(OPEN_MODE_RW);
                            return LocalStore.this.getMessages(
                                       listener,
                                       LocalFolder.this,
                                       "SELECT " + GET_MESSAGES_COLS +
                                       "FROM messages " +
                                       "LEFT JOIN threads ON (threads.message_id = messages.id) " +
                                       "WHERE (empty IS NULL OR empty != 1) AND " +
                                       (includeDeleted ? "" : "deleted = 0 AND ") +
                                       "folder_id = ? ORDER BY date DESC",
                                       new String[] { Long.toString(mFolderId) }
                                   );
                        } catch (MessagingException e) {
                            throw new WrappedException(e);
                        }
                    }
                });
            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }
        }

        @Override
        public Message[] getMessages(String[] uids, MessageRetrievalListener listener)
        throws MessagingException {
            open(OPEN_MODE_RW);
            if (uids == null) {
                return getMessages(listener);
            }
            ArrayList<Message> messages = new ArrayList<Message>();
            for (String uid : uids) {
                Message message = getMessage(uid);
                if (message != null) {
                    messages.add(message);
                }
            }
            return messages.toArray(EMPTY_MESSAGE_ARRAY);
        }

        @Override
        public Map<String, String> copyMessages(Message[] msgs, Folder folder) throws MessagingException {
            if (!(folder instanceof LocalFolder)) {
                throw new MessagingException("copyMessages called with incorrect Folder");
            }
            return ((LocalFolder) folder).appendMessages(msgs, true);
        }

        @Override
        public Map<String, String> moveMessages(final Message[] msgs, final Folder destFolder) throws MessagingException {
            if (!(destFolder instanceof LocalFolder)) {
                throw new MessagingException("moveMessages called with non-LocalFolder");
            }

            final LocalFolder lDestFolder = (LocalFolder)destFolder;

            final Map<String, String> uidMap = new HashMap<String, String>();

            try {
                database.execute(false, new DbCallback<Void>() {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException {
                        try {
                            lDestFolder.open(OPEN_MODE_RW);
                            for (Message message : msgs) {
                                LocalMessage lMessage = (LocalMessage)message;

                                String oldUID = message.getUid();

                                if (K9.DEBUG) {
                                    Log.d(K9.LOG_TAG, "Updating folder_id to " + lDestFolder.getId() + " for message with UID "
                                          + message.getUid() + ", id " + lMessage.getId() + " currently in folder " + getName());
                                }

                                String newUid = K9.LOCAL_UID_PREFIX + UUID.randomUUID().toString();
                                message.setUid(newUid);

                                uidMap.put(oldUID, newUid);

                                // Message threading in the target folder
                                ThreadInfo threadInfo = lDestFolder.doMessageThreading(db, message);

                                /*
                                 * "Move" the message into the new folder
                                 */
                                long msgId = lMessage.getId();
                                String[] idArg = new String[] { Long.toString(msgId) };

                                ContentValues cv = new ContentValues();
                                cv.put("folder_id", lDestFolder.getId());
                                cv.put("uid", newUid);

                                db.update("messages", cv, "id = ?", idArg);

                                // Create/update entry in 'threads' table for the message in the
                                // target folder
                                cv.clear();
                                cv.put("message_id", msgId);
                                if (threadInfo.threadId == -1) {
                                    if (threadInfo.rootId != -1) {
                                        cv.put("root", threadInfo.rootId);
                                    }

                                    if (threadInfo.parentId != -1) {
                                        cv.put("parent", threadInfo.parentId);
                                    }

                                    db.insert("threads", null, cv);
                                } else {
                                    db.update("threads", cv, "id = ?",
                                            new String[] { Long.toString(threadInfo.threadId) });
                                }

                                /*
                                 * Add a placeholder message so we won't download the original
                                 * message again if we synchronize before the remote move is
                                 * complete.
                                 */

                                // We need to open this folder to get the folder id
                                open(OPEN_MODE_RW);

                                cv.clear();
                                cv.put("uid", oldUID);
                                cv.putNull("flags");
                                cv.put("read", 1);
                                cv.put("deleted", 1);
                                cv.put("folder_id", mFolderId);
                                cv.put("empty", 0);

                                String messageId = message.getMessageId();
                                if (messageId != null) {
                                    cv.put("message_id", messageId);
                                }

                                final long newId;
                                if (threadInfo.msgId != -1) {
                                    // There already existed an empty message in the target folder.
                                    // Let's use it as placeholder.

                                    newId = threadInfo.msgId;

                                    db.update("messages", cv, "id = ?",
                                            new String[] { Long.toString(newId) });
                                } else {
                                    newId = db.insert("messages", null, cv);
                                }

                                /*
                                 * Update old entry in 'threads' table to point to the newly
                                 * created placeholder.
                                 */

                                cv.clear();
                                cv.put("message_id", newId);
                                db.update("threads", cv, "id = ?",
                                        new String[] { Long.toString(lMessage.getThreadId()) });
                            }
                        } catch (MessagingException e) {
                            throw new WrappedException(e);
                        }
                        return null;
                    }
                });

                notifyChange();

                return uidMap;
            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }

        }

        /**
         * Convenience transaction wrapper for storing a message and set it as fully downloaded. Implemented mainly to speed up DB transaction commit.
         *
         * @param message Message to store. Never <code>null</code>.
         * @param runnable What to do before setting {@link Flag#X_DOWNLOADED_FULL}. Never <code>null</code>.
         * @return The local version of the message. Never <code>null</code>.
         * @throws MessagingException
         */
        public Message storeSmallMessage(final Message message, final Runnable runnable) throws MessagingException {
            return database.execute(true, new DbCallback<Message>() {
                @Override
                public Message doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException {
                    try {
                        appendMessages(new Message[] { message });
                        final String uid = message.getUid();
                        final Message result = getMessage(uid);
                        runnable.run();
                        // Set a flag indicating this message has now be fully downloaded
                        result.setFlag(Flag.X_DOWNLOADED_FULL, true);
                        return result;
                    } catch (MessagingException e) {
                        throw new WrappedException(e);
                    }
                }
            });
        }

        /**
         * The method differs slightly from the contract; If an incoming message already has a uid
         * assigned and it matches the uid of an existing message then this message will replace the
         * old message. It is implemented as a delete/insert. This functionality is used in saving
         * of drafts and re-synchronization of updated server messages.
         *
         * NOTE that although this method is located in the LocalStore class, it is not guaranteed
         * that the messages supplied as parameters are actually {@link LocalMessage} instances (in
         * fact, in most cases, they are not). Therefore, if you want to make local changes only to a
         * message, retrieve the appropriate local message instance first (if it already exists).
         */
        @Override
        public Map<String, String> appendMessages(Message[] messages) throws MessagingException {
            return appendMessages(messages, false);
        }

        public void destroyMessages(final Message[] messages) {
            try {
                database.execute(true, new DbCallback<Void>() {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException {
                        for (Message message : messages) {
                            try {
                                message.destroy();
                            } catch (MessagingException e) {
                                throw new WrappedException(e);
                            }
                        }
                        return null;
                    }
                });
            } catch (MessagingException e) {
                throw new WrappedException(e);
            }
        }

        private ThreadInfo getThreadInfo(SQLiteDatabase db, String messageId, boolean onlyEmpty) {
            String sql = "SELECT t.id, t.message_id, t.root, t.parent " +
                    "FROM messages m " +
                    "LEFT JOIN threads t ON (t.message_id = m.id) " +
                    "WHERE m.folder_id = ? AND m.message_id = ? " +
                    ((onlyEmpty) ? "AND m.empty = 1 " : "") +
                    "ORDER BY m.id LIMIT 1";
            String[] selectionArgs = { Long.toString(mFolderId), messageId };
            Cursor cursor = db.rawQuery(sql, selectionArgs);

            if (cursor != null) {
                try {
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        long threadId = cursor.getLong(0);
                        long msgId = cursor.getLong(1);
                        long rootId = (cursor.isNull(2)) ? -1 : cursor.getLong(2);
                        long parentId = (cursor.isNull(3)) ? -1 : cursor.getLong(3);

                        return new ThreadInfo(threadId, msgId, messageId, rootId, parentId);
                    }
                } finally {
                    cursor.close();
                }
            }

            return null;
        }

        /**
         * The method differs slightly from the contract; If an incoming message already has a uid
         * assigned and it matches the uid of an existing message then this message will replace
         * the old message. This functionality is used in saving of drafts and re-synchronization
         * of updated server messages.
         *
         * NOTE that although this method is located in the LocalStore class, it is not guaranteed
         * that the messages supplied as parameters are actually {@link LocalMessage} instances (in
         * fact, in most cases, they are not). Therefore, if you want to make local changes only to a
         * message, retrieve the appropriate local message instance first (if it already exists).
         * @param messages
         * @param copy
         * @return Map<String, String> uidMap of srcUids -> destUids
         */
        private Map<String, String> appendMessages(final Message[] messages, final boolean copy) throws MessagingException {
            open(OPEN_MODE_RW);
            try {
                final Map<String, String> uidMap = new HashMap<String, String>();
                database.execute(true, new DbCallback<Void>() {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException {
                        try {
                            for (Message message : messages) {
                                if (!(message instanceof MimeMessage)) {
                                    throw new Error("LocalStore can only store Messages that extend MimeMessage");
                                }

                                long oldMessageId = -1;
                                String uid = message.getUid();
                                if (uid == null || copy) {
                                    /*
                                     * Create a new message in the database
                                     */
                                    String randomLocalUid = K9.LOCAL_UID_PREFIX +
                                            UUID.randomUUID().toString();

                                    if (copy) {
                                        // Save mapping: source UID -> target UID
                                        uidMap.put(uid, randomLocalUid);
                                    } else {
                                        // Modify the Message instance to reference the new UID
                                        message.setUid(randomLocalUid);
                                    }

                                    // The message will be saved with the newly generated UID
                                    uid = randomLocalUid;
                                } else {
                                    /*
                                     * Replace an existing message in the database
                                     */
                                    LocalMessage oldMessage = getMessage(uid);

                                    if (oldMessage != null) {
                                        oldMessageId = oldMessage.getId();
                                    }

                                    deleteAttachments(message.getUid());
                                }

                                long rootId = -1;
                                long parentId = -1;

                                if (oldMessageId == -1) {
                                    // This is a new message. Do the message threading.
                                    ThreadInfo threadInfo = doMessageThreading(db, message);
                                    oldMessageId = threadInfo.msgId;
                                    rootId = threadInfo.rootId;
                                    parentId = threadInfo.parentId;
                                }

                                boolean isDraft = (message.getHeader(K9.IDENTITY_HEADER) != null);

                                List<Part> attachments;
                                String text;
                                String html;
                                if (isDraft) {
                                    // Don't modify the text/plain or text/html part of our own
                                    // draft messages because this will cause the values stored in
                                    // the identity header to be wrong.
                                    ViewableContainer container =
                                            MimeUtility.extractPartsFromDraft(message);

                                    text = container.text;
                                    html = container.html;
                                    attachments = container.attachments;
                                } else {
                                    ViewableContainer container =
                                            MimeUtility.extractTextAndAttachments(mApplication, message);

                                    attachments = container.attachments;
                                    text = container.text;
                                    html = HtmlConverter.convertEmoji2Img(container.html);
                                }

                                String preview = Message.calculateContentPreview(text);

                                try {
                                    ContentValues cv = new ContentValues();
                                    cv.put("uid", uid);
                                    cv.put("subject", message.getSubject());
                                    cv.put("sender_list", Address.pack(message.getFrom()));
                                    cv.put("date", message.getSentDate() == null
                                           ? System.currentTimeMillis() : message.getSentDate().getTime());
                                    cv.put("flags", serializeFlags(message.getFlags()));
                                    cv.put("deleted", message.isSet(Flag.DELETED) ? 1 : 0);
                                    cv.put("read", message.isSet(Flag.SEEN) ? 1 : 0);
                                    cv.put("flagged", message.isSet(Flag.FLAGGED) ? 1 : 0);
                                    cv.put("answered", message.isSet(Flag.ANSWERED) ? 1 : 0);
                                    cv.put("forwarded", message.isSet(Flag.FORWARDED) ? 1 : 0);
                                    cv.put("folder_id", mFolderId);
                                    cv.put("to_list", Address.pack(message.getRecipients(RecipientType.TO)));
                                    cv.put("cc_list", Address.pack(message.getRecipients(RecipientType.CC)));
                                    cv.put("bcc_list", Address.pack(message.getRecipients(RecipientType.BCC)));
                                    cv.put("html_content", html.length() > 0 ? html : null);
                                    cv.put("text_content", text.length() > 0 ? text : null);
                                    cv.put("preview", preview.length() > 0 ? preview : null);
                                    cv.put("reply_to_list", Address.pack(message.getReplyTo()));
                                    cv.put("attachment_count", attachments.size());
                                    cv.put("internal_date",  message.getInternalDate() == null
                                           ? System.currentTimeMillis() : message.getInternalDate().getTime());
                                    cv.put("mime_type", message.getMimeType());
                                    cv.put("empty", 0);

                                    String messageId = message.getMessageId();
                                    if (messageId != null) {
                                        cv.put("message_id", messageId);
                                    }

                                    long msgId;

                                    if (oldMessageId == -1) {
                                        msgId = db.insert("messages", "uid", cv);

                                        // Create entry in 'threads' table
                                        cv.clear();
                                        cv.put("message_id", msgId);

                                        if (rootId != -1) {
                                            cv.put("root", rootId);
                                        }
                                        if (parentId != -1) {
                                            cv.put("parent", parentId);
                                        }

                                        db.insert("threads", null, cv);
                                    } else {
                                        db.update("messages", cv, "id = ?", new String[] { Long.toString(oldMessageId) });
                                        msgId = oldMessageId;
                                    }

                                    for (Part attachment : attachments) {
                                        saveAttachment(msgId, attachment, copy);
                                    }
                                    saveHeaders(msgId, (MimeMessage)message);
                                } catch (Exception e) {
                                    throw new MessagingException("Error appending message", e);
                                }
                            }
                        } catch (MessagingException e) {
                            throw new WrappedException(e);
                        }
                        return null;
                    }
                });

                notifyChange();

                return uidMap;
            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }
        }

        /**
         * Update the given message in the LocalStore without first deleting the existing
         * message (contrast with appendMessages). This method is used to store changes
         * to the given message while updating attachments and not removing existing
         * attachment data.
         * TODO In the future this method should be combined with appendMessages since the Message
         * contains enough data to decide what to do.
         * @param message
         * @throws MessagingException
         */
        public void updateMessage(final LocalMessage message) throws MessagingException {
            open(OPEN_MODE_RW);
            try {
                database.execute(false, new DbCallback<Void>() {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException {
                        try {
                            message.buildMimeRepresentation();

                            ViewableContainer container =
                                    MimeUtility.extractTextAndAttachments(mApplication, message);

                            List<Part> attachments = container.attachments;
                            String text = container.text;
                            String html = HtmlConverter.convertEmoji2Img(container.html);

                            String preview = Message.calculateContentPreview(text);

                            try {
                                db.execSQL("UPDATE messages SET "
                                           + "uid = ?, subject = ?, sender_list = ?, date = ?, flags = ?, "
                                           + "folder_id = ?, to_list = ?, cc_list = ?, bcc_list = ?, "
                                           + "html_content = ?, text_content = ?, preview = ?, reply_to_list = ?, "
                                           + "attachment_count = ?, read = ?, flagged = ?, answered = ?, forwarded = ? "
                                           + "WHERE id = ?",
                                           new Object[] {
                                               message.getUid(),
                                               message.getSubject(),
                                               Address.pack(message.getFrom()),
                                               message.getSentDate() == null ? System
                                               .currentTimeMillis() : message.getSentDate()
                                               .getTime(),
                                               serializeFlags(message.getFlags()),
                                               mFolderId,
                                               Address.pack(message
                                                            .getRecipients(RecipientType.TO)),
                                               Address.pack(message
                                                            .getRecipients(RecipientType.CC)),
                                               Address.pack(message
                                                            .getRecipients(RecipientType.BCC)),
                                               html.length() > 0 ? html : null,
                                               text.length() > 0 ? text : null,
                                               preview.length() > 0 ? preview : null,
                                               Address.pack(message.getReplyTo()),
                                               attachments.size(),
                                               message.isSet(Flag.SEEN) ? 1 : 0,
                                               message.isSet(Flag.FLAGGED) ? 1 : 0,
                                               message.isSet(Flag.ANSWERED) ? 1 : 0,
                                               message.isSet(Flag.FORWARDED) ? 1 : 0,
                                               message.mId
                                           });

                                for (int i = 0, count = attachments.size(); i < count; i++) {
                                    Part attachment = attachments.get(i);
                                    saveAttachment(message.mId, attachment, false);
                                }
                                saveHeaders(message.getId(), message);
                            } catch (Exception e) {
                                throw new MessagingException("Error appending message", e);
                            }
                        } catch (MessagingException e) {
                            throw new WrappedException(e);
                        }
                        return null;
                    }
                });
            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }

            notifyChange();
        }

        /**
         * Save the headers of the given message. Note that the message is not
         * necessarily a {@link LocalMessage} instance.
         * @param id
         * @param message
         * @throws com.fsck.k9.mail.MessagingException
         */
        private void saveHeaders(final long id, final MimeMessage message) throws MessagingException {
            database.execute(true, new DbCallback<Void>() {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException {

                    deleteHeaders(id);
                    for (String name : message.getHeaderNames()) {
                            String[] values = message.getHeader(name);
                            for (String value : values) {
                                ContentValues cv = new ContentValues();
                                cv.put("message_id", id);
                                cv.put("name", name);
                                cv.put("value", value);
                                db.insert("headers", "name", cv);
                            }
                    }

                    // Remember that all headers for this message have been saved, so it is
                    // not necessary to download them again in case the user wants to see all headers.
                    List<Flag> appendedFlags = new ArrayList<Flag>();
                    appendedFlags.addAll(Arrays.asList(message.getFlags()));
                    appendedFlags.add(Flag.X_GOT_ALL_HEADERS);

                    db.execSQL("UPDATE messages " + "SET flags = ? " + " WHERE id = ?",
                               new Object[]
                               { serializeFlags(appendedFlags.toArray(EMPTY_FLAG_ARRAY)), id });

                    return null;
                }
            });
        }

        private void deleteHeaders(final long id) throws UnavailableStorageException {
            database.execute(false, new DbCallback<Void>() {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException {
                    db.execSQL("DELETE FROM headers WHERE message_id = ?", new Object[]
                               { id });
                    return null;
                }
            });
        }

        /**
         * @param messageId
         * @param attachment
         * @param saveAsNew
         * @throws IOException
         * @throws MessagingException
         */
        private void saveAttachment(final long messageId, final Part attachment, final boolean saveAsNew)
        throws IOException, MessagingException {
            try {
                database.execute(true, new DbCallback<Void>() {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException {
                        try {
                            long attachmentId = -1;
                            Uri contentUri = null;
                            int size = -1;
                            File tempAttachmentFile = null;

                            if ((!saveAsNew) && (attachment instanceof LocalAttachmentBodyPart)) {
                                attachmentId = ((LocalAttachmentBodyPart) attachment).getAttachmentId();
                            }

                            final File attachmentDirectory = StorageManager.getInstance(mApplication).getAttachmentDirectory(uUid, database.getStorageProviderId());
                            if (attachment.getBody() != null) {
                                Body body = attachment.getBody();
                                if (body instanceof LocalAttachmentBody) {
                                    contentUri = ((LocalAttachmentBody) body).getContentUri();
                                } else if (body instanceof Message) {
                                    // It's a message, so use Message.writeTo() to output the
                                    // message including all children.
                                    Message message = (Message) body;
                                    tempAttachmentFile = File.createTempFile("att", null, attachmentDirectory);
                                    FileOutputStream out = new FileOutputStream(tempAttachmentFile);
                                    try {
                                        message.writeTo(out);
                                    } finally {
                                        out.close();
                                    }
                                    size = (int) (tempAttachmentFile.length() & 0x7FFFFFFFL);
                                } else {
                                    /*
                                     * If the attachment has a body we're expected to save it into the local store
                                     * so we copy the data into a cached attachment file.
                                     */
                                    InputStream in = attachment.getBody().getInputStream();
                                    try {
                                        tempAttachmentFile = File.createTempFile("att", null, attachmentDirectory);
                                        FileOutputStream out = new FileOutputStream(tempAttachmentFile);
                                        try {
                                            size = IOUtils.copy(in, out);
                                        } finally {
                                            out.close();
                                        }
                                    } finally {
                                        try { in.close(); } catch (Throwable ignore) {}
                                    }
                                }
                            }

                            if (size == -1) {
                                /*
                                 * If the attachment is not yet downloaded see if we can pull a size
                                 * off the Content-Disposition.
                                 */
                                String disposition = attachment.getDisposition();
                                if (disposition != null) {
                                    String s = MimeUtility.getHeaderParameter(disposition, "size");
                                    if (s != null) {
                                        try {
                                            size = Integer.parseInt(s);
                                        } catch (NumberFormatException e) { /* Ignore */ }
                                    }
                                }
                            }
                            if (size == -1) {
                                size = 0;
                            }

                            String storeData =
                                Utility.combine(attachment.getHeader(
                                                    MimeHeader.HEADER_ANDROID_ATTACHMENT_STORE_DATA), ',');

                            String name = MimeUtility.getHeaderParameter(attachment.getContentType(), "name");
                            String contentId = MimeUtility.getHeaderParameter(attachment.getContentId(), null);

                            String contentDisposition = MimeUtility.unfoldAndDecode(attachment.getDisposition());
                            String dispositionType = contentDisposition;

                            if (dispositionType != null) {
                                int pos = dispositionType.indexOf(';');
                                if (pos != -1) {
                                    // extract the disposition-type, "attachment", "inline" or extension-token (see the RFC 2183)
                                    dispositionType = dispositionType.substring(0, pos);
                                }
                            }

                            if (name == null && contentDisposition != null) {
                                name = MimeUtility.getHeaderParameter(contentDisposition, "filename");
                            }
                            if (attachmentId == -1) {
                                ContentValues cv = new ContentValues();
                                cv.put("message_id", messageId);
                                cv.put("content_uri", contentUri != null ? contentUri.toString() : null);
                                cv.put("store_data", storeData);
                                cv.put("size", size);
                                cv.put("name", name);
                                cv.put("mime_type", attachment.getMimeType());
                                cv.put("content_id", contentId);
                                cv.put("content_disposition", dispositionType);

                                attachmentId = db.insert("attachments", "message_id", cv);
                            } else {
                                ContentValues cv = new ContentValues();
                                cv.put("content_uri", contentUri != null ? contentUri.toString() : null);
                                cv.put("size", size);
                                db.update("attachments", cv, "id = ?", new String[]
                                          { Long.toString(attachmentId) });
                            }

                            if (attachmentId != -1 && tempAttachmentFile != null) {
                                File attachmentFile = new File(attachmentDirectory, Long.toString(attachmentId));
                                tempAttachmentFile.renameTo(attachmentFile);
                                contentUri = AttachmentProvider.getAttachmentUri(
                                                 mAccount,
                                                 attachmentId);
                                if (MimeUtil.isMessage(attachment.getMimeType())) {
                                    attachment.setBody(new LocalAttachmentMessageBody(
                                            contentUri, mApplication));
                                } else {
                                    attachment.setBody(new LocalAttachmentBody(
                                            contentUri, mApplication));
                                }
                                ContentValues cv = new ContentValues();
                                cv.put("content_uri", contentUri != null ? contentUri.toString() : null);
                                db.update("attachments", cv, "id = ?", new String[]
                                          { Long.toString(attachmentId) });
                            }

                            /* The message has attachment with Content-ID */
                            if (contentId != null && contentUri != null) {
                                Cursor cursor = db.query("messages", new String[]
                                                         { "html_content" }, "id = ?", new String[]
                                                         { Long.toString(messageId) }, null, null, null);
                                try {
                                    if (cursor.moveToNext()) {
                                        String htmlContent = cursor.getString(0);

                                        if (htmlContent != null) {
                                            String newHtmlContent = htmlContent.replaceAll(
                                                                        Pattern.quote("cid:" + contentId),
                                                                        contentUri.toString());

                                            ContentValues cv = new ContentValues();
                                            cv.put("html_content", newHtmlContent);
                                            db.update("messages", cv, "id = ?", new String[]
                                                      { Long.toString(messageId) });
                                        }
                                    }
                                } finally {
                                    Utility.closeQuietly(cursor);
                                }
                            }

                            if (attachmentId != -1 && attachment instanceof LocalAttachmentBodyPart) {
                                ((LocalAttachmentBodyPart) attachment).setAttachmentId(attachmentId);
                            }
                            return null;
                        } catch (MessagingException e) {
                            throw new WrappedException(e);
                        } catch (IOException e) {
                            throw new WrappedException(e);
                        }
                    }
                });
            } catch (WrappedException e) {
                final Throwable cause = e.getCause();
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                }

                throw (MessagingException) cause;
            }
        }

        /**
         * Changes the stored uid of the given message (using it's internal id as a key) to
         * the uid in the message.
         * @param message
         * @throws com.fsck.k9.mail.MessagingException
         */
        public void changeUid(final LocalMessage message) throws MessagingException {
            open(OPEN_MODE_RW);
            final ContentValues cv = new ContentValues();
            cv.put("uid", message.getUid());
            database.execute(false, new DbCallback<Void>() {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException {
                    db.update("messages", cv, "id = ?", new String[]
                              { Long.toString(message.mId) });
                    return null;
                }
            });

            //TODO: remove this once the UI code exclusively uses the database id
            notifyChange();
        }

        @Override
        public void setFlags(final Message[] messages, final Flag[] flags, final boolean value)
        throws MessagingException {
            open(OPEN_MODE_RW);

            // Use one transaction to set all flags
            try {
                database.execute(true, new DbCallback<Void>() {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException,
                            UnavailableStorageException {

                        for (Message message : messages) {
                            try {
                                message.setFlags(flags, value);
                            } catch (MessagingException e) {
                                Log.e(K9.LOG_TAG, "Something went wrong while setting flag", e);
                            }
                        }

                        return null;
                    }
                });
            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }
        }

        @Override
        public void setFlags(Flag[] flags, boolean value)
        throws MessagingException {
            open(OPEN_MODE_RW);
            for (Message message : getMessages(null)) {
                message.setFlags(flags, value);
            }
        }

        @Override
        public String getUidFromMessageId(Message message) throws MessagingException {
            throw new MessagingException("Cannot call getUidFromMessageId on LocalFolder");
        }

        public void clearMessagesOlderThan(long cutoff) throws MessagingException {
            open(OPEN_MODE_RO);

            Message[] messages  = LocalStore.this.getMessages(
                                      null,
                                      this,
                                      "SELECT " + GET_MESSAGES_COLS +
                                      "FROM messages " +
                                      "LEFT JOIN threads ON (threads.message_id = messages.id) " +
                                      "WHERE (empty IS NULL OR empty != 1) AND " +
                                      "(folder_id = ? and date < ?)",
                                      new String[] {
                                              Long.toString(mFolderId), Long.toString(cutoff)
                                      });

            for (Message message : messages) {
                message.destroy();
            }

            notifyChange();
        }

        public void clearAllMessages() throws MessagingException {
            final String[] folderIdArg = new String[] { Long.toString(mFolderId) };

            open(OPEN_MODE_RO);

            try {
                database.execute(false, new DbCallback<Void>() {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException {
                        try {
                            // Get UIDs for all messages to delete
                            Cursor cursor = db.query("messages", new String[] { "uid" },
                                    "folder_id = ? AND (empty IS NULL OR empty != 1)",
                                    folderIdArg, null, null, null);

                            try {
                                // Delete attachments of these messages
                                while (cursor.moveToNext()) {
                                    deleteAttachments(cursor.getString(0));
                                }
                            } finally {
                                cursor.close();
                            }

                            // Delete entries in 'threads' and 'messages'
                            db.execSQL("DELETE FROM threads WHERE message_id IN " +
                                    "(SELECT id FROM messages WHERE folder_id = ?)", folderIdArg);
                            db.execSQL("DELETE FROM messages WHERE folder_id = ?", folderIdArg);

                            return null;
                        } catch (MessagingException e) {
                            throw new WrappedException(e);
                        }
                    }
                });
            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }

            notifyChange();

            setPushState(null);
            setLastPush(0);
            setLastChecked(0);
            setVisibleLimit(mAccount.getDisplayCount());
        }

        @Override
        public void delete(final boolean recurse) throws MessagingException {
            try {
                database.execute(false, new DbCallback<Void>() {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException {
                        try {
                            // We need to open the folder first to make sure we've got it's id
                            open(OPEN_MODE_RO);
                            Message[] messages = getMessages(null);
                            for (Message message : messages) {
                                deleteAttachments(message.getUid());
                            }
                        } catch (MessagingException e) {
                            throw new WrappedException(e);
                        }
                        db.execSQL("DELETE FROM folders WHERE id = ?", new Object[]
                                   { Long.toString(mFolderId), });
                        return null;
                    }
                });
            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof LocalFolder) {
                return ((LocalFolder)o).mName.equals(mName);
            }
            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return mName.hashCode();
        }

        private void deleteAttachments(final long messageId) throws MessagingException {
            open(OPEN_MODE_RW);
            database.execute(false, new DbCallback<Void>() {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException {
                    Cursor attachmentsCursor = null;
                    try {
                        String accountUuid = mAccount.getUuid();
                        Context context = mApplication;

                        // Get attachment IDs
                        String[] whereArgs = new String[] { Long.toString(messageId) };
                        attachmentsCursor = db.query("attachments", new String[] { "id" },
                                "message_id = ?", whereArgs, null, null, null);

                        final File attachmentDirectory = StorageManager.getInstance(mApplication)
                                .getAttachmentDirectory(uUid, database.getStorageProviderId());

                        while (attachmentsCursor.moveToNext()) {
                            String attachmentId = Long.toString(attachmentsCursor.getLong(0));
                            try {
                                // Delete stored attachment
                                File file = new File(attachmentDirectory, attachmentId);
                                if (file.exists()) {
                                    file.delete();
                                }

                                // Delete thumbnail file
                                AttachmentProvider.deleteThumbnail(context, accountUuid,
                                        attachmentId);
                            } catch (Exception e) { /* ignore */ }
                        }

                        // Delete attachment metadata from the database
                        db.delete("attachments", "message_id = ?", whereArgs);
                    } finally {
                        Utility.closeQuietly(attachmentsCursor);
                    }
                    return null;
                }
            });
        }

        private void deleteAttachments(final String uid) throws MessagingException {
            open(OPEN_MODE_RW);
            try {
                database.execute(false, new DbCallback<Void>() {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException {
                        Cursor messagesCursor = null;
                        try {
                            messagesCursor = db.query("messages", new String[]
                                                      { "id" }, "folder_id = ? AND uid = ?", new String[]
                                                      { Long.toString(mFolderId), uid }, null, null, null);
                            while (messagesCursor.moveToNext()) {
                                long messageId = messagesCursor.getLong(0);
                                deleteAttachments(messageId);

                            }
                        } catch (MessagingException e) {
                            throw new WrappedException(e);
                        } finally {
                            Utility.closeQuietly(messagesCursor);
                        }
                        return null;
                    }
                });
            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }
        }

        @Override
        public boolean isInTopGroup() {
            return mInTopGroup;
        }

        public void setInTopGroup(boolean inTopGroup) throws MessagingException {
            mInTopGroup = inTopGroup;
            updateFolderColumn("top_group", mInTopGroup ? 1 : 0);
        }

        public Integer getLastUid() {
            return mLastUid;
        }

        /**
         * <p>Fetches the most recent <b>numeric</b> UID value in this folder.  This is used by
         * {@link com.fsck.k9.controller.MessagingController#shouldNotifyForMessage} to see if messages being
         * fetched are new and unread.  Messages are "new" if they have a UID higher than the most recent UID prior
         * to synchronization.</p>
         *
         * <p>This only works for protocols with numeric UIDs (like IMAP). For protocols with
         * alphanumeric UIDs (like POP), this method quietly fails and shouldNotifyForMessage() will
         * always notify for unread messages.</p>
         *
         * <p>Once Issue 1072 has been fixed, this method and shouldNotifyForMessage() should be
         * updated to use internal dates rather than UIDs to determine new-ness. While this doesn't
         * solve things for POP (which doesn't have internal dates), we can likely use this as a
         * framework to examine send date in lieu of internal date.</p>
         * @throws MessagingException
         */
        public void updateLastUid() throws MessagingException {
            Integer lastUid = database.execute(false, new DbCallback<Integer>() {
                @Override
                public Integer doDbWork(final SQLiteDatabase db) {
                    Cursor cursor = null;
                    try {
                        open(OPEN_MODE_RO);
                        cursor = db.rawQuery("SELECT MAX(uid) FROM messages WHERE folder_id=?", new String[] { Long.toString(mFolderId) });
                        if (cursor.getCount() > 0) {
                            cursor.moveToFirst();
                            return cursor.getInt(0);
                        }
                    } catch (Exception e) {
                        Log.e(K9.LOG_TAG, "Unable to updateLastUid: ", e);
                    } finally {
                        Utility.closeQuietly(cursor);
                    }
                    return null;
                }
            });
            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "Updated last UID for folder " + mName + " to " + lastUid);
            mLastUid = lastUid;
        }

        public Long getOldestMessageDate() throws MessagingException {
            return database.execute(false, new DbCallback<Long>() {
                @Override
                public Long doDbWork(final SQLiteDatabase db) {
                    Cursor cursor = null;
                    try {
                        open(OPEN_MODE_RO);
                        cursor = db.rawQuery("SELECT MIN(date) FROM messages WHERE folder_id=?", new String[] { Long.toString(mFolderId) });
                        if (cursor.getCount() > 0) {
                            cursor.moveToFirst();
                            return cursor.getLong(0);
                        }
                    } catch (Exception e) {
                        Log.e(K9.LOG_TAG, "Unable to fetch oldest message date: ", e);
                    } finally {
                        Utility.closeQuietly(cursor);
                    }
                    return null;
                }
            });
        }

        private ThreadInfo doMessageThreading(SQLiteDatabase db, Message message)
                throws MessagingException {
            long rootId = -1;
            long parentId = -1;

            String messageId = message.getMessageId();

            // If there's already an empty message in the database, update that
            ThreadInfo msgThreadInfo = getThreadInfo(db, messageId, true);

            // Get the message IDs from the "References" header line
            String[] referencesArray = message.getHeader("References");
            List<String> messageIds = null;
            if (referencesArray != null && referencesArray.length > 0) {
                messageIds = Utility.extractMessageIds(referencesArray[0]);
            }

            // Append the first message ID from the "In-Reply-To" header line
            String[] inReplyToArray = message.getHeader("In-Reply-To");
            String inReplyTo = null;
            if (inReplyToArray != null && inReplyToArray.length > 0) {
                inReplyTo = Utility.extractMessageId(inReplyToArray[0]);
                if (inReplyTo != null) {
                    if (messageIds == null) {
                        messageIds = new ArrayList<String>(1);
                        messageIds.add(inReplyTo);
                    } else if (!messageIds.contains(inReplyTo)) {
                        messageIds.add(inReplyTo);
                    }
                }
            }

            if (messageIds == null) {
                // This is not a reply, nothing to do for us.
                return (msgThreadInfo != null) ?
                        msgThreadInfo : new ThreadInfo(-1, -1, messageId, -1, -1);
            }

            for (String reference : messageIds) {
                ThreadInfo threadInfo = getThreadInfo(db, reference, false);

                if (threadInfo == null) {
                    // Create placeholder message in 'messages' table
                    ContentValues cv = new ContentValues();
                    cv.put("message_id", reference);
                    cv.put("folder_id", mFolderId);
                    cv.put("empty", 1);

                    long newMsgId = db.insert("messages", null, cv);

                    // Create entry in 'threads' table
                    cv.clear();
                    cv.put("message_id", newMsgId);
                    if (rootId != -1) {
                        cv.put("root", rootId);
                    }
                    if (parentId != -1) {
                        cv.put("parent", parentId);
                    }

                    parentId = db.insert("threads", null, cv);
                    if (rootId == -1) {
                        rootId = parentId;
                    }
                } else {
                    if (rootId != -1 && threadInfo.rootId == -1 && rootId != threadInfo.threadId) {
                        // We found an existing root container that is not
                        // the root of our current path (References).
                        // Connect it to the current parent.

                        // Let all children know who's the new root
                        ContentValues cv = new ContentValues();
                        cv.put("root", rootId);
                        db.update("threads", cv, "root = ?",
                                new String[] { Long.toString(threadInfo.threadId) });

                        // Connect the message to the current parent
                        cv.put("parent", parentId);
                        db.update("threads", cv, "id = ?",
                                new String[] { Long.toString(threadInfo.threadId) });
                    } else {
                        rootId = (threadInfo.rootId == -1) ?
                                threadInfo.threadId : threadInfo.rootId;
                    }
                    parentId = threadInfo.threadId;
                }
            }

            //TODO: set in-reply-to "link" even if one already exists

            long threadId;
            long msgId;
            if (msgThreadInfo != null) {
                threadId = msgThreadInfo.threadId;
                msgId = msgThreadInfo.msgId;
            } else {
                threadId = -1;
                msgId = -1;
            }

            return new ThreadInfo(threadId, msgId, messageId, rootId, parentId);
        }

        public List<Message> extractNewMessages(final List<Message> messages)
                throws MessagingException {

            try {
                return database.execute(false, new DbCallback<List<Message>>() {
                    @Override
                    public List<Message> doDbWork(final SQLiteDatabase db) throws WrappedException {
                        try {
                            open(OPEN_MODE_RW);
                        } catch (MessagingException e) {
                            throw new WrappedException(e);
                        }

                        List<Message> result = new ArrayList<Message>();

                        List<String> selectionArgs = new ArrayList<String>();
                        Set<String> existingMessages = new HashSet<String>();
                        int start = 0;

                        while (start < messages.size()) {
                            StringBuilder selection = new StringBuilder();

                            selection.append("folder_id = ? AND UID IN (");
                            selectionArgs.add(Long.toString(mFolderId));

                            int count = Math.min(messages.size() - start, UID_CHECK_BATCH_SIZE);

                            for (int i = start, end = start + count; i < end; i++) {
                                if (i > start) {
                                    selection.append(",?");
                                } else {
                                    selection.append("?");
                                }

                                selectionArgs.add(messages.get(i).getUid());
                            }

                            selection.append(")");

                            Cursor cursor = db.query("messages", UID_CHECK_PROJECTION,
                                    selection.toString(), selectionArgs.toArray(EMPTY_STRING_ARRAY),
                                    null, null, null);

                            try {
                                while (cursor.moveToNext()) {
                                    String uid = cursor.getString(0);
                                    existingMessages.add(uid);
                                }
                            } finally {
                                Utility.closeQuietly(cursor);
                            }

                            for (int i = start, end = start + count; i < end; i++) {
                                Message message = messages.get(i);
                                if (!existingMessages.contains(message.getUid())) {
                                    result.add(message);
                                }
                            }

                            existingMessages.clear();
                            selectionArgs.clear();
                            start += count;
                        }

                        return result;
                    }
                });
            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }
        }
    }

    public static class LocalTextBody extends TextBody {
        /**
         * This is an HTML-ified version of the message for display purposes.
         */
        private String mBodyForDisplay;

        public LocalTextBody(String body) {
            super(body);
        }

        public LocalTextBody(String body, String bodyForDisplay) {
            super(body);
            this.mBodyForDisplay = bodyForDisplay;
        }

        public String getBodyForDisplay() {
            return mBodyForDisplay;
        }

        public void setBodyForDisplay(String mBodyForDisplay) {
            this.mBodyForDisplay = mBodyForDisplay;
        }

    }//LocalTextBody

    public class LocalMessage extends MimeMessage {
        private long mId;
        private int mAttachmentCount;
        private String mSubject;

        private String mPreview = "";

        private boolean mHeadersLoaded = false;
        private boolean mMessageDirty = false;

        private long mThreadId;
        private long mRootId;

        public LocalMessage() {
        }

        LocalMessage(String uid, Folder folder) {
            this.mUid = uid;
            this.mFolder = folder;
        }

        private void populateFromGetMessageCursor(Cursor cursor)
        throws MessagingException {
            final String subject = cursor.getString(0);
            this.setSubject(subject == null ? "" : subject);

            Address[] from = Address.unpack(cursor.getString(1));
            if (from.length > 0) {
                this.setFrom(from[0]);
            }
            this.setInternalSentDate(new Date(cursor.getLong(2)));
            this.setUid(cursor.getString(3));
            String flagList = cursor.getString(4);
            if (flagList != null && flagList.length() > 0) {
                String[] flags = flagList.split(",");

                for (String flag : flags) {
                    try {
                        this.setFlagInternal(Flag.valueOf(flag), true);
                    }

                    catch (Exception e) {
                        if (!"X_BAD_FLAG".equals(flag)) {
                            Log.w(K9.LOG_TAG, "Unable to parse flag " + flag);
                        }
                    }
                }
            }
            this.mId = cursor.getLong(5);
            this.setRecipients(RecipientType.TO, Address.unpack(cursor.getString(6)));
            this.setRecipients(RecipientType.CC, Address.unpack(cursor.getString(7)));
            this.setRecipients(RecipientType.BCC, Address.unpack(cursor.getString(8)));
            this.setReplyTo(Address.unpack(cursor.getString(9)));

            this.mAttachmentCount = cursor.getInt(10);
            this.setInternalDate(new Date(cursor.getLong(11)));
            this.setMessageId(cursor.getString(12));

            final String preview = cursor.getString(14);
            mPreview = (preview == null ? "" : preview);

            if (this.mFolder == null) {
                LocalFolder f = new LocalFolder(cursor.getInt(13));
                f.open(LocalFolder.OPEN_MODE_RW);
                this.mFolder = f;
            }

            mThreadId = (cursor.isNull(15)) ? -1 : cursor.getLong(15);
            mRootId = (cursor.isNull(16)) ? -1 : cursor.getLong(16);

            boolean deleted = (cursor.getInt(17) == 1);
            boolean read = (cursor.getInt(18) == 1);
            boolean flagged = (cursor.getInt(19) == 1);
            boolean answered = (cursor.getInt(20) == 1);
            boolean forwarded = (cursor.getInt(21) == 1);

            setFlagInternal(Flag.DELETED, deleted);
            setFlagInternal(Flag.SEEN, read);
            setFlagInternal(Flag.FLAGGED, flagged);
            setFlagInternal(Flag.ANSWERED, answered);
            setFlagInternal(Flag.FORWARDED, forwarded);
        }

        /**
         * Fetch the message text for display. This always returns an HTML-ified version of the
         * message, even if it was originally a text-only message.
         * @return HTML version of message for display purposes or null.
         * @throws MessagingException
         */
        public String getTextForDisplay() throws MessagingException {
            String text = null;    // First try and fetch an HTML part.
            Part part = MimeUtility.findFirstPartByMimeType(this, "text/html");
            if (part == null) {
                // If that fails, try and get a text part.
                part = MimeUtility.findFirstPartByMimeType(this, "text/plain");
                if (part != null && part.getBody() instanceof LocalStore.LocalTextBody) {
                    text = ((LocalStore.LocalTextBody) part.getBody()).getBodyForDisplay();
                }
            } else {
                // We successfully found an HTML part; do the necessary character set decoding.
                text = MimeUtility.getTextFromPart(part);
            }
            return text;
        }


        /* Custom version of writeTo that updates the MIME message based on localMessage
         * changes.
         */

        @Override
        public void writeTo(OutputStream out) throws IOException, MessagingException {
            if (mMessageDirty) buildMimeRepresentation();
            super.writeTo(out);
        }

        private void buildMimeRepresentation() throws MessagingException {
            if (!mMessageDirty) {
                return;
            }

            super.setSubject(mSubject);
            if (this.mFrom != null && this.mFrom.length > 0) {
                super.setFrom(this.mFrom[0]);
            }

            super.setReplyTo(mReplyTo);
            super.setSentDate(this.getSentDate());
            super.setRecipients(RecipientType.TO, mTo);
            super.setRecipients(RecipientType.CC, mCc);
            super.setRecipients(RecipientType.BCC, mBcc);
            if (mMessageId != null) super.setMessageId(mMessageId);

            mMessageDirty = false;
        }

        @Override
        public String getPreview() {
            return mPreview;
        }

        @Override
        public String getSubject() {
            return mSubject;
        }


        @Override
        public void setSubject(String subject) throws MessagingException {
            mSubject = subject;
            mMessageDirty = true;
        }


        @Override
        public void setMessageId(String messageId) {
            mMessageId = messageId;
            mMessageDirty = true;
        }

        @Override
        public boolean hasAttachments() {
            return (mAttachmentCount > 0);
        }

        public int getAttachmentCount() {
            return mAttachmentCount;
        }

        @Override
        public void setFrom(Address from) throws MessagingException {
            this.mFrom = new Address[] { from };
            mMessageDirty = true;
        }


        @Override
        public void setReplyTo(Address[] replyTo) throws MessagingException {
            if (replyTo == null || replyTo.length == 0) {
                mReplyTo = null;
            } else {
                mReplyTo = replyTo;
            }
            mMessageDirty = true;
        }


        /*
         * For performance reasons, we add headers instead of setting them (see super implementation)
         * which removes (expensive) them before adding them
         */
        @Override
        public void setRecipients(RecipientType type, Address[] addresses) throws MessagingException {
            if (type == RecipientType.TO) {
                if (addresses == null || addresses.length == 0) {
                    this.mTo = null;
                } else {
                    this.mTo = addresses;
                }
            } else if (type == RecipientType.CC) {
                if (addresses == null || addresses.length == 0) {
                    this.mCc = null;
                } else {
                    this.mCc = addresses;
                }
            } else if (type == RecipientType.BCC) {
                if (addresses == null || addresses.length == 0) {
                    this.mBcc = null;
                } else {
                    this.mBcc = addresses;
                }
            } else {
                throw new MessagingException("Unrecognized recipient type.");
            }
            mMessageDirty = true;
        }

        public void setFlagInternal(Flag flag, boolean set) throws MessagingException {
            super.setFlag(flag, set);
        }

        @Override
        public long getId() {
            return mId;
        }

        @Override
        public void setFlag(final Flag flag, final boolean set) throws MessagingException {

            try {
                database.execute(true, new DbCallback<Void>() {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException {
                        try {
                            if (flag == Flag.DELETED && set) {
                                delete();
                            }

                            LocalMessage.super.setFlag(flag, set);
                        } catch (MessagingException e) {
                            throw new WrappedException(e);
                        }
                        /*
                         * Set the flags on the message.
                         */
                        ContentValues cv = new ContentValues();
                        cv.put("flags", serializeFlags(getFlags()));
                        cv.put("read", isSet(Flag.SEEN) ? 1 : 0);
                        cv.put("flagged", isSet(Flag.FLAGGED) ? 1 : 0);
                        cv.put("answered", isSet(Flag.ANSWERED) ? 1 : 0);
                        cv.put("forwarded", isSet(Flag.FORWARDED) ? 1 : 0);

                        db.update("messages", cv, "id = ?", new String[] { Long.toString(mId) });

                        return null;
                    }
                });
            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }

            notifyChange();
        }

        /*
         * If a message is being marked as deleted we want to clear out it's content
         * and attachments as well. Delete will not actually remove the row since we need
         * to retain the uid for synchronization purposes.
         */
        private void delete() throws MessagingException

        {
            /*
             * Delete all of the message's content to save space.
             */
            try {
                database.execute(true, new DbCallback<Void>() {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException,
                            UnavailableStorageException {
                        String[] idArg = new String[] { Long.toString(mId) };

                        ContentValues cv = new ContentValues();
                        cv.put("deleted", 1);
                        cv.put("empty", 1);
                        cv.putNull("subject");
                        cv.putNull("sender_list");
                        cv.putNull("date");
                        cv.putNull("to_list");
                        cv.putNull("cc_list");
                        cv.putNull("bcc_list");
                        cv.putNull("preview");
                        cv.putNull("html_content");
                        cv.putNull("text_content");
                        cv.putNull("reply_to_list");

                        db.update("messages", cv, "id = ?", idArg);

                        /*
                         * Delete all of the message's attachments to save space.
                         * We do this explicit deletion here because we're not deleting the record
                         * in messages, which means our ON DELETE trigger for messages won't cascade
                         */
                        try {
                            ((LocalFolder) mFolder).deleteAttachments(mId);
                        } catch (MessagingException e) {
                            throw new WrappedException(e);
                        }

                        db.delete("attachments", "message_id = ?", idArg);
                        return null;
                    }
                });
            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }
            ((LocalFolder)mFolder).deleteHeaders(mId);

            notifyChange();
        }

        /*
         * Completely remove a message from the local database
         *
         * TODO: document how this updates the thread structure
         */
        @Override
        public void destroy() throws MessagingException {
            try {
                database.execute(true, new DbCallback<Void>() {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException,
                        UnavailableStorageException {
                        try {
                            LocalFolder localFolder = (LocalFolder) mFolder;

                            localFolder.deleteAttachments(mId);

                            if (hasThreadChildren(db, mId)) {
                                // This message has children in the thread structure so we need to
                                // make it an empty message.
                                ContentValues cv = new ContentValues();
                                cv.put("id", mId);
                                cv.put("folder_id", localFolder.getId());
                                cv.put("deleted", 0);
                                cv.put("message_id", getMessageId());
                                cv.put("empty", 1);

                                db.replace("messages", null, cv);

                                // Nothing else to do
                                return null;
                            }

                            // Get the message ID of the parent message if it's empty
                            long currentId = getEmptyThreadParent(db, mId);

                            // Delete the placeholder message
                            deleteMessageRow(db, mId);

                            /*
                             * Walk the thread tree to delete all empty parents without children
                             */

                            while (currentId != -1) {
                                if (hasThreadChildren(db, currentId)) {
                                    // We made sure there are no empty leaf nodes and can stop now.
                                    break;
                                }

                                // Get ID of the (empty) parent for the next iteration
                                long newId = getEmptyThreadParent(db, currentId);

                                // Delete the empty message
                                deleteMessageRow(db, currentId);

                                currentId = newId;
                            }

                        } catch (MessagingException e) {
                            throw new WrappedException(e);
                        }
                        return null;
                    }
                });
            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }

            notifyChange();
        }

        /**
         * Get ID of the the given message's parent if the parent is an empty message.
         *
         * @param db
         *         {@link SQLiteDatabase} instance to access the database.
         * @param messageId
         *         The database ID of the message to get the parent for.
         *
         * @return Message ID of the parent message if there exists a parent and it is empty.
         *         Otherwise {@code -1}.
         */
        private long getEmptyThreadParent(SQLiteDatabase db, long messageId) {
            Cursor cursor = db.rawQuery(
                    "SELECT m.id " +
                    "FROM threads t1 " +
                    "JOIN threads t2 ON (t1.parent = t2.id) " +
                    "LEFT JOIN messages m ON (t2.message_id = m.id) " +
                    "WHERE t1.message_id = ? AND m.empty = 1",
                    new String[] { Long.toString(messageId) });

            try {
                return (cursor.moveToFirst() && !cursor.isNull(0)) ? cursor.getLong(0) : -1;
            } finally {
                cursor.close();
            }
        }

        /**
         * Check whether or not a message has child messages in the thread structure.
         *
         * @param db
         *         {@link SQLiteDatabase} instance to access the database.
         * @param messageId
         *         The database ID of the message to get the children for.
         *
         * @return {@code true} if the message has children. {@code false} otherwise.
         */
        private boolean hasThreadChildren(SQLiteDatabase db, long messageId) {
            Cursor cursor = db.rawQuery(
                    "SELECT COUNT(t2.id) " +
                    "FROM threads t1 " +
                    "JOIN threads t2 ON (t2.parent = t1.id) " +
                    "WHERE t1.message_id = ?",
                    new String[] { Long.toString(messageId) });

            try {
                return (cursor.moveToFirst() && !cursor.isNull(0) && cursor.getLong(0) > 0L);
            } finally {
                cursor.close();
            }
        }

        /**
         * Delete a message from the 'messages' and 'threads' tables.
         *
         * @param db
         *         {@link SQLiteDatabase} instance to access the database.
         * @param messageId
         *         The database ID of the message to delete.
         */
        private void deleteMessageRow(SQLiteDatabase db, long messageId) {
            String[] idArg = { Long.toString(messageId) };

            // Delete the message
            db.delete("messages", "id = ?", idArg);

            // Delete row in 'threads' table
            // TODO: create trigger for 'messages' table to get rid of the row in 'threads' table
            db.delete("threads", "message_id = ?", idArg);
        }

        private void loadHeaders() throws UnavailableStorageException {
            ArrayList<LocalMessage> messages = new ArrayList<LocalMessage>();
            messages.add(this);
            mHeadersLoaded = true; // set true before calling populate headers to stop recursion
            ((LocalFolder) mFolder).populateHeaders(messages);

        }

        @Override
        public void addHeader(String name, String value) throws UnavailableStorageException {
            if (!mHeadersLoaded)
                loadHeaders();
            super.addHeader(name, value);
        }

        @Override
        public void setHeader(String name, String value) throws UnavailableStorageException {
            if (!mHeadersLoaded)
                loadHeaders();
            super.setHeader(name, value);
        }

        @Override
        public String[] getHeader(String name) throws UnavailableStorageException {
            if (!mHeadersLoaded)
                loadHeaders();
            return super.getHeader(name);
        }

        @Override
        public void removeHeader(String name) throws UnavailableStorageException {
            if (!mHeadersLoaded)
                loadHeaders();
            super.removeHeader(name);
        }

        @Override
        public Set<String> getHeaderNames() throws UnavailableStorageException {
            if (!mHeadersLoaded)
                loadHeaders();
            return super.getHeaderNames();
        }

        @Override
        public LocalMessage clone() {
            LocalMessage message = new LocalMessage();
            super.copy(message);

            message.mId = mId;
            message.mAttachmentCount = mAttachmentCount;
            message.mSubject = mSubject;
            message.mPreview = mPreview;
            message.mHeadersLoaded = mHeadersLoaded;
            message.mMessageDirty = mMessageDirty;

            return message;
        }

        public long getThreadId() {
            return mThreadId;
        }

        public long getRootId() {
            return mRootId;
        }
    }

    public static class LocalAttachmentBodyPart extends MimeBodyPart {
        private long mAttachmentId = -1;

        public LocalAttachmentBodyPart(Body body, long attachmentId) throws MessagingException {
            super(body);
            mAttachmentId = attachmentId;
        }

        /**
         * Returns the local attachment id of this body, or -1 if it is not stored.
         * @return
         */
        public long getAttachmentId() {
            return mAttachmentId;
        }

        public void setAttachmentId(long attachmentId) {
            mAttachmentId = attachmentId;
        }

        @Override
        public String toString() {
            return "" + mAttachmentId;
        }
    }

    public abstract static class BinaryAttachmentBody implements Body {
        protected String mEncoding;

        @Override
        public abstract InputStream getInputStream() throws MessagingException;

        @Override
        public void writeTo(OutputStream out) throws IOException, MessagingException {
            InputStream in = getInputStream();
            try {
                boolean closeStream = false;
                if (MimeUtil.isBase64Encoding(mEncoding)) {
                    out = new Base64OutputStream(out);
                    closeStream = true;
                } else if (MimeUtil.isQuotedPrintableEncoded(mEncoding)){
                    out = new QuotedPrintableOutputStream(out, false);
                    closeStream = true;
                }

                try {
                    IOUtils.copy(in, out);
                } finally {
                    if (closeStream) {
                        out.close();
                    }
                }
            } finally {
                in.close();
            }
        }

        @Override
        public void setEncoding(String encoding) throws MessagingException {
            mEncoding = encoding;
        }

        public String getEncoding() {
            return mEncoding;
        }
    }

    public static class TempFileBody extends BinaryAttachmentBody {
        private final File mFile;

        public TempFileBody(String filename) {
            mFile = new File(filename);
        }

        @Override
        public InputStream getInputStream() throws MessagingException {
            try {
                return new FileInputStream(mFile);
            } catch (FileNotFoundException e) {
                return new ByteArrayInputStream(EMPTY_BYTE_ARRAY);
            }
        }
    }

    public static class LocalAttachmentBody extends BinaryAttachmentBody {
        private Application mApplication;
        private Uri mUri;

        public LocalAttachmentBody(Uri uri, Application application) {
            mApplication = application;
            mUri = uri;
        }

        @Override
        public InputStream getInputStream() throws MessagingException {
            try {
                return mApplication.getContentResolver().openInputStream(mUri);
            } catch (FileNotFoundException fnfe) {
                /*
                 * Since it's completely normal for us to try to serve up attachments that
                 * have been blown away, we just return an empty stream.
                 */
                return new ByteArrayInputStream(EMPTY_BYTE_ARRAY);
            }
        }

        public Uri getContentUri() {
            return mUri;
        }
    }

    /**
     * A {@link LocalAttachmentBody} extension containing a message/rfc822 type body
     *
     */
    public static class LocalAttachmentMessageBody extends LocalAttachmentBody implements CompositeBody {

        public LocalAttachmentMessageBody(Uri uri, Application application) {
            super(uri, application);
        }

        @Override
        public void writeTo(OutputStream out) throws IOException, MessagingException {
            AttachmentMessageBodyUtil.writeTo(this, out);
        }

        @Override
        public void setUsing7bitTransport() throws MessagingException {
            /*
             * There's nothing to recurse into here, so there's nothing to do.
             * The enclosing BodyPart already called setEncoding(MimeUtil.ENC_7BIT).  Once
             * writeTo() is called, the file with the rfc822 body will be opened
             * for reading and will then be recursed.
             */

        }

        @Override
        public void setEncoding(String encoding) throws MessagingException {
            if (!MimeUtil.ENC_7BIT.equalsIgnoreCase(encoding)
                    && !MimeUtil.ENC_8BIT.equalsIgnoreCase(encoding)) {
                throw new MessagingException(
                        "Incompatible content-transfer-encoding applied to a CompositeBody");
            }
            mEncoding = encoding;
        }
    }

    public static class TempFileMessageBody extends TempFileBody implements CompositeBody {

        public TempFileMessageBody(String filename) {
            super(filename);
        }

        @Override
        public void writeTo(OutputStream out) throws IOException, MessagingException {
            AttachmentMessageBodyUtil.writeTo(this, out);
        }

        @Override
        public void setUsing7bitTransport() throws MessagingException {
            // see LocalAttachmentMessageBody.setUsing7bitTransport()
        }

        @Override
        public void setEncoding(String encoding) throws MessagingException {
            if (!MimeUtil.ENC_7BIT.equalsIgnoreCase(encoding)
                    && !MimeUtil.ENC_8BIT.equalsIgnoreCase(encoding)) {
                throw new MessagingException(
                        "Incompatible content-transfer-encoding applied to a CompositeBody");
            }
            mEncoding = encoding;
        }
    }

    public static class AttachmentMessageBodyUtil {
        public static void writeTo(BinaryAttachmentBody body, OutputStream out) throws IOException,
                MessagingException {
            InputStream in = body.getInputStream();
            try {
                if (MimeUtil.ENC_7BIT.equalsIgnoreCase(body.getEncoding())) {
                    /*
                     * If we knew the message was already 7bit clean, then it
                     * could be sent along without processing. But since we
                     * don't know, we recursively parse it.
                     */
                    MimeMessage message = new MimeMessage(in, true);
                    message.setUsing7bitTransport();
                    message.writeTo(out);
                } else {
                    IOUtils.copy(in, out);
                }
            } finally {
                in.close();
            }
        }
    }

    static class ThreadInfo {
        public final long threadId;
        public final long msgId;
        public final String messageId;
        public final long rootId;
        public final long parentId;

        public ThreadInfo(long threadId, long msgId, String messageId, long rootId, long parentId) {
            this.threadId = threadId;
            this.msgId = msgId;
            this.messageId = messageId;
            this.rootId = rootId;
            this.parentId = parentId;
        }
    }

    public LockableDatabase getDatabase() {
        return database;
    }

    private void notifyChange() {
        Uri uri = Uri.withAppendedPath(EmailProvider.CONTENT_URI, "account/" + uUid + "/messages");
        mContentResolver.notifyChange(uri, null);
    }

    /**
     * Split database operations with a large set of arguments into multiple SQL statements.
     *
     * <p>
     * At the time of this writing (2012-12-06) SQLite only supports around 1000 arguments. That's
     * why we have to split SQL statements with a large set of arguments into multiple SQL
     * statements each working on a subset of the arguments.
     * </p>
     *
     * @param selectionCallback
     *         Supplies the argument set and the code to query/update the database.
     * @param batchSize
     *         The maximum size of the selection set in each SQL statement.
     *
     * @throws MessagingException
     */
    public void doBatchSetSelection(final BatchSetSelection selectionCallback, final int batchSize)
            throws MessagingException {

        final List<String> selectionArgs = new ArrayList<String>();
        int start = 0;

        while (start < selectionCallback.getListSize()) {
            final StringBuilder selection = new StringBuilder();

            selection.append(" IN (");

            int count = Math.min(selectionCallback.getListSize() - start, batchSize);

            for (int i = start, end = start + count; i < end; i++) {
                if (i > start) {
                    selection.append(",?");
                } else {
                    selection.append("?");
                }

                selectionArgs.add(selectionCallback.getListItem(i));
            }

            selection.append(")");

            try {
                database.execute(true, new DbCallback<Void>() {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException,
                            UnavailableStorageException {

                        selectionCallback.doDbWork(db, selection.toString(),
                                selectionArgs.toArray(EMPTY_STRING_ARRAY));

                        return null;
                    }
                });

                selectionCallback.postDbWork();

            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }

            selectionArgs.clear();
            start += count;
        }
    }

    /**
     * Defines the behavior of {@link LocalStore#doBatchSetSelection(BatchSetSelection, int)}.
     */
    public interface BatchSetSelection {
        /**
         * @return The size of the argument list.
         */
        int getListSize();

        /**
         * Get a specific item of the argument list.
         *
         * @param index
         *         The index of the item.
         *
         * @return Item at position {@code i} of the argument list.
         */
        String getListItem(int index);

        /**
         * Execute the SQL statement.
         *
         * @param db
         *         Use this {@link SQLiteDatabase} instance for your SQL statement.
         * @param selectionSet
         *         A partial selection string containing place holders for the argument list, e.g.
         *         {@code " IN (?,?,?)"} (starts with a space).
         * @param selectionArgs
         *         The current subset of the argument list.
         * @throws UnavailableStorageException
         */
        void doDbWork(SQLiteDatabase db, String selectionSet, String[] selectionArgs)
                throws UnavailableStorageException;

        /**
         * This will be executed after each invocation of
         * {@link #doDbWork(SQLiteDatabase, String, String[])} (after the transaction has been
         * committed).
         */
        void postDbWork();
    }

    /**
     * Change the state of a flag for a list of messages.
     *
     * <p>
     * The goal of this method is to be fast. Currently this means using as few SQL UPDATE
     * statements as possible.
     *
     * @param messageIds
     *         A list of primary keys in the "messages" table.
     * @param flag
     *         The flag to change. This must be a flag with a separate column in the database.
     * @param newState
     *         {@code true}, if the flag should be set. {@code false}, otherwise.
     *
     * @throws MessagingException
     */
    public void setFlag(final List<Long> messageIds, final Flag flag, final boolean newState)
            throws MessagingException {

        final ContentValues cv = new ContentValues();
        cv.put(getColumnNameForFlag(flag), newState);

        doBatchSetSelection(new BatchSetSelection() {

            @Override
            public int getListSize() {
                return messageIds.size();
            }

            @Override
            public String getListItem(int index) {
                return Long.toString(messageIds.get(index));
            }

            @Override
            public void doDbWork(SQLiteDatabase db, String selectionSet, String[] selectionArgs)
                    throws UnavailableStorageException {

                db.update("messages", cv, "(empty IS NULL OR empty != 1) AND id" + selectionSet,
                        selectionArgs);
            }

            @Override
            public void postDbWork() {
                notifyChange();
            }
        }, FLAG_UPDATE_BATCH_SIZE);
    }

    /**
     * Change the state of a flag for a list of threads.
     *
     * <p>
     * The goal of this method is to be fast. Currently this means using as few SQL UPDATE
     * statements as possible.
     *
     * @param threadRootIds
     *         A list of root thread IDs.
     * @param flag
     *         The flag to change. This must be a flag with a separate column in the database.
     * @param newState
     *         {@code true}, if the flag should be set. {@code false}, otherwise.
     *
     * @throws MessagingException
     */
    public void setFlagForThreads(final List<Long> threadRootIds, Flag flag, final boolean newState)
            throws MessagingException {

        final String flagColumn = getColumnNameForFlag(flag);

        doBatchSetSelection(new BatchSetSelection() {

            @Override
            public int getListSize() {
                return threadRootIds.size();
            }

            @Override
            public String getListItem(int index) {
                return Long.toString(threadRootIds.get(index));
            }

            @Override
            public void doDbWork(SQLiteDatabase db, String selectionSet, String[] selectionArgs)
                    throws UnavailableStorageException {

                db.execSQL("UPDATE messages SET " + flagColumn + " = " + ((newState) ? "1" : "0") +
                        " WHERE id IN (" +
                        "SELECT m.id FROM threads t " +
                        "LEFT JOIN messages m ON (t.message_id = m.id) " +
                        "WHERE (m.empty IS NULL OR m.empty != 1) AND m.deleted = 0 " +
                        "AND t.root" + selectionSet + ")",
                        selectionArgs);
            }

            @Override
            public void postDbWork() {
                notifyChange();
            }
        }, THREAD_FLAG_UPDATE_BATCH_SIZE);
    }

    /**
     * Get folder name and UID for the supplied messages.
     *
     * @param messageIds
     *         A list of primary keys in the "messages" table.
     * @param threadedList
     *         If this is {@code true}, {@code messageIds} contains the thread IDs of the messages
     *         at the root of a thread. In that case return UIDs for all messages in these threads.
     *         If this is {@code false} only the UIDs for messages in {@code messageIds} are
     *         returned.
     *
     * @return The list of UIDs for the messages grouped by folder name.
     *
     * @throws MessagingException
     */
    public Map<String, List<String>> getFoldersAndUids(final List<Long> messageIds,
            final boolean threadedList) throws MessagingException {

        final Map<String, List<String>> folderMap = new HashMap<String, List<String>>();

        doBatchSetSelection(new BatchSetSelection() {

            @Override
            public int getListSize() {
                return messageIds.size();
            }

            @Override
            public String getListItem(int index) {
                return Long.toString(messageIds.get(index));
            }

            @Override
            public void doDbWork(SQLiteDatabase db, String selectionSet, String[] selectionArgs)
                    throws UnavailableStorageException {

                if (threadedList) {
                    String sql = "SELECT m.uid, f.name " +
                            "FROM threads t " +
                            "LEFT JOIN messages m ON (t.message_id = m.id) " +
                            "LEFT JOIN folders f ON (m.folder_id = f.id) " +
                            "WHERE (m.empty IS NULL OR m.empty != 1) AND m.deleted = 0 " +
                            "AND t.root" + selectionSet;

                    getDataFromCursor(db.rawQuery(sql, selectionArgs));

                } else {
                    String sql =
                            "SELECT m.uid, f.name " +
                            "FROM messages m " +
                            "LEFT JOIN folders f ON (m.folder_id = f.id) " +
                            "WHERE (m.empty IS NULL OR m.empty != 1) AND m.id" + selectionSet;

                    getDataFromCursor(db.rawQuery(sql, selectionArgs));
                }
            }

            private void getDataFromCursor(Cursor cursor) {
                try {
                    while (cursor.moveToNext()) {
                        String uid = cursor.getString(0);
                        String folderName = cursor.getString(1);

                        List<String> uidList = folderMap.get(folderName);
                        if (uidList == null) {
                            uidList = new ArrayList<String>();
                            folderMap.put(folderName, uidList);
                        }

                        uidList.add(uid);
                    }
                } finally {
                    cursor.close();
                }
            }

            @Override
            public void postDbWork() {
                notifyChange();

            }
        }, UID_CHECK_BATCH_SIZE);

        return folderMap;
    }
}
