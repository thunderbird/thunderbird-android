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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.k9mail.legacy.di.DI;
import com.fsck.k9.helper.MimeTypeUtil;
import com.fsck.k9.mailstore.LocalStoreProvider;
import net.thunderbird.core.android.account.LegacyAccountDto;
import net.thunderbird.feature.account.AccountId;
import net.thunderbird.feature.account.AccountIdFactory;
import net.thunderbird.legacy.logging.Log;
import com.fsck.k9.Preferences;
import net.thunderbird.core.common.exception.MessagingException;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.mailstore.LocalStore.AttachmentInfo;
import org.openintents.openpgp.util.OpenPgpApi.OpenPgpDataSource;


/**
 * A simple ContentProvider that allows file access to attachments.
 */
public class AttachmentProvider extends ContentProvider {
    private static final String TAG = "AttachmentProvider";

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
        AccountId accountId = parseAccountId(segments);
        if (accountId == null || segments.size() < 2) {
            return MimeTypeUtil.DEFAULT_ATTACHMENT_MIME_TYPE;
        }
        String id = segments.get(1);
        String mimeType = (segments.size() < 3) ? null : segments.get(2);

        return getType(accountId, id, mimeType);
    }

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        List<String> segments = uri.getPathSegments();
        AccountId accountId = parseAccountId(segments);
        if (accountId == null || segments.size() < 2) {
            throw new FileNotFoundException("Attachment missing or cannot be opened!");
        }
        String attachmentId = segments.get(1);

        ParcelFileDescriptor parcelFileDescriptor = openAttachment(accountId, attachmentId);
        if (parcelFileDescriptor == null) {
            throw new FileNotFoundException("Attachment missing or cannot be opened!");
        }
        return parcelFileDescriptor;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        String[] columnNames = (projection == null) ? DEFAULT_PROJECTION : projection;

        List<String> segments = uri.getPathSegments();
        AccountId accountId = parseAccountId(segments);
        if (accountId == null || segments.size() < 2) {
            return null;
        }
        String id = segments.get(1);

        final AttachmentInfo attachmentInfo;
        try {
            final LegacyAccountDto account = Preferences.getPreferences().getById(accountId);
            attachmentInfo = DI.get(LocalStoreProvider.class).getInstance(account).getAttachmentInfo(id);
        } catch (MessagingException e) {
            Log.e(TAG, e, "Unable to retrieve attachment info from local store for ID: %s", id);
            return null;
        }

        if (attachmentInfo == null) {
            Log.d(TAG, "No attachment info for ID: %s", id);
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

    @Nullable
    private AccountId parseAccountId(List<String> segments) {
        if (segments.isEmpty()) {
            return null;
        }

        try {
            return AccountIdFactory.INSTANCE.of(segments.get(0));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String getType(AccountId accountId, String id, String mimeType) {
        String type;
        final LegacyAccountDto account = Preferences.getPreferences().getById(accountId);

        try {
            final LocalStore localStore = DI.get(LocalStoreProvider.class).getInstance(account);

            AttachmentInfo attachmentInfo = localStore.getAttachmentInfo(id);
            if (mimeType != null) {
                type = mimeType;
            } else {
                type = attachmentInfo.type;
            }
        } catch (MessagingException e) {
            Log.e(TAG, e, "Unable to retrieve LocalStore for %s", account);
            type = MimeTypeUtil.DEFAULT_ATTACHMENT_MIME_TYPE;
        }

        return type;
    }

    @Nullable
    private ParcelFileDescriptor openAttachment(AccountId accountId, String attachmentId) {
        try {
            OpenPgpDataSource openPgpDataSource = getAttachmentDataSource(accountId, attachmentId);
            if (openPgpDataSource == null) {
                Log.e(TAG, "Error getting data source for attachment (part doesn't exist?)");
                return null;
            }
            return openPgpDataSource.startPumpThread();
        } catch (MessagingException e) {
            Log.e(TAG, e, "Error getting InputStream for attachment");
            return null;
        } catch (IOException e) {
            Log.e(TAG, e, "Error creating ParcelFileDescriptor");
            return null;
        }
    }

    @Nullable
    private OpenPgpDataSource getAttachmentDataSource(AccountId accountId, String attachmentId) throws MessagingException {
        final LegacyAccountDto account = Preferences.getPreferences().getById(accountId);
        LocalStore localStore = DI.get(LocalStoreProvider.class).getInstance(account);
        return localStore.getAttachmentDataSource(attachmentId);
    }
}
