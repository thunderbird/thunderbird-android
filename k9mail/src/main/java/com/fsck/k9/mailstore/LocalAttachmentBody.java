package com.fsck.k9.mailstore;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import android.content.Context;
import android.net.Uri;

import com.fsck.k9.mail.MessagingException;

/**
 * An attachment whose contents are loaded from an URI.
 */
public class LocalAttachmentBody extends BinaryAttachmentBody {
    private Context context;
    private Uri mUri;

    public LocalAttachmentBody(Uri uri, Context context) {
        this.context = context;
        mUri = uri;
    }

    @Override
    public InputStream getInputStream() throws MessagingException {
        try {
            return context.getContentResolver().openInputStream(mUri);
        } catch (FileNotFoundException fnfe) {
            /*
             * Since it's completely normal for us to try to serve up attachments that
             * have been blown away, we just return an empty stream.
             */
            return new ByteArrayInputStream(LocalStore.EMPTY_BYTE_ARRAY);
        }
    }

    public Uri getContentUri() {
        return mUri;
    }
}
