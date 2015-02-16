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
import com.fsck.k9.BuildConfig;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.mailstore.LocalStore.AttachmentInfo;
import com.fsck.k9.mailstore.StorageManager;

import java.io.*;
import java.util.List;

/**
 * A simple ContentProvider that allows file access to attachments.
 *
 * <p>
 * Warning! We make heavy assumptions about the Uris used by the {@link LocalStore} for an
 * {@link Account} here.
 * </p>
 */
public class AttachmentProvider extends ContentProvider {
    private static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".attachmentprovider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    private static final String FORMAT_RAW = "RAW";
    private static final String FORMAT_VIEW = "VIEW";
    private static final String FORMAT_THUMBNAIL = "THUMBNAIL";

    private static final String[] DEFAULT_PROJECTION = new String[] {
        AttachmentProviderColumns._ID,
        AttachmentProviderColumns.DATA,
    };

    public static class AttachmentProviderColumns {
        public static final String _ID = "_id";
        public static final String DATA = "_data";
        public static final String DISPLAY_NAME = "_display_name";
        public static final String SIZE = "_size";
    }


    public static Uri getAttachmentUri(Account account, long id) {
        return CONTENT_URI.buildUpon()
                .appendPath(account.getUuid())
                .appendPath(Long.toString(id))
                .appendPath(FORMAT_RAW)
                .build();
    }

    public static Uri getAttachmentUriForViewing(Account account, long id, String mimeType, String filename) {
        return CONTENT_URI.buildUpon()
                .appendPath(account.getUuid())
                .appendPath(Long.toString(id))
                .appendPath(FORMAT_VIEW)
                .appendPath(mimeType)
                .appendPath(filename)
                .build();
    }

    public static Uri getAttachmentThumbnailUri(Account account, long id, int width, int height) {
        return CONTENT_URI.buildUpon()
               .appendPath(account.getUuid())
               .appendPath(Long.toString(id))
               .appendPath(FORMAT_THUMBNAIL)
               .appendPath(Integer.toString(width))
               .appendPath(Integer.toString(height))
               .build();
    }

    public static void clear(Context context) {
        /*
         * We use the cache dir as a temporary directory (since Android doesn't give us one) so
         * on startup we'll clean up any .tmp files from the last run.
         */
        File[] files = context.getCacheDir().listFiles();
        for (File file : files) {
            try {
                if (K9.DEBUG) {
                    Log.d(K9.LOG_TAG, "Deleting file " + file.getCanonicalPath());
                }
            } catch (IOException ioe) { /* No need to log failure to log */ }
            file.delete();
        }
    }

    /**
     * Delete the thumbnail of an attachment.
     *
     * @param context
     *         The application context.
     * @param accountUuid
     *         The UUID of the account the attachment belongs to.
     * @param attachmentId
     *         The ID of the attachment the thumbnail was created for.
     */
    public static void deleteThumbnail(Context context, String accountUuid, String attachmentId) {
        File file = getThumbnailFile(context, accountUuid, attachmentId);
        if (file.exists()) {
            file.delete();
        }
    }

    private static File getThumbnailFile(Context context, String accountUuid,
            String attachmentId) {
        String filename = "thmb_" + accountUuid + "_" + attachmentId + ".tmp";
        File dir = context.getCacheDir();
        return new File(dir, filename);
    }


