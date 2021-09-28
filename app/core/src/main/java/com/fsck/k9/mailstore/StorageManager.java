package com.fsck.k9.mailstore;


import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.os.Environment;


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

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public void init(Context context) {
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

        /**
         * Chosen base directory.
         */
        private File mApplicationDirectory;


        @Override
        public String getId() {
            return ID;
        }

        @Override
        public void init(Context context) {
            mApplicationDirectory = context.getExternalFilesDir(null);
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
    }

    /**
     * The active storage providers.
     */
    private final Map<String, StorageProvider> mProviders = new LinkedHashMap<>();

    protected final Context context;

    private static transient StorageManager instance;

    public static synchronized StorageManager getInstance(final Context context) {
        if (instance == null) {
            Context applicationContext = context.getApplicationContext();
            instance = new StorageManager(applicationContext);
        }
        return instance;
    }

    /**
     * @param context
     *            Never <code>null</code>.
     * @throws NullPointerException
     *             If <tt>context</tt> is <code>null</code>.
     */
    protected StorageManager(final Context context) throws NullPointerException {
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
                new InternalStorageProvider(),
                new ExternalStorageProvider()
        );
        for (final StorageProvider provider : allProviders) {
            // check for provider compatibility
            if (provider.isSupported(context)) {
                // provider is compatible! proceeding

                provider.init(context);
                mProviders.put(provider.getId(), provider);
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

    public Set<String> getAvailableProviders() {
        return mProviders.keySet();
    }
}
