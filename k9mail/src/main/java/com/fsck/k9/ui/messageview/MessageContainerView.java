package com.fsck.k9.ui.messageview;


import java.util.HashMap;
import java.util.Map;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.R;
import com.fsck.k9.helper.ClipboardManager;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.HtmlConverter;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mailstore.AttachmentResolver;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.view.MessageHeader.OnLayoutChangedListener;
import com.fsck.k9.view.MessageWebView;
import com.fsck.k9.view.MessageWebView.OnPageFinishedListener;


public class MessageContainerView extends LinearLayout implements OnLayoutChangedListener, OnCreateContextMenuListener {
    private static final int MENU_ITEM_LINK_VIEW = Menu.FIRST;
    private static final int MENU_ITEM_LINK_SHARE = Menu.FIRST + 1;
    private static final int MENU_ITEM_LINK_COPY = Menu.FIRST + 2;

    private static final int MENU_ITEM_IMAGE_VIEW = Menu.FIRST;
    private static final int MENU_ITEM_IMAGE_SAVE = Menu.FIRST + 1;
    private static final int MENU_ITEM_IMAGE_COPY = Menu.FIRST + 2;

    private static final int MENU_ITEM_PHONE_CALL = Menu.FIRST;
    private static final int MENU_ITEM_PHONE_SAVE = Menu.FIRST + 1;
    private static final int MENU_ITEM_PHONE_COPY = Menu.FIRST + 2;

    private static final int MENU_ITEM_EMAIL_SEND = Menu.FIRST;
    private static final int MENU_ITEM_EMAIL_SAVE = Menu.FIRST + 1;
    private static final int MENU_ITEM_EMAIL_COPY = Menu.FIRST + 2;

    private MessageWebView mMessageContentView;
    private LinearLayout mAttachments;
    private View unsignedTextContainer;
    private View unsignedTextDivider;
    private TextView unsignedText;
    private View mAttachmentsContainer;

    private boolean showingPictures;
    private LayoutInflater mInflater;
    private AttachmentViewCallback attachmentCallback;
    private SavedState mSavedState;
    private ClipboardManager mClipboardManager;
    private Map<AttachmentViewInfo, AttachmentView> attachmentViewMap = new HashMap<>();
    private Map<Uri, AttachmentViewInfo> attachments = new HashMap<>();
    private boolean hasHiddenExternalImages;

    private String currentHtmlText;
    private AttachmentResolver currentAttachmentResolver;


    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        mMessageContentView = (MessageWebView) findViewById(R.id.message_content);
        if (!isInEditMode()) {
            mMessageContentView.configure();
        }
        mMessageContentView.setOnCreateContextMenuListener(this);
        mMessageContentView.setVisibility(View.VISIBLE);

        mAttachmentsContainer = findViewById(R.id.attachments_container);
        mAttachments = (LinearLayout) findViewById(R.id.attachments);

        unsignedTextContainer = findViewById(R.id.message_unsigned_container);
        unsignedTextDivider = findViewById(R.id.message_unsigned_divider);
        unsignedText = (TextView) findViewById(R.id.message_unsigned_text);

        showingPictures = false;

