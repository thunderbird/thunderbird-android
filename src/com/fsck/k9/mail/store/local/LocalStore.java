
package com.fsck.k9.mail.store.local;

import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;


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
import com.fsck.k9.controller.MessageRetrievalListener;
import com.fsck.k9.helper.StringUtils;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.store.LockableDatabase;
import com.fsck.k9.mail.store.StorageManager;
import com.fsck.k9.mail.store.UnavailableStorageException;
import com.fsck.k9.mail.store.LockableDatabase.DbCallback;
import com.fsck.k9.mail.store.LockableDatabase.SchemaDefinition;
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

    static final Message[] EMPTY_MESSAGE_ARRAY = new Message[0];
    static final String[] EMPTY_STRING_ARRAY = new String[0];
    static final Flag[] EMPTY_FLAG_ARRAY = new Flag[0];
    static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /*
     * a String containing the columns getMessages expects to work with
     * in the correct order.
     */
    static String GET_MESSAGES_COLS =
        "subject, sender_list, date, uid, flags, messages.id, to_list, cc_list, " +
        "bcc_list, reply_to_list, attachment_count, internal_date, messages.message_id, " +
        "folder_id, preview, threads.id, threads.root, deleted, read, flagged, answered, " +
        "forwarded ";

    static final String GET_FOLDER_COLS =
        "folders.id, name, visible_limit, last_updated, status, push_state, last_pushed, " +
        "integrate, top_group, poll_class, push_class, display_class, notify_class";

    static final int FOLDER_ID_INDEX = 0;
    static final int FOLDER_NAME_INDEX = 1;
    static final int FOLDER_VISIBLE_LIMIT_INDEX = 2;
    static final int FOLDER_LAST_CHECKED_INDEX = 3;
    static final int FOLDER_STATUS_INDEX = 4;
    static final int FOLDER_PUSH_STATE_INDEX = 5;
    static final int FOLDER_LAST_PUSHED_INDEX = 6;
    static final int FOLDER_INTEGRATE_INDEX = 7;
    static final int FOLDER_TOP_GROUP_INDEX = 8;
    static final int FOLDER_SYNC_CLASS_INDEX = 9;
    static final int FOLDER_PUSH_CLASS_INDEX = 10;
    static final int FOLDER_DISPLAY_CLASS_INDEX = 11;
    static final int FOLDER_NOTIFY_CLASS_INDEX = 12;

    static final String[] UID_CHECK_PROJECTION = { "uid" };

    /**
     * Maximum number of UIDs to check for existence at once.
     *
     * @see LocalFolder#extractNewMessages(List)
     */
    static final int UID_CHECK_BATCH_SIZE = 500;

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

    public static final int DB_VERSION = 50;


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

    final Application mApplication;

    LockableDatabase database;

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
            Log.i(K9.LOG_TAG, String.format(Locale.US, "Upgrading database from version %d to version %d",
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
                               + "integrate INTEGER, top_group INTEGER, poll_class TEXT, push_class TEXT, display_class TEXT, notify_class TEXT"
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
                        } catch (SQLiteException e) {
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
                            LocalFolder oldOutbox = new LocalFolder(LocalStore.this, "OUTBOX");
                            if (oldOutbox.exists()) {
                                ContentValues cv = new ContentValues();
                                cv.put("name", Account.OUTBOX);
                                db.update("folders", cv, "name = ?", new String[] { "OUTBOX" });
                                Log.i(K9.LOG_TAG, "Renamed folder OUTBOX to " + Account.OUTBOX);
                            }

                            // Check if old (pre v3.800) localized outbox folder exists
                            String localizedOutbox = K9.app.getString(R.string.special_mailbox_name_outbox);
                            LocalFolder obsoleteOutbox = new LocalFolder(LocalStore.this, localizedOutbox);
                            if (obsoleteOutbox.exists()) {
                                // Get all messages from the localized outbox ...
                                Message[] messages = obsoleteOutbox.getMessages(null, false);

                                if (messages.length > 0) {
                                    // ... and move them to the drafts folder (we don't want to
                                    // surprise the user by sending potentially very old messages)
                                    LocalFolder drafts = new LocalFolder(LocalStore.this, mAccount.getDraftsFolderName());
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
                    if (db.getVersion() < 50) {
                        try {
                            db.execSQL("ALTER TABLE folders ADD notify_class TEXT default '" +
                                    Folder.FolderClass.INHERITED.name() + "'");
                        } catch (SQLiteException e) {
                            if (! e.getMessage().startsWith("duplicate column name:")) {
                                throw e;
                            }
                        }

                        ContentValues cv = new ContentValues();
                        cv.put("notify_class", Folder.FolderClass.FIRST_CLASS.name());

                        db.update("folders", cv, "name = ?",
                                new String[] { getAccount().getInboxFolderName() });
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
                if (files != null) {
                    for (File file : files) {
                        if (file.exists()) {
                            attachmentLength += file.length();
                        }
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
        return new LocalFolder(this, name);
    }

    public LocalFolder getFolderById(long folderId) {
        return new LocalFolder(this, folderId);
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
                            LocalFolder folder = new LocalFolder(LocalStore.this, folderName);
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
        } catch (UnsupportedEncodingException uee) {
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
    Message[] getMessages(
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
                        LocalMessage message = new LocalMessage(LocalStore.this, null, folder);
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
                        LocalMessage message = new LocalMessage(LocalStore.this, null, folder);
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
                            prefHolder.notifyClass = LocalFolder.FolderClass.FIRST_CLASS;
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

                    db.execSQL("INSERT INTO folders (name, visible_limit, top_group, display_class, poll_class, notify_class, push_class, integrate) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {
                                   name,
                                   visibleLimit,
                                   prefHolder.inTopGroup ? 1 : 0,
                                   prefHolder.displayClass.name(),
                                   prefHolder.syncClass.name(),
                                   prefHolder.notifyClass.name(),
                                   prefHolder.pushClass.name(),
                                   prefHolder.integrate ? 1 : 0,
                               });

                }
                return null;
            }
        });
    }


    String serializeFlags(Flag[] flags) {
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

    public LockableDatabase getDatabase() {
        return database;
    }

    void notifyChange() {
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
