package com.fsck.k9.mail.store;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

public class StorageManager
{

    public static interface StorageProvider
    {

        /**
         * @return Never <code>null</code>.
         */
        String getId();

        /**
         * @param context
         *            Never <code>null</code>.
         * @return A user friendly name for this provider. Never
         *         <code>null</code>.
         */
        String getName(Context context);

        /**
         * @return Whether this provider supports the current device.
         */
        boolean supports();

        /**
         * @param context
         *            Never <code>null</code>.
         * @param id
         *            Never <code>null</code>.
         * @return Never <code>null</code>.
         */
        File getDatabase(Context context, String id);

        /**
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
            return getRootDirectory().isDirectory()
                    && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        }

        @Override
        public final boolean supports()
        {
            return isReady() && supportsVendor();
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
            // TODO localization
            return "Regular internal storage";
        }

        @Override
        public boolean supports()
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
            // TODO localization
            return "External storage (SD Card)";
        }

        @Override
        public boolean supports()
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
            // TODO localization
            return Build.MODEL + " additional internal storage";
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
            // TODO localization
            return Build.MODEL + " additional internal storage";
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
            return new File("/sdcard");
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

    protected StorageManager()
    {
        final List<StorageProvider> allProviders = Arrays.asList(new InternalStorageProvider(),
                new ExternalStorageProvider());
        for (final StorageProvider provider : allProviders)
        {
            if (provider.supports())
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

    public File getDatabase(final Context context, final String dbName, final String providerId)
    {
        StorageProvider provider = getProvider(providerId);
        // TODO fallback to internal storage if no provider
        return provider.getDatabase(context, dbName);
    }

    public File getAttachmentDirectory(final Context context, final String dbName,
            final String providerId)
    {
        StorageProvider provider = getProvider(providerId);
        // TODO fallback to internal storage if no provider
        return provider.getAttachmentDirectory(context, dbName);
    }

    public boolean isReady(final Context context, final String providerId)
    {
        // TODO null handling
        return getProvider(providerId).isReady();
    }

    public Map<String, String> getAvailableProviders(final Context context)
    {
        final Map<String, String> result = new HashMap<String, String>();
        for (final Map.Entry<String, StorageProvider> entry : mProviders.entrySet())
        {
            result.put(entry.getKey(), entry.getValue().getName(context));
        }
        return result;
    }

}
