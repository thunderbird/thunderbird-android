package com.fsck.k9.mailstore;


import java.io.File;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.fsck.k9.K9;
import com.fsck.k9.helper.FileHelper;
import com.fsck.k9.mail.MessagingException;
import net.thunderbird.core.logging.legacy.Log;
import net.thunderbird.core.preference.GeneralSettingsManager;

import static java.lang.System.currentTimeMillis;


public class LockableDatabase {

    /**
     * Callback interface for DB operations. Concept is similar to Spring
     * HibernateCallback.
     *
     * @param <T>
     *            Return value type for {@link #doDbWork(SQLiteDatabase)}
     */
    public interface DbCallback<T> {
        /**
         * @param db
         *            The locked database on which the work should occur. Never
         *            <code>null</code>.
         * @return Any relevant data. Can be <code>null</code>.
         */
        T doDbWork(SQLiteDatabase db) throws MessagingException;
    }

    public interface SchemaDefinition {
        int getVersion();

        /**
         * @param db Never <code>null</code>.
         */
        void doDbUpgrade(SQLiteDatabase db);
    }

    private SQLiteDatabase mDb;
    /**
     * Reentrant read lock
     */
    private final Lock mReadLock;
    /**
     * Reentrant write lock (if you lock it 2x from the same thread, you have to
     * unlock it 2x to release it)
     */
    private final Lock mWriteLock;

    {
        final ReadWriteLock lock = new ReentrantReadWriteLock(true);
        mReadLock = lock.readLock();
        mWriteLock = lock.writeLock();
    }

    private Context context;
    private final StorageFilesProvider storageFilesProvider;

    /**
     * {@link ThreadLocal} to check whether a DB transaction is occurring in the
     * current {@link Thread}.
     *
     * @see #execute(boolean, DbCallback)
     */
    private ThreadLocal<Boolean> inTransaction = new ThreadLocal<>();

    private SchemaDefinition mSchemaDefinition;
    private GeneralSettingsManager generalSettingsManager;

    public LockableDatabase(Context context, StorageFilesProvider storageFilesProvider,
            SchemaDefinition schemaDefinition, GeneralSettingsManager generalSettingsManager) {
        this.context = context;
        this.storageFilesProvider = storageFilesProvider;
        this.mSchemaDefinition = schemaDefinition;
        this.generalSettingsManager = generalSettingsManager;
    }

    /**
     * Lock the storage for shared operations (concurrent threads are allowed to
     * run simultaneously).
     *
     * <p>
     * You <strong>have to</strong> invoke {@link #unlockRead()} when you're
     * done with the storage.
     * </p>
     */
    protected void lockRead() {
        mReadLock.lock();
    }

