package com.fsck.k9.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import com.fsck.k9.*;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.helper.FileBrowserHelper;
import com.fsck.k9.helper.FileBrowserHelper.FileBrowserFailOverCallback;
import com.fsck.k9.mail.*;
import com.fsck.k9.mail.store.LocalStore.LocalMessage;
import com.fsck.k9.mail.store.StorageManager;
import com.fsck.k9.view.AttachmentView;
import com.fsck.k9.view.SingleMessageView;
import com.fsck.k9.view.AttachmentView.AttachmentFileDownloadCallback;

import java.io.File;
import java.util.*;

public class MessageView extends K9Activity implements OnClickListener {
    private static final String EXTRA_MESSAGE_REFERENCE = "com.fsck.k9.MessageView_messageReference";
    private static final String EXTRA_MESSAGE_REFERENCES = "com.fsck.k9.MessageView_messageReferences";
    private static final String EXTRA_NEXT = "com.fsck.k9.MessageView_next";
    private static final String EXTRA_MESSAGE_LIST_EXTRAS = "com.fsck.k9.MessageView_messageListExtras";
    private static final String STATE_PGP_DATA = "pgpData";
    private static final int ACTIVITY_CHOOSE_FOLDER_MOVE = 1;
    private static final int ACTIVITY_CHOOSE_FOLDER_COPY = 2;
    private static final int ACTIVITY_CHOOSE_DIRECTORY = 3;

    private SingleMessageView mMessageView;

    private PgpData mPgpData;

    private View mNext;
    private View mPrevious;
    private View mDelete;
    private View mArchive;
    private View mMove;
    private View mSpam;
    private Account mAccount;
    private MessageReference mMessageReference;
    private ArrayList<MessageReference> mMessageReferences;
    private Message mMessage;
    private static final int PREVIOUS = 1;
    private static final int NEXT = 2;
    private int mLastDirection = (K9.messageViewShowNext()) ? NEXT : PREVIOUS;
    private MessagingController mController = MessagingController.getInstance(getApplication());
    private MessageReference mNextMessage = null;
    private MessageReference mPreviousMessage = null;
    private Listener mListener = new Listener();
    private MessageViewHandler mHandler = new MessageViewHandler();
    private StorageManager.StorageListener mStorageListener = new StorageListenerImplementation();

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

    /**
     * The extras used to create the {@link MessageList} instance that created this activity. May
     * be {@code null}.
     *
     * @see MessageList#actionHandleFolder(Context, Bundle)
     */
    private Bundle mMessageListExtras;

    /**
     * Screen width in pixels.
     *
     * <p>
     * Used to detect right-to-left bezel swipes.
     * </p>
     *
     * @see #onSwipeRightToLeft(MotionEvent, MotionEvent)
     */
    private int mScreenWidthInPixels;


