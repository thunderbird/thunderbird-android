package com.android.email.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Config;
import android.util.Log;

import com.android.email.Account;
import com.android.email.Email;
import com.android.email.Utility;
import com.android.email.mail.internet.MimeUtility;

/*
 * A simple ContentProvider that allows file access to Email's attachments.
 */
public class AttachmentProvider extends ContentProvider {
    public static final Uri CONTENT_URI = Uri.parse( "content://com.android.email.attachmentprovider");

    private static final String FORMAT_RAW = "RAW";
    private static final String FORMAT_THUMBNAIL = "THUMBNAIL";

    public static class AttachmentProviderColumns {
        public static final String _ID = "_id";
        public static final String DATA = "_data";
        public static final String DISPLAY_NAME = "_display_name";
        public static final String SIZE = "_size";
    }

    public static Uri getAttachmentUri(Account account, long id) {
        return CONTENT_URI.buildUpon()
                .appendPath(account.getUuid() + ".db")
                .appendPath(Long.toString(id))
                .appendPath(FORMAT_RAW)
                .build();
    }

    public static Uri getAttachmentThumbnailUri(Account account, long id, int width, int height) {
        return CONTENT_URI.buildUpon()
                .appendPath(account.getUuid() + ".db")
                .appendPath(Long.toString(id))
                .appendPath(FORMAT_THUMBNAIL)
                .appendPath(Integer.toString(width))
                .appendPath(Integer.toString(height))
                .build();
    }

    public static Uri getAttachmentUri(String db, long id) {
        return CONTENT_URI.buildUpon()
                .appendPath(db)
                .appendPath(Long.toString(id))
                .appendPath(FORMAT_RAW)
                .build();
    }

    @Override
    public boolean onCreate() {
        /*
         * We use the cache dir as a temporary directory (since Android doesn't give us one) so
         * on startup we'll clean up any .tmp files from the last run.
         */
        File[] files = getContext().getCacheDir().listFiles();
        for (File file : files) {
            if (file.getName().endsWith(".tmp")) {
                file.delete();
            }
        }
        return true;
    }

    @Override
    public String getType(Uri uri) {
        List<String> segments = uri.getPathSegments();
        String dbName = segments.get(0);
        String id = segments.get(1);
        String format = segments.get(2);
        if (FORMAT_THUMBNAIL.equals(format)) {
            return "image/png";
        }
        else {
            String path = getContext().getDatabasePath(dbName).getAbsolutePath();
            SQLiteDatabase db = null;
            Cursor cursor = null;
            try {
                db = SQLiteDatabase.openDatabase(path, null, 0);
                cursor = db.query(
                        "attachments",
                        new String[] { "mime_type" },
                        "id = ?",
                        new String[] { id },
                        null,
                        null,
                        null);
                cursor.moveToFirst();
                String type = cursor.getString(0);
                cursor.close();
                db.close();
                return type;

            }
            finally {
                if (cursor != null) {
                    cursor.close();
                }
                if (db != null) {
                    db.close();
                }

            }
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        List<String> segments = uri.getPathSegments();
        String dbName = segments.get(0);
        String id = segments.get(1);
        String format = segments.get(2);
        if (FORMAT_THUMBNAIL.equals(format)) {
            int width = Integer.parseInt(segments.get(3));
            int height = Integer.parseInt(segments.get(4));
            String filename = "thmb_" + dbName + "_" + id;
            File dir = getContext().getCacheDir();
            File file = new File(dir, filename);
            if (!file.exists()) {
                Uri attachmentUri = getAttachmentUri(dbName, Long.parseLong(id));
                String type = getType(attachmentUri);
                try {
                    FileInputStream in = new FileInputStream(
                            new File(getContext().getDatabasePath(dbName + "_att"), id));
                    Bitmap thumbnail = createThumbnail(type, in);
                    thumbnail = thumbnail.createScaledBitmap(thumbnail, width, height, true);
                    FileOutputStream out = new FileOutputStream(file);
                    thumbnail.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.close();
                    in.close();
                }
                catch (IOException ioe) {
                    return null;
                }
            }
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        }
        else {
            return ParcelFileDescriptor.open(
                    new File(getContext().getDatabasePath(dbName + "_att"), id),
                    ParcelFileDescriptor.MODE_READ_ONLY);
        }
    }

    @Override
    public int delete(Uri uri, String arg1, String[] arg2) {
        return 0;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (projection == null) {
            projection =
                new String[] {
                    AttachmentProviderColumns._ID,
                    AttachmentProviderColumns.DATA,
                    };
        }

        List<String> segments = uri.getPathSegments();
        String dbName = segments.get(0);
        String id = segments.get(1);
        String format = segments.get(2);
        String path = getContext().getDatabasePath(dbName).getAbsolutePath();
        String name = null;
        int size = -1;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = SQLiteDatabase.openDatabase(path, null, 0);
            cursor = db.query(
                    "attachments",
                    new String[] { "name", "size" },
                    "id = ?",
                    new String[] { id },
                    null,
                    null,
                    null);
            if (!cursor.moveToFirst()) {
                return null;
            }
            name = cursor.getString(0);
            size = cursor.getInt(1);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }

        MatrixCursor ret = new MatrixCursor(projection);
        Object[] values = new Object[projection.length];
        for (int i = 0, count = projection.length; i < count; i++) {
            String column = projection[i];
            if (AttachmentProviderColumns._ID.equals(column)) {
                values[i] = id;
            }
            else if (AttachmentProviderColumns.DATA.equals(column)) {
                values[i] = uri.toString();
            }
            else if (AttachmentProviderColumns.DISPLAY_NAME.equals(column)) {
                values[i] = name;
            }
            else if (AttachmentProviderColumns.SIZE.equals(column)) {
                values[i] = size;
            }
        }
        ret.addRow(values);
        return ret;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private Bitmap createThumbnail(String type, InputStream data) {
        if(MimeUtility.mimeTypeMatches(type, "image/*")) {
            return createImageThumbnail(data);
        }
        return null;
    }

    private Bitmap createImageThumbnail(InputStream data) {
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(data);
            return bitmap;
        }
        catch (OutOfMemoryError oome) {
            /*
             * Improperly downloaded images, corrupt bitmaps and the like can commonly
             * cause OOME due to invalid allocation sizes. We're happy with a null bitmap in
             * that case. If the system is really out of memory we'll know about it soon
             * enough.
             */
            return null;
        }
        catch (Exception e) {
            return null;
        }
    }
}
