package com.fsck.k9.ui.messageview;


import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.fsck.k9.K9;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.ui.R;
import com.fsck.k9.ui.helper.ContextHelper;
import com.fsck.k9.ui.helper.SizeFormatter;
import com.google.android.material.textview.MaterialTextView;


public class AttachmentView extends FrameLayout implements OnClickListener {
    private final SizeFormatter sizeFormatter;

    private AttachmentViewInfo attachment;
    private AttachmentViewCallback callback;

    private View cardView;
    private View saveButton;
    private ImageView preview;
    private ImageView attachmentType;


    public AttachmentView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        sizeFormatter = new SizeFormatter(context.getResources());
    }

    public AttachmentView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AttachmentView(Context context) {
        this(context, null);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        cardView = findViewById(R.id.attachment_card);
        saveButton = findViewById(R.id.save_button);
        preview = findViewById(R.id.attachment_preview);
        attachmentType = findViewById(R.id.attachment_type);
    }

    public void enableButtons() {
        setEnabled(true);
        saveButton.setVisibility(View.INVISIBLE);
    }

    public void disableButtons() {
        setEnabled(false);
        saveButton.setVisibility(View.VISIBLE);
    }

    public void setAttachment(AttachmentViewInfo attachment) {
        this.attachment = attachment;

        displayAttachmentInformation();
    }

    private void displayAttachmentInformation() {
        if (attachment.size > K9.MAX_ATTACHMENT_DOWNLOAD_SIZE) {
            saveButton.setVisibility(View.INVISIBLE);
        }

        cardView.setOnClickListener(this);
        saveButton.setOnClickListener(this);

        MaterialTextView attachmentName = findViewById(R.id.attachment_name);
        attachmentName.setText(attachment.displayName);

        setAttachmentSize(attachment.size);

        if (attachment.isSupportedImage()) {
            attachmentType.setImageResource(Icons.Outlined.Image);
            if (attachment.isContentAvailable()) {
                refreshThumbnail();
            }
        } else {
            preview.setVisibility(View.GONE);
        }
    }

    private void setAttachmentSize(long size) {
        MaterialTextView attachmentSize = findViewById(R.id.attachment_size);
        if (size == AttachmentViewInfo.UNKNOWN_SIZE) {
            attachmentSize.setText("");
        } else {
            String text = sizeFormatter.formatSize(size);
            attachmentSize.setText(text);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.attachment_card) {
            onViewButtonClick();
        } else if (view.getId() == R.id.save_button) {
            onSaveButtonClick();
        }
    }

    private void onViewButtonClick() {
        callback.onViewAttachment(attachment);
    }

    private void onSaveButtonClick() {
        callback.onSaveAttachment(attachment);
    }

    public void setCallback(AttachmentViewCallback callback) {
        this.callback = callback;
    }

    public void refreshThumbnail() {
        Context context = getContext();
        Activity activity = ContextHelper.findActivity(context);
        if (activity != null && activity.isDestroyed()) {
            // Do nothing because Glide would throw an exception
            return;
        }

        preview.setVisibility(View.VISIBLE);
        Glide.with(context)
                .load(attachment.internalUri)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(preview);
    }
}
