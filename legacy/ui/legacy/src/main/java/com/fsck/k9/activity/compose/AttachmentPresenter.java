package com.fsck.k9.activity.compose;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import androidx.core.os.BundleCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import com.fsck.k9.activity.compose.ComposeCryptoStatus.AttachErrorState;
import com.fsck.k9.activity.loader.AttachmentContentLoader;
import com.fsck.k9.activity.loader.AttachmentInfoLoader;
import com.fsck.k9.activity.misc.Attachment;
import com.fsck.k9.activity.misc.InlineAttachment;
import app.k9mail.legacy.message.controller.MessageReference;
import net.thunderbird.core.common.exception.MessagingException;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.message.Attachment.LoadingState;
import com.fsck.k9.provider.RawMessageProvider;


public class AttachmentPresenter {
    private static final String STATE_KEY_ATTACHMENTS = "com.fsck.k9.activity.MessageCompose.attachments";
    private static final String STATE_KEY_WAITING_FOR_ATTACHMENTS = "waitingForAttachments";
    private static final String STATE_KEY_NEXT_LOADER_ID = "nextLoaderId";

    private static final String LOADER_ARG_ATTACHMENT = "attachment";
    private static final int LOADER_ID_MASK = 1 << 6;
    private static final int MAX_TOTAL_LOADERS = LOADER_ID_MASK - 1;
    public static final int REQUEST_CODE_ATTACHMENT_URI = 1;


    // injected state
    private final Context context;
    private final AttachmentMvpView attachmentMvpView;
    private final LoaderManager loaderManager;
    private final AttachmentsChangedListener listener;

    // persistent state
    private final LinkedHashMap<Uri, Attachment> attachments;
    private final LinkedHashMap<Uri, InlineAttachment> inlineAttachments;
    private int nextLoaderId = 0;
    private WaitingAction actionToPerformAfterWaiting = WaitingAction.NONE;


    public AttachmentPresenter(Context context, AttachmentMvpView attachmentMvpView, LoaderManager loaderManager,
                               AttachmentsChangedListener listener) {
        this.context = context;
        this.attachmentMvpView = attachmentMvpView;
        this.loaderManager = loaderManager;
        this.listener = listener;

        attachments = new LinkedHashMap<>();
        inlineAttachments = new LinkedHashMap<>();
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_KEY_WAITING_FOR_ATTACHMENTS, actionToPerformAfterWaiting.name());
        outState.putParcelableArrayList(STATE_KEY_ATTACHMENTS, createAttachmentList());
        outState.putInt(STATE_KEY_NEXT_LOADER_ID, nextLoaderId);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        actionToPerformAfterWaiting = WaitingAction.valueOf(
                savedInstanceState.getString(STATE_KEY_WAITING_FOR_ATTACHMENTS));
        nextLoaderId = savedInstanceState.getInt(STATE_KEY_NEXT_LOADER_ID);

