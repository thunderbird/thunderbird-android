package com.fsck.k9.mail.store;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.R;

/**
 * Manager for different {@link StorageProvider} -classes that
 * abstract access to sd-cards, additional internal memory and other
 * storage-locations.
 */
public class StorageManager
{

	/**
	 * Abstract provider that allows access to sd-cards, additional internal memory and other
     * storage-locations.
	 */
    public static interface StorageProvider
    {

        /**
         * @return Never <code>null</code>.
         */
        String getId();

        /**
         * @param context
         *            Never <code>null</code>. Used to localize resources.
         * @return A user displayable, localized name for this provider. Never
         *         <code>null</code>.
         */
        String getName(Context context);

        /**
         * @return Whether this provider supports the current device.
         * @see StorageManager#getAvailableProviders(Context)
         */
        boolean isSupported();

        /**
         * Return the path to the email-database.
         * @param context
         *            Never <code>null</code>.
         * @param id
         *            Never <code>null</code>.
         * @return Never <code>null</code>.
         */
        File getDatabase(Context context, String id);

        /**
         * Return the path to the attachment-directory.
         * @param context
         *            Never <code>null</code>.
         * @param id
         *            Never <code>null</code>.
         * @return Never <code>null</code>.
         */
        File getAttachmentDirectory(Context context, String id);

        /**
         * @return Whether the underlying storage returned by this provider is
         *         ready for read/write operations at the time of invokation.
         */
        boolean isReady();
    }

    public abstract static class FixedStorageProviderBase implements StorageProvider
    {
        /**
         * @return The root directory of the denoted storage. Never
         *         <code>null</code>.
         */
        protected abstract File getRootDirectory();

        /**
         * Vendor specific checks
         * 
         * @return Whether this provider supports the underlying vendor specific
         *         storage
         */
        protected abstract boolean supportsVendor();

        @Override
        public boolean isReady()
        {
            try
            {
                final File root = getRootDirectory().getCanonicalFile();
                return isMountPoint(root)
                        && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
            }
            catch (IOException e)
            {
                Log.w(K9.LOG_TAG, "Specified root isn't ready: " + getRootDirectory(), e);
                return false;
            }
        }

        @Override
        public final boolean isSupported()
        {
            return getRootDirectory().isDirectory() && supportsVendor();
        }

        protected File getApplicationDir(Context context)
        {
            return new File(getRootDirectory(), "k9");
        }

        @Override
        public File getDatabase(Context context, String id)
        {
            return new File(getApplicationDir(context), id + ".db");
        }

        @Override
        public File getAttachmentDirectory(Context context, String id)
        {
            return new File(getApplicationDir(context), id + ".db_att");
        }

    }

    public static class InternalStorageProvider implements StorageProvider
    {

        public static final String ID = "InternalStorage";

        @Override
        public String getId()
        {
            return ID;
        }

        @Override
        public String getName(Context context)
        {
            return context.getString(R.string.local_storage_provider_internal_label);
        }

        @Override
        public boolean isSupported()
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
        public boolean isReady()
        {
            return true;
        }

    }

    public static class ExternalStorageProvider implements StorageProvider
    {

        public static final String ID = "ExternalStorage";

        public String getId()
        {
            return ID;
        }

        @Override
        public String getName(Context context)
        {
            return context.getString(R.string.local_storage_provider_external_label);
        }

        @Override
        public boolean isSupported()
        {
            return true;
        }

        protected File getApplicationDirectory(Context context)
        {
            // XXX should use /Android/data/<package_name>/files/ for proper cleaning when uninstalling
            return new File(Environment.getExternalStorageDirectory(), "k9");
        }

        @Override
        public File getDatabase(Context context, String id)
        {
            return new File(getApplicationDirectory(context), id + ".db");
        }

        @Override
        public File getAttachmentDirectory(Context context, String id)
        {
            return new File(getApplicationDirectory(context), id + ".db_att");
        }

        @Override
        public boolean isReady()
        {
            return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
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
            return context.getString(R.string.local_storage_provider_samsunggalaxy_label, Build.MODEL);
        }

        @Override
        protected boolean supportsVendor()
        {
            return "inc".equals(Build.DEVICE);
        }

        @Override
        protected File getRootDirectory()
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
            return context.getString(R.string.local_storage_provider_samsunggalaxy_label, Build.MODEL);
        }

        @Override
        protected boolean supportsVendor()
        {
            // FIXME
            return "GT-I5800".equals(Build.DEVICE) || "GT-I9000".equals(Build.DEVICE)
                    || "SGH-T959".equals(Build.DEVICE) || "SGH-I897".equals(Build.DEVICE);
        }

        @Override
        protected File getRootDirectory()
        {
            return Environment.getExternalStorageDirectory(); // was: new File("/sdcard")
        }
    }

    private final Map<String, StorageProvider> mProviders = new LinkedHashMap<String, StorageProvider>();

    private static StorageManager instance;

    public static synchronized StorageManager getInstance()
    {
        if (instance == null)
        {
            instance = new StorageManager();
        }
        return instance;
    }

    /**
     * @param file
     *            Canonical file to matach. Never <code>null</code>.
     * @return Whether the specified file matches a filesystem root;
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

    protected StorageManager()
    {
        final List<StorageProvider> allProviders = Arrays.asList(new InternalStorageProvider(),
                new ExternalStorageProvider());
        for (final StorageProvider provider : allProviders)
        {
            if (provider.isSupported())
            {
                mProviders.put(provider.getId(), provider);
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
     * @param context
     *            Never <code>null</code>.
     * @param dbName
     *            Never <code>null</code>.
     * @param providerId
     *            Never <code>null</code>.
     * @return The resolved database file for the given provider ID.
     */
    public File getDatabase(final Context context, final String dbName, final String providerId)
    {
        StorageProvider provider = getProvider(providerId);
        // TODO fallback to internal storage if no provider
        return provider.getDatabase(context, dbName);
    }

    /**
     * @param context
     *            Never <code>null</code>.
     * @param dbName
     *            Never <code>null</code>.
     * @param providerId
     *            Never <code>null</code>.
     * @return The resolved attachement directory for the given provider ID.
     */
    public File getAttachmentDirectory(final Context context, final String dbName,
            final String providerId)
    {
        StorageProvider provider = getProvider(providerId);
        // TODO fallback to internal storage if no provider
        return provider.getAttachmentDirectory(context, dbName);
    }

    /**
     * @param context
     * @param providerId
     *            Never <code>null</code>.
     * @return Whether the specified provider is ready for read/write operations
     */
    public boolean isReady(final Context context, final String providerId)
    {
        StorageProvider provider = getProvider(providerId);
        if (provider == null) {
        	Log.w(K9.LOG_TAG, "Storage-Provider \"" + providerId + "\" does not exist");
        	return false;
        }
		return provider.isReady();
    }

    /**
     * @param context
     * @return A map of available providers names, indexed by their ID. Never
     *         <code>null</code>.
     * @see StorageManager
     * @see StorageProvider#isSupported()
     */
    public Map<String, String> getAvailableProviders(final Context context)
    {
        final Map<String, String> result = new LinkedHashMap<String, String>();
        for (final Map.Entry<String, StorageProvider> entry : mProviders.entrySet())
        {
            result.put(entry.getKey(), entry.getValue().getName(context));
        }
        return result;
    }

}
