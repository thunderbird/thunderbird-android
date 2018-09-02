package com.fsck.k9.provider;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import timber.log.Timber;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.mailstore.LocalStore.AttachmentInfo;
import org.openintents.openpgp.util.OpenPgpApi.OpenPgpDataSource;


/**
 * A simple ContentProvider that allows file access to attachments.
 */
public class AttachmentProvider extends ContentProvider {
    public static Uri CONTENT_URI;

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
        String packageName = getContext().getPackageName();
        String authority = packageName + ".attachmentprovider";
        CONTENT_URI = Uri.parse("content://" + authority);

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
            Timber.e(e, "Unable to retrieve attachment info from local store for ID: %s", id);
            return null;
        }

        if (attachmentInfo == null) {
            Timber.d("No attachment info for ID: %s", id);
            return null;
        }

        MatrixCursor ret = new MatrixCursor(columnNames);
        Object[] values = new Object[columnNames.length];
        for (int i = 0, count = columnNames.length; i < count; i++) {
            String column = columnNames[i];
            switch (column) {
                case AttachmentProviderColumns._ID:
                    values[i] = id;
                    break;
                case AttachmentProviderColumns.DATA:
                    values[i] = uri.toString();
                    break;
                case AttachmentProviderColumns.DISPLAY_NAME:
                    values[i] = attachmentInfo.name;
                    break;
                case AttachmentProviderColumns.SIZE:
                    values[i] = attachmentInfo.size;
                    break;
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
            Timber.e(e, "Unable to retrieve LocalStore for %s", account);
            type = MimeUtility.DEFAULT_ATTACHMENT_MIME_TYPE;
        }

        return type;
    }

    @Nullable
    private ParcelFileDescriptor openAttachment(String accountUuid, String attachmentId) {
        try {
            OpenPgpDataSource openPgpDataSource = getAttachmentDataSource(accountUuid, attachmentId);
            if (openPgpDataSource == null) {
                Timber.e("Error getting data source for attachment (part doesn't exist?)");
                return null;
            }
            return openPgpDataSource.startPumpThread();
        } catch (MessagingException e) {
            Timber.e(e, "Error getting InputStream for attachment");
            return null;
        } catch (IOException e) {
            Timber.e(e, "Error creating ParcelFileDescriptor");
            return null;
        }
    }

    @Nullable
    private OpenPgpDataSource getAttachmentDataSource(String accountUuid, String attachmentId) throws MessagingException {
        final Account account = Preferences.getPreferences(getContext()).getAccount(accountUuid);
        LocalStore localStore = LocalStore.getInstance(account, getContext());
        return localStore.getAttachmentDataSource(attachmentId);
    }
}
