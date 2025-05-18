package com.fsck.k9.ui.messageview;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.WorkerThread;

import com.fsck.k9.Preferences;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalPart;
import com.fsck.k9.provider.AttachmentTempFileProvider;
import com.fsck.k9.ui.R;

import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import app.k9mail.legacy.account.LegacyAccount;
import app.k9mail.legacy.message.controller.SimpleMessagingListener;
import timber.log.Timber;

public class AttachmentController {
    private final Context context;
    private final MessagingController controller;
    private final MessageViewFragment messageViewFragment;
    private final AttachmentViewInfo attachment;
    private final ViewIntentFinder viewIntentFinder;

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    AttachmentController(Context context, MessagingController controller, MessageViewFragment messageViewFragment,
        AttachmentViewInfo attachment) {
        this.context = context;
        this.controller = controller;
        this.messageViewFragment = messageViewFragment;
        this.attachment = attachment;
        this.viewIntentFinder = new ViewIntentFinder(context);
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
                showToast(context.getString(R.string.message_view_status_attachment_not_saved));
            }
        });
    }

    private void viewLocalAttachment() {
        executor.execute(() -> {
            final Intent intent = getBestViewIntent();
            mainHandler.post(() -> {
                if (intent != null) {
                    try {
                        context.startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Timber.e(e, "Could not display attachment of type %s", attachment.mimeType);
                        showToast(context.getString(R.string.message_view_no_viewer, attachment.mimeType));
                    }
                }
            });
        });
    }

    private void saveLocalAttachmentTo(Uri documentUri) {
        executor.execute(() -> {
            boolean success;
            try {
                writeAttachment(documentUri);
                success = true;
            } catch (IOException e) {
                Timber.e(e, "Error saving attachment");
                success = false;
            }
            final boolean finalSuccess = success;
            mainHandler.post(() -> {
                if (!finalSuccess) {
                    showToast(context.getString(R.string.message_view_status_attachment_not_saved));
                }
            });
        });
    }

    private void writeAttachment(Uri documentUri) throws IOException {
        ContentResolver contentResolver = context.getContentResolver();

        try (InputStream in = contentResolver.openInputStream(attachment.internalUri);
             OutputStream out = contentResolver.openOutputStream(documentUri, "wt")) {
            if (in == null || out == null) {
                throw new IOException("Could not open streams for attachment");
            }
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();
        }
    }

    @WorkerThread
    private Intent getBestViewIntent() {
        try {
            Uri intentDataUri = AttachmentTempFileProvider.createTempUriForContentUri(context, attachment.internalUri, renameAttachment(attachment.displayName));
            Intent intent = viewIntentFinder.getBestViewIntent(intentDataUri, attachment.displayName, attachment.mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return intent;
        } catch (IOException e) {
            Timber.e(e, "Error creating temp file for attachment!");
            return null;
        }
    }

    private void showToast(final String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }

    /**
     * Rename attachment to fix duplicate naming from "file.pdf (1)" to "file (1).pdf"
     */
    private String renameAttachment(String originalName) {
        if (originalName == null) return null;

        // Extract base name and extension
        String baseName = FilenameUtils.getBaseName(originalName);
        String extension = FilenameUtils.getExtension(originalName);

        // Check if originalName ends with " (number)" but before the extension
        // Example: "file.pdf (1)" → should become "file (1).pdf"
        // We detect if baseName ends with " (number)"
        // Then reconstruct with extension

        if (extension.isEmpty()) {
            // No extension, no rename needed
            return originalName;
        }

        // Pattern: check if baseName contains a duplicate index, e.g. "file.pdf (1)" means baseName is "file.pdf (1)"
        // So if baseName contains an extension-like string, we fix it.

        // If baseName ends with ")" and contains " (", check if extension actually belongs to inside baseName
        if (baseName.endsWith(")") && baseName.contains(" (")) {
            int lastParenIndex = baseName.lastIndexOf(')');
            int lastSpaceIndex = baseName.lastIndexOf('(');
            if (lastSpaceIndex < lastParenIndex) {
                String possibleExtension = baseName.substring(lastSpaceIndex - 1, lastSpaceIndex); // might be dot
                // Just move extension outside
                String newBaseName = baseName.substring(0, lastSpaceIndex - 1) + baseName.substring(lastSpaceIndex, lastParenIndex + 1);
                return newBaseName + "." + extension;
            }
        }

        // If original name is normal, just return
        return originalName;
    }
}
