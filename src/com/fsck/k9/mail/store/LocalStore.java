
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
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;

import org.apache.commons.io.IOUtils;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.text.Html;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.Account.LocalStoreMigrationListener;
import com.fsck.k9.controller.MessageRemovalListener;
import com.fsck.k9.controller.MessageRetrievalListener;
import com.fsck.k9.helper.Regex;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.filter.Base64OutputStream;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mail.store.StorageManager.StorageProvider;
import com.fsck.k9.provider.AttachmentProvider;

/**
 * <pre>
 * Implements a SQLite database backed local store for Messages.
 * </pre>
 */
public class LocalStore extends Store implements Serializable, LocalStoreMigrationListener
{

    /**
     * Callback interface for DB operations. Concept is similar to Spring
     * HibernateCallback.
     *
     * @param <T>
     *            Return value type for {@link #doDbWork(SQLiteDatabase)}
     */
    public static interface DbCallback<T>
    {
        /**
         * @param db
         *            The locked database on which the work should occur. Never
         *            <code>null</code>.
         * @return Any relevant data. Can be <code>null</code>.
         * @throws WrappedException
         * @throws UnavailableStorageException
         */
        T doDbWork(SQLiteDatabase db) throws WrappedException, UnavailableStorageException;
    }

    /**
     * Workaround exception wrapper used to keep the inner exception generated
     * in a {@link DbCallback}.
     */
    protected static class WrappedException extends RuntimeException
    {
        /**
         *
         */
        private static final long serialVersionUID = 8184421232587399369L;

        public WrappedException(final Exception cause)
        {
            super(cause);
        }
    }

    private static final Message[] EMPTY_MESSAGE_ARRAY = new Message[0];

    /**
     * Immutable empty {@link String} array
     */
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final int DB_VERSION = 39;
    private static final Flag[] PERMANENT_FLAGS = { Flag.DELETED, Flag.X_DESTROYED, Flag.SEEN, Flag.FLAGGED };

    private static final int MAX_SMART_HTMLIFY_MESSAGE_LENGTH = 1024 * 256 ;

    private String mStorageProviderId;

    private SQLiteDatabase mDb;

    {
        final ReadWriteLock lock = new ReentrantReadWriteLock(true);
        mReadLock = lock.readLock();
        mWriteLock = lock.writeLock();
    }

    /**
     * Reentrant read lock
     */
    protected final Lock mReadLock;

    /**
     * Reentrant write lock (if you lock it 2x from the same thread, you have to
     * unlock it 2x to release it)
     */
    protected final Lock mWriteLock;

    private Application mApplication;
    private String uUid = null;

    private static Set<String> HEADERS_TO_SAVE = new HashSet<String>();
    static
    {
        HEADERS_TO_SAVE.add(K9.K9MAIL_IDENTITY);
        HEADERS_TO_SAVE.add("To");
        HEADERS_TO_SAVE.add("Cc");
        HEADERS_TO_SAVE.add("From");
        HEADERS_TO_SAVE.add("In-Reply-To");
        HEADERS_TO_SAVE.add("References");
        HEADERS_TO_SAVE.add("Content-ID");
        HEADERS_TO_SAVE.add("Content-Disposition");
        HEADERS_TO_SAVE.add("X-User-Agent");
    }
    /*
     * a String containing the columns getMessages expects to work with
     * in the correct order.
     */
    static private String GET_MESSAGES_COLS =
        "subject, sender_list, date, uid, flags, id, to_list, cc_list, "
        + "bcc_list, reply_to_list, attachment_count, internal_date, message_id, folder_id, preview ";

    private final StorageListener mStorageListener = new StorageListener();

    /**
     * {@link ThreadLocal} to check whether a DB transaction is occuring in the
     * current {@link Thread}.
     *
     * @see #execute(boolean, DbCallback)
     */
    private ThreadLocal<Boolean> inTransaction = new ThreadLocal<Boolean>();

    /**
     * local://localhost/path/to/database/uuid.db
     * This constructor is only used by {@link Store#getLocalInstance(Account, Application)}
     * @throws UnavailableStorageException if not {@link StorageProvider#isReady(Context)}
     */
    public LocalStore(Account account, Application application) throws MessagingException
    {
        super(account);
        mApplication = application;
        mStorageProviderId = account.getLocalStorageProviderId();
        account.setLocalStoreMigrationListener(this);
        uUid = account.getUuid();

        lockWrite();
        try
        {
            openOrCreateDataspace(application);
        }
        finally
        {
            unlockWrite();
        }

        StorageManager.getInstance(application).addListener(mStorageListener);
    }

    /**
     * Lock the storage for shared operations (concurrent threads are allowed to
     * run simultaneously).
     *
     * <p>
     * You <strong>have to</strong> invoke {@link #unlockRead()} when you're
     * done with the storage.
     * </p>
     *
     * @throws UnavailableStorageException
     *             If storage can't be locked because it is not available
     */
    protected void lockRead() throws UnavailableStorageException
    {
        mReadLock.lock();
        try
        {
            StorageManager.getInstance(mApplication).lockProvider(mStorageProviderId);
        }
        catch (UnavailableStorageException e)
        {
            mReadLock.unlock();
            throw e;
        }
        catch (RuntimeException e)
        {
            mReadLock.unlock();
            throw e;
        }
    }

    protected void unlockRead()
    {
        StorageManager.getInstance(mApplication).unlockProvider(mStorageProviderId);
        mReadLock.unlock();
    }

    /**
     * Lock the storage for exclusive access (other threads aren't allowed to
     * run simultaneously)
     *
     * <p>
     * You <strong>have to</strong> invoke {@link #unlockWrite()} when you're
     * done with the storage.
     * </p>
     *
     * @throws UnavailableStorageException
     *             If storage can't be locked because it is not available.
     */
    protected void lockWrite() throws UnavailableStorageException
    {
        lockWrite(mStorageProviderId);
    }

    /**
     * Lock the storage for exclusive access (other threads aren't allowed to
     * run simultaneously)
     *
     * <p>
     * You <strong>have to</strong> invoke {@link #unlockWrite()} when you're
     * done with the storage.
     * </p>
     *
     * @param providerId
     *            Never <code>null</code>.
     *
     * @throws UnavailableStorageException
     *             If storage can't be locked because it is not available.
     */
    protected void lockWrite(final String providerId) throws UnavailableStorageException
    {
        mWriteLock.lock();
        try
        {
            StorageManager.getInstance(mApplication).lockProvider(providerId);
        }
        catch (UnavailableStorageException e)
        {
            mWriteLock.unlock();
            throw e;
        }
        catch (RuntimeException e)
        {
            mWriteLock.unlock();
            throw e;
        }
    }

    protected void unlockWrite()
    {
        unlockWrite(mStorageProviderId);
    }

    protected void unlockWrite(final String providerId)
    {
        StorageManager.getInstance(mApplication).unlockProvider(providerId);
        mWriteLock.unlock();
    }

    /**
     * Execute a DB callback in a shared context (doesn't prevent concurrent
     * shared executions), taking care of locking the DB storage.
     *
     * <p>
     * Can be instructed to start a transaction if none is currently active in
     * the current thread. Callback will participe in any active transaction (no
     * inner transaction created).
     * </p>
     *
     * @param transactional
     *            <code>true</code> the callback must be executed in a
     *            transactional context.
     * @param callback
     *            Never <code>null</code>.
     *
     * @param <T>
     * @return Whatever {@link DbCallback#doDbWork(SQLiteDatabase)} returns.
     * @throws UnavailableStorageException
     */
    protected <T> T execute(final boolean transactional, final DbCallback<T> callback) throws UnavailableStorageException
    {
        lockRead();
        final boolean doTransaction = transactional && inTransaction.get() == null;
        try
        {

            final boolean debug = K9.DEBUG;
            if (doTransaction)
            {
                inTransaction.set(Boolean.TRUE);
                mDb.beginTransaction();
            }
            try
            {
                final T result = callback.doDbWork(mDb);
                if (doTransaction)
                {
                    mDb.setTransactionSuccessful();
                }
                return result;
            }
            finally
            {
                if (doTransaction)
                {
                    final long begin;
                    if (debug)
                    {
                        begin = System.currentTimeMillis();
                    }
                    else
                    {
                        begin = 0l;
                    }
                    // not doing endTransaction in the same 'finally' block of unlockRead() because endTransaction() may throw an exception
                    mDb.endTransaction();
                    if (debug)
                    {
                        Log.v(K9.LOG_TAG, "LocalStore: Transaction ended, took " + Long.toString(System.currentTimeMillis() - begin) + "ms / " + new Exception().getStackTrace()[1].toString());
                    }
                }
            }
        }
        finally
        {
            if (doTransaction)
            {
                inTransaction.set(null);
            }
            unlockRead();
        }
    }

    public void onLocalStoreMigration(final String oldProviderId,
                                      final String newProviderId) throws MessagingException
    {
        lockWrite(oldProviderId);
        try
        {
            lockWrite(newProviderId);
            try
            {
                try
                {
                    mDb.close();
                }
                catch (Exception e)
                {
                    Log.i(K9.LOG_TAG, "Unable to close DB on local store migration", e);
                }

                final StorageManager storageManager = StorageManager.getInstance(mApplication);

                // create new path
                prepareStorage(newProviderId);

                // move all database files
                moveRecursive(storageManager.getDatabase(uUid, oldProviderId), storageManager.getDatabase(uUid, newProviderId));
                // move all attachment files
                moveRecursive(storageManager.getAttachmentDirectory(uUid, oldProviderId), storageManager.getAttachmentDirectory(uUid, newProviderId));

                mStorageProviderId = newProviderId;

                // re-initialize this class with the new Uri
                openOrCreateDataspace(mApplication);
            }
            finally
            {
                unlockWrite(newProviderId);
            }
        }
        finally
        {
            unlockWrite(oldProviderId);
        }
    }

    private void moveRecursive(File fromDir, File toDir)
    {
        if (!fromDir.exists())
        {
            return;
        }
        if (!fromDir.isDirectory())
        {
            if (toDir.exists())
            {
                if (!toDir.delete())
                {
                    Log.w(K9.LOG_TAG, "cannot delete already existing file/directory " + toDir.getAbsolutePath() + " during migration to/from SD-card");
                }
            }
            if (!fromDir.renameTo(toDir))
            {
                Log.w(K9.LOG_TAG, "cannot rename " + fromDir.getAbsolutePath() + " to " + toDir.getAbsolutePath() + " - moving instead");
                move(fromDir, toDir);
            }
            return;
        }
        if (!toDir.exists() || !toDir.isDirectory())
        {
            if (toDir.exists() )
            {
                toDir.delete();
            }
            if (!toDir.mkdirs())
            {
                Log.w(K9.LOG_TAG, "cannot create directory " + toDir.getAbsolutePath() + " during migration to/from SD-card");
            }
        }
        File[] files = fromDir.listFiles();
        for (File file : files)
        {
            if (file.isDirectory())
            {
                moveRecursive(file, new File(toDir, file.getName()));
                file.delete();
            }
            else
            {
                File target = new File(toDir, file.getName());
                if (!file.renameTo(target))
                {
                    Log.w(K9.LOG_TAG, "cannot rename " + file.getAbsolutePath() + " to " + target.getAbsolutePath() + " - moving instead");
                    move(file, target);
                }
            }
        }
        if (!fromDir.delete())
        {
            Log.w(K9.LOG_TAG, "cannot delete " + fromDir.getAbsolutePath() + " after migration to/from SD-card");
        }
    }
    private boolean move(File from, File to)
    {
        if (to.exists())
        {
            to.delete();
        }
        to.getParentFile().mkdirs();

        try
        {
            FileInputStream in = new FileInputStream(from);
            FileOutputStream out = new FileOutputStream(to);
            byte[] buffer = new byte[1024];
            int count = -1;
            while ((count = in.read(buffer)) > 0)
            {
                out.write(buffer, 0, count);
            }
            out.close();
            in.close();
            from.delete();
            return true;
        }
        catch (Exception e)
        {
            Log.w(K9.LOG_TAG, "cannot move " + from.getAbsolutePath() + " to " + to.getAbsolutePath(), e);
            return false;
        }

    }

    /**
     *
     * @param application
     * @throws UnavailableStorageException
     */
    void openOrCreateDataspace(final Application application) throws UnavailableStorageException
    {

        lockWrite();
        try
        {
            final File databaseFile = prepareStorage(mStorageProviderId);
            try
            {
                mDb = SQLiteDatabase.openOrCreateDatabase(databaseFile, null);
            }
            catch (SQLiteException e)
            {
                // try to gracefully handle DB corruption - see issue 2537
                Log.w(K9.LOG_TAG, "Unable to open DB " + databaseFile + " - removing file and retrying", e);
                databaseFile.delete();
                mDb = SQLiteDatabase.openOrCreateDatabase(databaseFile, null);
            }
            if (mDb.getVersion() != DB_VERSION)
            {
                doDbUpgrade(mDb, application);
            }
        }
        finally
        {
            unlockWrite();
        }
    }

    /**
     * @param providerId
     *            Never <code>null</code>.
     * @return DB file.
     * @throws UnavailableStorageException
     */
    protected File prepareStorage(final String providerId) throws UnavailableStorageException
    {
        final StorageManager storageManager = StorageManager.getInstance(mApplication);

        final File databaseFile;
        final File databaseParentDir;
        databaseFile = storageManager.getDatabase(uUid, providerId);
        databaseParentDir = databaseFile.getParentFile();
        if (databaseParentDir.isFile())
        {
            // should be safe to inconditionally delete clashing file: user is not supposed to mess with our directory
            databaseParentDir.delete();
        }
        if (!databaseParentDir.exists())
        {
            if (!databaseParentDir.mkdirs())
            {
                // Android seems to be unmounting the storage...
                throw new UnavailableStorageException("Unable to access: " + databaseParentDir);
            }
            touchFile(databaseParentDir, ".nomedia");
        }

        final File attachmentDir;
        final File attachmentParentDir;
        attachmentDir = storageManager
                        .getAttachmentDirectory(uUid, providerId);
        attachmentParentDir = attachmentDir.getParentFile();
        if (!attachmentParentDir.exists())
        {
            attachmentParentDir.mkdirs();
            touchFile(attachmentParentDir, ".nomedia");
        }
        if (!attachmentDir.exists())
        {
            attachmentDir.mkdirs();
        }
        return databaseFile;
    }

    /**
     * @param parentDir
     * @param name
     *            Never <code>null</code>.
     */
    protected void touchFile(final File parentDir, String name)
    {
        final File file = new File(parentDir, name);
        try
        {
            if (!file.exists())
            {
                file.createNewFile();
            }
            else
            {
                file.setLastModified(System.currentTimeMillis());
            }
        }
        catch (Exception e)
        {
            Log.d(K9.LOG_TAG, "Unable to touch file: " + file.getAbsolutePath(), e);
        }
    }

