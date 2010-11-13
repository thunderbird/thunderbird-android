package com.fsck.k9.mail.store;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.service.MailService;

/**
 * Manager for different {@link StorageProvider} -classes that abstract access
 * to sd-cards, additional internal memory and other storage-locations.
 */
public class StorageManager
{

    /**
     * Abstract provider that allows access to sd-cards, additional internal
     * memory and other storage-locations.
     */
    public static interface StorageProvider
    {

        /**
         * @return Never <code>null</code>.
         */
        String getId();

        /**
         * @param context
         *            Never <code>null</code>.
         */
        void init(Context context);

        /**
         * @param context
         *            Never <code>null</code>. Used to localize resources.
         * @return A user displayable, localized name for this provider. Never
         *         <code>null</code>.
         */
        String getName(Context context);

        /**
         * @param context
         *            TODO
         * @return Whether this provider supports the current device.
         * @see StorageManager#getAvailableProviders()
         */
        boolean isSupported(Context context);

        /**
         * Return the path to the email-database.
         *
         * @param context
         *            Never <code>null</code>.
         * @param id
         *            Never <code>null</code>.
         * @return Never <code>null</code>.
         */
        File getDatabase(Context context, String id);

        /**
         * Return the path to the attachment-directory.
         *
         * @param context
         *            Never <code>null</code>.
         * @param id
         *            Never <code>null</code>.
         * @return Never <code>null</code>.
         */
        File getAttachmentDirectory(Context context, String id);

        /**
         * @param context
         *            Never <code>null</code>.
         * @return Whether the underlying storage returned by this provider is
         *         ready for read/write operations at the time of invokation.
         */
        boolean isReady(Context context);

        /**
         * @param context
         *            Never <code>null</code>.
         * @return The root directory of the denoted storage. Never
         *         <code>null</code>.
         */
        File getRoot(Context context);
    }

    public static interface StorageListener
    {
        /**
         * @param providerId
         *            Never <code>null</code>.
         */
        void onMount(String providerId);

        /**
         * @param providerId
         *            Never <code>null</code>.
         */
        void onUnmount(String providerId);
    }

    public abstract static class FixedStorageProviderBase implements StorageProvider
    {
        protected File mRoot;

        protected File mApplicationDir;

        @Override
        public void init(final Context context)
        {
            mRoot = getRoot(context);
            mApplicationDir = new File(mRoot, "k9");

        }

        /**
         * Vendor specific checks
         *
         * @return Whether this provider supports the underlying vendor specific
         *         storage
         */
        protected abstract boolean supportsVendor();

        @Override
        public boolean isReady(Context context)
        {
            try
            {
                final File root = mRoot.getCanonicalFile();
                return isMountPoint(root)
                       && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
            }
            catch (IOException e)
            {
                Log.w(K9.LOG_TAG, "Specified root isn't ready: " + mRoot, e);
                return false;
            }
        }

        @Override
        public final boolean isSupported(Context context)
        {
            return mRoot.isDirectory() && supportsVendor();
        }

        @Override
        public File getDatabase(Context context, String id)
        {
            return new File(mApplicationDir, id + ".db");
        }

        @Override
        public File getAttachmentDirectory(Context context, String id)
        {
            return new File(mApplicationDir, id + ".db_att");
        }

        @Override
        public final File getRoot(Context context)
        {
            return mRoot;
        }

        protected abstract File computeRoot(Context context);
    }

    public static class InternalStorageProvider implements StorageProvider
    {

        public static final String ID = "InternalStorage";

        protected File mRoot;

        @Override
        public String getId()
        {
            return ID;
        }

        @Override
        public void init(Context context)
        {
            // XXX
            mRoot = new File("/");
        }

        @Override
        public String getName(Context context)
        {
            return context.getString(R.string.local_storage_provider_internal_label);
        }

        @Override
        public boolean isSupported(Context context)
        {
            return true;
        }

        @Override
        public File getDatabase(Context context, String id)
        {
            return context.getDatabasePath(id + ".db");
        }

        @Override
        public File getAttachmentDirectory(Context context, String id)
        {
            // we store attachments in the database directory
            return context.getDatabasePath(id + ".db_att");
        }

        @Override
        public boolean isReady(Context context)
        {
            return true;
        }

        @Override
        public File getRoot(Context context)
        {
            return mRoot;
        }
    }

    public static class ExternalStorageProvider implements StorageProvider
    {

        public static final String ID = "ExternalStorage";

        protected File mRoot;

        protected File mApplicationDirectory;

        public String getId()
        {
            return ID;
        }

        @Override
        public void init(Context context)
        {
            mRoot = Environment.getExternalStorageDirectory();
            mApplicationDirectory = new File(new File(new File(new File(mRoot, "Android"), "data"),
                                             context.getPackageName()), "files");
        }

        @Override
        public String getName(Context context)
        {
            return context.getString(R.string.local_storage_provider_external_label);
        }

