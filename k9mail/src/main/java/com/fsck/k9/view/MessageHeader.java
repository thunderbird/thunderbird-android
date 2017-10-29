package com.fsck.k9.view;


import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import timber.log.Timber;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.misc.ContactPictureLoader;
import com.fsck.k9.helper.ClipboardManager;
import com.fsck.k9.helper.ContactPicture;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.ui.messageview.OnCryptoClickListener;
import com.fsck.k9.ui.ContactBadge;


public class MessageHeader extends LinearLayout implements OnClickListener, OnLongClickListener {
    private Context context;
    private TextView fromView;
    private TextView senderView;
    private TextView dateView;
    private TextView toView;
    private TextView toLabel;
    private TextView ccView;
    private TextView ccLabel;
    private TextView bccView;
    private TextView bccLabel;
    private TextView subjectView;
    private MessageCryptoStatusView cryptoStatusIcon;

    private View chip;
    private CheckBox flagged;
    private int defaultSubjectColor;
    private TextView additionalHeadersView;
    private View answeredIcon;
    private View forwardedIcon;
    private Message message;
    private Account account;
    private FontSizes fontSizes = K9.getFontSizes();
    private Contacts contacts;
    private SavedState savedState;

    private MessageHelper messageHelper;
    private ContactPictureLoader contactPictureLoader;
    private ContactBadge contactBadge;

    private OnLayoutChangedListener onLayoutChangedListener;
    private OnCryptoClickListener onCryptoClickListener;

    /**
     * Pair class is only available since API Level 5, so we need
     * this helper class unfortunately
     */
    private static class HeaderEntry {
        public String label;
        public String value;

