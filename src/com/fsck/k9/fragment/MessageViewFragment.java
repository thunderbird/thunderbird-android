package com.fsck.k9.fragment;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.ChooseFolder;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.crypto.CryptoProvider.CryptoDecryptCallback;
import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.fsck.k9.helper.FileBrowserHelper;
import com.fsck.k9.helper.FileBrowserHelper.FileBrowserFailOverCallback;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.store.LocalStore.LocalMessage;
import com.fsck.k9.view.AttachmentView;
import com.fsck.k9.view.AttachmentView.AttachmentFileDownloadCallback;
import com.fsck.k9.view.MessageHeader;
import com.fsck.k9.view.SingleMessageView;


public class MessageViewFragment extends SherlockFragment implements OnClickListener,
        CryptoDecryptCallback, ConfirmationDialogFragmentListener {

    private static final String ARG_REFERENCE = "reference";

    private static final String STATE_MESSAGE_REFERENCE = "reference";
    private static final String STATE_PGP_DATA = "pgpData";

    private static final int ACTIVITY_CHOOSE_FOLDER_MOVE = 1;
    private static final int ACTIVITY_CHOOSE_FOLDER_COPY = 2;
    private static final int ACTIVITY_CHOOSE_DIRECTORY = 3;


    public static MessageViewFragment newInstance(MessageReference reference) {
        MessageViewFragment fragment = new MessageViewFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_REFERENCE, reference);
        fragment.setArguments(args);

        return fragment;
    }


    private SingleMessageView mMessageView;
    private PgpData mPgpData;
    private Menu mMenu;
    private Account mAccount;
    private MessageReference mMessageReference;
    private Message mMessage;
    private MessagingController mController;
    private Listener mListener = new Listener();
    private MessageViewHandler mHandler = new MessageViewHandler();

    private MenuItem mToggleMessageViewMenu;

    /** this variable is used to save the calling AttachmentView
     *  until the onActivityResult is called.
     *  => with this reference we can identity the caller
     */
    private AttachmentView attachmentTmpStore;

    /**
     * Used to temporarily store the destination folder for refile operations if a confirmation
     * dialog is shown.
     */
    private String mDstFolder;

    private MessageViewFragmentListener mFragmentListener;


    class MessageViewHandler extends Handler {

        public void progress(final boolean progress) {
            post(new Runnable() {
                public void run() {
                    setProgress(progress);
                }
            });
        }

        public void addAttachment(final View attachmentView) {
            post(new Runnable() {
                public void run() {
                    mMessageView.addAttachment(attachmentView);
                }
            });
        }

        /* A helper for a set of "show a toast" methods */
        private void showToast(final String message, final int toastLength)  {
            post(new Runnable() {
                public void run() {
                    Toast.makeText(getActivity(), message, toastLength).show();
                }
            });
        }

        public void networkError() {
            showToast(getString(R.string.status_network_error), Toast.LENGTH_LONG);
        }

        public void invalidIdError() {
            showToast(getString(R.string.status_invalid_id_error), Toast.LENGTH_LONG);
        }


        public void fetchingAttachment() {
            showToast(getString(R.string.message_view_fetching_attachment_toast), Toast.LENGTH_SHORT);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

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

        mController = MessagingController.getInstance(getActivity().getApplication());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.message, container, false);


        mMessageView = (SingleMessageView) view.findViewById(R.id.message_view);

        //set a callback for the attachment view. With this callback the attachmentview
        //request the start of a filebrowser activity.
        mMessageView.setAttachmentCallback(new AttachmentFileDownloadCallback() {

            @Override
            public void showFileBrowser(final AttachmentView caller) {
                FileBrowserHelper.getInstance()
                .showFileBrowserActivity(MessageViewFragment.this,
                                         null,
                                         ACTIVITY_CHOOSE_DIRECTORY,
                                         callback);
                attachmentTmpStore = caller;
            }

            FileBrowserFailOverCallback callback = new FileBrowserFailOverCallback() {

                @Override
                public void onPathEntered(String path) {
                    attachmentTmpStore.writeFile(new File(path));
                }

                @Override
                public void onCancel() {
                    // canceled, do nothing
                }
            };
        });

        mMessageView.initialize(this);
        mMessageView.downloadRemainderButton().setOnClickListener(this);

        mFragmentListener.messageHeaderViewAvailable(mMessageView.getMessageHeaderView());

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        MessageReference messageReference;
        if (savedInstanceState != null) {
            mPgpData = (PgpData) savedInstanceState.get(STATE_PGP_DATA);
            messageReference = (MessageReference) savedInstanceState.get(STATE_MESSAGE_REFERENCE);
        } else {
            Bundle args = getArguments();
            messageReference = (MessageReference) args.getParcelable(ARG_REFERENCE);
        }

        displayMessage(messageReference, (mPgpData == null));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_MESSAGE_REFERENCE, mMessageReference);
        outState.putSerializable(STATE_PGP_DATA, mPgpData);
    }

    public void displayMessage(MessageReference ref) {
        displayMessage(ref, true);
    }

    private void displayMessage(MessageReference ref, boolean resetPgpData) {
        mMessageReference = ref;
        if (K9.DEBUG) {
            Log.d(K9.LOG_TAG, "MessageView displaying message " + mMessageReference);
        }

        Context appContext = getActivity().getApplicationContext();
        mAccount = Preferences.getPreferences(appContext).getAccount(mMessageReference.accountUuid);

        if (resetPgpData) {
            // start with fresh, empty PGP data
            mPgpData = new PgpData();
        }

        // Clear previous message
        mMessageView.resetView();
        mMessageView.resetHeaderView();

        mController.loadMessageForView(mAccount, mMessageReference.folderName, mMessageReference.uid, mListener);
        configureMenu(mMenu);
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

    private void delete() {
        if (mMessage != null) {
            // Disable the delete button after it's tapped (to try to prevent
            // accidental clicks)
            mMenu.findItem(R.id.delete).setEnabled(false);
            Message messageToDelete = mMessage;
            mFragmentListener.showNextMessageOrReturn();
            mController.deleteMessages(new Message[] {messageToDelete}, null);
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
        String srcFolder = mMessageReference.folderName;
        Message messageToMove = mMessage;
        mFragmentListener.showNextMessageOrReturn();
        mController.moveMessage(mAccount, srcFolder, messageToMove, dstFolder, null);
    }

    public void onReply() {
        if (mMessage != null) {
            mFragmentListener.onReply(mMessage, mPgpData);
        }
    }

    public void onReplyAll() {
        if (mMessage != null) {
            mFragmentListener.onReplyAll(mMessage, mPgpData);
        }
    }

    public void onForward() {
        if (mMessage != null) {
            mFragmentListener.onForward(mMessage, mPgpData);
        }
    }

    public void onFlag() {
        if (mMessage != null) {
            boolean newState = !mMessage.isSet(Flag.FLAGGED);
            mController.setFlag(mAccount, mMessage.getFolder().getName(),
                    new Message[] { mMessage }, Flag.FLAGGED, newState);
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

    private void onToggleColors() {
        if (K9.getK9MessageViewTheme() == K9.THEME_DARK) {
            K9.setK9MessageViewTheme(K9.THEME_LIGHT);
        } else {
            K9.setK9MessageViewTheme(K9.THEME_DARK);
        }

        new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(Object... params) {
                Context appContext = getActivity().getApplicationContext();
                Preferences prefs = Preferences.getPreferences(appContext);
                Editor editor = prefs.getPreferences().edit();
                K9.save(editor);
                editor.commit();
                return null;
            }
        }.execute();

        mFragmentListener.restartActivity();
    }

    private void startRefileActivity(int activity) {
        Intent intent = new Intent(getActivity(), ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount.getUuid());
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, mMessageReference.folderName);
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, mAccount.getLastSelectedFolderName());
        intent.putExtra(ChooseFolder.EXTRA_MESSAGE, mMessageReference);
        startActivityForResult(intent, activity);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mAccount.getCryptoProvider().onDecryptActivityResult(this, requestCode, resultCode, data, mPgpData)) {
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case ACTIVITY_CHOOSE_DIRECTORY: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // obtain the filename
                    Uri fileUri = data.getData();
                    if (fileUri != null) {
                        String filePath = fileUri.getPath();
                        if (filePath != null) {
                            attachmentTmpStore.writeFile(new File(filePath));
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

    private void onSendAlternate() {
        if (mMessage != null) {
            mController.sendAlternate(getActivity(), mAccount, mMessage);
        }
    }

    private void onToggleRead() {
        if (mMessage != null) {
            mController.setFlag(mAccount, mMessage.getFolder().getName(),
                    new Message[] { mMessage }, Flag.SEEN, !mMessage.isSet(Flag.SEEN));
            mMessageView.setHeaders(mMessage, mAccount);
            String subject = mMessage.getSubject();
            displayMessageSubject(subject);
            updateUnreadToggleTitle();
        }
    }

    private void onDownloadRemainder() {
        if (mMessage.isSet(Flag.X_DOWNLOADED_FULL)) {
            return;
        }
        mMessageView.downloadRemainderButton().setEnabled(false);
        mController.loadMessageForViewRemote(mAccount, mMessageReference.folderName, mMessageReference.uid, mListener);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.download: {
                ((AttachmentView)view).saveFile();
                break;
            }
            case R.id.download_remainder: {
                onDownloadRemainder();
                break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.delete:
            onDelete();
            break;
        case R.id.reply:
            onReply();
            break;
        case R.id.reply_all:
            onReplyAll();
            break;
        case R.id.forward:
            onForward();
            break;
        case R.id.share:
            onSendAlternate();
            break;
        case R.id.toggle_unread:
            onToggleRead();
            break;
        case R.id.archive:
            onRefile(mAccount.getArchiveFolderName());
            break;
        case R.id.spam:
            onRefile(mAccount.getSpamFolderName());
            break;
        case R.id.move:
            onMove();
            break;
        case R.id.copy:
            onCopy();
            break;
        case R.id.select_text:
            mMessageView.beginSelectingText();
            break;
        case R.id.toggle_message_view_theme:
            onToggleColors();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.message_view_fragment, menu);
        mMenu = menu;
    }

    private void configureMenu(Menu menu) {
        // first run displayMessage() gets called before onCreateOptionMenu()
        if (menu == null) {
            return;
        }

        // enable them all
        menu.findItem(R.id.copy).setVisible(true);
        menu.findItem(R.id.move).setVisible(true);
        menu.findItem(R.id.archive).setVisible(true);
        menu.findItem(R.id.spam).setVisible(true);

        mToggleMessageViewMenu = menu.findItem(R.id.toggle_message_view_theme);
        if (K9.getK9MessageViewTheme() == K9.THEME_DARK) {
            mToggleMessageViewMenu.setTitle(R.string.message_view_theme_action_light);
        } else {
            mToggleMessageViewMenu.setTitle(R.string.message_view_theme_action_dark);
        }

        toggleActionsState(menu, true);

        updateUnreadToggleTitle();

        // comply with the setting
        if (!mAccount.getEnableMoveButtons()) {
            menu.findItem(R.id.move).setVisible(false);
            menu.findItem(R.id.archive).setVisible(false);
            menu.findItem(R.id.spam).setVisible(false);
        } else {
            // check message, folder capability
            if (!mController.isCopyCapable(mAccount)) {
                menu.findItem(R.id.copy).setVisible(false);
            }

            if (mController.isMoveCapable(mAccount)) {
                menu.findItem(R.id.move).setVisible(true);

                menu.findItem(R.id.archive).setVisible(
                    !mMessageReference.folderName.equals(mAccount.getArchiveFolderName())
                        && mAccount.hasArchiveFolder());

                menu.findItem(R.id.spam).setVisible(
                    !mMessageReference.folderName.equals(mAccount.getSpamFolderName())
                        && mAccount.hasSpamFolder());
            } else {
                menu.findItem(R.id.copy).setVisible(false);
                menu.findItem(R.id.move).setVisible(false);
                menu.findItem(R.id.archive).setVisible(false);
                menu.findItem(R.id.spam).setVisible(false);
            }
        }
    }

    /**
     * Set the title of the "Toggle Unread" menu item based upon the current read state of the message.
     */
    public void updateUnreadToggleTitle() {
        if (mMessage != null && mMenu != null) {
            if (mMessage.isSet(Flag.SEEN)) {
                mMenu.findItem(R.id.toggle_unread).setTitle(R.string.mark_as_unread_action);
            } else {
                mMenu.findItem(R.id.toggle_unread).setTitle(R.string.mark_as_read_action);
            }
        }
    }

    private void toggleActionsState(Menu menu, boolean state) {
        for (int i = 0; i < menu.size(); ++i) {
            menu.getItem(i).setEnabled(state);
        }
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

    public void moveMessage(MessageReference reference, String destFolderName) {
        mController.moveMessage(mAccount, mMessageReference.folderName, mMessage,
                destFolderName, null);
    }

    public void copyMessage(MessageReference reference, String destFolderName) {
        mController.copyMessage(mAccount, mMessageReference.folderName, mMessage,
                destFolderName, null);
    }

    class Listener extends MessagingListener {
        @Override
        public void loadMessageForViewHeadersAvailable(final Account account, String folder, String uid,
                final Message message) {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }

            /*
             * Clone the message object because the original could be modified by
             * MessagingController later. This could lead to a ConcurrentModificationException
             * when that same object is accessed by the UI thread (below).
             *
             * See issue 3953
             *
             * This is just an ugly hack to get rid of the most pressing problem. A proper way to
             * fix this is to make Message thread-safe. Or, even better, rewriting the UI code to
             * access messages via a ContentProvider.
             *
             */
            final Message clonedMessage = message.clone();

            mHandler.post(new Runnable() {
                public void run() {
                    if (!clonedMessage.isSet(Flag.X_DOWNLOADED_FULL) &&
                            !clonedMessage.isSet(Flag.X_DOWNLOADED_PARTIAL)) {
                        String text = getString(R.string.message_view_downloading);
                        mMessageView.showStatusMessage(text);
                    }
                    mMessageView.setHeaders(clonedMessage, account);
                    final String subject = clonedMessage.getSubject();
                    if (subject == null || subject.equals("")) {
                        displayMessageSubject(getString(R.string.general_no_subject));
                    } else {
                        displayMessageSubject(clonedMessage.getSubject());
                    }
                    mMessageView.setOnFlagListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onFlag();
                        }
                    });
                }
            });
        }

        @Override
        public void loadMessageForViewBodyAvailable(final Account account, String folder,
                String uid, final Message message) {
            if (!mMessageReference.uid.equals(uid) ||
                    !mMessageReference.folderName.equals(folder) ||
                    !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        mMessage = message;
                        mMessageView.setMessage(account, (LocalMessage) message, mPgpData,
                                mController, mListener);
                        updateUnreadToggleTitle();

                    } catch (MessagingException e) {
                        Log.v(K9.LOG_TAG, "loadMessageForViewBodyAvailable", e);
                    }
                }
            });
        }

        @Override
        public void loadMessageForViewFailed(Account account, String folder, String uid, final Throwable t) {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }
            mHandler.post(new Runnable() {
                public void run() {
                    setProgress(false);
                    if (t instanceof IllegalArgumentException) {
                        mHandler.invalidIdError();
                    } else {
                        mHandler.networkError();
                    }
                    if (mMessage == null || mMessage.isSet(Flag.X_DOWNLOADED_PARTIAL)) {
                        mMessageView.showStatusMessage(getString(R.string.webview_empty_message));
                    }
                }
            });
        }

        @Override
        public void loadMessageForViewFinished(Account account, String folder, String uid, final Message message) {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }
            mHandler.post(new Runnable() {
                public void run() {
                    setProgress(false);
                    mMessageView.setShowDownloadButton(message);
                }
            });
        }

        @Override
        public void loadMessageForViewStarted(Account account, String folder, String uid) {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }
            mHandler.post(new Runnable() {
                public void run() {
                    setProgress(true);
                }
            });
        }

        @Override
        public void loadAttachmentStarted(Account account, Message message, Part part, Object tag, final boolean requiresDownload) {
            if (mMessage != message) {
                return;
            }
            mHandler.post(new Runnable() {
                public void run() {
                    mMessageView.setAttachmentsEnabled(false);
                    showDialog(R.id.dialog_attachment_progress);
                    if (requiresDownload) {
                        mHandler.fetchingAttachment();
                    }
                }
            });
        }

        @Override
        public void loadAttachmentFinished(Account account, Message message, Part part, final Object tag) {
            if (mMessage != message) {
                return;
            }
            mHandler.post(new Runnable() {
                public void run() {
                    mMessageView.setAttachmentsEnabled(true);
                    removeDialog(R.id.dialog_attachment_progress);
                    Object[] params = (Object[]) tag;
                    boolean download = (Boolean) params[0];
                    AttachmentView attachment = (AttachmentView) params[1];
                    if (download) {
                        attachment.writeFile();
                    } else {
                        attachment.showFile();
                    }
                }
            });
        }

        @Override
        public void loadAttachmentFailed(Account account, Message message, Part part, Object tag, String reason) {
            if (mMessage != message) {
                return;
            }
            mHandler.post(new Runnable() {
                public void run() {
                    mMessageView.setAttachmentsEnabled(true);
                    removeDialog(R.id.dialog_attachment_progress);
                    mHandler.networkError();
                }
            });
        }
    }

    // This REALLY should be in MessageCryptoView
    @Override
    public void onDecryptDone(PgpData pgpData) {
        Account account = mAccount;
        LocalMessage message = (LocalMessage) mMessage;
        MessagingController controller = mController;
        Listener listener = mListener;
        try {
            mMessageView.setMessage(account, message, pgpData, controller, listener);
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "displayMessageBody failed", e);
        }
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
                String title = getString(R.string.dialog_attachment_progress_title);
                fragment = ProgressDialogFragment.newInstance(title);
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
        return String.format("dialog-%d", dialogId);
    }

    public void zoom(KeyEvent event) {
        mMessageView.zoom(event);
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


    public interface MessageViewFragmentListener {
        public void onForward(Message mMessage, PgpData mPgpData);
        public void onReplyAll(Message mMessage, PgpData mPgpData);
        public void onReply(Message mMessage, PgpData mPgpData);
        public void displayMessageSubject(String title);
        public void setProgress(boolean b);
        public void restartActivity();
        public void showNextMessageOrReturn();
        public void messageHeaderViewAvailable(MessageHeader messageHeaderView);
    }
}
