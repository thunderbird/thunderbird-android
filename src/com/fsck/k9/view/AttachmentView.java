package com.fsck.k9.view;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.helper.MediaScannerNotifier;
import com.fsck.k9.helper.SizeFormatter;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.store.LocalStore.LocalAttachmentBodyPart;
import com.fsck.k9.provider.AttachmentProvider;
import org.apache.commons.io.IOUtils;


public class AttachmentView extends FrameLayout implements OnClickListener, OnLongClickListener {
    private Context context;
    private Button viewButton;
    private Button downloadButton;
    private LocalAttachmentBodyPart part;
    private Message message;
    private Account account;
    private MessagingController controller;
    private MessagingListener listener;
    private String name;
    private String contentType;
    private long size;

    private AttachmentFileDownloadCallback callback;

    public AttachmentView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    public AttachmentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public AttachmentView(Context context) {
        super(context);
        this.context = context;
    }


    public interface AttachmentFileDownloadCallback {
        /**
         * This method is called to ask the user to pick a directory to save the attachment to.
         * <p/>
         * After the user has selected a directory, the implementation of this interface has to call
         * {@link #writeFile(File)} on the object supplied as argument in order for the attachment to be saved.
         */
        public void pickDirectoryToSaveAttachmentTo(AttachmentView caller);
    }

    public void setButtonsEnabled(boolean enabled) {
        viewButton.setEnabled(enabled);
        downloadButton.setEnabled(enabled);
    }

    /**
     * Populates this view with information about the attachment.
     * <p>
     * This method also decides which attachments are displayed when the "show attachments" button
     * is pressed, and which attachments are only displayed after the "show more attachments"
     * button was pressed.<br>
     * Inline attachments with content ID and unnamed attachments fall into the second category.
     * </p>
     *
     * @return {@code true} for a regular attachment. {@code false} for attachments that should be initially hidden.
     */
    public boolean populateFromPart(Part inputPart, Message message, Account account,
            MessagingController controller, MessagingListener listener) throws MessagingException {

        part = (LocalAttachmentBodyPart) inputPart;
        this.message = message;
        this.account = account;
        this.controller = controller;
        this.listener = listener;

        boolean firstClassAttachment = extractAttachmentInformation(part);

        displayAttachmentInformation();

        return firstClassAttachment;
    }

    //TODO: extract this code to a helper class
    private boolean extractAttachmentInformation(Part part) throws MessagingException {
        boolean firstClassAttachment = true;

        contentType = MimeUtility.unfoldAndDecode(part.getContentType());
        String contentDisposition = MimeUtility.unfoldAndDecode(part.getDisposition());

        name = MimeUtility.getHeaderParameter(contentType, "name");
        if (name == null) {
            name = MimeUtility.getHeaderParameter(contentDisposition, "filename");
        }

        if (name == null) {
            firstClassAttachment = false;
            String extension = MimeUtility.getExtensionByMimeType(contentType);
            name = "noname" + ((extension != null) ? "." + extension : "");
        }

        // Inline parts with a content-id are almost certainly components of an HTML message
        // not attachments. Only show them if the user pressed the button to show more
        // attachments.
        if (contentDisposition != null &&
                MimeUtility.getHeaderParameter(contentDisposition, null).matches("^(?i:inline)")
                && part.getHeader(MimeHeader.HEADER_CONTENT_ID) != null) {
            firstClassAttachment = false;
        }

        String sizeParam = MimeUtility.getHeaderParameter(contentDisposition, "size");
        if (sizeParam != null) {
            try {
                size = Integer.parseInt(sizeParam);
            } catch (NumberFormatException e) { /* ignore */ }
        }

        contentType = MimeUtility.getMimeTypeForViewing(part.getMimeType(), name);
        return firstClassAttachment;
    }

