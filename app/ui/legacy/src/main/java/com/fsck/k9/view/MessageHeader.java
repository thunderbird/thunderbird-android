package com.fsck.k9.view;


import java.util.Arrays;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
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
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.activity.misc.ContactPicture;
import com.fsck.k9.contacts.ContactPictureLoader;
import com.fsck.k9.helper.ClipboardManager;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.ui.ContactBadge;
import com.fsck.k9.ui.R;
import com.fsck.k9.ui.messageview.OnCryptoClickListener;
import timber.log.Timber;


public class MessageHeader extends LinearLayout implements OnClickListener, OnLongClickListener {
    private static final int DEFAULT_SUBJECT_LINES = 3;

    private final ClipboardManager clipboardManager = DI.get(ClipboardManager.class);

    private Context mContext;
    private TextView mFromView;
    private TextView mSenderView;
    private TextView mDateView;
    private TextView mToView;
    private TextView mToLabel;
    private TextView mCcView;
    private TextView mCcLabel;
    private TextView mBccView;
    private TextView mBccLabel;
    private TextView mSubjectView;
    private ImageView mCryptoStatusIcon;

    private View mChip;
    private CheckBox mFlagged;
    private int defaultSubjectColor;
    private View singleMessageOptionIcon;
    private View mAnsweredIcon;
    private View mForwardedIcon;
    private Message mMessage;
    private Account mAccount;
    private FontSizes mFontSizes = K9.getFontSizes();
    private Contacts mContacts;

    private MessageHelper mMessageHelper;
    private ContactPictureLoader mContactsPictureLoader;
    private ContactBadge mContactBadge;

    private OnCryptoClickListener onCryptoClickListener;
    private OnMenuItemClickListener onMenuItemClickListener;


    public MessageHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mContacts = Contacts.getInstance(mContext);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mAnsweredIcon = findViewById(R.id.answered);
        mForwardedIcon = findViewById(R.id.forwarded);
        mFromView = findViewById(R.id.from);
        mSenderView = findViewById(R.id.sender);
        mToView = findViewById(R.id.to);
        mToLabel = findViewById(R.id.to_label);
        mCcView = findViewById(R.id.cc);
        mCcLabel = findViewById(R.id.cc_label);
        mBccView = findViewById(R.id.bcc);
        mBccLabel = findViewById(R.id.bcc_label);

        mContactBadge = findViewById(R.id.contact_badge);

        singleMessageOptionIcon = findViewById(R.id.icon_single_message_options);

        mSubjectView = findViewById(R.id.subject);
        mChip = findViewById(R.id.chip);
        mDateView = findViewById(R.id.date);
        mFlagged = findViewById(R.id.flagged);

        defaultSubjectColor = mSubjectView.getCurrentTextColor();
        mFontSizes.setViewTextSize(mSubjectView, mFontSizes.getMessageViewSubject());
        mFontSizes.setViewTextSize(mDateView, mFontSizes.getMessageViewDate());

        mFontSizes.setViewTextSize(mFromView, mFontSizes.getMessageViewSender());
        mFontSizes.setViewTextSize(mToView, mFontSizes.getMessageViewTo());
        mFontSizes.setViewTextSize(mToLabel, mFontSizes.getMessageViewTo());
        mFontSizes.setViewTextSize(mCcView, mFontSizes.getMessageViewCC());
        mFontSizes.setViewTextSize(mCcLabel, mFontSizes.getMessageViewCC());
        mFontSizes.setViewTextSize(mBccView, mFontSizes.getMessageViewBCC());
        mFontSizes.setViewTextSize(mBccLabel, mFontSizes.getMessageViewBCC());

        singleMessageOptionIcon.setOnClickListener(this);

        mSubjectView.setOnClickListener(this);
        mFromView.setOnClickListener(this);
        mToView.setOnClickListener(this);
        mCcView.setOnClickListener(this);
        mBccView.setOnClickListener(this);

        mSubjectView.setOnLongClickListener(this);
        mFromView.setOnLongClickListener(this);
        mToView.setOnLongClickListener(this);
        mCcView.setOnLongClickListener(this);
        mBccView.setOnLongClickListener(this);

        mCryptoStatusIcon = findViewById(R.id.crypto_status_icon);
        mCryptoStatusIcon.setOnClickListener(this);

