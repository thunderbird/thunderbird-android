package com.fsck.k9.provider;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.BuildConfig;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.mailstore.LocalStore.AttachmentInfo;
import org.openintents.openpgp.util.ParcelFileDescriptorUtil;


/**
 * A simple ContentProvider that allows file access to attachments.
 */
public class AttachmentProvider extends ContentProvider {
    private static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".attachmentprovider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

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


    public static Uri getAttachmentUri(String accountUuid, long id) {
        return CONTENT_URI.buildUpon()
                .appendPath(accountUuid)
                .appendPath(Long.toString(id))
                .build();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        List<String> segments = uri.getPathSegments();
        String accountUuid = segments.get(0);
        String id = segments.get(1);
        String mimeType = (segments.size() < 3) ? null : segments.get(2);

        return getType(accountUuid, id, mimeType);
    }

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        List<String> segments = uri.getPathSegments();
        String accountUuid = segments.get(0);
        String attachmentId = segments.get(1);

        ParcelFileDescriptor parcelFileDescriptor = openAttachment(accountUuid, attachmentId);
        if (parcelFileDescriptor == null) {
            throw new FileNotFoundException("Attachment missing or cannot be opened!");
        }
        return parcelFileDescriptor;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        String[] columnNames = (projection == null) ? DEFAULT_PROJECTION : projection;

        List<String> segments = uri.getPathSegments();
        String accountUuid = segments.get(0);
        String id = segments.get(1);

        final AttachmentInfo attachmentInfo;
        try {
            final Account account = Preferences.getPreferences(getContext()).getAccount(accountUuid);
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
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(@NonNull Uri uri, String arg1, String[] arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    private String getType(String accountUuid, String id, String mimeType) {
        String type;
        final Account account = Preferences.getPreferences(getContext()).getAccount(accountUuid);

        try {
            final LocalStore localStore = LocalStore.getInstance(account, getContext());

            AttachmentInfo attachmentInfo = localStore.getAttachmentInfo(id);
            if (mimeType != null) {
                type = mimeType;
            } else {
                type = attachmentInfo.type;
            }
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Unable to retrieve LocalStore for " + account, e);
            type = MimeUtility.DEFAULT_ATTACHMENT_MIME_TYPE;
        }

        return type;
    }

    @Nullable
    private ParcelFileDescriptor openAttachment(String accountUuid, String attachmentId) {
        try {
            InputStream inputStream = getAttachmentInputStream(accountUuid, attachmentId);
            if (inputStream == null) {
                Log.e(K9.LOG_TAG, "Error getting InputStream for attachment (part doesn't exist?)");
                return null;
            }
            return ParcelFileDescriptorUtil.pipeFrom(inputStream);
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Error getting InputStream for attachment", e);
            return null;
        } catch (IOException e) {
            Log.e(K9.LOG_TAG, "Error creating ParcelFileDescriptor", e);
            return null;
        }
    }

    @Nullable
    private InputStream getAttachmentInputStream(String accountUuid, String attachmentId) throws MessagingException {
        final Account account = Preferences.getPreferences(getContext()).getAccount(accountUuid);
        LocalStore localStore = LocalStore.getInstance(account, getContext());
        return localStore.getAttachmentInputStream(attachmentId);
    }
}
