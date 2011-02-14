package com.fsck.k9.view;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.crypto.CryptoProvider;
import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.mail.*;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.store.LocalStore;


/**
 */
public class SingleMessageView extends LinearLayout {
    private boolean mScreenReaderEnabled;
    private MessageCryptoView mCryptoView;
    private MessageWebView mMessageContentView;
    private AccessibleWebView mAccessibleMessageContentView;
    private MessageHeader mHeaderContainer;
    private LinearLayout        mAttachments;
    private View mShowPicturesSection;
    private boolean mShowPictures;
    private Button mDownloadRemainder;
    private LayoutInflater mInflater;


    public void initialize(Activity activity, Boolean isScreenReaderActive) {
        mMessageContentView = (MessageWebView) findViewById(R.id.message_content);
        mAccessibleMessageContentView = (AccessibleWebView) findViewById(R.id.accessible_message_content);
        mAttachments = (LinearLayout) findViewById(R.id.attachments);
        mHeaderContainer = (MessageHeader) findViewById(R.id.header_container);
        mCryptoView = (MessageCryptoView) findViewById(R.id.layout_decrypt);
        mCryptoView.setActivity(activity);
        mCryptoView.setupChildViews();
        mShowPicturesSection = findViewById(R.id.show_pictures_section);
        mShowPictures = false;

        mInflater = activity.getLayoutInflater();
        mDownloadRemainder = (Button) findViewById(R.id.download_remainder);
        mMessageContentView.configure();


        mAttachments.setVisibility(View.GONE);
        if (isScreenReaderActive) {
            mAccessibleMessageContentView.setVisibility(View.VISIBLE);
            mMessageContentView.setVisibility(View.GONE);
        } else {
            mAccessibleMessageContentView.setVisibility(View.GONE);
            mMessageContentView.setVisibility(View.VISIBLE);
        }
        mScreenReaderEnabled = isScreenReaderActive;

    }

    public SingleMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean showPictures() {
        return mShowPictures;
    }
    public void setShowPictures(Boolean show) {
        mShowPictures = show;
    }

    /**
     * Enable/disable image loading of the WebView. But always hide the
     * "Show pictures" button!
     *
     * @param enable true, if (network) images should be loaded.
     *               false, otherwise.
     */
     public void setLoadPictures(boolean enable) {
        mMessageContentView.blockNetworkData(!enable);
        setShowPictures(enable);
        showShowPicturesSection(false);
    }

    public Button downloadRemainderButton() {
        return  mDownloadRemainder;
    }

    public void showShowPicturesSection(boolean show) {
        mShowPicturesSection.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void setHeaders(final Message message, Account account) {
        try {
            mHeaderContainer.populate(message, account);


        } catch (Exception me) {
            Log.e(K9.LOG_TAG, "setHeaders - error", me);
        }
    }

    public void setShowDownloadButton(Message message) {
        if (message.isSet(Flag.X_DOWNLOADED_FULL)) {
            mDownloadRemainder.setVisibility(View.GONE);
        } else {
            mDownloadRemainder.setEnabled(true);
            mDownloadRemainder.setVisibility(View.VISIBLE);
        }
    }

    public void setOnFlagListener(OnClickListener listener) {
        mHeaderContainer.setOnFlagListener(listener);
    }

    public void showAllHeaders() {
        mHeaderContainer.onShowAdditionalHeaders();
    }

    public boolean additionalHeadersVisible() {
        return mHeaderContainer.additionalHeadersVisible();
    }


    public void loadBodyFromUrl(String url) {
        mMessageContentView.loadUrl(url);
        mCryptoView.hide();

    }

    public void loadBodyFromText(CryptoProvider cryptoProvider, PgpData pgpData, Message message, String emailText, String contentType) {
        if (mScreenReaderEnabled) {
            mAccessibleMessageContentView.loadDataWithBaseURL("http://", emailText, contentType, "utf-8", null);
        } else {
            mMessageContentView.loadDataWithBaseURL("http://", emailText, contentType, "utf-8", null);
            mMessageContentView.scrollTo(0, 0);
        }
        updateCryptoLayout(cryptoProvider, pgpData, message);

    }

    public void updateCryptoLayout(CryptoProvider cp, PgpData pgpData, Message message) {
        mCryptoView.updateLayout(cp, pgpData, message);
    }

    public void setAttachmentsEnabled(boolean enabled) {
        for (int i = 0, count = mAttachments.getChildCount(); i < count; i++) {
            AttachmentView attachment = (AttachmentView) mAttachments.getChildAt(i);
            attachment.viewButton.setEnabled(enabled);
            attachment.downloadButton.setEnabled(enabled);
        }
    }


    public void removeAllAttachments() {
        for (int i = 0, count = mAttachments.getChildCount(); i < count; i++) {
            mAttachments.removeView(mAttachments.getChildAt(i));
        }
    }

    public void renderAttachments(Part part, int depth,

                                  Message message, Account account, MessagingController controller, MessagingListener listener) throws MessagingException {

        if (part.getBody() instanceof Multipart) {
            Multipart mp = (Multipart) part.getBody();
            for (int i = 0; i < mp.getCount(); i++) {
                renderAttachments(mp.getBodyPart(i), depth + 1, message, account, controller, listener);
            }
        } else if (part instanceof LocalStore.LocalAttachmentBodyPart) {
            String contentDisposition = MimeUtility.unfoldAndDecode(part.getDisposition());
            // Inline parts with a content-id are almost certainly components of an HTML message
            // not attachments. Don't show attachment download buttons for them.
            if (contentDisposition != null &&
                    MimeUtility.getHeaderParameter(contentDisposition, null).matches("^(?i:inline)")
                    && part.getHeader("Content-ID") != null) {
                return;
            }
            AttachmentView view = (AttachmentView)mInflater.inflate(R.layout.message_view_attachment, null);
            if (view.populateFromPart(part, message, account, controller, listener)) {
                addAttachment(view);
            }
        }
    }


    public void addAttachment(View attachmentView) {
        mAttachments.addView(attachmentView);
        mAttachments.setVisibility(View.VISIBLE);
    }
    public void zoom(KeyEvent event) {
        if (mScreenReaderEnabled) {
            mAccessibleMessageContentView.zoomIn();
        } else {
            if (event.isShiftPressed()) {
                mMessageContentView.zoomIn();
            } else {
                mMessageContentView.zoomOut();
            }
        }
    }
    public void beginSelectingText() {
        mMessageContentView.emulateShiftHeld();
    }


    public void resetView() {
        setLoadPictures(false);
        mMessageContentView.scrollTo(0, 0);
        mHeaderContainer.setVisibility(View.GONE);
        mMessageContentView.clearView();
        mAttachments.removeAllViews();
    }
}