        Context context = getContext();
        mInflater = LayoutInflater.from(context);
        mClipboardManager = ClipboardManager.getInstance(context);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu);

        WebView webview = (WebView) v;
        WebView.HitTestResult result = webview.getHitTestResult();

        if (result == null) {
            return;
        }

        int type = result.getType();
        Context context = getContext();

        switch (type) {
            case HitTestResult.SRC_ANCHOR_TYPE: {
                final String url = result.getExtra();
                OnMenuItemClickListener listener = new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case MENU_ITEM_LINK_VIEW: {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                startActivityIfAvailable(getContext(), intent);
                                break;
                            }
                            case MENU_ITEM_LINK_SHARE: {
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("text/plain");
                                intent.putExtra(Intent.EXTRA_TEXT, url);
                                startActivityIfAvailable(getContext(), intent);
                                break;
                            }
                            case MENU_ITEM_LINK_COPY: {
                                String label = getContext().getString(
                                        R.string.webview_contextmenu_link_clipboard_label);
                                mClipboardManager.setText(label, url);
                                break;
                            }
                        }
                        return true;
                    }
                };

                menu.setHeaderTitle(url);

                menu.add(Menu.NONE, MENU_ITEM_LINK_VIEW, 0,
                        context.getString(R.string.webview_contextmenu_link_view_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_LINK_SHARE, 1,
                        context.getString(R.string.webview_contextmenu_link_share_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_LINK_COPY, 2,
                        context.getString(R.string.webview_contextmenu_link_copy_action))
                        .setOnMenuItemClickListener(listener);

                break;
            }
            case HitTestResult.IMAGE_TYPE:
            case HitTestResult.SRC_IMAGE_ANCHOR_TYPE: {
                final Uri uri = Uri.parse(result.getExtra());
                if (uri == null) {
                    return;
                }
                
                final AttachmentViewInfo attachmentViewInfo = getAttachmentViewInfoIfCidUri(uri);
                final boolean inlineImage = attachmentViewInfo != null;

                OnMenuItemClickListener listener = new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case MENU_ITEM_IMAGE_VIEW: {
                                if (inlineImage) {
                                    attachmentCallback.onViewAttachment(attachmentViewInfo);
                                } else {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                    startActivityIfAvailable(getContext(), intent);
                                }
                                break;
                            }
                            case MENU_ITEM_IMAGE_SAVE: {
                                if (inlineImage) {
                                    attachmentCallback.onSaveAttachment(attachmentViewInfo);
                                } else {
                                    //TODO: Use download manager
                                    new DownloadImageTask(getContext()).execute(uri.toString());
                                }
                                break;
                            }
                            case MENU_ITEM_IMAGE_COPY: {
                                String label = getContext().getString(
                                        R.string.webview_contextmenu_image_clipboard_label);
                                mClipboardManager.setText(label, uri.toString());
                                break;
                            }
                        }
                        return true;
                    }
                };

                menu.setHeaderTitle(inlineImage ?
                        context.getString(R.string.webview_contextmenu_image_title) : uri.toString());

                menu.add(Menu.NONE, MENU_ITEM_IMAGE_VIEW, 0,
                        context.getString(R.string.webview_contextmenu_image_view_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_IMAGE_SAVE, 1,
                        inlineImage ?
                                context.getString(R.string.webview_contextmenu_image_save_action) :
                                context.getString(R.string.webview_contextmenu_image_download_action))
                        .setOnMenuItemClickListener(listener);

                if (!inlineImage) {
                    menu.add(Menu.NONE, MENU_ITEM_IMAGE_COPY, 2,
                            context.getString(R.string.webview_contextmenu_image_copy_action))
                            .setOnMenuItemClickListener(listener);
                }

                break;
            }
            case HitTestResult.PHONE_TYPE: {
                final String phoneNumber = result.getExtra();
                OnMenuItemClickListener listener = new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case MENU_ITEM_PHONE_CALL: {
                                Uri uri = Uri.parse(WebView.SCHEME_TEL + phoneNumber);
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                startActivityIfAvailable(getContext(), intent);
                                break;
                            }
                            case MENU_ITEM_PHONE_SAVE: {
                                Contacts contacts = Contacts.getInstance(getContext());
                                contacts.addPhoneContact(phoneNumber);
                                break;
                            }
                            case MENU_ITEM_PHONE_COPY: {
                                String label = getContext().getString(
                                        R.string.webview_contextmenu_phone_clipboard_label);
                                mClipboardManager.setText(label, phoneNumber);
                                break;
                            }
                        }

                        return true;
                    }
                };

                menu.setHeaderTitle(phoneNumber);

                menu.add(Menu.NONE, MENU_ITEM_PHONE_CALL, 0,
                        context.getString(R.string.webview_contextmenu_phone_call_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_PHONE_SAVE, 1,
                        context.getString(R.string.webview_contextmenu_phone_save_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_PHONE_COPY, 2,
                        context.getString(R.string.webview_contextmenu_phone_copy_action))
                        .setOnMenuItemClickListener(listener);

                break;
            }
            case WebView.HitTestResult.EMAIL_TYPE: {
                final String email = result.getExtra();
                OnMenuItemClickListener listener = new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case MENU_ITEM_EMAIL_SEND: {
                                Uri uri = Uri.parse(WebView.SCHEME_MAILTO + email);
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                startActivityIfAvailable(getContext(), intent);
                                break;
                            }
                            case MENU_ITEM_EMAIL_SAVE: {
                                Contacts contacts = Contacts.getInstance(getContext());
                                contacts.createContact(new Address(email));
                                break;
                            }
                            case MENU_ITEM_EMAIL_COPY: {
                                String label = getContext().getString(
                                        R.string.webview_contextmenu_email_clipboard_label);
                                mClipboardManager.setText(label, email);
                                break;
                            }
                        }

                        return true;
                    }
                };

                menu.setHeaderTitle(email);

                menu.add(Menu.NONE, MENU_ITEM_EMAIL_SEND, 0,
                        context.getString(R.string.webview_contextmenu_email_send_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_EMAIL_SAVE, 1,
                        context.getString(R.string.webview_contextmenu_email_save_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_EMAIL_COPY, 2,
                        context.getString(R.string.webview_contextmenu_email_copy_action))
                        .setOnMenuItemClickListener(listener);

                break;
            }
        }
    }

    private AttachmentViewInfo getAttachmentViewInfoIfCidUri(Uri uri) {
        if (!"cid".equals(uri.getScheme())) {
            return null;
        }

        String cid = uri.getSchemeSpecificPart();
        Uri internalUri = currentAttachmentResolver.getAttachmentUriForContentId(cid);

        return attachments.get(internalUri);
    }

    private void startActivityIfAvailable(Context context, Intent intent) {
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.error_activity_not_found, Toast.LENGTH_LONG).show();
        }
    }

    public MessageContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    private boolean isShowingPictures() {
        return showingPictures;
    }

    private void setLoadPictures(boolean enable) {
        mMessageContentView.blockNetworkData(!enable);
        showingPictures = enable;
    }

    public void showPictures() {
        setLoadPictures(true);
        refreshDisplayedContent();
    }

    public void enableAttachmentButtons() {
        for (AttachmentView attachmentView : attachmentViewMap.values()) {
            attachmentView.enableButtons();
        }
    }

    public void disableAttachmentButtons() {
        for (AttachmentView attachmentView : attachmentViewMap.values()) {
            attachmentView.disableButtons();
        }
    }

    public void displayMessageViewContainer(MessageViewInfo messageViewInfo,
            final OnRenderingFinishedListener onRenderingFinishedListener, boolean automaticallyLoadPictures,
            boolean hideUnsignedTextDivider, AttachmentViewCallback attachmentCallback) {

        this.attachmentCallback = attachmentCallback;

        resetView();

        renderAttachments(messageViewInfo);

        if (mSavedState != null) {
            if (mSavedState.showingPictures) {
                setLoadPictures(true);
            }

            mSavedState = null;
        }

        String textToDisplay = messageViewInfo.text;
        if (textToDisplay != null && !isShowingPictures()) {
            if (Utility.hasExternalImages(textToDisplay)) {
                if (automaticallyLoadPictures) {
                    setLoadPictures(true);
                } else {
                    hasHiddenExternalImages = true;
                }
            }
        }

        if (textToDisplay == null) {
            textToDisplay = HtmlConverter.wrapStatusMessage(getContext().getString(R.string.webview_empty_message));
        }

        OnPageFinishedListener onPageFinishedListener = new OnPageFinishedListener() {
            @Override
            public void onPageFinished() {
                onRenderingFinishedListener.onLoadFinished();
            }
        };

        displayHtmlContentWithInlineAttachments(
                textToDisplay, messageViewInfo.attachmentResolver, onPageFinishedListener);

        if (!TextUtils.isEmpty(messageViewInfo.extraText)) {
            unsignedTextContainer.setVisibility(View.VISIBLE);
            unsignedTextDivider.setVisibility(hideUnsignedTextDivider ? View.GONE : View.VISIBLE);
            unsignedText.setText(messageViewInfo.extraText);
        }
    }

    public boolean hasHiddenExternalImages() {
        return hasHiddenExternalImages;
    }

    private void displayHtmlContentWithInlineAttachments(String htmlText, AttachmentResolver attachmentResolver,
            OnPageFinishedListener onPageFinishedListener) {
        currentHtmlText = htmlText;
        currentAttachmentResolver = attachmentResolver;
        mMessageContentView.displayHtmlContentWithInlineAttachments(htmlText, attachmentResolver, onPageFinishedListener);
    }

    private void refreshDisplayedContent() {
        mMessageContentView.displayHtmlContentWithInlineAttachments(currentHtmlText, currentAttachmentResolver, null);
    }

    private void clearDisplayedContent() {
        mMessageContentView.displayHtmlContentWithInlineAttachments("", null, null);
        unsignedTextContainer.setVisibility(View.GONE);
        unsignedText.setText("");
    }

    public void renderAttachments(MessageViewInfo messageViewInfo) {
        if (messageViewInfo.attachments != null) {
            for (AttachmentViewInfo attachment : messageViewInfo.attachments) {
                attachments.put(attachment.internalUri, attachment);
                if (attachment.inlineAttachment) {
                    continue;
                }

                AttachmentView view =
                        (AttachmentView) mInflater.inflate(R.layout.message_view_attachment, mAttachments, false);
                view.setCallback(attachmentCallback);
                view.setAttachment(attachment);

                attachmentViewMap.put(attachment, view);
                mAttachments.addView(view);
            }
        }

        if (messageViewInfo.extraAttachments != null) {
            for (AttachmentViewInfo attachment : messageViewInfo.extraAttachments) {
                attachments.put(attachment.internalUri, attachment);
                if (attachment.inlineAttachment) {
                    continue;
                }

                LockedAttachmentView view = (LockedAttachmentView) mInflater
                        .inflate(R.layout.message_view_attachment_locked, mAttachments, false);
                view.setCallback(attachmentCallback);
                view.setAttachment(attachment);

                // attachments.put(attachment, view);
                mAttachments.addView(view);
            }
        }
    }

    public void zoom(KeyEvent event) {
        if (event.isShiftPressed()) {
            mMessageContentView.zoomIn();
        } else {
            mMessageContentView.zoomOut();
        }
    }

    public void beginSelectingText() {
        mMessageContentView.emulateShiftHeld();
    }

    public void resetView() {
        setLoadPictures(false);
        mAttachments.removeAllViews();

        currentHtmlText = null;
        currentAttachmentResolver = null;

        /*
         * Clear the WebView content
         *
         * For some reason WebView.clearView() doesn't clear the contents when the WebView changes
         * its size because the button to download the complete message was previously shown and
         * is now hidden.
         */
        clearDisplayedContent();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState savedState = new SavedState(superState);

        savedState.attachmentViewVisible = (mAttachmentsContainer != null &&
                mAttachmentsContainer.getVisibility() == View.VISIBLE);
        savedState.showingPictures = showingPictures;

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

    @Override
    public void onLayoutChanged() {
        if (mMessageContentView != null) {
            mMessageContentView.invalidate();
        }
    }

    public void enableAttachmentButtons(AttachmentViewInfo attachment) {
        getAttachmentView(attachment).enableButtons();
    }

    public void disableAttachmentButtons(AttachmentViewInfo attachment) {
        getAttachmentView(attachment).disableButtons();
    }

    public void refreshAttachmentThumbnail(AttachmentViewInfo attachment) {
        getAttachmentView(attachment).refreshThumbnail();
    }

    private AttachmentView getAttachmentView(AttachmentViewInfo attachment) {
        return attachmentViewMap.get(attachment);
    }

    static class SavedState extends BaseSavedState {
        boolean attachmentViewVisible;
        boolean showingPictures;

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
            this.attachmentViewVisible = (in.readInt() != 0);
            this.showingPictures = (in.readInt() != 0);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt((this.attachmentViewVisible) ? 1 : 0);
            out.writeInt((this.showingPictures) ? 1 : 0);
        }
    }

    interface OnRenderingFinishedListener {
        void onLoadFinished();
    }
}