        mMessageHelper = MessageHelper.getInstance(mContext);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.subject) {
            toggleSubjectViewMaxLines();
        } else if (id == R.id.from) {
            onAddSenderToContacts();
        } else if (id == R.id.to || id == R.id.cc || id == R.id.bcc) {
            expand((TextView)view, ((TextView)view).getEllipsize() != null);
        } else if (id == R.id.crypto_status_icon) {
            onCryptoClickListener.onCryptoClick();
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
        } else if (id == R.id.from) {
            onAddAddressesToClipboard(mMessage.getFrom());
        } else if (id == R.id.to) {
            onAddRecipientsToClipboard(Message.RecipientType.TO);
        } else if (id == R.id.cc) {
            onAddRecipientsToClipboard(Message.RecipientType.CC);
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

    private void onAddSenderToContacts() {
        if (mMessage != null) {
            try {
                final Address senderEmail = mMessage.getFrom()[0];
                mContacts.createContact(senderEmail);
            } catch (Exception e) {
                Timber.e(e, "Couldn't create contact");
            }
        }
    }

    public String createMessageForSubject() {
        return  mContext.getResources().getString(R.string.copy_subject_to_clipboard);
    }

    public String createMessage(int addressesCount) {
        return mContext.getResources().getQuantityString(R.plurals.copy_address_to_clipboard, addressesCount);
    }

    private void onAddAddressesToClipboard(Address[] addresses) {
        String addressList = Address.toString(addresses);
        clipboardManager.setText("addresses", addressList);

        Toast.makeText(mContext, createMessage(addresses.length), Toast.LENGTH_LONG).show();
    }

    private void onAddRecipientsToClipboard(Message.RecipientType recipientType) {
        onAddAddressesToClipboard(mMessage.getRecipients(recipientType));
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

        final Contacts contacts = K9.isShowContactName() ? mContacts : null;
        final CharSequence from = mMessageHelper.getSenderDisplayName(fromAddress);
        final CharSequence to = MessageHelper.toFriendly(message.getRecipients(Message.RecipientType.TO), contacts);
        final CharSequence cc = MessageHelper.toFriendly(message.getRecipients(Message.RecipientType.CC), contacts);
        final CharSequence bcc = MessageHelper.toFriendly(message.getRecipients(Message.RecipientType.BCC), contacts);

        mMessage = message;
        mAccount = account;

        if (K9.isShowContactPicture()) {
            mContactBadge.setVisibility(View.VISIBLE);
            mContactsPictureLoader = ContactPicture.getContactPictureLoader();
        }  else {
            mContactBadge.setVisibility(View.GONE);
        }

        if (shouldShowSender(message)) {
            mSenderView.setVisibility(VISIBLE);
            String sender = getResources().getString(R.string.message_view_sender_label,
                    MessageHelper.toFriendly(message.getSender(), contacts));
            mSenderView.setText(sender);
        } else {
            mSenderView.setVisibility(View.GONE);
        }

        String dateTime = DateUtils.formatDateTime(mContext,
                message.getSentDate().getTime(),
                DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_ABBREV_ALL
                | DateUtils.FORMAT_SHOW_TIME
                | DateUtils.FORMAT_SHOW_YEAR);
        mDateView.setText(dateTime);

        if (K9.isShowContactPicture()) {
            if (fromAddress != null) {
                mContactBadge.setContact(fromAddress);
                mContactsPictureLoader.setContactPicture(mContactBadge, fromAddress);
            } else {
                mContactBadge.setImageResource(R.drawable.ic_contact_picture);
            }
        }

        mFromView.setText(from);

        updateAddressField(mToView, to, mToLabel);
        updateAddressField(mCcView, cc, mCcLabel);
        updateAddressField(mBccView, bcc, mBccLabel);
        mAnsweredIcon.setVisibility(message.isSet(Flag.ANSWERED) ? View.VISIBLE : View.GONE);
        mForwardedIcon.setVisibility(message.isSet(Flag.FORWARDED) ? View.VISIBLE : View.GONE);

        if (showStar) {
            mFlagged.setVisibility(View.VISIBLE);
            mFlagged.setChecked(message.isSet(Flag.FLAGGED));
        } else {
            mFlagged.setVisibility(View.GONE);
        }

        mChip.setBackgroundColor(mAccount.getChipColor());

        setVisibility(View.VISIBLE);
    }

    public void setSubject(@NonNull String subject) {
        mSubjectView.setText(subject);
        mSubjectView.setTextColor(0xff000000 | defaultSubjectColor);
    }

    public static boolean shouldShowSender(Message message) {
        Address[] from = message.getFrom();
        Address[] sender = message.getSender();

        return sender != null && sender.length != 0 && !Arrays.equals(from, sender);
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

    private void updateAddressField(TextView v, CharSequence text, View label) {
        boolean hasText = !TextUtils.isEmpty(text);

        v.setText(text);
        v.setVisibility(hasText ? View.VISIBLE : View.GONE);
        label.setVisibility(hasText ? View.VISIBLE : View.GONE);
    }

    /**
     * Expand or collapse a TextView by removing or adding the 2 lines limitation
     */
    private void expand(TextView v, boolean expand) {
       if (expand) {
           v.setMaxLines(Integer.MAX_VALUE);
           v.setEllipsize(null);
       } else {
           v.setMaxLines(2);
           v.setEllipsize(android.text.TextUtils.TruncateAt.END);
       }
    }

    public void setOnCryptoClickListener(OnCryptoClickListener onCryptoClickListener) {
        this.onCryptoClickListener = onCryptoClickListener;
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener onMenuItemClickListener) {
        this.onMenuItemClickListener = onMenuItemClickListener;
    }
}