    private final class StorageListenerImplementation implements StorageManager.StorageListener {
        @Override
        public void onUnmount(String providerId) {
            if (!providerId.equals(mAccount.getLocalStorageProviderId())) {
                return;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onAccountUnavailable();
                }
            });
        }

        @Override
        public void onMount(String providerId) { /* no-op */ }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean ret = false;
        if (KeyEvent.ACTION_DOWN == event.getAction()) {
            ret = onCustomKeyDown(event.getKeyCode(), event);
        }
        if (!ret) {
            ret = super.dispatchKeyEvent(event);
        }
        return ret;
    }

    /**
     * Handle hotkeys
     *
     * <p>
     * This method is called by {@link #dispatchKeyEvent(KeyEvent)} before any view had the chance
     * to consume this key event.
     * </p>
     *
     * @param keyCode
     *         The value in {@code event.getKeyCode()}.
     * @param event
     *         Description of the key event.
     *
     * @return {@code true} if this event was consumed.
     */
    public boolean onCustomKeyDown(final int keyCode, final KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_VOLUME_UP: {
            if (K9.useVolumeKeysForNavigationEnabled()) {
                onNext();
                return true;
            }
            break;
        }
        case KeyEvent.KEYCODE_VOLUME_DOWN: {
            if (K9.useVolumeKeysForNavigationEnabled()) {
                onPrevious();
                return true;
            }
            break;
        }
        case KeyEvent.KEYCODE_DEL: {
            onDelete();
            return true;
        }
        case KeyEvent.KEYCODE_D: {
            onDelete();
            return true;
        }
        case KeyEvent.KEYCODE_F: {
            onForward();
            return true;
        }
        case KeyEvent.KEYCODE_A: {
            onReplyAll();
            return true;
        }
        case KeyEvent.KEYCODE_R: {
            onReply();
            return true;
        }
        case KeyEvent.KEYCODE_G: {
            onFlag();
            return true;
        }
        case KeyEvent.KEYCODE_M: {
            onMove();
            return true;
        }
        case KeyEvent.KEYCODE_S: {
            onRefile(mAccount.getSpamFolderName());
            return true;
        }
        case KeyEvent.KEYCODE_V: {
            onRefile(mAccount.getArchiveFolderName());
            return true;
        }
        case KeyEvent.KEYCODE_Y: {
            onCopy();
            return true;
        }
        case KeyEvent.KEYCODE_J:
        case KeyEvent.KEYCODE_P: {
            onPrevious();
            return true;
        }
        case KeyEvent.KEYCODE_N:
        case KeyEvent.KEYCODE_K: {
            onNext();
            return true;
        }
        case KeyEvent.KEYCODE_Z: {
            mHandler.post(new Runnable() {
                public void run() {
                    mMessageView.zoom(event);
                }
            });
            return true;
        }
        case KeyEvent.KEYCODE_H: {
            Toast toast = Toast.makeText(this, R.string.message_help_key, Toast.LENGTH_LONG);
            toast.show();
            return true;
        }
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Swallow these events too to avoid the audible notification of a volume change
        if (K9.useVolumeKeysForNavigationEnabled()) {
            if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "Swallowed key up.");
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (K9.manageBack()) {
            if (mMessageListExtras != null) {
                MessageList.actionHandleFolder(this, mMessageListExtras);
            }
            finish();
        } else {
            super.onBackPressed();
        }
    }

    class MessageViewHandler extends Handler {

        public void progress(final boolean progress) {
            runOnUiThread(new Runnable() {
                public void run() {
                    setProgressBarIndeterminateVisibility(progress);
                }
            });
        }

        public void addAttachment(final View attachmentView) {
            runOnUiThread(new Runnable() {
                public void run() {
                    mMessageView.addAttachment(attachmentView);
                }
            });
        }

        /* A helper for a set of "show a toast" methods */
        private void showToast(final String message, final int toastLength)  {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MessageView.this, message, toastLength).show();
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

    public static void actionView(Context context, MessageReference messRef,
            ArrayList<MessageReference> messReferences, Bundle messageListExtras) {
        Intent i = new Intent(context, MessageView.class);
        i.putExtra(EXTRA_MESSAGE_LIST_EXTRAS, messageListExtras);
        i.putExtra(EXTRA_MESSAGE_REFERENCE, messRef);
        i.putParcelableArrayListExtra(EXTRA_MESSAGE_REFERENCES, messReferences);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.message_view);

        mMessageView = (SingleMessageView) findViewById(R.id.message_view);

        //set a callback for the attachment view. With this callback the attachmentview
        //request the start of a filebrowser activity.
        mMessageView.setAttachmentCallback(new AttachmentFileDownloadCallback() {

            @Override
            public void showFileBrowser(final AttachmentView caller) {
                FileBrowserHelper.getInstance()
                .showFileBrowserActivity(MessageView.this,
                                         null,
                                         MessageView.ACTIVITY_CHOOSE_DIRECTORY,
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

        setTitle("");
        final Intent intent = getIntent();

        mMessageListExtras = intent.getParcelableExtra(EXTRA_MESSAGE_LIST_EXTRAS);

        Uri uri = intent.getData();
        if (icicle != null) {
            // TODO This code seems unnecessary since the icicle should already be thawed in onRestoreInstanceState().
            mMessageReference = icicle.getParcelable(EXTRA_MESSAGE_REFERENCE);
            mMessageReferences = icicle.getParcelableArrayList(EXTRA_MESSAGE_REFERENCES);
            mPgpData = (PgpData) icicle.getSerializable(STATE_PGP_DATA);
        } else {
            if (uri == null) {
                mMessageReference = intent.getParcelableExtra(EXTRA_MESSAGE_REFERENCE);
                mMessageReferences = intent.getParcelableArrayListExtra(EXTRA_MESSAGE_REFERENCES);
            } else {
                List<String> segmentList = uri.getPathSegments();
                if (segmentList.size() != 3) {
                    //TODO: Use resource to externalize message
                    Toast.makeText(this, "Invalid intent uri: " + uri.toString(), Toast.LENGTH_LONG).show();
                    return;
                }

                String accountId = segmentList.get(0);
                Collection<Account> accounts = Preferences.getPreferences(this).getAvailableAccounts();
                boolean found = false;
                for (Account account : accounts) {
                    if (String.valueOf(account.getAccountNumber()).equals(accountId)) {
                        mAccount = account;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    //TODO: Use resource to externalize message
                    Toast.makeText(this, "Invalid account id: " + accountId, Toast.LENGTH_LONG).show();
                    return;
                }

                mMessageReference = new MessageReference();
                mMessageReference.accountUuid = mAccount.getUuid();
                mMessageReference.folderName = segmentList.get(1);
                mMessageReference.uid = segmentList.get(2);
                mMessageReferences = new ArrayList<MessageReference>();
            }
        }

        mAccount = Preferences.getPreferences(this).getAccount(mMessageReference.accountUuid);


        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "MessageView got message " + mMessageReference);
        if (intent.getBooleanExtra(EXTRA_NEXT, false)) {
            mNext.requestFocus();
        }

        mScreenWidthInPixels = getResources().getDisplayMetrics().widthPixels;

        // Enable gesture detection for MessageViews
        mGestureDetector = new GestureDetector(new MyGestureDetector(false));

        setupButtonViews();
        displayMessage(mMessageReference);
    }

    private void setupButtonViews() {
        setOnClickListener(R.id.from);
        setOnClickListener(R.id.reply);
        setOnClickListener(R.id.reply_all);
        setOnClickListener(R.id.delete);
        setOnClickListener(R.id.forward);
        setOnClickListener(R.id.next);
        setOnClickListener(R.id.previous);
        setOnClickListener(R.id.archive);
        setOnClickListener(R.id.move);
        setOnClickListener(R.id.spam);
        setOnClickListener(R.id.download_remainder);


        mNext = findViewById(R.id.next);
        mPrevious = findViewById(R.id.previous);
        mDelete = findViewById(R.id.delete);

        mArchive = findViewById(R.id.archive);
        mMove = findViewById(R.id.move);
        mSpam = findViewById(R.id.spam);

        if (!mAccount.getEnableMoveButtons()) {
            View buttons = findViewById(R.id.move_buttons);
            buttons.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_MESSAGE_REFERENCE, mMessageReference);
        outState.putParcelableArrayList(EXTRA_MESSAGE_REFERENCES, mMessageReferences);
        outState.putSerializable(STATE_PGP_DATA, mPgpData);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mPgpData = (PgpData) savedInstanceState.getSerializable(STATE_PGP_DATA);
        mMessageView.updateCryptoLayout(mAccount.getCryptoProvider(), mPgpData, mMessage);
    }

    private void displayMessage(MessageReference ref) {
        mMessageReference = ref;
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "MessageView displaying message " + mMessageReference);
        mAccount = Preferences.getPreferences(this).getAccount(mMessageReference.accountUuid);
        findSurroundingMessagesUid();
        // start with fresh, empty PGP data
        mPgpData = new PgpData();

        // Clear previous message
        mMessageView.resetView();
        mMessageView.resetHeaderView();

        mController.loadMessageForView(mAccount, mMessageReference.folderName, mMessageReference.uid, mListener);
        setupDisplayMessageButtons();
    }

    private void setupDisplayMessageButtons() {
        mDelete.setEnabled(true);
        mNext.setEnabled(mNextMessage != null);
        mPrevious.setEnabled(mPreviousMessage != null);
        // If moving isn't support at all, then all of them must be disabled anyway.
        if (mController.isMoveCapable(mAccount)) {
            // Only enable the button if they have an archive folder and it's not the current folder.
            mArchive.setEnabled(!mMessageReference.folderName.equals(mAccount.getArchiveFolderName()) &&
                                mAccount.hasArchiveFolder());
            // Only enable the button if the Spam folder is not the current folder and not NONE.
            mSpam.setEnabled(!mMessageReference.folderName.equals(mAccount.getSpamFolderName()) &&
                             mAccount.hasSpamFolder());
            mMove.setEnabled(true);
        } else {
            disableMoveButtons();
        }
    }

    private void disableButtons() {
        mMessageView.setLoadPictures(false);
        disableMoveButtons();
        mNext.setEnabled(false);
        mPrevious.setEnabled(false);
        mDelete.setEnabled(false);
    }

    private void disableMoveButtons() {
        mArchive.setEnabled(false);
        mMove.setEnabled(false);
        mSpam.setEnabled(false);
    }

    private void setOnClickListener(int viewCode) {
        View thisView = findViewById(viewCode);
        if (thisView != null) {
            thisView.setOnClickListener(this);
        }
    }

    private void findSurroundingMessagesUid() {
        mNextMessage = mPreviousMessage = null;
        int i = mMessageReferences.indexOf(mMessageReference);
        if (i < 0)
            return;
        if (i != 0)
            mNextMessage = mMessageReferences.get(i - 1);
        if (i != (mMessageReferences.size() - 1))
            mPreviousMessage = mMessageReferences.get(i + 1);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mAccount.isAvailable(this)) {
            onAccountUnavailable();
            return;
        }
        StorageManager.getInstance(getApplication()).addListener(mStorageListener);
    }

    @Override
    protected void onPause() {
        StorageManager.getInstance(getApplication()).removeListener(mStorageListener);
        super.onPause();
    }

    protected void onAccountUnavailable() {
        finish();
        // TODO inform user about account unavailability using Toast
        Accounts.listAccounts(this);
    }

    /**
     * Called from UI thread when user select Delete
     */
    private void onDelete() {
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
            disableButtons();
            Message messageToDelete = mMessage;
            showNextMessageOrReturn();
            mController.deleteMessages(new Message[] {messageToDelete}, null);
        }
    }

    private void onRefile(String dstFolder) {
        if (!mController.isMoveCapable(mAccount)) {
            return;
        }
        if (!mController.isMoveCapable(mMessage)) {
            Toast toast = Toast.makeText(this, R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
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
        showNextMessageOrReturn();
        mController.moveMessage(mAccount, srcFolder, messageToMove, dstFolder, null);
    }

    private void showNextMessageOrReturn() {
        if (K9.messageViewReturnToList()) {
            finish();
        } else {
            showNextMessage();
        }
    }

    private void showNextMessage() {
        findSurroundingMessagesUid();
        mMessageReferences.remove(mMessageReference);
        if (mLastDirection == NEXT && mNextMessage != null) {
            onNext();
        } else if (mLastDirection == PREVIOUS && mPreviousMessage != null) {
            onPrevious();
        } else if (mNextMessage != null) {
            onNext();
        } else if (mPreviousMessage != null) {
            onPrevious();
        } else {
            finish();
        }
    }


    private void onReply() {
        if (mMessage != null) {
            MessageCompose.actionReply(this, mAccount, mMessage, false, mPgpData.getDecryptedData());
            finish();
        }
    }

    private void onReplyAll() {
        if (mMessage != null) {
            MessageCompose.actionReply(this, mAccount, mMessage, true, mPgpData.getDecryptedData());
            finish();
        }
    }

    private void onForward() {
        if (mMessage != null) {
            MessageCompose.actionForward(this, mAccount, mMessage, mPgpData.getDecryptedData());
            finish();
        }
    }

    private void onFlag() {
        if (mMessage != null) {
            boolean newState = !mMessage.isSet(Flag.FLAGGED);
            mController.setFlag(mAccount, mMessage.getFolder().getName(),
                    new Message[] { mMessage }, Flag.FLAGGED, newState);
            mMessageView.setHeaders(mMessage, mAccount);
        }
    }

    private void onMove() {
        if ((!mController.isMoveCapable(mAccount))
                || (mMessage == null)) {
            return;
        }
        if (!mController.isMoveCapable(mMessage)) {
            Toast toast = Toast.makeText(this, R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        startRefileActivity(ACTIVITY_CHOOSE_FOLDER_MOVE);
    }

    private void onCopy() {
        if ((!mController.isCopyCapable(mAccount))
                || (mMessage == null)) {
            return;
        }
        if (!mController.isCopyCapable(mMessage)) {
            Toast toast = Toast.makeText(this, R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        startRefileActivity(ACTIVITY_CHOOSE_FOLDER_COPY);
    }

    private void startRefileActivity(int activity) {
        Intent intent = new Intent(this, ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount.getUuid());
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, mMessageReference.folderName);
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, mAccount.getLastSelectedFolderName());
        intent.putExtra(ChooseFolder.EXTRA_MESSAGE, mMessageReference);
        startActivityForResult(intent, activity);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mAccount.getCryptoProvider().onActivityResult(this, requestCode, resultCode, data, mPgpData)) {
            return;
        }
        if (resultCode != RESULT_OK)
            return;
        switch (requestCode) {
        case ACTIVITY_CHOOSE_DIRECTORY:
            if (resultCode == RESULT_OK && data != null) {
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
        case ACTIVITY_CHOOSE_FOLDER_MOVE:
        case ACTIVITY_CHOOSE_FOLDER_COPY:
            if (data == null)
                return;
            String destFolderName = data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER);
            String srcFolderName = data.getStringExtra(ChooseFolder.EXTRA_CUR_FOLDER);
            MessageReference ref = data.getParcelableExtra(ChooseFolder.EXTRA_MESSAGE);
            if (mMessageReference.equals(ref)) {
                mAccount.setLastSelectedFolderName(destFolderName);
                switch (requestCode) {
                case ACTIVITY_CHOOSE_FOLDER_MOVE:
                    Message messageToMove = mMessage;
                    showNextMessageOrReturn();
                    mController.moveMessage(mAccount, srcFolderName, messageToMove, destFolderName, null);
                    break;
                case ACTIVITY_CHOOSE_FOLDER_COPY:
                    mController.copyMessage(mAccount, srcFolderName, mMessage, destFolderName, null);
                    break;
                }
            }
            break;
        }
    }

    private void onSendAlternate() {
        if (mMessage != null) {
            mController.sendAlternate(this, mAccount, mMessage);
        }
    }

    /**
     * Handle a right-to-left swipe starting at the edge of the screen as "move to next message."
     */
    @Override
    protected void onSwipeRightToLeft(MotionEvent e1, MotionEvent e2) {
        if ((int) e1.getRawX() > mScreenWidthInPixels - BEZEL_SWIPE_THRESHOLD) {
            onNext();
        }
    }

    /**
     * Handle a left-to-right swipe starting at the edge of the screen as
     * "move to previous message."
     */
    @Override
    protected void onSwipeLeftToRight(MotionEvent e1, MotionEvent e2) {
        if ((int) e1.getRawX() < BEZEL_SWIPE_THRESHOLD) {
            onPrevious();
        }
    }

    protected void onNext() {
        // Reset scroll percentage when we change messages
        if (mNextMessage == null) {
            Toast.makeText(this, getString(R.string.end_of_folder), Toast.LENGTH_SHORT).show();
            return;
        }
        mLastDirection = NEXT;
        disableButtons();
        if (K9.showAnimations()) {
            mMessageView.startAnimation(outToLeftAnimation());
        }
        displayMessage(mNextMessage);
        mNext.requestFocus();
    }

    protected void onPrevious() {
        // Reset scroll percentage when we change messages
        if (mPreviousMessage == null) {
            Toast.makeText(this, getString(R.string.end_of_folder), Toast.LENGTH_SHORT).show();
            return;
        }
        mLastDirection = PREVIOUS;
        disableButtons();
        if (K9.showAnimations()) {
            mMessageView.startAnimation(inFromRightAnimation());
        }
        displayMessage(mPreviousMessage);
        mPrevious.requestFocus();
    }

    private void onToggleRead() {
        if (mMessage != null) {
            mController.setFlag(mAccount, mMessage.getFolder().getName(),
                    new Message[] { mMessage }, Flag.SEEN, !mMessage.isSet(Flag.SEEN));
            mMessageView.setHeaders(mMessage, mAccount);
            String subject = mMessage.getSubject();
            setTitle(subject);
        }
    }

    private void onDownloadRemainder() {
        if (mMessage.isSet(Flag.X_DOWNLOADED_FULL)) {
            return;
        }
        mMessageView.downloadRemainderButton().setEnabled(false);
        mController.loadMessageForViewRemote(mAccount, mMessageReference.folderName, mMessageReference.uid, mListener);
    }


    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.reply:
            onReply();
            break;
        case R.id.reply_all:
            onReplyAll();
            break;
        case R.id.delete:
            onDelete();
            break;
        case R.id.forward:
            onForward();
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
        case R.id.next:
            onNext();
            break;
        case R.id.previous:
            onPrevious();
            break;
        case R.id.download:
            ((AttachmentView)view).saveFile();
            break;
        case R.id.download_remainder:
            onDownloadRemainder();
            break;
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
        case R.id.send_alternate:
            onSendAlternate();
            break;
        case R.id.mark_as_unread:
            onToggleRead();
            break;
        case R.id.flag:
            onFlag();
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
        case R.id.show_full_header:
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMessageView.showAllHeaders();
                }
            });
            break;
        case R.id.select_text:
            mMessageView.beginSelectingText();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.message_view_option, menu);
        if (!mController.isCopyCapable(mAccount)) {
            menu.findItem(R.id.copy).setVisible(false);
        }
        if (!mController.isMoveCapable(mAccount)) {
            menu.findItem(R.id.move).setVisible(false);
            menu.findItem(R.id.archive).setVisible(false);
            menu.findItem(R.id.spam).setVisible(false);
        }
        if (!mAccount.hasArchiveFolder()) {
            menu.findItem(R.id.archive).setVisible(false);
        }
        if (!mAccount.hasSpamFolder()) {
            menu.findItem(R.id.spam).setVisible(false);
        }
        return true;
    }

    // TODO: when switching to API version 8, override onCreateDialog(int, Bundle)

    /**
     * @param id The id of the dialog.
     * @return The dialog. If you return null, the dialog will not be created.
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(final int id) {
        switch (id) {
        case R.id.dialog_confirm_delete:
            return ConfirmationDialog.create(this, id,
                                             R.string.dialog_confirm_delete_title,
                                             R.string.dialog_confirm_delete_message,
                                             R.string.dialog_confirm_delete_confirm_button,
                                             R.string.dialog_confirm_delete_cancel_button,
            new Runnable() {
                @Override
                public void run() {
                    delete();
                }
            });
        case R.id.dialog_confirm_spam:
            return ConfirmationDialog.create(this, id,
                                             R.string.dialog_confirm_spam_title,
                                             getResources().getQuantityString(R.plurals.dialog_confirm_spam_message, 1),
                                             R.string.dialog_confirm_spam_confirm_button,
                                             R.string.dialog_confirm_spam_cancel_button,
            new Runnable() {
                @Override
                public void run() {
                    refileMessage(mDstFolder);
                    mDstFolder = null;
                }
            });
        case R.id.dialog_attachment_progress:
            ProgressDialog d = new ProgressDialog(this);
            d.setIndeterminate(true);
            d.setTitle(R.string.dialog_attachment_progress_title);
            return d;
        }
        return super.onCreateDialog(id);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu != null) {
            MenuItem flagItem = menu.findItem(R.id.flag);
            if (flagItem != null && mMessage != null) {
                flagItem.setTitle((mMessage.isSet(Flag.FLAGGED) ? R.string.unflag_action : R.string.flag_action));
            }
            MenuItem additionalHeadersItem = menu.findItem(R.id.show_full_header);
            if (additionalHeadersItem != null) {
                additionalHeadersItem.setTitle(mMessageView.additionalHeadersVisible() ?
                                               R.string.hide_full_header_action : R.string.show_full_header_action);
            }

            if (mMessage != null) {
                int actionTitle = mMessage.isSet(Flag.SEEN) ?
                        R.string.mark_as_unread_action : R.string.mark_as_read_action;
                menu.findItem(R.id.mark_as_unread).setTitle(actionTitle);
            }
        }
        return super.onPrepareOptionsMenu(menu);
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

            runOnUiThread(new Runnable() {
                public void run() {
                    if (!clonedMessage.isSet(Flag.X_DOWNLOADED_FULL) &&
                            !clonedMessage.isSet(Flag.X_DOWNLOADED_PARTIAL)) {
                        String text = getString(R.string.message_view_downloading);
                        mMessageView.showStatusMessage(text);
                    }
                    mMessageView.setHeaders(clonedMessage, account);
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

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mMessage = message;
                        mMessageView.setMessage(account, (LocalMessage) message, mPgpData,
                                mController, mListener);

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
                    setProgressBarIndeterminateVisibility(false);
                    if (t instanceof IllegalArgumentException) {
                        mHandler.invalidIdError();
                    } else {
                        mHandler.networkError();
                    }
                    if ((MessageView.this.mMessage == null) ||
                    !MessageView.this.mMessage.isSet(Flag.X_DOWNLOADED_PARTIAL)) {
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
                    setProgressBarIndeterminateVisibility(false);
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
                    setProgressBarIndeterminateVisibility(true);
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
}
