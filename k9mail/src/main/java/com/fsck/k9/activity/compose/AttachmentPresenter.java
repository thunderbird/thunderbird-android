package com.fsck.k9.activity.compose;


import java.util.ArrayList;
import java.util.LinkedHashMap;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import com.fsck.k9.activity.compose.ComposeCryptoStatus.AttachErrorState;
import com.fsck.k9.activity.loader.AttachmentContentLoader;
import com.fsck.k9.activity.loader.AttachmentInfoLoader;
import com.fsck.k9.activity.misc.Attachment;
import com.fsck.k9.activity.misc.Attachment.LoadingState;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.LocalBodyPart;
import com.fsck.k9.provider.AttachmentProvider;


public class AttachmentPresenter {
    private static final String STATE_KEY_ATTACHMENTS = "com.fsck.k9.activity.MessageCompose.attachments";
    private static final String STATE_KEY_WAITING_FOR_ATTACHMENTS = "waitingForAttachments";
    private static final String STATE_KEY_NEXT_LOADER_ID = "nextLoaderId";

    private static final String LOADER_ARG_ATTACHMENT = "attachment";
    private static final int LOADER_ID_MASK = 1 << 6;
    public static final int MAX_TOTAL_LOADERS = LOADER_ID_MASK -1;
    public static final int REQUEST_CODE_ATTACHMENT_URI = 1;


    // injected state
    private final Context context;
    private final AttachmentMvpView attachmentMvpView;
    private final LoaderManager loaderManager;

    // persistent state
    private LinkedHashMap<Uri, Attachment> attachments;
    private int nextLoaderId = 0;
    private WaitingAction actionToPerformAfterWaiting = WaitingAction.NONE;


    public AttachmentPresenter(Context context, AttachmentMvpView attachmentMvpView, LoaderManager loaderManager) {
        this.context = context;
        this.attachmentMvpView = attachmentMvpView;
        this.loaderManager = loaderManager;

        attachments = new LinkedHashMap<>();
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

    public void onClickAddAttachment(RecipientPresenter recipientPresenter) {
        AttachErrorState maybeAttachErrorState = recipientPresenter.getCurrentCryptoStatus().getAttachErrorStateOrNull();
        if (maybeAttachErrorState != null) {
            recipientPresenter.showPgpAttachError(maybeAttachErrorState);
            return;
        }

        attachmentMvpView.showPickAttachmentDialog(REQUEST_CODE_ATTACHMENT_URI);
    }

    private void addAttachment(Uri uri) {
        addAttachment(uri, null);
    }

    public void addAttachment(Uri uri, String contentType) {
        int loaderId = getNextFreeLoaderId();
        Attachment attachment = Attachment.createAttachment(uri, loaderId, contentType);

        if (attachments.containsKey(uri)) {
            return;
        }

        attachments.put(uri, attachment);
        attachmentMvpView.addAttachmentView(attachment);

        initAttachmentInfoLoader(attachment);
    }

    private void initAttachmentInfoLoader(Attachment attachment) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(LOADER_ARG_ATTACHMENT, attachment.uri);

        loaderManager.initLoader(attachment.loaderId, bundle, mAttachmentInfoLoaderCallback);
    }

    private void initAttachmentContentLoader(Attachment attachment) {
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
                    Uri uri = args.getParcelable(LOADER_ARG_ATTACHMENT);
                    return new AttachmentInfoLoader(context, attachments.get(uri));
                }

                @Override
                public void onLoadFinished(Loader<Attachment> loader, Attachment attachment) {
                    int loaderId = loader.getId();
                    loaderManager.destroyLoader(loaderId);

                    if (!attachments.containsKey(attachment.uri)) {
                        return;
                    }

                    attachmentMvpView.updateAttachmentView(attachment);
                    attachments.put(attachment.uri, attachment);
                    initAttachmentContentLoader(attachment);
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
                    Uri uri = args.getParcelable(LOADER_ARG_ATTACHMENT);
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

    private void postPerformStalledAction() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                performStalledAction();
            }
        });
    }

    void performStalledAction() {
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

    /**
     * Add all attachments of an existing message as if they were added by hand.
     *
     * @param part
     *         The message part to check for being an attachment. This method will recurse if it's
     *         a multipart part.
     * @param depth
     *         The recursion depth. Currently unused.
     *
     * @return {@code true} if all attachments were able to be attached, {@code false} otherwise.
     *
     * @throws MessagingException
     *          In case of an error
     */
    public boolean loadAttachments(Part part, int depth) throws MessagingException {
        if (part.getBody() instanceof Multipart) {
            Multipart mp = (Multipart) part.getBody();
            boolean ret = true;
            for (int i = 0, count = mp.getCount(); i < count; i++) {
                if (!loadAttachments(mp.getBodyPart(i), depth + 1)) {
                    ret = false;
                }
            }
            return ret;
        }

        String contentType = part.getContentType();
        String name = MimeUtility.getHeaderParameter(contentType, "name");
        if (name != null) {
            if (part instanceof LocalBodyPart) {
                LocalBodyPart localBodyPart = (LocalBodyPart) part;
                String accountUuid = localBodyPart.getAccountUuid();
                long attachmentId = localBodyPart.getId();
                Uri uri = AttachmentProvider.getAttachmentUri(accountUuid, attachmentId);
                addAttachment(uri);
                return true;
            }
            return false;
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    void addAttachmentsFromResultIntent(Intent data) {
        // TODO draftNeedsSaving = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ClipData clipData = data.getClipData();
            if (clipData != null) {
                for (int i = 0, end = clipData.getItemCount(); i < end; i++) {
                    Uri uri = clipData.getItemAt(i).getUri();
                    if (uri != null) {
                        addAttachment(uri);
                    }
                }
                return;
            }
        }

        Uri uri = data.getData();
        if (uri != null) {
            addAttachment(uri);
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

    }
}
