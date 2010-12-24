package com.fsck.k9.mail.store;

import java.io.File;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.MessagingException;

public class LockableDatabase
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

    public static interface SchemaDefinition
    {
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
                Log.d(K9.LOG_TAG, "LockableDatabase: Closing DB " + uUid + " due to unmount event on StorageProvider: " + providerId);
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
        public void onMount(final String providerId)
        {
            if (!providerId.equals(mStorageProviderId))
            {
                return;
            }

            if (K9.DEBUG)
            {
                Log.d(K9.LOG_TAG, "LockableDatabase: Opening DB " + uUid + " due to mount event on StorageProvider: " + providerId);
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

    private Application mApplication;

    /**
     * {@link ThreadLocal} to check whether a DB transaction is occuring in the
     * current {@link Thread}.
     *
     * @see #execute(boolean, DbCallback)
     */
    private ThreadLocal<Boolean> inTransaction = new ThreadLocal<Boolean>();

    private SchemaDefinition mSchemaDefinition;

    private String uUid;

    /**
     * @param application
     *            Never <code>null</code>.
     * @param uUid
     *            Never <code>null</code>.
     * @param schemaDefinition
     *            Never <code>null</code
     */
    public LockableDatabase(final Application application, final String uUid, final SchemaDefinition schemaDefinition)
    {
        this.mApplication = application;
        this.uUid = uUid;
        this.mSchemaDefinition = schemaDefinition;
    }

    public void setStorageProviderId(String mStorageProviderId)
    {
        this.mStorageProviderId = mStorageProviderId;
    }

    public String getStorageProviderId()
    {
        return mStorageProviderId;
    }

    private StorageManager getStorageManager()
    {
        return StorageManager.getInstance(mApplication);
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
            getStorageManager().lockProvider(mStorageProviderId);
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
            getStorageManager().lockProvider(providerId);
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
        getStorageManager().unlockProvider(providerId);
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
    public <T> T execute(final boolean transactional, final DbCallback<T> callback) throws UnavailableStorageException
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
                        Log.v(K9.LOG_TAG, "LockableDatabase: Transaction ended, took " + Long.toString(System.currentTimeMillis() - begin) + "ms / " + new Exception().getStackTrace()[1].toString());
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

    /**
     * @param newProviderId
     *            Never <code>null</code>.
     * @throws MessagingException
     */
    public void switchProvider(final String newProviderId) throws MessagingException
    {
        if (newProviderId.equals(mStorageProviderId))
        {
            Log.v(K9.LOG_TAG, "LockableDatabase: Ignoring provider switch request as they are equal: " + newProviderId);
            return;
        }

        final String oldProviderId = mStorageProviderId;
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

                final StorageManager storageManager = getStorageManager();

                // create new path
                prepareStorage(newProviderId);

                // move all database files
                Utility.moveRecursive(storageManager.getDatabase(uUid, oldProviderId), storageManager.getDatabase(uUid, newProviderId));
                // move all attachment files
                Utility.moveRecursive(storageManager.getAttachmentDirectory(uUid, oldProviderId), storageManager.getAttachmentDirectory(uUid, newProviderId));

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

    public void open() throws UnavailableStorageException
    {
        lockWrite();
        try
        {
            openOrCreateDataspace(mApplication);
        }
        finally
        {
            unlockWrite();
        }
        StorageManager.getInstance(mApplication).addListener(mStorageListener);
    }

    /**
     *
     * @param application
     * @throws UnavailableStorageException
     */
    protected void openOrCreateDataspace(final Application application) throws UnavailableStorageException
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
            if (mDb.getVersion() != mSchemaDefinition.getVersion())
            {
                mSchemaDefinition.doDbUpgrade(mDb);
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
        final StorageManager storageManager = getStorageManager();

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
            Utility.touchFile(databaseParentDir, ".nomedia");
        }

        final File attachmentDir;
        final File attachmentParentDir;
        attachmentDir = storageManager
                        .getAttachmentDirectory(uUid, providerId);
        attachmentParentDir = attachmentDir.getParentFile();
        if (!attachmentParentDir.exists())
        {
            attachmentParentDir.mkdirs();
            Utility.touchFile(attachmentParentDir, ".nomedia");
        }
        if (!attachmentDir.exists())
        {
            attachmentDir.mkdirs();
        }
        return databaseFile;
    }

    /**
     * Delete the backing database.
     *
     * @throws UnavailableStorageException
     */
    public void delete() throws UnavailableStorageException
    {
        delete(false);
    }

    public void recreate() throws UnavailableStorageException
    {
        delete(true);
    }

    /**
     * @param recreate
     *            <code>true</code> if the DB should be recreated after delete
     * @throws UnavailableStorageException
     */
    private void delete(final boolean recreate) throws UnavailableStorageException
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
            final StorageManager storageManager = getStorageManager();
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
                Log.i(K9.LOG_TAG, "LockableDatabase: delete(): Unable to delete backing DB file", e);
            }

            if (recreate)
            {
                openOrCreateDataspace(mApplication);
            }
            else
            {
                // stop waiting for mount/unmount events
                getStorageManager().removeListener(mStorageListener);
            }
        }
        finally
        {
            unlockWrite();
        }
    }

}
