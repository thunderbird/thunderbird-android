package com.fsck.k9.ui.messageview;


import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.helper.SizeFormatter;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.AttachmentViewInfo;


public class AttachmentView extends FrameLayout implements OnClickListener, OnLongClickListener {
    private AttachmentViewInfo attachment;
    private AttachmentViewCallback callback;

    private Button viewButton;
    private Button downloadButton;


    public AttachmentView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public AttachmentView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AttachmentView(Context context) {
        super(context);
    }

    public AttachmentViewInfo getAttachment() {
        return attachment;
    }

    public void enableViewButton() {
        viewButton.setEnabled(true);
    }

    public void disableViewButton() {
        viewButton.setEnabled(false);
    }

    public void setAttachment(AttachmentViewInfo attachment) throws MessagingException {
        this.attachment = attachment;

        displayAttachmentInformation();
    }

    private void displayAttachmentInformation() {
        TextView attachmentName = (TextView) findViewById(R.id.attachment_name);
        TextView attachmentInfo = (TextView) findViewById(R.id.attachment_info);
        viewButton = (Button) findViewById(R.id.view);
        downloadButton = (Button) findViewById(R.id.download);

        if (attachment.size > K9.MAX_ATTACHMENT_DOWNLOAD_SIZE) {
            viewButton.setVisibility(View.GONE);
            downloadButton.setVisibility(View.GONE);
        }

        viewButton.setOnClickListener(this);
        downloadButton.setOnClickListener(this);
        downloadButton.setOnLongClickListener(this);

        attachmentName.setText(attachment.displayName);
        attachmentInfo.setText(SizeFormatter.formatSize(getContext(), attachment.size));

        ImageView thumbnail = (ImageView) findViewById(R.id.attachment_icon);
        new LoadAndDisplayThumbnailAsyncTask(thumbnail).execute();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.view: {
                onViewButtonClick();
                break;
            }
            case R.id.download: {
                onSaveButtonClick();
                break;
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (view.getId() == R.id.download) {
            onSaveButtonLongClick();
            return true;
        }

        return false;
    }

    private void onViewButtonClick() {
        callback.onViewAttachment(attachment);
    }

    private void onSaveButtonClick() {
        callback.onSaveAttachment(attachment);
    }

    private void onSaveButtonLongClick() {
        callback.onSaveAttachmentToUserProvidedDirectory(attachment);
    }

    public void setCallback(AttachmentViewCallback callback) {
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
            //FIXME - temporarily disabled
            return null;
//            Bitmap icon = null;
//            try {
//                InputStream input = context.getContentResolver().openInputStream(
//                        AttachmentProvider.getAttachmentThumbnailUri(account,
//                                part.getAttachmentId(),
//                                62,
//                                62));
//                icon = BitmapFactory.decodeStream(input);
//                input.close();
//            } catch (Exception e) {
//                // We don't care what happened, we just return null for the preview icon.
//            }
//
//            return icon;
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
