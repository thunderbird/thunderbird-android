package com.fsck.k9.view;


import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
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
import com.fsck.k9.ui.R;
import com.fsck.k9.ui.helper.RelativeDateTimeFormatter;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;


public class MessageHeader extends LinearLayout implements OnClickListener, OnLongClickListener {
    private static final int DEFAULT_SUBJECT_LINES = 3;

    private Chip accountChip;
    private TextView subjectView;
    private ImageView starView;
    private ImageView contactPictureView;
    private TextView fromView;
    private ImageView cryptoStatusIcon;
    private TextView dateView;

    private MessageHelper messageHelper;
    private RelativeDateTimeFormatter relativeDateTimeFormatter;

    private OnMenuItemClickListener onMenuItemClickListener;


    public MessageHeader(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (!isInEditMode()) {
            messageHelper = MessageHelper.getInstance(getContext());
            relativeDateTimeFormatter = DI.get(RelativeDateTimeFormatter.class);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        accountChip = findViewById(R.id.chip);
        subjectView = findViewById(R.id.subject);
        starView = findViewById(R.id.flagged);
        contactPictureView = findViewById(R.id.contact_picture);
        fromView = findViewById(R.id.from);
        cryptoStatusIcon = findViewById(R.id.crypto_status_icon);
        dateView = findViewById(R.id.date);

        subjectView.setOnClickListener(this);
        subjectView.setOnLongClickListener(this);

        View menuPrimaryActionView = findViewById(R.id.menu_primary_action);
        menuPrimaryActionView.setOnClickListener(this);
        menuPrimaryActionView.setOnLongClickListener(this);

        View menuOverflowView = findViewById(R.id.menu_overflow);
        menuOverflowView.setOnClickListener(this);
        menuOverflowView.setOnLongClickListener(this);

        findViewById(R.id.participants_container).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.subject) {
            toggleSubjectViewMaxLines();
        } else if (id == R.id.menu_primary_action) {
            Snackbar.make(getRootView(), "TODO: Perform primary action", Snackbar.LENGTH_LONG).show();
        } else if (id == R.id.menu_overflow) {
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
            onAddSubjectToClipboard(subjectView.getText().toString());
        }

        return true;
    }

    private void toggleSubjectViewMaxLines() {
        if (subjectView.getMaxLines() == DEFAULT_SUBJECT_LINES) {
            subjectView.setMaxLines(Integer.MAX_VALUE);
        } else {
            subjectView.setMaxLines(DEFAULT_SUBJECT_LINES);
        }
    }

    private void onAddSubjectToClipboard(String subject) {
        ClipboardManager clipboardManager = DI.get(ClipboardManager.class);
        clipboardManager.setText("subject", subject);

        Toast.makeText(getContext(), createMessageForSubject(), Toast.LENGTH_LONG).show();
    }

    public String createMessageForSubject() {
        return getResources().getString(R.string.copy_subject_to_clipboard);
    }

    public void setOnFlagListener(OnClickListener listener) {
        starView.setOnClickListener(listener);
    }

    public void populate(final Message message, final Account account, boolean showStar, boolean showAccountChip) {
        if (showAccountChip) {
            accountChip.setVisibility(View.VISIBLE);
            accountChip.setText(account.getDisplayName());
            accountChip.setChipBackgroundColor(ColorStateList.valueOf(account.getChipColor()));
        } else {
            accountChip.setVisibility(View.GONE);
        }

        Address fromAddress = null;
        Address[] fromAddresses = message.getFrom();
        if (fromAddresses.length > 0) {
            fromAddress = fromAddresses[0];
        }

        if (K9.isShowContactPicture()) {
            contactPictureView.setVisibility(View.VISIBLE);
            if (fromAddress != null) {
                ContactPictureLoader contactsPictureLoader = ContactPicture.getContactPictureLoader();
                contactsPictureLoader.setContactPicture(contactPictureView, fromAddress);
            } else {
                contactPictureView.setImageResource(R.drawable.ic_contact_picture);
            }
        } else {
            contactPictureView.setVisibility(View.GONE);
        }

        CharSequence from = messageHelper.getSenderDisplayName(fromAddress);
        fromView.setText(from);

        if (showStar) {
            starView.setVisibility(View.VISIBLE);
            starView.setSelected(message.isSet(Flag.FLAGGED));
        } else {
            starView.setVisibility(View.GONE);
        }

        if (message.getSentDate() != null) {
            dateView.setText(relativeDateTimeFormatter.formatDate(message.getSentDate().getTime()));
        } else {
            dateView.setText("");
        }

        setVisibility(View.VISIBLE);
    }

    public void setSubject(@NonNull String subject) {
        subjectView.setText(subject);
    }

    public void hideCryptoStatus() {
        cryptoStatusIcon.setVisibility(View.GONE);
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
        cryptoStatusIcon.setEnabled(displayStatus.isEnabled());
        cryptoStatusIcon.setVisibility(View.VISIBLE);
        cryptoStatusIcon.setImageResource(displayStatus.getStatusIconRes());
        cryptoStatusIcon.setColorFilter(color);
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener onMenuItemClickListener) {
        this.onMenuItemClickListener = onMenuItemClickListener;
    }
}
