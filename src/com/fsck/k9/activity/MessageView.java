package com.fsck.k9.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Config;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import com.fsck.k9.*;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.*;
import com.fsck.k9.mail.store.LocalStore.LocalMessage;
import com.fsck.k9.view.AttachmentView;
import com.fsck.k9.view.ToggleScrollView;
import com.fsck.k9.view.SingleMessageView;

import java.util.*;

public class MessageView extends K9Activity implements OnClickListener {
    private static final String EXTRA_NEXT = "com.fsck.k9.MessageView_next";
    private static final String SHOW_PICTURES = "showPictures";
    private static final String STATE_PGP_DATA = "pgpData";
    private static final int ACTIVITY_CHOOSE_FOLDER_MOVE = 1;
    private static final int ACTIVITY_CHOOSE_FOLDER_COPY = 2;


    private SingleMessageView mMessageView;

    private PgpData mPgpData;


    private View mNext;
    private View mPrevious;
    private View mDelete;
    private View mArchive;
    private View mMove;
    private View mSpam;
    private ToggleScrollView mToggleScrollView;
    private Account mAccount;
    private MessageReference mMessageReference;
    private ArrayList<MessageReference> mMessageReferences;
    private Message mMessage;
    private static final int PREVIOUS = 1;
    private static final int NEXT = 2;
    private int mLastDirection = PREVIOUS;
    private MessagingController mController = MessagingController.getInstance(getApplication());
    private MessageReference mNextMessage = null;
    private MessageReference mPreviousMessage = null;
    private Listener mListener = new Listener();
    private MessageViewHandler mHandler = new MessageViewHandler();


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            // Text selection is finished. Allow scrolling again.
            mToggleScrollView.setScrolling(true);
        } else if (K9.zoomControlsEnabled()) {
            // If we have system zoom controls enabled, disable scrolling so the screen isn't wiggling around while
            // trying to zoom.
            if (ev.getAction() == MotionEvent.ACTION_POINTER_2_DOWN) {
                mToggleScrollView.setScrolling(false);
            } else if (ev.getAction() == MotionEvent.ACTION_POINTER_2_UP) {
                mToggleScrollView.setScrolling(true);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        switch (keyCode) {
        }
        return super.onKeyDown(keyCode, event);
    }


    public static void actionView(Context context, MessageReference messRef, ArrayList<MessageReference> messReferences) {
        actionView(context, messRef, messReferences, null);
    }

    public static void actionView(Context context, MessageReference messRef, ArrayList<MessageReference> messReferences, Bundle extras) {
        Intent i = new Intent(context, MessageView.class);
        i.putExtra(EXTRA_MESSAGE_REFERENCE, messRef);
        i.putParcelableArrayListExtra(EXTRA_MESSAGE_REFERENCES, messReferences);
        if (extras != null) {
            i.putExtras(extras);
        }
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle, false);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.message_view);

        mTopView = mToggleScrollView = (ToggleScrollView) findViewById(R.id.top_view);
        mMessageView = (SingleMessageView) findViewById(R.id.message_view);

        mMessageView.initialize(this);

        setTitle("");
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (icicle != null) {
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
                    //TODO: Use ressource to externalize message
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
                    //TODO: Use ressource to externalize message
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

        setupButtonViews();
        displayMessage(mMessageReference);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(STATE_PGP_DATA, mPgpData);
        outState.putBoolean(SHOW_PICTURES, mMessageView.showPictures());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mPgpData = (PgpData) savedInstanceState.getSerializable(STATE_PGP_DATA);
        mMessageView.updateCryptoLayout(mAccount.getCryptoProvider(), mPgpData, mMessage);
        mMessageView.setLoadPictures(savedInstanceState.getBoolean(SHOW_PICTURES));
    }

    private void displayMessage(MessageReference ref) {
        mMessageReference = ref;
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "MessageView displaying message " + mMessageReference);
        mAccount = Preferences.getPreferences(this).getAccount(mMessageReference.accountUuid);
        clearMessageDisplay();
        findSurroundingMessagesUid();
        // start with fresh, empty PGP data
        mPgpData = new PgpData();
        mTopView.setVisibility(View.VISIBLE);
        mController.loadMessageForView(mAccount, mMessageReference.folderName, mMessageReference.uid, mListener);
        setupDisplayMessageButtons();
    }

    private void clearMessageDisplay() {
        mTopView.setVisibility(View.GONE);
        mTopView.scrollTo(0, 0);
        mMessageView.resetView();

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
            delete();
        }
    }

    /**
     * @param id
     * @return Never <code>null</code>
     */
    protected Dialog createConfirmDeleteDialog(final int id) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_confirm_delete_title);
        builder.setMessage(R.string.dialog_confirm_delete_message);
        builder.setPositiveButton(R.string.dialog_confirm_delete_confirm_button,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissDialog(id);
                delete();
            }
        });
        builder.setNegativeButton(R.string.dialog_confirm_delete_cancel_button,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissDialog(id);
            }
        });
        return builder.create();
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
        String srcFolder = mMessageReference.folderName;
        Message messageToMove = mMessage;
        if (K9.FOLDER_NONE.equalsIgnoreCase(dstFolder)) {
            return;
        }
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


    private void onSendAlternate() {
        if (mMessage != null) {
            mController.sendAlternate(this, mAccount, mMessage);
        }
    }

    @Override
    protected void onNext() {
        if (mNextMessage == null) {
            Toast.makeText(this, getString(R.string.end_of_folder), Toast.LENGTH_SHORT).show();
            return;
        }
        mLastDirection = NEXT;
        disableButtons();
        if (K9.showAnimations()) {
            mTopView.startAnimation(outToLeftAnimation());
        }
        displayMessage(mNextMessage);
        mNext.requestFocus();
    }

    @Override
    protected void onPrevious() {
        if (mPreviousMessage == null) {
            Toast.makeText(this, getString(R.string.end_of_folder), Toast.LENGTH_SHORT).show();
            return;
        }
        mLastDirection = PREVIOUS;
        disableButtons();
        if (K9.showAnimations()) {
            mTopView.startAnimation(inFromRightAnimation());
        }
        displayMessage(mPreviousMessage);
        mPrevious.requestFocus();
    }

    private void onMarkAsUnread() {
        if (mMessage != null) {
            mController.setFlag(mAccount, mMessageReference.folderName, new String[] { mMessage.getUid() }, Flag.SEEN, false);
            try {
                mMessage.setFlag(Flag.SEEN, false);
                mMessageView.setHeaders(mMessage, mAccount);
                String subject = mMessage.getSubject();
                setTitle(subject);
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Unable to unset SEEN flag on message", e);
            }
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
        case R.id.show_pictures:
            mMessageView.setLoadPictures(true);
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
            onMarkAsUnread();
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
            mToggleScrollView.setScrolling(false);
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
        if (K9.FOLDER_NONE.equalsIgnoreCase(mAccount.getArchiveFolderName())) {
            menu.findItem(R.id.archive).setVisible(false);
        }
        if (K9.FOLDER_NONE.equalsIgnoreCase(mAccount.getSpamFolderName())) {
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
            return createConfirmDeleteDialog(id);
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
        }
        return super.onPrepareOptionsMenu(menu);
    }

    public void displayMessageBody(final Account account, final String folder, final String uid, final Message message) {
            runOnUiThread(new Runnable() {
                public void run() {
                    mTopView.scrollTo(0, 0);
                    try {
                    if (MessageView.this.mMessage != null
                            && MessageView.this.mMessage.isSet(Flag.X_DOWNLOADED_PARTIAL)
                    && message.isSet(Flag.X_DOWNLOADED_FULL)) {
                        mMessageView.setHeaders(message, account);
                    }
                    MessageView.this.mMessage = message;
                    mMessageView.displayMessageBody(account, folder, uid, message, mPgpData);
                    mMessageView.renderAttachments(mMessage, 0, mMessage, mAccount, mController, mListener);
                } catch (MessagingException e) {
                    if (Config.LOGV) {
                        Log.v(K9.LOG_TAG, "loadMessageForViewBodyAvailable", e);
                    }
                }
            }
    }

    class Listener extends MessagingListener {
        @Override
        public void loadMessageForViewHeadersAvailable(final Account account, String folder, String uid,
                final Message message) {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }
            MessageView.this.mMessage = message;
            runOnUiThread(new Runnable() {
                public void run() {
                    if (!message.isSet(Flag.X_DOWNLOADED_FULL) && !message.isSet(Flag.X_DOWNLOADED_PARTIAL)) {
                        mMessageView.loadBodyFromUrl("file:///android_asset/downloading.html");
                    }
                    mMessageView.setHeaders(message, account);
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
        public void loadMessageForViewBodyAvailable(Account account, String folder, String uid,
                Message message) {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }

            displayMessageBody(account, folder, uid, message);
        }//loadMessageForViewBodyAvailable



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
                        mMessageView.loadBodyFromUrl("file:///android_asset/empty.html");
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
        // TODO: this might not be enough if the orientation was changed while in APG,
        // sometimes shows the original encrypted content
        mMessageView.loadBodyFromText(mAccount.getCryptoProvider(), mPgpData, mMessage, mPgpData.getDecryptedData(), "text/plain");
    }

}
