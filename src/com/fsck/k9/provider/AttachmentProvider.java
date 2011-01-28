package com.fsck.k9.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.mail.store.LocalStore.AttachmentInfo;
import com.fsck.k9.mail.store.StorageManager;

import java.io.*;
import java.util.List;

/**
 * A simple ContentProvider that allows file access to Email's attachments.<br/>
 * Warning! We make heavy assumptions about the Uris used by the {@link LocalStore} for an {@link Account} here.
 */
public class AttachmentProvider extends ContentProvider
{
    public static final Uri CONTENT_URI = Uri.parse("content://com.fsck.k9.attachmentprovider");

    private static final String FORMAT_RAW = "RAW";
    private static final String FORMAT_THUMBNAIL = "THUMBNAIL";

    public static class AttachmentProviderColumns
    {
        public static final String _ID = "_id";
        public static final String DATA = "_data";
        public static final String DISPLAY_NAME = "_display_name";
        public static final String SIZE = "_size";
    }

    public static Uri getAttachmentUri(Account account, long id)
    {
        return getAttachmentUri(account.getUuid(), id);
    }

    public static Uri getAttachmentThumbnailUri(Account account, long id, int width, int height)
    {
        return CONTENT_URI.buildUpon()
               .appendPath(account.getUuid())
               .appendPath(Long.toString(id))
               .appendPath(FORMAT_THUMBNAIL)
               .appendPath(Integer.toString(width))
               .appendPath(Integer.toString(height))
               .build();
    }

    private static Uri getAttachmentUri(String db, long id)
    {
        return CONTENT_URI.buildUpon()
               .appendPath(db)
               .appendPath(Long.toString(id))
               .appendPath(FORMAT_RAW)
               .build();
    }

    @Override
    public boolean onCreate()
    {
        /*
         * We use the cache dir as a temporary directory (since Android doesn't give us one) so
         * on startup we'll clean up any .tmp files from the last run.
         */
        File[] files = getContext().getCacheDir().listFiles();
        for (File file : files)
        {
            if (file.getName().endsWith(".tmp"))
            {
                file.delete();
            }
        }

        return true;
    }