        @Override
        public boolean isSupported(Context context)
        {
            return true;
        }

        @Override
        public File getDatabase(Context context, String id)
        {
            return new File(mApplicationDirectory, id + ".db");
        }

        @Override
        public File getAttachmentDirectory(Context context, String id)
        {
            return new File(mApplicationDirectory, id + ".db_att");
        }

        @Override
        public boolean isReady(Context context)
        {
            return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        }

        @Override
        public File getRoot(Context context)
        {
            return mRoot;
        }
    }

    public static class HtcIncredibleStorageProvider extends FixedStorageProviderBase
    {

        public static final String ID = "HtcIncredibleStorage";

        public String getId()
        {
            return ID;
        }

        @Override
        public String getName(Context context)
        {
            return context.getString(R.string.local_storage_provider_samsunggalaxy_label,
                                     Build.MODEL);
        }

        @Override
        protected boolean supportsVendor()
        {
            return "inc".equals(Build.DEVICE);
        }

        @Override
        protected File computeRoot(Context context)
        {
            return new File("/emmc");
        }
    }

    public static class SamsungGalaxySStorageProvider extends FixedStorageProviderBase
    {

        public static final String ID = "SamsungGalaxySStorage";

        public String getId()
        {
            return ID;
        }

        @Override
        public String getName(Context context)
        {
            return context.getString(R.string.local_storage_provider_samsunggalaxy_label,
                                     Build.MODEL);
        }

        @Override
        protected boolean supportsVendor()
        {
            // FIXME
            return "GT-I5800".equals(Build.DEVICE) || "GT-I9000".equals(Build.DEVICE)
                   || "SGH-T959".equals(Build.DEVICE) || "SGH-I897".equals(Build.DEVICE);
        }

        @Override
        protected File computeRoot(Context context)
        {
            return Environment.getExternalStorageDirectory(); // was: new
            // File("/sdcard")
        }
    }

    public static class SynchronizationAid
    {
        /**
         * {@link Lock} has a thread semantic so it can't be released from
         * another thread - this flags act as a holder for the unmount state
         */
        public boolean unmounting = false;

        public final Lock readLock;

        public final Lock writeLock;
        {
            final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
            readLock = readWriteLock.readLock();
            writeLock = readWriteLock.writeLock();
        }
    }

    private final Map<String, StorageProvider> mProviders = new LinkedHashMap<String, StorageProvider>();

    private final Map<StorageProvider, SynchronizationAid> mProviderLocks = new IdentityHashMap<StorageProvider, SynchronizationAid>();

    protected final Application mApplication;

    private List<StorageListener> mListeners = new ArrayList<StorageListener>();

    private static transient StorageManager instance;

    public static synchronized StorageManager getInstance(final Application application)
    {
        if (instance == null)
        {
            instance = new StorageManager(application);
        }
        return instance;
    }

