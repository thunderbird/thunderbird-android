package com.fsck.k9.ui.messageview;


import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.fsck.k9.Account;
import com.fsck.k9.Account.ShowPictures;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.view.MessageHeader;


public class MessageTopView extends LinearLayout implements ShowPicturesController {

    private MessageHeader mHeaderContainer;
    private LayoutInflater mInflater;
    private FrameLayout containerView;
    private Button mDownloadRemainder;
    private AttachmentViewCallback attachmentCallback;
    private Button showPicturesButton;
    private List<MessageContainerView> messageContainerViewsWithPictures = new ArrayList<MessageContainerView>();


    public MessageTopView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        mHeaderContainer = (MessageHeader) findViewById(R.id.header_container);
        // mHeaderContainer.setOnLayoutChangedListener(this);
        mInflater = LayoutInflater.from(getContext());

        mDownloadRemainder = (Button) findViewById(R.id.download_remainder);
        mDownloadRemainder.setVisibility(View.GONE);

        showPicturesButton = (Button) findViewById(R.id.show_pictures);
        setShowPicturesButtonListener();

        containerView = (FrameLayout) findViewById(R.id.message_container);

        hideHeaderView();
    }

    private void setShowPicturesButtonListener() {
        showPicturesButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showPicturesInAllContainerViews();
            }
        });
    }

    private void showPicturesInAllContainerViews() {
        for (MessageContainerView containerView : messageContainerViewsWithPictures) {
            containerView.showPictures();
        }

        hideShowPicturesButton();
    }

    public void resetView() {
        mDownloadRemainder.setVisibility(View.GONE);
        containerView.removeAllViews();
    }

    public void setMessage(Account account, MessageViewInfo messageViewInfo)
            throws MessagingException {
        resetView();

        ShowPictures showPicturesSetting = account.getShowPictures();
        boolean automaticallyLoadPictures =
                shouldAutomaticallyLoadPictures(showPicturesSetting, messageViewInfo.message);

        MessageContainerView view = (MessageContainerView) mInflater.inflate(R.layout.message_container,
                containerView, false);
        view.displayMessageViewContainer(messageViewInfo, automaticallyLoadPictures, this, attachmentCallback);

        boolean displayPgpHeader = account.isOpenPgpProviderConfigured();
        if (displayPgpHeader) {
            mHeaderContainer.setCryptoStatus(messageViewInfo.cryptoResultAnnotation);
        }

        containerView.addView(view);

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

    private void hideHeaderView() {
        mHeaderContainer.setVisibility(View.GONE);
    }

    public void setOnDownloadButtonClickListener(OnClickListener listener) {
        mDownloadRemainder.setOnClickListener(listener);
    }

    public void setAttachmentCallback(AttachmentViewCallback callback) {
        attachmentCallback = callback;
    }

    public void setOnCryptoClickListener(OnCryptoClickListener listener) {
        mHeaderContainer.setOnCryptoClickListener(listener);
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

    private void showShowPicturesButton() {
        showPicturesButton.setVisibility(View.VISIBLE);
    }

    private void hideShowPicturesButton() {
        showPicturesButton.setVisibility(View.GONE);
    }

    @Override
    public void notifyMessageContainerContainsPictures(MessageContainerView messageContainerView) {
        messageContainerViewsWithPictures.add(messageContainerView);

        showShowPicturesButton();
    }

    private boolean shouldAutomaticallyLoadPictures(ShowPictures showPicturesSetting, Message message) {
        return showPicturesSetting == ShowPictures.ALWAYS || shouldShowPicturesFromSender(showPicturesSetting, message);
    }

    private boolean shouldShowPicturesFromSender(ShowPictures showPicturesSetting, Message message) {
        if (showPicturesSetting != ShowPictures.ONLY_FROM_CONTACTS) {
            return false;
        }

        String senderEmailAddress = getSenderEmailAddress(message);
        if (senderEmailAddress == null) {
            return false;
        }

        Contacts contacts = Contacts.getInstance(getContext());
        return contacts.isInContacts(senderEmailAddress);
    }

    private String getSenderEmailAddress(Message message) {
        Address[] from = message.getFrom();
        if (from == null || from.length == 0) {
            return null;
        }

        return from[0].getAddress();
    }
}
