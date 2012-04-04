package com.fsck.k9.view;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.Account;
import com.fsck.k9.helper.DateFormatter;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeUtility;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.text.DateFormat;

public class MessageHeader extends ScrollView implements OnClickListener {
    private Context mContext;
    private TextView mFromView;
    private TextView mDateView;
    private TextView mTimeView;
    private TextView mToView;
    private TextView mCcView;
    private TextView mSubjectView;
    private DateFormat mDateFormat;
    private DateFormat mTimeFormat;

    private View mChip;
    private View mChip2;
    private View mChip3;
    private CheckBox mFlagged;
    private int defaultSubjectColor;
    private LinearLayout mToContainerView;
    private LinearLayout mCcContainerView;
    private TextView mAdditionalHeadersView;
    private View mAnsweredIcon;
    private Message mMessage;
    private Account mAccount;
    private FontSizes mFontSizes = K9.getFontSizes();
    private Contacts mContacts;
    private ImageView mShowAdditionalHeadersIcon;
    private SavedState mSavedState;

    private OnLayoutChangedListener mOnLayoutChangedListener;

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
        mContext = context;
        mDateFormat = DateFormatter.getDateFormat(mContext);
        mTimeFormat = android.text.format.DateFormat.getTimeFormat(mContext);   // 12/24 date format
        mContacts = Contacts.getInstance(mContext);
    }

    private void initializeLayout() {
        mAnsweredIcon = findViewById(R.id.answered);
        mFromView = (TextView) findViewById(R.id.from);
        mToView = (TextView) findViewById(R.id.to);
        mCcView = (TextView) findViewById(R.id.cc);
        mToContainerView = (LinearLayout) findViewById(R.id.to_container);
        mCcContainerView = (LinearLayout) findViewById(R.id.cc_container);
        mSubjectView = (TextView) findViewById(R.id.subject);
        mAdditionalHeadersView = (TextView) findViewById(R.id.additional_headers_view);
        mChip = findViewById(R.id.chip);
        mChip2 = findViewById(R.id.chip2);
        mChip3 = findViewById(R.id.chip3);
        mDateView = (TextView) findViewById(R.id.date);
        mTimeView = (TextView) findViewById(R.id.time);
        mFlagged = (CheckBox) findViewById(R.id.flagged);
        mShowAdditionalHeadersIcon = (ImageView) findViewById(R.id.show_additional_headers_icon);

        defaultSubjectColor = mSubjectView.getCurrentTextColor();
        mSubjectView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getMessageViewSubject());
        mTimeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getMessageViewTime());
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getMessageViewDate());
        mAdditionalHeadersView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getMessageViewAdditionalHeaders());
        hideAdditionalHeaders();

        mAnsweredIcon.setVisibility(View.GONE);
        mFromView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getMessageViewSender());
        mToView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getMessageViewTo());
        mCcView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getMessageViewCC());
        ((TextView) findViewById(R.id.to_label)).setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getMessageViewTo());
        ((TextView) findViewById(R.id.cc_label)).setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getMessageViewCC());

        findViewById(R.id.show_additional_headers_area).setOnClickListener(this);
        findViewById(R.id.additional_headers_row).setOnClickListener(this);
        mFromView.setOnClickListener(this);
        mToView.setOnClickListener(this);
        mCcView.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.additional_headers_row:
            case R.id.show_additional_headers_area: {
                onShowAdditionalHeaders();
                break;
            }
            case R.id.from: {
                onAddSenderToContacts();
                break;
            }
            case R.id.to:
            case R.id.cc: {
                expand((TextView)view, ((TextView)view).getEllipsize() != null);
                layoutChanged();
            }
        }
    }

    private void onAddSenderToContacts() {
        if (mMessage != null) {
            try {
                final Address senderEmail = mMessage.getFrom()[0];
                mContacts.createContact(senderEmail);
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Couldn't create contact", e);
            }
        }
    }

    public void setOnFlagListener(OnClickListener listener) {
        if (mFlagged == null)
            return;
        mFlagged.setOnClickListener(listener);
    }


    public boolean additionalHeadersVisible() {
        return (mAdditionalHeadersView != null &&
                mAdditionalHeadersView.getVisibility() == View.VISIBLE);
    }

    /**
     * Clear the text field for the additional headers display if they are
     * not shown, to save UI resources.
     */
    private void hideAdditionalHeaders() {
        mAdditionalHeadersView.setVisibility(View.GONE);
        mAdditionalHeadersView.setText("");
        mShowAdditionalHeadersIcon.setImageResource(R.drawable.show_more);
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
            boolean allHeadersDownloaded = mMessage.isSet(Flag.X_GOT_ALL_HEADERS);
            List<HeaderEntry> additionalHeaders = getAdditionalHeaders(mMessage);
            if (!additionalHeaders.isEmpty()) {
                // Show the additional headers that we have got.
                populateAdditionalHeadersView(additionalHeaders);
                mAdditionalHeadersView.setVisibility(View.VISIBLE);
                mShowAdditionalHeadersIcon.setImageResource(R.drawable.show_less);
            }
            if (!allHeadersDownloaded) {
                /*
                * Tell the user about the "save all headers" setting
                *
                * NOTE: This is only a temporary solution... in fact,
                * the system should download headers on-demand when they
                * have not been saved in their entirety initially.
                */
                messageToShow = R.string.message_additional_headers_not_downloaded;
            } else if (additionalHeaders.isEmpty()) {
                // All headers have been downloaded, but there are no additional headers.
                messageToShow = R.string.message_no_additional_headers_available;
            }
        } catch (MessagingException e) {
            messageToShow = R.string.message_additional_headers_retrieval_failed;
        }
        // Show a message to the user, if any
        if (messageToShow != null) {
            Toast toast = Toast.makeText(mContext, messageToShow, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
        }

    }

    public void populate(final Message message, final Account account) throws MessagingException {
        final Contacts contacts = K9.showContactName() ? mContacts : null;
        final CharSequence from = Address.toFriendly(message.getFrom(), contacts);
        final String date = mDateFormat.format(message.getSentDate());
        final String time = mTimeFormat.format(message.getSentDate());
        final CharSequence to = Address.toFriendly(message.getRecipients(Message.RecipientType.TO), contacts);
        final CharSequence cc = Address.toFriendly(message.getRecipients(Message.RecipientType.CC), contacts);

        mMessage = message;
        mAccount = account;

        initializeLayout();
        String subject = message.getSubject();
        if (subject == null || subject.equals("")) {
            mSubjectView.setText(mContext.getText(R.string.general_no_subject));
        } else {
            mSubjectView.setText(subject);
        }
        mSubjectView.setTextColor(0xff000000 | defaultSubjectColor);

        mFromView.setText(from);

        if (date != null) {
            mDateView.setText(date);
            mDateView.setVisibility(View.VISIBLE);
        } else {
            mDateView.setVisibility(View.GONE);
        }
        mTimeView.setText(time);
        mToContainerView.setVisibility((to != null && to.length() > 0) ? View.VISIBLE : View.GONE);
        mToView.setText(to);
        mCcContainerView.setVisibility((cc != null && cc.length() > 0) ? View.VISIBLE : View.GONE);
        mCcView.setText(cc);
        mAnsweredIcon.setVisibility(message.isSet(Flag.ANSWERED) ? View.VISIBLE : View.GONE);
        mFlagged.setChecked(message.isSet(Flag.FLAGGED));

        int chipColor = mAccount.getChipColor();
        int chipColorAlpha = (!message.isSet(Flag.SEEN)) ? 255 : 127;
        mChip.setBackgroundColor(chipColor);
        mChip.getBackground().setAlpha(chipColorAlpha);
        mChip2.setBackgroundColor(chipColor);
        mChip2.getBackground().setAlpha(chipColorAlpha);
        mChip3.setBackgroundColor(chipColor);
        mChip3.getBackground().setAlpha(chipColorAlpha);

        setVisibility(View.VISIBLE);

        if (mSavedState != null) {
            if (mSavedState.additionalHeadersVisible) {
                showAdditionalHeaders();
            }
            mSavedState = null;
        } else {
            hideAdditionalHeaders();
        }
    }

    public void onShowAdditionalHeaders() {
        int currentVisibility = mAdditionalHeadersView.getVisibility();
        if (currentVisibility == View.VISIBLE) {
            hideAdditionalHeaders();
            expand(mToView, false);
            expand(mCcView, false);
        } else {
            showAdditionalHeaders();
            expand(mToView, true);
            expand(mCcView, true);
        }
        layoutChanged();
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
        /*
        * Remove "Subject" header as it is already shown in the standard
        * message view header. But do show "From", "To", and "Cc" again.
        * This time including the email addresses. See issue 1805.
        */
        Set<String> headerNames = new LinkedHashSet<String>(message.getHeaderNames());
        headerNames.remove("Subject");
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
        mAdditionalHeadersView.setText(sb);
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

        mSavedState = savedState;
    }

    static class SavedState extends BaseSavedState {
        boolean additionalHeadersVisible;

        @SuppressWarnings("hiding")
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
        mOnLayoutChangedListener = listener;
    }

    private void layoutChanged() {
        if (mOnLayoutChangedListener != null) {
            mOnLayoutChangedListener.onLayoutChanged();
        }
    }
}
