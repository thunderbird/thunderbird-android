
package com.fsck.k9.mail.store;

import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fsck.k9.helper.HtmlConverter;
import org.apache.commons.io.IOUtils;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.controller.MessageRemovalListener;
import com.fsck.k9.controller.MessageRetrievalListener;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
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
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mail.store.LockableDatabase.DbCallback;
import com.fsck.k9.mail.store.LockableDatabase.WrappedException;
import com.fsck.k9.mail.store.StorageManager.StorageProvider;
import com.fsck.k9.provider.AttachmentProvider;

/**
 * <pre>
 * Implements a SQLite database backed local store for Messages.
 * </pre>
 */
public class LocalStore extends Store implements Serializable
{

    private static final Message[] EMPTY_MESSAGE_ARRAY = new Message[0];

    /**
     * Immutable empty {@link String} array
     */
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final Flag[] PERMANENT_FLAGS = { Flag.DELETED, Flag.X_DESTROYED, Flag.SEEN, Flag.FLAGGED };

    private static Set<String> HEADERS_TO_SAVE = new HashSet<String>();
    static
    {
        HEADERS_TO_SAVE.add(K9.IDENTITY_HEADER);
        HEADERS_TO_SAVE.add("To");
        HEADERS_TO_SAVE.add("Cc");
        HEADERS_TO_SAVE.add("From");
        HEADERS_TO_SAVE.add("In-Reply-To");
        HEADERS_TO_SAVE.add("References");
        HEADERS_TO_SAVE.add("Content-ID");
        HEADERS_TO_SAVE.add("Content-Disposition");
        HEADERS_TO_SAVE.add("User-Agent");
    }
    /*
     * a String containing the columns getMessages expects to work with
     * in the correct order.
     */
    static private String GET_MESSAGES_COLS =
        "subject, sender_list, date, uid, flags, id, to_list, cc_list, "
        + "bcc_list, reply_to_list, attachment_count, internal_date, message_id, folder_id, preview ";

    protected static final int DB_VERSION = 41;

    protected String uUid = null;

    private final Application mApplication;

    private LockableDatabase database;

    /**
     * local://localhost/path/to/database/uuid.db
     * This constructor is only used by {@link Store#getLocalInstance(Account, Application)}
     * @param account
     * @param application
     * @throws UnavailableStorageException if not {@link StorageProvider#isReady(Context)}
     */
    public LocalStore(final Account account, final Application application) throws MessagingException
    {
        super(account);
        database = new LockableDatabase(application, account.getUuid(), new StoreSchemaDefinition());

        mApplication = application;
        database.setStorageProviderId(account.getLocalStorageProviderId());
        uUid = account.getUuid();

        database.open();
    }

    public void switchLocalStorage(final String newStorageProviderId) throws MessagingException
    {
        database.switchProvider(newStorageProviderId);
    }

    private class StoreSchemaDefinition implements LockableDatabase.SchemaDefinition
    {
        @Override
        public int getVersion()
        {
            return DB_VERSION;
        }

        @Override
        public void doDbUpgrade(final SQLiteDatabase db)
        {
            Log.i(K9.LOG_TAG, String.format("Upgrading database from version %d to version %d",
                                            db.getVersion(), DB_VERSION));


            AttachmentProvider.clear(mApplication);

            try
            {
                // schema version 29 was when we moved to incremental updates
                // in the case of a new db or a < v29 db, we blow away and start from scratch
                if (db.getVersion() < 29)
                {

                    db.execSQL("DROP TABLE IF EXISTS folders");
                    db.execSQL("CREATE TABLE folders (id INTEGER PRIMARY KEY, name TEXT, "
                               + "last_updated INTEGER, unread_count INTEGER, visible_limit INTEGER, status TEXT, "
                               + "push_state TEXT, last_pushed INTEGER, flagged_count INTEGER default 0, "
                               + "integrate INTEGER, top_group INTEGER, poll_class TEXT, push_class TEXT, display_class TEXT"
                               +")");

                    db.execSQL("CREATE INDEX IF NOT EXISTS folder_name ON folders (name)");
                    db.execSQL("DROP TABLE IF EXISTS messages");
                    db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY, deleted INTEGER default 0, folder_id INTEGER, uid TEXT, subject TEXT, "
                               + "date INTEGER, flags TEXT, sender_list TEXT, to_list TEXT, cc_list TEXT, bcc_list TEXT, reply_to_list TEXT, "
                               + "html_content TEXT, text_content TEXT, attachment_count INTEGER, internal_date INTEGER, message_id TEXT, preview TEXT, "
                               + "mime_type TEXT)");

                    db.execSQL("DROP TABLE IF EXISTS headers");
                    db.execSQL("CREATE TABLE headers (id INTEGER PRIMARY KEY, message_id INTEGER, name TEXT, value TEXT)");
                    db.execSQL("CREATE INDEX IF NOT EXISTS header_folder ON headers (message_id)");

                    db.execSQL("CREATE INDEX IF NOT EXISTS msg_uid ON messages (uid, folder_id)");
                    db.execSQL("DROP INDEX IF EXISTS msg_folder_id");
                    db.execSQL("DROP INDEX IF EXISTS msg_folder_id_date");
                    db.execSQL("CREATE INDEX IF NOT EXISTS msg_folder_id_deleted_date ON messages (folder_id,deleted,internal_date)");
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
                }
                else
                {
                    // in the case that we're starting out at 29 or newer, run all the needed updates

                    if (db.getVersion() < 30)
                    {
                        try
                        {
                            db.execSQL("ALTER TABLE messages ADD deleted INTEGER default 0");
                        }
                        catch (SQLiteException e)
                        {
                            if (! e.toString().startsWith("duplicate column name: deleted"))
                            {
                                throw e;
                            }
                        }
                    }
                    if (db.getVersion() < 31)
                    {
                        db.execSQL("DROP INDEX IF EXISTS msg_folder_id_date");
                        db.execSQL("CREATE INDEX IF NOT EXISTS msg_folder_id_deleted_date ON messages (folder_id,deleted,internal_date)");
                    }
                    if (db.getVersion() < 32)
                    {
                        db.execSQL("UPDATE messages SET deleted = 1 WHERE flags LIKE '%DELETED%'");
                    }
                    if (db.getVersion() < 33)
                    {

                        try
                        {
                            db.execSQL("ALTER TABLE messages ADD preview TEXT");
                        }
                        catch (SQLiteException e)
                        {
                            if (! e.toString().startsWith("duplicate column name: preview"))
                            {
                                throw e;
                            }
                        }

                    }
                    if (db.getVersion() < 34)
                    {
                        try
                        {
                            db.execSQL("ALTER TABLE folders ADD flagged_count INTEGER default 0");
                        }
                        catch (SQLiteException e)
                        {
                            if (! e.getMessage().startsWith("duplicate column name: flagged_count"))
                            {
                                throw e;
                            }
                        }
                    }
                    if (db.getVersion() < 35)
                    {
                        try
                        {
                            db.execSQL("update messages set flags = replace(flags, 'X_NO_SEEN_INFO', 'X_BAD_FLAG')");
                        }
                        catch (SQLiteException e)
                        {
                            Log.e(K9.LOG_TAG, "Unable to get rid of obsolete flag X_NO_SEEN_INFO", e);
                        }
                    }
                    if (db.getVersion() < 36)
                    {
                        try
                        {
                            db.execSQL("ALTER TABLE attachments ADD content_id TEXT");
                        }
                        catch (SQLiteException e)
                        {
                            Log.e(K9.LOG_TAG, "Unable to add content_id column to attachments");
                        }
                    }
                    if (db.getVersion() < 37)
                    {
                        try
                        {
                            db.execSQL("ALTER TABLE attachments ADD content_disposition TEXT");
                        }
                        catch (SQLiteException e)
                        {
                            Log.e(K9.LOG_TAG, "Unable to add content_disposition column to attachments");
                        }
                    }

                    // Database version 38 is solely to prune cached attachments now that we clear them better
                    if (db.getVersion() < 39)
                    {
                        try
                        {
                            db.execSQL("DELETE FROM headers WHERE id in (SELECT headers.id FROM headers LEFT JOIN messages ON headers.message_id = messages.id WHERE messages.id IS NULL)");
                        }
                        catch (SQLiteException e)
                        {
                            Log.e(K9.LOG_TAG, "Unable to remove extra header data from the database");
                        }
                    }

                    // V40: Store the MIME type for a message.
                    if (db.getVersion() < 40)
                    {
                        try
                        {
                            db.execSQL("ALTER TABLE messages ADD mime_type TEXT");
                        }
                        catch (SQLiteException e)
                        {
                            Log.e(K9.LOG_TAG, "Unable to add mime_type column to messages");
                        }
                    }

                    if (db.getVersion() < 41)
                    {
                        try
                        {
                            db.execSQL("ALTER TABLE folders ADD integrate INTEGER");
                            db.execSQL("ALTER TABLE folders ADD top_group INTEGER");
                            db.execSQL("ALTER TABLE folders ADD poll_class TEXT");
                            db.execSQL("ALTER TABLE folders ADD push_class TEXT");
                            db.execSQL("ALTER TABLE folders ADD display_class TEXT");
                        }
                        catch (SQLiteException e)
                        {
                            if (! e.getMessage().startsWith("duplicate column name:"))
                            {
                                throw e;
                            }
                        }
                        Cursor cursor = null;

                        try
                        {

                            SharedPreferences prefs = Preferences.getPreferences(mApplication).getPreferences();
                            SharedPreferences.Editor editor = prefs.edit();
                            cursor = db.rawQuery("SELECT id, name FROM folders", null);
                            while (cursor.moveToNext())
                            {
                                try
                                {
                                    int id = cursor.getInt(0);
                                    String name = cursor.getString(1);
                                    update41Metadata(db, prefs, editor, id, name);
                                }
                                catch (Exception e)
                                {
                                    Log.e(K9.LOG_TAG," error trying to ugpgrade a folder class: " +e);
                                }
                            }
                            editor.commit();


                        }


                        catch (SQLiteException e)
                        {
                            Log.e(K9.LOG_TAG, "Exception while upgrading database to v41. folder classes may have vanished "+e);

                        }
                        finally
                        {
                            if (cursor != null)
                            {
                                cursor.close();
                            }
                        }
                    }
                }
            }

            catch (SQLiteException e)
            {
                Log.e(K9.LOG_TAG, "Exception while upgrading database. Resetting the DB to v0");
                db.setVersion(0);
                throw new Error("Database upgrade failed! Resetting your DB version to 0 to force a full schema recreation.");
            }



            db.setVersion(DB_VERSION);

            if (db.getVersion() != DB_VERSION)
            {
                throw new Error("Database upgrade failed!");
            }

            // Unless we're blowing away the whole data store, there's no reason to prune attachments
            // every time the user upgrades. it'll just cost them money and pain.
            // try
            //{
            //        pruneCachedAttachments(true);
            //}
            //catch (Exception me)
            //{
            //   Log.e(K9.LOG_TAG, "Exception while force pruning attachments during DB update", me);
            //}
        }