    /**
     * @param file
     *            Canonical file to match. Never <code>null</code>.
     * @return Whether the specified file matches a filesystem root.
     * @throws IOException
     */
    public static boolean isMountPoint(final File file) throws IOException
    {
        for (final File root : File.listRoots())
        {
            if (root.equals(file))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @param application
     *            Never <code>null</code>.
     * @throws NullPointerException
     *             If <tt>application</tt> is <code>null</code>.
     */
    protected StorageManager(final Application application) throws NullPointerException
    {
        if (application == null)
        {
            throw new NullPointerException("No application instance given");
        }

        mApplication = application;

        final List<StorageProvider> allProviders = Arrays.asList(new InternalStorageProvider(),
                new ExternalStorageProvider());
        for (final StorageProvider provider : allProviders)
        {
            if (provider.isSupported(mApplication))
            {
                provider.init(application);
                mProviders.put(provider.getId(), provider);
                mProviderLocks.put(provider, new SynchronizationAid());
            }
        }

    }

    /**
     * @return Never <code>null</code>.
     */
    public String getDefaultProviderId()
    {
        // assume there is at least 1 provider defined
        return mProviders.entrySet().iterator().next().getKey();
    }

    /**
     * @param providerId
     *            Never <code>null</code>.
     * @return <code>null</code> if not found.
     */
    protected StorageProvider getProvider(final String providerId)
    {
        return mProviders.get(providerId);
    }

    /**
     * @param dbName
     *            Never <code>null</code>.
     * @param providerId
     *            Never <code>null</code>.
     * @return The resolved database file for the given provider ID.
     */
    public File getDatabase(final String dbName, final String providerId)
    {
        StorageProvider provider = getProvider(providerId);
        // TODO fallback to internal storage if no provider
        return provider.getDatabase(mApplication, dbName);
    }

    /**
     * @param dbName
     *            Never <code>null</code>.
     * @param providerId
     *            Never <code>null</code>.
     * @return The resolved attachement directory for the given provider ID.
     */
    public File getAttachmentDirectory(final String dbName, final String providerId)
    {
        StorageProvider provider = getProvider(providerId);
        // TODO fallback to internal storage if no provider
        return provider.getAttachmentDirectory(mApplication, dbName);
    }

    /**
     * @param providerId
     *            Never <code>null</code>.
     * @return Whether the specified provider is ready for read/write operations
     */
    public boolean isReady(final String providerId)
    {
        StorageProvider provider = getProvider(providerId);
        if (provider == null)
        {
            Log.w(K9.LOG_TAG, "Storage-Provider \"" + providerId + "\" does not exist");
            return false;
        }
        return provider.isReady(mApplication);
    }

    /**
     * @return A map of available providers names, indexed by their ID. Never
     *         <code>null</code>.
     * @see StorageManager
     * @see StorageProvider#isSupported(Context)
     */
    public Map<String, String> getAvailableProviders()
    {
        final Map<String, String> result = new LinkedHashMap<String, String>();
        for (final Map.Entry<String, StorageProvider> entry : mProviders.entrySet())
        {
            result.put(entry.getKey(), entry.getValue().getName(mApplication));
        }
        return result;
    }

    /**
     * @param path
     */
    public void onBeforeUnmount(final String path)
    {
        Log.i(K9.LOG_TAG, "storage path \"" + path + "\" unmounting");
        final StorageProvider provider = resolveProvider(path);
        if (provider == null)
        {
            return;
        }
        for (final StorageListener listener : mListeners)
        {
            try
            {
                listener.onUnmount(provider.getId());
            }
            catch (Exception e)
            {
                Log.w(K9.LOG_TAG, "Error while notifying StorageListener", e);
            }
        }
        final SynchronizationAid sync = mProviderLocks.get(resolveProvider(path));
        sync.writeLock.lock();
        sync.unmounting = true;
        sync.writeLock.unlock();
    }

    public void onAfterUnmount(final String path)
    {
        Log.i(K9.LOG_TAG, "storage path \"" + path + "\" unmounted");
        final StorageProvider provider = resolveProvider(path);
        if (provider == null)
        {
            return;
        }
        final SynchronizationAid sync = mProviderLocks.get(resolveProvider(path));
        sync.writeLock.lock();
        sync.unmounting = false;
        sync.writeLock.unlock();
    }

    /**
     * @param path
     * @param readOnly
     */
    public void onMount(final String path, final boolean readOnly)
    {
        Log.i(K9.LOG_TAG, "storage path \"" + path + "\" mounted readOnly=" + readOnly);
        if (readOnly)
        {
            return;
        }

        final StorageProvider provider = resolveProvider(path);
        if (provider == null)
        {
            return;
        }
        for (final StorageListener listener : mListeners)
        {
            try
            {
                listener.onMount(provider.getId());
            }
            catch (Exception e)
            {
                Log.w(K9.LOG_TAG, "Error while notifying StorageListener", e);
            }
        }

        // XXX we should reset mail service ONLY if there are accounts using the storage (this is not done in a regular listener because it has to be invoked afterward)
        MailService.actionReset(mApplication, null);
    }

    /**
     * @param path
     *            Never <code>null</code>.
     * @return The corresponding provider. <code>null</code> if no match.
     */
    protected StorageProvider resolveProvider(final String path)
    {
        for (final StorageProvider provider : mProviders.values())
        {
            if (path.equals(provider.getRoot(mApplication).getAbsolutePath()))
            {
                return provider;
            }
        }
        return null;
    }

    public void addListener(final StorageListener listener)
    {
        mListeners.add(listener);
    }

    public void removeListener(final StorageListener listener)
    {
        mListeners.remove(listener);
    }

    /**
     * Try to lock the underlying storage to prevent concurrent unmount.
     *
     * <p>
     * You must invoke {@link #unlockProvider(String)} when you're done with the
     * storage.
     * </p>
     *
     * @param providerId
     * @throws UnavailableStorageException
     *             If the storage can't be locked.
     */
    public void lockProvider(final String providerId) throws UnavailableStorageException
    {
        final StorageProvider provider = getProvider(providerId);
        if (provider == null)
        {
            throw new UnavailableStorageException("StorageProvider not found: " + providerId);
        }
        // lock provider
        final SynchronizationAid sync = mProviderLocks.get(provider);
        final boolean locked = sync.readLock.tryLock();
        if (!locked || (locked && sync.unmounting))
        {
            if (locked)
            {
                sync.readLock.unlock();
            }
            throw new UnavailableStorageException("StorageProvider is unmounting");
        }
        else if (locked && !provider.isReady(mApplication))
        {
            sync.readLock.unlock();
            throw new UnavailableStorageException("StorageProvider not ready");
        }
    }

    public void unlockProvider(final String providerId)
    {
        final StorageProvider provider = getProvider(providerId);
        final SynchronizationAid sync = mProviderLocks.get(provider);
        sync.readLock.unlock();
    }
}
