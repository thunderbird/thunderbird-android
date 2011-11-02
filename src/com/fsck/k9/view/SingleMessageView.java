package com.fsck.k9.view;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
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
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.*;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.store.LocalStore;

import java.util.List;
import java.util.Set;


/**
 */
public class SingleMessageView extends LinearLayout {
    private boolean mScreenReaderEnabled;
    private MessageCryptoView mCryptoView;
    private MessageWebView mMessageContentView;
    private AccessibleWebView mAccessibleMessageContentView;
    private MessageHeader mHeaderContainer;
    private LinearLayout mAttachments;
    private View mShowPicturesSection;
    private boolean mShowPictures;
    private Button mDownloadRemainder;
    private LayoutInflater mInflater;
    private Contacts mContacts;
    private AttachmentView.AttachmentFileDownloadCallback attachmentCallback;

    public void initialize(Activity activity) {
        mMessageContentView = (MessageWebView) findViewById(R.id.message_content);
        mAccessibleMessageContentView = (AccessibleWebView) findViewById(R.id.accessible_message_content);
        mAttachments = (LinearLayout) findViewById(R.id.attachments);
        mHeaderContainer = (MessageHeader) findViewById(R.id.header_container);
        mCryptoView = (MessageCryptoView) findViewById(R.id.layout_decrypt);
        mCryptoView.setActivity(activity);
        mCryptoView.setupChildViews();
        mShowPicturesSection = findViewById(R.id.show_pictures_section);
        mShowPictures = false;

        mContacts = Contacts.getInstance(activity);

        mInflater = activity.getLayoutInflater();
        mDownloadRemainder = (Button) findViewById(R.id.download_remainder);
        mMessageContentView.configure();


        mAttachments.setVisibility(View.GONE);
        if (isScreenReaderActive(activity)) {
            mAccessibleMessageContentView.setVisibility(View.VISIBLE);
            mMessageContentView.setVisibility(View.GONE);
            mScreenReaderEnabled = true;
        } else {
            mAccessibleMessageContentView.setVisibility(View.GONE);
            mMessageContentView.setVisibility(View.VISIBLE);
            mScreenReaderEnabled = false;
        }

    }

    public SingleMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    private boolean isScreenReaderActive(Activity activity) {
        final String SCREENREADER_INTENT_ACTION = "android.accessibilityservice.AccessibilityService";
        final String SCREENREADER_INTENT_CATEGORY = "android.accessibilityservice.category.FEEDBACK_SPOKEN";
        // Restrict the set of intents to only accessibility services that have
        // the category FEEDBACK_SPOKEN (aka, screen readers).
        Intent screenReaderIntent = new Intent(SCREENREADER_INTENT_ACTION);
        screenReaderIntent.addCategory(SCREENREADER_INTENT_CATEGORY);
        List<ResolveInfo> screenReaders = activity.getPackageManager().queryIntentServices(
                                              screenReaderIntent, 0);
        ContentResolver cr = activity.getContentResolver();
        Cursor cursor = null;
        int status = 0;
        for (ResolveInfo screenReader : screenReaders) {
            // All screen readers are expected to implement a content provider
            // that responds to
            // content://<nameofpackage>.providers.StatusProvider
            cursor = cr.query(Uri.parse("content://" + screenReader.serviceInfo.packageName
                                        + ".providers.StatusProvider"), null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                // These content providers use a special cursor that only has
                // one element,
                // an integer that is 1 if the screen reader is running.
                status = cursor.getInt(0);
                cursor.close();
                if (status == 1) {
                    return true;
                }
            }
        }
        return false;
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

    public void displayMessageBody(Account account, String folder, String uid, Message message, PgpData pgpData) throws MessagingException {
        // TODO - really this code  path? this is an odd place to put it
        removeAllAttachments();

        String type;
        String text = pgpData.getDecryptedData();
        if (text != null) {
            type = "text/plain";
        } else {
            // getTextForDisplay() always returns HTML-ified content.
            text = ((LocalStore.LocalMessage) message).getTextForDisplay();
            type = "text/html";
        }
        if (text != null) {
            final String emailText = text;
            final String contentType = type;
            loadBodyFromText(account.getCryptoProvider(), pgpData, message, emailText, contentType);
            // If the message contains external pictures and the "Show pictures"
            // button wasn't already pressed, see if the user's preferences has us
            // showing them anyway.
            if (Utility.hasExternalImages(text) && !showPictures()) {
                Address[] from = message.getFrom();
                if ((account.getShowPictures() == Account.ShowPictures.ALWAYS) ||
                        ((account.getShowPictures() == Account.ShowPictures.ONLY_FROM_CONTACTS) &&
                         // Make sure we have at least one from address
                         (from != null && from.length > 0) &&
                         mContacts.isInContacts(from[0].getAddress()))) {
                    setLoadPictures(true);
                } else {
                    showShowPicturesSection(true);
                }
            }
        } else {
            loadBodyFromUrl("file:///android_asset/empty.html");
        }
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

    public void renderAttachments(Part part, int depth, Message message, Account account,
                                  MessagingController controller, MessagingListener listener) throws MessagingException {

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
                    && part.getHeader(MimeHeader.HEADER_CONTENT_ID) != null) {
                return;
            }
            AttachmentView view = (AttachmentView)mInflater.inflate(R.layout.message_view_attachment, null);
            view.setCallback(attachmentCallback);
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

    public AttachmentView.AttachmentFileDownloadCallback getAttachmentCallback() {
        return attachmentCallback;
    }

    public void setAttachmentCallback(
        AttachmentView.AttachmentFileDownloadCallback attachmentCallback) {
        this.attachmentCallback = attachmentCallback;
    }

    /**
     * Save a copy of the {@link com.fsck.k9.controller.MessagingController#getListeners()}.  This method will also
     * pass along these listeners to the underlying views.
     * @param listeners Set of listeners.
     */
    public void setListeners(final Set<MessagingListener> listeners) {
        if(!mScreenReaderEnabled) {
            if(mMessageContentView != null) {
                mMessageContentView.setListeners(listeners);
            }
        } else {
            if(mAccessibleMessageContentView != null) {
                mAccessibleMessageContentView.setListeners(listeners);
            }
        }
    }
}
