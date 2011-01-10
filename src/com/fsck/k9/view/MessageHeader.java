package com.fsck.k9.view;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
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
import com.fsck.k9.mail.store.LocalStore;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.text.DateFormat;
public class MessageHeader extends LinearLayout
{
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
    private CheckBox mFlagged;
    private int defaultSubjectColor;
    private LinearLayout mToContainerView;
    private LinearLayout mCcContainerView;
    private TextView mAdditionalHeadersView;
    private View mAttachmentIcon;
    private static Drawable answeredIcon;
    private Message mMessage;
    private Account mAccount;
    private FontSizes mFontSizes = K9.getFontSizes();
    private Contacts mContacts;

    /**
     * Pair class is only available since API Level 5, so we need
     * this helper class unfortunately
     */
    private static class HeaderEntry
    {
        public String label;
        public String value;

        public HeaderEntry(String label, String value)
        {
            this.label = label;
            this.value = value;
        }
    }

    public MessageHeader(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        mContext = context;
        mDateFormat = DateFormatter.getDateFormat(mContext);
        mTimeFormat = android.text.format.DateFormat.getTimeFormat(mContext);   // 12/24 date format
        mContacts = Contacts.getInstance(mContext);
    }

    private void initializeLayout()
    {
        mAttachmentIcon = findViewById(R.id.attachment);
        mFromView = (TextView) findViewById(R.id.from);
        mToView = (TextView) findViewById(R.id.to);
        mCcView = (TextView) findViewById(R.id.cc);
        mToContainerView = (LinearLayout) findViewById(R.id.to_container);
        mCcContainerView = (LinearLayout) findViewById(R.id.cc_container);
        mSubjectView = (TextView) findViewById(R.id.subject);
        mAdditionalHeadersView = (TextView) findViewById(R.id.additional_headers_view);
        mChip = findViewById(R.id.chip);
        mDateView = (TextView) findViewById(R.id.date);
        mTimeView = (TextView) findViewById(R.id.time);
        mFlagged = (CheckBox) findViewById(R.id.flagged);

        defaultSubjectColor = mSubjectView.getCurrentTextColor();
        answeredIcon = getResources().getDrawable(R.drawable.ic_mms_answered_small);
        mSubjectView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageViewSubject());
        mTimeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageViewTime());
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageViewDate());
        mAdditionalHeadersView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageViewAdditionalHeaders());
        mAdditionalHeadersView.setVisibility(View.GONE);
        mAttachmentIcon.setVisibility(View.GONE);
        mFromView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageViewSender());
        mToView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageViewTo());
        mCcView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageViewCC());
        ((TextView) findViewById(R.id.to_label)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageViewTo());
        ((TextView) findViewById(R.id.cc_label)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageViewCC());

        setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onShowAdditionalHeaders();
                return;
            }
        });

        mFromView.setOnClickListener( new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (mMessage != null)
                {
                    try
                    {
                        final Address senderEmail = mMessage.getFrom()[0];
                        mContacts.createContact(senderEmail);
                    }
                    catch (Exception e)
                    {
                        Log.e(K9.LOG_TAG, "Couldn't create contact", e);
                    }
                }
            }
        });
    }

    public void setOnFlagListener (OnClickListener listener)
    {
        mFlagged.setOnClickListener(listener);
    }


    public boolean additionalHeadersVisible()
    {
        if ( mAdditionalHeadersView.getVisibility() == View.VISIBLE )
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Clear the text field for the additional headers display if they are
     * not shown, to save UI resources.
     */
    private void hideAdditionalHeaders()
    {
        mAdditionalHeadersView.setVisibility(View.GONE);
        mAdditionalHeadersView.setText("");

    }


    /**
     * Set up and then show the additional headers view. Called by
     * {@link #onShowAdditionalHeaders()}
     * (when switching between messages).
     */
    private void showAdditionalHeaders()
    {
        Integer messageToShow = null;
        try
        {
            // Retrieve additional headers
            boolean allHeadersDownloaded = mMessage.isSet(Flag.X_GOT_ALL_HEADERS);
            List<HeaderEntry> additionalHeaders = getAdditionalHeaders(mMessage);
            if (!additionalHeaders.isEmpty())
            {
                // Show the additional headers that we have got.
                populateAdditionalHeadersView(additionalHeaders);
                mAdditionalHeadersView.setVisibility(View.VISIBLE);
            }
            if (!allHeadersDownloaded)
            {
                /*
                * Tell the user about the "save all headers" setting
                *
                * NOTE: This is only a temporary solution... in fact,
                * the system should download headers on-demand when they
                * have not been saved in their entirety initially.
                */
                messageToShow = R.string.message_additional_headers_not_downloaded;
            }
            else if (additionalHeaders.isEmpty())
            {
                // All headers have been downloaded, but there are no additional headers.
                messageToShow = R.string.message_no_additional_headers_available;
            }
        }
        catch (MessagingException e)
        {
            messageToShow = R.string.message_additional_headers_retrieval_failed;
        }
        // Show a message to the user, if any
        if (messageToShow != null)
        {
            Toast toast = Toast.makeText(mContext, messageToShow, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
        }

    }

    public void populate( final Message message, final Account account) throws MessagingException
    {
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
        if (subject == null || subject.equals(""))
        {
            mSubjectView.setText(mContext.getText(R.string.general_no_subject));
        }
        else
        {
            mSubjectView.setText(subject);
        }
        mSubjectView.setTextColor(0xff000000 | defaultSubjectColor);

        mFromView.setText(from);

        if (date != null)
        {
            mDateView.setText(date);
            mDateView.setVisibility(View.VISIBLE);
        }
        else
        {
            mDateView.setVisibility(View.GONE);
        }
        mTimeView.setText(time);
        mToContainerView.setVisibility((to != null && to.length() > 0) ? View.VISIBLE : View.GONE);
        mToView.setText(to);
        mCcContainerView.setVisibility((cc != null && cc.length() > 0) ? View.VISIBLE : View.GONE);
        mCcView.setText(cc);
        mAttachmentIcon.setVisibility(((LocalStore.LocalMessage) message).hasAttachments() ? View.VISIBLE : View.GONE);
        mFlagged.setChecked(message.isSet(Flag.FLAGGED));
        mChip.setBackgroundDrawable(mAccount.generateColorChip().drawable());
        mChip.getBackground().setAlpha(!message.isSet(Flag.SEEN) ? 255 : 127);

        if (message.isSet(Flag.ANSWERED))
        {
            mSubjectView.setCompoundDrawablesWithIntrinsicBounds(answeredIcon, null, null, null);
        }
        else
        {
            mSubjectView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        }
        setVisibility(View.VISIBLE);
        if (mAdditionalHeadersView.getVisibility() == View.VISIBLE)
        {
            showAdditionalHeaders();
        }
    }

    public void onShowAdditionalHeaders()
    {
        int currentVisibility = mAdditionalHeadersView.getVisibility();
        if (currentVisibility == View.VISIBLE)
        {
            hideAdditionalHeaders();
        }
        else
        {
            showAdditionalHeaders();
        }
    }

    private List<HeaderEntry> getAdditionalHeaders(final Message message)
    throws MessagingException
    {
        List<HeaderEntry> additionalHeaders = new LinkedList<HeaderEntry>();
        /*
        * Remove "Subject" header as it is already shown in the standard
        * message view header. But do show "From", "To", and "Cc" again.
        * This time including the email addresses. See issue 1805.
        */
        Set<String> headerNames = new HashSet<String>(message.getHeaderNames());
        headerNames.remove("Subject");
        for (String headerName : headerNames)
        {
            String[] headerValues = message.getHeader(headerName);
            for (String headerValue : headerValues)
            {
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
    private void populateAdditionalHeadersView(final List<HeaderEntry> additionalHeaders)
    {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        boolean first = true;
        for (HeaderEntry additionalHeader : additionalHeaders)
        {
            if (!first)
            {
                sb.append("\n");
            }
            else
            {
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

}
