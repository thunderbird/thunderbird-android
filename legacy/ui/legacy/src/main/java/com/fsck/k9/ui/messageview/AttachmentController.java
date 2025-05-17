package com.fsck.k9.ui.messageview;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.WorkerThread;
import net.thunderbird.core.android.account.LegacyAccount;
import com.fsck.k9.Preferences;
import com.fsck.k9.controller.MessagingController;
import app.k9mail.legacy.message.controller.SimpleMessagingListener;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalPart;
import com.fsck.k9.provider.AttachmentTempFileProvider;
import com.fsck.k9.ui.R;
import org.apache.commons.io.IOUtils;
import timber.log.Timber;


public class AttachmentController {
    private final Context context;
    private final MessagingController controller;
    private final MessageViewFragment messageViewFragment;
    private final AttachmentViewInfo attachment;
    private final ViewIntentFinder viewIntentFinder;


    AttachmentController(Context context, MessagingController controller, MessageViewFragment messageViewFragment,
            AttachmentViewInfo attachment) {
        this.context = context;
        this.controller = controller;
        this.messageViewFragment = messageViewFragment;
        this.attachment = attachment;
        viewIntentFinder = new ViewIntentFinder(context);
    }

    public void viewAttachment() {
        if (!attachment.isContentAvailable()) {
            downloadAndViewAttachment((LocalPart) attachment.part);
        } else {
            viewLocalAttachment();
        }
    }

    public void saveAttachmentTo(Uri documentUri) {
        if (!attachment.isContentAvailable()) {
            downloadAndSaveAttachmentTo((LocalPart) attachment.part, documentUri);
        } else {
            saveLocalAttachmentTo(documentUri);
        }
    }

    private void downloadAndViewAttachment(LocalPart localPart) {
        downloadAttachment(localPart, new Runnable() {
            @Override
            public void run() {
                messageViewFragment.refreshAttachmentThumbnail(attachment);
                viewLocalAttachment();
            }
        });
    }

    private void downloadAndSaveAttachmentTo(LocalPart localPart, final Uri documentUri) {
        downloadAttachment(localPart, new Runnable() {
            @Override
            public void run() {
                messageViewFragment.refreshAttachmentThumbnail(attachment);
                saveLocalAttachmentTo(documentUri);
            }
        });
    }

    private void downloadAttachment(LocalPart localPart, final Runnable attachmentDownloadedCallback) {
        String accountUuid = localPart.getAccountUuid();
        LegacyAccount account = Preferences.getPreferences().getAccount(accountUuid);
        LocalMessage message = localPart.getMessage();

        messageViewFragment.showAttachmentLoadingDialog();
        controller.loadAttachment(account, message, attachment.part, new SimpleMessagingListener() {
            @Override
            public void loadAttachmentFinished(LegacyAccount account, Message message, Part part) {
                attachment.setContentAvailable();
                messageViewFragment.hideAttachmentLoadingDialogOnMainThread();
                messageViewFragment.runOnMainThread(attachmentDownloadedCallback);
            }

            @Override
            public void loadAttachmentFailed(LegacyAccount account, Message message, Part part, String reason) {
                messageViewFragment.hideAttachmentLoadingDialogOnMainThread();
            }
        });
    }

    private void viewLocalAttachment() {
        new ViewAttachmentAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void saveLocalAttachmentTo(Uri documentUri) {
        new SaveAttachmentAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, documentUri);
    }

    private void writeAttachment(Uri documentUri) throws IOException {
        ContentResolver contentResolver = context.getContentResolver();
        InputStream in = contentResolver.openInputStream(attachment.internalUri);
        try {
            OutputStream out = contentResolver.openOutputStream(documentUri, "wt");
            try {
                IOUtils.copy(in, out);
                out.flush();
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    @WorkerThread
    private Intent getBestViewIntent() {
        try {
            Uri intentDataUri = AttachmentTempFileProvider.createTempUriForContentUri(context, attachment.internalUri, attachment.displayName);

            return viewIntentFinder.getBestViewIntent(intentDataUri, attachment.displayName, attachment.mimeType);
        } catch (IOException e) {
            Timber.e(e, "Error creating temp file for attachment!");
            return null;
        }
    }

    private void displayAttachmentNotSavedMessage() {
        String message = context.getString(R.string.message_view_status_attachment_not_saved);
        displayMessageToUser(message);
    }

    private void displayMessageToUser(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    private class ViewAttachmentAsyncTask extends AsyncTask<Void, Void, Intent> {

        @Override
        protected Intent doInBackground(Void... params) {
            return getBestViewIntent();
        }

        @Override
        protected void onPostExecute(Intent intent) {
            viewAttachment(intent);
        }

        private void viewAttachment(Intent intent) {
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Timber.e(e, "Could not display attachment of type %s", attachment.mimeType);

                String message = context.getString(R.string.message_view_no_viewer, attachment.mimeType);
                displayMessageToUser(message);
            }
        }
    }

    private class SaveAttachmentAsyncTask extends AsyncTask<Uri, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Uri... params) {
            try {
                Uri documentUri = params[0];
                writeAttachment(documentUri);
                return true;
            } catch (IOException e) {
                Timber.e(e, "Error saving attachment");
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                displayAttachmentNotSavedMessage();
            }
        }
    }
}