    @Override
    public boolean onCreate() {
        /*
         * We use the cache dir as a temporary directory (since Android doesn't give us one) so
         * on startup we'll clean up any .tmp files from the last run.
         */
        final File cacheDir = getContext().getCacheDir();
        if (cacheDir == null) {
            return true;
        }
        File[] files = cacheDir.listFiles();
        if (files == null) {
            return true;
        }
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
        String mimeType = (segments.size() < 4) ? null : segments.get(3);

        return getType(dbName, id, format, mimeType);
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File file;

        List<String> segments = uri.getPathSegments();
        String accountUuid = segments.get(0);
        String attachmentId = segments.get(1);
        String format = segments.get(2);

        if (FORMAT_THUMBNAIL.equals(format)) {
            int width = Integer.parseInt(segments.get(3));
            int height = Integer.parseInt(segments.get(4));

            file = getThumbnailFile(getContext(), accountUuid, attachmentId);
            if (!file.exists()) {
                String type = getType(accountUuid, attachmentId, FORMAT_VIEW, null);
                try {
                    FileInputStream in = new FileInputStream(getFile(accountUuid, attachmentId));
                    try {
                        Bitmap thumbnail = createThumbnail(type, in);
                        if (thumbnail != null) {
                            thumbnail = Bitmap.createScaledBitmap(thumbnail, width, height, true);
                            FileOutputStream out = new FileOutputStream(file);
                            try {
                                thumbnail.compress(Bitmap.CompressFormat.PNG, 100, out);
                            } finally {
                                out.close();
                            }
                        }
                    } finally {
                        try { in.close(); } catch (Throwable ignore) { /* ignore */ }
                    }
                } catch (IOException ioe) {
                    return null;
                }
            }
        } else {
            file = getFile(accountUuid, attachmentId);
        }

        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        String[] columnNames = (projection == null) ? DEFAULT_PROJECTION : projection;

        List<String> segments = uri.getPathSegments();
        String dbName = segments.get(0);
        String id = segments.get(1);

        // Versions of K-9 before 3.400 had a database name here, not an
        // account UID, so implement a bit of backcompat
        if (dbName.endsWith(".db")) {
            dbName = dbName.substring(0, dbName.length() - 3);
        }

        final AttachmentInfo attachmentInfo;
        try {
            final Account account = Preferences.getPreferences(getContext()).getAccount(dbName);
            attachmentInfo = LocalStore.getInstance(account, getContext()).getAttachmentInfo(id);
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Unable to retrieve attachment info from local store for ID: " + id, e);
            return null;
        }

        if (attachmentInfo == null) {
            if (K9.DEBUG) {
                Log.d(K9.LOG_TAG, "No attachment info for ID: " + id);
            }
            return null;
        }

        MatrixCursor ret = new MatrixCursor(columnNames);
        Object[] values = new Object[columnNames.length];
        for (int i = 0, count = columnNames.length; i < count; i++) {
            String column = columnNames[i];
            if (AttachmentProviderColumns._ID.equals(column)) {
                values[i] = id;
            } else if (AttachmentProviderColumns.DATA.equals(column)) {
                values[i] = uri.toString();
            } else if (AttachmentProviderColumns.DISPLAY_NAME.equals(column)) {
                values[i] = attachmentInfo.name;
            } else if (AttachmentProviderColumns.SIZE.equals(column)) {
                values[i] = attachmentInfo.size;
            }
        }
        ret.addRow(values);
        return ret;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int delete(Uri uri, String arg1, String[] arg2) {
        return 0;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    private String getType(String dbName, String id, String format, String mimeType) {
        String type;
        if (FORMAT_THUMBNAIL.equals(format)) {
            type = "image/png";
        } else {
            final Account account = Preferences.getPreferences(getContext()).getAccount(dbName);

            try {
                final LocalStore localStore = LocalStore.getInstance(account, getContext());

                AttachmentInfo attachmentInfo = localStore.getAttachmentInfo(id);
                if (FORMAT_VIEW.equals(format) && mimeType != null) {
                    type = mimeType;
                } else {
                    type = attachmentInfo.type;
                }
            } catch (MessagingException e) {
                Log.e(K9.LOG_TAG, "Unable to retrieve LocalStore for " + account, e);
                type = null;
            }
        }

        return type;
    }

    private File getFile(String dbName, String id) throws FileNotFoundException {
        Account account = Preferences.getPreferences(getContext()).getAccount(dbName);

        File attachmentsDir = StorageManager.getInstance(getContext()).getAttachmentDirectory(dbName,
                account.getLocalStorageProviderId());

        File file = new File(attachmentsDir, id);
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }

        return file;
    }

    private Bitmap createThumbnail(String type, InputStream data) {
        if (MimeUtility.mimeTypeMatches(type, "image/*")) {
            return createImageThumbnail(data);
        }
        return null;
    }

    private Bitmap createImageThumbnail(InputStream data) {
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(data);
            return bitmap;
        } catch (OutOfMemoryError oome) {
            /*
             * Improperly downloaded images, corrupt bitmaps and the like can commonly
             * cause OOME due to invalid allocation sizes. We're happy with a null bitmap in
             * that case. If the system is really out of memory we'll know about it soon
             * enough.
             */
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
