package com.fsck.k9.view;


import java.util.Calendar;
import java.util.Locale;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener;
import com.fsck.k9.Account;
import com.fsck.k9.DI;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.activity.misc.ContactPicture;
import com.fsck.k9.contacts.ContactPictureLoader;
import com.fsck.k9.helper.ClipboardManager;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.ui.ContactBadge;
import com.fsck.k9.ui.R;
import com.fsck.k9.ui.messageview.OnCryptoClickListener;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;


public class MessageHeader extends LinearLayout implements OnClickListener, OnLongClickListener {
    private static final int DEFAULT_SUBJECT_LINES = 3;

    private final ClipboardManager clipboardManager = DI.get(ClipboardManager.class);

    private final Context mContext;
    private TextView mFromView;
    private TextView mDateView;
    private TextView mToCount;
    private TextView mSubjectView;
    private ImageView mCryptoStatusIcon;
    private Chip mAccountChip;
    private ImageView mFlagged;
    private int defaultSubjectColor;
    private final FontSizes mFontSizes = K9.getFontSizes();
    private MessageHelper mMessageHelper;
    private ContactPictureLoader mContactsPictureLoader;
    private ContactBadge mContactBadge;
    private OnMenuItemClickListener onMenuItemClickListener;


    public MessageHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mFromView = findViewById(R.id.from);
        mToCount = findViewById(R.id.to_count);

        mContactBadge = findViewById(R.id.contact_badge);
        mContactBadge.setClickable(false);

        View singleMessageOptionIcon = findViewById(R.id.icon_single_message_options);

        mSubjectView = findViewById(R.id.subject);
        mAccountChip = findViewById(R.id.chip);
        mDateView = findViewById(R.id.date);
        mFlagged = findViewById(R.id.flagged);

        defaultSubjectColor = mSubjectView.getCurrentTextColor();
        mFontSizes.setViewTextSize(mSubjectView, mFontSizes.getMessageViewSubject());
        mFontSizes.setViewTextSize(mDateView, mFontSizes.getMessageViewDate());
        mFontSizes.setViewTextSize(mFromView, mFontSizes.getMessageViewSender());

        singleMessageOptionIcon.setOnClickListener(this);

        findViewById(R.id.participants_container).setOnClickListener(this);
        mSubjectView.setOnClickListener(this);
        mSubjectView.setOnLongClickListener(this);
        mCryptoStatusIcon = findViewById(R.id.crypto_status_icon);
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
        } else if (id == R.id.participants_container) {
            Snackbar.make(getRootView(), "TODO: Display details popup", Snackbar.LENGTH_LONG).show();
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

        mFromView.setText(mMessageHelper.getSenderDisplayName(fromAddress));

        if (K9.isShowContactPicture()) {
            mContactBadge.setVisibility(View.VISIBLE);
            mContactsPictureLoader = ContactPicture.getContactPictureLoader();
        }  else {
            mContactBadge.setVisibility(View.GONE);
        }

        Calendar sentDate = Calendar.getInstance();
        sentDate.setTime(message.getSentDate());
        Calendar today = Calendar.getInstance();

        int flags = DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_TIME;
        if (today.get(Calendar.YEAR) != sentDate.get(Calendar.YEAR)) {
            flags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        } else if (today.get(Calendar.DAY_OF_YEAR) != sentDate.get(Calendar.DAY_OF_YEAR)) {
            flags |= DateUtils.FORMAT_SHOW_DATE;
        }
        mDateView.setText(DateUtils.formatDateTime(mContext, sentDate.getTimeInMillis(), flags));

        if (K9.isShowContactPicture()) {
            if (fromAddress != null) {
                mContactBadge.setContact(fromAddress);
                mContactsPictureLoader.setContactPicture(mContactBadge, fromAddress);
            } else {
                mContactBadge.setImageResource(R.drawable.ic_contact_picture);
            }
        }

        if (showStar) {
            mFlagged.setVisibility(View.VISIBLE);
            mFlagged.setSelected(message.isSet(Flag.FLAGGED));
        } else {
            mFlagged.setVisibility(View.GONE);
        }

        Address[] toRecipients = message.getRecipients(RecipientType.TO);
        Address[] ccRecipients = message.getRecipients(RecipientType.CC);
        Address[] bccRecipients = message.getRecipients(RecipientType.BCC);
        int recipientCount = toRecipients.length + ccRecipients.length + bccRecipients.length;
        mToCount.setText(recipientCount <= 1 ? "" : String.format(Locale.getDefault(), "+%d", recipientCount - 1));

        mAccountChip.setText(account.getDisplayName());
        mAccountChip.setChipBackgroundColor(ColorStateList.valueOf(account.getChipColor()));

        setVisibility(View.VISIBLE);
    }

    public void setSubject(@NonNull String subject) {
        mSubjectView.setText(subject);
        mSubjectView.setTextColor(0xff000000 | defaultSubjectColor);
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

    public void setOnCryptoClickListener(OnCryptoClickListener onCryptoClickListener) {
        // No-op for now
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener onMenuItemClickListener) {
        this.onMenuItemClickListener = onMenuItemClickListener;
    }
}
