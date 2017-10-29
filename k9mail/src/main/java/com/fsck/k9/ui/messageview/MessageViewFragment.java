package com.fsck.k9.ui.messageview;


import java.util.Collections;
import java.util.Locale;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.SystemClock;
import android.text.TextUtils;
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
import com.fsck.k9.activity.MessageLoaderHelper;
import com.fsck.k9.activity.MessageLoaderHelper.MessageLoaderCallbacks;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.activity.setup.OpenPgpAppSelectDialog;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.fragment.AttachmentDownloadDialogFragment;
import com.fsck.k9.fragment.ConfirmationDialogFragment;
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.fsck.k9.helper.FileBrowserHelper;
import com.fsck.k9.helper.FileBrowserHelper.FileBrowserFailOverCallback;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.ui.messageview.CryptoInfoDialog.OnClickShowCryptoKeyListener;
import com.fsck.k9.ui.messageview.MessageCryptoPresenter.MessageCryptoMvpView;
import com.fsck.k9.view.MessageCryptoDisplayStatus;
import com.fsck.k9.view.MessageHeader;
import timber.log.Timber;


public class MessageViewFragment extends Fragment implements ConfirmationDialogFragmentListener,
        AttachmentViewCallback, OnClickShowCryptoKeyListener {

    private static final String ARG_REFERENCE = "reference";

    private static final int ACTIVITY_CHOOSE_FOLDER_MOVE = 1;
    private static final int ACTIVITY_CHOOSE_FOLDER_COPY = 2;
    private static final int ACTIVITY_CHOOSE_DIRECTORY = 3;

    public static final int REQUEST_MASK_LOADER_HELPER = (1 << 8);
    public static final int REQUEST_MASK_CRYPTO_PRESENTER = (1 << 9);

    public static final int PROGRESS_THRESHOLD_MILLIS = 500 * 1000;

    public static MessageViewFragment newInstance(MessageReference reference) {
        MessageViewFragment fragment = new MessageViewFragment();

        Bundle args = new Bundle();
        args.putString(ARG_REFERENCE, reference.toIdentityString());
        fragment.setArguments(args);

        return fragment;
    }

    private MessageTopView messageView;

    private Account account;
    private MessageReference messageReference;
    private LocalMessage message;
    private MessagingController controller;
    private DownloadManager downloadManager;
    private Handler handler = new Handler();
    private MessageLoaderHelper messageLoaderHelper;
    private MessageCryptoPresenter messageCryptoPresenter;
    private Long showProgressThreshold;

    /**
     * Used to temporarily store the destination folder for refile operations if a confirmation
     * dialog is shown.
     */
    private String destinationFolder;

    private MessageViewFragmentListener fragmentListener;

    /**
     * {@code true} after {@link #onCreate(Bundle)} has been executed. This is used by
     * {@code MessageList.configureMenu()} to make sure the fragment has been initialized before
     * it is used.
     */
    private boolean initialized = false;

    private Context context;

    private AttachmentViewInfo currentAttachmentViewInfo;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        context = activity.getApplicationContext();

        try {
            fragmentListener = (MessageViewFragmentListener) activity;
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
        controller = MessagingController.getInstance(context);
        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        messageCryptoPresenter = new MessageCryptoPresenter(savedInstanceState, messageCryptoMvpView);
        messageLoaderHelper =
                new MessageLoaderHelper(context, getLoaderManager(), getFragmentManager(), messageLoaderCallbacks);
        initialized = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        messageCryptoPresenter.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        messageCryptoPresenter.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Activity activity = getActivity();
        boolean isChangingConfigurations = activity != null && activity.isChangingConfigurations();
        if (isChangingConfigurations) {
            messageLoaderHelper.onDestroyChangingConfigurations();
            return;
        }

        messageLoaderHelper.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Context context = new ContextThemeWrapper(inflater.getContext(),
                K9.getK9ThemeResourceId(K9.getK9MessageViewTheme()));
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.message, container, false);

        messageView = (MessageTopView) view.findViewById(R.id.message_view);
        messageView.setAttachmentCallback(this);
        messageView.setMessageCryptoPresenter(messageCryptoPresenter);

        messageView.setOnToggleFlagClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleFlagged();
            }
        });

        messageView.setOnDownloadButtonClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                messageView.disableDownloadButton();
                messageLoaderHelper.downloadCompleteMessage();
            }
        });

        fragmentListener.messageHeaderViewAvailable(messageView.getMessageHeaderView());

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle arguments = getArguments();
        String messageReferenceString = arguments.getString(ARG_REFERENCE);
        MessageReference messageReference = MessageReference.parse(messageReferenceString);

        displayMessage(messageReference);
    }

    private void displayMessage(MessageReference messageReference) {
        this.messageReference = messageReference;
        Timber.d("MessageView displaying message %s", this.messageReference);

        account = Preferences.getPreferences(getApplicationContext()).getAccount(this.messageReference.getAccountUuid());
        messageLoaderHelper.asyncStartOrResumeLoadingMessage(messageReference, null);

        fragmentListener.updateMenu();
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

    private void showUnableToDecodeError() {
        Context context = getActivity().getApplicationContext();
        Toast.makeText(context, R.string.message_view_toast_unable_to_display_message, Toast.LENGTH_SHORT).show();
    }

    private void showMessage(MessageViewInfo messageViewInfo) {
        hideKeyboard();

        boolean handledByCryptoPresenter = messageCryptoPresenter.maybeHandleShowMessage(
                messageView, account, messageViewInfo);
        if (!handledByCryptoPresenter) {
            messageView.showMessage(account, messageViewInfo);
            if (K9.isOpenPgpProviderConfigured()) {
                messageView.getMessageHeaderView().setCryptoStatusDisabled();
            } else {
                messageView.getMessageHeaderView().hideCryptoStatus();
            }
        }
    }

    private void displayHeaderForLoadingMessage(LocalMessage message) {
        messageView.setHeaders(message, account);
        if (K9.isOpenPgpProviderConfigured()) {
            messageView.getMessageHeaderView().setCryptoStatusLoading();
        }
        displayMessageSubject(getSubjectForMessage(message));
        fragmentListener.updateMenu();
    }

    /**
     * Called from UI thread when user select Delete
     */
    public void onDelete() {
        if (K9.confirmDelete() || (K9.confirmDeleteStarred() && message.isSet(Flag.FLAGGED))) {
            showDialog(R.id.dialog_confirm_delete);
        } else {
            delete();
        }
    }

    public void onToggleAllHeadersView() {
        messageView.getMessageHeaderView().onShowAdditionalHeaders();
    }

    public boolean allHeadersVisible() {
        return messageView.getMessageHeaderView().additionalHeadersVisible();
    }

    private void delete() {
        if (message != null) {
            // Disable the delete button after it's tapped (to try to prevent
            // accidental clicks)
            fragmentListener.disableDeleteAction();
            LocalMessage messageToDelete = message;
            fragmentListener.showNextMessageOrReturn();
            controller.deleteMessage(messageReference, null);
        }
    }

    public void onRefile(String dstFolder) {
        if (!controller.isMoveCapable(account)) {
            return;
        }
        if (!controller.isMoveCapable(messageReference)) {
            Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        if (K9.FOLDER_NONE.equalsIgnoreCase(dstFolder)) {
            return;
        }

        if (account.getSpamFolderName().equals(dstFolder) && K9.confirmSpam()) {
            this.destinationFolder = dstFolder;
            showDialog(R.id.dialog_confirm_spam);
        } else {
            refileMessage(dstFolder);
        }
    }

    private void refileMessage(String dstFolder) {
        String srcFolder = messageReference.getFolderName();
        MessageReference messageToMove = messageReference;
        fragmentListener.showNextMessageOrReturn();
        controller.moveMessage(account, srcFolder, messageToMove, dstFolder);
    }

    public void onReply() {
        if (message != null) {
            fragmentListener.onReply(message.makeMessageReference(), messageCryptoPresenter.getDecryptionResultForReply());
        }
    }

    public void onReplyAll() {
        if (message != null) {
            fragmentListener.onReplyAll(message.makeMessageReference(), messageCryptoPresenter.getDecryptionResultForReply());
        }
    }

    public void onForward() {
        if (message != null) {
            fragmentListener.onForward(message.makeMessageReference(), messageCryptoPresenter.getDecryptionResultForReply());
        }
    }

    public void onToggleFlagged() {
        if (message != null) {
            boolean newState = !message.isSet(Flag.FLAGGED);
            controller.setFlag(account, message.getFolder().getName(),
                    Collections.singletonList(message), Flag.FLAGGED, newState);
            messageView.setHeaders(message, account);
        }
    }

    public void onMove() {
        if ((!controller.isMoveCapable(account))
                || (message == null)) {
            return;
        }
        if (!controller.isMoveCapable(messageReference)) {
            Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        startRefileActivity(ACTIVITY_CHOOSE_FOLDER_MOVE);

    }

    public void onCopy() {
        if ((!controller.isCopyCapable(account))
                || (message == null)) {
            return;
        }
        if (!controller.isCopyCapable(messageReference)) {
            Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        startRefileActivity(ACTIVITY_CHOOSE_FOLDER_COPY);
    }

    public void onArchive() {
        onRefile(account.getArchiveFolderName());
    }

    public void onSpam() {
        onRefile(account.getSpamFolderName());
    }

    public void onSelectText() {
        // FIXME
        // messageView.beginSelectingText();
    }

    private void startRefileActivity(int activity) {
        Intent intent = new Intent(getActivity(), ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, account.getUuid());
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, messageReference.getFolderName());
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, account.getLastSelectedFolderName());
        intent.putExtra(ChooseFolder.EXTRA_MESSAGE, messageReference.toIdentityString());
        startActivityForResult(intent, activity);
    }

    private void startOpenPgpChooserActivity() {
        Intent i = new Intent(getActivity(), OpenPgpAppSelectDialog.class);
        startActivity(i);
    }

    public void onPendingIntentResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode & REQUEST_MASK_LOADER_HELPER) == REQUEST_MASK_LOADER_HELPER) {
            requestCode ^= REQUEST_MASK_LOADER_HELPER;
            messageLoaderHelper.onActivityResult(requestCode, resultCode, data);
            return;
        }

        if ((requestCode & REQUEST_MASK_CRYPTO_PRESENTER) == REQUEST_MASK_CRYPTO_PRESENTER) {
            requestCode ^= REQUEST_MASK_CRYPTO_PRESENTER;
            messageCryptoPresenter.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        // Note: because fragments do not have a startIntentSenderForResult method, pending intent activities are
        // launched through the MessageList activity, and delivered back via onPendingIntentResult()

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
                String messageReferenceString = data.getStringExtra(ChooseFolder.EXTRA_MESSAGE);
                MessageReference ref = MessageReference.parse(messageReferenceString);
                if (messageReference.equals(ref)) {
                    account.setLastSelectedFolderName(destFolderName);
                    switch (requestCode) {
                        case ACTIVITY_CHOOSE_FOLDER_MOVE: {
                            fragmentListener.showNextMessageOrReturn();
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
        if (message != null) {
            controller.sendAlternate(getActivity(), account, message);
        }
    }

    public void onToggleRead() {
        if (message != null) {
            controller.setFlag(account, message.getFolder().getName(),
                    Collections.singletonList(message), Flag.SEEN, !message.isSet(Flag.SEEN));
            messageView.setHeaders(message, account);
            String subject = message.getSubject();
            displayMessageSubject(subject);
            fragmentListener.updateMenu();
        }
    }

    private void setProgress(boolean enable) {
        if (fragmentListener != null) {
            fragmentListener.setProgress(enable);
        }
    }

    private void displayMessageSubject(String subject) {
        if (fragmentListener != null) {
            fragmentListener.displayMessageSubject(subject);
        }
    }

    private String getSubjectForMessage(LocalMessage message) {
        String subject = message.getSubject();
        if (TextUtils.isEmpty(subject)) {
            return context.getString(R.string.general_no_subject);
        }

        return subject;
    }

    public void moveMessage(MessageReference reference, String destFolderName) {
        controller.moveMessage(account, messageReference.getFolderName(), reference, destFolderName);
    }

    public void copyMessage(MessageReference reference, String destFolderName) {
        controller.copyMessage(account, messageReference.getFolderName(), reference, destFolderName);
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
                int size = (int) currentAttachmentViewInfo.size;
                fragment = AttachmentDownloadDialogFragment.newInstance(size, message);
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
        // messageView.zoom(event);
    }

    @Override
    public void doPositiveClick(int dialogId) {
        switch (dialogId) {
            case R.id.dialog_confirm_delete: {
                delete();
                break;
            }
            case R.id.dialog_confirm_spam: {
                refileMessage(destinationFolder);
                destinationFolder = null;
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
        return messageReference;
    }

    public boolean isMessageRead() {
        return (message != null) ? message.isSet(Flag.SEEN) : false;
    }

    public boolean isCopyCapable() {
        return controller.isCopyCapable(account);
    }

    public boolean isMoveCapable() {
        return controller.isMoveCapable(account);
    }

    public boolean canMessageBeArchived() {
        return (!messageReference.getFolderName().equals(account.getArchiveFolderName())
                && account.hasArchiveFolder());
    }

    public boolean canMessageBeMovedToSpam() {
        return (!messageReference.getFolderName().equals(account.getSpamFolderName())
                && account.hasSpamFolder());
    }

    public void updateTitle() {
        if (message != null) {
            displayMessageSubject(message.getSubject());
        }
    }

    public Context getApplicationContext() {
        return context;
    }

    public void disableAttachmentButtons(AttachmentViewInfo attachment) {
        // messageView.disableAttachmentButtons(attachment);
    }

    public void enableAttachmentButtons(AttachmentViewInfo attachment) {
        // messageView.enableAttachmentButtons(attachment);
    }

    public void runOnMainThread(Runnable runnable) {
        handler.post(runnable);
    }

    public void showAttachmentLoadingDialog() {
        // messageView.disableAttachmentButtons();
        showDialog(R.id.dialog_attachment_progress);
    }

    public void hideAttachmentLoadingDialogOnMainThread() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                removeDialog(R.id.dialog_attachment_progress);
                // messageView.enableAttachmentButtons();
            }
        });
    }

    public void refreshAttachmentThumbnail(AttachmentViewInfo attachment) {
        // messageView.refreshAttachmentThumbnail(attachment);
    }

    private MessageCryptoMvpView messageCryptoMvpView = new MessageCryptoMvpView() {
        @Override
        public void redisplayMessage() {
            messageLoaderHelper.asyncReloadMessage();
        }

        @Override
        public void startPendingIntentForCryptoPresenter(IntentSender si, Integer requestCode, Intent fillIntent,
                int flagsMask, int flagValues, int extraFlags) throws SendIntentException {
            if (requestCode == null) {
                getActivity().startIntentSender(si, fillIntent, flagsMask, flagValues, extraFlags);
                return;
            }

            requestCode |= REQUEST_MASK_CRYPTO_PRESENTER;
            getActivity().startIntentSenderForResult(
                    si, requestCode, fillIntent, flagsMask, flagValues, extraFlags);
        }

        @Override
        public void showCryptoInfoDialog(MessageCryptoDisplayStatus displayStatus, boolean hasSecurityWarning) {
            CryptoInfoDialog dialog = CryptoInfoDialog.newInstance(displayStatus, hasSecurityWarning);
            dialog.setTargetFragment(MessageViewFragment.this, 0);
            dialog.show(getFragmentManager(), "crypto_info_dialog");
        }

        @Override
        public void restartMessageCryptoProcessing() {
            messageView.setToLoadingState();
            messageLoaderHelper.asyncRestartMessageCryptoProcessing();
        }

        @Override
        public void showCryptoConfigDialog() {
            startOpenPgpChooserActivity();
        }
    };

    @Override
    public void onClickShowSecurityWarning() {
        messageCryptoPresenter.onClickShowCryptoWarningDetails();
    }

    @Override
    public void onClickShowCryptoKey() {
        messageCryptoPresenter.onClickShowCryptoKey();
    }

    public interface MessageViewFragmentListener {
        void onForward(MessageReference messageReference, Parcelable decryptionResultForReply);
        void disableDeleteAction();
        void onReplyAll(MessageReference messageReference, Parcelable decryptionResultForReply);
        void onReply(MessageReference messageReference, Parcelable decryptionResultForReply);
        void displayMessageSubject(String title);
        void setProgress(boolean b);
        void showNextMessageOrReturn();
        void messageHeaderViewAvailable(MessageHeader messageHeaderView);
        void updateMenu();
    }

    public boolean isInitialized() {
        return initialized;
    }


    private MessageLoaderCallbacks messageLoaderCallbacks = new MessageLoaderCallbacks() {
        @Override
        public void onMessageDataLoadFinished(LocalMessage message) {
            MessageViewFragment.this.message = message;

            displayHeaderForLoadingMessage(message);
            messageView.setToLoadingState();
            showProgressThreshold = null;
        }

        @Override
        public void onMessageDataLoadFailed() {
            Toast.makeText(getActivity(), R.string.status_loading_error, Toast.LENGTH_LONG).show();
            showProgressThreshold = null;
        }

        @Override
        public void onMessageViewInfoLoadFinished(MessageViewInfo messageViewInfo) {
            showMessage(messageViewInfo);
            showProgressThreshold = null;
        }

        @Override
        public void onMessageViewInfoLoadFailed(MessageViewInfo messageViewInfo) {
            showMessage(messageViewInfo);
            showProgressThreshold = null;
        }

        @Override
        public void setLoadingProgress(int current, int max) {
            if (showProgressThreshold == null) {
                showProgressThreshold = SystemClock.elapsedRealtime() + PROGRESS_THRESHOLD_MILLIS;
            } else if (showProgressThreshold == 0L || SystemClock.elapsedRealtime() > showProgressThreshold) {
                showProgressThreshold = 0L;
                messageView.setLoadingProgress(current, max);
            }
        }

        @Override
        public void onDownloadErrorMessageNotFound() {
            messageView.enableDownloadButton();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), R.string.status_invalid_id_error, Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onDownloadErrorNetworkError() {
            messageView.enableDownloadButton();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), R.string.status_network_error, Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void startIntentSenderForMessageLoaderHelper(IntentSender si, int requestCode, Intent fillIntent,
                int flagsMask, int flagValues, int extraFlags) {
            showProgressThreshold = null;
            try {
                requestCode |= REQUEST_MASK_LOADER_HELPER;
                getActivity().startIntentSenderForResult(
                        si, requestCode, fillIntent, flagsMask, flagValues, extraFlags);
            } catch (SendIntentException e) {
                Timber.e(e, "Irrecoverable error calling PendingIntent!");
            }
        }
    };


    @Override
    public void onViewAttachment(AttachmentViewInfo attachment) {
        currentAttachmentViewInfo = attachment;
        getAttachmentController(attachment).viewAttachment();
    }

    @Override
    public void onSaveAttachment(AttachmentViewInfo attachment) {
        currentAttachmentViewInfo = attachment;
        getAttachmentController(attachment).saveAttachment();
    }

    @Override
    public void onSaveAttachmentToUserProvidedDirectory(final AttachmentViewInfo attachment) {
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
        return new AttachmentController(controller, downloadManager, this, attachment);
    }
}