    private void displayAttachmentInformation() {
        TextView attachmentName = (TextView) findViewById(R.id.attachment_name);
        TextView attachmentInfo = (TextView) findViewById(R.id.attachment_info);
        viewButton = (Button) findViewById(R.id.view);
        downloadButton = (Button) findViewById(R.id.download);

        if ((!MimeUtility.mimeTypeMatches(contentType, K9.ACCEPTABLE_ATTACHMENT_VIEW_TYPES))
                || (MimeUtility.mimeTypeMatches(contentType, K9.UNACCEPTABLE_ATTACHMENT_VIEW_TYPES))) {
            viewButton.setVisibility(View.GONE);
        }
        if ((!MimeUtility.mimeTypeMatches(contentType, K9.ACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES))
                || (MimeUtility.mimeTypeMatches(contentType, K9.UNACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES))) {
            downloadButton.setVisibility(View.GONE);
        }
        if (size > K9.MAX_ATTACHMENT_DOWNLOAD_SIZE) {
            viewButton.setVisibility(View.GONE);
            downloadButton.setVisibility(View.GONE);
        }

        viewButton.setOnClickListener(this);
        downloadButton.setOnClickListener(this);
        downloadButton.setOnLongClickListener(this);

        attachmentName.setText(name);
        attachmentInfo.setText(SizeFormatter.formatSize(context, size));

        ImageView thumbnail = (ImageView) findViewById(R.id.attachment_icon);
        new LoadAndDisplayThumbnailAsyncTask(thumbnail).execute();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.view: {
                onViewButtonClicked();
                break;
            }
            case R.id.download: {
                onSaveButtonClicked();
                break;
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (view.getId() == R.id.download) {
            callback.pickDirectoryToSaveAttachmentTo(this);
            return true;
        }

        return false;
    }

    private void onViewButtonClicked() {
        if (message != null) {
            controller.loadAttachment(account, message, part, new Object[] {false, this}, listener);
        }
    }


    private void onSaveButtonClicked() {
        boolean isExternalStorageMounted = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (!isExternalStorageMounted) {
            String message = context.getString(R.string.message_view_status_attachment_not_saved);
            displayMessageToUser(message);
            return;
        }

        if (message != null) {
            controller.loadAttachment(account, message, part, new Object[] {true, this}, listener);
        }
    }

    /**
     * Saves the attachment as file in the given directory
     */
    public void writeFile(File directory) {
        try {
            String filename = Utility.sanitizeFilename(name);
            File file = Utility.createUniqueFile(directory, filename);
            Uri uri = AttachmentProvider.getAttachmentUri(account, part.getAttachmentId());
            InputStream in = context.getContentResolver().openInputStream(uri);
            OutputStream out = new FileOutputStream(file);
            IOUtils.copy(in, out);
            out.flush();
            out.close();
            in.close();
            attachmentSaved(file.toString());
            new MediaScannerNotifier(context, file);
        } catch (IOException ioe) {
            if (K9.DEBUG) {
                Log.e(K9.LOG_TAG, "Error saving attachment", ioe);
            }
            attachmentNotSaved();
        }
    }

    public void writeFile() {
        writeFile(new File(K9.getAttachmentDefaultPath()));
    }

    public void showFile() {
        Uri uri = AttachmentProvider.getAttachmentUriForViewing(account, part.getAttachmentId());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        // We explicitly set the content type in addition to the URI because some attachment viewers
        // (such as Polaris office 3.0.x) choke on documents without a MIME type.
        intent.setDataAndType(uri, contentType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Could not display attachment of type " + contentType, e);

            String message = context.getString(R.string.message_view_no_viewer, contentType);
            displayMessageToUser(message);
        }
    }

    public void attachmentSaved(final String filename) {
        String message = context.getString(R.string.message_view_status_attachment_saved, filename);
        displayMessageToUser(message);
    }

    public void attachmentNotSaved() {
        String message = context.getString(R.string.message_view_status_attachment_not_saved);
        displayMessageToUser(message);
    }

    private void displayMessageToUser(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public void setCallback(AttachmentFileDownloadCallback callback) {
        this.callback = callback;
    }

    private class LoadAndDisplayThumbnailAsyncTask extends AsyncTask<Void, Void, Bitmap> {
        private final ImageView thumbnail;

        public LoadAndDisplayThumbnailAsyncTask(ImageView thumbnail) {
            this.thumbnail = thumbnail;
        }

        protected Bitmap doInBackground(Void... asyncTaskArgs) {
            return getPreviewIcon();
        }

        private Bitmap getPreviewIcon() {
            Bitmap icon = null;
            try {
                InputStream input = context.getContentResolver().openInputStream(
                        AttachmentProvider.getAttachmentThumbnailUri(account,
                                part.getAttachmentId(),
                                62,
                                62));
                icon = BitmapFactory.decodeStream(input);
                input.close();
            } catch (Exception e) {
                // We don't care what happened, we just return null for the preview icon.
            }

            return icon;
        }

        protected void onPostExecute(Bitmap previewIcon) {
            if (previewIcon != null) {
                thumbnail.setImageBitmap(previewIcon);
            } else {
                thumbnail.setImageResource(R.drawable.attached_image_placeholder);
            }
        }
    }
}
