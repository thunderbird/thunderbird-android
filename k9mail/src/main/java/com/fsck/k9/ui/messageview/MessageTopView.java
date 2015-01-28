package com.fsck.k9.ui.messageview;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.mailstore.MessageViewInfo.MessageViewContainer;
import com.fsck.k9.view.MessageHeader;


public class MessageTopView extends LinearLayout {

    private MessageHeader mHeaderContainer;
    private LayoutInflater mInflater;
    private LinearLayout containerViews;
    private Fragment fragment;
    private Button mDownloadRemainder;
    private AttachmentViewCallback attachmentCallback;
    private OpenPgpHeaderViewCallback openPgpHeaderViewCallback;

    public MessageTopView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize (Fragment fragment, AttachmentViewCallback attachmentCallback,
                            OpenPgpHeaderViewCallback openPgpHeaderViewCallback) {
        this.fragment = fragment;
        this.attachmentCallback = attachmentCallback;
        this.openPgpHeaderViewCallback = openPgpHeaderViewCallback;

        mHeaderContainer = (MessageHeader) findViewById(R.id.header_container);
        // mHeaderContainer.setOnLayoutChangedListener(this);
        mInflater = ((MessageViewFragment) fragment).getFragmentLayoutInflater();

        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.messageViewHeaderBackgroundColor, outValue, true);
        mHeaderContainer.setBackgroundColor(outValue.data);

        mDownloadRemainder = (Button) findViewById(R.id.download_remainder);
        mDownloadRemainder.setVisibility(View.GONE);

        containerViews = (LinearLayout) findViewById(R.id.message_containers);

    }

    public void resetView() {
        mDownloadRemainder.setVisibility(View.GONE);
        containerViews.removeAllViews();
    }

    public void setMessage(Account account, MessageViewInfo messageViewInfo)
            throws MessagingException {
        resetView();

        for (MessageViewContainer container : messageViewInfo.containers) {
            MessageContainerView view = (MessageContainerView) mInflater.inflate(R.layout.message_container, null);
            view.initialize(fragment, attachmentCallback, openPgpHeaderViewCallback);
            view.setMessage(container);
            containerViews.addView(view);
        }

    }

    /**
     * Fetch the message header view.  This is not the same as the message headers; this is the View shown at the top
     * of messages.
     * @return MessageHeader View.
     */
    public MessageHeader getMessageHeaderView() {
        return mHeaderContainer;
    }

    public void setHeaders(final Message message, Account account) {
        try {
            mHeaderContainer.populate(message, account);
            mHeaderContainer.setVisibility(View.VISIBLE);


        } catch (Exception me) {
            Log.e(K9.LOG_TAG, "setHeaders - error", me);
        }
    }

    public void setOnToggleFlagClickListener(OnClickListener listener) {
        mHeaderContainer.setOnFlagListener(listener);
    }

    public void showAllHeaders() {
        mHeaderContainer.onShowAdditionalHeaders();
    }

    public boolean additionalHeadersVisible() {
        return mHeaderContainer.additionalHeadersVisible();
    }

    public void resetHeaderView() {
        mHeaderContainer.setVisibility(View.GONE);
    }

    public void setOnDownloadButtonClickListener(OnClickListener listener) {
        mDownloadRemainder.setOnClickListener(listener);
    }

    public void enableDownloadButton() {
        mDownloadRemainder.setEnabled(true);
    }

    public void disableDownloadButton() {
        mDownloadRemainder.setEnabled(false);
    }

    public void setShowDownloadButton(Message message) {
        if (message.isSet(Flag.X_DOWNLOADED_FULL)) {
            mDownloadRemainder.setVisibility(View.GONE);
        } else {
            mDownloadRemainder.setEnabled(true);
            mDownloadRemainder.setVisibility(View.VISIBLE);
        }
    }

}
