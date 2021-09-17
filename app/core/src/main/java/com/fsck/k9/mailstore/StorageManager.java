package com.fsck.k9.mailstore;


import java.io.File;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.content.Context;
import android.os.Environment;

import com.fsck.k9.CoreResourceProvider;
import com.fsck.k9.DI;
import timber.log.Timber;

/**
 * Manager for different {@link StorageProvider} -classes that abstract access
 * to sd-cards, additional internal memory and other storage-locations.
 */
public class StorageManager {

    /**
     * Provides entry points (File objects) to an underlying storage,
     * alleviating the caller from having to know where that storage is located.
     *
     * <p>
     * Allow checking for the denoted storage availability since its lifecycle
     * can evolving (a storage might become unavailable at some time and be back
     * online later).
     * </p>
     */
    public interface StorageProvider {

        /**
         * Retrieve the uniquely identifier for the current implementation.
         *
         * <p>
         * It is expected that the identifier doesn't change over reboots since
         * it'll be used to save settings and retrieve the provider at a later
         * time.
         * </p>
         *
         * <p>
         * The returned identifier doesn't have to be user friendly.
         * </p>
         *
         * @return Never <code>null</code>.
         */
        String getId();

        /**
         * Hook point for provider initialization.
         *
         * @param context
         *            Never <code>null</code>.
         */
        void init(Context context);

        /**
         * @param context
         *            Never <code>null</code>.
         * @return A user displayable, localized name for this provider. Never
         *         <code>null</code>.
         */
        String getName(Context context);

        /**
         * Some implementations may not be able to return valid File handles
         * because the device doesn't provide the denoted storage. You can check
         * the provider compatibility with this method to prevent from having to
         * invoke this provider ever again.
         *
         * @param context
         *            TODO
         * @return Whether this provider supports the current device.
         * @see StorageManager#getAvailableProviders()
         */
        boolean isSupported(Context context);

        /**
         * Return the {@link File} to the chosen email database file. The
         * resulting {@link File} doesn't necessarily match an existing file on
         * the filesystem.
         *
         * @param context
         *            Never <code>null</code>.
         * @param id
         *            Never <code>null</code>.
         * @return Never <code>null</code>.
         */
        File getDatabase(Context context, String id);

        /**
         * Return the {@link File} to the chosen attachment directory. The
         * resulting {@link File} doesn't necessarily match an existing
         * directory on the filesystem.
         *
         * @param context
         *            Never <code>null</code>.
         * @param id
         *            Never <code>null</code>.
         * @return Never <code>null</code>.
         */
        File getAttachmentDirectory(Context context, String id);

        /**
         * Check for the underlying storage availability.
         *
         * @param context
         *            Never <code>null</code>.
         * @return Whether the underlying storage returned by this provider is
         *         ready for read/write operations at the time of invocation.
         */
        boolean isReady(Context context);
    }

    /**
     * Strategy to access the always available internal storage.
     *
     * <p>
     * This implementation is expected to work on every device since it's based
     * on the regular Android API {@link Context#getDatabasePath(String)} and
     * uses the result to retrieve the DB path and the attachment directory path.
     * </p>
     *
     * <p>
     * The underlying storage has always been used by K-9.
     * </p>
     */
    public static class InternalStorageProvider implements StorageProvider {
        public static final String ID = "InternalStorage";

        private final CoreResourceProvider resourceProvider;

        public InternalStorageProvider(CoreResourceProvider resourceProvider) {
            this.resourceProvider = resourceProvider;
        }

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public void init(Context context) {
        }

        @Override
        public String getName(Context context) {
            return resourceProvider.internalStorageProviderName();
        }

        @Override
        public boolean isSupported(Context context) {
            return true;
        }

        @Override
        public File getDatabase(Context context, String id) {
            return context.getDatabasePath(id + ".db");
        }

        @Override
        public File getAttachmentDirectory(Context context, String id) {
            // we store attachments in the database directory
            return context.getDatabasePath(id + ".db_att");
        }

        @Override
        public boolean isReady(Context context) {
            return true;
        }
    }

    /**
     * Strategy for accessing the storage as returned by
     * {@link Environment#getExternalStorageDirectory()}. In order to be
     * compliant with Android recommendation regarding application uninstalling
     * and to prevent from cluttering the storage root, the chosen directory
     * will be
     * <code>&lt;STORAGE_ROOT&gt;/Android/data/&lt;APPLICATION_PACKAGE_NAME&gt;/files/</code>
     *
     * <p>
     * The denoted storage is usually a SD card.
     * </p>
     *
     * <p>
     * This provider is expected to work on all devices but the returned
     * underlying storage might not be always available, due to
     * mount/unmount/USB share events.
     * </p>
     */
    public static class ExternalStorageProvider implements StorageProvider {
        public static final String ID = "ExternalStorage";

        private final CoreResourceProvider resourceProvider;

        /**
         * Chosen base directory.
         */
        private File mApplicationDirectory;


        public ExternalStorageProvider(CoreResourceProvider resourceProvider) {
            this.resourceProvider = resourceProvider;
        }

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public void init(Context context) {
            mApplicationDirectory = context.getExternalFilesDir(null);
        }