    protected void unlockRead() {
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
     */
    private void lockWrite() {
        mWriteLock.lock();
    }

    private void unlockWrite() {
        mWriteLock.unlock();
    }

    /**
     * Execute a DB callback in a shared context (doesn't prevent concurrent
     * shared executions), taking care of locking the DB storage.
     *
     * <p>
     * Can be instructed to start a transaction if none is currently active in
     * the current thread. Callback will participate in any active transaction (no
     * inner transaction created).
     * </p>
     *
     * @param transactional
     *            <code>true</code> the callback must be executed in a
     *            transactional context.
     * @param callback
     *            Never <code>null</code>.
     * @return Whatever {@link DbCallback#doDbWork(SQLiteDatabase)} returns.
     */
    public <T> T execute(final boolean transactional, final DbCallback<T> callback) throws MessagingException {
        lockRead();
        final boolean doTransaction = transactional && inTransaction.get() == null;
        try {
            final boolean debug = generalSettingsManager.getConfig().getDebugging().isDebugLoggingEnabled();
            if (doTransaction) {
                inTransaction.set(Boolean.TRUE);
                mDb.beginTransaction();
            }
            try {
                final T result = callback.doDbWork(mDb);
                if (doTransaction) {
                    mDb.setTransactionSuccessful();
                }
                return result;
            } finally {
                if (doTransaction) {
                    final long begin;
                    if (debug) {
                        begin = System.currentTimeMillis();
                    } else {
                        begin = 0L;
                    }
                    // not doing endTransaction in the same 'finally' block of unlockRead() because endTransaction() may throw an exception
                    mDb.endTransaction();
                    if (debug) {
                        Log.v("LockableDatabase: Transaction ended, took %d ms / %s",
                                currentTimeMillis() - begin,
                                new Exception().getStackTrace()[1]);
                    }
                }
            }
        } finally {
            if (doTransaction) {
                inTransaction.set(null);
            }
            unlockRead();
        }
    }

    public void open() {
        lockWrite();
        try {
            openOrCreateDataspace();
        } finally {
            unlockWrite();
        }
    }

    private void openOrCreateDataspace() {
        lockWrite();
        try {
            final File databaseFile = prepareStorage();
            try {
                doOpenOrCreateDb(databaseFile);
            } catch (SQLiteException e) {
                // TODO handle this error in a better way!
                Log.w(e, "Unable to open DB %s - removing file and retrying", databaseFile);
                if (databaseFile.exists() && !databaseFile.delete()) {
                    Log.d("Failed to remove %s that couldn't be opened", databaseFile);
                }
                doOpenOrCreateDb(databaseFile);
            }

            mDb.execSQL("PRAGMA foreign_keys = ON;");

            if (mDb.getVersion() != mSchemaDefinition.getVersion()) {
                mSchemaDefinition.doDbUpgrade(mDb);
            }
        } finally {
            unlockWrite();
        }
    }

    private void doOpenOrCreateDb(final File databaseFile) {
        mDb = context.openOrCreateDatabase(databaseFile.getName(), Context.MODE_PRIVATE, null);
    }

    protected File prepareStorage() {
        final File databaseFile = storageFilesProvider.getDatabaseFile();
        final File databaseParentDir = databaseFile.getParentFile();
        if (databaseParentDir.isFile()) {
            // should be safe to unconditionally delete clashing file: user is not supposed to mess with our directory
            // noinspection ResultOfMethodCallIgnored
            databaseParentDir.delete();
        }
        if (!databaseParentDir.exists()) {
            if (!databaseParentDir.mkdirs()) {
                throw new RuntimeException("Unable to access: " + databaseParentDir);
            }
            FileHelper.touchFile(databaseParentDir, ".nomedia");
        }

        final File attachmentDir = storageFilesProvider.getAttachmentDirectory();
        final File attachmentParentDir = attachmentDir.getParentFile();
        if (!attachmentParentDir.exists()) {
            // noinspection ResultOfMethodCallIgnored, TODO maybe throw UnavailableStorageException?
            attachmentParentDir.mkdirs();
            FileHelper.touchFile(attachmentParentDir, ".nomedia");
        }
        if (!attachmentDir.exists()) {
            // noinspection ResultOfMethodCallIgnored, TODO maybe throw UnavailableStorageException?
            attachmentDir.mkdirs();
        }
        return databaseFile;
    }

    /**
     * Delete the backing database.
     */
    public void delete() {
        delete(false);
    }

    /**
     * @param recreate
     *            <code>true</code> if the DB should be recreated after delete
     */
    private void delete(final boolean recreate) {
        lockWrite();
        try {
            try {
                mDb.close();
            } catch (Exception e) {
                Log.d("Exception caught in DB close: %s", e.getMessage());
            }
            try {
                final File attachmentDirectory = storageFilesProvider.getAttachmentDirectory();
                final File[] attachments = attachmentDirectory.listFiles();
                for (File attachment : attachments) {
                    if (attachment.exists()) {
                        boolean attachmentWasDeleted = attachment.delete();
                        if (!attachmentWasDeleted) {
                            Log.d("Attachment was not deleted!");
                        }
                    }
                }
                if (attachmentDirectory.exists()) {
                    boolean attachmentDirectoryWasDeleted = attachmentDirectory.delete();
                    if (!attachmentDirectoryWasDeleted) {
                        Log.d("Attachment directory was not deleted!");
                    }
                }
            } catch (Exception e) {
                Log.d("Exception caught in clearing attachments: %s", e.getMessage());
            }
            try {
                deleteDatabase(storageFilesProvider.getDatabaseFile());
            } catch (Exception e) {
                Log.i(e, "LockableDatabase: delete(): Unable to delete backing DB file");
            }

            if (recreate) {
                openOrCreateDataspace();
            }
        } finally {
            unlockWrite();
        }
    }

    private void deleteDatabase(File database) {
        boolean deleted = SQLiteDatabase.deleteDatabase(database);
        if (!deleted) {
            Log.i("LockableDatabase: deleteDatabase(): No files deleted.");
        }
    }
}
