package com.fsck.k9.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.Environment;
import android.util.Log;
import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.mail.internet.MimeUtility;

import java.io.*;
import java.util.List;

/*
 * A simple ContentProvider that allows file access to Email's attachments.
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
        return CONTENT_URI.buildUpon()
               .appendPath(account.getUuid() + ".db")
               .appendPath(Long.toString(id))
               .appendPath(FORMAT_RAW)
               .build();
    }

    public static Uri getAttachmentThumbnailUri(Account account, long id, int width, int height)
    {
        return CONTENT_URI.buildUpon()
               .appendPath(account.getUuid() + ".db")
               .appendPath(Long.toString(id))
               .appendPath(FORMAT_THUMBNAIL)
               .appendPath(Integer.toString(width))
               .appendPath(Integer.toString(height))
               .build();
    }

    public static Uri getAttachmentUri(String db, long id)
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
            String path = getContext().getDatabasePath(dbName).getAbsolutePath();
            SQLiteDatabase db = null;
            Cursor cursor = null;
            try
            {
                db = SQLiteDatabase.openDatabase(path, null, 0);
                cursor = db.query(
                             "attachments",
                             new String[] { "mime_type", "name" },
                             "id = ?",
                             new String[] { id },
                             null,
                             null,
                             null);
                cursor.moveToFirst();
                String type = cursor.getString(0);
                String name = cursor.getString(1);
                cursor.close();
                db.close();

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
                if (db != null)
                {
                    db.close();
                }

            }
        }
    }

    private File getFile(String dbName, String id)
    throws FileNotFoundException
    {
        try
        {
            File attachmentsDir = getContext().getDatabasePath(dbName + "_att");
            File file = new File(attachmentsDir, id);
            if (!file.exists())
            {
                file = new File(Environment.getExternalStorageDirectory()  + attachmentsDir.getCanonicalPath().substring("/data".length()), id);
                if (!file.exists())
                {
                    throw new FileNotFoundException();
                }
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
        String dbName = segments.get(0);
        String id = segments.get(1);
        String format = segments.get(2);
        if (FORMAT_THUMBNAIL.equals(format))
        {
            int width = Integer.parseInt(segments.get(3));
            int height = Integer.parseInt(segments.get(4));
            String filename = "thmb_" + dbName + "_" + id;
            File dir = getContext().getCacheDir();
            File file = new File(dir, filename);
            if (!file.exists())
            {
                Uri attachmentUri = getAttachmentUri(dbName, Long.parseLong(id));
                String type = getType(attachmentUri);
                try
                {
                    FileInputStream in = new FileInputStream(getFile(dbName, id));
                    Bitmap thumbnail = createThumbnail(type, in);
                    thumbnail = Bitmap.createScaledBitmap(thumbnail, width, height, true);
                    FileOutputStream out = new FileOutputStream(file);
                    thumbnail.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.close();
                    in.close();
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
        //String format = segments.get(2);
        String path = getContext().getDatabasePath(dbName).getAbsolutePath();
        String name = null;
        int size = -1;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try
        {
            db = SQLiteDatabase.openDatabase(path, null, 0);
            cursor = db.query(
                         "attachments",
                         new String[] { "name", "size" },
                         "id = ?",
                         new String[] { id },
                         null,
                         null,
                         null);
            if (!cursor.moveToFirst())
            {
                return null;
            }
            name = cursor.getString(0);
            size = cursor.getInt(1);
        }
        finally
        {
            if (cursor != null)
            {
                cursor.close();
            }
            if (db != null)
            {
                db.close();
            }
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
                values[i] = name;
            }
            else if (AttachmentProviderColumns.SIZE.equals(column))
            {
                values[i] = size;
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