        @Override
        public String getName(Context context) {
            return resourceProvider.externalStorageProviderName();
        }

        @Override
        public boolean isSupported(Context context) {
            return true;
        }

        @Override
        public File getDatabase(Context context, String id) {
            return new File(mApplicationDirectory, id + ".db");
        }

        @Override
        public File getAttachmentDirectory(Context context, String id) {
            return new File(mApplicationDirectory, id + ".db_att");
        }

        @Override
        public boolean isReady(Context context) {
            return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        }
    }

    /**
     * Stores storage provider locking information
     */
    public static class SynchronizationAid {
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

    /**
     * The active storage providers.
     */
    private final Map<String, StorageProvider> mProviders = new LinkedHashMap<>();

    /**
     * Locking data for the active storage providers.
     */
    private final Map<StorageProvider, SynchronizationAid> mProviderLocks = new IdentityHashMap<>();

    protected final Context context;

    private static transient StorageManager instance;

    public static synchronized StorageManager getInstance(final Context context) {
        if (instance == null) {
            Context applicationContext = context.getApplicationContext();
            CoreResourceProvider resourceProvider = DI.get(CoreResourceProvider.class);
            instance = new StorageManager(applicationContext, resourceProvider);
        }
        return instance;
    }

    /**
     * @param context
     *            Never <code>null</code>.
     * @throws NullPointerException
     *             If <tt>context</tt> is <code>null</code>.
     */
    protected StorageManager(final Context context, CoreResourceProvider resourceProvider) throws NullPointerException {
        if (context == null) {
            throw new NullPointerException("No Context given");
        }

        this.context = context;

        /*
         * 20101113/fiouzy:
         *
         * Here is where we define which providers are used, currently we only
         * allow the internal storage and the regular external storage.
         *
         * !!! Make sure InternalStorageProvider is the first provider as it'll
         * be considered as the default provider !!!
         */
        final List<StorageProvider> allProviders = Arrays.asList(
                new InternalStorageProvider(resourceProvider),
                new ExternalStorageProvider(resourceProvider));
        for (final StorageProvider provider : allProviders) {
            // check for provider compatibility
            if (provider.isSupported(context)) {
                // provider is compatible! proceeding

                provider.init(context);
                mProviders.put(provider.getId(), provider);
                mProviderLocks.put(provider, new SynchronizationAid());
            }
        }

    }

    /**
     * @return Never <code>null</code>.
     */
    public String getDefaultProviderId() {
        // assume there is at least 1 provider defined
        return mProviders.keySet().iterator().next();
    }

    /**
     * @param providerId
     *            Never <code>null</code>.
     * @return <code>null</code> if not found.
     */
    protected StorageProvider getProvider(final String providerId) {
        return mProviders.get(providerId);
    }

    /**
     * @param dbName
     *            Never <code>null</code>.
     * @param providerId
     *            Never <code>null</code>.
     * @return The resolved database file for the given provider ID.
     */
    public File getDatabase(final String dbName, final String providerId) {
        StorageProvider provider = getProvider(providerId);
        // TODO fallback to internal storage if no provider
        return provider.getDatabase(context, dbName);
    }

    /**
     * @param dbName
     *            Never <code>null</code>.
     * @param providerId
     *            Never <code>null</code>.
     * @return The resolved attachment directory for the given provider ID.
     */
    public File getAttachmentDirectory(final String dbName, final String providerId) {
        StorageProvider provider = getProvider(providerId);
        // TODO fallback to internal storage if no provider
        return provider.getAttachmentDirectory(context, dbName);
    }

    /**
     * @param providerId
     *            Never <code>null</code>.
     * @return Whether the specified provider is ready for read/write operations
     */
    public boolean isReady(final String providerId) {
        StorageProvider provider = getProvider(providerId);
        if (provider == null) {
            Timber.w("Storage-Provider \"%s\" does not exist", providerId);
            return false;
        }
        return provider.isReady(context);
    }

    /**
     * @return A map of available providers names, indexed by their ID. Never
     *         <code>null</code>.
     * @see StorageManager
     * @see StorageProvider#isSupported(Context)
     */
    public Map<String, String> getAvailableProviders() {
        final Map<String, String> result = new LinkedHashMap<>();
        for (final Map.Entry<String, StorageProvider> entry : mProviders.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getName(context));
        }
        return result;
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
    public void lockProvider(final String providerId) throws UnavailableStorageException {
        final StorageProvider provider = getProvider(providerId);
        if (provider == null) {
            throw new UnavailableStorageException("StorageProvider not found: " + providerId);
        }
        // lock provider
        final SynchronizationAid sync = mProviderLocks.get(provider);
        final boolean locked = sync.readLock.tryLock();
        if (!locked || (locked && sync.unmounting)) {
            if (locked) {
                sync.readLock.unlock();
            }
            throw new UnavailableStorageException("StorageProvider is unmounting");
        } else if (locked && !provider.isReady(context)) {
            sync.readLock.unlock();
            throw new UnavailableStorageException("StorageProvider not ready");
        }
    }

    public void unlockProvider(final String providerId) {
        final StorageProvider provider = getProvider(providerId);
        final SynchronizationAid sync = mProviderLocks.get(provider);
        sync.readLock.unlock();
    }
}
