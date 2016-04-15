package com.fsck.k9.ui.messageview;


import java.util.Collections;
import java.util.Locale;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.ChooseFolder;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.fragment.ConfirmationDialogFragment;
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.fsck.k9.fragment.ProgressDialogFragment;
import com.fsck.k9.helper.FileBrowserHelper;
import com.fsck.k9.helper.FileBrowserHelper.FileBrowserFailOverCallback;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.ui.crypto.MessageCryptoAnnotations;
import com.fsck.k9.ui.crypto.MessageCryptoCallback;
import com.fsck.k9.ui.crypto.MessageCryptoHelper;
import com.fsck.k9.ui.message.LocalMessageExtractorLoader;
import com.fsck.k9.ui.message.LocalMessageLoader;
import com.fsck.k9.view.MessageHeader;

public class MessageViewFragment extends Fragment implements ConfirmationDialogFragmentListener,
        AttachmentViewCallback, MessageCryptoCallback {

    private static final String ARG_REFERENCE = "reference";

    private static final int ACTIVITY_CHOOSE_FOLDER_MOVE = 1;
    private static final int ACTIVITY_CHOOSE_FOLDER_COPY = 2;
    private static final int ACTIVITY_CHOOSE_DIRECTORY = 3;

    private static final int LOCAL_MESSAGE_LOADER_ID = 1;
    private static final int DECODE_MESSAGE_LOADER_ID = 2;

    public static final int REQUEST_MASK_CRYPTO_HELPER = 1 << 8;

    public static MessageViewFragment newInstance(MessageReference reference) {
        MessageViewFragment fragment = new MessageViewFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_REFERENCE, reference);
        fragment.setArguments(args);

        return fragment;
    }

    private MessageTopView mMessageView;
    private Account mAccount;
    private MessageReference mMessageReference;
    private LocalMessage mMessage;
    private MessageCryptoAnnotations messageAnnotations;
    private MessagingController mController;
    private DownloadManager downloadManager;
    private Handler handler = new Handler();
    private DownloadMessageListener downloadMessageListener = new DownloadMessageListener();
    private MessageCryptoHelper messageCryptoHelper;

    /**
     * Used to temporarily store the destination folder for refile operations if a confirmation
     * dialog is shown.
     */
    private String mDstFolder;

    private MessageViewFragmentListener mFragmentListener;

    /**
     * {@code true} after {@link #onCreate(Bundle)} has been executed. This is used by
     * {@code MessageList.configureMenu()} to make sure the fragment has been initialized before
     * it is used.
     */
    private boolean mInitialized = false;

    private Context mContext;

    private LoaderCallbacks<LocalMessage> localMessageLoaderCallback = new LocalMessageLoaderCallback();
    private LoaderCallbacks<MessageViewInfo> decodeMessageLoaderCallback = new DecodeMessageLoaderCallback();
    private MessageViewInfo messageViewInfo;
    private AttachmentViewInfo currentAttachmentViewInfo;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mContext = activity.getApplicationContext();

        try {
            mFragmentListener = (MessageViewFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.getClass() +
                    " must implement MessageViewFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This fragments adds options to the action bar
        setHasOptionsMenu(true);

        Context context = getActivity().getApplicationContext();
        mController = MessagingController.getInstance(context);
        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        mInitialized = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Context context = new ContextThemeWrapper(inflater.getContext(),
                K9.getK9ThemeResourceId(K9.getK9MessageViewTheme()));
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.message, container, false);

        mMessageView = (MessageTopView) view.findViewById(R.id.message_view);
        mMessageView.setAttachmentCallback(this);

        mMessageView.setOnToggleFlagClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleFlagged();
            }
        });

        mMessageView.setOnDownloadButtonClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onDownloadRemainder();
            }
        });

        mFragmentListener.messageHeaderViewAvailable(mMessageView.getMessageHeaderView());

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle arguments = getArguments();
        MessageReference messageReference = arguments.getParcelable(ARG_REFERENCE);

        displayMessage(messageReference);
    }

    private void displayMessage(MessageReference messageReference) {
        mMessageReference = messageReference;
        if (K9.DEBUG) {
            Log.d(K9.LOG_TAG, "MessageView displaying message " + mMessageReference);
        }

        Activity activity = getActivity();
        Context appContext = activity.getApplicationContext();
        mAccount = Preferences.getPreferences(appContext).getAccount(mMessageReference.getAccountUuid());
        messageCryptoHelper = new MessageCryptoHelper(activity, mAccount, this);

        startLoadingMessageFromDatabase();

        mFragmentListener.updateMenu();
    }



    public void onPendingIntentResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode & REQUEST_MASK_CRYPTO_HELPER) == REQUEST_MASK_CRYPTO_HELPER) {
            hideKeyboard();

            requestCode ^= REQUEST_MASK_CRYPTO_HELPER;
            messageCryptoHelper.onActivityResult(requestCode, resultCode, data);
            return;
        }
    }

    private void hideKeyboard() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        View decorView = activity.getWindow().getDecorView();
        if (decorView != null) {
            imm.hideSoftInputFromWindow(decorView.getApplicationWindowToken(), 0);
        }
    }

    private void startLoadingMessageFromDatabase() {
        getLoaderManager().initLoader(LOCAL_MESSAGE_LOADER_ID, null, localMessageLoaderCallback);
    }

    private void onLoadMessageFromDatabaseFinished(LocalMessage message) {
        displayMessageHeader(message);

        if (message.isBodyMissing()) {
            startDownloadingMessageBody();
        } else {
            messageCryptoHelper.decryptOrVerifyMessagePartsIfNecessary(message);
        }
    }

    private void onLoadMessageFromDatabaseFailed() {
        // mMessageView.showStatusMessage(mContext.getString(R.string.status_invalid_id_error));
    }

    private void startDownloadingMessageBody() {
        mController.loadMessagePartialForViewRemote(
                mAccount, mMessageReference.getFolderName(), mMessageReference.getUid(), downloadMessageListener);
    }

    private void onMessageDownloadFinished(LocalMessage message) {
        mMessage = message;

        LoaderManager loaderManager = getLoaderManager();
        loaderManager.destroyLoader(LOCAL_MESSAGE_LOADER_ID);
        loaderManager.destroyLoader(DECODE_MESSAGE_LOADER_ID);

        onLoadMessageFromDatabaseFinished(mMessage);
    }

    private void onDownloadMessageFailed(Throwable t) {
        mMessageView.enableDownloadButton();
        String errorMessage;
        if (t instanceof IllegalArgumentException) {
            errorMessage = mContext.getString(R.string.status_invalid_id_error);
        } else {
            errorMessage = mContext.getString(R.string.status_network_error);
        }
        Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCryptoHelperProgress(int current, int max) {
        // TODO implement
    }

    @Override
    public void onCryptoOperationsFinished(MessageCryptoAnnotations annotations) {
        startExtractingTextAndAttachments(annotations);
    }

    private void startExtractingTextAndAttachments(MessageCryptoAnnotations annotations) {
        this.messageAnnotations = annotations;
        getLoaderManager().initLoader(DECODE_MESSAGE_LOADER_ID, null, decodeMessageLoaderCallback);
    }

    private void onDecodeMessageFinished(MessageViewInfo messageViewInfo) {
        if (messageViewInfo == null) {
            showUnableToDecodeError();
            messageViewInfo = MessageViewInfo.createWithErrorState(mMessage);
        }

        this.messageViewInfo = messageViewInfo;
        showMessage(messageViewInfo);
    }

    private void showUnableToDecodeError() {
        Context context = getActivity().getApplicationContext();
        Toast.makeText(context, R.string.message_view_toast_unable_to_display_message, Toast.LENGTH_SHORT).show();
    }

    private void showMessage(MessageViewInfo messageViewInfo) {
        try {
            mMessageView.setMessage(mAccount, messageViewInfo);
            mMessageView.setShowDownloadButton(mMessage);
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Error while trying to display message", e);
        }
    }

    private void displayMessageHeader(LocalMessage message) {
        mMessageView.setHeaders(message, mAccount);
        displayMessageSubject(getSubjectForMessage(message));
        mFragmentListener.updateMenu();
    }

    /**
     * Called from UI thread when user select Delete
     */
    public void onDelete() {
        if (K9.confirmDelete() || (K9.confirmDeleteStarred() && mMessage.isSet(Flag.FLAGGED))) {
            showDialog(R.id.dialog_confirm_delete);
        } else {
            delete();
        }
    }

    public void onToggleAllHeadersView() {
        mMessageView.getMessageHeaderView().onShowAdditionalHeaders();
    }

    public boolean allHeadersVisible() {
        return mMessageView.getMessageHeaderView().additionalHeadersVisible();
    }

    private void delete() {
        if (mMessage != null) {
            // Disable the delete button after it's tapped (to try to prevent
            // accidental clicks)
            mFragmentListener.disableDeleteAction();
            LocalMessage messageToDelete = mMessage;
            mFragmentListener.showNextMessageOrReturn();
            mController.deleteMessages(Collections.singletonList(messageToDelete), null);
        }
    }

    public void onRefile(String dstFolder) {
        if (!mController.isMoveCapable(mAccount)) {
            return;
        }
        if (!mController.isMoveCapable(mMessage)) {
            Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        if (K9.FOLDER_NONE.equalsIgnoreCase(dstFolder)) {
            return;
        }

        if (mAccount.getSpamFolderName().equals(dstFolder) && K9.confirmSpam()) {
            mDstFolder = dstFolder;
            showDialog(R.id.dialog_confirm_spam);
        } else {
            refileMessage(dstFolder);
        }
    }

    private void refileMessage(String dstFolder) {
        String srcFolder = mMessageReference.getFolderName();
        LocalMessage messageToMove = mMessage;
        mFragmentListener.showNextMessageOrReturn();
        mController.moveMessage(mAccount, srcFolder, messageToMove, dstFolder, null);
    }

    public void onReply() {
        if (mMessage != null) {
            mFragmentListener.onReply(mMessage);
        }
    }

    public void onReplyAll() {
        if (mMessage != null) {
            mFragmentListener.onReplyAll(mMessage);
        }
    }

    public void onForward() {
        if (mMessage != null) {
            mFragmentListener.onForward(mMessage);
        }
    }

    public void onToggleFlagged() {
        if (mMessage != null) {
            boolean newState = !mMessage.isSet(Flag.FLAGGED);
            mController.setFlag(mAccount, mMessage.getFolder().getName(),
                    Collections.singletonList(mMessage), Flag.FLAGGED, newState);
            mMessageView.setHeaders(mMessage, mAccount);
        }
    }

    public void onMove() {
        if ((!mController.isMoveCapable(mAccount))
                || (mMessage == null)) {
            return;
        }
        if (!mController.isMoveCapable(mMessage)) {
            Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        startRefileActivity(ACTIVITY_CHOOSE_FOLDER_MOVE);

    }

    public void onCopy() {
        if ((!mController.isCopyCapable(mAccount))
                || (mMessage == null)) {
            return;
        }
        if (!mController.isCopyCapable(mMessage)) {
            Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        startRefileActivity(ACTIVITY_CHOOSE_FOLDER_COPY);
    }

    public void onArchive() {
        onRefile(mAccount.getArchiveFolderName());
    }

    public void onSpam() {
        onRefile(mAccount.getSpamFolderName());
    }

    public void onSelectText() {
        // FIXME
        // mMessageView.beginSelectingText();
    }

    private void startRefileActivity(int activity) {
        Intent intent = new Intent(getActivity(), ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount.getUuid());
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, mMessageReference.getFolderName());
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, mAccount.getLastSelectedFolderName());
        intent.putExtra(ChooseFolder.EXTRA_MESSAGE, mMessageReference);
        startActivityForResult(intent, activity);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case ACTIVITY_CHOOSE_DIRECTORY: {
                if (data != null) {
                    // obtain the filename
                    Uri fileUri = data.getData();
                    if (fileUri != null) {
                        String filePath = fileUri.getPath();
                        if (filePath != null) {
                            getAttachmentController(currentAttachmentViewInfo).saveAttachmentTo(filePath);
                        }
                    }
                }
                break;
            }
            case ACTIVITY_CHOOSE_FOLDER_MOVE:
            case ACTIVITY_CHOOSE_FOLDER_COPY: {
                if (data == null) {
                    return;
                }

                String destFolderName = data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER);
                MessageReference ref = data.getParcelableExtra(ChooseFolder.EXTRA_MESSAGE);
                if (mMessageReference.equals(ref)) {
                    mAccount.setLastSelectedFolderName(destFolderName);
                    switch (requestCode) {
                        case ACTIVITY_CHOOSE_FOLDER_MOVE: {
                            mFragmentListener.showNextMessageOrReturn();
                            moveMessage(ref, destFolderName);
                            break;
                        }
                        case ACTIVITY_CHOOSE_FOLDER_COPY: {
                            copyMessage(ref, destFolderName);
                            break;
                        }
                    }
                }
                break;
            }
        }
    }

    public void onSendAlternate() {
        if (mMessage != null) {
            mController.sendAlternate(getActivity(), mAccount, mMessage);
        }
    }

    public void onToggleRead() {
        if (mMessage != null) {
            mController.setFlag(mAccount, mMessage.getFolder().getName(),
                    Collections.singletonList(mMessage), Flag.SEEN, !mMessage.isSet(Flag.SEEN));
            mMessageView.setHeaders(mMessage, mAccount);
            String subject = mMessage.getSubject();
            displayMessageSubject(subject);
            mFragmentListener.updateMenu();
        }
    }

    private void onDownloadRemainder() {
        if (mMessage.isSet(Flag.X_DOWNLOADED_FULL)) {
            return;
        }
        mMessageView.disableDownloadButton();
        mController.loadMessageForViewRemote(mAccount, mMessageReference.getFolderName(), mMessageReference.getUid(),
                downloadMessageListener);
    }

    private void setProgress(boolean enable) {
        if (mFragmentListener != null) {
            mFragmentListener.setProgress(enable);
        }
    }

    private void displayMessageSubject(String subject) {
        if (mFragmentListener != null) {
            mFragmentListener.displayMessageSubject(subject);
        }
    }

    private String getSubjectForMessage(LocalMessage message) {
        String subject = message.getSubject();
        if (TextUtils.isEmpty(subject)) {
            return mContext.getString(R.string.general_no_subject);
        }

        return subject;
    }

    public void moveMessage(MessageReference reference, String destFolderName) {
        mController.moveMessage(mAccount, mMessageReference.getFolderName(), mMessage,
                destFolderName, null);
    }

    public void copyMessage(MessageReference reference, String destFolderName) {
        mController.copyMessage(mAccount, mMessageReference.getFolderName(), mMessage,
                destFolderName, null);
    }

    private void showDialog(int dialogId) {
        DialogFragment fragment;
        switch (dialogId) {
            case R.id.dialog_confirm_delete: {
                String title = getString(R.string.dialog_confirm_delete_title);
                String message = getString(R.string.dialog_confirm_delete_message);
                String confirmText = getString(R.string.dialog_confirm_delete_confirm_button);
                String cancelText = getString(R.string.dialog_confirm_delete_cancel_button);

                fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message,
                        confirmText, cancelText);
                break;
            }
            case R.id.dialog_confirm_spam: {
                String title = getString(R.string.dialog_confirm_spam_title);
                String message = getResources().getQuantityString(R.plurals.dialog_confirm_spam_message, 1);
                String confirmText = getString(R.string.dialog_confirm_spam_confirm_button);
                String cancelText = getString(R.string.dialog_confirm_spam_cancel_button);

                fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message,
                        confirmText, cancelText);
                break;
            }
            case R.id.dialog_attachment_progress: {
                String message = getString(R.string.dialog_attachment_progress_title);
                fragment = ProgressDialogFragment.newInstance(null, message);
                break;
            }
            default: {
                throw new RuntimeException("Called showDialog(int) with unknown dialog id.");
            }
        }

        fragment.setTargetFragment(this, dialogId);
        fragment.show(getFragmentManager(), getDialogTag(dialogId));
    }

    private void removeDialog(int dialogId) {
        FragmentManager fm = getFragmentManager();

        if (fm == null || isRemoving() || isDetached()) {
            return;
        }

        // Make sure the "show dialog" transaction has been processed when we call
        // findFragmentByTag() below. Otherwise the fragment won't be found and the dialog will
        // never be dismissed.
        fm.executePendingTransactions();

        DialogFragment fragment = (DialogFragment) fm.findFragmentByTag(getDialogTag(dialogId));

        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private String getDialogTag(int dialogId) {
        return String.format(Locale.US, "dialog-%d", dialogId);
    }

    public void zoom(KeyEvent event) {
        // mMessageView.zoom(event);
    }

    @Override
    public void doPositiveClick(int dialogId) {
        switch (dialogId) {
            case R.id.dialog_confirm_delete: {
                delete();
                break;
            }
            case R.id.dialog_confirm_spam: {
                refileMessage(mDstFolder);
                mDstFolder = null;
                break;
            }
        }
    }

    @Override
    public void doNegativeClick(int dialogId) {
        /* do nothing */
    }

    @Override
    public void dialogCancelled(int dialogId) {
        /* do nothing */
    }

    /**
     * Get the {@link MessageReference} of the currently displayed message.
     */
    public MessageReference getMessageReference() {
        return mMessageReference;
    }

    public boolean isMessageRead() {
        return (mMessage != null) ? mMessage.isSet(Flag.SEEN) : false;
    }

    public boolean isCopyCapable() {
        return mController.isCopyCapable(mAccount);
    }

    public boolean isMoveCapable() {
        return mController.isMoveCapable(mAccount);
    }

    public boolean canMessageBeArchived() {
        return (!mMessageReference.getFolderName().equals(mAccount.getArchiveFolderName())
                && mAccount.hasArchiveFolder());
    }

    public boolean canMessageBeMovedToSpam() {
        return (!mMessageReference.getFolderName().equals(mAccount.getSpamFolderName())
                && mAccount.hasSpamFolder());
    }

    public void updateTitle() {
        if (mMessage != null) {
            displayMessageSubject(mMessage.getSubject());
        }
    }

    public Context getApplicationContext() {
        return mContext;
    }

    public void disableAttachmentButtons(AttachmentViewInfo attachment) {
        // mMessageView.disableAttachmentButtons(attachment);
    }

    public void enableAttachmentButtons(AttachmentViewInfo attachment) {
        // mMessageView.enableAttachmentButtons(attachment);
    }

    public void runOnMainThread(Runnable runnable) {
        handler.post(runnable);
    }

    public void showAttachmentLoadingDialog() {
        // mMessageView.disableAttachmentButtons();
        showDialog(R.id.dialog_attachment_progress);
    }

    public void hideAttachmentLoadingDialogOnMainThread() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                removeDialog(R.id.dialog_attachment_progress);
                // mMessageView.enableAttachmentButtons();
            }
        });
    }

    public void refreshAttachmentThumbnail(AttachmentViewInfo attachment) {
        // mMessageView.refreshAttachmentThumbnail(attachment);
    }

    @Override
    public void startPendingIntentForCryptoHelper(IntentSender si, int requestCode, Intent fillIntent,
            int flagsMask, int flagValues, int extraFlags) throws SendIntentException {
        requestCode |= REQUEST_MASK_CRYPTO_HELPER;
        getActivity().startIntentSenderForResult(
                si, requestCode, fillIntent, flagsMask, flagValues, extraFlags);
    }

    public interface MessageViewFragmentListener {
        public void onForward(LocalMessage mMessage);
        public void disableDeleteAction();
        public void onReplyAll(LocalMessage mMessage);
        public void onReply(LocalMessage mMessage);
        public void displayMessageSubject(String title);
        public void setProgress(boolean b);
        public void showNextMessageOrReturn();
        public void messageHeaderViewAvailable(MessageHeader messageHeaderView);
        public void updateMenu();
    }

    public boolean isInitialized() {
        return mInitialized ;
    }

    class LocalMessageLoaderCallback implements LoaderCallbacks<LocalMessage> {
        @Override
        public Loader<LocalMessage> onCreateLoader(int id, Bundle args) {
            setProgress(true);
            return new LocalMessageLoader(mContext, mController, mAccount, mMessageReference);
        }

        @Override
        public void onLoadFinished(Loader<LocalMessage> loader, LocalMessage message) {
            setProgress(false);
            mMessage = message;
            if (message == null) {
                onLoadMessageFromDatabaseFailed();
            } else {
                onLoadMessageFromDatabaseFinished(message);
            }
            getLoaderManager().destroyLoader(LOCAL_MESSAGE_LOADER_ID);
        }

        @Override
        public void onLoaderReset(Loader<LocalMessage> loader) {
            // Do nothing
        }
    }

    class DecodeMessageLoaderCallback implements LoaderCallbacks<MessageViewInfo> {
        @Override
        public Loader<MessageViewInfo> onCreateLoader(int id, Bundle args) {
            if (id != DECODE_MESSAGE_LOADER_ID) {
                throw new IllegalStateException("loader id must be message decoder id");
            }
            setProgress(true);
            return new LocalMessageExtractorLoader(mContext, mMessage, messageAnnotations);
        }

        @Override
        public void onLoadFinished(Loader<MessageViewInfo> loader, MessageViewInfo messageViewInfo) {
            if (loader.getId() != DECODE_MESSAGE_LOADER_ID) {
                throw new IllegalStateException("loader id must be message decoder id");
            }
            setProgress(false);
            onDecodeMessageFinished(messageViewInfo);
        }

        @Override
        public void onLoaderReset(Loader<MessageViewInfo> loader) {
            if (loader.getId() != DECODE_MESSAGE_LOADER_ID) {
                throw new IllegalStateException("loader id must be message decoder id");
            }
            // Do nothing
        }
    }

    @Override
    public void onViewAttachment(AttachmentViewInfo attachment) {
        //TODO: check if we have to download the attachment first

        getAttachmentController(attachment).viewAttachment();
    }

    @Override
    public void onSaveAttachment(AttachmentViewInfo attachment) {
        //TODO: check if we have to download the attachment first

        getAttachmentController(attachment).saveAttachment();
    }

    @Override
    public void onSaveAttachmentToUserProvidedDirectory(final AttachmentViewInfo attachment) {
        //TODO: check if we have to download the attachment first

        currentAttachmentViewInfo = attachment;
        FileBrowserHelper.getInstance().showFileBrowserActivity(MessageViewFragment.this, null,
                ACTIVITY_CHOOSE_DIRECTORY, new FileBrowserFailOverCallback() {
                    @Override
                    public void onPathEntered(String path) {
                        getAttachmentController(attachment).saveAttachmentTo(path);
                    }

                    @Override
                    public void onCancel() {
                        // Do nothing
                    }
                });
    }

    private AttachmentController getAttachmentController(AttachmentViewInfo attachment) {
        return new AttachmentController(mController, downloadManager, this, attachment);
    }

    private class DownloadMessageListener extends MessagingListener {
        @Override
        public void loadMessageForViewFinished(Account account, String folder, String uid, final LocalMessage message) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (isAdded()) {
                        onMessageDownloadFinished(message);
                    }
                }
            });
        }

        @Override
        public void loadMessageForViewFailed(Account account, String folder, String uid, final Throwable t) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (isAdded()) {
                        onDownloadMessageFailed(t);
                    }
                }
            });
        }
    }
}
