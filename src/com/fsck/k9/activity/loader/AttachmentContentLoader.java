package com.fsck.k9.activity.loader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.activity.misc.Attachment;

import de.cketti.safecontentresolver.SafeContentResolver;
import de.cketti.safecontentresolver.SafeContentResolverCompat;
import org.apache.commons.io.IOUtils;

/**
 * Loader to fetch the content of an attachment.
 *
 * This will copy the data to a temporary file in our app's cache directory.
 */
public class AttachmentContentLoader extends AsyncTaskLoader<Attachment> {
    private static final String FILENAME_PREFIX = "attachment";

    private final Attachment mAttachment;

    public AttachmentContentLoader(Context context, Attachment attachment) {
        super(context);
        mAttachment = attachment;
    }

    @Override
    protected void onStartLoading() {
        if (mAttachment.state == Attachment.LoadingState.COMPLETE) {
            deliverResult(mAttachment);
        }

        if (takeContentChanged() || mAttachment.state == Attachment.LoadingState.METADATA) {
            forceLoad();
        }
    }

    @Override
    public Attachment loadInBackground() {
        Context context = getContext();

        try {
            File file = File.createTempFile(FILENAME_PREFIX, null, context.getCacheDir());
            file.deleteOnExit();

            if (K9.DEBUG) {
                Log.v(K9.LOG_TAG, "Saving attachment to " + file.getAbsolutePath());
            }

            SafeContentResolver safeContentResolver = SafeContentResolverCompat.newInstance(context);
            InputStream in = safeContentResolver.openInputStream(mAttachment.uri);
            try {
                FileOutputStream out = new FileOutputStream(file);
                try {
                    IOUtils.copy(in, out);
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }

            mAttachment.filename = file.getAbsolutePath();
            mAttachment.state = Attachment.LoadingState.COMPLETE;

            return mAttachment;
        } catch (IOException e) {
            e.printStackTrace();
        }

        mAttachment.filename = null;
        mAttachment.state = Attachment.LoadingState.CANCELLED;

        return mAttachment;
    }
}
