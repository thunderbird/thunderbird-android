package com.fsck.k9.view;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener;
import com.fsck.k9.Account;
import com.fsck.k9.DI;
import com.fsck.k9.K9;
import com.fsck.k9.activity.misc.ContactPicture;
import com.fsck.k9.contacts.ContactPictureLoader;
import com.fsck.k9.helper.ClipboardManager;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.ui.ContactBadge;
import com.fsck.k9.ui.R;


public class MessageHeader extends LinearLayout implements OnClickListener, OnLongClickListener {
    private static final int DEFAULT_SUBJECT_LINES = 3;

    private final ClipboardManager clipboardManager = DI.get(ClipboardManager.class);

    private Context mContext;
    private TextView mFromView;
    private TextView mSubjectView;
    private ImageView mCryptoStatusIcon;
    private CheckBox mFlagged;
    private ContactBadge mContactBadge;

    private MessageHelper mMessageHelper;
    private ContactPictureLoader mContactsPictureLoader;

    private OnMenuItemClickListener onMenuItemClickListener;


    public MessageHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mFromView = findViewById(R.id.from);
        mContactBadge = findViewById(R.id.contact_badge);
        mSubjectView = findViewById(R.id.subject);
        mFlagged = findViewById(R.id.flagged);
        mCryptoStatusIcon = findViewById(R.id.crypto_status_icon);

        mSubjectView.setOnClickListener(this);
        mSubjectView.setOnLongClickListener(this);

        mMessageHelper = MessageHelper.getInstance(mContext);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.subject) {
            toggleSubjectViewMaxLines();
        } else if (id == R.id.icon_single_message_options) {
            PopupMenu popupMenu = new PopupMenu(getContext(), view);
            popupMenu.setOnMenuItemClickListener(onMenuItemClickListener);
            popupMenu.inflate(R.menu.single_message_options);
            popupMenu.show();
        }
    }

    @Override
    public boolean onLongClick(View view) {
        int id = view.getId();

        if (id == R.id.subject) {
            onAddSubjectToClipboard(mSubjectView.getText().toString());
        }

        return true;
    }

    private void toggleSubjectViewMaxLines() {
        if (mSubjectView.getMaxLines() == DEFAULT_SUBJECT_LINES) {
            mSubjectView.setMaxLines(Integer.MAX_VALUE);
        } else {
            mSubjectView.setMaxLines(DEFAULT_SUBJECT_LINES);
        }
    }

    private void onAddSubjectToClipboard(String subject) {
        clipboardManager.setText("subject", subject);

        Toast.makeText(mContext, createMessageForSubject(), Toast.LENGTH_LONG).show();
    }

    public String createMessageForSubject() {
        return  mContext.getResources().getString(R.string.copy_subject_to_clipboard);
    }

    public void setOnFlagListener(OnClickListener listener) {
        mFlagged.setOnClickListener(listener);
    }

    public void populate(final Message message, final Account account, boolean showStar) {
        Address fromAddress = null;
        Address[] fromAddresses = message.getFrom();
        if (fromAddresses.length > 0) {
            fromAddress = fromAddresses[0];
        }

        final CharSequence from = mMessageHelper.getSenderDisplayName(fromAddress);

        if (K9.isShowContactPicture()) {
            mContactBadge.setVisibility(View.VISIBLE);
            mContactsPictureLoader = ContactPicture.getContactPictureLoader();
        }  else {
            mContactBadge.setVisibility(View.GONE);
        }

        if (K9.isShowContactPicture()) {
            if (fromAddress != null) {
                mContactBadge.setContact(fromAddress);
                mContactsPictureLoader.setContactPicture(mContactBadge, fromAddress);
            } else {
                mContactBadge.setImageResource(R.drawable.ic_contact_picture);
            }
        }

        mFromView.setText(from);

        if (showStar) {
            mFlagged.setVisibility(View.VISIBLE);
            mFlagged.setChecked(message.isSet(Flag.FLAGGED));
        } else {
            mFlagged.setVisibility(View.GONE);
        }

        setVisibility(View.VISIBLE);
    }

    public void setSubject(@NonNull String subject) {
        mSubjectView.setText(subject);
    }

    public void hideCryptoStatus() {
        mCryptoStatusIcon.setVisibility(View.GONE);
    }

    public void setCryptoStatusLoading() {
        setCryptoDisplayStatus(MessageCryptoDisplayStatus.LOADING);
    }

    public void setCryptoStatusDisabled() {
        setCryptoDisplayStatus(MessageCryptoDisplayStatus.DISABLED);
    }

    public void setCryptoStatus(MessageCryptoDisplayStatus displayStatus) {
        setCryptoDisplayStatus(displayStatus);
    }

    private void setCryptoDisplayStatus(MessageCryptoDisplayStatus displayStatus) {
        int color = ThemeUtils.getStyledColor(getContext(), displayStatus.getColorAttr());
        mCryptoStatusIcon.setEnabled(displayStatus.isEnabled());
        mCryptoStatusIcon.setVisibility(View.VISIBLE);
        mCryptoStatusIcon.setImageResource(displayStatus.getStatusIconRes());
        mCryptoStatusIcon.setColorFilter(color);
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener onMenuItemClickListener) {
        this.onMenuItemClickListener = onMenuItemClickListener;
    }
}