        private void update41Metadata(final SQLiteDatabase  db, SharedPreferences prefs, SharedPreferences.Editor editor, int id, String name)
        {


            Folder.FolderClass displayClass = Folder.FolderClass.NO_CLASS;
            Folder.FolderClass syncClass = Folder.FolderClass.INHERITED;
            Folder.FolderClass pushClass = Folder.FolderClass.SECOND_CLASS;
            boolean inTopGroup = false;
            boolean integrate = false;
            if (K9.INBOX.equals(name))
            {
                displayClass = Folder.FolderClass.FIRST_CLASS;
                syncClass =  Folder.FolderClass.FIRST_CLASS;
                pushClass =  Folder.FolderClass.FIRST_CLASS;
                inTopGroup = true;
                integrate = true;
            }

            try
            {
                displayClass = Folder.FolderClass.valueOf(prefs.getString(uUid + "."+ name + ".displayMode", displayClass.name()));
                syncClass = Folder.FolderClass.valueOf(prefs.getString(uUid + "."+ name + ".syncMode", syncClass.name()));
                pushClass = Folder.FolderClass.valueOf(prefs.getString(uUid + "."+ name + ".pushMode", pushClass.name()));
                inTopGroup = prefs.getBoolean(uUid + "."+ name + ".inTopGroup",inTopGroup);
                integrate = prefs.getBoolean(uUid + "."+ name + ".integrate", integrate);
            }
            catch (Exception e)
            {
                Log.e(K9.LOG_TAG," Throwing away an error while trying to upgrade folder metadata: "+e);
            }

            if (displayClass == Folder.FolderClass.NONE)
            {
                displayClass = Folder.FolderClass.NO_CLASS;
            }
            if (syncClass == Folder.FolderClass.NONE)
            {
                syncClass = Folder.FolderClass.INHERITED;
            }
            if (pushClass == Folder.FolderClass.NONE)
            {
                pushClass = Folder.FolderClass.INHERITED;
            }

            db.execSQL("UPDATE folders SET integrate = ?, top_group = ?, poll_class=?, push_class =?, display_class = ? WHERE id = ?",
                       new Object[] { integrate, inTopGroup,syncClass,pushClass,displayClass, id });

            // now that we managed to update this folder, obliterate the old data

            editor.remove(uUid+"."+name + ".displayMode");
            editor.remove(uUid+"."+name + ".syncMode");
            editor.remove(uUid+"."+name + ".pushMode");
            editor.remove(uUid+"."+name + ".inTopGroup");
            editor.remove(uUid+"."+name + ".integrate");


        }
    }


    public long getSize() throws UnavailableStorageException
    {

        final StorageManager storageManager = StorageManager.getInstance(mApplication);

        final File attachmentDirectory = storageManager.getAttachmentDirectory(uUid,
                                         database.getStorageProviderId());

        return database.execute(false, new DbCallback<Long>()
        {
            @Override
            public Long doDbWork(final SQLiteDatabase db)
            {
                final File[] files = attachmentDirectory.listFiles();
                long attachmentLength = 0;
                for (File file : files)
                {
                    if (file.exists())
                    {
                        attachmentLength += file.length();
                    }
                }

                final File dbFile = storageManager.getDatabase(uUid, database.getStorageProviderId());
                return dbFile.length() + attachmentLength;
            }
        });
    }

    public void compact() throws MessagingException
    {
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "Before compaction size = " + getSize());

        database.execute(false, new DbCallback<Void>()
        {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws WrappedException
            {
                db.execSQL("VACUUM");
                return null;
            }
        });
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "After compaction size = " + getSize());
    }


    public void clear() throws MessagingException
    {
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "Before prune size = " + getSize());

        pruneCachedAttachments(true);
        if (K9.DEBUG)
        {
            Log.i(K9.LOG_TAG, "After prune / before compaction size = " + getSize());

            Log.i(K9.LOG_TAG, "Before clear folder count = " + getFolderCount());
            Log.i(K9.LOG_TAG, "Before clear message count = " + getMessageCount());

            Log.i(K9.LOG_TAG, "After prune / before clear size = " + getSize());
        }
        // don't delete messages that are Local, since there is no copy on the server.
        // Don't delete deleted messages.  They are essentially placeholders for UIDs of messages that have
        // been deleted locally.  They take up insignificant space
        database.execute(false, new DbCallback<Void>()
        {
            @Override
            public Void doDbWork(final SQLiteDatabase db)
            {
                db.execSQL("DELETE FROM messages WHERE deleted = 0 and uid not like 'Local%'");
                db.execSQL("update folders set flagged_count = 0, unread_count = 0");
                return null;
            }
        });

        compact();

        if (K9.DEBUG)
        {
            Log.i(K9.LOG_TAG, "After clear message count = " + getMessageCount());

            Log.i(K9.LOG_TAG, "After clear size = " + getSize());
        }
    }

    public int getMessageCount() throws MessagingException
    {
        return database.execute(false, new DbCallback<Integer>()
        {
            @Override
            public Integer doDbWork(final SQLiteDatabase db)
            {
                Cursor cursor = null;
                try
                {
                    cursor = db.rawQuery("SELECT COUNT(*) FROM messages", null);
                    cursor.moveToFirst();
                    return cursor.getInt(0);   // message count



                }
                finally
                {
                    if (cursor != null)
                    {
                        cursor.close();
                    }
                }
            }
        });
    }



    public void getMessageCounts(final AccountStats stats) throws MessagingException
    {
        final Account.FolderMode displayMode = mAccount.getFolderDisplayMode();

        database.execute(false, new DbCallback<Integer>()
        {
            @Override
            public Integer doDbWork(final SQLiteDatabase db)
            {
                Cursor cursor = null;
                try
                {
                    String baseQuery = "SELECT SUM(unread_count), SUM(flagged_count) FROM folders WHERE ( name != ? AND name != ? AND name != ? AND name != ? AND name != ? ) ";



                    if (displayMode == Account.FolderMode.NONE)
                    {
                        cursor = db.rawQuery(baseQuery+ "AND (name = ? )", new String[]
                                             {
                                                 mAccount.getTrashFolderName(),
                                                 mAccount.getDraftsFolderName(),
                                                 mAccount.getSpamFolderName(),
                                                 mAccount.getOutboxFolderName(),
                                                 mAccount.getSentFolderName(),


                                                 K9.INBOX
                                             });
                    }
                    else if (displayMode == Account.FolderMode.FIRST_CLASS )
                    {
                        cursor = db.rawQuery(baseQuery + " AND ( name = ? OR display_class = ?)", new String[]
                                             {

                                                 mAccount.getTrashFolderName(),
                                                 mAccount.getDraftsFolderName(),
                                                 mAccount.getSpamFolderName(),
                                                 mAccount.getOutboxFolderName(),
                                                 mAccount.getSentFolderName(),
                                                 K9.INBOX, Folder.FolderClass.FIRST_CLASS.name()
                                             });


                    }
                    else if (displayMode == Account.FolderMode.FIRST_AND_SECOND_CLASS)
                    {
                        cursor = db.rawQuery(baseQuery + " AND ( name = ? OR display_class = ? OR display_class = ? )", new String[]
                                             {
                                                 mAccount.getTrashFolderName(),
                                                 mAccount.getDraftsFolderName(),
                                                 mAccount.getSpamFolderName(),
                                                 mAccount.getOutboxFolderName(),
                                                 mAccount.getSentFolderName(),
                                                 K9.INBOX, Folder.FolderClass.FIRST_CLASS.name(), Folder.FolderClass.SECOND_CLASS.name()
                                             });
                    }
                    else if (displayMode == Account.FolderMode.NOT_SECOND_CLASS)
                    {
                        cursor = db.rawQuery(baseQuery + " AND ( name = ? OR display_class != ?)", new String[]
                                             {

                                                 mAccount.getTrashFolderName(),
                                                 mAccount.getDraftsFolderName(),
                                                 mAccount.getSpamFolderName(),
                                                 mAccount.getOutboxFolderName(),
                                                 mAccount.getSentFolderName(),
                                                 K9.INBOX, Folder.FolderClass.SECOND_CLASS.name()
                                             });
                    }
                    else if (displayMode == Account.FolderMode.ALL)
                    {
                        cursor = db.rawQuery(baseQuery,  new String[]
                                             {

                                                 mAccount.getTrashFolderName(),
                                                 mAccount.getDraftsFolderName(),
                                                 mAccount.getSpamFolderName(),
                                                 mAccount.getOutboxFolderName(),
                                                 mAccount.getSentFolderName()

                                             });
                    }
                    else
                    {
                        Log.e(K9.LOG_TAG, "asked to compute account statistics for an impossible folder mode " + displayMode);
                        stats.unreadMessageCount = 0;
                        stats.flaggedMessageCount = 0;
                        return null;
                    }

                    cursor.moveToFirst();
                    stats.unreadMessageCount = cursor.getInt(0);
                    stats.flaggedMessageCount = cursor.getInt(1);
                    return null;
                }
                finally
                {
                    if (cursor != null)
                    {
                        cursor.close();
                    }
                }
            }
        });
    }


    public int getFolderCount() throws MessagingException
    {
        return database.execute(false, new DbCallback<Integer>()
        {
            @Override
            public Integer doDbWork(final SQLiteDatabase db)
            {
                Cursor cursor = null;
                try
                {
                    cursor = db.rawQuery("SELECT COUNT(*) FROM folders", null);
                    cursor.moveToFirst();
                    return cursor.getInt(0);        // folder count
                }
                finally
                {
                    if (cursor != null)
                    {
                        cursor.close();
                    }
                }
            }
        });
    }

    @Override
    public LocalFolder getFolder(String name)
    {
        return new LocalFolder(name);
    }

    // TODO this takes about 260-300ms, seems slow.
    @Override
    public List<? extends Folder> getPersonalNamespaces(boolean forceListAll) throws MessagingException
    {
        final List<LocalFolder> folders = new LinkedList<LocalFolder>();
        try
        {
            database.execute(false, new DbCallback<List<? extends Folder>>()
            {
                @Override
                public List<? extends Folder> doDbWork(final SQLiteDatabase db) throws WrappedException
                {
                    Cursor cursor = null;

                    try
                    {
                        cursor = db.rawQuery("SELECT id, name, unread_count, visible_limit, last_updated, status, push_state, last_pushed, flagged_count, integrate, top_group, poll_class, push_class, display_class FROM folders ORDER BY name ASC", null);
                        while (cursor.moveToNext())
                        {
                            LocalFolder folder = new LocalFolder(cursor.getString(1));
                            folder.open(cursor.getInt(0), cursor.getString(1), cursor.getInt(2), cursor.getInt(3), cursor.getLong(4), cursor.getString(5), cursor.getString(6), cursor.getLong(7), cursor.getInt(8), cursor.getInt(9), cursor.getInt(10), cursor.getString(11), cursor.getString(12), cursor.getString(13));

                            folders.add(folder);
                        }
                        return folders;
                    }
                    catch (MessagingException e)
                    {
                        throw new WrappedException(e);
                    }
                    finally
                    {
                        if (cursor != null)
                        {
                            cursor.close();
                        }
                    }
                }
            });
        }
        catch (WrappedException e)
        {
            throw (MessagingException) e.getCause();
        }
        return folders;
    }

    @Override
    public void checkSettings() throws MessagingException
    {
    }

    public void delete() throws UnavailableStorageException
    {
        database.delete();
    }

    public void recreate() throws UnavailableStorageException
    {
        database.recreate();
    }

    public void pruneCachedAttachments() throws MessagingException
    {
        pruneCachedAttachments(false);
    }

    /**
     * Deletes all cached attachments for the entire store.
     * @param force
     * @throws com.fsck.k9.mail.MessagingException
     */
    private void pruneCachedAttachments(final boolean force) throws MessagingException
    {
        database.execute(false, new DbCallback<Void>()
        {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws WrappedException
            {
                if (force)
                {
                    ContentValues cv = new ContentValues();
                    cv.putNull("content_uri");
                    db.update("attachments", cv, null, null);
                }
                final StorageManager storageManager = StorageManager.getInstance(mApplication);
                File[] files = storageManager.getAttachmentDirectory(uUid, database.getStorageProviderId()).listFiles();
                for (File file : files)
                {
                    if (file.exists())
                    {
                        if (!force)
                        {
                            Cursor cursor = null;
                            try
                            {
                                cursor = db.query(
                                             "attachments",
                                             new String[] { "store_data" },
                                             "id = ?",
                                             new String[] { file.getName() },
                                             null,
                                             null,
                                             null);
                                if (cursor.moveToNext())
                                {
                                    if (cursor.getString(0) == null)
                                    {
                                        if (K9.DEBUG)
                                            Log.d(K9.LOG_TAG, "Attachment " + file.getAbsolutePath() + " has no store data, not deleting");
                                        /*
                                         * If the attachment has no store data it is not recoverable, so
                                         * we won't delete it.
                                         */
                                        continue;
                                    }
                                }
                            }
                            finally
                            {
                                if (cursor != null)
                                {
                                    cursor.close();
                                }
                            }
                        }
                        if (!force)
                        {
                            try
                            {
                                ContentValues cv = new ContentValues();
                                cv.putNull("content_uri");
                                db.update("attachments", cv, "id = ?", new String[] { file.getName() });
                            }
                            catch (Exception e)
                            {
                                /*
                                 * If the row has gone away before we got to mark it not-downloaded that's
                                 * okay.
                                 */
                            }
                        }
                        if (!file.delete())
                        {
                            file.deleteOnExit();
                        }
                    }
                }
                return null;
            }
        });
    }

    public void resetVisibleLimits() throws UnavailableStorageException
    {
        resetVisibleLimits(mAccount.getDisplayCount());
    }

    public void resetVisibleLimits(int visibleLimit) throws UnavailableStorageException
    {
        final ContentValues cv = new ContentValues();
        cv.put("visible_limit", Integer.toString(visibleLimit));
        database.execute(false, new DbCallback<Void>()
        {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws WrappedException
            {
                db.update("folders", cv, null, null);
                return null;
            }
        });
    }

    public ArrayList<PendingCommand> getPendingCommands() throws UnavailableStorageException
    {
        return database.execute(false, new DbCallback<ArrayList<PendingCommand>>()
        {
            @Override
            public ArrayList<PendingCommand> doDbWork(final SQLiteDatabase db) throws WrappedException
            {
                Cursor cursor = null;
                try
                {
                    cursor = db.query("pending_commands",
                                      new String[] { "id", "command", "arguments" },
                                      null,
                                      null,
                                      null,
                                      null,
                                      "id ASC");
                    ArrayList<PendingCommand> commands = new ArrayList<PendingCommand>();
                    while (cursor.moveToNext())
                    {
                        PendingCommand command = new PendingCommand();
                        command.mId = cursor.getLong(0);
                        command.command = cursor.getString(1);
                        String arguments = cursor.getString(2);
                        command.arguments = arguments.split(",");
                        for (int i = 0; i < command.arguments.length; i++)
                        {
                            command.arguments[i] = Utility.fastUrlDecode(command.arguments[i]);
                        }
                        commands.add(command);
                    }
                    return commands;
                }
                finally
                {
                    if (cursor != null)
                    {
                        cursor.close();
                    }
                }
            }
        });
    }

    public void addPendingCommand(PendingCommand command) throws UnavailableStorageException
    {
        try
        {
            for (int i = 0; i < command.arguments.length; i++)
            {
                command.arguments[i] = URLEncoder.encode(command.arguments[i], "UTF-8");
            }
            final ContentValues cv = new ContentValues();
            cv.put("command", command.command);
            cv.put("arguments", Utility.combine(command.arguments, ','));
            database.execute(false, new DbCallback<Void>()
            {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws WrappedException
                {
                    db.insert("pending_commands", "command", cv);
                    return null;
                }
            });
        }
        catch (UnsupportedEncodingException usee)
        {
            throw new Error("Aparently UTF-8 has been lost to the annals of history.");
        }
    }

    public void removePendingCommand(final PendingCommand command) throws UnavailableStorageException
    {
        database.execute(false, new DbCallback<Void>()
        {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws WrappedException
            {
                db.delete("pending_commands", "id = ?", new String[] { Long.toString(command.mId) });
                return null;
            }
        });
    }

    public void removePendingCommands() throws UnavailableStorageException
    {
        database.execute(false, new DbCallback<Void>()
        {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws WrappedException
            {
                db.delete("pending_commands", null, null);
                return null;
            }
        });
    }

    public static class PendingCommand
    {
        private long mId;
        public String command;
        public String[] arguments;

        @Override
        public String toString()
        {
            StringBuffer sb = new StringBuffer();
            sb.append(command);
            sb.append(": ");
            for (String argument : arguments)
            {
                sb.append(", ");
                sb.append(argument);
                //sb.append("\n");
            }
            return sb.toString();
        }
    }

    @Override
    public boolean isMoveCapable()
    {
        return true;
    }

    @Override
    public boolean isCopyCapable()
    {
        return true;
    }

    public Message[] searchForMessages(MessageRetrievalListener listener, String[] queryFields, String queryString,
                                       List<LocalFolder> folders, Message[] messages, final Flag[] requiredFlags, final Flag[] forbiddenFlags) throws MessagingException
    {
        List<String> args = new LinkedList<String>();

        StringBuilder whereClause = new StringBuilder();
        if (queryString != null && queryString.length() > 0)
        {
            boolean anyAdded = false;
            String likeString = "%"+queryString+"%";
            whereClause.append(" AND (");
            for (String queryField : queryFields)
            {

                if (anyAdded)
                {
                    whereClause.append(" OR ");
                }
                whereClause.append(queryField).append(" LIKE ? ");
                args.add(likeString);
                anyAdded = true;
            }


            whereClause.append(" )");
        }
        if (folders != null && folders.size() > 0)
        {
            whereClause.append(" AND folder_id in (");
            boolean anyAdded = false;
            for (LocalFolder folder : folders)
            {
                if (anyAdded)
                {
                    whereClause.append(",");
                }
                anyAdded = true;
                whereClause.append("?");
                args.add(Long.toString(folder.getId()));
            }
            whereClause.append(" )");
        }
        if (messages != null && messages.length > 0)
        {
            whereClause.append(" AND ( ");
            boolean anyAdded = false;
            for (Message message : messages)
            {
                if (anyAdded)
                {
                    whereClause.append(" OR ");
                }
                anyAdded = true;
                whereClause.append(" ( uid = ? AND folder_id = ? ) ");
                args.add(message.getUid());
                args.add(Long.toString(((LocalFolder)message.getFolder()).getId()));
            }
            whereClause.append(" )");
        }
        if (forbiddenFlags != null && forbiddenFlags.length > 0)
        {
            whereClause.append(" AND (");
            boolean anyAdded = false;
            for (Flag flag : forbiddenFlags)
            {
                if (anyAdded)
                {
                    whereClause.append(" AND ");
                }
                anyAdded = true;
                whereClause.append(" flags NOT LIKE ?");

                args.add("%" + flag.toString() + "%");
            }
            whereClause.append(" )");
        }
        if (requiredFlags != null && requiredFlags.length > 0)
        {
            whereClause.append(" AND (");
            boolean anyAdded = false;
            for (Flag flag : requiredFlags)
            {
                if (anyAdded)
                {
                    whereClause.append(" OR ");
                }
                anyAdded = true;
                whereClause.append(" flags LIKE ?");

                args.add("%" + flag.toString() + "%");
            }
            whereClause.append(" )");
        }

        if (K9.DEBUG)
        {
            Log.v(K9.LOG_TAG, "whereClause = " + whereClause.toString());
            Log.v(K9.LOG_TAG, "args = " + args);
        }
        return getMessages(
                   listener,
                   null,
                   "SELECT "
                   + GET_MESSAGES_COLS
                   + "FROM messages WHERE deleted = 0 " + whereClause.toString() + " ORDER BY date DESC"
                   , args.toArray(EMPTY_STRING_ARRAY)
               );
    }
    /*
     * Given a query string, actually do the query for the messages and
     * call the MessageRetrievalListener for each one
     */
    private Message[] getMessages(
        final MessageRetrievalListener listener,
        final LocalFolder folder,
        final String queryString, final String[] placeHolders
    ) throws MessagingException
    {
        final ArrayList<LocalMessage> messages = new ArrayList<LocalMessage>();
        final int j = database.execute(false, new DbCallback<Integer>()
        {
            @Override
            public Integer doDbWork(final SQLiteDatabase db) throws WrappedException
            {
                Cursor cursor = null;
                int i = 0;
                try
                {
                    cursor = db.rawQuery(queryString + " LIMIT 10", placeHolders);

                    while (cursor.moveToNext())
                    {
                        LocalMessage message = new LocalMessage(null, folder);
                        message.populateFromGetMessageCursor(cursor);

                        messages.add(message);
                        if (listener != null)
                        {
                            listener.messageFinished(message, i, -1);
                        }
                        i++;
                    }
                    cursor.close();
                    cursor = db.rawQuery(queryString + " LIMIT -1 OFFSET 10", placeHolders);

                    while (cursor.moveToNext())
                    {
                        LocalMessage message = new LocalMessage(null, folder);
                        message.populateFromGetMessageCursor(cursor);

                        messages.add(message);
                        if (listener != null)
                        {
                            listener.messageFinished(message, i, -1);
                        }
                        i++;
                    }
                }
                catch (Exception e)
                {
                    Log.d(K9.LOG_TAG,"Got an exception "+e);
                }
                finally
                {
                    if (cursor != null)
                    {
                        cursor.close();
                    }
                }
                return i;
            }
        });
        if (listener != null)
        {
            listener.messagesFinished(j);
        }

        return messages.toArray(EMPTY_MESSAGE_ARRAY);

    }

    public String getAttachmentType(final String attachmentId) throws UnavailableStorageException
    {
        return database.execute(false, new DbCallback<String>()
        {
            @Override
            public String doDbWork(final SQLiteDatabase db) throws WrappedException
            {
                Cursor cursor = null;
                try
                {
                    cursor = db.query(
                                 "attachments",
                                 new String[] { "mime_type", "name" },
                                 "id = ?",
                                 new String[] { attachmentId },
                                 null,
                                 null,
                                 null);
                    cursor.moveToFirst();
                    String type = cursor.getString(0);
                    String name = cursor.getString(1);
                    cursor.close();

                    if (MimeUtility.DEFAULT_ATTACHMENT_MIME_TYPE.equals(type))
                    {
                        type = MimeUtility.getMimeTypeByExtension(name);
                    }
                    return type;
                }
                finally
                {
                    if (cursor != null)
                    {
                        cursor.close();
                    }
                }
            }
        });
    }

    public AttachmentInfo getAttachmentInfo(final String attachmentId) throws UnavailableStorageException
    {
        return database.execute(false, new DbCallback<AttachmentInfo>()
        {
            @Override
            public AttachmentInfo doDbWork(final SQLiteDatabase db) throws WrappedException
            {
                String name;
                int size;
                Cursor cursor = null;
                try
                {
                    cursor = db.query(
                                 "attachments",
                                 new String[] { "name", "size" },
                                 "id = ?",
                                 new String[] { attachmentId },
                                 null,
                                 null,
                                 null);
                    if (!cursor.moveToFirst())
                    {
                        return null;
                    }
                    name = cursor.getString(0);
                    size = cursor.getInt(1);
                    final AttachmentInfo attachmentInfo = new AttachmentInfo();
                    attachmentInfo.name = name;
                    attachmentInfo.size = size;
                    return attachmentInfo;
                }
                finally
                {
                    if (cursor != null)
                    {
                        cursor.close();
                    }
                }
            }
        });
    }

    public static class AttachmentInfo
    {
        public String name;
        public int size;
    }

    public class LocalFolder extends Folder implements Serializable
    {
        private String mName = null;
        private long mFolderId = -1;
        private int mUnreadMessageCount = -1;
        private int mFlaggedMessageCount = -1;
        private int mVisibleLimit = -1;
        private FolderClass mDisplayClass = FolderClass.NO_CLASS;
        private FolderClass mSyncClass = FolderClass.INHERITED;
        private FolderClass mPushClass = FolderClass.SECOND_CLASS;
        private boolean mInTopGroup = false;
        private String mPushState = null;
        private boolean mIntegrate = false;
        // mLastUid is used during syncs. It holds the highest UID within the local folder so we
        // know whether or not an unread message added to the local folder is actually "new" or not.
        private Integer mLastUid = null;

        public LocalFolder(String name)
        {
            super(LocalStore.this.mAccount);
            this.mName = name;

            if (K9.INBOX.equals(getName()))
            {
                mSyncClass =  FolderClass.FIRST_CLASS;
                mPushClass =  FolderClass.FIRST_CLASS;
                mInTopGroup = true;
            }


        }

        public LocalFolder(long id)
        {
            super(LocalStore.this.mAccount);
            this.mFolderId = id;
        }

        public long getId()
        {
            return mFolderId;
        }

        @Override
        public void open(final OpenMode mode) throws MessagingException
        {
            if (isOpen())
            {
                return;
            }
            try
            {
                database.execute(false, new DbCallback<Void>()
                {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException
                    {
                        Cursor cursor = null;
                        try
                        {
                            String baseQuery =
                                "SELECT id, name, unread_count, visible_limit, last_updated, status, push_state, last_pushed, flagged_count, integrate, top_group, poll_class, push_class, display_class FROM folders ";

                            if (mName != null)
                            {
                                cursor = db.rawQuery(baseQuery + "where folders.name = ?", new String[] { mName });
                            }
                            else
                            {
                                cursor = db.rawQuery(baseQuery + "where folders.id = ?", new String[] { Long.toString(mFolderId) });
                            }

                            if (cursor.moveToFirst())
                            {
                                int folderId = cursor.getInt(0);
                                if (folderId > 0)
                                {
                                    open(folderId, cursor.getString(1), cursor.getInt(2), cursor.getInt(3), cursor.getLong(4), cursor.getString(5), cursor.getString(6), cursor.getLong(7), cursor.getInt(8), cursor.getInt(9), cursor.getInt(10), cursor.getString(11), cursor.getString(12), cursor.getString(13));
                                }
                            }
                            else
                            {
                                Log.w(K9.LOG_TAG, "Creating folder " + getName() + " with existing id " + getId());
                                create(FolderType.HOLDS_MESSAGES);
                                open(mode);
                            }
                        }
                        catch (MessagingException e)
                        {
                            throw new WrappedException(e);
                        }
                        finally
                        {
                            if (cursor != null)
                            {
                                cursor.close();
                            }
                        }
                        return null;
                    }
                });
            }
            catch (WrappedException e)
            {
                throw (MessagingException) e.getCause();
            }
        }

        private void open(int id, String name, int unreadCount, int visibleLimit, long lastChecked, String status, String pushState, long lastPushed, int flaggedCount, int integrate, int topGroup, String syncClass, String pushClass, String displayClass) throws MessagingException
        {
            mFolderId = id;
            mName = name;
            mUnreadMessageCount = unreadCount;
            mVisibleLimit = visibleLimit;
            mPushState = pushState;
            mFlaggedMessageCount = flaggedCount;
            super.setStatus(status);
            // Only want to set the local variable stored in the super class.  This class
            // does a DB update on setLastChecked
            super.setLastChecked(lastChecked);
            super.setLastPush(lastPushed);
            mInTopGroup = topGroup ==1  ? true : false;
            mIntegrate = integrate == 1 ? true : false;
            mDisplayClass = Folder.FolderClass.valueOf(displayClass == null?"NO_CLASS":displayClass);
            mPushClass = Folder.FolderClass.valueOf(pushClass == null?"NO_CLASS":pushClass);
            mSyncClass = Folder.FolderClass.valueOf(syncClass == null?"NO_CLASS":syncClass);

        }

        @Override
        public boolean isOpen()
        {
            return (mFolderId != -1 && mName != null);
        }

        @Override
        public OpenMode getMode()
        {
            return OpenMode.READ_WRITE;
        }

        @Override
        public String getName()
        {
            return mName;
        }

        @Override
        public boolean exists() throws MessagingException
        {
            return database.execute(false, new DbCallback<Boolean>()
            {
                @Override
                public Boolean doDbWork(final SQLiteDatabase db) throws WrappedException
                {
                    Cursor cursor = null;
                    try
                    {
                        cursor = db.rawQuery("SELECT id FROM folders "
                                             + "where folders.name = ?", new String[] { LocalFolder.this
                                                     .getName()
                                                                                      });
                        if (cursor.moveToFirst())
                        {
                            int folderId = cursor.getInt(0);
                            return (folderId > 0);
                        }
                        else
                        {
                            return false;
                        }
                    }
                    finally
                    {
                        if (cursor != null)
                        {
                            cursor.close();
                        }
                    }
                }
            });
        }

        @Override
        public boolean create(FolderType type) throws MessagingException
        {
            return create(type, mAccount.getDisplayCount());
        }

        @Override
        public boolean create(FolderType type, final int visibleLimit) throws MessagingException
        {
            if (exists())
            {
                throw new MessagingException("Folder " + mName + " already exists.");
            }
            database.execute(false, new DbCallback<Void>()
            {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws WrappedException
                {
                    db.execSQL("INSERT INTO folders (name, visible_limit) VALUES (?, ?)", new Object[]
                               {
                                   mName,
                                   visibleLimit
                               });


                    return null;
                }
            });

            // When created, special folders should always be displayed
            // inbox should be integrated
            // and the inbox and drafts folders should be syncced by default
            if (mAccount.isSpecialFolder(mName))
            {
                LocalFolder f = new LocalFolder(mName);
                f.open(OpenMode.READ_WRITE);
                f.setInTopGroup(true);
                f.setDisplayClass(FolderClass.FIRST_CLASS);
                if (mName.equalsIgnoreCase(K9.INBOX))
                {
                    f.setIntegrate(true);
                    f.setPushClass(FolderClass.FIRST_CLASS);
                }
                else
                {
                    f.setPushClass(FolderClass.INHERITED);

                }
                if ( mName.equalsIgnoreCase(K9.INBOX) ||
                        mName.equalsIgnoreCase(mAccount.getDraftsFolderName()) )
                {
                    f.setSyncClass(FolderClass.FIRST_CLASS);
                }
                else
                {
                    f.setSyncClass(FolderClass.NO_CLASS);
                }
            }

            return true;
        }

        @Override
        public void close()
        {
            mFolderId = -1;
        }

        @Override
        public int getMessageCount() throws MessagingException
        {
            try
            {
                return database.execute(false, new DbCallback<Integer>()
                {
                    @Override
                    public Integer doDbWork(final SQLiteDatabase db) throws WrappedException
                    {
                        try
                        {
                            open(OpenMode.READ_WRITE);
                        }
                        catch (MessagingException e)
                        {
                            throw new WrappedException(e);
                        }
                        Cursor cursor = null;
                        try
                        {
                            cursor = db.rawQuery("SELECT COUNT(*) FROM messages WHERE folder_id = ?",
                                                 new String[]
                                                 {
                                                     Long.toString(mFolderId)
                                                 });
                            cursor.moveToFirst();
                            return cursor.getInt(0);   //messagecount
                        }
                        finally
                        {
                            if (cursor != null)
                            {
                                cursor.close();
                            }
                        }
                    }
                });
            }
            catch (WrappedException e)
            {
                throw (MessagingException) e.getCause();
            }
        }

        @Override
        public int getUnreadMessageCount() throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            return mUnreadMessageCount;
        }

        @Override
        public int getFlaggedMessageCount() throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            return mFlaggedMessageCount;
        }

        public void setUnreadMessageCount(final int unreadMessageCount) throws MessagingException
        {
            mUnreadMessageCount = Math.max(0, unreadMessageCount);
            updateFolderColumn( "unread_count", mUnreadMessageCount);
        }

        public void setFlaggedMessageCount(final int flaggedMessageCount) throws MessagingException
        {
            mFlaggedMessageCount = Math.max(0, flaggedMessageCount);
            updateFolderColumn( "flagged_count", mFlaggedMessageCount);
        }

        @Override
        public void setLastChecked(final long lastChecked) throws MessagingException
        {
            try
            {
                open(OpenMode.READ_WRITE);
                LocalFolder.super.setLastChecked(lastChecked);
            }
            catch (MessagingException e)
            {
                throw new WrappedException(e);
            }
            updateFolderColumn( "last_updated", lastChecked);
        }

        @Override
        public void setLastPush(final long lastChecked) throws MessagingException
        {
            try
            {
                open(OpenMode.READ_WRITE);
                LocalFolder.super.setLastPush(lastChecked);
            }
            catch (MessagingException e)
            {
                throw new WrappedException(e);
            }
            updateFolderColumn( "last_pushed", lastChecked);
        }

        public int getVisibleLimit() throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            return mVisibleLimit;
        }

        public void purgeToVisibleLimit(MessageRemovalListener listener) throws MessagingException
        {
            if ( mVisibleLimit == 0)
            {
                return ;
            }
            open(OpenMode.READ_WRITE);
            Message[] messages = getMessages(null, false);
            for (int i = mVisibleLimit; i < messages.length; i++)
            {
                if (listener != null)
                {
                    listener.messageRemoved(messages[i]);
                }
                messages[i].destroy();

            }
        }


        public void setVisibleLimit(final int visibleLimit) throws MessagingException
        {
            mVisibleLimit = visibleLimit;
            updateFolderColumn( "visible_limit", mVisibleLimit);
        }

        @Override
        public void setStatus(final String status) throws MessagingException
        {
            updateFolderColumn( "status", status);
        }
        public void setPushState(final String pushState) throws MessagingException
        {
            mPushState = pushState;
            updateFolderColumn("push_state", pushState);
        }

        private void updateFolderColumn(final String column, final Object value) throws MessagingException
        {
            try
            {
                database.execute(false, new DbCallback<Void>()
                {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException
                    {
                        try
                        {
                            open(OpenMode.READ_WRITE);
                        }
                        catch (MessagingException e)
                        {
                            throw new WrappedException(e);
                        }
                        db.execSQL("UPDATE folders SET "+column+" = ? WHERE id = ?", new Object[] { value, mFolderId });
                        return null;
                    }
                });
            }
            catch (WrappedException e)
            {
                throw (MessagingException) e.getCause();
            }
        }
        public String getPushState()
        {
            return mPushState;
        }
        @Override
        public FolderClass getDisplayClass()
        {
            return mDisplayClass;
        }

        @Override
        public FolderClass getSyncClass()
        {
            if (FolderClass.INHERITED == mSyncClass)
            {
                return getDisplayClass();
            }
            else
            {
                return mSyncClass;
            }
        }

        public FolderClass getRawSyncClass()
        {
            return mSyncClass;

        }

        @Override
        public FolderClass getPushClass()
        {
            if (FolderClass.INHERITED == mPushClass)
            {
                return getSyncClass();
            }
            else
            {
                return mPushClass;
            }
        }

        public FolderClass getRawPushClass()
        {
            return mPushClass;

        }

        public void setDisplayClass(FolderClass displayClass) throws MessagingException
        {
            mDisplayClass = displayClass;
            updateFolderColumn( "display_class", mDisplayClass.name());

        }

        public void setSyncClass(FolderClass syncClass) throws MessagingException
        {
            mSyncClass = syncClass;
            updateFolderColumn( "poll_class", mSyncClass.name());
        }
        public void setPushClass(FolderClass pushClass) throws MessagingException
        {
            mPushClass = pushClass;
            updateFolderColumn( "push_class", mPushClass.name());
        }

        public boolean isIntegrate()
        {
            return mIntegrate;
        }
        public void setIntegrate(boolean integrate) throws MessagingException
        {
            mIntegrate = integrate;
            updateFolderColumn( "integrate", mIntegrate ? 1 : 0 );
        }


        @Override
        public void fetch(final Message[] messages, final FetchProfile fp, final MessageRetrievalListener listener)
        throws MessagingException
        {
            try
            {
                database.execute(false, new DbCallback<Void>()
                {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException
                    {
                        try
                        {
                            open(OpenMode.READ_WRITE);
                            if (fp.contains(FetchProfile.Item.BODY))
                            {
                                for (Message message : messages)
                                {
                                    LocalMessage localMessage = (LocalMessage)message;
                                    Cursor cursor = null;
                                    MimeMultipart mp = new MimeMultipart();
                                    mp.setSubType("mixed");
                                    try
                                    {
                                        cursor = db.rawQuery("SELECT html_content, text_content, mime_type FROM messages "
                                                             + "WHERE id = ?",
                                                             new String[] { Long.toString(localMessage.mId) });
                                        cursor.moveToNext();
                                        String htmlContent = cursor.getString(0);
                                        String textContent = cursor.getString(1);
                                        String mimeType = cursor.getString(2);
                                        if (mimeType != null && mimeType.toLowerCase().startsWith("multipart/"))
                                        {
                                            // If this is a multipart message, preserve both text
                                            // and html parts, as well as the subtype.
                                            mp.setSubType(mimeType.toLowerCase().replaceFirst("^multipart/", ""));
                                            if (textContent != null)
                                            {
                                                LocalTextBody body = new LocalTextBody(textContent, htmlContent);
                                                MimeBodyPart bp = new MimeBodyPart(body, "text/plain");
                                                mp.addBodyPart(bp);
                                            }
                                            if (htmlContent != null)
                                            {
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
                                            if (textContent != null && htmlContent != null && !mimeType.equalsIgnoreCase("multipart/alternative"))
                                            {
                                                MimeMultipart alternativeParts = mp;
                                                alternativeParts.setSubType("alternative");
                                                mp = new MimeMultipart();
                                                mp.addBodyPart(new MimeBodyPart(alternativeParts));
                                            }
                                        }
                                        else if (mimeType != null && mimeType.equalsIgnoreCase("text/plain"))
                                        {
                                            // If it's text, add only the plain part. The MIME
                                            // container will drop away below.
                                            if (textContent != null)
                                            {
                                                LocalTextBody body = new LocalTextBody(textContent, htmlContent);
                                                MimeBodyPart bp = new MimeBodyPart(body, "text/plain");
                                                mp.addBodyPart(bp);
                                            }
                                        }
                                        else if (mimeType != null && mimeType.equalsIgnoreCase("text/html"))
                                        {
                                            // If it's html, add only the html part. The MIME
                                            // container will drop away below.
                                            if (htmlContent != null)
                                            {
                                                TextBody body = new TextBody(htmlContent);
                                                MimeBodyPart bp = new MimeBodyPart(body, "text/html");
                                                mp.addBodyPart(bp);
                                            }
                                        }
                                        else
                                        {
                                            // MIME type not set. Grab whatever part we can get,
                                            // with Text taking precedence. This preserves pre-HTML
                                            // composition behaviour.
                                            if (textContent != null)
                                            {
                                                LocalTextBody body = new LocalTextBody(textContent, htmlContent);
                                                MimeBodyPart bp = new MimeBodyPart(body, "text/plain");
                                                mp.addBodyPart(bp);
                                            }
                                            else if (htmlContent != null)
                                            {
                                                TextBody body = new TextBody(htmlContent);
                                                MimeBodyPart bp = new MimeBodyPart(body, "text/html");
                                                mp.addBodyPart(bp);
                                            }
                                        }

                                    }
                                    catch (Exception e)
                                    {
                                        Log.e(K9.LOG_TAG, "Exception fetching message:", e);
                                    }
                                    finally
                                    {
                                        if (cursor != null)
                                        {
                                            cursor.close();
                                        }
                                    }

                                    try
                                    {
                                        cursor = db.query(
                                                     "attachments",
                                                     new String[]
                                                     {
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

                                        while (cursor.moveToNext())
                                        {
                                            long id = cursor.getLong(0);
                                            int size = cursor.getInt(1);
                                            String name = cursor.getString(2);
                                            String type = cursor.getString(3);
                                            String storeData = cursor.getString(4);
                                            String contentUri = cursor.getString(5);
                                            String contentId = cursor.getString(6);
                                            String contentDisposition = cursor.getString(7);
                                            Body body = null;

                                            if (contentDisposition == null)
                                            {
                                                contentDisposition = "attachment";
                                            }

                                            if (contentUri != null)
                                            {
                                                body = new LocalAttachmentBody(Uri.parse(contentUri), mApplication);
                                            }
                                            MimeBodyPart bp = new LocalAttachmentBodyPart(body, id);
                                            bp.setHeader(MimeHeader.HEADER_CONTENT_TYPE,
                                                         String.format("%s;\n name=\"%s\"",
                                                                       type,
                                                                       name));
                                            bp.setHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING, "base64");
                                            bp.setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION,
                                                         String.format("%s;\n filename=\"%s\";\n size=%d",
                                                                       contentDisposition,
                                                                       name,
                                                                       size));

                                            bp.setHeader(MimeHeader.HEADER_CONTENT_ID, contentId);
                                            /*
                                             * HEADER_ANDROID_ATTACHMENT_STORE_DATA is a custom header we add to that
                                             * we can later pull the attachment from the remote store if necessary.
                                             */
                                            bp.setHeader(MimeHeader.HEADER_ANDROID_ATTACHMENT_STORE_DATA, storeData);

                                            mp.addBodyPart(bp);
                                        }
                                    }
                                    finally
                                    {
                                        if (cursor != null)
                                        {
                                            cursor.close();
                                        }
                                    }

                                    if (mp.getCount() == 0)
                                    {
                                        // If we have no body, remove the container and create a
                                        // dummy plain text body. This check helps prevents us from
                                        // triggering T_MIME_NO_TEXT and T_TVD_MIME_NO_HEADERS
                                        // SpamAssassin rules.
                                        localMessage.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/plain");
                                        localMessage.setBody(new TextBody(""));
                                    }
                                    else if (mp.getCount() == 1)
                                    {
                                        // If we have only one part, drop the MimeMultipart container.
                                        BodyPart part = mp.getBodyPart(0);
                                        localMessage.setHeader(MimeHeader.HEADER_CONTENT_TYPE, part.getContentType());
                                        localMessage.setBody(part.getBody());
                                    }
                                    else
                                    {
                                        // Otherwise, attach the MimeMultipart to the message.
                                        localMessage.setBody(mp);
                                    }
                                }
                            }
                        }
                        catch (MessagingException e)
                        {
                            throw new WrappedException(e);
                        }
                        return null;
                    }
                });
            }
            catch (WrappedException e)
            {
                throw (MessagingException) e.getCause();
            }
        }

        @Override
        public Message[] getMessages(int start, int end, Date earliestDate, MessageRetrievalListener listener)
        throws MessagingException
        {
            open(OpenMode.READ_WRITE);
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
        private void populateHeaders(final List<LocalMessage> messages) throws UnavailableStorageException
        {
            database.execute(false, new DbCallback<Void>()
            {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException
                {
                    Cursor cursor = null;
                    if (messages.size() == 0)
                    {
                        return null;
                    }
                    try
                    {
                        Map<Long, LocalMessage> popMessages = new HashMap<Long, LocalMessage>();
                        List<String> ids = new ArrayList<String>();
                        StringBuffer questions = new StringBuffer();

                        for (int i = 0; i < messages.size(); i++)
                        {
                            if (i != 0)
                            {
                                questions.append(", ");
                            }
                            questions.append("?");
                            LocalMessage message = messages.get(i);
                            Long id = message.getId();
                            ids.add(Long.toString(id));
                            popMessages.put(id, message);

                        }

                        cursor = db.rawQuery(
                                     "SELECT message_id, name, value FROM headers " + "WHERE message_id in ( " + questions + ") ",
                                     ids.toArray(EMPTY_STRING_ARRAY));


                        while (cursor.moveToNext())
                        {
                            Long id = cursor.getLong(0);
                            String name = cursor.getString(1);
                            String value = cursor.getString(2);
                            //Log.i(K9.LOG_TAG, "Retrieved header name= " + name + ", value = " + value + " for message " + id);
                            popMessages.get(id).addHeader(name, value);
                        }
                    }
                    finally
                    {
                        if (cursor != null)
                        {
                            cursor.close();
                        }
                    }
                    return null;
                }
            });
        }

        @Override
        public Message getMessage(final String uid) throws MessagingException
        {
            try
            {
                return database.execute(false, new DbCallback<Message>()
                {
                    @Override
                    public Message doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException
                    {
                        try
                        {
                            open(OpenMode.READ_WRITE);
                            LocalMessage message = new LocalMessage(uid, LocalFolder.this);
                            Cursor cursor = null;

                            try
                            {
                                cursor = db.rawQuery(
                                             "SELECT "
                                             + GET_MESSAGES_COLS
                                             + "FROM messages WHERE uid = ? AND folder_id = ?",
                                             new String[]
                                             {
                                                 message.getUid(), Long.toString(mFolderId)
                                             });
                                if (!cursor.moveToNext())
                                {
                                    return null;
                                }
                                message.populateFromGetMessageCursor(cursor);
                            }
                            finally
                            {
                                if (cursor != null)
                                {
                                    cursor.close();
                                }
                            }
                            return message;
                        }
                        catch (MessagingException e)
                        {
                            throw new WrappedException(e);
                        }
                    }
                });
            }
            catch (WrappedException e)
            {
                throw (MessagingException) e.getCause();
            }
        }

        @Override
        public Message[] getMessages(MessageRetrievalListener listener) throws MessagingException
        {
            return getMessages(listener, true);
        }

        @Override
        public Message[] getMessages(final MessageRetrievalListener listener, final boolean includeDeleted) throws MessagingException
        {
            try
            {
                return database.execute(false, new DbCallback<Message[]>()
                {
                    @Override
                    public Message[] doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException
                    {
                        try
                        {
                            open(OpenMode.READ_WRITE);
                            return LocalStore.this.getMessages(
                                       listener,
                                       LocalFolder.this,
                                       "SELECT " + GET_MESSAGES_COLS
                                       + "FROM messages WHERE "
                                       + (includeDeleted ? "" : "deleted = 0 AND ")
                                       + " folder_id = ? ORDER BY date DESC"
                                       , new String[]
                                       {
                                           Long.toString(mFolderId)
                                       }
                                   );
                        }
                        catch (MessagingException e)
                        {
                            throw new WrappedException(e);
                        }
                    }
                });
            }
            catch (WrappedException e)
            {
                throw (MessagingException) e.getCause();
            }
        }


        @Override
        public Message[] getMessages(String[] uids, MessageRetrievalListener listener)
        throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            if (uids == null)
            {
                return getMessages(listener);
            }
            ArrayList<Message> messages = new ArrayList<Message>();
            for (String uid : uids)
            {
                Message message = getMessage(uid);
                if (message != null)
                {
                    messages.add(message);
                }
            }
            return messages.toArray(EMPTY_MESSAGE_ARRAY);
        }

        @Override
        public void copyMessages(Message[] msgs, Folder folder) throws MessagingException
        {
            if (!(folder instanceof LocalFolder))
            {
                throw new MessagingException("copyMessages called with incorrect Folder");
            }
            ((LocalFolder) folder).appendMessages(msgs, true);
        }

        @Override
        public void moveMessages(final Message[] msgs, final Folder destFolder) throws MessagingException
        {
            if (!(destFolder instanceof LocalFolder))
            {
                throw new MessagingException("moveMessages called with non-LocalFolder");
            }

            final LocalFolder lDestFolder = (LocalFolder)destFolder;

            try
            {
                database.execute(false, new DbCallback<Void>()
                {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException
                    {
                        try
                        {
                            lDestFolder.open(OpenMode.READ_WRITE);
                            for (Message message : msgs)
                            {
                                LocalMessage lMessage = (LocalMessage)message;

                                if (!message.isSet(Flag.SEEN))
                                {
                                    setUnreadMessageCount(getUnreadMessageCount() - 1);
                                    lDestFolder.setUnreadMessageCount(lDestFolder.getUnreadMessageCount() + 1);
                                }

                                if (message.isSet(Flag.FLAGGED))
                                {
                                    setFlaggedMessageCount(getFlaggedMessageCount() - 1);
                                    lDestFolder.setFlaggedMessageCount(lDestFolder.getFlaggedMessageCount() + 1);
                                }

                                String oldUID = message.getUid();

                                if (K9.DEBUG)
                                    Log.d(K9.LOG_TAG, "Updating folder_id to " + lDestFolder.getId() + " for message with UID "
                                          + message.getUid() + ", id " + lMessage.getId() + " currently in folder " + getName());

                                message.setUid(K9.LOCAL_UID_PREFIX + UUID.randomUUID().toString());

                                db.execSQL("UPDATE messages " + "SET folder_id = ?, uid = ? " + "WHERE id = ?", new Object[]
                                           {
                                               lDestFolder.getId(),
                                               message.getUid(),
                                               lMessage.getId()
                                           });

                                LocalMessage placeHolder = new LocalMessage(oldUID, LocalFolder.this);
                                placeHolder.setFlagInternal(Flag.DELETED, true);
                                placeHolder.setFlagInternal(Flag.SEEN, true);
                                appendMessages(new Message[] { placeHolder });
                            }
                        }
                        catch (MessagingException e)
                        {
                            throw new WrappedException(e);
                        }
                        return null;
                    }
                });
            }
            catch (WrappedException e)
            {
                throw (MessagingException) e.getCause();
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
        public Message storeSmallMessage(final Message message, final Runnable runnable) throws MessagingException
        {
            return database.execute(true, new DbCallback<Message>()
            {
                @Override
                public Message doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException
                {
                    try
                    {
                        appendMessages(new Message[] { message });
                        final String uid = message.getUid();
                        final Message result = getMessage(uid);
                        runnable.run();
                        // Set a flag indicating this message has now be fully downloaded
                        result.setFlag(Flag.X_DOWNLOADED_FULL, true);
                        return result;
                    }
                    catch (MessagingException e)
                    {
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
        public void appendMessages(Message[] messages) throws MessagingException
        {
            appendMessages(messages, false);
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
         * @param messages
         * @param copy
         */
        private void appendMessages(final Message[] messages, final boolean copy) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            try
            {
                database.execute(true, new DbCallback<Void>()
                {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException
                    {
                        try
                        {
                            for (Message message : messages)
                            {
                                if (!(message instanceof MimeMessage))
                                {
                                    throw new Error("LocalStore can only store Messages that extend MimeMessage");
                                }

                                String uid = message.getUid();
                                if (uid == null || copy)
                                {
                                    uid = K9.LOCAL_UID_PREFIX + UUID.randomUUID().toString();
                                    if (!copy)
                                    {
                                        message.setUid(uid);
                                    }
                                }
                                else
                                {
                                    Message oldMessage = getMessage(uid);
                                    if (oldMessage != null && !oldMessage.isSet(Flag.SEEN))
                                    {
                                        setUnreadMessageCount(getUnreadMessageCount() - 1);
                                    }
                                    if (oldMessage != null && oldMessage.isSet(Flag.FLAGGED))
                                    {
                                        setFlaggedMessageCount(getFlaggedMessageCount() - 1);
                                    }
                                    /*
                                     * The message may already exist in this Folder, so delete it first.
                                     */
                                    deleteAttachments(message.getUid());
                                    db.execSQL("DELETE FROM messages WHERE folder_id = ? AND uid = ?",
                                               new Object[]
                                               { mFolderId, message.getUid() });
                                }

                                ArrayList<Part> viewables = new ArrayList<Part>();
                                ArrayList<Part> attachments = new ArrayList<Part>();
                                MimeUtility.collectParts(message, viewables, attachments);

                                StringBuffer sbHtml = new StringBuffer();
                                StringBuffer sbText = new StringBuffer();
                                for (Part viewable : viewables)
                                {
                                    try
                                    {
                                        String text = MimeUtility.getTextFromPart(viewable);
                                        /*
                                         * Anything with MIME type text/html will be stored as such. Anything
                                         * else will be stored as text/plain.
                                         */
                                        if (viewable.getMimeType().equalsIgnoreCase("text/html"))
                                        {
                                            sbHtml.append(text);
                                        }
                                        else
                                        {
                                            sbText.append(text);
                                        }
                                    }
                                    catch (Exception e)
                                    {
                                        throw new MessagingException("Unable to get text for message part", e);
                                    }
                                }

                                String text = sbText.toString();
                                String html = markupContent(text, sbHtml.toString());
                                String preview = calculateContentPreview(text);
                                // If we couldn't generate a reasonable preview from the text part, try doing it with the HTML part.
                                if (preview == null || preview.length() == 0)
                                {
                                    preview = calculateContentPreview(HtmlConverter.htmlToText(html));
                                }

                                try
                                {
                                    ContentValues cv = new ContentValues();
                                    cv.put("uid", uid);
                                    cv.put("subject", message.getSubject());
                                    cv.put("sender_list", Address.pack(message.getFrom()));
                                    cv.put("date", message.getSentDate() == null
                                           ? System.currentTimeMillis() : message.getSentDate().getTime());
                                    cv.put("flags", Utility.combine(message.getFlags(), ',').toUpperCase());
                                    cv.put("deleted", message.isSet(Flag.DELETED) ? 1 : 0);
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

                                    String messageId = message.getMessageId();
                                    if (messageId != null)
                                    {
                                        cv.put("message_id", messageId);
                                    }
                                    long messageUid;
                                    messageUid = db.insert("messages", "uid", cv);
                                    for (Part attachment : attachments)
                                    {
                                        saveAttachment(messageUid, attachment, copy);
                                    }
                                    saveHeaders(messageUid, (MimeMessage)message);
                                    if (!message.isSet(Flag.SEEN))
                                    {
                                        setUnreadMessageCount(getUnreadMessageCount() + 1);
                                    }
                                    if (message.isSet(Flag.FLAGGED))
                                    {
                                        setFlaggedMessageCount(getFlaggedMessageCount() + 1);
                                    }
                                }
                                catch (Exception e)
                                {
                                    throw new MessagingException("Error appending message", e);
                                }
                            }
                        }
                        catch (MessagingException e)
                        {
                            throw new WrappedException(e);
                        }
                        return null;
                    }
                });
            }
            catch (WrappedException e)
            {
                throw (MessagingException) e.getCause();
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
        public void updateMessage(final LocalMessage message) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            try
            {
                database.execute(false, new DbCallback<Void>()
                {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException
                    {
                        try
                        {
                            ArrayList<Part> viewables = new ArrayList<Part>();
                            ArrayList<Part> attachments = new ArrayList<Part>();

                            message.buildMimeRepresentation();

                            MimeUtility.collectParts(message, viewables, attachments);

                            StringBuffer sbHtml = new StringBuffer();
                            StringBuffer sbText = new StringBuffer();
                            for (int i = 0, count = viewables.size(); i < count; i++)
                            {
                                Part viewable = viewables.get(i);
                                try
                                {
                                    String text = MimeUtility.getTextFromPart(viewable);
                                    /*
                                     * Anything with MIME type text/html will be stored as such. Anything
                                     * else will be stored as text/plain.
                                     */
                                    if (viewable.getMimeType().equalsIgnoreCase("text/html"))
                                    {
                                        sbHtml.append(text);
                                    }
                                    else
                                    {
                                        sbText.append(text);
                                    }
                                }
                                catch (Exception e)
                                {
                                    throw new MessagingException("Unable to get text for message part", e);
                                }
                            }

                            String text = sbText.toString();
                            String html = markupContent(text, sbHtml.toString());
                            String preview = calculateContentPreview(text);
                            // If we couldn't generate a reasonable preview from the text part, try doing it with the HTML part.
                            if (preview == null || preview.length() == 0)
                            {
                                preview = calculateContentPreview(HtmlConverter.htmlToText(html));
                            }
                            try
                            {
                                db.execSQL("UPDATE messages SET "
                                           + "uid = ?, subject = ?, sender_list = ?, date = ?, flags = ?, "
                                           + "folder_id = ?, to_list = ?, cc_list = ?, bcc_list = ?, "
                                           + "html_content = ?, text_content = ?, preview = ?, reply_to_list = ?, "
                                           + "attachment_count = ? WHERE id = ?",
                                           new Object[]
                                           {
                                               message.getUid(),
                                               message.getSubject(),
                                               Address.pack(message.getFrom()),
                                               message.getSentDate() == null ? System
                                               .currentTimeMillis() : message.getSentDate()
                                               .getTime(),
                                               Utility.combine(message.getFlags(), ',').toUpperCase(),
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
                                               message.mId
                                           });

                                for (int i = 0, count = attachments.size(); i < count; i++)
                                {
                                    Part attachment = attachments.get(i);
                                    saveAttachment(message.mId, attachment, false);
                                }
                                saveHeaders(message.getId(), message);
                            }
                            catch (Exception e)
                            {
                                throw new MessagingException("Error appending message", e);
                            }
                        }
                        catch (MessagingException e)
                        {
                            throw new WrappedException(e);
                        }
                        return null;
                    }
                });
            }
            catch (WrappedException e)
            {
                throw (MessagingException) e.getCause();
            }
        }

        /**
         * Save the headers of the given message. Note that the message is not
         * necessarily a {@link LocalMessage} instance.
         * @param id
         * @param message
         * @throws com.fsck.k9.mail.MessagingException
         */
        private void saveHeaders(final long id, final MimeMessage message) throws MessagingException
        {
            database.execute(true, new DbCallback<Void>()
            {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException
                {
                    boolean saveAllHeaders = mAccount.saveAllHeaders();
                    boolean gotAdditionalHeaders = false;

                    deleteHeaders(id);
                    for (String name : message.getHeaderNames())
                    {
                        if (saveAllHeaders || HEADERS_TO_SAVE.contains(name))
                        {
                            String[] values = message.getHeader(name);
                            for (String value : values)
                            {
                                ContentValues cv = new ContentValues();
                                cv.put("message_id", id);
                                cv.put("name", name);
                                cv.put("value", value);
                                db.insert("headers", "name", cv);
                            }
                        }
                        else
                        {
                            gotAdditionalHeaders = true;
                        }
                    }

                    if (!gotAdditionalHeaders)
                    {
                        // Remember that all headers for this message have been saved, so it is
                        // not necessary to download them again in case the user wants to see all headers.
                        List<Flag> appendedFlags = new ArrayList<Flag>();
                        appendedFlags.addAll(Arrays.asList(message.getFlags()));
                        appendedFlags.add(Flag.X_GOT_ALL_HEADERS);

                        db.execSQL("UPDATE messages " + "SET flags = ? " + " WHERE id = ?",
                                   new Object[]
                                   { Utility.combine(appendedFlags.toArray(), ',').toUpperCase(), id });
                    }
                    return null;
                }
            });
        }

        private void deleteHeaders(final long id) throws UnavailableStorageException
        {
            database.execute(false, new DbCallback<Void>()
            {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException
                {
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
        throws IOException, MessagingException
        {
            try
            {
                database.execute(true, new DbCallback<Void>()
                {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException
                    {
                        try
                        {
                            long attachmentId = -1;
                            Uri contentUri = null;
                            int size = -1;
                            File tempAttachmentFile = null;

                            if ((!saveAsNew) && (attachment instanceof LocalAttachmentBodyPart))
                            {
                                attachmentId = ((LocalAttachmentBodyPart) attachment).getAttachmentId();
                            }

                            final File attachmentDirectory = StorageManager.getInstance(mApplication).getAttachmentDirectory(uUid, database.getStorageProviderId());
                            if (attachment.getBody() != null)
                            {
                                Body body = attachment.getBody();
                                if (body instanceof LocalAttachmentBody)
                                {
                                    contentUri = ((LocalAttachmentBody) body).getContentUri();
                                }
                                else
                                {
                                    /*
                                     * If the attachment has a body we're expected to save it into the local store
                                     * so we copy the data into a cached attachment file.
                                     */
                                    InputStream in = attachment.getBody().getInputStream();
                                    tempAttachmentFile = File.createTempFile("att", null, attachmentDirectory);
                                    FileOutputStream out = new FileOutputStream(tempAttachmentFile);
                                    size = IOUtils.copy(in, out);
                                    in.close();
                                    out.close();
                                }
                            }

                            if (size == -1)
                            {
                                /*
                                 * If the attachment is not yet downloaded see if we can pull a size
                                 * off the Content-Disposition.
                                 */
                                String disposition = attachment.getDisposition();
                                if (disposition != null)
                                {
                                    String s = MimeUtility.getHeaderParameter(disposition, "size");
                                    if (s != null)
                                    {
                                        size = Integer.parseInt(s);
                                    }
                                }
                            }
                            if (size == -1)
                            {
                                size = 0;
                            }

                            String storeData =
                                Utility.combine(attachment.getHeader(
                                                    MimeHeader.HEADER_ANDROID_ATTACHMENT_STORE_DATA), ',');

                            String name = MimeUtility.getHeaderParameter(attachment.getContentType(), "name");
                            String contentId = MimeUtility.getHeaderParameter(attachment.getContentId(), null);

                            String contentDisposition = MimeUtility.unfoldAndDecode(attachment.getDisposition());
                            if (name == null && contentDisposition != null)
                            {
                                name = MimeUtility.getHeaderParameter(contentDisposition, "filename");
                            }
                            if (attachmentId == -1)
                            {
                                ContentValues cv = new ContentValues();
                                cv.put("message_id", messageId);
                                cv.put("content_uri", contentUri != null ? contentUri.toString() : null);
                                cv.put("store_data", storeData);
                                cv.put("size", size);
                                cv.put("name", name);
                                cv.put("mime_type", attachment.getMimeType());
                                cv.put("content_id", contentId);
                                cv.put("content_disposition", contentDisposition);

                                attachmentId = db.insert("attachments", "message_id", cv);
                            }
                            else
                            {
                                ContentValues cv = new ContentValues();
                                cv.put("content_uri", contentUri != null ? contentUri.toString() : null);
                                cv.put("size", size);
                                db.update("attachments", cv, "id = ?", new String[]
                                          { Long.toString(attachmentId) });
                            }

                            if (attachmentId != -1 && tempAttachmentFile != null)
                            {
                                File attachmentFile = new File(attachmentDirectory, Long.toString(attachmentId));
                                tempAttachmentFile.renameTo(attachmentFile);
                                contentUri = AttachmentProvider.getAttachmentUri(
                                                 mAccount,
                                                 attachmentId);
                                attachment.setBody(new LocalAttachmentBody(contentUri, mApplication));
                                ContentValues cv = new ContentValues();
                                cv.put("content_uri", contentUri != null ? contentUri.toString() : null);
                                db.update("attachments", cv, "id = ?", new String[]
                                          { Long.toString(attachmentId) });
                            }

                            /* The message has attachment with Content-ID */
                            if (contentId != null && contentUri != null)
                            {
                                Cursor cursor = db.query("messages", new String[]
                                                         { "html_content" }, "id = ?", new String[]
                                                         { Long.toString(messageId) }, null, null, null);
                                try
                                {
                                    if (cursor.moveToNext())
                                    {
                                        String new_html;

                                        new_html = cursor.getString(0);
                                        new_html = new_html.replaceAll("cid:" + contentId,
                                                                       contentUri.toString());

                                        ContentValues cv = new ContentValues();
                                        cv.put("html_content", new_html);
                                        db.update("messages", cv, "id = ?", new String[]
                                                  { Long.toString(messageId) });
                                    }
                                }
                                finally
                                {
                                    if (cursor != null)
                                    {
                                        cursor.close();
                                    }
                                }
                            }

                            if (attachmentId != -1 && attachment instanceof LocalAttachmentBodyPart)
                            {
                                ((LocalAttachmentBodyPart) attachment).setAttachmentId(attachmentId);
                            }
                            return null;
                        }
                        catch (MessagingException e)
                        {
                            throw new WrappedException(e);
                        }
                        catch (IOException e)
                        {
                            throw new WrappedException(e);
                        }
                    }
                });
            }
            catch (WrappedException e)
            {
                final Throwable cause = e.getCause();
                if (cause instanceof IOException)
                {
                    throw (IOException) cause;
                }
                else
                {
                    throw (MessagingException) cause;
                }
            }
        }

        /**
         * Changes the stored uid of the given message (using it's internal id as a key) to
         * the uid in the message.
         * @param message
         * @throws com.fsck.k9.mail.MessagingException
         */
        public void changeUid(final LocalMessage message) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            final ContentValues cv = new ContentValues();
            cv.put("uid", message.getUid());
            database.execute(false, new DbCallback<Void>()
            {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException
                {
                    db.update("messages", cv, "id = ?", new String[]
                              { Long.toString(message.mId) });
                    return null;
                }
            });
        }

        @Override
        public void setFlags(Message[] messages, Flag[] flags, boolean value)
        throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            for (Message message : messages)
            {
                message.setFlags(flags, value);
            }
        }

        @Override
        public void setFlags(Flag[] flags, boolean value)
        throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            for (Message message : getMessages(null))
            {
                message.setFlags(flags, value);
            }
        }

        @Override
        public String getUidFromMessageId(Message message) throws MessagingException
        {
            throw new MessagingException("Cannot call getUidFromMessageId on LocalFolder");
        }

        private void clearMessagesWhere(final String whereClause, final String[] params)  throws MessagingException
        {
            open(OpenMode.READ_ONLY);
            Message[] messages  = LocalStore.this.getMessages(
                                      null,
                                      this,
                                      "SELECT " + GET_MESSAGES_COLS + "FROM messages WHERE " + whereClause,
                                      params);

            for (Message message : messages)
            {
                deleteAttachments(message.getUid());
            }
            database.execute(false, new DbCallback<Void>()
            {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException
                {
                    db.execSQL("DELETE FROM messages WHERE " + whereClause, params);
                    return null;
                }
            });
            resetUnreadAndFlaggedCounts();
        }

        public void clearMessagesOlderThan(long cutoff) throws MessagingException
        {
            final String where = "folder_id = ? and date < ?";
            final String[] params = new String[]
            {
                Long.toString(mFolderId), Long.toString(cutoff)
            };

            clearMessagesWhere(where, params);
        }



        public void clearAllMessages() throws MessagingException
        {
            final String where = "folder_id = ?";
            final String[] params = new String[]
            {
                Long.toString(mFolderId)
            };


            clearMessagesWhere(where, params);
            setPushState(null);
            setLastPush(0);
            setLastChecked(0);
        }

        private void resetUnreadAndFlaggedCounts()
        {
            try
            {
                int newUnread = 0;
                int newFlagged = 0;
                Message[] messages = getMessages(null);
                for (Message message : messages)
                {
                    if (!message.isSet(Flag.SEEN))
                    {
                        newUnread++;
                    }
                    if (message.isSet(Flag.FLAGGED))
                    {
                        newFlagged++;
                    }
                }
                setUnreadMessageCount(newUnread);
                setFlaggedMessageCount(newFlagged);
            }
            catch (Exception e)
            {
                Log.e(K9.LOG_TAG, "Unable to fetch all messages from LocalStore", e);
            }
        }


        @Override
        public void delete(final boolean recurse) throws MessagingException
        {
            try
            {
                database.execute(false, new DbCallback<Void>()
                {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException
                    {
                        try
                        {
                            // We need to open the folder first to make sure we've got it's id
                            open(OpenMode.READ_ONLY);
                            Message[] messages = getMessages(null);
                            for (Message message : messages)
                            {
                                deleteAttachments(message.getUid());
                            }
                        }
                        catch (MessagingException e)
                        {
                            throw new WrappedException(e);
                        }
                        db.execSQL("DELETE FROM folders WHERE id = ?", new Object[]
                                   { Long.toString(mFolderId), });
                        return null;
                    }
                });
            }
            catch (WrappedException e)
            {
                throw (MessagingException) e.getCause();
            }
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof LocalFolder)
            {
                return ((LocalFolder)o).mName.equals(mName);
            }
            return super.equals(o);
        }

        @Override
        public int hashCode()
        {
            return mName.hashCode();
        }

        @Override
        public Flag[] getPermanentFlags()
        {
            return PERMANENT_FLAGS;
        }


        private void deleteAttachments(final long messageId) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            database.execute(false, new DbCallback<Void>()
            {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException
                {
                    Cursor attachmentsCursor = null;
                    try
                    {
                        attachmentsCursor = db.query("attachments", new String[]
                                                     { "id" }, "message_id = ?", new String[]
                                                     { Long.toString(messageId) }, null, null, null);
                        final File attachmentDirectory = StorageManager.getInstance(mApplication)
                                                         .getAttachmentDirectory(uUid, database.getStorageProviderId());
                        while (attachmentsCursor.moveToNext())
                        {
                            long attachmentId = attachmentsCursor.getLong(0);
                            try
                            {
                                File file = new File(attachmentDirectory, Long.toString(attachmentId));
                                if (file.exists())
                                {
                                    file.delete();
                                }
                            }
                            catch (Exception e)
                            {

                            }
                        }
                    }
                    finally
                    {
                        if (attachmentsCursor != null)
                        {
                            attachmentsCursor.close();
                        }
                    }
                    return null;
                }
            });
        }

        private void deleteAttachments(final String uid) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            try
            {
                database.execute(false, new DbCallback<Void>()
                {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException
                    {
                        Cursor messagesCursor = null;
                        try
                        {
                            messagesCursor = db.query("messages", new String[]
                                                      { "id" }, "folder_id = ? AND uid = ?", new String[]
                                                      { Long.toString(mFolderId), uid }, null, null, null);
                            while (messagesCursor.moveToNext())
                            {
                                long messageId = messagesCursor.getLong(0);
                                deleteAttachments(messageId);

                            }
                        }
                        catch (MessagingException e)
                        {
                            throw new WrappedException(e);
                        }
                        finally
                        {
                            if (messagesCursor != null)
                            {
                                messagesCursor.close();
                            }
                        }
                        return null;
                    }
                });
            }
            catch (WrappedException e)
            {
                throw (MessagingException) e.getCause();
            }
        }

        /*
         * calculateContentPreview
         * Takes a plain text message body as a string.
         * Returns a message summary as a string suitable for showing in a message list
         *
         * A message summary should be about the first 160 characters
         * of unique text written by the message sender
         * Quoted text, "On $date" and so on will be stripped out.
         * All newlines and whitespace will be compressed.
         *
         */
        public String calculateContentPreview(String text)
        {
            if (text == null)
            {
                return null;
            }

            // Only look at the first 8k of a message when calculating
            // the preview.  This should avoid unnecessary
            // memory usage on large messages
            if (text.length() > 8192)
            {
                text = text.substring(0,8192);
            }


            text = text.replaceAll("(?m)^----.*?$","");
            text = text.replaceAll("(?m)^[#>].*$","");
            text = text.replaceAll("(?m)^On .*wrote.?$","");
            text = text.replaceAll("(?m)^.*\\w+:$","");
            text = text.replaceAll("https?://\\S+","...");
            text = text.replaceAll("(\\r|\\n)+"," ");
            text = text.replaceAll("\\s+"," ");
            if (text.length() <= 512)
            {
                return text;
            }
            else
            {
                text = text.substring(0,512);
                return text;
            }

        }

        public String markupContent(String text, String html)
        {
            if (text.length() > 0 && html.length() == 0)
            {
                html = HtmlConverter.textToHtml(text);
            }

            html = HtmlConverter.convertEmoji2Img(html);

            return html;
        }


        @Override
        public boolean isInTopGroup()
        {
            return mInTopGroup;
        }

        public void setInTopGroup(boolean inTopGroup) throws MessagingException
        {
            mInTopGroup = inTopGroup;
            updateFolderColumn( "top_group", mInTopGroup ? 1 : 0 );
        }

        public Integer getLastUid()
        {
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
        public void updateLastUid() throws MessagingException
        {
            Integer lastUid = database.execute(false, new DbCallback<Integer>()
            {
                @Override
                public Integer doDbWork(final SQLiteDatabase db)
                {
                    Cursor cursor = null;
                    try
                    {
                        open(OpenMode.READ_ONLY);
                        cursor = db.rawQuery("SELECT MAX(uid) FROM messages WHERE folder_id=?", new String[] { Long.toString(mFolderId) });
                        if (cursor.getCount() > 0)
                        {
                            cursor.moveToFirst();
                            return cursor.getInt(0);
                        }
                    }
                    catch (Exception e)
                    {
                        Log.e(K9.LOG_TAG, "Unable to updateLastUid: ", e);
                    }
                    finally
                    {
                        if (cursor != null)
                        {
                            cursor.close();
                        }
                    }
                    return null;
                }
            });
            if(K9.DEBUG)
                Log.d(K9.LOG_TAG, "Updated last UID for folder " + mName + " to " + lastUid);
            mLastUid = lastUid;
        }
    }

    public static class LocalTextBody extends TextBody
    {
        /**
         * This is an HTML-ified version of the message for display purposes.
         */
        private String mBodyForDisplay;

        public LocalTextBody(String body)
        {
            super(body);
        }

        public LocalTextBody(String body, String bodyForDisplay)
        {
            super(body);
            this.mBodyForDisplay = bodyForDisplay;
        }

        public String getBodyForDisplay()
        {
            return mBodyForDisplay;
        }

        public void setBodyForDisplay(String mBodyForDisplay)
        {
            this.mBodyForDisplay = mBodyForDisplay;
        }

    }//LocalTextBody

    public class LocalMessage extends MimeMessage
    {
        private long mId;
        private int mAttachmentCount;
        private String mSubject;

        private String mPreview = "";

        private boolean mToMeCalculated = false;
        private boolean mCcMeCalculated = false;
        private boolean mToMe = false;
        private boolean mCcMe = false;


        private boolean mHeadersLoaded = false;
        private boolean mMessageDirty = false;

        public LocalMessage()
        {
        }

        LocalMessage(String uid, Folder folder)
        {
            this.mUid = uid;
            this.mFolder = folder;
        }

        private void populateFromGetMessageCursor(Cursor cursor)
        throws MessagingException
        {
            final String subject = cursor.getString(0);
            this.setSubject(subject == null ? "" : subject);

            Address[] from = Address.unpack(cursor.getString(1));
            if (from.length > 0)
            {
                this.setFrom(from[0]);
            }
            this.setInternalSentDate(new Date(cursor.getLong(2)));
            this.setUid(cursor.getString(3));
            String flagList = cursor.getString(4);
            if (flagList != null && flagList.length() > 0)
            {
                String[] flags = flagList.split(",");

                for (String flag : flags)
                {
                    try
                    {
                        this.setFlagInternal(Flag.valueOf(flag), true);
                    }

                    catch (Exception e)
                    {
                        if (!"X_BAD_FLAG".equals(flag))
                        {
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

            if (this.mFolder == null)
            {
                LocalFolder f = new LocalFolder(cursor.getInt(13));
                f.open(LocalFolder.OpenMode.READ_WRITE);
                this.mFolder = f;
            }
        }

        /**
         * Fetch the message text for display. This always returns an HTML-ified version of the
         * message, even if it was originally a text-only message.
         * @return HTML version of message for display purposes.
         * @throws MessagingException
         */
        public String getTextForDisplay() throws MessagingException
        {
            String text;    // First try and fetch an HTML part.
            Part part = MimeUtility.findFirstPartByMimeType(this, "text/html");
            if (part == null)
            {
                // If that fails, try and get a text part.
                part = MimeUtility.findFirstPartByMimeType(this, "text/plain");
                if (part == null)
                {
                    text = null;
                }
                else
                {
                    LocalStore.LocalTextBody body = (LocalStore.LocalTextBody) part.getBody();
                    if (body == null)
                    {
                        text = null;
                    }
                    else
                    {
                        text = body.getBodyForDisplay();
                    }
                }
            }
            else
            {
                // We successfully found an HTML part; do the necessary character set decoding.
                text = MimeUtility.getTextFromPart(part);
            }
            return text;
        }


        /* Custom version of writeTo that updates the MIME message based on localMessage
         * changes.
         */

        @Override
        public void writeTo(OutputStream out) throws IOException, MessagingException
        {
            if (mMessageDirty) buildMimeRepresentation();
            super.writeTo(out);
        }

        private void buildMimeRepresentation() throws MessagingException
        {
            if (!mMessageDirty)
            {
                return;
            }

            super.setSubject(mSubject);
            if (this.mFrom != null && this.mFrom.length > 0)
            {
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

        public String getPreview()
        {
            return mPreview;
        }

        @Override
        public String getSubject()
        {
            return mSubject;
        }


        @Override
        public void setSubject(String subject) throws MessagingException
        {
            mSubject = subject;
            mMessageDirty = true;
        }


        @Override
        public void setMessageId(String messageId)
        {
            mMessageId = messageId;
            mMessageDirty = true;
        }

        public boolean hasAttachments()
        {
            if (mAttachmentCount > 0)
            {
                return true;
            }
            else
            {
                return false;
            }

        }

        public int getAttachmentCount()
        {
            return mAttachmentCount;
        }

        @Override
        public void setFrom(Address from) throws MessagingException
        {
            this.mFrom = new Address[] { from };
            mMessageDirty = true;
        }


        @Override
        public void setReplyTo(Address[] replyTo) throws MessagingException
        {
            if (replyTo == null || replyTo.length == 0)
            {
                mReplyTo = null;
            }
            else
            {
                mReplyTo = replyTo;
            }
            mMessageDirty = true;
        }


        /*
         * For performance reasons, we add headers instead of setting them (see super implementation)
         * which removes (expensive) them before adding them
         */
        @Override
        public void setRecipients(RecipientType type, Address[] addresses) throws MessagingException
        {
            if (type == RecipientType.TO)
            {
                if (addresses == null || addresses.length == 0)
                {
                    this.mTo = null;
                }
                else
                {
                    this.mTo = addresses;
                }
            }
            else if (type == RecipientType.CC)
            {
                if (addresses == null || addresses.length == 0)
                {
                    this.mCc = null;
                }
                else
                {
                    this.mCc = addresses;
                }
            }
            else if (type == RecipientType.BCC)
            {
                if (addresses == null || addresses.length == 0)
                {
                    this.mBcc = null;
                }
                else
                {
                    this.mBcc = addresses;
                }
            }
            else
            {
                throw new MessagingException("Unrecognized recipient type.");
            }
            mMessageDirty = true;
        }



        public boolean toMe()
        {
            try
            {
                if (!mToMeCalculated)
                {
                    for (Address address : getRecipients(RecipientType.TO))
                    {
                        if (mAccount.isAnIdentity(address))
                        {
                            mToMe = true;
                            mToMeCalculated = true;
                        }
                    }
                }
            }
            catch (MessagingException e)
            {
                // do something better than ignore this
                // getRecipients can throw a messagingexception
            }
            return mToMe;
        }





        public boolean ccMe()
        {
            try
            {

                if (!mCcMeCalculated)
                {
                    for(Address address : getRecipients(RecipientType.CC))
                    {
                        if (mAccount.isAnIdentity(address))
                        {
                            mCcMe = true;
                            mCcMeCalculated = true;
                        }
                    }

                }
            }
            catch (MessagingException e)
            {
                // do something better than ignore this
                // getRecipients can throw a messagingexception
            }

            return mCcMe;
        }






        public void setFlagInternal(Flag flag, boolean set) throws MessagingException
        {
            super.setFlag(flag, set);
        }

        public long getId()
        {
            return mId;
        }

        @Override
        public void setFlag(final Flag flag, final boolean set) throws MessagingException
        {

            try
            {
                database.execute(true, new DbCallback<Void>()
                {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException
                    {
                        try
                        {
                            if (flag == Flag.DELETED && set)
                            {
                                delete();
                            }

                            updateFolderCountsOnFlag(flag, set);


                            LocalMessage.super.setFlag(flag, set);
                        }
                        catch (MessagingException e)
                        {
                            throw new WrappedException(e);
                        }
                        /*
                         * Set the flags on the message.
                         */
                        db.execSQL("UPDATE messages " + "SET flags = ? " + " WHERE id = ?", new Object[]
                                   { Utility.combine(getFlags(), ',').toUpperCase(), mId });
                        return null;
                    }
                });
            }
            catch (WrappedException e)
            {
                throw (MessagingException) e.getCause();
            }


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
            try
            {
                database.execute(true, new DbCallback<Void>()
                {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException
                    {
                        db.execSQL("UPDATE messages SET " + "deleted = 1," + "subject = NULL, "
                                   + "sender_list = NULL, " + "date = NULL, " + "to_list = NULL, "
                                   + "cc_list = NULL, " + "bcc_list = NULL, " + "preview = NULL, "
                                   + "html_content = NULL, " + "text_content = NULL, "
                                   + "reply_to_list = NULL " + "WHERE id = ?", new Object[]
                                   { mId });
                        /*
                         * Delete all of the message's attachments to save space.
                         * We do this explicit deletion here because we're not deleting the record
                         * in messages, which means our ON DELETE trigger for messages won't cascade
                         */
                        try
                        {
                            ((LocalFolder) mFolder).deleteAttachments(mId);
                        }
                        catch (MessagingException e)
                        {
                            throw new WrappedException(e);
                        }
                        db.execSQL("DELETE FROM attachments WHERE message_id = ?", new Object[]
                                   { mId });
                        return null;
                    }
                });
            }
            catch (WrappedException e)
            {
                throw (MessagingException) e.getCause();
            }
            ((LocalFolder)mFolder).deleteHeaders(mId);


        }

        /*
         * Completely remove a message from the local database
         */
        @Override
        public void destroy() throws MessagingException
        {
            try
            {
                database.execute(true, new DbCallback<Void>()
                {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException,
                        UnavailableStorageException
                    {
                        try
                        {
                            updateFolderCountsOnFlag(Flag.X_DESTROYED, true);
                            ((LocalFolder) mFolder).deleteAttachments(mId);
                            db.execSQL("DELETE FROM messages WHERE id = ?", new Object[] { mId });
                        }
                        catch (MessagingException e)
                        {
                            throw new WrappedException(e);
                        }
                        return null;
                    }
                });
            }
            catch (WrappedException e)
            {
                throw (MessagingException) e.getCause();
            }
        }

        private void updateFolderCountsOnFlag(Flag flag, boolean set)
        {
            /*
             * Update the unread count on the folder.
             */
            try
            {
                LocalFolder folder = (LocalFolder)mFolder;
                if (flag == Flag.DELETED || flag == Flag.X_DESTROYED)
                {
                    if (!isSet(Flag.SEEN))
                    {
                        folder.setUnreadMessageCount(folder.getUnreadMessageCount() + ( set ? -1:1) );
                    }
                    if (isSet(Flag.FLAGGED))
                    {
                        folder.setFlaggedMessageCount(folder.getFlaggedMessageCount() + (set ? -1 : 1));
                    }
                }


                if ( !isSet(Flag.DELETED) )
                {

                    if ( flag == Flag.SEEN )
                    {
                        if (set != isSet(Flag.SEEN))
                        {
                            folder.setUnreadMessageCount(folder.getUnreadMessageCount() + ( set ? -1: 1) );
                        }
                    }

                    if ( flag == Flag.FLAGGED )
                    {
                        folder.setFlaggedMessageCount(folder.getFlaggedMessageCount() + (set ?  1 : -1));
                    }
                }
            }
            catch (MessagingException me)
            {
                Log.e(K9.LOG_TAG, "Unable to update LocalStore unread message count",
                      me);
                throw new RuntimeException(me);
            }
        }

        private void loadHeaders() throws UnavailableStorageException
        {
            ArrayList<LocalMessage> messages = new ArrayList<LocalMessage>();
            messages.add(this);
            mHeadersLoaded = true; // set true before calling populate headers to stop recursion
            ((LocalFolder) mFolder).populateHeaders(messages);

        }

        @Override
        public void addHeader(String name, String value) throws UnavailableStorageException
        {
            if (!mHeadersLoaded)
                loadHeaders();
            super.addHeader(name, value);
        }

        @Override
        public void setHeader(String name, String value) throws UnavailableStorageException
        {
            if (!mHeadersLoaded)
                loadHeaders();
            super.setHeader(name, value);
        }

        @Override
        public String[] getHeader(String name) throws UnavailableStorageException
        {
            if (!mHeadersLoaded)
                loadHeaders();
            return super.getHeader(name);
        }

        @Override
        public void removeHeader(String name) throws UnavailableStorageException
        {
            if (!mHeadersLoaded)
                loadHeaders();
            super.removeHeader(name);
        }

        @Override
        public Set<String> getHeaderNames() throws UnavailableStorageException
        {
            if (!mHeadersLoaded)
                loadHeaders();
            return super.getHeaderNames();
        }
    }

    public static class LocalAttachmentBodyPart extends MimeBodyPart
    {
        private long mAttachmentId = -1;

        public LocalAttachmentBodyPart(Body body, long attachmentId) throws MessagingException
        {
            super(body);
            mAttachmentId = attachmentId;
        }

        /**
         * Returns the local attachment id of this body, or -1 if it is not stored.
         * @return
         */
        public long getAttachmentId()
        {
            return mAttachmentId;
        }

        public void setAttachmentId(long attachmentId)
        {
            mAttachmentId = attachmentId;
        }

        @Override
        public String toString()
        {
            return "" + mAttachmentId;
        }
    }

    public static class LocalAttachmentBody implements Body
    {
        private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
        private Application mApplication;
        private Uri mUri;

        public LocalAttachmentBody(Uri uri, Application application)
        {
            mApplication = application;
            mUri = uri;
        }

        public InputStream getInputStream() throws MessagingException
        {
            try
            {
                return mApplication.getContentResolver().openInputStream(mUri);
            }
            catch (FileNotFoundException fnfe)
            {
                /*
                 * Since it's completely normal for us to try to serve up attachments that
                 * have been blown away, we just return an empty stream.
                 */
                return new ByteArrayInputStream(EMPTY_BYTE_ARRAY);
            }
        }

        public void writeTo(OutputStream out) throws IOException, MessagingException
        {
            InputStream in = getInputStream();
            Base64OutputStream base64Out = new Base64OutputStream(out);
            IOUtils.copy(in, base64Out);
            base64Out.close();
        }

        public Uri getContentUri()
        {
            return mUri;
        }
    }
}