        public HeaderEntry(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    public MessageHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        contacts = Contacts.getInstance(this.context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        answeredIcon = findViewById(R.id.answered);
        forwardedIcon = findViewById(R.id.forwarded);
        fromView = (TextView) findViewById(R.id.from);
        senderView = (TextView) findViewById(R.id.sender);
        toView = (TextView) findViewById(R.id.to);
        toLabel = (TextView) findViewById(R.id.to_label);
        ccView = (TextView) findViewById(R.id.cc);
        ccLabel = (TextView) findViewById(R.id.cc_label);
        bccView = (TextView) findViewById(R.id.bcc);
        bccLabel = (TextView) findViewById(R.id.bcc_label);

        contactBadge = (ContactBadge) findViewById(R.id.contact_badge);

        subjectView = (TextView) findViewById(R.id.subject);
        additionalHeadersView = (TextView) findViewById(R.id.additional_headers_view);
        chip = findViewById(R.id.chip);
        dateView = (TextView) findViewById(R.id.date);
        flagged = (CheckBox) findViewById(R.id.flagged);

        defaultSubjectColor = subjectView.getCurrentTextColor();
        fontSizes.setViewTextSize(subjectView, fontSizes.getMessageViewSubject());
        fontSizes.setViewTextSize(dateView, fontSizes.getMessageViewDate());
        fontSizes.setViewTextSize(additionalHeadersView, fontSizes.getMessageViewAdditionalHeaders());

        fontSizes.setViewTextSize(fromView, fontSizes.getMessageViewSender());
        fontSizes.setViewTextSize(toView, fontSizes.getMessageViewTo());
        fontSizes.setViewTextSize(toLabel, fontSizes.getMessageViewTo());
        fontSizes.setViewTextSize(ccView, fontSizes.getMessageViewCC());
        fontSizes.setViewTextSize(ccLabel, fontSizes.getMessageViewCC());
        fontSizes.setViewTextSize(bccView, fontSizes.getMessageViewBCC());
        fontSizes.setViewTextSize(bccLabel, fontSizes.getMessageViewBCC());

        fromView.setOnClickListener(this);
        toView.setOnClickListener(this);
        ccView.setOnClickListener(this);
        bccView.setOnClickListener(this);

        fromView.setOnLongClickListener(this);
        toView.setOnLongClickListener(this);
        ccView.setOnLongClickListener(this);
        bccView.setOnLongClickListener(this);

        cryptoStatusIcon = (MessageCryptoStatusView) findViewById(R.id.crypto_status_icon);
        cryptoStatusIcon.setOnClickListener(this);

        messageHelper = MessageHelper.getInstance(context);

        hideAdditionalHeaders();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.from: {
                onAddSenderToContacts();
                break;
            }
            case R.id.to:
            case R.id.cc:
            case R.id.bcc: {
                expand((TextView)view, ((TextView)view).getEllipsize() != null);
                layoutChanged();
                break;
            }
            case R.id.crypto_status_icon: {
                onCryptoClickListener.onCryptoClick();
                break;
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.from:
                onAddAddressesToClipboard(message.getFrom());
                break;
            case R.id.to:
                onAddRecipientsToClipboard(Message.RecipientType.TO);
                break;
            case R.id.cc:
                onAddRecipientsToClipboard(Message.RecipientType.CC);
                break;
        }

        return true;
    }

    private void onAddSenderToContacts() {
        if (message != null) {
            try {
                final Address senderEmail = message.getFrom()[0];
                contacts.createContact(senderEmail);
            } catch (Exception e) {
                Timber.e(e, "Couldn't create contact");
            }
        }
    }

    public String createMessage(int addressesCount) {
        return context.getResources().getQuantityString(R.plurals.copy_address_to_clipboard, addressesCount);
    }

    private void onAddAddressesToClipboard(Address[] addresses) {
        String addressList = Address.toString(addresses);

        ClipboardManager clipboardManager = ClipboardManager.getInstance(context);
        clipboardManager.setText("addresses", addressList);

        Toast.makeText(context, createMessage(addresses.length), Toast.LENGTH_LONG).show();
    }

    private void onAddRecipientsToClipboard(Message.RecipientType recipientType) {
        onAddAddressesToClipboard(message.getRecipients(recipientType));
    }

    public void setOnFlagListener(OnClickListener listener) {
        flagged.setOnClickListener(listener);
    }

    public boolean additionalHeadersVisible() {
        return (additionalHeadersView != null &&
                additionalHeadersView.getVisibility() == View.VISIBLE);
    }

    /**
     * Clear the text field for the additional headers display if they are
     * not shown, to save UI resources.
     */
    private void hideAdditionalHeaders() {
        additionalHeadersView.setVisibility(View.GONE);
        additionalHeadersView.setText("");
    }


    /**
     * Set up and then show the additional headers view. Called by
     * {@link #onShowAdditionalHeaders()}
     * (when switching between messages).
     */
    private void showAdditionalHeaders() {
        Integer messageToShow = null;
        try {
            // Retrieve additional headers
            List<HeaderEntry> additionalHeaders = getAdditionalHeaders(message);
            if (!additionalHeaders.isEmpty()) {
                // Show the additional headers that we have got.
                populateAdditionalHeadersView(additionalHeaders);
                additionalHeadersView.setVisibility(View.VISIBLE);
            } else {
                // All headers have been downloaded, but there are no additional headers.
                messageToShow = R.string.message_no_additional_headers_available;
            }
        } catch (Exception e) {
            messageToShow = R.string.message_additional_headers_retrieval_failed;
        }
        // Show a message to the user, if any
        if (messageToShow != null) {
            Toast toast = Toast.makeText(context, messageToShow, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
        }

    }

    public void populate(final Message message, final Account account) {
        final Contacts contacts = K9.showContactName() ? this.contacts : null;
        final CharSequence from = MessageHelper.toFriendly(message.getFrom(), contacts);
        final CharSequence to = MessageHelper.toFriendly(message.getRecipients(Message.RecipientType.TO), contacts);
        final CharSequence cc = MessageHelper.toFriendly(message.getRecipients(Message.RecipientType.CC), contacts);
        final CharSequence bcc = MessageHelper.toFriendly(message.getRecipients(Message.RecipientType.BCC), contacts);

        Address[] fromAddrs = message.getFrom();
        Address[] toAddrs = message.getRecipients(Message.RecipientType.TO);
        Address[] ccAddrs = message.getRecipients(Message.RecipientType.CC);
        boolean fromMe = messageHelper.toMe(account, fromAddrs);

        Address counterpartyAddress = null;
        if (fromMe) {
            if (toAddrs.length > 0) {
                counterpartyAddress = toAddrs[0];
            } else if (ccAddrs.length > 0) {
                counterpartyAddress = ccAddrs[0];
            }
        } else if (fromAddrs.length > 0) {
            counterpartyAddress = fromAddrs[0];
        }

        /* We hide the subject by default for each new message, and MessageTitleView might show
         * it later by calling showSubjectLine(). */
        boolean newMessageShown = this.message == null || !this.message.getUid().equals(message.getUid());
        if (newMessageShown) {
            subjectView.setVisibility(GONE);
        }

        this.message = message;
        this.account = account;

        if (K9.showContactPicture()) {
            contactBadge.setVisibility(View.VISIBLE);
            contactPictureLoader = ContactPicture.getContactPictureLoader(context);
        }  else {
            contactBadge.setVisibility(View.GONE);
        }

        if (shouldShowSender(message)) {
            senderView.setVisibility(VISIBLE);
            String sender = getResources().getString(R.string.message_view_sender_label,
                    MessageHelper.toFriendly(message.getSender(), contacts));
            senderView.setText(sender);
        } else {
            senderView.setVisibility(View.GONE);
        }

        final String subject = message.getSubject();
        if (TextUtils.isEmpty(subject)) {
            subjectView.setText(context.getText(R.string.general_no_subject));
        } else {
            subjectView.setText(subject);
        }
        subjectView.setTextColor(0xff000000 | defaultSubjectColor);

        String dateTime = DateUtils.formatDateTime(context,
                message.getSentDate().getTime(),
                DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_ABBREV_ALL
                | DateUtils.FORMAT_SHOW_TIME
                | DateUtils.FORMAT_SHOW_YEAR);
        dateView.setText(dateTime);

        if (K9.showContactPicture()) {
            if (counterpartyAddress != null) {
                Utility.setContactForBadge(contactBadge, counterpartyAddress);
                contactPictureLoader.loadContactPicture(counterpartyAddress, contactBadge);
            } else {
                contactBadge.setImageResource(R.drawable.ic_contact_picture);
            }
        }

        fromView.setText(from);

        updateAddressField(toView, to, toLabel);
        updateAddressField(ccView, cc, ccLabel);
        updateAddressField(bccView, bcc, bccLabel);
        answeredIcon.setVisibility(message.isSet(Flag.ANSWERED) ? View.VISIBLE : View.GONE);
        forwardedIcon.setVisibility(message.isSet(Flag.FORWARDED) ? View.VISIBLE : View.GONE);
        flagged.setChecked(message.isSet(Flag.FLAGGED));

        chip.setBackgroundColor(this.account.getChipColor());

        setVisibility(View.VISIBLE);

        if (savedState != null) {
            if (savedState.additionalHeadersVisible) {
                showAdditionalHeaders();
            }
            savedState = null;
        } else {
            hideAdditionalHeaders();
        }
    }

    public static boolean shouldShowSender(Message message) {
        Address[] from = message.getFrom();
        Address[] sender = message.getSender();

        if (sender == null || sender.length == 0) {
            return false;
        }
        return !Arrays.equals(from, sender);
    }

    public void hideCryptoStatus() {
        cryptoStatusIcon.setVisibility(View.GONE);
    }

    public void setCryptoStatusLoading() {
        cryptoStatusIcon.setVisibility(View.VISIBLE);
        cryptoStatusIcon.setEnabled(false);
        cryptoStatusIcon.setCryptoDisplayStatus(MessageCryptoDisplayStatus.LOADING);
    }

    public void setCryptoStatusDisabled() {
        cryptoStatusIcon.setVisibility(View.VISIBLE);
        cryptoStatusIcon.setEnabled(false);
        cryptoStatusIcon.setCryptoDisplayStatus(MessageCryptoDisplayStatus.DISABLED);
    }

    public void setCryptoStatus(MessageCryptoDisplayStatus displayStatus) {
        cryptoStatusIcon.setVisibility(View.VISIBLE);
        cryptoStatusIcon.setEnabled(true);
        cryptoStatusIcon.setCryptoDisplayStatus(displayStatus);
    }

    public void onShowAdditionalHeaders() {
        int currentVisibility = additionalHeadersView.getVisibility();
        if (currentVisibility == View.VISIBLE) {
            hideAdditionalHeaders();
            expand(toView, false);
            expand(ccView, false);
        } else {
            showAdditionalHeaders();
            expand(toView, true);
            expand(ccView, true);
        }
        layoutChanged();
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

    private List<HeaderEntry> getAdditionalHeaders(final Message message)
    throws MessagingException {
        List<HeaderEntry> additionalHeaders = new LinkedList<HeaderEntry>();

        Set<String> headerNames = new LinkedHashSet<String>(message.getHeaderNames());
        for (String headerName : headerNames) {
            String[] headerValues = message.getHeader(headerName);
            for (String headerValue : headerValues) {
                additionalHeaders.add(new HeaderEntry(headerName, headerValue));
            }
        }
        return additionalHeaders;
    }

    /**
     * Set up the additional headers text view with the supplied header data.
     *
     * @param additionalHeaders List of header entries. Each entry consists of a header
     *                          name and a header value. Header names may appear multiple
     *                          times.
     *                          <p/>
     *                          This method is always called from within the UI thread by
     *                          {@link #showAdditionalHeaders()}.
     */
    private void populateAdditionalHeadersView(final List<HeaderEntry> additionalHeaders) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        boolean first = true;
        for (HeaderEntry additionalHeader : additionalHeaders) {
            if (!first) {
                sb.append("\n");
            } else {
                first = false;
            }
            StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
            SpannableString label = new SpannableString(additionalHeader.label + ": ");
            label.setSpan(boldSpan, 0, label.length(), 0);
            sb.append(label);
            sb.append(MimeUtility.unfoldAndDecode(additionalHeader.value));
        }
        additionalHeadersView.setText(sb);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState savedState = new SavedState(superState);

        savedState.additionalHeadersVisible = additionalHeadersVisible();

        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState)state;
        super.onRestoreInstanceState(savedState.getSuperState());

        this.savedState = savedState;
    }

    static class SavedState extends BaseSavedState {
        boolean additionalHeadersVisible;

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };


        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.additionalHeadersVisible = (in.readInt() != 0);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt((this.additionalHeadersVisible) ? 1 : 0);
        }
    }

    public interface OnLayoutChangedListener {
        void onLayoutChanged();
    }

    public void setOnLayoutChangedListener(OnLayoutChangedListener listener) {
        onLayoutChangedListener = listener;
    }

    private void layoutChanged() {
        if (onLayoutChangedListener != null) {
            onLayoutChangedListener.onLayoutChanged();
        }
    }

    public void showSubjectLine() {
        subjectView.setVisibility(VISIBLE);
    }

    public void setOnCryptoClickListener(OnCryptoClickListener onCryptoClickListener) {
        this.onCryptoClickListener = onCryptoClickListener;
    }
}
