package com.fsck.k9.mailstore;

import java.io.File;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import timber.log.Timber;

import com.fsck.k9.K9;
import com.fsck.k9.helper.FileHelper;
import com.fsck.k9.mail.MessagingException;

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
         * @throws WrappedException
         * @throws com.fsck.k9.mail.MessagingException
         * @throws com.fsck.k9.mailstore.UnavailableStorageException
         */
        T doDbWork(SQLiteDatabase db) throws WrappedException, MessagingException;
    }

    public interface SchemaDefinition {
        int getVersion();

        /**
         * @param db Never <code>null</code>.
         */
        void doDbUpgrade(SQLiteDatabase db);
    }

    /**
     * Workaround exception wrapper used to keep the inner exception generated
     * in a {@link DbCallback}.
     */
    public static class WrappedException extends RuntimeException {
        /**
         *
         */
        private static final long serialVersionUID = 8184421232587399369L;

        public WrappedException(final Exception cause) {
            super(cause);
        }
    }

    /**
     * Open the DB on mount and close the DB on unmount
     */
    private class StorageListener implements StorageManager.StorageListener {
        @Override
        public void onUnmount(final String providerId) {
            if (!providerId.equals(mStorageProviderId)) {
                return;
            }

            Timber.d("LockableDatabase: Closing DB %s due to unmount event on StorageProvider: %s", uUid, providerId);

            try {
                lockWrite();
                try {
                    mDb.close();
                } finally {
                    unlockWrite();
                }
            } catch (UnavailableStorageException e) {
                Timber.w(e, "Unable to writelock on unmount");
            }
        }

        @Override
        public void onMount(final String providerId) {
            if (!providerId.equals(mStorageProviderId)) {
                return;
            }

            Timber.d("LockableDatabase: Opening DB %s due to mount event on StorageProvider: %s", uUid, providerId);

            try {
                openOrCreateDataspace();
            } catch (UnavailableStorageException e) {
                Timber.e(e, "Unable to open DB on mount");
            }
        }
    }

    private String mStorageProviderId;

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

    private final StorageListener mStorageListener = new StorageListener();

    private Context context;

    /**
     * {@link ThreadLocal} to check whether a DB transaction is occurring in the
     * current {@link Thread}.
     *
     * @see #execute(boolean, DbCallback)
     */
    private ThreadLocal<Boolean> inTransaction = new ThreadLocal<>();

    private SchemaDefinition mSchemaDefinition;

    private String uUid;

    /**
     * @param context
     *            Never <code>null</code>.
     * @param uUid
     *            Never <code>null</code>.
     * @param schemaDefinition
     *            Never <code>null</code>.
     */
    public LockableDatabase(final Context context, final String uUid, final SchemaDefinition schemaDefinition) {
        this.context = context;
        this.uUid = uUid;
        this.mSchemaDefinition = schemaDefinition;
    }

    public void setStorageProviderId(String mStorageProviderId) {
        this.mStorageProviderId = mStorageProviderId;
    }

    public String getStorageProviderId() {
        return mStorageProviderId;
    }

    private StorageManager getStorageManager() {
        return StorageManager.getInstance(context);
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
    protected void lockRead() throws UnavailableStorageException {
        mReadLock.lock();
        try {
            getStorageManager().lockProvider(mStorageProviderId);
        } catch (UnavailableStorageException | RuntimeException e) {
            mReadLock.unlock();
            throw e;
        }
    }

    protected void unlockRead() {
        getStorageManager().unlockProvider(mStorageProviderId);
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
    protected void lockWrite() throws UnavailableStorageException {
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
    protected void lockWrite(final String providerId) throws UnavailableStorageException {
        mWriteLock.lock();
        try {
            getStorageManager().lockProvider(providerId);
        } catch (UnavailableStorageException | RuntimeException e) {
            mWriteLock.unlock();
            throw e;
        }
    }

    protected void unlockWrite() {
        unlockWrite(mStorageProviderId);
    }

    protected void unlockWrite(final String providerId) {
        getStorageManager().unlockProvider(providerId);
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
     * @throws UnavailableStorageException
     */
    public <T> T execute(final boolean transactional, final DbCallback<T> callback) throws MessagingException {
        lockRead();
        final boolean doTransaction = transactional && inTransaction.get() == null;
        try {
            final boolean debug = K9.isDebug();
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
                        Timber.v("LockableDatabase: Transaction ended, took %d ms / %s",
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

    /**
     * @param newProviderId
     *            Never <code>null</code>.
     * @throws MessagingException
     */
    public void switchProvider(final String newProviderId) throws MessagingException {
        if (newProviderId.equals(mStorageProviderId)) {
            Timber.v("LockableDatabase: Ignoring provider switch request as they are equal: %s", newProviderId);
            return;
        }

        final String oldProviderId = mStorageProviderId;
        lockWrite(oldProviderId);
        try {
            lockWrite(newProviderId);
            try {
                try {
                    mDb.close();
                } catch (Exception e) {
                    Timber.i(e, "Unable to close DB on local store migration");
                }

                final StorageManager storageManager = getStorageManager();
                File oldDatabase = storageManager.getDatabase(uUid, oldProviderId);

                // create new path
                prepareStorage(newProviderId);

                // move all database files
                FileHelper.moveRecursive(oldDatabase, storageManager.getDatabase(uUid, newProviderId));
                // move all attachment files
                FileHelper.moveRecursive(storageManager.getAttachmentDirectory(uUid, oldProviderId),
                        storageManager.getAttachmentDirectory(uUid, newProviderId));
                // remove any remaining old journal files
                deleteDatabase(oldDatabase);

                mStorageProviderId = newProviderId;

                // re-initialize this class with the new Uri
                openOrCreateDataspace();
            } finally {
                unlockWrite(newProviderId);
            }
        } finally {
            unlockWrite(oldProviderId);
        }
    }

    public void open() throws UnavailableStorageException {
        lockWrite();
        try {
            openOrCreateDataspace();
        } finally {
            unlockWrite();
        }
        StorageManager.getInstance(context).addListener(mStorageListener);
    }

    /**
     *
     * @throws UnavailableStorageException
     */
    private void openOrCreateDataspace() throws UnavailableStorageException {

        lockWrite();
        try {
            final File databaseFile = prepareStorage(mStorageProviderId);
            try {
                doOpenOrCreateDb(databaseFile);
            } catch (SQLiteException e) {
                // TODO handle this error in a better way!
                Timber.w(e, "Unable to open DB %s - removing file and retrying", databaseFile);
                if (databaseFile.exists() && !databaseFile.delete()) {
                    Timber.d("Failed to remove %s that couldn't be opened", databaseFile);
                }
                doOpenOrCreateDb(databaseFile);
            }
            if (mDb.getVersion() != mSchemaDefinition.getVersion()) {
                mSchemaDefinition.doDbUpgrade(mDb);
            }
        } finally {
            unlockWrite();
        }
    }

    private void doOpenOrCreateDb(final File databaseFile) {
        if (StorageManager.InternalStorageProvider.ID.equals(mStorageProviderId)) {
            // internal storage
            mDb = context.openOrCreateDatabase(databaseFile.getName(), Context.MODE_PRIVATE,
                    null);
        } else {
            // external storage
            mDb = SQLiteDatabase.openOrCreateDatabase(databaseFile, null);
        }
    }

    /**
     * @param providerId
     *            Never <code>null</code>.
     * @return DB file.
     * @throws UnavailableStorageException
     */
    protected File prepareStorage(final String providerId) throws UnavailableStorageException {
        final StorageManager storageManager = getStorageManager();

        final File databaseFile = storageManager.getDatabase(uUid, providerId);
        final File databaseParentDir = databaseFile.getParentFile();
        if (databaseParentDir.isFile()) {
            // should be safe to unconditionally delete clashing file: user is not supposed to mess with our directory
            // noinspection ResultOfMethodCallIgnored
            databaseParentDir.delete();
        }
        if (!databaseParentDir.exists()) {
            if (!databaseParentDir.mkdirs()) {
                // Android seems to be unmounting the storage...
                throw new UnavailableStorageException("Unable to access: " + databaseParentDir);
            }
            FileHelper.touchFile(databaseParentDir, ".nomedia");
        }

        final File attachmentDir = storageManager.getAttachmentDirectory(uUid, providerId);
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
     *
     * @throws UnavailableStorageException
     */
    public void delete() throws UnavailableStorageException {
        delete(false);
    }

    public void recreate() throws UnavailableStorageException {
        delete(true);
    }

    /**
     * @param recreate
     *            <code>true</code> if the DB should be recreated after delete
     * @throws UnavailableStorageException
     */
    private void delete(final boolean recreate) throws UnavailableStorageException {
        lockWrite();
        try {
            try {
                mDb.close();
            } catch (Exception e) {
                Timber.d("Exception caught in DB close: %s", e.getMessage());
            }
            final StorageManager storageManager = getStorageManager();
            try {
                final File attachmentDirectory = storageManager.getAttachmentDirectory(uUid, mStorageProviderId);
                final File[] attachments = attachmentDirectory.listFiles();
                for (File attachment : attachments) {
                    if (attachment.exists()) {
                        boolean attachmentWasDeleted = attachment.delete();
                        if (!attachmentWasDeleted) {
                            Timber.d("Attachment was not deleted!");
                        }
                    }
                }
                if (attachmentDirectory.exists()) {
                    boolean attachmentDirectoryWasDeleted = attachmentDirectory.delete();
                    if (!attachmentDirectoryWasDeleted) {
                        Timber.d("Attachment directory was not deleted!");
                    }
                }
            } catch (Exception e) {
                Timber.d("Exception caught in clearing attachments: %s", e.getMessage());
            }
            try {
                deleteDatabase(storageManager.getDatabase(uUid, mStorageProviderId));
            } catch (Exception e) {
                Timber.i(e, "LockableDatabase: delete(): Unable to delete backing DB file");
            }

            if (recreate) {
                openOrCreateDataspace();
            } else {
                // stop waiting for mount/unmount events
                getStorageManager().removeListener(mStorageListener);
            }
        } finally {
            unlockWrite();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void deleteDatabase(File database) {
        boolean deleted;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            deleted = SQLiteDatabase.deleteDatabase(database);
        } else {
            deleted = database.delete();
            deleted |= new File(database.getPath() + "-journal").delete();
        }
        if (!deleted) {
            Timber.i("LockableDatabase: deleteDatabase(): No files deleted.");
        }
    }
}
