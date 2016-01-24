package com.fsck.k9.mailstore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;

class StoreSchemaDefinition implements LockableDatabase.SchemaDefinition {
    private final LocalStore localStore;

    StoreSchemaDefinition(LocalStore localStore) {
        this.localStore = localStore;
    }

    @Override
    public int getVersion() {
        return LocalStore.DB_VERSION;
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
                                        db.getVersion(), LocalStore.DB_VERSION));

        db.beginTransaction();
        try {
            // schema version 29 was when we moved to incremental updates
            // in the case of a new db or a < v29 db, we blow away and start from scratch
            if (db.getVersion() < 29) {
                dbCreateDatabaseFromScratch(db);
            } else {
                switch (db.getVersion()) {
                    // in the case that we're starting out at 29 or newer, run all the needed updates
                    case 29:
                        db30AddDeletedColumn(db);
                    case 30:
                        db31ChangeMsgFolderIdDeletedDateIndex(db);
                    case 31:
                        db32UpdateDeletedColumnFromFlags(db);
                    case 32:
                        db33AddPreviewColumn(db);
                    case 33:
                        db34AddFlaggedCountColumn(db);
                    case 34:
                        db35UpdateRemoveXNoSeenInfoFlag(db);
                    case 35:
                        db36AddAttachmentsContentIdColumn(db);
                    case 36:
                        db37AddAttachmentsContentDispositionColumn(db);
                    case 37:
                        // Database version 38 is solely to prune cached attachments now that we clear them better
                    case 38:
                        db39HeadersPruneOrphans(db);
                    case 39:
                        db40AddMimeTypeColumn(db);
                    case 40:
                        db41FoldersAddClassColumns(db);
                        db41UpdateFolderMetadata(db, localStore);
                    case 41:
                        boolean notUpdatingFromEarlierThan41 = db.getVersion() == 41;
                        if (notUpdatingFromEarlierThan41) {
                            db42From41MoveFolderPreferences(localStore);
                        }
                    case 42:
                        db43FixOutboxFolders(db, localStore);
                    case 43:
                        db44AddMessagesThreadingColumns(db);
                    case 44:
                        db45ChangeThreadingIndexes(db);
                    case 45:
                        db46AddMessagesFlagColumns(db, localStore);
                    case 46:
                        db47CreateThreadsTable(db);
                    case 47:
                        db48UpdateThreadsSetRootWhereNull(db);
                    case 48:
                        db49CreateMsgCompositeIndex(db);
                    case 49:
                        db50FoldersAddNotifyClassColumn(db, localStore);
                    case 50:
                        throw new IllegalStateException("Database upgrade not supported yet!");
                    case 51:
                        db52AddMoreMessagesColumnToFoldersTable(db);
                    case 52:
                        db53RemoveNullValuesFromEmptyColumnInMessagesTable(db);
                    case 53:
                        db54AddPreviewTypeColumn(db);
                }
            }

            db.setVersion(LocalStore.DB_VERSION);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (db.getVersion() != LocalStore.DB_VERSION) {
            throw new RuntimeException("Database upgrade failed!");
        }
    }

    private static void dbCreateDatabaseFromScratch(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS folders");
        db.execSQL("CREATE TABLE folders (" +
                "id INTEGER PRIMARY KEY," +
                "name TEXT, " +
                "last_updated INTEGER, " +
                "unread_count INTEGER, " +
                "visible_limit INTEGER, " +
                "status TEXT, " +
                "push_state TEXT, " +
                "last_pushed INTEGER, " +
                "flagged_count INTEGER default 0, " +
                "integrate INTEGER, " +
                "top_group INTEGER, " +
                "poll_class TEXT, " +
                "push_class TEXT, " +
                "display_class TEXT, " +
                "notify_class TEXT, " +
                "more_messages TEXT default \"unknown\"" +
                ")");

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
                "attachment_count INTEGER, " +
                "internal_date INTEGER, " +
                "message_id TEXT, " +
                "preview_type TEXT default \"none\", " +
                "preview TEXT, " +
                "mime_type TEXT, "+
                "normalized_subject_hash INTEGER, " +
                "empty INTEGER default 0, " +
                "read INTEGER default 0, " +
                "flagged INTEGER default 0, " +
                "answered INTEGER default 0, " +
                "forwarded INTEGER default 0, " +
                "message_part_id INTEGER" +
                ")");

        db.execSQL("CREATE TABLE message_parts (" +
                "id INTEGER PRIMARY KEY, " +
                "type INTEGER NOT NULL, " +
                "root INTEGER, " +
                "parent INTEGER NOT NULL, " +
                "seq INTEGER NOT NULL, " +
                "mime_type TEXT, " +
                "decoded_body_size INTEGER, " +
                "display_name TEXT, " +
                "header TEXT, " +
                "encoding TEXT, " +
                "charset TEXT, " +
                "data_location INTEGER NOT NULL, " +
                "data BLOB, " +
                "preamble TEXT, " +
                "epilogue TEXT, " +
                "boundary TEXT, " +
                "content_id TEXT, " +
                "server_extra TEXT" +
                ")");

        db.execSQL("CREATE TRIGGER set_message_part_root " +
                "AFTER INSERT ON message_parts " +
                "BEGIN " +
                "UPDATE message_parts SET root=id WHERE root IS NULL AND ROWID = NEW.ROWID; " +
                "END");

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

        db.execSQL("DROP TABLE IF EXISTS pending_commands");
        db.execSQL("CREATE TABLE pending_commands " +
                "(id INTEGER PRIMARY KEY, command TEXT, arguments TEXT)");

        db.execSQL("DROP TRIGGER IF EXISTS delete_folder");
        db.execSQL("CREATE TRIGGER delete_folder BEFORE DELETE ON folders BEGIN DELETE FROM messages WHERE old.id = folder_id; END;");

        db.execSQL("DROP TRIGGER IF EXISTS delete_message");
        db.execSQL("CREATE TRIGGER delete_message " +
                "BEFORE DELETE ON messages " +
                "BEGIN " +
                "DELETE FROM message_parts WHERE root = OLD.message_part_id;" +
                "END");
    }

    private static void db30AddDeletedColumn(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE messages ADD deleted INTEGER default 0");
        } catch (SQLiteException e) {
            if (! e.toString().startsWith("duplicate column name: deleted")) {
                throw e;
            }
        }
    }

    private static void db31ChangeMsgFolderIdDeletedDateIndex(SQLiteDatabase db) {
        db.execSQL("DROP INDEX IF EXISTS msg_folder_id_date");
        db.execSQL("CREATE INDEX IF NOT EXISTS msg_folder_id_deleted_date ON messages (folder_id,deleted,internal_date)");
    }

    private static void db32UpdateDeletedColumnFromFlags(SQLiteDatabase db) {
        db.execSQL("UPDATE messages SET deleted = 1 WHERE flags LIKE '%DELETED%'");
    }

    private static void db33AddPreviewColumn(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE messages ADD preview TEXT");
        } catch (SQLiteException e) {
            if (! e.toString().startsWith("duplicate column name: preview")) {
                throw e;
            }
        }
    }

    private static void db34AddFlaggedCountColumn(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE folders ADD flagged_count INTEGER default 0");
        } catch (SQLiteException e) {
            if (! e.getMessage().startsWith("duplicate column name: flagged_count")) {
                throw e;
            }
        }
    }

    private static void db35UpdateRemoveXNoSeenInfoFlag(SQLiteDatabase db) {
        try {
            db.execSQL("update messages set flags = replace(flags, 'X_NO_SEEN_INFO', 'X_BAD_FLAG')");
        } catch (SQLiteException e) {
            Log.e(K9.LOG_TAG, "Unable to get rid of obsolete flag X_NO_SEEN_INFO", e);
        }
    }

    private static void db36AddAttachmentsContentIdColumn(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE attachments ADD content_id TEXT");
        } catch (SQLiteException e) {
            Log.e(K9.LOG_TAG, "Unable to add content_id column to attachments");
        }
    }

    private static void db37AddAttachmentsContentDispositionColumn(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE attachments ADD content_disposition TEXT");
        } catch (SQLiteException e) {
            Log.e(K9.LOG_TAG, "Unable to add content_disposition column to attachments");
        }
    }

    private static void db39HeadersPruneOrphans(SQLiteDatabase db) {
        try {
            db.execSQL("DELETE FROM headers WHERE id in (SELECT headers.id FROM headers LEFT JOIN messages ON headers.message_id = messages.id WHERE messages.id IS NULL)");
        } catch (SQLiteException e) {
            Log.e(K9.LOG_TAG, "Unable to remove extra header data from the database");
        }
    }

    private static void db40AddMimeTypeColumn(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE messages ADD mime_type TEXT");
        } catch (SQLiteException e) {
            Log.e(K9.LOG_TAG, "Unable to add mime_type column to messages");
        }
    }

    private static void db41UpdateFolderMetadata(SQLiteDatabase db, LocalStore localStore) {
        Cursor cursor = null;
        try {
            SharedPreferences prefs = localStore.getPreferences();
            cursor = db.rawQuery("SELECT id, name FROM folders", null);
            while (cursor.moveToNext()) {
                try {
                    int id = cursor.getInt(0);
                    String name = cursor.getString(1);
                    update41Metadata(db, localStore, prefs, id, name);
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

    private static void update41Metadata(SQLiteDatabase db, LocalStore localStore, SharedPreferences prefs,
            int id, String name) {

        Folder.FolderClass displayClass = Folder.FolderClass.NO_CLASS;
        Folder.FolderClass syncClass = Folder.FolderClass.INHERITED;
        Folder.FolderClass pushClass = Folder.FolderClass.SECOND_CLASS;
        boolean inTopGroup = false;
        boolean integrate = false;
        if (localStore.getAccount().getInboxFolderName().equals(name)) {
            displayClass = Folder.FolderClass.FIRST_CLASS;
            syncClass =  Folder.FolderClass.FIRST_CLASS;
            pushClass =  Folder.FolderClass.FIRST_CLASS;
            inTopGroup = true;
            integrate = true;
        }

        try {
            displayClass = Folder.FolderClass.valueOf(prefs.getString(localStore.uUid + "." + name + ".displayMode", displayClass.name()));
            syncClass = Folder.FolderClass.valueOf(prefs.getString(localStore.uUid + "." + name + ".syncMode", syncClass.name()));
            pushClass = Folder.FolderClass.valueOf(prefs.getString(localStore.uUid + "." + name + ".pushMode", pushClass.name()));
            inTopGroup = prefs.getBoolean(localStore.uUid + "." + name + ".inTopGroup", inTopGroup);
            integrate = prefs.getBoolean(localStore.uUid + "." + name + ".integrate", integrate);
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

    private static void db41FoldersAddClassColumns(SQLiteDatabase db) {
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
    }

    private static void db42From41MoveFolderPreferences(LocalStore localStore) {
        try {
            long startTime = System.currentTimeMillis();
            SharedPreferences.Editor editor = localStore.getPreferences().edit();

            List<? extends Folder > folders = localStore.getPersonalNamespaces(true);
            for (Folder folder : folders) {
                if (folder instanceof LocalFolder) {
                    LocalFolder lFolder = (LocalFolder)folder;
                    lFolder.save(editor);
                }
            }

            editor.commit();
            long endTime = System.currentTimeMillis();
            Log.i(K9.LOG_TAG, "Putting folder preferences for " + folders.size() +
                    " folders back into Preferences took " + (endTime - startTime) + " ms");
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Could not replace Preferences in upgrade from DB_VERSION 41", e);
        }
    }

    private static void db43FixOutboxFolders(SQLiteDatabase db, LocalStore localStore) {
        try {
            // If folder "OUTBOX" (old, v3.800 - v3.802) exists, rename it to
            // "K9MAIL_INTERNAL_OUTBOX" (new)
            LocalFolder oldOutbox = new LocalFolder(localStore, "OUTBOX");
            if (oldOutbox.exists()) {
                ContentValues cv = new ContentValues();
                cv.put("name", Account.OUTBOX);
                db.update("folders", cv, "name = ?", new String[] { "OUTBOX" });
                Log.i(K9.LOG_TAG, "Renamed folder OUTBOX to " + Account.OUTBOX);
            }

            // Check if old (pre v3.800) localized outbox folder exists
            String localizedOutbox = localStore.context.getString(R.string.special_mailbox_name_outbox);
            LocalFolder obsoleteOutbox = new LocalFolder(localStore, localizedOutbox);
            if (obsoleteOutbox.exists()) {
                // Get all messages from the localized outbox ...
                List<? extends Message> messages = obsoleteOutbox.getMessages(null, false);

                if (messages.size() > 0) {
                    // ... and move them to the drafts folder (we don't want to
                    // surprise the user by sending potentially very old messages)
                    LocalFolder drafts = new LocalFolder(localStore, localStore.getAccount().getDraftsFolderName());
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

    private static void db44AddMessagesThreadingColumns(SQLiteDatabase db) {
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

    private static void db45ChangeThreadingIndexes(SQLiteDatabase db) {
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

    private static void db46AddMessagesFlagColumns(SQLiteDatabase db, LocalStore localStore) {
        db.execSQL("ALTER TABLE messages ADD read INTEGER default 0");
        db.execSQL("ALTER TABLE messages ADD flagged INTEGER default 0");
        db.execSQL("ALTER TABLE messages ADD answered INTEGER default 0");
        db.execSQL("ALTER TABLE messages ADD forwarded INTEGER default 0");

        String[] projection = { "id", "flags" };

        ContentValues cv = new ContentValues();
        List<Flag> extraFlags = new ArrayList<>();

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


                cv.put("flags", localStore.serializeFlags(extraFlags));
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

    private static void db47CreateThreadsTable(SQLiteDatabase db) {
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

    private static void db48UpdateThreadsSetRootWhereNull(SQLiteDatabase db) {
        db.execSQL("UPDATE threads SET root=id WHERE root IS NULL");

        db.execSQL("CREATE TRIGGER set_thread_root " +
                "AFTER INSERT ON threads " +
                "BEGIN " +
                "UPDATE threads SET root=id WHERE root IS NULL AND ROWID = NEW.ROWID; " +
                "END");
    }

    private static void db49CreateMsgCompositeIndex(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS msg_composite ON messages (deleted, empty,folder_id,flagged,read)");
    }

    private static void db50FoldersAddNotifyClassColumn(SQLiteDatabase db, LocalStore localStore) {
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
                new String[] { localStore.getAccount().getInboxFolderName() });
    }

    private static void db52AddMoreMessagesColumnToFoldersTable(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE folders ADD more_messages TEXT default \"unknown\"");
    }

    private static void db53RemoveNullValuesFromEmptyColumnInMessagesTable(SQLiteDatabase db) {
        db.execSQL("UPDATE messages SET empty = 0 WHERE empty IS NULL");
    }

    private static void db54AddPreviewTypeColumn(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE messages ADD preview_type TEXT default \"none\"");
        db.execSQL("UPDATE messages SET preview_type = 'text' WHERE preview IS NOT NULL");
    }

}