    private void doDbUpgrade(final SQLiteDatabase db, final Application application)
    {
        Log.i(K9.LOG_TAG, String.format("Upgrading database from version %d to version %d",
                                        db.getVersion(), DB_VERSION));


        AttachmentProvider.clear(application);

        try
        {
            // schema version 29 was when we moved to incremental updates
            // in the case of a new db or a < v29 db, we blow away and start from scratch
            if (db.getVersion() < 29)
            {

                db.execSQL("DROP TABLE IF EXISTS folders");
                db.execSQL("CREATE TABLE folders (id INTEGER PRIMARY KEY, name TEXT, "
                           + "last_updated INTEGER, unread_count INTEGER, visible_limit INTEGER, status TEXT, push_state TEXT, last_pushed INTEGER, flagged_count INTEGER default 0)");

                db.execSQL("CREATE INDEX IF NOT EXISTS folder_name ON folders (name)");
                db.execSQL("DROP TABLE IF EXISTS messages");
                db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY, deleted INTEGER default 0, folder_id INTEGER, uid TEXT, subject TEXT, "
                           + "date INTEGER, flags TEXT, sender_list TEXT, to_list TEXT, cc_list TEXT, bcc_list TEXT, reply_to_list TEXT, "
                           + "html_content TEXT, text_content TEXT, attachment_count INTEGER, internal_date INTEGER, message_id TEXT, preview TEXT)");

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

        try
        {
            pruneCachedAttachments(true);
        }
        catch (Exception me)
        {
            Log.e(K9.LOG_TAG, "Exception while force pruning attachments during DB update", me);
        }
    }

    public long getSize() throws UnavailableStorageException
    {

        final StorageManager storageManager = StorageManager.getInstance(mApplication);

        final File attachmentDirectory = storageManager.getAttachmentDirectory(uUid,
                                         mStorageProviderId);

        return execute(false, new DbCallback<Long>()
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

                final File dbFile = storageManager.getDatabase(uUid, mStorageProviderId);
                return dbFile.length() + attachmentLength;
            }
        });
    }

    public void compact() throws MessagingException
    {
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "Before prune size = " + getSize());

        pruneCachedAttachments();
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "After prune / before compaction size = " + getSize());

        execute(false, new DbCallback<Void>()
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
        execute(false, new DbCallback<Void>()
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
        return execute(false, new DbCallback<Integer>()
        {
            @Override
            public Integer doDbWork(final SQLiteDatabase db)
            {
                Cursor cursor = null;
                try
                {
                    cursor = db.rawQuery("SELECT COUNT(*) FROM messages", null);
                    cursor.moveToFirst();
                    int messageCount = cursor.getInt(0);
                    return messageCount;
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
        return execute(false, new DbCallback<Integer>()
        {
            @Override
            public Integer doDbWork(final SQLiteDatabase db)
            {
                Cursor cursor = null;
                try
                {
                    cursor = db.rawQuery("SELECT COUNT(*) FROM folders", null);
                    cursor.moveToFirst();
                    int messageCount = cursor.getInt(0);
                    return messageCount;
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
    public LocalFolder getFolder(String name) {
        return new LocalFolder(name);
    }

    // TODO this takes about 260-300ms, seems slow.
    @Override
    public List<? extends Folder> getPersonalNamespaces(boolean forceListAll) throws MessagingException
    {
        final List<LocalFolder> folders = new LinkedList<LocalFolder>();
        try
        {
            execute(false, new DbCallback<List<? extends Folder>>()
            {
                @Override
                public List<? extends Folder> doDbWork(final SQLiteDatabase db) throws WrappedException
                {
                    Cursor cursor = null;

                    try
                    {
                        cursor = db.rawQuery("SELECT id, name, unread_count, visible_limit, last_updated, status, push_state, last_pushed, flagged_count FROM folders ORDER BY name ASC", null);
                        while (cursor.moveToNext())
                        {
                            LocalFolder folder = new LocalFolder(cursor.getString(1));
                            folder.open(cursor.getInt(0), cursor.getString(1), cursor.getInt(2), cursor.getInt(3), cursor.getLong(4), cursor.getString(5), cursor.getString(6), cursor.getLong(7), cursor.getInt(8));

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

    /**
     * Delete the entire Store and it's backing database.
     * @throws UnavailableStorageException
     */
    public void delete() throws UnavailableStorageException
    {
        lockWrite();
        try
        {
            try
            {
                mDb.close();
            }
            catch (Exception e)
            {

            }
            final StorageManager storageManager = StorageManager.getInstance(mApplication);
            try
            {
                final File attachmentDirectory = storageManager.getAttachmentDirectory(uUid, mStorageProviderId);
                final File[] attachments = attachmentDirectory.listFiles();
                for (File attachment : attachments)
                {
                    if (attachment.exists())
                    {
                        attachment.delete();
                    }
                }
                if (attachmentDirectory.exists())
                {
                    attachmentDirectory.delete();
                }
            }
            catch (Exception e)
            {
            }
            try
            {
                storageManager.getDatabase(uUid, mStorageProviderId).delete();
            }
            catch (Exception e)
            {
                Log.i(K9.LOG_TAG, "LocalStore: delete(): Unable to delete backing DB file", e);
            }

            // stop waiting for mount/unmount events
            StorageManager.getInstance(mApplication).removeListener(mStorageListener);
        }
        finally
        {
            unlockWrite();
        }
    }

    public void recreate() throws UnavailableStorageException
    {
        lockWrite();
        try
        {
            try
            {
                mDb.close();
            }
            catch (Exception e)
            {

            }
            final StorageManager storageManager = StorageManager.getInstance(mApplication);
            try
            {
                final File attachmentDirectory = storageManager.getAttachmentDirectory(uUid, mStorageProviderId);
                final File[] attachments = attachmentDirectory.listFiles();
                for (File attachment : attachments)
                {
                    if (attachment.exists())
                    {
                        attachment.delete();
                    }
                }
                if (attachmentDirectory.exists())
                {
                    attachmentDirectory.delete();
                }
            }
            catch (Exception e)
            {
            }
            try
            {
                storageManager.getDatabase(uUid, mStorageProviderId).delete();
            }
            catch (Exception e)
            {

            }
            openOrCreateDataspace(mApplication);
        }
        finally
        {
            unlockWrite();
        }
    }

    public void pruneCachedAttachments() throws MessagingException
    {
        pruneCachedAttachments(false);
    }

    /**
     * Deletes all cached attachments for the entire store.
     */
    public void pruneCachedAttachments(final boolean force) throws MessagingException
    {
        execute(false, new DbCallback<Void>()
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
                File[] files = storageManager.getAttachmentDirectory(uUid, mStorageProviderId).listFiles();
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
        execute(false, new DbCallback<Void>()
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
        return execute(false, new DbCallback<ArrayList<PendingCommand>>()
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
            execute(false, new DbCallback<Void>()
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
        execute(false, new DbCallback<Void>()
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
        execute(false, new DbCallback<Void>()
        {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws WrappedException
            {
                db.delete("pending_commands", null, null);
                return null;
            }
        });
    }

    /**
     * Open the DB on mount and close the DB on unmount
     */
    private class StorageListener implements StorageManager.StorageListener
    {
        @Override
        public void onUnmount(final String providerId)
        {
            if (!providerId.equals(mStorageProviderId))
            {
                return;
            }

            if (K9.DEBUG)
            {
                Log.d(K9.LOG_TAG, "LocalStore: Closing DB " + uUid + " due to unmount event on StorageProvider: " + providerId);
            }

            try
            {
                lockWrite();
                try
                {
                    mDb.close();
                }
                finally
                {
                    unlockWrite();
                }
            }
            catch (UnavailableStorageException e)
            {
                Log.w(K9.LOG_TAG, "Unable to writelock on unmount", e);
            }
        }

        @Override
        public void onMount(String providerId)
        {
            if (!providerId.equals(mStorageProviderId))
            {
                return;
            }

            if (K9.DEBUG)
            {
                Log.d(K9.LOG_TAG, "LocalStore: Opening DB " + uUid + " due to mount event on StorageProvider: " + providerId);
            }

            try
            {
                openOrCreateDataspace(mApplication);
            }
            catch (UnavailableStorageException e)
            {
                Log.e(K9.LOG_TAG, "Unable to open DB on mount", e);
            }
        }
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
        final int j = execute(false, new DbCallback<Integer>()
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
        return execute(false, new DbCallback<String>()
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
        return execute(false, new DbCallback<AttachmentInfo>()
        {
            @Override
            public AttachmentInfo doDbWork(final SQLiteDatabase db) throws WrappedException
            {
                String name = null;
                int size = -1;
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
        private FolderClass displayClass = FolderClass.NO_CLASS;
        private FolderClass syncClass = FolderClass.INHERITED;
        private FolderClass pushClass = FolderClass.SECOND_CLASS;
        private boolean inTopGroup = false;
        private String prefId = null;
        private String mPushState = null;
        private boolean mIntegrate = false;


        public LocalFolder(String name)
        {
            super(LocalStore.this.mAccount);
            this.mName = name;

            if (K9.INBOX.equals(getName()))
            {
                syncClass =  FolderClass.FIRST_CLASS;
                pushClass =  FolderClass.FIRST_CLASS;
                inTopGroup = true;
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
                execute(false, new DbCallback<Void>()
                {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException
                    {
                        Cursor cursor = null;
                        try
                        {
                            String baseQuery =
                                "SELECT id, name,unread_count, visible_limit, last_updated, status, push_state, last_pushed, flagged_count FROM folders ";
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
                                    open(folderId, cursor.getString(1), cursor.getInt(2), cursor.getInt(3), cursor.getLong(4), cursor.getString(5), cursor.getString(6), cursor.getLong(7), cursor.getInt(8));
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

        private void open(int id, String name, int unreadCount, int visibleLimit, long lastChecked, String status, String pushState, long lastPushed, int flaggedCount) throws MessagingException
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
        }

        @Override
        public boolean isOpen()
        {
            return (mFolderId != -1 && mName != null);
        }

        @Override
        public OpenMode getMode() {
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
            return execute(false, new DbCallback<Boolean>()
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
            if (exists())
            {
                throw new MessagingException("Folder " + mName + " already exists.");
            }
            execute(false, new DbCallback<Void>()
            {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws WrappedException
                {
                    db.execSQL("INSERT INTO folders (name, visible_limit) VALUES (?, ?)", new Object[]
                               {
                                   mName,
                                   mAccount.getDisplayCount()
                               });
                    return null;
                }
            });
            return true;
        }

        @Override
        public boolean create(FolderType type, final int visibleLimit) throws MessagingException
        {
            if (exists())
            {
                throw new MessagingException("Folder " + mName + " already exists.");
            }
            execute(false, new DbCallback<Void>()
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
                return execute(false, new DbCallback<Integer>()
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
                            cursor = db.rawQuery("SELECT COUNT(*) FROM messages WHERE messages.folder_id = ?",
                                                 new String[]
                                                 {
                                                     Long.toString(mFolderId)
                                                 });
                            cursor.moveToFirst();
                            int messageCount = cursor.getInt(0);
                            return messageCount;
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
            try
            {
                execute(false, new DbCallback<Void>()
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
                        mUnreadMessageCount = Math.max(0, unreadMessageCount);
                        db.execSQL("UPDATE folders SET unread_count = ? WHERE id = ?",
                                   new Object[] { mUnreadMessageCount, mFolderId });
                        return null;
                    }
                });
            }
            catch (WrappedException e)
            {
                throw (MessagingException) e.getCause();
            }
        }

        public void setFlaggedMessageCount(final int flaggedMessageCount) throws MessagingException
        {
            try
            {
                execute(false, new DbCallback<Integer>()
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
                        mFlaggedMessageCount = Math.max(0, flaggedMessageCount);
                        db.execSQL("UPDATE folders SET flagged_count = ? WHERE id = ?", new Object[]
                                   { mFlaggedMessageCount, mFolderId });
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
        public void setLastChecked(final long lastChecked) throws MessagingException
        {
            try
            {
                execute(false, new DbCallback<Void>()
                {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException
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
                        db.execSQL("UPDATE folders SET last_updated = ? WHERE id = ?", new Object[]
                                   { lastChecked, mFolderId });
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
        public void setLastPush(final long lastChecked) throws MessagingException
        {
            try
            {
                execute(false, new DbCallback<Void>()
                {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException
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
                        db.execSQL("UPDATE folders SET last_pushed = ? WHERE id = ?", new Object[]
                                   { lastChecked, mFolderId });
                        return null;
                    }
                });
            }
            catch (WrappedException e)
            {
                throw (MessagingException) e.getCause();
            }
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
            execute(false, new DbCallback<Void>()
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
                    mVisibleLimit = visibleLimit;
                    db.execSQL("UPDATE folders SET visible_limit = ? WHERE id = ?",
                               new Object[] { mVisibleLimit, mFolderId });
                    return null;
                }
            });
        }

        @Override
        public void setStatus(final String status) throws MessagingException
        {
            try
            {
                execute(false, new DbCallback<Void>()
                {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException
                    {
                        try
                        {
                            open(OpenMode.READ_WRITE);
                            LocalFolder.super.setStatus(status);
                        }
                        catch (MessagingException e)
                        {
                            throw new WrappedException(e);
                        }
                        db.execSQL("UPDATE folders SET status = ? WHERE id = ?", new Object[]
                                   { status, mFolderId });
                        return null;
                    }
                });
            }
            catch (WrappedException e)
            {
                throw (MessagingException) e.getCause();
            }
        }
        public void setPushState(final String pushState) throws MessagingException
        {
            try
            {
                execute(false, new DbCallback<Void>()
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
                        mPushState = pushState;
                        db.execSQL("UPDATE folders SET push_state = ? WHERE id = ?", new Object[]
                                   { pushState, mFolderId });
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
            return displayClass;
        }

        @Override
        public FolderClass getSyncClass()
        {
            if (FolderClass.INHERITED == syncClass)
            {
                return getDisplayClass();
            }
            else
            {
                return syncClass;
            }
        }

        public FolderClass getRawSyncClass()
        {
            return syncClass;

        }

        @Override
        public FolderClass getPushClass()
        {
            if (FolderClass.INHERITED == pushClass)
            {
                return getSyncClass();
            }
            else
            {
                return pushClass;
            }
        }

        public FolderClass getRawPushClass()
        {
            return pushClass;

        }

        public void setDisplayClass(FolderClass displayClass)
        {
            this.displayClass = displayClass;
        }

        public void setSyncClass(FolderClass syncClass)
        {
            this.syncClass = syncClass;
        }
        public void setPushClass(FolderClass pushClass)
        {
            this.pushClass = pushClass;
        }

        public boolean isIntegrate()
        {
            return mIntegrate;
        }
        public void setIntegrate(boolean integrate)
        {
            mIntegrate = integrate;
        }

        private String getPrefId() throws MessagingException
        {
            open(OpenMode.READ_WRITE);

            if (prefId == null)
            {
                prefId = uUid + "." + mName;
            }

            return prefId;
        }

        public void delete(Preferences preferences) throws MessagingException
        {
            String id = getPrefId();

            SharedPreferences.Editor editor = preferences.getPreferences().edit();

            editor.remove(id + ".displayMode");
            editor.remove(id + ".syncMode");
            editor.remove(id + ".pushMode");
            editor.remove(id + ".inTopGroup");
            editor.remove(id + ".integrate");

            editor.commit();
        }

        public void save(Preferences preferences) throws MessagingException
        {
            String id = getPrefId();

            SharedPreferences.Editor editor = preferences.getPreferences().edit();
            // there can be a lot of folders.  For the defaults, let's not save prefs, saving space, except for INBOX
            if (displayClass == FolderClass.NO_CLASS && !K9.INBOX.equals(getName()))
            {
                editor.remove(id + ".displayMode");
            }
            else
            {
                editor.putString(id + ".displayMode", displayClass.name());
            }

            if (syncClass == FolderClass.INHERITED && !K9.INBOX.equals(getName()))
            {
                editor.remove(id + ".syncMode");
            }
            else
            {
                editor.putString(id + ".syncMode", syncClass.name());
            }

            if (pushClass == FolderClass.SECOND_CLASS && !K9.INBOX.equals(getName()))
            {
                editor.remove(id + ".pushMode");
            }
            else
            {
                editor.putString(id + ".pushMode", pushClass.name());
            }
            editor.putBoolean(id + ".inTopGroup", inTopGroup);

            editor.putBoolean(id + ".integrate", mIntegrate);

            editor.commit();
        }


        public FolderClass getDisplayClass(Preferences preferences) throws MessagingException
        {
            String id = getPrefId();
            return FolderClass.valueOf(preferences.getPreferences().getString(id + ".displayMode",
                                       FolderClass.NO_CLASS.name()));
        }

        @Override
        public void refresh(Preferences preferences) throws MessagingException
        {

            String id = getPrefId();

            try
            {
                displayClass = FolderClass.valueOf(preferences.getPreferences().getString(id + ".displayMode",
                                                   FolderClass.NO_CLASS.name()));
            }
            catch (Exception e)
            {
                Log.e(K9.LOG_TAG, "Unable to load displayMode for " + getName(), e);

                displayClass = FolderClass.NO_CLASS;
            }
            if (displayClass == FolderClass.NONE)
            {
                displayClass = FolderClass.NO_CLASS;
            }


            FolderClass defSyncClass = FolderClass.INHERITED;
            if (K9.INBOX.equals(getName()))
            {
                defSyncClass =  FolderClass.FIRST_CLASS;
            }

            try
            {
                syncClass = FolderClass.valueOf(preferences.getPreferences().getString(id  + ".syncMode",
                                                defSyncClass.name()));
            }
            catch (Exception e)
            {
                Log.e(K9.LOG_TAG, "Unable to load syncMode for " + getName(), e);

                syncClass = defSyncClass;
            }
            if (syncClass == FolderClass.NONE)
            {
                syncClass = FolderClass.INHERITED;
            }

            FolderClass defPushClass = FolderClass.SECOND_CLASS;
            boolean defInTopGroup = false;
            boolean defIntegrate = false;
            if (K9.INBOX.equals(getName()))
            {
                defPushClass =  FolderClass.FIRST_CLASS;
                defInTopGroup = true;
                defIntegrate = true;
            }

            try
            {
                pushClass = FolderClass.valueOf(preferences.getPreferences().getString(id  + ".pushMode",
                                                defPushClass.name()));
            }
            catch (Exception e)
            {
                Log.e(K9.LOG_TAG, "Unable to load pushMode for " + getName(), e);

                pushClass = defPushClass;
            }
            if (pushClass == FolderClass.NONE)
            {
                pushClass = FolderClass.INHERITED;
            }
            inTopGroup = preferences.getPreferences().getBoolean(id + ".inTopGroup", defInTopGroup);
            mIntegrate = preferences.getPreferences().getBoolean(id + ".integrate", defIntegrate);

        }

        @Override
        public void fetch(final Message[] messages, final FetchProfile fp, final MessageRetrievalListener listener)
        throws MessagingException
        {
            try
            {
                execute(false, new DbCallback<Void>()
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
                                        cursor = db.rawQuery("SELECT html_content, text_content FROM messages "
                                                             + "WHERE id = ?",
                                                             new String[] { Long.toString(localMessage.mId) });
                                        cursor.moveToNext();
                                        String htmlContent = cursor.getString(0);
                                        String textContent = cursor.getString(1);

                                        if (textContent != null)
                                        {
                                            LocalTextBody body = new LocalTextBody(textContent, htmlContent);
                                            MimeBodyPart bp = new MimeBodyPart(body, "text/plain");
                                            mp.addBodyPart(bp);
                                        }
                                        else
                                        {
                                            TextBody body = new TextBody(htmlContent);
                                            MimeBodyPart bp = new MimeBodyPart(body, "text/html");
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
                                             * we can later pull the attachment from the remote store if neccesary.
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

                                    if (mp.getCount() == 1)
                                    {
                                        BodyPart part = mp.getBodyPart(0);
                                        localMessage.setHeader(MimeHeader.HEADER_CONTENT_TYPE, part.getContentType());
                                        localMessage.setBody(part.getBody());
                                    }
                                    else
                                    {
                                        localMessage.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "multipart/mixed");
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
            execute(false, new DbCallback<Void>()
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
                return execute(false, new DbCallback<Message>()
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
                return execute(false, new DbCallback<Message[]>()
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
                execute(false, new DbCallback<Void>()
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
         */
        private void appendMessages(final Message[] messages, final boolean copy) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            try
            {
                execute(true, new DbCallback<Void>()
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
                execute(false, new DbCallback<Void>()
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
                            if (preview == null || preview.length() == 0)
                            {
                                preview = calculateContentPreview(Html.fromHtml(html).toString());
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
         */
        private void saveHeaders(final long id, final MimeMessage message) throws MessagingException
        {
            execute(true, new DbCallback<Void>()
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
            execute(false, new DbCallback<Void>()
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
         * @param attachmentId -1 to create a new attachment or >= 0 to update an existing
         * @throws IOException
         * @throws MessagingException
         */
        private void saveAttachment(final long messageId, final Part attachment, final boolean saveAsNew)
        throws IOException, MessagingException
        {
            try
            {
                execute(true, new DbCallback<Void>()
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

                            final File attachmentDirectory = StorageManager.getInstance(mApplication).getAttachmentDirectory(uUid, mStorageProviderId);
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
                                Cursor cursor = null;
                                cursor = db.query("messages", new String[]
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
         */
        public void changeUid(final LocalMessage message) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            final ContentValues cv = new ContentValues();
            cv.put("uid", message.getUid());
            execute(false, new DbCallback<Void>()
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
            execute(false, new DbCallback<Void>()
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
                execute(false, new DbCallback<Void>()
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
        public Flag[] getPermanentFlags() {
            return PERMANENT_FLAGS;
        }


        private void deleteAttachments(final long messageId) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            execute(false, new DbCallback<Void>()
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
                                                         .getAttachmentDirectory(uUid, mStorageProviderId);
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
                execute(false, new DbCallback<Void>()
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
                html = htmlifyString(text);
            }

            html = convertEmoji2ImgForDocomo(html);

            return html;
        }

        public String htmlifyString(String text)
        {
            // Our HTMLification code is somewhat memory intensive
            // and was causing lots of OOM errors on the market
            // if the message is big and plain text, just do
            // a trivial htmlification
            if (text.length() > MAX_SMART_HTMLIFY_MESSAGE_LENGTH)
            {
                return "<html><head/><body>" +
                       htmlifyMessageHeader() +
                       text +
                       htmlifyMessageFooter() +
                       "</body></html>";
            }
            StringReader reader = new StringReader(text);
            StringBuilder buff = new StringBuilder(text.length() + 512);
            int c = 0;
            try
            {
                while ((c = reader.read()) != -1)
                {
                    switch (c)
                    {
                        case '&':
                            buff.append("&amp;");
                            break;
                        case '<':
                            buff.append("&lt;");
                            break;
                        case '>':
                            buff.append("&gt;");
                            break;
                        case '\r':
                            break;
                        default:
                            buff.append((char)c);
                    }//switch
                }
            }
            catch (IOException e)
            {
                //Should never happen
                Log.e(K9.LOG_TAG, null, e);
            }
            text = buff.toString();
            text = text.replaceAll("\\s*([-=_]{30,}+)\\s*","<hr />");
            text = text.replaceAll("(?m)^([^\r\n]{4,}[\\s\\w,:;+/])(?:\r\n|\n|\r)(?=[a-z]\\S{0,10}[\\s\\n\\r])","$1 ");
            text = text.replaceAll("(?m)(\r\n|\n|\r){4,}","\n\n");


            Matcher m = Regex.WEB_URL_PATTERN.matcher(text);
            StringBuffer sb = new StringBuffer(text.length() + 512);
            sb.append("<html><head></head><body>");
            sb.append(htmlifyMessageHeader());
            while (m.find())
            {
                int start = m.start();
                if (start == 0 || (start != 0 && text.charAt(start - 1) != '@'))
                {
                    if (m.group().indexOf(':') > 0)   // With no URI-schema we may get "http:/" links with the second / missing
                    {
                        m.appendReplacement(sb, "<a href=\"$0\">$0</a>");
                    }
                    else
                    {
                        m.appendReplacement(sb, "<a href=\"http://$0\">$0</a>");
                    }
                }
                else
                {
                    m.appendReplacement(sb, "$0");
                }
            }




            m.appendTail(sb);
            sb.append(htmlifyMessageFooter());
            sb.append("</body></html>");
            text = sb.toString();

            return text;
        }

        private String htmlifyMessageHeader()
        {
            if (K9.messageViewFixedWidthFont())
            {
                return "<pre style=\"white-space: pre-wrap; word-wrap:break-word; \">";
            }
            else
            {
                return "<div style=\"white-space: pre-wrap; word-wrap:break-word; \">";
            }
        }


        private String htmlifyMessageFooter()
        {
            if (K9.messageViewFixedWidthFont())
            {
                return "</pre>";
            }
            else
            {
                return "</div>";
            }
        }

        public String convertEmoji2ImgForDocomo(String html)
        {
            StringReader reader = new StringReader(html);
            StringBuilder buff = new StringBuilder(html.length() + 512);
            int c = 0;
            try
            {
                while ((c = reader.read()) != -1)
                {
                    switch (c)
                    {
                            // These emoji codepoints are generated by tools/make_emoji in the K-9 source tree

                        case 0xE6F9: //docomo kissmark
                            buff.append("<img src=\"file:///android_asset/emoticons/kissmark.gif\" alt=\"kissmark\" />");
                            break;
                        case 0xE729: //docomo wink
                            buff.append("<img src=\"file:///android_asset/emoticons/wink.gif\" alt=\"wink\" />");
                            break;
                        case 0xE6D2: //docomo info02
                            buff.append("<img src=\"file:///android_asset/emoticons/info02.gif\" alt=\"info02\" />");
                            break;
                        case 0xE753: //docomo smile
                            buff.append("<img src=\"file:///android_asset/emoticons/smile.gif\" alt=\"smile\" />");
                            break;
                        case 0xE68D: //docomo heart
                            buff.append("<img src=\"file:///android_asset/emoticons/heart.gif\" alt=\"heart\" />");
                            break;
                        case 0xE6A5: //docomo downwardleft
                            buff.append("<img src=\"file:///android_asset/emoticons/downwardleft.gif\" alt=\"downwardleft\" />");
                            break;
                        case 0xE6AD: //docomo pouch
                            buff.append("<img src=\"file:///android_asset/emoticons/pouch.gif\" alt=\"pouch\" />");
                            break;
                        case 0xE6D4: //docomo by-d
                            buff.append("<img src=\"file:///android_asset/emoticons/by-d.gif\" alt=\"by-d\" />");
                            break;
                        case 0xE6D7: //docomo free
                            buff.append("<img src=\"file:///android_asset/emoticons/free.gif\" alt=\"free\" />");
                            break;
                        case 0xE6E8: //docomo seven
                            buff.append("<img src=\"file:///android_asset/emoticons/seven.gif\" alt=\"seven\" />");
                            break;
                        case 0xE74E: //docomo snail
                            buff.append("<img src=\"file:///android_asset/emoticons/snail.gif\" alt=\"snail\" />");
                            break;
                        case 0xE658: //docomo basketball
                            buff.append("<img src=\"file:///android_asset/emoticons/basketball.gif\" alt=\"basketball\" />");
                            break;
                        case 0xE65A: //docomo pocketbell
                            buff.append("<img src=\"file:///android_asset/emoticons/pocketbell.gif\" alt=\"pocketbell\" />");
                            break;
                        case 0xE6E3: //docomo two
                            buff.append("<img src=\"file:///android_asset/emoticons/two.gif\" alt=\"two\" />");
                            break;
                        case 0xE74A: //docomo cake
                            buff.append("<img src=\"file:///android_asset/emoticons/cake.gif\" alt=\"cake\" />");
                            break;
                        case 0xE6D0: //docomo faxto
                            buff.append("<img src=\"file:///android_asset/emoticons/faxto.gif\" alt=\"faxto\" />");
                            break;
                        case 0xE661: //docomo ship
                            buff.append("<img src=\"file:///android_asset/emoticons/ship.gif\" alt=\"ship\" />");
                            break;
                        case 0xE64B: //docomo virgo
                            buff.append("<img src=\"file:///android_asset/emoticons/virgo.gif\" alt=\"virgo\" />");
                            break;
                        case 0xE67E: //docomo ticket
                            buff.append("<img src=\"file:///android_asset/emoticons/ticket.gif\" alt=\"ticket\" />");
                            break;
                        case 0xE6D6: //docomo yen
                            buff.append("<img src=\"file:///android_asset/emoticons/yen.gif\" alt=\"yen\" />");
                            break;
                        case 0xE6E0: //docomo sharp
                            buff.append("<img src=\"file:///android_asset/emoticons/sharp.gif\" alt=\"sharp\" />");
                            break;
                        case 0xE6FE: //docomo bomb
                            buff.append("<img src=\"file:///android_asset/emoticons/bomb.gif\" alt=\"bomb\" />");
                            break;
                        case 0xE6E1: //docomo mobaq
                            buff.append("<img src=\"file:///android_asset/emoticons/mobaq.gif\" alt=\"mobaq\" />");
                            break;
                        case 0xE70A: //docomo sign05
                            buff.append("<img src=\"file:///android_asset/emoticons/sign05.gif\" alt=\"sign05\" />");
                            break;
                        case 0xE667: //docomo bank
                            buff.append("<img src=\"file:///android_asset/emoticons/bank.gif\" alt=\"bank\" />");
                            break;
                        case 0xE731: //docomo copyright
                            buff.append("<img src=\"file:///android_asset/emoticons/copyright.gif\" alt=\"copyright\" />");
                            break;
                        case 0xE678: //docomo upwardright
                            buff.append("<img src=\"file:///android_asset/emoticons/upwardright.gif\" alt=\"upwardright\" />");
                            break;
                        case 0xE694: //docomo scissors
                            buff.append("<img src=\"file:///android_asset/emoticons/scissors.gif\" alt=\"scissors\" />");
                            break;
                        case 0xE682: //docomo bag
                            buff.append("<img src=\"file:///android_asset/emoticons/bag.gif\" alt=\"bag\" />");
                            break;
                        case 0xE64D: //docomo scorpius
                            buff.append("<img src=\"file:///android_asset/emoticons/scorpius.gif\" alt=\"scorpius\" />");
                            break;
                        case 0xE6D9: //docomo key
                            buff.append("<img src=\"file:///android_asset/emoticons/key.gif\" alt=\"key\" />");
                            break;
                        case 0xE734: //docomo secret
                            buff.append("<img src=\"file:///android_asset/emoticons/secret.gif\" alt=\"secret\" />");
                            break;
                        case 0xE74F: //docomo chick
                            buff.append("<img src=\"file:///android_asset/emoticons/chick.gif\" alt=\"chick\" />");
                            break;
                        case 0xE691: //docomo eye
                            buff.append("<img src=\"file:///android_asset/emoticons/eye.gif\" alt=\"eye\" />");
                            break;
                        case 0xE70B: //docomo ok
                            buff.append("<img src=\"file:///android_asset/emoticons/ok.gif\" alt=\"ok\" />");
                            break;
                        case 0xE714: //docomo door
                            buff.append("<img src=\"file:///android_asset/emoticons/door.gif\" alt=\"door\" />");
                            break;
                        case 0xE64F: //docomo capricornus
                            buff.append("<img src=\"file:///android_asset/emoticons/capricornus.gif\" alt=\"capricornus\" />");
                            break;
                        case 0xE674: //docomo boutique
                            buff.append("<img src=\"file:///android_asset/emoticons/boutique.gif\" alt=\"boutique\" />");
                            break;
                        case 0xE726: //docomo lovely
                            buff.append("<img src=\"file:///android_asset/emoticons/lovely.gif\" alt=\"lovely\" />");
                            break;
                        case 0xE68F: //docomo diamond
                            buff.append("<img src=\"file:///android_asset/emoticons/diamond.gif\" alt=\"diamond\" />");
                            break;
                        case 0xE69B: //docomo wheelchair
                            buff.append("<img src=\"file:///android_asset/emoticons/wheelchair.gif\" alt=\"wheelchair\" />");
                            break;
                        case 0xE747: //docomo maple
                            buff.append("<img src=\"file:///android_asset/emoticons/maple.gif\" alt=\"maple\" />");
                            break;
                        case 0xE64C: //docomo libra
                            buff.append("<img src=\"file:///android_asset/emoticons/libra.gif\" alt=\"libra\" />");
                            break;
                        case 0xE647: //docomo taurus
                            buff.append("<img src=\"file:///android_asset/emoticons/taurus.gif\" alt=\"taurus\" />");
                            break;
                        case 0xE645: //docomo sprinkle
                            buff.append("<img src=\"file:///android_asset/emoticons/sprinkle.gif\" alt=\"sprinkle\" />");
                            break;
                        case 0xE6FC: //docomo annoy
                            buff.append("<img src=\"file:///android_asset/emoticons/annoy.gif\" alt=\"annoy\" />");
                            break;
                        case 0xE6E6: //docomo five
                            buff.append("<img src=\"file:///android_asset/emoticons/five.gif\" alt=\"five\" />");
                            break;
                        case 0xE676: //docomo karaoke
                            buff.append("<img src=\"file:///android_asset/emoticons/karaoke.gif\" alt=\"karaoke\" />");
                            break;
                        case 0xE69D: //docomo moon1
                            buff.append("<img src=\"file:///android_asset/emoticons/moon1.gif\" alt=\"moon1\" />");
                            break;
                        case 0xE709: //docomo sign04
                            buff.append("<img src=\"file:///android_asset/emoticons/sign04.gif\" alt=\"sign04\" />");
                            break;
                        case 0xE72A: //docomo happy02
                            buff.append("<img src=\"file:///android_asset/emoticons/happy02.gif\" alt=\"happy02\" />");
                            break;
                        case 0xE669: //docomo hotel
                            buff.append("<img src=\"file:///android_asset/emoticons/hotel.gif\" alt=\"hotel\" />");
                            break;
                        case 0xE71B: //docomo ring
                            buff.append("<img src=\"file:///android_asset/emoticons/ring.gif\" alt=\"ring\" />");
                            break;
                        case 0xE644: //docomo mist
                            buff.append("<img src=\"file:///android_asset/emoticons/mist.gif\" alt=\"mist\" />");
                            break;
                        case 0xE73B: //docomo full
                            buff.append("<img src=\"file:///android_asset/emoticons/full.gif\" alt=\"full\" />");
                            break;
                        case 0xE683: //docomo book
                            buff.append("<img src=\"file:///android_asset/emoticons/book.gif\" alt=\"book\" />");
                            break;
                        case 0xE707: //docomo sweat02
                            buff.append("<img src=\"file:///android_asset/emoticons/sweat02.gif\" alt=\"sweat02\" />");
                            break;
                        case 0xE716: //docomo pc
                            buff.append("<img src=\"file:///android_asset/emoticons/pc.gif\" alt=\"pc\" />");
                            break;
                        case 0xE671: //docomo bar
                            buff.append("<img src=\"file:///android_asset/emoticons/bar.gif\" alt=\"bar\" />");
                            break;
                        case 0xE72B: //docomo bearing
                            buff.append("<img src=\"file:///android_asset/emoticons/bearing.gif\" alt=\"bearing\" />");
                            break;
                        case 0xE65C: //docomo subway
                            buff.append("<img src=\"file:///android_asset/emoticons/subway.gif\" alt=\"subway\" />");
                            break;
                        case 0xE725: //docomo gawk
                            buff.append("<img src=\"file:///android_asset/emoticons/gawk.gif\" alt=\"gawk\" />");
                            break;
                        case 0xE745: //docomo apple
                            buff.append("<img src=\"file:///android_asset/emoticons/apple.gif\" alt=\"apple\" />");
                            break;
                        case 0xE65F: //docomo rvcar
                            buff.append("<img src=\"file:///android_asset/emoticons/rvcar.gif\" alt=\"rvcar\" />");
                            break;
                        case 0xE664: //docomo building
                            buff.append("<img src=\"file:///android_asset/emoticons/building.gif\" alt=\"building\" />");
                            break;
                        case 0xE737: //docomo danger
                            buff.append("<img src=\"file:///android_asset/emoticons/danger.gif\" alt=\"danger\" />");
                            break;
                        case 0xE702: //docomo sign01
                            buff.append("<img src=\"file:///android_asset/emoticons/sign01.gif\" alt=\"sign01\" />");
                            break;
                        case 0xE6EC: //docomo heart01
                            buff.append("<img src=\"file:///android_asset/emoticons/heart01.gif\" alt=\"heart01\" />");
                            break;
                        case 0xE660: //docomo bus
                            buff.append("<img src=\"file:///android_asset/emoticons/bus.gif\" alt=\"bus\" />");
                            break;
                        case 0xE72D: //docomo crying
                            buff.append("<img src=\"file:///android_asset/emoticons/crying.gif\" alt=\"crying\" />");
                            break;
                        case 0xE652: //docomo sports
                            buff.append("<img src=\"file:///android_asset/emoticons/sports.gif\" alt=\"sports\" />");
                            break;
                        case 0xE6B8: //docomo on
                            buff.append("<img src=\"file:///android_asset/emoticons/on.gif\" alt=\"on\" />");
                            break;
                        case 0xE73C: //docomo leftright
                            buff.append("<img src=\"file:///android_asset/emoticons/leftright.gif\" alt=\"leftright\" />");
                            break;
                        case 0xE6BA: //docomo clock
                            buff.append("<img src=\"file:///android_asset/emoticons/clock.gif\" alt=\"clock\" />");
                            break;
                        case 0xE6F0: //docomo happy01
                            buff.append("<img src=\"file:///android_asset/emoticons/happy01.gif\" alt=\"happy01\" />");
                            break;
                        case 0xE701: //docomo sleepy
                            buff.append("<img src=\"file:///android_asset/emoticons/sleepy.gif\" alt=\"sleepy\" />");
                            break;
                        case 0xE63E: //docomo sun
                            buff.append("<img src=\"file:///android_asset/emoticons/sun.gif\" alt=\"sun\" />");
                            break;
                        case 0xE67D: //docomo event
                            buff.append("<img src=\"file:///android_asset/emoticons/event.gif\" alt=\"event\" />");
                            break;
                        case 0xE689: //docomo memo
                            buff.append("<img src=\"file:///android_asset/emoticons/memo.gif\" alt=\"memo\" />");
                            break;
                        case 0xE68B: //docomo game
                            buff.append("<img src=\"file:///android_asset/emoticons/game.gif\" alt=\"game\" />");
                            break;
                        case 0xE718: //docomo wrench
                            buff.append("<img src=\"file:///android_asset/emoticons/wrench.gif\" alt=\"wrench\" />");
                            break;
                        case 0xE741: //docomo clover
                            buff.append("<img src=\"file:///android_asset/emoticons/clover.gif\" alt=\"clover\" />");
                            break;
                        case 0xE693: //docomo rock
                            buff.append("<img src=\"file:///android_asset/emoticons/rock.gif\" alt=\"rock\" />");
                            break;
                        case 0xE6F6: //docomo note
                            buff.append("<img src=\"file:///android_asset/emoticons/note.gif\" alt=\"note\" />");
                            break;
                        case 0xE67A: //docomo music
                            buff.append("<img src=\"file:///android_asset/emoticons/music.gif\" alt=\"music\" />");
                            break;
                        case 0xE743: //docomo tulip
                            buff.append("<img src=\"file:///android_asset/emoticons/tulip.gif\" alt=\"tulip\" />");
                            break;
                        case 0xE656: //docomo soccer
                            buff.append("<img src=\"file:///android_asset/emoticons/soccer.gif\" alt=\"soccer\" />");
                            break;
                        case 0xE69C: //docomo newmoon
                            buff.append("<img src=\"file:///android_asset/emoticons/newmoon.gif\" alt=\"newmoon\" />");
                            break;
                        case 0xE73E: //docomo school
                            buff.append("<img src=\"file:///android_asset/emoticons/school.gif\" alt=\"school\" />");
                            break;
                        case 0xE750: //docomo penguin
                            buff.append("<img src=\"file:///android_asset/emoticons/penguin.gif\" alt=\"penguin\" />");
                            break;
                        case 0xE696: //docomo downwardright
                            buff.append("<img src=\"file:///android_asset/emoticons/downwardright.gif\" alt=\"downwardright\" />");
                            break;
                        case 0xE6CE: //docomo phoneto
                            buff.append("<img src=\"file:///android_asset/emoticons/phoneto.gif\" alt=\"phoneto\" />");
                            break;
                        case 0xE728: //docomo bleah
                            buff.append("<img src=\"file:///android_asset/emoticons/bleah.gif\" alt=\"bleah\" />");
                            break;
                        case 0xE662: //docomo airplane
                            buff.append("<img src=\"file:///android_asset/emoticons/airplane.gif\" alt=\"airplane\" />");
                            break;
                        case 0xE74C: //docomo noodle
                            buff.append("<img src=\"file:///android_asset/emoticons/noodle.gif\" alt=\"noodle\" />");
                            break;
                        case 0xE704: //docomo sign03
                            buff.append("<img src=\"file:///android_asset/emoticons/sign03.gif\" alt=\"sign03\" />");
                            break;
                        case 0xE68E: //docomo spade
                            buff.append("<img src=\"file:///android_asset/emoticons/spade.gif\" alt=\"spade\" />");
                            break;
                        case 0xE698: //docomo foot
                            buff.append("<img src=\"file:///android_asset/emoticons/foot.gif\" alt=\"foot\" />");
                            break;
                        case 0xE712: //docomo snowboard
                            buff.append("<img src=\"file:///android_asset/emoticons/snowboard.gif\" alt=\"snowboard\" />");
                            break;
                        case 0xE684: //docomo ribbon
                            buff.append("<img src=\"file:///android_asset/emoticons/ribbon.gif\" alt=\"ribbon\" />");
                            break;
                        case 0xE6DA: //docomo enter
                            buff.append("<img src=\"file:///android_asset/emoticons/enter.gif\" alt=\"enter\" />");
                            break;
                        case 0xE6EA: //docomo nine
                            buff.append("<img src=\"file:///android_asset/emoticons/nine.gif\" alt=\"nine\" />");
                            break;
                        case 0xE722: //docomo coldsweats01
                            buff.append("<img src=\"file:///android_asset/emoticons/coldsweats01.gif\" alt=\"coldsweats01\" />");
                            break;
                        case 0xE6F7: //docomo spa
                            buff.append("<img src=\"file:///android_asset/emoticons/spa.gif\" alt=\"spa\" />");
                            break;
                        case 0xE710: //docomo rouge
                            buff.append("<img src=\"file:///android_asset/emoticons/rouge.gif\" alt=\"rouge\" />");
                            break;
                        case 0xE73F: //docomo wave
                            buff.append("<img src=\"file:///android_asset/emoticons/wave.gif\" alt=\"wave\" />");
                            break;
                        case 0xE686: //docomo birthday
                            buff.append("<img src=\"file:///android_asset/emoticons/birthday.gif\" alt=\"birthday\" />");
                            break;
                        case 0xE721: //docomo confident
                            buff.append("<img src=\"file:///android_asset/emoticons/confident.gif\" alt=\"confident\" />");
                            break;
                        case 0xE6FF: //docomo notes
                            buff.append("<img src=\"file:///android_asset/emoticons/notes.gif\" alt=\"notes\" />");
                            break;
                        case 0xE724: //docomo pout
                            buff.append("<img src=\"file:///android_asset/emoticons/pout.gif\" alt=\"pout\" />");
                            break;
                        case 0xE6A4: //docomo xmas
                            buff.append("<img src=\"file:///android_asset/emoticons/xmas.gif\" alt=\"xmas\" />");
                            break;
                        case 0xE6FB: //docomo flair
                            buff.append("<img src=\"file:///android_asset/emoticons/flair.gif\" alt=\"flair\" />");
                            break;
                        case 0xE71D: //docomo bicycle
                            buff.append("<img src=\"file:///android_asset/emoticons/bicycle.gif\" alt=\"bicycle\" />");
                            break;
                        case 0xE6DC: //docomo search
                            buff.append("<img src=\"file:///android_asset/emoticons/search.gif\" alt=\"search\" />");
                            break;
                        case 0xE757: //docomo shock
                            buff.append("<img src=\"file:///android_asset/emoticons/shock.gif\" alt=\"shock\" />");
                            break;
                        case 0xE680: //docomo nosmoking
                            buff.append("<img src=\"file:///android_asset/emoticons/nosmoking.gif\" alt=\"nosmoking\" />");
                            break;
                        case 0xE66D: //docomo signaler
                            buff.append("<img src=\"file:///android_asset/emoticons/signaler.gif\" alt=\"signaler\" />");
                            break;
                        case 0xE66A: //docomo 24hours
                            buff.append("<img src=\"file:///android_asset/emoticons/24hours.gif\" alt=\"24hours\" />");
                            break;
                        case 0xE6F4: //docomo wobbly
                            buff.append("<img src=\"file:///android_asset/emoticons/wobbly.gif\" alt=\"wobbly\" />");
                            break;
                        case 0xE641: //docomo snow
                            buff.append("<img src=\"file:///android_asset/emoticons/snow.gif\" alt=\"snow\" />");
                            break;
                        case 0xE6AE: //docomo pen
                            buff.append("<img src=\"file:///android_asset/emoticons/pen.gif\" alt=\"pen\" />");
                            break;
                        case 0xE70D: //docomo appli02
                            buff.append("<img src=\"file:///android_asset/emoticons/appli02.gif\" alt=\"appli02\" />");
                            break;
                        case 0xE732: //docomo tm
                            buff.append("<img src=\"file:///android_asset/emoticons/tm.gif\" alt=\"tm\" />");
                            break;
                        case 0xE755: //docomo pig
                            buff.append("<img src=\"file:///android_asset/emoticons/pig.gif\" alt=\"pig\" />");
                            break;
                        case 0xE648: //docomo gemini
                            buff.append("<img src=\"file:///android_asset/emoticons/gemini.gif\" alt=\"gemini\" />");
                            break;
                        case 0xE6DE: //docomo flag
                            buff.append("<img src=\"file:///android_asset/emoticons/flag.gif\" alt=\"flag\" />");
                            break;
                        case 0xE6A1: //docomo dog
                            buff.append("<img src=\"file:///android_asset/emoticons/dog.gif\" alt=\"dog\" />");
                            break;
                        case 0xE6EF: //docomo heart04
                            buff.append("<img src=\"file:///android_asset/emoticons/heart04.gif\" alt=\"heart04\" />");
                            break;
                        case 0xE643: //docomo typhoon
                            buff.append("<img src=\"file:///android_asset/emoticons/typhoon.gif\" alt=\"typhoon\" />");
                            break;
                        case 0xE65B: //docomo train
                            buff.append("<img src=\"file:///android_asset/emoticons/train.gif\" alt=\"train\" />");
                            break;
                        case 0xE746: //docomo bud
                            buff.append("<img src=\"file:///android_asset/emoticons/bud.gif\" alt=\"bud\" />");
                            break;
                        case 0xE653: //docomo baseball
                            buff.append("<img src=\"file:///android_asset/emoticons/baseball.gif\" alt=\"baseball\" />");
                            break;
                        case 0xE6B2: //docomo chair
                            buff.append("<img src=\"file:///android_asset/emoticons/chair.gif\" alt=\"chair\" />");
                            break;
                        case 0xE64A: //docomo leo
                            buff.append("<img src=\"file:///android_asset/emoticons/leo.gif\" alt=\"leo\" />");
                            break;
                        case 0xE6E7: //docomo six
                            buff.append("<img src=\"file:///android_asset/emoticons/six.gif\" alt=\"six\" />");
                            break;
                        case 0xE6E4: //docomo three
                            buff.append("<img src=\"file:///android_asset/emoticons/three.gif\" alt=\"three\" />");
                            break;
                        case 0xE6DF: //docomo freedial
                            buff.append("<img src=\"file:///android_asset/emoticons/freedial.gif\" alt=\"freedial\" />");
                            break;
                        case 0xE744: //docomo banana
                            buff.append("<img src=\"file:///android_asset/emoticons/banana.gif\" alt=\"banana\" />");
                            break;
                        case 0xE6DB: //docomo clear
                            buff.append("<img src=\"file:///android_asset/emoticons/clear.gif\" alt=\"clear\" />");
                            break;
                        case 0xE6AC: //docomo slate
                            buff.append("<img src=\"file:///android_asset/emoticons/slate.gif\" alt=\"slate\" />");
                            break;
                        case 0xE666: //docomo hospital
                            buff.append("<img src=\"file:///android_asset/emoticons/hospital.gif\" alt=\"hospital\" />");
                            break;
                        case 0xE663: //docomo house
                            buff.append("<img src=\"file:///android_asset/emoticons/house.gif\" alt=\"house\" />");
                            break;
                        case 0xE695: //docomo paper
                            buff.append("<img src=\"file:///android_asset/emoticons/paper.gif\" alt=\"paper\" />");
                            break;
                        case 0xE67F: //docomo smoking
                            buff.append("<img src=\"file:///android_asset/emoticons/smoking.gif\" alt=\"smoking\" />");
                            break;
                        case 0xE65D: //docomo bullettrain
                            buff.append("<img src=\"file:///android_asset/emoticons/bullettrain.gif\" alt=\"bullettrain\" />");
                            break;
                        case 0xE6B1: //docomo shadow
                            buff.append("<img src=\"file:///android_asset/emoticons/shadow.gif\" alt=\"shadow\" />");
                            break;
                        case 0xE670: //docomo cafe
                            buff.append("<img src=\"file:///android_asset/emoticons/cafe.gif\" alt=\"cafe\" />");
                            break;
                        case 0xE654: //docomo golf
                            buff.append("<img src=\"file:///android_asset/emoticons/golf.gif\" alt=\"golf\" />");
                            break;
                        case 0xE708: //docomo dash
                            buff.append("<img src=\"file:///android_asset/emoticons/dash.gif\" alt=\"dash\" />");
                            break;
                        case 0xE748: //docomo cherryblossom
                            buff.append("<img src=\"file:///android_asset/emoticons/cherryblossom.gif\" alt=\"cherryblossom\" />");
                            break;
                        case 0xE6F1: //docomo angry
                            buff.append("<img src=\"file:///android_asset/emoticons/angry.gif\" alt=\"angry\" />");
                            break;
                        case 0xE736: //docomo r-mark
                            buff.append("<img src=\"file:///android_asset/emoticons/r-mark.gif\" alt=\"r-mark\" />");
                            break;
                        case 0xE6A2: //docomo cat
                            buff.append("<img src=\"file:///android_asset/emoticons/cat.gif\" alt=\"cat\" />");
                            break;
                        case 0xE6D1: //docomo info01
                            buff.append("<img src=\"file:///android_asset/emoticons/info01.gif\" alt=\"info01\" />");
                            break;
                        case 0xE687: //docomo telephone
                            buff.append("<img src=\"file:///android_asset/emoticons/telephone.gif\" alt=\"telephone\" />");
                            break;
                        case 0xE68C: //docomo cd
                            buff.append("<img src=\"file:///android_asset/emoticons/cd.gif\" alt=\"cd\" />");
                            break;
                        case 0xE70E: //docomo t-shirt
                            buff.append("<img src=\"file:///android_asset/emoticons/t-shirt.gif\" alt=\"t-shirt\" />");
                            break;
                        case 0xE733: //docomo run
                            buff.append("<img src=\"file:///android_asset/emoticons/run.gif\" alt=\"run\" />");
                            break;
                        case 0xE679: //docomo carouselpony
                            buff.append("<img src=\"file:///android_asset/emoticons/carouselpony.gif\" alt=\"carouselpony\" />");
                            break;
                        case 0xE646: //docomo aries
                            buff.append("<img src=\"file:///android_asset/emoticons/aries.gif\" alt=\"aries\" />");
                            break;
                        case 0xE690: //docomo club
                            buff.append("<img src=\"file:///android_asset/emoticons/club.gif\" alt=\"club\" />");
                            break;
                        case 0xE64E: //docomo sagittarius
                            buff.append("<img src=\"file:///android_asset/emoticons/sagittarius.gif\" alt=\"sagittarius\" />");
                            break;
                        case 0xE6F5: //docomo up
                            buff.append("<img src=\"file:///android_asset/emoticons/up.gif\" alt=\"up\" />");
                            break;
                        case 0xE720: //docomo think
                            buff.append("<img src=\"file:///android_asset/emoticons/think.gif\" alt=\"think\" />");
                            break;
                        case 0xE6E2: //docomo one
                            buff.append("<img src=\"file:///android_asset/emoticons/one.gif\" alt=\"one\" />");
                            break;
                        case 0xE6D8: //docomo id
                            buff.append("<img src=\"file:///android_asset/emoticons/id.gif\" alt=\"id\" />");
                            break;
                        case 0xE675: //docomo hairsalon
                            buff.append("<img src=\"file:///android_asset/emoticons/hairsalon.gif\" alt=\"hairsalon\" />");
                            break;
                        case 0xE6B7: //docomo soon
                            buff.append("<img src=\"file:///android_asset/emoticons/soon.gif\" alt=\"soon\" />");
                            break;
                        case 0xE717: //docomo loveletter
                            buff.append("<img src=\"file:///android_asset/emoticons/loveletter.gif\" alt=\"loveletter\" />");
                            break;
                        case 0xE673: //docomo fastfood
                            buff.append("<img src=\"file:///android_asset/emoticons/fastfood.gif\" alt=\"fastfood\" />");
                            break;
                        case 0xE719: //docomo pencil
                            buff.append("<img src=\"file:///android_asset/emoticons/pencil.gif\" alt=\"pencil\" />");
                            break;
                        case 0xE697: //docomo upwardleft
                            buff.append("<img src=\"file:///android_asset/emoticons/upwardleft.gif\" alt=\"upwardleft\" />");
                            break;
                        case 0xE730: //docomo clip
                            buff.append("<img src=\"file:///android_asset/emoticons/clip.gif\" alt=\"clip\" />");
                            break;
                        case 0xE6ED: //docomo heart02
                            buff.append("<img src=\"file:///android_asset/emoticons/heart02.gif\" alt=\"heart02\" />");
                            break;
                        case 0xE69A: //docomo eyeglass
                            buff.append("<img src=\"file:///android_asset/emoticons/eyeglass.gif\" alt=\"eyeglass\" />");
                            break;
                        case 0xE65E: //docomo car
                            buff.append("<img src=\"file:///android_asset/emoticons/car.gif\" alt=\"car\" />");
                            break;
                        case 0xE742: //docomo cherry
                            buff.append("<img src=\"file:///android_asset/emoticons/cherry.gif\" alt=\"cherry\" />");
                            break;
                        case 0xE71C: //docomo sandclock
                            buff.append("<img src=\"file:///android_asset/emoticons/sandclock.gif\" alt=\"sandclock\" />");
                            break;
                        case 0xE735: //docomo recycle
                            buff.append("<img src=\"file:///android_asset/emoticons/recycle.gif\" alt=\"recycle\" />");
                            break;
                        case 0xE752: //docomo delicious
                            buff.append("<img src=\"file:///android_asset/emoticons/delicious.gif\" alt=\"delicious\" />");
                            break;
                        case 0xE69E: //docomo moon2
                            buff.append("<img src=\"file:///android_asset/emoticons/moon2.gif\" alt=\"moon2\" />");
                            break;
                        case 0xE68A: //docomo tv
                            buff.append("<img src=\"file:///android_asset/emoticons/tv.gif\" alt=\"tv\" />");
                            break;
                        case 0xE706: //docomo sweat01
                            buff.append("<img src=\"file:///android_asset/emoticons/sweat01.gif\" alt=\"sweat01\" />");
                            break;
                        case 0xE738: //docomo ban
                            buff.append("<img src=\"file:///android_asset/emoticons/ban.gif\" alt=\"ban\" />");
                            break;
                        case 0xE672: //docomo beer
                            buff.append("<img src=\"file:///android_asset/emoticons/beer.gif\" alt=\"beer\" />");
                            break;
                        case 0xE640: //docomo rain
                            buff.append("<img src=\"file:///android_asset/emoticons/rain.gif\" alt=\"rain\" />");
                            break;
                        case 0xE69F: //docomo moon3
                            buff.append("<img src=\"file:///android_asset/emoticons/moon3.gif\" alt=\"moon3\" />");
                            break;
                        case 0xE657: //docomo ski
                            buff.append("<img src=\"file:///android_asset/emoticons/ski.gif\" alt=\"ski\" />");
                            break;
                        case 0xE70C: //docomo appli01
                            buff.append("<img src=\"file:///android_asset/emoticons/appli01.gif\" alt=\"appli01\" />");
                            break;
                        case 0xE6E5: //docomo four
                            buff.append("<img src=\"file:///android_asset/emoticons/four.gif\" alt=\"four\" />");
                            break;
                        case 0xE699: //docomo shoe
                            buff.append("<img src=\"file:///android_asset/emoticons/shoe.gif\" alt=\"shoe\" />");
                            break;
                        case 0xE63F: //docomo cloud
                            buff.append("<img src=\"file:///android_asset/emoticons/cloud.gif\" alt=\"cloud\" />");
                            break;
                        case 0xE72F: //docomo ng
                            buff.append("<img src=\"file:///android_asset/emoticons/ng.gif\" alt=\"ng\" />");
                            break;
                        case 0xE6A3: //docomo yacht
                            buff.append("<img src=\"file:///android_asset/emoticons/yacht.gif\" alt=\"yacht\" />");
                            break;
                        case 0xE73A: //docomo pass
                            buff.append("<img src=\"file:///android_asset/emoticons/pass.gif\" alt=\"pass\" />");
                            break;
                        case 0xE67C: //docomo drama
                            buff.append("<img src=\"file:///android_asset/emoticons/drama.gif\" alt=\"drama\" />");
                            break;
                        case 0xE727: //docomo good
                            buff.append("<img src=\"file:///android_asset/emoticons/good.gif\" alt=\"good\" />");
                            break;
                        case 0xE6EB: //docomo zero
                            buff.append("<img src=\"file:///android_asset/emoticons/zero.gif\" alt=\"zero\" />");
                            break;
                        case 0xE72C: //docomo catface
                            buff.append("<img src=\"file:///android_asset/emoticons/catface.gif\" alt=\"catface\" />");
                            break;
                        case 0xE6D5: //docomo d-point
                            buff.append("<img src=\"file:///android_asset/emoticons/d-point.gif\" alt=\"d-point\" />");
                            break;
                        case 0xE6F2: //docomo despair
                            buff.append("<img src=\"file:///android_asset/emoticons/despair.gif\" alt=\"despair\" />");
                            break;
                        case 0xE700: //docomo down
                            buff.append("<img src=\"file:///android_asset/emoticons/down.gif\" alt=\"down\" />");
                            break;
                        case 0xE655: //docomo tennis
                            buff.append("<img src=\"file:///android_asset/emoticons/tennis.gif\" alt=\"tennis\" />");
                            break;
                        case 0xE703: //docomo sign02
                            buff.append("<img src=\"file:///android_asset/emoticons/sign02.gif\" alt=\"sign02\" />");
                            break;
                        case 0xE711: //docomo denim
                            buff.append("<img src=\"file:///android_asset/emoticons/denim.gif\" alt=\"denim\" />");
                            break;
                        case 0xE705: //docomo impact
                            buff.append("<img src=\"file:///android_asset/emoticons/impact.gif\" alt=\"impact\" />");
                            break;
                        case 0xE642: //docomo thunder
                            buff.append("<img src=\"file:///android_asset/emoticons/thunder.gif\" alt=\"thunder\" />");
                            break;
                        case 0xE66C: //docomo parking
                            buff.append("<img src=\"file:///android_asset/emoticons/parking.gif\" alt=\"parking\" />");
                            break;
                        case 0xE6F3: //docomo sad
                            buff.append("<img src=\"file:///android_asset/emoticons/sad.gif\" alt=\"sad\" />");
                            break;
                        case 0xE71E: //docomo japanesetea
                            buff.append("<img src=\"file:///android_asset/emoticons/japanesetea.gif\" alt=\"japanesetea\" />");
                            break;
                        case 0xE6FD: //docomo punch
                            buff.append("<img src=\"file:///android_asset/emoticons/punch.gif\" alt=\"punch\" />");
                            break;
                        case 0xE73D: //docomo updown
                            buff.append("<img src=\"file:///android_asset/emoticons/updown.gif\" alt=\"updown\" />");
                            break;
                        case 0xE66F: //docomo restaurant
                            buff.append("<img src=\"file:///android_asset/emoticons/restaurant.gif\" alt=\"restaurant\" />");
                            break;
                        case 0xE66E: //docomo toilet
                            buff.append("<img src=\"file:///android_asset/emoticons/toilet.gif\" alt=\"toilet\" />");
                            break;
                        case 0xE739: //docomo empty
                            buff.append("<img src=\"file:///android_asset/emoticons/empty.gif\" alt=\"empty\" />");
                            break;
                        case 0xE723: //docomo coldsweats02
                            buff.append("<img src=\"file:///android_asset/emoticons/coldsweats02.gif\" alt=\"coldsweats02\" />");
                            break;
                        case 0xE6B9: //docomo end
                            buff.append("<img src=\"file:///android_asset/emoticons/end.gif\" alt=\"end\" />");
                            break;
                        case 0xE67B: //docomo art
                            buff.append("<img src=\"file:///android_asset/emoticons/art.gif\" alt=\"art\" />");
                            break;
                        case 0xE72E: //docomo weep
                            buff.append("<img src=\"file:///android_asset/emoticons/weep.gif\" alt=\"weep\" />");
                            break;
                        case 0xE715: //docomo dollar
                            buff.append("<img src=\"file:///android_asset/emoticons/dollar.gif\" alt=\"dollar\" />");
                            break;
                        case 0xE6CF: //docomo mailto
                            buff.append("<img src=\"file:///android_asset/emoticons/mailto.gif\" alt=\"mailto\" />");
                            break;
                        case 0xE6F8: //docomo cute
                            buff.append("<img src=\"file:///android_asset/emoticons/cute.gif\" alt=\"cute\" />");
                            break;
                        case 0xE6DD: //docomo new
                            buff.append("<img src=\"file:///android_asset/emoticons/new.gif\" alt=\"new\" />");
                            break;
                        case 0xE651: //docomo pisces
                            buff.append("<img src=\"file:///android_asset/emoticons/pisces.gif\" alt=\"pisces\" />");
                            break;
                        case 0xE756: //docomo wine
                            buff.append("<img src=\"file:///android_asset/emoticons/wine.gif\" alt=\"wine\" />");
                            break;
                        case 0xE649: //docomo cancer
                            buff.append("<img src=\"file:///android_asset/emoticons/cancer.gif\" alt=\"cancer\" />");
                            break;
                        case 0xE650: //docomo aquarius
                            buff.append("<img src=\"file:///android_asset/emoticons/aquarius.gif\" alt=\"aquarius\" />");
                            break;
                        case 0xE740: //docomo fuji
                            buff.append("<img src=\"file:///android_asset/emoticons/fuji.gif\" alt=\"fuji\" />");
                            break;
                        case 0xE681: //docomo camera
                            buff.append("<img src=\"file:///android_asset/emoticons/camera.gif\" alt=\"camera\" />");
                            break;
                        case 0xE71F: //docomo watch
                            buff.append("<img src=\"file:///android_asset/emoticons/watch.gif\" alt=\"watch\" />");
                            break;
                        case 0xE6EE: //docomo heart03
                            buff.append("<img src=\"file:///android_asset/emoticons/heart03.gif\" alt=\"heart03\" />");
                            break;
                        case 0xE71A: //docomo crown
                            buff.append("<img src=\"file:///android_asset/emoticons/crown.gif\" alt=\"crown\" />");
                            break;
                        case 0xE6B3: //docomo night
                            buff.append("<img src=\"file:///android_asset/emoticons/night.gif\" alt=\"night\" />");
                            break;
                        case 0xE66B: //docomo gasstation
                            buff.append("<img src=\"file:///android_asset/emoticons/gasstation.gif\" alt=\"gasstation\" />");
                            break;
                        case 0xE692: //docomo ear
                            buff.append("<img src=\"file:///android_asset/emoticons/ear.gif\" alt=\"ear\" />");
                            break;
                        case 0xE685: //docomo present
                            buff.append("<img src=\"file:///android_asset/emoticons/present.gif\" alt=\"present\" />");
                            break;
                        case 0xE6E9: //docomo eight
                            buff.append("<img src=\"file:///android_asset/emoticons/eight.gif\" alt=\"eight\" />");
                            break;
                        case 0xE70F: //docomo moneybag
                            buff.append("<img src=\"file:///android_asset/emoticons/moneybag.gif\" alt=\"moneybag\" />");
                            break;
                        case 0xE749: //docomo riceball
                            buff.append("<img src=\"file:///android_asset/emoticons/riceball.gif\" alt=\"riceball\" />");
                            break;
                        case 0xE6A0: //docomo fullmoon
                            buff.append("<img src=\"file:///android_asset/emoticons/fullmoon.gif\" alt=\"fullmoon\" />");
                            break;
                        case 0xE74D: //docomo bread
                            buff.append("<img src=\"file:///android_asset/emoticons/bread.gif\" alt=\"bread\" />");
                            break;
                        case 0xE665: //docomo postoffice
                            buff.append("<img src=\"file:///android_asset/emoticons/postoffice.gif\" alt=\"postoffice\" />");
                            break;
                        case 0xE677: //docomo movie
                            buff.append("<img src=\"file:///android_asset/emoticons/movie.gif\" alt=\"movie\" />");
                            break;
                        case 0xE668: //docomo atm
                            buff.append("<img src=\"file:///android_asset/emoticons/atm.gif\" alt=\"atm\" />");
                            break;
                        case 0xE688: //docomo mobilephone
                            buff.append("<img src=\"file:///android_asset/emoticons/mobilephone.gif\" alt=\"mobilephone\" />");
                            break;
                        case 0xE6FA: //docomo shine
                            buff.append("<img src=\"file:///android_asset/emoticons/shine.gif\" alt=\"shine\" />");
                            break;
                        case 0xE713: //docomo bell
                            buff.append("<img src=\"file:///android_asset/emoticons/bell.gif\" alt=\"bell\" />");
                            break;
                        case 0xE74B: //docomo bottle
                            buff.append("<img src=\"file:///android_asset/emoticons/bottle.gif\" alt=\"bottle\" />");
                            break;
                        case 0xE754: //docomo horse
                            buff.append("<img src=\"file:///android_asset/emoticons/horse.gif\" alt=\"horse\" />");
                            break;
                        case 0xE751: //docomo fish
                            buff.append("<img src=\"file:///android_asset/emoticons/fish.gif\" alt=\"fish\" />");
                            break;
                        case 0xE659: //docomo motorsports
                            buff.append("<img src=\"file:///android_asset/emoticons/motorsports.gif\" alt=\"motorsports\" />");
                            break;
                        case 0xE6D3: //docomo mail
                            buff.append("<img src=\"file:///android_asset/emoticons/mail.gif\" alt=\"mail\" />");
                            break;
                            // These emoji codepoints are generated by tools/make_emoji in the K-9 source tree
                            // The spaces between the < and the img are a hack to avoid triggering
                            // K-9's 'load images' button

                        case 0xE223: //softbank eight
                            buff.append("<img src=\"file:///android_asset/emoticons/eight.gif\" alt=\"eight\" />");
                            break;
                        case 0xE415: //softbank coldsweats01
                            buff.append("<img src=\"file:///android_asset/emoticons/coldsweats01.gif\" alt=\"coldsweats01\" />");
                            break;
                        case 0xE21F: //softbank four
                            buff.append("<img src=\"file:///android_asset/emoticons/four.gif\" alt=\"four\" />");
                            break;
                        case 0xE125: //softbank ticket
                            buff.append("<img src=\"file:///android_asset/emoticons/ticket.gif\" alt=\"ticket\" />");
                            break;
                        case 0xE148: //softbank book
                            buff.append("<img src=\"file:///android_asset/emoticons/book.gif\" alt=\"book\" />");
                            break;
                        case 0xE242: //softbank cancer
                            buff.append("<img src=\"file:///android_asset/emoticons/cancer.gif\" alt=\"cancer\" />");
                            break;
                        case 0xE31C: //softbank rouge
                            buff.append("<img src=\"file:///android_asset/emoticons/rouge.gif\" alt=\"rouge\" />");
                            break;
                        case 0xE252: //softbank danger
                            buff.append("<img src=\"file:///android_asset/emoticons/danger.gif\" alt=\"danger\" />");
                            break;
                        case 0xE011: //softbank scissors
                            buff.append("<img src=\"file:///android_asset/emoticons/scissors.gif\" alt=\"scissors\" />");
                            break;
                        case 0xE342: //softbank riceball
                            buff.append("<img src=\"file:///android_asset/emoticons/riceball.gif\" alt=\"riceball\" />");
                            break;
                        case 0xE04B: //softbank rain
                            buff.append("<img src=\"file:///android_asset/emoticons/rain.gif\" alt=\"rain\" />");
                            break;
                        case 0xE03E: //softbank note
                            buff.append("<img src=\"file:///android_asset/emoticons/note.gif\" alt=\"note\" />");
                            break;
                        case 0xE43C: //softbank sprinkle
                            buff.append("<img src=\"file:///android_asset/emoticons/sprinkle.gif\" alt=\"sprinkle\" />");
                            break;
                        case 0xE20A: //softbank wheelchair
                            buff.append("<img src=\"file:///android_asset/emoticons/wheelchair.gif\" alt=\"wheelchair\" />");
                            break;
                        case 0xE42A: //softbank basketball
                            buff.append("<img src=\"file:///android_asset/emoticons/basketball.gif\" alt=\"basketball\" />");
                            break;
                        case 0xE03D: //softbank movie
                            buff.append("<img src=\"file:///android_asset/emoticons/movie.gif\" alt=\"movie\" />");
                            break;
                        case 0xE30E: //softbank smoking
                            buff.append("<img src=\"file:///android_asset/emoticons/smoking.gif\" alt=\"smoking\" />");
                            break;
                        case 0xE003: //softbank kissmark
                            buff.append("<img src=\"file:///android_asset/emoticons/kissmark.gif\" alt=\"kissmark\" />");
                            break;
                        case 0xE21C: //softbank one
                            buff.append("<img src=\"file:///android_asset/emoticons/one.gif\" alt=\"one\" />");
                            break;
                        case 0xE237: //softbank upwardleft
                            buff.append("<img src=\"file:///android_asset/emoticons/upwardleft.gif\" alt=\"upwardleft\" />");
                            break;
                        case 0xE407: //softbank sad
                            buff.append("<img src=\"file:///android_asset/emoticons/sad.gif\" alt=\"sad\" />");
                            break;
                        case 0xE03B: //softbank fuji
                            buff.append("<img src=\"file:///android_asset/emoticons/fuji.gif\" alt=\"fuji\" />");
                            break;
                        case 0xE40E: //softbank gawk
                            buff.append("<img src=\"file:///android_asset/emoticons/gawk.gif\" alt=\"gawk\" />");
                            break;
                        case 0xE245: //softbank libra
                            buff.append("<img src=\"file:///android_asset/emoticons/libra.gif\" alt=\"libra\" />");
                            break;
                        case 0xE24A: //softbank pisces
                            buff.append("<img src=\"file:///android_asset/emoticons/pisces.gif\" alt=\"pisces\" />");
                            break;
                        case 0xE443: //softbank typhoon
                            buff.append("<img src=\"file:///android_asset/emoticons/typhoon.gif\" alt=\"typhoon\" />");
                            break;
                        case 0xE052: //softbank dog
                            buff.append("<img src=\"file:///android_asset/emoticons/dog.gif\" alt=\"dog\" />");
                            break;
                        case 0xE244: //softbank virgo
                            buff.append("<img src=\"file:///android_asset/emoticons/virgo.gif\" alt=\"virgo\" />");
                            break;
                        case 0xE523: //softbank chick
                            buff.append("<img src=\"file:///android_asset/emoticons/chick.gif\" alt=\"chick\" />");
                            break;
                        case 0xE023: //softbank heart03
                            buff.append("<img src=\"file:///android_asset/emoticons/heart03.gif\" alt=\"heart03\" />");
                            break;
                        case 0xE325: //softbank bell
                            buff.append("<img src=\"file:///android_asset/emoticons/bell.gif\" alt=\"bell\" />");
                            break;
                        case 0xE239: //softbank downwardleft
                            buff.append("<img src=\"file:///android_asset/emoticons/downwardleft.gif\" alt=\"downwardleft\" />");
                            break;
                        case 0xE20C: //softbank heart
                            buff.append("<img src=\"file:///android_asset/emoticons/heart.gif\" alt=\"heart\" />");
                            break;
                        case 0xE211: //softbank freedial
                            buff.append("<img src=\"file:///android_asset/emoticons/freedial.gif\" alt=\"freedial\" />");
                            break;
                        case 0xE11F: //softbank chair
                            buff.append("<img src=\"file:///android_asset/emoticons/chair.gif\" alt=\"chair\" />");
                            break;
                        case 0xE108: //softbank coldsweats02
                            buff.append("<img src=\"file:///android_asset/emoticons/coldsweats02.gif\" alt=\"coldsweats02\" />");
                            break;
                        case 0xE330: //softbank dash
                            buff.append("<img src=\"file:///android_asset/emoticons/dash.gif\" alt=\"dash\" />");
                            break;
                        case 0xE404: //softbank smile
                            buff.append("<img src=\"file:///android_asset/emoticons/smile.gif\" alt=\"smile\" />");
                            break;
                        case 0xE304: //softbank tulip
                            buff.append("<img src=\"file:///android_asset/emoticons/tulip.gif\" alt=\"tulip\" />");
                            break;
                        case 0xE419: //softbank eye
                            buff.append("<img src=\"file:///android_asset/emoticons/eye.gif\" alt=\"eye\" />");
                            break;
                        case 0xE13D: //softbank thunder
                            buff.append("<img src=\"file:///android_asset/emoticons/thunder.gif\" alt=\"thunder\" />");
                            break;
                        case 0xE013: //softbank ski
                            buff.append("<img src=\"file:///android_asset/emoticons/ski.gif\" alt=\"ski\" />");
                            break;
                        case 0xE136: //softbank bicycle
                            buff.append("<img src=\"file:///android_asset/emoticons/bicycle.gif\" alt=\"bicycle\" />");
                            break;
                        case 0xE059: //softbank angry
                            buff.append("<img src=\"file:///android_asset/emoticons/angry.gif\" alt=\"angry\" />");
                            break;
                        case 0xE01D: //softbank airplane
                            buff.append("<img src=\"file:///android_asset/emoticons/airplane.gif\" alt=\"airplane\" />");
                            break;
                        case 0xE048: //softbank snow
                            buff.append("<img src=\"file:///android_asset/emoticons/snow.gif\" alt=\"snow\" />");
                            break;
                        case 0xE435: //softbank bullettrain
                            buff.append("<img src=\"file:///android_asset/emoticons/bullettrain.gif\" alt=\"bullettrain\" />");
                            break;
                        case 0xE20E: //softbank spade
                            buff.append("<img src=\"file:///android_asset/emoticons/spade.gif\" alt=\"spade\" />");
                            break;
                        case 0xE247: //softbank sagittarius
                            buff.append("<img src=\"file:///android_asset/emoticons/sagittarius.gif\" alt=\"sagittarius\" />");
                            break;
                        case 0xE157: //softbank school
                            buff.append("<img src=\"file:///android_asset/emoticons/school.gif\" alt=\"school\" />");
                            break;
                        case 0xE10F: //softbank flair
                            buff.append("<img src=\"file:///android_asset/emoticons/flair.gif\" alt=\"flair\" />");
                            break;
                        case 0xE502: //softbank art
                            buff.append("<img src=\"file:///android_asset/emoticons/art.gif\" alt=\"art\" />");
                            break;
                        case 0xE338: //softbank japanesetea
                            buff.append("<img src=\"file:///android_asset/emoticons/japanesetea.gif\" alt=\"japanesetea\" />");
                            break;
                        case 0xE34B: //softbank birthday
                            buff.append("<img src=\"file:///android_asset/emoticons/birthday.gif\" alt=\"birthday\" />");
                            break;
                        case 0xE22B: //softbank empty
                            buff.append("<img src=\"file:///android_asset/emoticons/empty.gif\" alt=\"empty\" />");
                            break;
                        case 0xE311: //softbank bomb
                            buff.append("<img src=\"file:///android_asset/emoticons/bomb.gif\" alt=\"bomb\" />");
                            break;
                        case 0xE012: //softbank paper
                            buff.append("<img src=\"file:///android_asset/emoticons/paper.gif\" alt=\"paper\" />");
                            break;
                        case 0xE151: //softbank toilet
                            buff.append("<img src=\"file:///android_asset/emoticons/toilet.gif\" alt=\"toilet\" />");
                            break;
                        case 0xE01A: //softbank horse
                            buff.append("<img src=\"file:///android_asset/emoticons/horse.gif\" alt=\"horse\" />");
                            break;
                        case 0xE03A: //softbank gasstation
                            buff.append("<img src=\"file:///android_asset/emoticons/gasstation.gif\" alt=\"gasstation\" />");
                            break;
                        case 0xE03F: //softbank key
                            buff.append("<img src=\"file:///android_asset/emoticons/key.gif\" alt=\"key\" />");
                            break;
                        case 0xE00D: //softbank punch
                            buff.append("<img src=\"file:///android_asset/emoticons/punch.gif\" alt=\"punch\" />");
                            break;
                        case 0xE24D: //softbank ok
                            buff.append("<img src=\"file:///android_asset/emoticons/ok.gif\" alt=\"ok\" />");
                            break;
                        case 0xE105: //softbank bleah
                            buff.append("<img src=\"file:///android_asset/emoticons/bleah.gif\" alt=\"bleah\" />");
                            break;
                        case 0xE00E: //softbank good
                            buff.append("<img src=\"file:///android_asset/emoticons/good.gif\" alt=\"good\" />");
                            break;
                        case 0xE154: //softbank atm
                            buff.append("<img src=\"file:///android_asset/emoticons/atm.gif\" alt=\"atm\" />");
                            break;
                        case 0xE405: //softbank wink
                            buff.append("<img src=\"file:///android_asset/emoticons/wink.gif\" alt=\"wink\" />");
                            break;
                        case 0xE030: //softbank cherryblossom
                            buff.append("<img src=\"file:///android_asset/emoticons/cherryblossom.gif\" alt=\"cherryblossom\" />");
                            break;
                        case 0xE057: //softbank happy01
                            buff.append("<img src=\"file:///android_asset/emoticons/happy01.gif\" alt=\"happy01\" />");
                            break;
                        case 0xE229: //softbank id
                            buff.append("<img src=\"file:///android_asset/emoticons/id.gif\" alt=\"id\" />");
                            break;
                        case 0xE016: //softbank baseball
                            buff.append("<img src=\"file:///android_asset/emoticons/baseball.gif\" alt=\"baseball\" />");
                            break;
                        case 0xE044: //softbank wine
                            buff.append("<img src=\"file:///android_asset/emoticons/wine.gif\" alt=\"wine\" />");
                            break;
                        case 0xE115: //softbank run
                            buff.append("<img src=\"file:///android_asset/emoticons/run.gif\" alt=\"run\" />");
                            break;
                        case 0xE14F: //softbank parking
                            buff.append("<img src=\"file:///android_asset/emoticons/parking.gif\" alt=\"parking\" />");
                            break;
                        case 0xE327: //softbank heart04
                            buff.append("<img src=\"file:///android_asset/emoticons/heart04.gif\" alt=\"heart04\" />");
                            break;
                        case 0xE014: //softbank golf
                            buff.append("<img src=\"file:///android_asset/emoticons/golf.gif\" alt=\"golf\" />");
                            break;
                        case 0xE021: //softbank sign01
                            buff.append("<img src=\"file:///android_asset/emoticons/sign01.gif\" alt=\"sign01\" />");
                            break;
                        case 0xE30A: //softbank music
                            buff.append("<img src=\"file:///android_asset/emoticons/music.gif\" alt=\"music\" />");
                            break;
                        case 0xE411: //softbank crying
                            buff.append("<img src=\"file:///android_asset/emoticons/crying.gif\" alt=\"crying\" />");
                            break;
                        case 0xE536: //softbank foot
                            buff.append("<img src=\"file:///android_asset/emoticons/foot.gif\" alt=\"foot\" />");
                            break;
                        case 0xE047: //softbank beer
                            buff.append("<img src=\"file:///android_asset/emoticons/beer.gif\" alt=\"beer\" />");
                            break;
                        case 0xE43E: //softbank wave
                            buff.append("<img src=\"file:///android_asset/emoticons/wave.gif\" alt=\"wave\" />");
                            break;
                        case 0xE022: //softbank heart01
                            buff.append("<img src=\"file:///android_asset/emoticons/heart01.gif\" alt=\"heart01\" />");
                            break;
                        case 0xE007: //softbank shoe
                            buff.append("<img src=\"file:///android_asset/emoticons/shoe.gif\" alt=\"shoe\" />");
                            break;
                        case 0xE010: //softbank rock
                            buff.append("<img src=\"file:///android_asset/emoticons/rock.gif\" alt=\"rock\" />");
                            break;
                        case 0xE32E: //softbank shine
                            buff.append("<img src=\"file:///android_asset/emoticons/shine.gif\" alt=\"shine\" />");
                            break;
                        case 0xE055: //softbank penguin
                            buff.append("<img src=\"file:///android_asset/emoticons/penguin.gif\" alt=\"penguin\" />");
                            break;
                        case 0xE03C: //softbank karaoke
                            buff.append("<img src=\"file:///android_asset/emoticons/karaoke.gif\" alt=\"karaoke\" />");
                            break;
                        case 0xE018: //softbank soccer
                            buff.append("<img src=\"file:///android_asset/emoticons/soccer.gif\" alt=\"soccer\" />");
                            break;
                        case 0xE159: //softbank bus
                            buff.append("<img src=\"file:///android_asset/emoticons/bus.gif\" alt=\"bus\" />");
                            break;
                        case 0xE107: //softbank shock
                            buff.append("<img src=\"file:///android_asset/emoticons/shock.gif\" alt=\"shock\" />");
                            break;
                        case 0xE04A: //softbank sun
                            buff.append("<img src=\"file:///android_asset/emoticons/sun.gif\" alt=\"sun\" />");
                            break;
                        case 0xE156: //softbank 24hours
                            buff.append("<img src=\"file:///android_asset/emoticons/24hours.gif\" alt=\"24hours\" />");
                            break;
                        case 0xE110: //softbank clover
                            buff.append("<img src=\"file:///android_asset/emoticons/clover.gif\" alt=\"clover\" />");
                            break;
                        case 0xE034: //softbank ring
                            buff.append("<img src=\"file:///android_asset/emoticons/ring.gif\" alt=\"ring\" />");
                            break;
                        case 0xE24F: //softbank r-mark
                            buff.append("<img src=\"file:///android_asset/emoticons/r-mark.gif\" alt=\"r-mark\" />");
                            break;
                        case 0xE112: //softbank present
                            buff.append("<img src=\"file:///android_asset/emoticons/present.gif\" alt=\"present\" />");
                            break;
                        case 0xE14D: //softbank bank
                            buff.append("<img src=\"file:///android_asset/emoticons/bank.gif\" alt=\"bank\" />");
                            break;
                        case 0xE42E: //softbank rvcar
                            buff.append("<img src=\"file:///android_asset/emoticons/rvcar.gif\" alt=\"rvcar\" />");
                            break;
                        case 0xE13E: //softbank boutique
                            buff.append("<img src=\"file:///android_asset/emoticons/boutique.gif\" alt=\"boutique\" />");
                            break;
                        case 0xE413: //softbank weep
                            buff.append("<img src=\"file:///android_asset/emoticons/weep.gif\" alt=\"weep\" />");
                            break;
                        case 0xE241: //softbank gemini
                            buff.append("<img src=\"file:///android_asset/emoticons/gemini.gif\" alt=\"gemini\" />");
                            break;
                        case 0xE212: //softbank new
                            buff.append("<img src=\"file:///android_asset/emoticons/new.gif\" alt=\"new\" />");
                            break;
                        case 0xE324: //softbank slate
                            buff.append("<img src=\"file:///android_asset/emoticons/slate.gif\" alt=\"slate\" />");
                            break;
                        case 0xE220: //softbank five
                            buff.append("<img src=\"file:///android_asset/emoticons/five.gif\" alt=\"five\" />");
                            break;
                        case 0xE503: //softbank drama
                            buff.append("<img src=\"file:///android_asset/emoticons/drama.gif\" alt=\"drama\" />");
                            break;
                        case 0xE248: //softbank capricornus
                            buff.append("<img src=\"file:///android_asset/emoticons/capricornus.gif\" alt=\"capricornus\" />");
                            break;
                        case 0xE049: //softbank cloud
                            buff.append("<img src=\"file:///android_asset/emoticons/cloud.gif\" alt=\"cloud\" />");
                            break;
                        case 0xE243: //softbank leo
                            buff.append("<img src=\"file:///android_asset/emoticons/leo.gif\" alt=\"leo\" />");
                            break;
                        case 0xE326: //softbank notes
                            buff.append("<img src=\"file:///android_asset/emoticons/notes.gif\" alt=\"notes\" />");
                            break;
                        case 0xE00B: //softbank faxto
                            buff.append("<img src=\"file:///android_asset/emoticons/faxto.gif\" alt=\"faxto\" />");
                            break;
                        case 0xE221: //softbank six
                            buff.append("<img src=\"file:///android_asset/emoticons/six.gif\" alt=\"six\" />");
                            break;
                        case 0xE240: //softbank taurus
                            buff.append("<img src=\"file:///android_asset/emoticons/taurus.gif\" alt=\"taurus\" />");
                            break;
                        case 0xE24E: //softbank copyright
                            buff.append("<img src=\"file:///android_asset/emoticons/copyright.gif\" alt=\"copyright\" />");
                            break;
                        case 0xE224: //softbank nine
                            buff.append("<img src=\"file:///android_asset/emoticons/nine.gif\" alt=\"nine\" />");
                            break;
                        case 0xE008: //softbank camera
                            buff.append("<img src=\"file:///android_asset/emoticons/camera.gif\" alt=\"camera\" />");
                            break;
                        case 0xE01E: //softbank train
                            buff.append("<img src=\"file:///android_asset/emoticons/train.gif\" alt=\"train\" />");
                            break;
                        case 0xE20D: //softbank diamond
                            buff.append("<img src=\"file:///android_asset/emoticons/diamond.gif\" alt=\"diamond\" />");
                            break;
                        case 0xE009: //softbank telephone
                            buff.append("<img src=\"file:///android_asset/emoticons/telephone.gif\" alt=\"telephone\" />");
                            break;
                        case 0xE019: //softbank fish
                            buff.append("<img src=\"file:///android_asset/emoticons/fish.gif\" alt=\"fish\" />");
                            break;
                        case 0xE01C: //softbank yacht
                            buff.append("<img src=\"file:///android_asset/emoticons/yacht.gif\" alt=\"yacht\" />");
                            break;
                        case 0xE40A: //softbank confident
                            buff.append("<img src=\"file:///android_asset/emoticons/confident.gif\" alt=\"confident\" />");
                            break;
                        case 0xE246: //softbank scorpius
                            buff.append("<img src=\"file:///android_asset/emoticons/scorpius.gif\" alt=\"scorpius\" />");
                            break;
                        case 0xE120: //softbank fastfood
                            buff.append("<img src=\"file:///android_asset/emoticons/fastfood.gif\" alt=\"fastfood\" />");
                            break;
                        case 0xE323: //softbank bag
                            buff.append("<img src=\"file:///android_asset/emoticons/bag.gif\" alt=\"bag\" />");
                            break;
                        case 0xE345: //softbank apple
                            buff.append("<img src=\"file:///android_asset/emoticons/apple.gif\" alt=\"apple\" />");
                            break;
                        case 0xE339: //softbank bread
                            buff.append("<img src=\"file:///android_asset/emoticons/bread.gif\" alt=\"bread\" />");
                            break;
                        case 0xE13C: //softbank sleepy
                            buff.append("<img src=\"file:///android_asset/emoticons/sleepy.gif\" alt=\"sleepy\" />");
                            break;
                        case 0xE106: //softbank lovely
                            buff.append("<img src=\"file:///android_asset/emoticons/lovely.gif\" alt=\"lovely\" />");
                            break;
                        case 0xE340: //softbank noodle
                            buff.append("<img src=\"file:///android_asset/emoticons/noodle.gif\" alt=\"noodle\" />");
                            break;
                        case 0xE20F: //softbank club
                            buff.append("<img src=\"file:///android_asset/emoticons/club.gif\" alt=\"club\" />");
                            break;
                        case 0xE114: //softbank search
                            buff.append("<img src=\"file:///android_asset/emoticons/search.gif\" alt=\"search\" />");
                            break;
                        case 0xE10E: //softbank crown
                            buff.append("<img src=\"file:///android_asset/emoticons/crown.gif\" alt=\"crown\" />");
                            break;
                        case 0xE406: //softbank wobbly
                            buff.append("<img src=\"file:///android_asset/emoticons/wobbly.gif\" alt=\"wobbly\" />");
                            break;
                        case 0xE331: //softbank sweat02
                            buff.append("<img src=\"file:///android_asset/emoticons/sweat02.gif\" alt=\"sweat02\" />");
                            break;
                        case 0xE04F: //softbank cat
                            buff.append("<img src=\"file:///android_asset/emoticons/cat.gif\" alt=\"cat\" />");
                            break;
                        case 0xE301: //softbank memo
                            buff.append("<img src=\"file:///android_asset/emoticons/memo.gif\" alt=\"memo\" />");
                            break;
                        case 0xE01B: //softbank car
                            buff.append("<img src=\"file:///android_asset/emoticons/car.gif\" alt=\"car\" />");
                            break;
                        case 0xE314: //softbank ribbon
                            buff.append("<img src=\"file:///android_asset/emoticons/ribbon.gif\" alt=\"ribbon\" />");
                            break;
                        case 0xE315: //softbank secret
                            buff.append("<img src=\"file:///android_asset/emoticons/secret.gif\" alt=\"secret\" />");
                            break;
                        case 0xE236: //softbank up
                            buff.append("<img src=\"file:///android_asset/emoticons/up.gif\" alt=\"up\" />");
                            break;
                        case 0xE208: //softbank nosmoking
                            buff.append("<img src=\"file:///android_asset/emoticons/nosmoking.gif\" alt=\"nosmoking\" />");
                            break;
                        case 0xE006: //softbank t-shirt
                            buff.append("<img src=\"file:///android_asset/emoticons/t-shirt.gif\" alt=\"t-shirt\" />");
                            break;
                        case 0xE12A: //softbank tv
                            buff.append("<img src=\"file:///android_asset/emoticons/tv.gif\" alt=\"tv\" />");
                            break;
                        case 0xE238: //softbank downwardright
                            buff.append("<img src=\"file:///android_asset/emoticons/downwardright.gif\" alt=\"downwardright\" />");
                            break;
                        case 0xE10B: //softbank pig
                            buff.append("<img src=\"file:///android_asset/emoticons/pig.gif\" alt=\"pig\" />");
                            break;
                        case 0xE126: //softbank cd
                            buff.append("<img src=\"file:///android_asset/emoticons/cd.gif\" alt=\"cd\" />");
                            break;
                        case 0xE402: //softbank catface
                            buff.append("<img src=\"file:///android_asset/emoticons/catface.gif\" alt=\"catface\" />");
                            break;
                        case 0xE416: //softbank pout
                            buff.append("<img src=\"file:///android_asset/emoticons/pout.gif\" alt=\"pout\" />");
                            break;
                        case 0xE045: //softbank cafe
                            buff.append("<img src=\"file:///android_asset/emoticons/cafe.gif\" alt=\"cafe\" />");
                            break;
                        case 0xE41B: //softbank ear
                            buff.append("<img src=\"file:///android_asset/emoticons/ear.gif\" alt=\"ear\" />");
                            break;
                        case 0xE23F: //softbank aries
                            buff.append("<img src=\"file:///android_asset/emoticons/aries.gif\" alt=\"aries\" />");
                            break;
                        case 0xE21E: //softbank three
                            buff.append("<img src=\"file:///android_asset/emoticons/three.gif\" alt=\"three\" />");
                            break;
                        case 0xE056: //softbank delicious
                            buff.append("<img src=\"file:///android_asset/emoticons/delicious.gif\" alt=\"delicious\" />");
                            break;
                        case 0xE14E: //softbank signaler
                            buff.append("<img src=\"file:///android_asset/emoticons/signaler.gif\" alt=\"signaler\" />");
                            break;
                        case 0xE155: //softbank hospital
                            buff.append("<img src=\"file:///android_asset/emoticons/hospital.gif\" alt=\"hospital\" />");
                            break;
                        case 0xE033: //softbank xmas
                            buff.append("<img src=\"file:///android_asset/emoticons/xmas.gif\" alt=\"xmas\" />");
                            break;
                        case 0xE22A: //softbank full
                            buff.append("<img src=\"file:///android_asset/emoticons/full.gif\" alt=\"full\" />");
                            break;
                        case 0xE123: //softbank spa
                            buff.append("<img src=\"file:///android_asset/emoticons/spa.gif\" alt=\"spa\" />");
                            break;
                        case 0xE132: //softbank motorsports
                            buff.append("<img src=\"file:///android_asset/emoticons/motorsports.gif\" alt=\"motorsports\" />");
                            break;
                        case 0xE434: //softbank subway
                            buff.append("<img src=\"file:///android_asset/emoticons/subway.gif\" alt=\"subway\" />");
                            break;
                        case 0xE403: //softbank think
                            buff.append("<img src=\"file:///android_asset/emoticons/think.gif\" alt=\"think\" />");
                            break;
                        case 0xE043: //softbank restaurant
                            buff.append("<img src=\"file:///android_asset/emoticons/restaurant.gif\" alt=\"restaurant\" />");
                            break;
                        case 0xE537: //softbank tm
                            buff.append("<img src=\"file:///android_asset/emoticons/tm.gif\" alt=\"tm\" />");
                            break;
                        case 0xE058: //softbank despair
                            buff.append("<img src=\"file:///android_asset/emoticons/despair.gif\" alt=\"despair\" />");
                            break;
                        case 0xE04C: //softbank moon3
                            buff.append("<img src=\"file:///android_asset/emoticons/moon3.gif\" alt=\"moon3\" />");
                            break;
                        case 0xE21D: //softbank two
                            buff.append("<img src=\"file:///android_asset/emoticons/two.gif\" alt=\"two\" />");
                            break;
                        case 0xE202: //softbank ship
                            buff.append("<img src=\"file:///android_asset/emoticons/ship.gif\" alt=\"ship\" />");
                            break;
                        case 0xE30B: //softbank bottle
                            buff.append("<img src=\"file:///android_asset/emoticons/bottle.gif\" alt=\"bottle\" />");
                            break;
                        case 0xE118: //softbank maple
                            buff.append("<img src=\"file:///android_asset/emoticons/maple.gif\" alt=\"maple\" />");
                            break;
                        case 0xE103: //softbank loveletter
                            buff.append("<img src=\"file:///android_asset/emoticons/loveletter.gif\" alt=\"loveletter\" />");
                            break;
                        case 0xE225: //softbank zero
                            buff.append("<img src=\"file:///android_asset/emoticons/zero.gif\" alt=\"zero\" />");
                            break;
                        case 0xE00C: //softbank pc
                            buff.append("<img src=\"file:///android_asset/emoticons/pc.gif\" alt=\"pc\" />");
                            break;
                        case 0xE210: //softbank sharp
                            buff.append("<img src=\"file:///android_asset/emoticons/sharp.gif\" alt=\"sharp\" />");
                            break;
                        case 0xE015: //softbank tennis
                            buff.append("<img src=\"file:///android_asset/emoticons/tennis.gif\" alt=\"tennis\" />");
                            break;
                        case 0xE038: //softbank building
                            buff.append("<img src=\"file:///android_asset/emoticons/building.gif\" alt=\"building\" />");
                            break;
                        case 0xE02D: //softbank clock
                            buff.append("<img src=\"file:///android_asset/emoticons/clock.gif\" alt=\"clock\" />");
                            break;
                        case 0xE334: //softbank annoy
                            buff.append("<img src=\"file:///android_asset/emoticons/annoy.gif\" alt=\"annoy\" />");
                            break;
                        case 0xE153: //softbank postoffice
                            buff.append("<img src=\"file:///android_asset/emoticons/postoffice.gif\" alt=\"postoffice\" />");
                            break;
                        case 0xE222: //softbank seven
                            buff.append("<img src=\"file:///android_asset/emoticons/seven.gif\" alt=\"seven\" />");
                            break;
                        case 0xE12F: //softbank dollar
                            buff.append("<img src=\"file:///android_asset/emoticons/dollar.gif\" alt=\"dollar\" />");
                            break;
                        case 0xE00A: //softbank mobilephone
                            buff.append("<img src=\"file:///android_asset/emoticons/mobilephone.gif\" alt=\"mobilephone\" />");
                            break;
                        case 0xE158: //softbank hotel
                            buff.append("<img src=\"file:///android_asset/emoticons/hotel.gif\" alt=\"hotel\" />");
                            break;
                        case 0xE249: //softbank aquarius
                            buff.append("<img src=\"file:///android_asset/emoticons/aquarius.gif\" alt=\"aquarius\" />");
                            break;
                        case 0xE036: //softbank house
                            buff.append("<img src=\"file:///android_asset/emoticons/house.gif\" alt=\"house\" />");
                            break;
                        case 0xE046: //softbank cake
                            buff.append("<img src=\"file:///android_asset/emoticons/cake.gif\" alt=\"cake\" />");
                            break;
                        case 0xE104: //softbank phoneto
                            buff.append("<img src=\"file:///android_asset/emoticons/phoneto.gif\" alt=\"phoneto\" />");
                            break;
                        case 0xE44B: //softbank night
                            buff.append("<img src=\"file:///android_asset/emoticons/night.gif\" alt=\"night\" />");
                            break;
                        case 0xE313: //softbank hairsalon
                            buff.append("<img src=\"file:///android_asset/emoticons/hairsalon.gif\" alt=\"hairsalon\" />");
                            break;
                            // These emoji codepoints are generated by tools/make_emoji in the K-9 source tree
                            // The spaces between the < and the img are a hack to avoid triggering
                            // K-9's 'load images' button

                        case 0xE488: //kddi sun
                            buff.append("<img src=\"file:///android_asset/emoticons/sun.gif\" alt=\"sun\" />");
                            break;
                        case 0xEA88: //kddi id
                            buff.append("<img src=\"file:///android_asset/emoticons/id.gif\" alt=\"id\" />");
                            break;
                        case 0xE4BA: //kddi baseball
                            buff.append("<img src=\"file:///android_asset/emoticons/baseball.gif\" alt=\"baseball\" />");
                            break;
                        case 0xE525: //kddi four
                            buff.append("<img src=\"file:///android_asset/emoticons/four.gif\" alt=\"four\" />");
                            break;
                        case 0xE578: //kddi free
                            buff.append("<img src=\"file:///android_asset/emoticons/free.gif\" alt=\"free\" />");
                            break;
                        case 0xE4C1: //kddi wine
                            buff.append("<img src=\"file:///android_asset/emoticons/wine.gif\" alt=\"wine\" />");
                            break;
                        case 0xE512: //kddi bell
                            buff.append("<img src=\"file:///android_asset/emoticons/bell.gif\" alt=\"bell\" />");
                            break;
                        case 0xEB83: //kddi rock
                            buff.append("<img src=\"file:///android_asset/emoticons/rock.gif\" alt=\"rock\" />");
                            break;
                        case 0xE4D0: //kddi cake
                            buff.append("<img src=\"file:///android_asset/emoticons/cake.gif\" alt=\"cake\" />");
                            break;
                        case 0xE473: //kddi crying
                            buff.append("<img src=\"file:///android_asset/emoticons/crying.gif\" alt=\"crying\" />");
                            break;
                        case 0xE48C: //kddi rain
                            buff.append("<img src=\"file:///android_asset/emoticons/rain.gif\" alt=\"rain\" />");
                            break;
                        case 0xEAC2: //kddi bearing
                            buff.append("<img src=\"file:///android_asset/emoticons/bearing.gif\" alt=\"bearing\" />");
                            break;
                        case 0xE47E: //kddi nosmoking
                            buff.append("<img src=\"file:///android_asset/emoticons/nosmoking.gif\" alt=\"nosmoking\" />");
                            break;
                        case 0xEAC0: //kddi despair
                            buff.append("<img src=\"file:///android_asset/emoticons/despair.gif\" alt=\"despair\" />");
                            break;
                        case 0xE559: //kddi r-mark
                            buff.append("<img src=\"file:///android_asset/emoticons/r-mark.gif\" alt=\"r-mark\" />");
                            break;
                        case 0xEB2D: //kddi up
                            buff.append("<img src=\"file:///android_asset/emoticons/up.gif\" alt=\"up\" />");
                            break;
                        case 0xEA89: //kddi full
                            buff.append("<img src=\"file:///android_asset/emoticons/full.gif\" alt=\"full\" />");
                            break;
                        case 0xEAC9: //kddi gawk
                            buff.append("<img src=\"file:///android_asset/emoticons/gawk.gif\" alt=\"gawk\" />");
                            break;
                        case 0xEB79: //kddi recycle
                            buff.append("<img src=\"file:///android_asset/emoticons/recycle.gif\" alt=\"recycle\" />");
                            break;
                        case 0xE5AC: //kddi zero
                            buff.append("<img src=\"file:///android_asset/emoticons/zero.gif\" alt=\"zero\" />");
                            break;
                        case 0xEAAE: //kddi japanesetea
                            buff.append("<img src=\"file:///android_asset/emoticons/japanesetea.gif\" alt=\"japanesetea\" />");
                            break;
                        case 0xEB30: //kddi sign03
                            buff.append("<img src=\"file:///android_asset/emoticons/sign03.gif\" alt=\"sign03\" />");
                            break;
                        case 0xE4B6: //kddi soccer
                            buff.append("<img src=\"file:///android_asset/emoticons/soccer.gif\" alt=\"soccer\" />");
                            break;
                        case 0xE556: //kddi downwardleft
                            buff.append("<img src=\"file:///android_asset/emoticons/downwardleft.gif\" alt=\"downwardleft\" />");
                            break;
                        case 0xE4BE: //kddi slate
                            buff.append("<img src=\"file:///android_asset/emoticons/slate.gif\" alt=\"slate\" />");
                            break;
                        case 0xE4A5: //kddi toilet
                            buff.append("<img src=\"file:///android_asset/emoticons/toilet.gif\" alt=\"toilet\" />");
                            break;
                            // Skipping kddi codepoint E523 two
                            // It conflicts with an earlier definition from another carrier:
                            // softbank chick

                        case 0xE496: //kddi scorpius
                            buff.append("<img src=\"file:///android_asset/emoticons/scorpius.gif\" alt=\"scorpius\" />");
                            break;
                        case 0xE4C6: //kddi game
                            buff.append("<img src=\"file:///android_asset/emoticons/game.gif\" alt=\"game\" />");
                            break;
                        case 0xE5A0: //kddi birthday
                            buff.append("<img src=\"file:///android_asset/emoticons/birthday.gif\" alt=\"birthday\" />");
                            break;
                        case 0xE5B8: //kddi pc
                            buff.append("<img src=\"file:///android_asset/emoticons/pc.gif\" alt=\"pc\" />");
                            break;
                        case 0xE516: //kddi hairsalon
                            buff.append("<img src=\"file:///android_asset/emoticons/hairsalon.gif\" alt=\"hairsalon\" />");
                            break;
                        case 0xE475: //kddi sleepy
                            buff.append("<img src=\"file:///android_asset/emoticons/sleepy.gif\" alt=\"sleepy\" />");
                            break;
                        case 0xE4A3: //kddi atm
                            buff.append("<img src=\"file:///android_asset/emoticons/atm.gif\" alt=\"atm\" />");
                            break;
                        case 0xE59A: //kddi basketball
                            buff.append("<img src=\"file:///android_asset/emoticons/basketball.gif\" alt=\"basketball\" />");
                            break;
                        case 0xE497: //kddi sagittarius
                            buff.append("<img src=\"file:///android_asset/emoticons/sagittarius.gif\" alt=\"sagittarius\" />");
                            break;
                        case 0xEACD: //kddi delicious
                            buff.append("<img src=\"file:///android_asset/emoticons/delicious.gif\" alt=\"delicious\" />");
                            break;
                        case 0xE5A8: //kddi newmoon
                            buff.append("<img src=\"file:///android_asset/emoticons/newmoon.gif\" alt=\"newmoon\" />");
                            break;
                        case 0xE49E: //kddi ticket
                            buff.append("<img src=\"file:///android_asset/emoticons/ticket.gif\" alt=\"ticket\" />");
                            break;
                        case 0xE5AE: //kddi wobbly
                            buff.append("<img src=\"file:///android_asset/emoticons/wobbly.gif\" alt=\"wobbly\" />");
                            break;
                        case 0xE4E6: //kddi sweat02
                            buff.append("<img src=\"file:///android_asset/emoticons/sweat02.gif\" alt=\"sweat02\" />");
                            break;
                        case 0xE59E: //kddi event
                            buff.append("<img src=\"file:///android_asset/emoticons/event.gif\" alt=\"event\" />");
                            break;
                        case 0xE4AB: //kddi house
                            buff.append("<img src=\"file:///android_asset/emoticons/house.gif\" alt=\"house\" />");
                            break;
                        case 0xE491: //kddi gemini
                            buff.append("<img src=\"file:///android_asset/emoticons/gemini.gif\" alt=\"gemini\" />");
                            break;
                        case 0xE4C9: //kddi xmas
                            buff.append("<img src=\"file:///android_asset/emoticons/xmas.gif\" alt=\"xmas\" />");
                            break;
                        case 0xE5BE: //kddi note
                            buff.append("<img src=\"file:///android_asset/emoticons/note.gif\" alt=\"note\" />");
                            break;
                        case 0xEB2F: //kddi sign02
                            buff.append("<img src=\"file:///android_asset/emoticons/sign02.gif\" alt=\"sign02\" />");
                            break;
                        case 0xE508: //kddi music
                            buff.append("<img src=\"file:///android_asset/emoticons/music.gif\" alt=\"music\" />");
                            break;
                        case 0xE5DF: //kddi hospital
                            buff.append("<img src=\"file:///android_asset/emoticons/hospital.gif\" alt=\"hospital\" />");
                            break;
                        case 0xE5BC: //kddi subway
                            buff.append("<img src=\"file:///android_asset/emoticons/subway.gif\" alt=\"subway\" />");
                            break;
                        case 0xE5C9: //kddi crown
                            buff.append("<img src=\"file:///android_asset/emoticons/crown.gif\" alt=\"crown\" />");
                            break;
                        case 0xE4BC: //kddi spa
                            buff.append("<img src=\"file:///android_asset/emoticons/spa.gif\" alt=\"spa\" />");
                            break;
                        case 0xE514: //kddi ring
                            buff.append("<img src=\"file:///android_asset/emoticons/ring.gif\" alt=\"ring\" />");
                            break;
                            // Skipping kddi codepoint E502 tv
                            // It conflicts with an earlier definition from another carrier:
                            // softbank art

                        case 0xE4AC: //kddi restaurant
                            buff.append("<img src=\"file:///android_asset/emoticons/restaurant.gif\" alt=\"restaurant\" />");
                            break;
                        case 0xE529: //kddi eight
                            buff.append("<img src=\"file:///android_asset/emoticons/eight.gif\" alt=\"eight\" />");
                            break;
                        case 0xE518: //kddi search
                            buff.append("<img src=\"file:///android_asset/emoticons/search.gif\" alt=\"search\" />");
                            break;
                        case 0xE505: //kddi notes
                            buff.append("<img src=\"file:///android_asset/emoticons/notes.gif\" alt=\"notes\" />");
                            break;
                        case 0xE498: //kddi capricornus
                            buff.append("<img src=\"file:///android_asset/emoticons/capricornus.gif\" alt=\"capricornus\" />");
                            break;
                        case 0xEB7E: //kddi snail
                            buff.append("<img src=\"file:///android_asset/emoticons/snail.gif\" alt=\"snail\" />");
                            break;
                        case 0xEA97: //kddi bottle
                            buff.append("<img src=\"file:///android_asset/emoticons/bottle.gif\" alt=\"bottle\" />");
                            break;
                        case 0xEB08: //kddi phoneto
                            buff.append("<img src=\"file:///android_asset/emoticons/phoneto.gif\" alt=\"phoneto\" />");
                            break;
                        case 0xE4D2: //kddi cherry
                            buff.append("<img src=\"file:///android_asset/emoticons/cherry.gif\" alt=\"cherry\" />");
                            break;
                        case 0xE54D: //kddi downwardright
                            buff.append("<img src=\"file:///android_asset/emoticons/downwardright.gif\" alt=\"downwardright\" />");
                            break;
                        case 0xE5C3: //kddi wink
                            buff.append("<img src=\"file:///android_asset/emoticons/wink.gif\" alt=\"wink\" />");
                            break;
                        case 0xEAAC: //kddi ski
                            buff.append("<img src=\"file:///android_asset/emoticons/ski.gif\" alt=\"ski\" />");
                            break;
                        case 0xE515: //kddi camera
                            buff.append("<img src=\"file:///android_asset/emoticons/camera.gif\" alt=\"camera\" />");
                            break;
                        case 0xE5B6: //kddi t-shirt
                            buff.append("<img src=\"file:///android_asset/emoticons/t-shirt.gif\" alt=\"t-shirt\" />");
                            break;
                        case 0xE5C4: //kddi lovely
                            buff.append("<img src=\"file:///android_asset/emoticons/lovely.gif\" alt=\"lovely\" />");
                            break;
                        case 0xE4AD: //kddi building
                            buff.append("<img src=\"file:///android_asset/emoticons/building.gif\" alt=\"building\" />");
                            break;
                        case 0xE4CE: //kddi maple
                            buff.append("<img src=\"file:///android_asset/emoticons/maple.gif\" alt=\"maple\" />");
                            break;
                        case 0xE5AA: //kddi moon2
                            buff.append("<img src=\"file:///android_asset/emoticons/moon2.gif\" alt=\"moon2\" />");
                            break;
                        case 0xE5B4: //kddi noodle
                            buff.append("<img src=\"file:///android_asset/emoticons/noodle.gif\" alt=\"noodle\" />");
                            break;
                        case 0xE5A6: //kddi scissors
                            buff.append("<img src=\"file:///android_asset/emoticons/scissors.gif\" alt=\"scissors\" />");
                            break;
                        case 0xE4AA: //kddi bank
                            buff.append("<img src=\"file:///android_asset/emoticons/bank.gif\" alt=\"bank\" />");
                            break;
                        case 0xE4B5: //kddi train
                            buff.append("<img src=\"file:///android_asset/emoticons/train.gif\" alt=\"train\" />");
                            break;
                        case 0xE477: //kddi heart03
                            buff.append("<img src=\"file:///android_asset/emoticons/heart03.gif\" alt=\"heart03\" />");
                            break;
                        case 0xE481: //kddi danger
                            buff.append("<img src=\"file:///android_asset/emoticons/danger.gif\" alt=\"danger\" />");
                            break;
                        case 0xE597: //kddi cafe
                            buff.append("<img src=\"file:///android_asset/emoticons/cafe.gif\" alt=\"cafe\" />");
                            break;
                        case 0xEB2B: //kddi shoe
                            buff.append("<img src=\"file:///android_asset/emoticons/shoe.gif\" alt=\"shoe\" />");
                            break;
                        case 0xEB7C: //kddi wave
                            buff.append("<img src=\"file:///android_asset/emoticons/wave.gif\" alt=\"wave\" />");
                            break;
                        case 0xE471: //kddi happy01
                            buff.append("<img src=\"file:///android_asset/emoticons/happy01.gif\" alt=\"happy01\" />");
                            break;
                        case 0xE4CA: //kddi cherryblossom
                            buff.append("<img src=\"file:///android_asset/emoticons/cherryblossom.gif\" alt=\"cherryblossom\" />");
                            break;
                        case 0xE4D5: //kddi riceball
                            buff.append("<img src=\"file:///android_asset/emoticons/riceball.gif\" alt=\"riceball\" />");
                            break;
                        case 0xE587: //kddi wrench
                            buff.append("<img src=\"file:///android_asset/emoticons/wrench.gif\" alt=\"wrench\" />");
                            break;
                        case 0xEB2A: //kddi foot
                            buff.append("<img src=\"file:///android_asset/emoticons/foot.gif\" alt=\"foot\" />");
                            break;
                        case 0xE47D: //kddi smoking
                            buff.append("<img src=\"file:///android_asset/emoticons/smoking.gif\" alt=\"smoking\" />");
                            break;
                        case 0xE4DC: //kddi penguin
                            buff.append("<img src=\"file:///android_asset/emoticons/penguin.gif\" alt=\"penguin\" />");
                            break;
                        case 0xE4B3: //kddi airplane
                            buff.append("<img src=\"file:///android_asset/emoticons/airplane.gif\" alt=\"airplane\" />");
                            break;
                        case 0xE4DE: //kddi pig
                            buff.append("<img src=\"file:///android_asset/emoticons/pig.gif\" alt=\"pig\" />");
                            break;
                        case 0xE59B: //kddi pocketbell
                            buff.append("<img src=\"file:///android_asset/emoticons/pocketbell.gif\" alt=\"pocketbell\" />");
                            break;
                        case 0xE4AF: //kddi bus
                            buff.append("<img src=\"file:///android_asset/emoticons/bus.gif\" alt=\"bus\" />");
                            break;
                        case 0xE4A6: //kddi parking
                            buff.append("<img src=\"file:///android_asset/emoticons/parking.gif\" alt=\"parking\" />");
                            break;
                        case 0xE486: //kddi moon3
                            buff.append("<img src=\"file:///android_asset/emoticons/moon3.gif\" alt=\"moon3\" />");
                            break;
                        case 0xE5A4: //kddi eye
                            buff.append("<img src=\"file:///android_asset/emoticons/eye.gif\" alt=\"eye\" />");
                            break;
                        case 0xE50C: //kddi cd
                            buff.append("<img src=\"file:///android_asset/emoticons/cd.gif\" alt=\"cd\" />");
                            break;
                        case 0xE54C: //kddi upwardleft
                            buff.append("<img src=\"file:///android_asset/emoticons/upwardleft.gif\" alt=\"upwardleft\" />");
                            break;
                        case 0xEA82: //kddi ship
                            buff.append("<img src=\"file:///android_asset/emoticons/ship.gif\" alt=\"ship\" />");
                            break;
                        case 0xE4B1: //kddi car
                            buff.append("<img src=\"file:///android_asset/emoticons/car.gif\" alt=\"car\" />");
                            break;
                        case 0xEB80: //kddi smile
                            buff.append("<img src=\"file:///android_asset/emoticons/smile.gif\" alt=\"smile\" />");
                            break;
                        case 0xE5B0: //kddi impact
                            buff.append("<img src=\"file:///android_asset/emoticons/impact.gif\" alt=\"impact\" />");
                            break;
                        case 0xE504: //kddi moneybag
                            buff.append("<img src=\"file:///android_asset/emoticons/moneybag.gif\" alt=\"moneybag\" />");
                            break;
                        case 0xE4B9: //kddi motorsports
                            buff.append("<img src=\"file:///android_asset/emoticons/motorsports.gif\" alt=\"motorsports\" />");
                            break;
                        case 0xE494: //kddi virgo
                            buff.append("<img src=\"file:///android_asset/emoticons/virgo.gif\" alt=\"virgo\" />");
                            break;
                        case 0xE595: //kddi heart01
                            buff.append("<img src=\"file:///android_asset/emoticons/heart01.gif\" alt=\"heart01\" />");
                            break;
                        case 0xEB03: //kddi pen
                            buff.append("<img src=\"file:///android_asset/emoticons/pen.gif\" alt=\"pen\" />");
                            break;
                        case 0xE57D: //kddi yen
                            buff.append("<img src=\"file:///android_asset/emoticons/yen.gif\" alt=\"yen\" />");
                            break;
                        case 0xE598: //kddi mist
                            buff.append("<img src=\"file:///android_asset/emoticons/mist.gif\" alt=\"mist\" />");
                            break;
                        case 0xE5A2: //kddi diamond
                            buff.append("<img src=\"file:///android_asset/emoticons/diamond.gif\" alt=\"diamond\" />");
                            break;
                        case 0xE4A4: //kddi 24hours
                            buff.append("<img src=\"file:///android_asset/emoticons/24hours.gif\" alt=\"24hours\" />");
                            break;
                        case 0xE524: //kddi three
                            buff.append("<img src=\"file:///android_asset/emoticons/three.gif\" alt=\"three\" />");
                            break;
                        case 0xEB7B: //kddi updown
                            buff.append("<img src=\"file:///android_asset/emoticons/updown.gif\" alt=\"updown\" />");
                            break;
                        case 0xE5A1: //kddi spade
                            buff.append("<img src=\"file:///android_asset/emoticons/spade.gif\" alt=\"spade\" />");
                            break;
                        case 0xE495: //kddi libra
                            buff.append("<img src=\"file:///android_asset/emoticons/libra.gif\" alt=\"libra\" />");
                            break;
                        case 0xE588: //kddi mobilephone
                            buff.append("<img src=\"file:///android_asset/emoticons/mobilephone.gif\" alt=\"mobilephone\" />");
                            break;
                        case 0xE599: //kddi golf
                            buff.append("<img src=\"file:///android_asset/emoticons/golf.gif\" alt=\"golf\" />");
                            break;
                        case 0xE520: //kddi faxto
                            buff.append("<img src=\"file:///android_asset/emoticons/faxto.gif\" alt=\"faxto\" />");
                            break;
                            // Skipping kddi codepoint E503 karaoke
                            // It conflicts with an earlier definition from another carrier:
                            // softbank drama

                        case 0xE4D6: //kddi fastfood
                            buff.append("<img src=\"file:///android_asset/emoticons/fastfood.gif\" alt=\"fastfood\" />");
                            break;
                        case 0xE4A1: //kddi pencil
                            buff.append("<img src=\"file:///android_asset/emoticons/pencil.gif\" alt=\"pencil\" />");
                            break;
                        case 0xE522: //kddi one
                            buff.append("<img src=\"file:///android_asset/emoticons/one.gif\" alt=\"one\" />");
                            break;
                        case 0xEB84: //kddi sharp
                            buff.append("<img src=\"file:///android_asset/emoticons/sharp.gif\" alt=\"sharp\" />");
                            break;
                        case 0xE476: //kddi flair
                            buff.append("<img src=\"file:///android_asset/emoticons/flair.gif\" alt=\"flair\" />");
                            break;
                        case 0xE46B: //kddi run
                            buff.append("<img src=\"file:///android_asset/emoticons/run.gif\" alt=\"run\" />");
                            break;
                        case 0xEAF5: //kddi drama
                            buff.append("<img src=\"file:///android_asset/emoticons/drama.gif\" alt=\"drama\" />");
                            break;
                        case 0xEAB9: //kddi apple
                            buff.append("<img src=\"file:///android_asset/emoticons/apple.gif\" alt=\"apple\" />");
                            break;
                        case 0xE4EB: //kddi kissmark
                            buff.append("<img src=\"file:///android_asset/emoticons/kissmark.gif\" alt=\"kissmark\" />");
                            break;
                        case 0xE55D: //kddi enter
                            buff.append("<img src=\"file:///android_asset/emoticons/enter.gif\" alt=\"enter\" />");
                            break;
                        case 0xE59F: //kddi ribbon
                            buff.append("<img src=\"file:///android_asset/emoticons/ribbon.gif\" alt=\"ribbon\" />");
                            break;
                        case 0xE526: //kddi five
                            buff.append("<img src=\"file:///android_asset/emoticons/five.gif\" alt=\"five\" />");
                            break;
                        case 0xE571: //kddi gasstation
                            buff.append("<img src=\"file:///android_asset/emoticons/gasstation.gif\" alt=\"gasstation\" />");
                            break;
                        case 0xE517: //kddi movie
                            buff.append("<img src=\"file:///android_asset/emoticons/movie.gif\" alt=\"movie\" />");
                            break;
                        case 0xE4B8: //kddi snowboard
                            buff.append("<img src=\"file:///android_asset/emoticons/snowboard.gif\" alt=\"snowboard\" />");
                            break;
                        case 0xEAE8: //kddi sprinkle
                            buff.append("<img src=\"file:///android_asset/emoticons/sprinkle.gif\" alt=\"sprinkle\" />");
                            break;
                        case 0xEA80: //kddi school
                            buff.append("<img src=\"file:///android_asset/emoticons/school.gif\" alt=\"school\" />");
                            break;
                        case 0xE47C: //kddi sandclock
                            buff.append("<img src=\"file:///android_asset/emoticons/sandclock.gif\" alt=\"sandclock\" />");
                            break;
                        case 0xEB31: //kddi sign05
                            buff.append("<img src=\"file:///android_asset/emoticons/sign05.gif\" alt=\"sign05\" />");
                            break;
                        case 0xE5AB: //kddi clear
                            buff.append("<img src=\"file:///android_asset/emoticons/clear.gif\" alt=\"clear\" />");
                            break;
                        case 0xE5DE: //kddi postoffice
                            buff.append("<img src=\"file:///android_asset/emoticons/postoffice.gif\" alt=\"postoffice\" />");
                            break;
                        case 0xEB62: //kddi mailto
                            buff.append("<img src=\"file:///android_asset/emoticons/mailto.gif\" alt=\"mailto\" />");
                            break;
                        case 0xE528: //kddi seven
                            buff.append("<img src=\"file:///android_asset/emoticons/seven.gif\" alt=\"seven\" />");
                            break;
                        case 0xE4C2: //kddi bar
                            buff.append("<img src=\"file:///android_asset/emoticons/bar.gif\" alt=\"bar\" />");
                            break;
                        case 0xE487: //kddi thunder
                            buff.append("<img src=\"file:///android_asset/emoticons/thunder.gif\" alt=\"thunder\" />");
                            break;
                        case 0xE5A9: //kddi moon1
                            buff.append("<img src=\"file:///android_asset/emoticons/moon1.gif\" alt=\"moon1\" />");
                            break;
                        case 0xEB7A: //kddi leftright
                            buff.append("<img src=\"file:///android_asset/emoticons/leftright.gif\" alt=\"leftright\" />");
                            break;
                        case 0xE513: //kddi clover
                            buff.append("<img src=\"file:///android_asset/emoticons/clover.gif\" alt=\"clover\" />");
                            break;
                        case 0xE492: //kddi cancer
                            buff.append("<img src=\"file:///android_asset/emoticons/cancer.gif\" alt=\"cancer\" />");
                            break;
                        case 0xEB78: //kddi loveletter
                            buff.append("<img src=\"file:///android_asset/emoticons/loveletter.gif\" alt=\"loveletter\" />");
                            break;
                        case 0xE4E0: //kddi chick
                            buff.append("<img src=\"file:///android_asset/emoticons/chick.gif\" alt=\"chick\" />");
                            break;
                        case 0xE4CF: //kddi present
                            buff.append("<img src=\"file:///android_asset/emoticons/present.gif\" alt=\"present\" />");
                            break;
                        case 0xE478: //kddi heart04
                            buff.append("<img src=\"file:///android_asset/emoticons/heart04.gif\" alt=\"heart04\" />");
                            break;
                        case 0xEAC3: //kddi sad
                            buff.append("<img src=\"file:///android_asset/emoticons/sad.gif\" alt=\"sad\" />");
                            break;
                        case 0xE52A: //kddi nine
                            buff.append("<img src=\"file:///android_asset/emoticons/nine.gif\" alt=\"nine\" />");
                            break;
                        case 0xE482: //kddi sign01
                            buff.append("<img src=\"file:///android_asset/emoticons/sign01.gif\" alt=\"sign01\" />");
                            break;
                        case 0xEABF: //kddi catface
                            buff.append("<img src=\"file:///android_asset/emoticons/catface.gif\" alt=\"catface\" />");
                            break;
                        case 0xE527: //kddi six
                            buff.append("<img src=\"file:///android_asset/emoticons/six.gif\" alt=\"six\" />");
                            break;
                        case 0xE52C: //kddi mobaq
                            buff.append("<img src=\"file:///android_asset/emoticons/mobaq.gif\" alt=\"mobaq\" />");
                            break;
                        case 0xE485: //kddi snow
                            buff.append("<img src=\"file:///android_asset/emoticons/snow.gif\" alt=\"snow\" />");
                            break;
                        case 0xE4B7: //kddi tennis
                            buff.append("<img src=\"file:///android_asset/emoticons/tennis.gif\" alt=\"tennis\" />");
                            break;
                        case 0xE5BD: //kddi fuji
                            buff.append("<img src=\"file:///android_asset/emoticons/fuji.gif\" alt=\"fuji\" />");
                            break;
                        case 0xE558: //kddi copyright
                            buff.append("<img src=\"file:///android_asset/emoticons/copyright.gif\" alt=\"copyright\" />");
                            break;
                        case 0xE4D8: //kddi horse
                            buff.append("<img src=\"file:///android_asset/emoticons/horse.gif\" alt=\"horse\" />");
                            break;
                        case 0xE4B0: //kddi bullettrain
                            buff.append("<img src=\"file:///android_asset/emoticons/bullettrain.gif\" alt=\"bullettrain\" />");
                            break;
                        case 0xE596: //kddi telephone
                            buff.append("<img src=\"file:///android_asset/emoticons/telephone.gif\" alt=\"telephone\" />");
                            break;
                        case 0xE48F: //kddi aries
                            buff.append("<img src=\"file:///android_asset/emoticons/aries.gif\" alt=\"aries\" />");
                            break;
                        case 0xE46A: //kddi signaler
                            buff.append("<img src=\"file:///android_asset/emoticons/signaler.gif\" alt=\"signaler\" />");
                            break;
                        case 0xE472: //kddi angry
                            buff.append("<img src=\"file:///android_asset/emoticons/angry.gif\" alt=\"angry\" />");
                            break;
                        case 0xE54E: //kddi tm
                            buff.append("<img src=\"file:///android_asset/emoticons/tm.gif\" alt=\"tm\" />");
                            break;
                        case 0xE51A: //kddi boutique
                            buff.append("<img src=\"file:///android_asset/emoticons/boutique.gif\" alt=\"boutique\" />");
                            break;
                        case 0xE493: //kddi leo
                            buff.append("<img src=\"file:///android_asset/emoticons/leo.gif\" alt=\"leo\" />");
                            break;
                        case 0xE5A3: //kddi club
                            buff.append("<img src=\"file:///android_asset/emoticons/club.gif\" alt=\"club\" />");
                            break;
                        case 0xE499: //kddi aquarius
                            buff.append("<img src=\"file:///android_asset/emoticons/aquarius.gif\" alt=\"aquarius\" />");
                            break;
                        case 0xE4AE: //kddi bicycle
                            buff.append("<img src=\"file:///android_asset/emoticons/bicycle.gif\" alt=\"bicycle\" />");
                            break;
                        case 0xE4E7: //kddi bleah
                            buff.append("<img src=\"file:///android_asset/emoticons/bleah.gif\" alt=\"bleah\" />");
                            break;
                        case 0xE49F: //kddi book
                            buff.append("<img src=\"file:///android_asset/emoticons/book.gif\" alt=\"book\" />");
                            break;
                        case 0xE5AD: //kddi ok
                            buff.append("<img src=\"file:///android_asset/emoticons/ok.gif\" alt=\"ok\" />");
                            break;
                        case 0xE5A7: //kddi paper
                            buff.append("<img src=\"file:///android_asset/emoticons/paper.gif\" alt=\"paper\" />");
                            break;
                        case 0xE4E5: //kddi annoy
                            buff.append("<img src=\"file:///android_asset/emoticons/annoy.gif\" alt=\"annoy\" />");
                            break;
                        case 0xE4A0: //kddi clip
                            buff.append("<img src=\"file:///android_asset/emoticons/clip.gif\" alt=\"clip\" />");
                            break;
                        case 0xE509: //kddi rouge
                            buff.append("<img src=\"file:///android_asset/emoticons/rouge.gif\" alt=\"rouge\" />");
                            break;
                        case 0xEAAF: //kddi bread
                            buff.append("<img src=\"file:///android_asset/emoticons/bread.gif\" alt=\"bread\" />");
                            break;
                        case 0xE519: //kddi key
                            buff.append("<img src=\"file:///android_asset/emoticons/key.gif\" alt=\"key\" />");
                            break;
                        case 0xE594: //kddi clock
                            buff.append("<img src=\"file:///android_asset/emoticons/clock.gif\" alt=\"clock\" />");
                            break;
                        case 0xEB7D: //kddi bud
                            buff.append("<img src=\"file:///android_asset/emoticons/bud.gif\" alt=\"bud\" />");
                            break;
                        case 0xEA8A: //kddi empty
                            buff.append("<img src=\"file:///android_asset/emoticons/empty.gif\" alt=\"empty\" />");
                            break;
                        case 0xE5B5: //kddi new
                            buff.append("<img src=\"file:///android_asset/emoticons/new.gif\" alt=\"new\" />");
                            break;
                        case 0xE47A: //kddi bomb
                            buff.append("<img src=\"file:///android_asset/emoticons/bomb.gif\" alt=\"bomb\" />");
                            break;
                        case 0xE5C6: //kddi coldsweats02
                            buff.append("<img src=\"file:///android_asset/emoticons/coldsweats02.gif\" alt=\"coldsweats02\" />");
                            break;
                        case 0xE49A: //kddi pisces
                            buff.append("<img src=\"file:///android_asset/emoticons/pisces.gif\" alt=\"pisces\" />");
                            break;
                        case 0xE4F3: //kddi punch
                            buff.append("<img src=\"file:///android_asset/emoticons/punch.gif\" alt=\"punch\" />");
                            break;
                        case 0xEB5D: //kddi pout
                            buff.append("<img src=\"file:///android_asset/emoticons/pout.gif\" alt=\"pout\" />");
                            break;
                        case 0xE469: //kddi typhoon
                            buff.append("<img src=\"file:///android_asset/emoticons/typhoon.gif\" alt=\"typhoon\" />");
                            break;
                        case 0xE5B1: //kddi sweat01
                            buff.append("<img src=\"file:///android_asset/emoticons/sweat01.gif\" alt=\"sweat01\" />");
                            break;
                        case 0xE4C7: //kddi dollar
                            buff.append("<img src=\"file:///android_asset/emoticons/dollar.gif\" alt=\"dollar\" />");
                            break;
                        case 0xE5C5: //kddi shock
                            buff.append("<img src=\"file:///android_asset/emoticons/shock.gif\" alt=\"shock\" />");
                            break;
                        case 0xE4F9: //kddi good
                            buff.append("<img src=\"file:///android_asset/emoticons/good.gif\" alt=\"good\" />");
                            break;
                        case 0xE4F1: //kddi secret
                            buff.append("<img src=\"file:///android_asset/emoticons/secret.gif\" alt=\"secret\" />");
                            break;
                        case 0xE4E4: //kddi tulip
                            buff.append("<img src=\"file:///android_asset/emoticons/tulip.gif\" alt=\"tulip\" />");
                            break;
                        case 0xEA81: //kddi hotel
                            buff.append("<img src=\"file:///android_asset/emoticons/hotel.gif\" alt=\"hotel\" />");
                            break;
                        case 0xE4FE: //kddi eyeglass
                            buff.append("<img src=\"file:///android_asset/emoticons/eyeglass.gif\" alt=\"eyeglass\" />");
                            break;
                        case 0xEAF1: //kddi night
                            buff.append("<img src=\"file:///android_asset/emoticons/night.gif\" alt=\"night\" />");
                            break;
                        case 0xE555: //kddi upwardright
                            buff.append("<img src=\"file:///android_asset/emoticons/upwardright.gif\" alt=\"upwardright\" />");
                            break;
                        case 0xEB2E: //kddi down
                            buff.append("<img src=\"file:///android_asset/emoticons/down.gif\" alt=\"down\" />");
                            break;
                        case 0xE4DB: //kddi cat
                            buff.append("<img src=\"file:///android_asset/emoticons/cat.gif\" alt=\"cat\" />");
                            break;
                        case 0xE59C: //kddi art
                            buff.append("<img src=\"file:///android_asset/emoticons/art.gif\" alt=\"art\" />");
                            break;
                        case 0xEB69: //kddi weep
                            buff.append("<img src=\"file:///android_asset/emoticons/weep.gif\" alt=\"weep\" />");
                            break;
                        case 0xE4F4: //kddi dash
                            buff.append("<img src=\"file:///android_asset/emoticons/dash.gif\" alt=\"dash\" />");
                            break;
                        case 0xE490: //kddi taurus
                            buff.append("<img src=\"file:///android_asset/emoticons/taurus.gif\" alt=\"taurus\" />");
                            break;
                        case 0xE57A: //kddi watch
                            buff.append("<img src=\"file:///android_asset/emoticons/watch.gif\" alt=\"watch\" />");
                            break;
                        case 0xEB2C: //kddi flag
                            buff.append("<img src=\"file:///android_asset/emoticons/flag.gif\" alt=\"flag\" />");
                            break;
                        case 0xEB77: //kddi denim
                            buff.append("<img src=\"file:///android_asset/emoticons/denim.gif\" alt=\"denim\" />");
                            break;
                        case 0xEAC5: //kddi confident
                            buff.append("<img src=\"file:///android_asset/emoticons/confident.gif\" alt=\"confident\" />");
                            break;
                        case 0xE4B4: //kddi yacht
                            buff.append("<img src=\"file:///android_asset/emoticons/yacht.gif\" alt=\"yacht\" />");
                            break;
                        case 0xE49C: //kddi bag
                            buff.append("<img src=\"file:///android_asset/emoticons/bag.gif\" alt=\"bag\" />");
                            break;
                        case 0xE5A5: //kddi ear
                            buff.append("<img src=\"file:///android_asset/emoticons/ear.gif\" alt=\"ear\" />");
                            break;
                        case 0xE4E1: //kddi dog
                            buff.append("<img src=\"file:///android_asset/emoticons/dog.gif\" alt=\"dog\" />");
                            break;
                        case 0xE521: //kddi mail
                            buff.append("<img src=\"file:///android_asset/emoticons/mail.gif\" alt=\"mail\" />");
                            break;
                        case 0xEB35: //kddi banana
                            buff.append("<img src=\"file:///android_asset/emoticons/banana.gif\" alt=\"banana\" />");
                            break;
                        case 0xEAA5: //kddi heart
                            buff.append("<img src=\"file:///android_asset/emoticons/heart.gif\" alt=\"heart\" />");
                            break;
                        case 0xE47F: //kddi wheelchair
                            buff.append("<img src=\"file:///android_asset/emoticons/wheelchair.gif\" alt=\"wheelchair\" />");
                            break;
                        case 0xEB75: //kddi heart02
                            buff.append("<img src=\"file:///android_asset/emoticons/heart02.gif\" alt=\"heart02\" />");
                            break;
                        case 0xE48D: //kddi cloud
                            buff.append("<img src=\"file:///android_asset/emoticons/cloud.gif\" alt=\"cloud\" />");
                            break;
                        case 0xE4C3: //kddi beer
                            buff.append("<img src=\"file:///android_asset/emoticons/beer.gif\" alt=\"beer\" />");
                            break;
                        case 0xEAAB: //kddi shine
                            buff.append("<img src=\"file:///android_asset/emoticons/shine.gif\" alt=\"shine\" />");
                            break;
                        case 0xEA92: //kddi memo
                            buff.append("<img src=\"file:///android_asset/emoticons/memo.gif\" alt=\"memo\" />");
                            break;
                        default:
                            buff.append((char)c);
                    }//switch
                }
            }
            catch (IOException e)
            {
                //Should never happen
                Log.e(K9.LOG_TAG, null, e);
            }

            return buff.toString();
        }

        @Override
        public boolean isInTopGroup()
        {
            return inTopGroup;
        }

        public void setInTopGroup(boolean inTopGroup)
        {
            this.inTopGroup = inTopGroup;
        }
    }

    public class LocalTextBody extends TextBody
    {
        private String mBodyForDisplay;

        public LocalTextBody(String body)
        {
            super(body);
        }

        public LocalTextBody(String body, String bodyForDisplay) {
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

        LocalMessage(String uid, Folder folder) {
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
                execute(true, new DbCallback<Void>()
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
                execute(true, new DbCallback<Void>()
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
                execute(true, new DbCallback<Void>()
                {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException,
                        UnavailableStorageException
                    {
                        try
                        {
                            updateFolderCountsOnFlag(Flag.X_DESTROYED, true);
                            ((LocalFolder) mFolder).deleteAttachments(mId);
                            mDb.execSQL("DELETE FROM messages WHERE id = ?", new Object[] { mId });
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

        private void updateFolderCountsOnFlag(Flag flag, boolean set) {
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

    public class LocalAttachmentBodyPart extends MimeBodyPart
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