        ArrayList<Attachment> attachmentList = savedInstanceState.getParcelableArrayList(STATE_KEY_ATTACHMENTS);
        // noinspection ConstantConditions, we know this is set in onSaveInstanceState
        for (Attachment attachment : attachmentList) {
            attachments.put(attachment.uri, attachment);
            attachmentMvpView.addAttachmentView(attachment);

            if (attachment.state == LoadingState.URI_ONLY) {
                initAttachmentInfoLoader(attachment);
            } else if (attachment.state == LoadingState.METADATA) {
                initAttachmentContentLoader(attachment);
            }
        }
    }

    public boolean checkOkForSendingOrDraftSaving() {
        if (actionToPerformAfterWaiting != WaitingAction.NONE) {
            return true;
        }

        if (hasLoadingAttachments()) {
            actionToPerformAfterWaiting = WaitingAction.SEND;
            attachmentMvpView.showWaitingForAttachmentDialog(actionToPerformAfterWaiting);
            return true;
        }

        return false;
    }

    private boolean hasLoadingAttachments() {
        for (Attachment attachment : attachments.values()) {
            Loader loader = loaderManager.getLoader(attachment.loaderId);
            if (loader != null && loader.isStarted()) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<Attachment> createAttachmentList() {
        ArrayList<Attachment> result = new ArrayList<>();
        for (Attachment attachment : attachments.values()) {
            result.add(attachment);
        }
        return result;
    }

    public List<com.fsck.k9.message.Attachment> getAttachments() {
        return new ArrayList<>(attachments.values());
    }

    public Map<String, com.fsck.k9.message.Attachment> getInlineAttachments() {
        Map<String, com.fsck.k9.message.Attachment> result = new LinkedHashMap<>();
        for (InlineAttachment attachment : inlineAttachments.values()) {
            result.put(attachment.getContentId(), attachment.getAttachment());
        }
        return result;
    }

    public void onClickAddAttachment(RecipientPresenter recipientPresenter) {
        ComposeCryptoStatus currentCachedCryptoStatus = recipientPresenter.getCurrentCachedCryptoStatus();
        if (currentCachedCryptoStatus == null) {
            return;
        }

        AttachErrorState maybeAttachErrorState = currentCachedCryptoStatus.getAttachErrorStateOrNull();
        if (maybeAttachErrorState != null) {
            recipientPresenter.showPgpAttachError(maybeAttachErrorState);
            return;
        }

        attachmentMvpView.showPickAttachmentDialog(REQUEST_CODE_ATTACHMENT_URI);
    }

    private void addExternalAttachment(Uri uri) {
        addExternalAttachment(uri, null);
    }

    private void addInternalAttachment(AttachmentViewInfo attachmentViewInfo) {
        if (attachments.containsKey(attachmentViewInfo.internalUri)) {
            throw new IllegalStateException("Received the same attachmentViewInfo twice!");
        }

        int loaderId = getNextFreeLoaderId();
        Attachment attachment = Attachment.createAttachment(
                attachmentViewInfo.internalUri, loaderId, attachmentViewInfo.mimeType, true, true);
        attachment = attachment.deriveWithMetadataLoaded(
                attachmentViewInfo.mimeType, attachmentViewInfo.displayName, attachmentViewInfo.size);

        addAttachmentAndStartLoader(attachment);
    }

    public void addExternalAttachment(Uri uri, String contentType) {
        addAttachment(uri, contentType, false, false);
    }

    private void addInlineAttachment(AttachmentViewInfo attachmentViewInfo) {
        if (inlineAttachments.containsKey(attachmentViewInfo.internalUri)) {
            throw new IllegalStateException("Received the same attachmentViewInfo twice!");
        }

        int loaderId = getNextFreeLoaderId();
        Attachment attachment = Attachment.createAttachment(
                attachmentViewInfo.internalUri, loaderId, attachmentViewInfo.mimeType, true, true);
        attachment = attachment.deriveWithMetadataLoaded(
                attachmentViewInfo.mimeType, attachmentViewInfo.displayName, attachmentViewInfo.size);

        inlineAttachments.put(attachment.uri, new InlineAttachment(attachmentViewInfo.part.getContentId(), attachment));

        Bundle bundle = new Bundle();
        bundle.putParcelable(LOADER_ARG_ATTACHMENT, attachment.uri);
        loaderManager.initLoader(attachment.loaderId, bundle, mInlineAttachmentContentLoaderCallback);
    }

    private void addInternalAttachment(Uri uri, String contentType, boolean allowMessageType) {
        addAttachment(uri, contentType, allowMessageType, true);
    }

    private void addAttachment(Uri uri, String contentType, boolean allowMessageType, boolean internalAttachment) {
        if (attachments.containsKey(uri)) {
            return;
        }

        int loaderId = getNextFreeLoaderId();
        Attachment attachment = Attachment.createAttachment(uri, loaderId, contentType, allowMessageType, internalAttachment);

        addAttachmentAndStartLoader(attachment);
    }

    public boolean loadAllAvailableAttachments(MessageViewInfo messageViewInfo) {
        boolean allPartsAvailable = true;

        for (AttachmentViewInfo attachmentViewInfo : messageViewInfo.attachments) {
            if (attachmentViewInfo.isContentAvailable()) {
                if (attachmentViewInfo.inlineAttachment) {
                    addInlineAttachment(attachmentViewInfo);
                } else {
                    addInternalAttachment(attachmentViewInfo);
                }
            } else {
                allPartsAvailable = false;
            }
        }

        return allPartsAvailable;
    }

    public void processMessageToForward(MessageViewInfo messageViewInfo) {
        boolean isMissingParts = !loadAllAvailableAttachments(messageViewInfo);
        if (isMissingParts) {
            attachmentMvpView.showMissingAttachmentsPartialMessageWarning();
        }
    }

    public void processMessageToForwardAsAttachment(MessageViewInfo messageViewInfo) throws MessagingException {
        if (messageViewInfo.isMessageIncomplete) {
            attachmentMvpView.showMissingAttachmentsPartialMessageForwardWarning();
        } else {
            LocalMessage localMessage = (LocalMessage) messageViewInfo.message;
            MessageReference messageReference = localMessage.makeMessageReference();
            Uri rawMessageUri = RawMessageProvider.getRawMessageUri(messageReference);

            addInternalAttachment(rawMessageUri, "message/rfc822", true);
        }
    }

    private void addAttachmentAndStartLoader(Attachment attachment) {
        attachments.put(attachment.uri, attachment);
        listener.onAttachmentAdded();
        attachmentMvpView.addAttachmentView(attachment);

        if (attachment.state == LoadingState.URI_ONLY) {
            initAttachmentInfoLoader(attachment);
        } else if (attachment.state == LoadingState.METADATA) {
            initAttachmentContentLoader(attachment);
        } else {
            throw new IllegalStateException("Attachment can only be added in URI_ONLY or METADATA state!");
        }
    }

    private void initAttachmentInfoLoader(Attachment attachment) {
        if (attachment.state != LoadingState.URI_ONLY) {
            throw new IllegalStateException("initAttachmentInfoLoader can only be called for URI_ONLY state!");
        }

        Bundle bundle = new Bundle();
        bundle.putParcelable(LOADER_ARG_ATTACHMENT, attachment.uri);
        loaderManager.initLoader(attachment.loaderId, bundle, mAttachmentInfoLoaderCallback);
    }

    private void initAttachmentContentLoader(Attachment attachment) {
        if (attachment.state != LoadingState.METADATA) {
            throw new IllegalStateException("initAttachmentContentLoader can only be called for METADATA state!");
        }

        Bundle bundle = new Bundle();
        bundle.putParcelable(LOADER_ARG_ATTACHMENT, attachment.uri);
        loaderManager.initLoader(attachment.loaderId, bundle, mAttachmentContentLoaderCallback);
    }

    private int getNextFreeLoaderId() {
        if (nextLoaderId >= MAX_TOTAL_LOADERS) {
            throw new AssertionError("more than " + MAX_TOTAL_LOADERS + " attachments? hum.");
        }
        return LOADER_ID_MASK | nextLoaderId++;
    }

    private LoaderManager.LoaderCallbacks<Attachment> mAttachmentInfoLoaderCallback =
            new LoaderManager.LoaderCallbacks<Attachment>() {
                @Override
                public Loader<Attachment> onCreateLoader(int id, Bundle args) {
                    Uri uri = BundleCompat.getParcelable(args, LOADER_ARG_ATTACHMENT, Uri.class);
                    return new AttachmentInfoLoader(context, attachments.get(uri));
                }

                @Override
                public void onLoadFinished(Loader<Attachment> loader, Attachment attachment) {
                    int loaderId = loader.getId();
                    loaderManager.destroyLoader(loaderId);

                    if (!attachments.containsKey(attachment.uri)) {
                        return;
                    }

                    if (attachment.state == LoadingState.METADATA) {
                        attachmentMvpView.updateAttachmentView(attachment);
                        attachments.put(attachment.uri, attachment);
                        initAttachmentContentLoader(attachment);
                    } else {
                        attachments.remove(attachment.uri);
                        attachmentMvpView.removeAttachmentView(attachment);
                    }
                }

                @Override
                public void onLoaderReset(Loader<Attachment> loader) {
                    // nothing to do
                }
            };

    private LoaderManager.LoaderCallbacks<Attachment> mAttachmentContentLoaderCallback =
            new LoaderManager.LoaderCallbacks<Attachment>() {
                @Override
                public Loader<Attachment> onCreateLoader(int id, Bundle args) {
                    Uri uri = BundleCompat.getParcelable(args, LOADER_ARG_ATTACHMENT, Uri.class);
                    return new AttachmentContentLoader(context, attachments.get(uri));
                }

                @Override
                public void onLoadFinished(Loader<Attachment> loader, Attachment attachment) {
                    int loaderId = loader.getId();
                    loaderManager.destroyLoader(loaderId);

                    if (!attachments.containsKey(attachment.uri)) {
                        return;
                    }

                    if (attachment.state == Attachment.LoadingState.COMPLETE) {
                        attachmentMvpView.updateAttachmentView(attachment);
                        attachments.put(attachment.uri, attachment);
                    } else {
                        attachments.remove(attachment.uri);
                        attachmentMvpView.removeAttachmentView(attachment);
                    }

                    postPerformStalledAction();
                }

                @Override
                public void onLoaderReset(Loader<Attachment> loader) {
                    // nothing to do
                }
            };

    private LoaderManager.LoaderCallbacks<Attachment> mInlineAttachmentContentLoaderCallback =
            new LoaderManager.LoaderCallbacks<Attachment>() {
                @Override
                public Loader<Attachment> onCreateLoader(int id, Bundle args) {
                    Uri uri = BundleCompat.getParcelable(args, LOADER_ARG_ATTACHMENT, Uri.class);
                    return new AttachmentContentLoader(context, inlineAttachments.get(uri).getAttachment());
                }

                @Override
                public void onLoadFinished(Loader<Attachment> loader, Attachment attachment) {
                    int loaderId = loader.getId();
                    loaderManager.destroyLoader(loaderId);

                    if (attachment.state == Attachment.LoadingState.COMPLETE) {
                        inlineAttachments.put(attachment.uri, new InlineAttachment(
                                inlineAttachments.get(attachment.uri).getContentId(), attachment));
                    } else {
                        inlineAttachments.remove(attachment.uri);
                    }

                    postPerformStalledAction();
                }

                @Override
                public void onLoaderReset(Loader<Attachment> loader) {
                    // nothing to do
                }
            };

    private void postPerformStalledAction() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                performStalledAction();
            }
        });
    }

    private void performStalledAction() {
        attachmentMvpView.dismissWaitingForAttachmentDialog();

        WaitingAction waitingFor = actionToPerformAfterWaiting;
        actionToPerformAfterWaiting = WaitingAction.NONE;

        switch (waitingFor) {
            case SEND: {
                attachmentMvpView.performSendAfterChecks();
                break;
            }
            case SAVE: {
                attachmentMvpView.performSaveAfterChecks();
                break;
            }
        }
    }

    private void addAttachmentsFromResultIntent(Intent data) {
        // TODO draftNeedsSaving = true
        ClipData clipData = data.getClipData();
        if (clipData != null) {
            for (int i = 0, end = clipData.getItemCount(); i < end; i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                if (uri != null) {
                    addExternalAttachment(uri);
                }
            }
            return;
        }

        Uri uri = data.getData();
        if (uri != null) {
            addExternalAttachment(uri);
        }
    }

    public void attachmentProgressDialogCancelled() {
        actionToPerformAfterWaiting = WaitingAction.NONE;
    }

    public void onClickRemoveAttachment(Uri uri) {
        Attachment attachment = attachments.get(uri);

        loaderManager.destroyLoader(attachment.loaderId);

        attachmentMvpView.removeAttachmentView(attachment);
        attachments.remove(uri);
        listener.onAttachmentRemoved();
    }

    public void onActivityResult(int resultCode, int requestCode, Intent data) {
        if (requestCode != REQUEST_CODE_ATTACHMENT_URI) {
            throw new AssertionError("onActivityResult must only be called for our request code");
        }
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (data == null) {
            return;
        }
        addAttachmentsFromResultIntent(data);
    }

    public enum WaitingAction {
        NONE,
        SEND,
        SAVE
    }

    public interface AttachmentMvpView {
        void showWaitingForAttachmentDialog(WaitingAction waitingAction);
        void dismissWaitingForAttachmentDialog();
        void showPickAttachmentDialog(int requestCode);

        void addAttachmentView(Attachment attachment);
        void removeAttachmentView(Attachment attachment);
        void updateAttachmentView(Attachment attachment);

        // TODO these should not really be here :\
        void performSendAfterChecks();
        void performSaveAfterChecks();

        void showMissingAttachmentsPartialMessageWarning();
        void showMissingAttachmentsPartialMessageForwardWarning();
    }

    public interface AttachmentsChangedListener {
        void onAttachmentAdded();
        void onAttachmentRemoved();
    }
}