    public static void clear(Context lContext)
    {
        /*
         * We use the cache dir as a temporary directory (since Android doesn't give us one) so
         * on startup we'll clean up any .tmp files from the last run.
         */
        File[] files = lContext.getCacheDir().listFiles();
        for (File file : files)
        {
            try
            {
                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "Deleting file " + file.getCanonicalPath());
            }
            catch (IOException ioe) {}   // No need to log failure to log
            file.delete();
        }
    }

    @Override
    public String getType(Uri uri)
    {
        List<String> segments = uri.getPathSegments();
        String dbName = segments.get(0);
        String id = segments.get(1);
        String format = segments.get(2);
        if (FORMAT_THUMBNAIL.equals(format))
        {
            return "image/png";
        }
        else
        {
            final Account account = Preferences.getPreferences(getContext()).getAccount(dbName);

            try
            {
                final LocalStore localStore = LocalStore.getLocalInstance(account, K9.app);
                return localStore.getAttachmentType(id);
            }
            catch (MessagingException e)
            {
                Log.e(K9.LOG_TAG, "Unable to retrieve LocalStore for " + account, e);
                return null;
            }
        }
    }

    private File getFile(String dbName, String id)
    throws FileNotFoundException
    {
        try
        {
            final Account account = Preferences.getPreferences(getContext()).getAccount(dbName);
            final File attachmentsDir;
            attachmentsDir = StorageManager.getInstance(K9.app).getAttachmentDirectory(dbName,
                             account.getLocalStorageProviderId());
            final File file = new File(attachmentsDir, id);
            if (!file.exists())
            {
                throw new FileNotFoundException(file.getAbsolutePath());
            }
            return file;
        }
        catch (IOException e)
        {
            Log.w(K9.LOG_TAG, null, e);
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException
    {
        List<String> segments = uri.getPathSegments();
        String dbName = segments.get(0); // "/sdcard/..." is URL-encoded and makes up only 1 segment
        String id = segments.get(1);
        String format = segments.get(2);
        if (FORMAT_THUMBNAIL.equals(format))
        {
            int width = Integer.parseInt(segments.get(3));
            int height = Integer.parseInt(segments.get(4));
            String filename = "thmb_" + dbName + "_" + id + ".tmp";
            int index = dbName.lastIndexOf('/');
            if (index >= 0)
            {
                filename = /*dbName.substring(0, index + 1) + */"thmb_" + dbName.substring(index + 1) + "_" + id + ".tmp";
            }
            File dir = getContext().getCacheDir();
            File file = new File(dir, filename);
            if (!file.exists())
            {
                Uri attachmentUri = getAttachmentUri(dbName, Long.parseLong(id));
                String type = getType(attachmentUri);
                try
                {
                    FileInputStream in = new FileInputStream(getFile(dbName, id));
                    try
                    {
                        Bitmap thumbnail = createThumbnail(type, in);
                        if (thumbnail != null)
                        {
                            thumbnail = Bitmap.createScaledBitmap(thumbnail, width, height, true);
                            FileOutputStream out = new FileOutputStream(file);
                            thumbnail.compress(Bitmap.CompressFormat.PNG, 100, out);
                            out.close();
                        }
                    }
                    finally
                    {
                        in.close();
                    }
                }
                catch (IOException ioe)
                {
                    return null;
                }
            }
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        }
        else
        {
            return ParcelFileDescriptor.open(
                       getFile(dbName, id),
                       ParcelFileDescriptor.MODE_READ_ONLY);
        }
    }

    @Override
    public int delete(Uri uri, String arg1, String[] arg2)
    {
        return 0;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values)
    {
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder)
    {
        if (projection == null)
        {
            projection =
                new String[]
            {
                AttachmentProviderColumns._ID,
                AttachmentProviderColumns.DATA,
            };
        }

        List<String> segments = uri.getPathSegments();
        String dbName = segments.get(0);
        String id = segments.get(1);

        // Versions of K-9 before 3.400 had a database name here, not an
        // account UID, so implement a bit of backcompat
        if (dbName.endsWith(".db"))
        {
            dbName = dbName.substring(0, dbName.length()-3);
        }

        //String format = segments.get(2);
        final AttachmentInfo attachmentInfo;
        try
        {
            final Account account = Preferences.getPreferences(getContext()).getAccount(dbName);
            attachmentInfo = LocalStore.getLocalInstance(account, K9.app).getAttachmentInfo(id);
        }
        catch (MessagingException e)
        {
            Log.e(K9.LOG_TAG, "Uname to retrieve attachment info from local store for ID: " + id, e);
            return null;
        }

        MatrixCursor ret = new MatrixCursor(projection);
        Object[] values = new Object[projection.length];
        for (int i = 0, count = projection.length; i < count; i++)
        {
            String column = projection[i];
            if (AttachmentProviderColumns._ID.equals(column))
            {
                values[i] = id;
            }
            else if (AttachmentProviderColumns.DATA.equals(column))
            {
                values[i] = uri.toString();
            }
            else if (AttachmentProviderColumns.DISPLAY_NAME.equals(column))
            {
                values[i] = attachmentInfo.name;
            }
            else if (AttachmentProviderColumns.SIZE.equals(column))
            {
                values[i] = attachmentInfo.size;
            }
        }
        ret.addRow(values);
        return ret;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        return 0;
    }

    private Bitmap createThumbnail(String type, InputStream data)
    {
        if (MimeUtility.mimeTypeMatches(type, "image/*"))
        {
            return createImageThumbnail(data);
        }
        return null;
    }

    private Bitmap createImageThumbnail(InputStream data)
    {
        try
        {
            Bitmap bitmap = BitmapFactory.decodeStream(data);
            return bitmap;
        }
        catch (OutOfMemoryError oome)
        {
            /*
             * Improperly downloaded images, corrupt bitmaps and the like can commonly
             * cause OOME due to invalid allocation sizes. We're happy with a null bitmap in
             * that case. If the system is really out of memory we'll know about it soon
             * enough.
             */
            return null;
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
