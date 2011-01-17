package com.fsck.k9.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Config;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import com.fsck.k9.*;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.crypto.CryptoProvider;
import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.*;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.store.LocalStore.LocalAttachmentBodyPart;
import com.fsck.k9.mail.store.LocalStore.LocalMessage;
import com.fsck.k9.mail.store.StorageManager;
import com.fsck.k9.view.AccessibleWebView;
import com.fsck.k9.view.AttachmentView;
import com.fsck.k9.view.MessageWebView;
import com.fsck.k9.view.ToggleScrollView;
import com.fsck.k9.view.MessageHeader;

import java.io.Serializable;
import java.util.*;

public class MessageView extends K9Activity implements OnClickListener
{
    private static final String EXTRA_MESSAGE_REFERENCE = "com.fsck.k9.MessageView_messageReference";
    private static final String EXTRA_MESSAGE_REFERENCES = "com.fsck.k9.MessageView_messageReferences";
    private static final String EXTRA_NEXT = "com.fsck.k9.MessageView_next";
    private static final String SHOW_PICTURES = "showPictures";
    private static final String STATE_PGP_DATA = "pgpData";
    private static final int ACTIVITY_CHOOSE_FOLDER_MOVE = 1;
    private static final int ACTIVITY_CHOOSE_FOLDER_COPY = 2;
    private View mDecryptLayout;
    private Button mDecryptButton;
    private LinearLayout mCryptoSignatureLayout = null;
    private ImageView mCryptoSignatureStatusImage = null;
    private TextView mCryptoSignatureUserId = null;
    private TextView mCryptoSignatureUserIdRest = null;
    private MessageWebView mMessageContentView;
    private boolean mScreenReaderEnabled;
    private AccessibleWebView mAccessibleMessageContentView;
    private MessageHeader mHeaderContainer;
    private LinearLayout        mAttachments;
    private View mShowPicturesSection;
    private boolean mShowPictures;
    private Button mDownloadRemainder;
    View next;
    View previous;
    private View mDelete;
    private View mArchive;
    private View mMove;
    private View mSpam;
    private ToggleScrollView mToggleScrollView;
    private Account mAccount;
    private MessageReference mMessageReference;
    private ArrayList<MessageReference> mMessageReferences;
    private Message mMessage;
    private PgpData mPgpData = null;
    private static final int PREVIOUS = 1;
    private static final int NEXT = 2;
    private int mLastDirection = PREVIOUS;
    private MessagingController mController = MessagingController.getInstance(getApplication());
    private MessageReference mNextMessage = null;
    private MessageReference mPreviousMessage = null;
    private Menu optionsMenu = null;
    private Listener mListener = new Listener();
    private MessageViewHandler mHandler = new MessageViewHandler();
    private FontSizes mFontSizes = K9.getFontSizes();
    private Contacts mContacts;
    private StorageManager.StorageListener mStorageListener = new StorageListenerImplementation();

    private final class StorageListenerImplementation implements StorageManager.StorageListener
    {
        @Override
        public void onUnmount(String providerId)
        {
            if (!providerId.equals(mAccount.getLocalStorageProviderId()))
            {
                return;
            }
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    onAccountUnavailable();
                }
            });
        }

        @Override
        public void onMount(String providerId) {} // no-op
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev)
    {
        if (ev.getAction() == MotionEvent.ACTION_UP)
        {
            // Text selection is finished. Allow scrolling again.
            mToggleScrollView.setScrolling(true);
        }
        else if (K9.zoomControlsEnabled())
        {
            // If we have system zoom controls enabled, disable scrolling so the screen isn't wiggling around while
            // trying to zoom.
            if (ev.getAction() == MotionEvent.ACTION_POINTER_2_DOWN)
            {
                mToggleScrollView.setScrolling(false);
            }
            else if (ev.getAction() == MotionEvent.ACTION_POINTER_2_UP)
            {
                mToggleScrollView.setScrolling(true);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        boolean ret = false;
        if (KeyEvent.ACTION_DOWN == event.getAction())
        {
            ret = onKeyDown(event.getKeyCode(), event);
        }
        if (!ret)
        {
            ret = super.dispatchKeyEvent(event);
        }
        return ret;
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event)
    {
        switch (keyCode)
        {
            case KeyEvent.KEYCODE_VOLUME_UP:
            {
                if (K9.useVolumeKeysForNavigationEnabled())
                {
                    onNext();
                    return true;
                }
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            {
                if (K9.useVolumeKeysForNavigationEnabled())
                {
                    onPrevious();
                    return true;
                }
            }
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
            {
                /*
                 * Selecting text started via shift key. Disable scrolling as
                 * this causes problems when selecting text.
                 */
                mToggleScrollView.setScrolling(false);
                break;
            }
            case KeyEvent.KEYCODE_DEL:
            {
                onDelete();
                return true;
            }
            case KeyEvent.KEYCODE_D:
            {
                onDelete();
                return true;
            }
            case KeyEvent.KEYCODE_F:
            {
                onForward();
                return true;
            }
            case KeyEvent.KEYCODE_A:
            {
                onReplyAll();
                return true;
            }
            case KeyEvent.KEYCODE_R:
            {
                onReply();
                return true;
            }
            case KeyEvent.KEYCODE_G:
            {
                onFlag();
                return true;
            }
            case KeyEvent.KEYCODE_M:
            {
                onMove();
                return true;
            }
            case KeyEvent.KEYCODE_S:
            {
                onRefile(mAccount.getSpamFolderName());
                return true;
            }
            case KeyEvent.KEYCODE_V:
            {
                onRefile(mAccount.getArchiveFolderName());
                return true;
            }
            case KeyEvent.KEYCODE_Y:
            {
                onCopy();
                return true;
            }
            case KeyEvent.KEYCODE_J:
            case KeyEvent.KEYCODE_P:
            {
                onPrevious();
                return true;
            }
            case KeyEvent.KEYCODE_N:
            case KeyEvent.KEYCODE_K:
            {
                onNext();
                return true;
            }
            case KeyEvent.KEYCODE_Z:
            {
                mHandler.post(new Runnable()
                {
                    public void run()
                    {
                        if (mScreenReaderEnabled)
                        {
                            mAccessibleMessageContentView.zoomIn();
                        }
                        else
                        {
                            if (event.isShiftPressed())
                            {
                                mMessageContentView.zoomIn();
                            }
                            else
                            {
                                mMessageContentView.zoomOut();
                            }
                        }
                    }
                });
                return true;
            }
            case KeyEvent.KEYCODE_H:
            {
                Toast toast = Toast.makeText(this, R.string.message_help_key, Toast.LENGTH_LONG);
                toast.show();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        // Swallow these events too to avoid the audible notification of a volume change
        if (K9.useVolumeKeysForNavigationEnabled())
        {
            if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN))
            {
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "Swallowed key up.");
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    class MessageViewHandler extends Handler
    {
        public void setHeaders (final Message message)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        mHeaderContainer.populate( message,mAccount);
                        mHeaderContainer.setOnFlagListener( new OnClickListener()
                        {
                            @Override public void onClick(View v)
                            {
                                if (mMessage != null)
                                {
                                    onFlag();
                                }
                            }
                        });
                    }
                    catch (Exception me)
                    {
                        Log.e(K9.LOG_TAG, "setHeaders - error", me);
                    }
                    if (mMessage.isSet(Flag.X_DOWNLOADED_FULL))
                    {
                        mDownloadRemainder.setVisibility(View.GONE);
                    }
                    else
                    {
                        mDownloadRemainder.setEnabled(true);
                        mDownloadRemainder.setVisibility(View.VISIBLE);
                    }
                }
            });

        }

        public void progress(final boolean progress)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    setProgressBarIndeterminateVisibility(progress);
                }
            });
        }

        public void addAttachment(final View attachmentView)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    mAttachments.addView(attachmentView);
                    mAttachments.setVisibility(View.VISIBLE);
                }
            });
        }

        public void removeAllAttachments()
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    for (int i = 0, count = mAttachments.getChildCount(); i < count; i++)
                    {
                        mAttachments.removeView(mAttachments.getChildAt(i));
                    }
                }
            });
        }

        public void setAttachmentsEnabled(final boolean enabled)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    for (int i = 0, count = mAttachments.getChildCount(); i < count; i++)
                    {
                        AttachmentView attachment = (AttachmentView) mAttachments.getChildAt(i);
                        attachment.viewButton.setEnabled(enabled);
                        attachment.downloadButton.setEnabled(enabled);

                        if (enabled) {
                        	attachment.checkViewable();
                        }
                    }
                }
            });
        }



        public void networkError()
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    Toast.makeText(MessageView.this,
                                   R.string.status_network_error, Toast.LENGTH_LONG).show();
                }
            });
        }

        public void invalidIdError()
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    Toast.makeText(MessageView.this,
                                   R.string.status_invalid_id_error, Toast.LENGTH_LONG).show();
                }
            });
        }


        public void fetchingAttachment()
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    Toast.makeText(MessageView.this,
                                   getString(R.string.message_view_fetching_attachment_toast),
                                   Toast.LENGTH_SHORT).show();
                }
            });
        }

        public void showShowPictures(final boolean show)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    mShowPicturesSection.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        }

    }

    public static void actionView(Context context, MessageReference messRef, List<MessageReference> messReferences)
    {
        actionView(context, messRef, messReferences, null);
    }

    public static void actionView(Context context, MessageReference messRef, List<MessageReference> messReferences, Bundle extras)
    {
        Intent i = new Intent(context, MessageView.class);
        i.putExtra(EXTRA_MESSAGE_REFERENCE, messRef);
        i.putExtra(EXTRA_MESSAGE_REFERENCES, (Serializable) messReferences);
        if (extras != null)
        {
            i.putExtras(extras);
        }
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle, false);
        mContacts = Contacts.getInstance(this);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.message_view);
        mMessageContentView = (MessageWebView) findViewById(R.id.message_content);
        mAccessibleMessageContentView = (AccessibleWebView) findViewById(R.id.accessible_message_content);
        mAttachments = (LinearLayout) findViewById(R.id.attachments);

        mHeaderContainer = (MessageHeader) findViewById(R.id.header_container);

        mScreenReaderEnabled = isScreenReaderActive();
        if (mScreenReaderEnabled)
        {
            mAccessibleMessageContentView.setVisibility(View.VISIBLE);
            mMessageContentView.setVisibility(View.GONE);
        }
        else
        {
            mAccessibleMessageContentView.setVisibility(View.GONE);
            mMessageContentView.setVisibility(View.VISIBLE);
        }

        setupDecryptLayout();

        setTitle("");
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (icicle != null)
        {
            mMessageReference = (MessageReference) icicle.getSerializable(EXTRA_MESSAGE_REFERENCE);
            mMessageReferences = (ArrayList<MessageReference>) icicle.getSerializable(EXTRA_MESSAGE_REFERENCES);
            mPgpData = (PgpData) icicle.getSerializable(STATE_PGP_DATA);
            updateDecryptLayout();
        }
        else
        {
            if (uri == null)
            {
                mMessageReference = (MessageReference) intent.getSerializableExtra(EXTRA_MESSAGE_REFERENCE);
                mMessageReferences = (ArrayList<MessageReference>) intent.getSerializableExtra(EXTRA_MESSAGE_REFERENCES);
            }
            else
            {
                List<String> segmentList = uri.getPathSegments();
                if (segmentList.size() != 3)
                {
                    //TODO: Use ressource to externalize message
                    Toast.makeText(this, "Invalid intent uri: " + uri.toString(), Toast.LENGTH_LONG).show();
                    return;
                }

                String accountId = segmentList.get(0);
                Collection<Account> accounts = Preferences.getPreferences(this).getAvailableAccounts();
                boolean found = false;
                for (Account account : accounts)
                {
                    if (String.valueOf(account.getAccountNumber()).equals(accountId))
                    {
                        mAccount = account;
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
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
        if (intent.getBooleanExtra(EXTRA_NEXT, false))
        {
            next.requestFocus();
        }

        setupHeaderLayout();
        setupButtonViews();

        displayMessage(mMessageReference);
    }

    private void setupButtonViews()
    {
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
        // To show full header
        setOnClickListener(R.id.header_container);
        setOnClickListener(R.id.reply_scrolling);
//       setOnClickListener(R.id.reply_all_scrolling);
        setOnClickListener(R.id.delete_scrolling);
        setOnClickListener(R.id.forward_scrolling);
        setOnClickListener(R.id.next_scrolling);
        setOnClickListener(R.id.previous_scrolling);
        setOnClickListener(R.id.archive_scrolling);
        setOnClickListener(R.id.move_scrolling);
        setOnClickListener(R.id.spam_scrolling);
        setOnClickListener(R.id.show_pictures);
        setOnClickListener(R.id.download_remainder);


        // Perhaps the ScrollButtons should be global, instead of account-specific
        Account.ScrollButtons scrollButtons = mAccount.getScrollMessageViewButtons();
        if
        ((Account.ScrollButtons.ALWAYS == scrollButtons)
                ||
                (Account.ScrollButtons.KEYBOARD_AVAILABLE == scrollButtons &&
                 (this.getResources().getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO)))
        {
            scrollButtons();
        }
        else      // never or the keyboard is open
        {
            staticButtons();
        }
        Account.ScrollButtons scrollMoveButtons = mAccount.getScrollMessageViewMoveButtons();
        if ((Account.ScrollButtons.ALWAYS == scrollMoveButtons)
                || (Account.ScrollButtons.KEYBOARD_AVAILABLE == scrollMoveButtons &&
                    (this.getResources().getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO)))
        {
            scrollMoveButtons();
        }
        else
        {
            staticMoveButtons();
        }
        if (!mAccount.getEnableMoveButtons())
        {
            View buttons = findViewById(R.id.move_buttons);
            if (buttons != null)
            {
                buttons.setVisibility(View.GONE);
            }
            buttons = findViewById(R.id.scrolling_move_buttons);
            if (buttons != null)
            {
                buttons.setVisibility(View.GONE);
            }
        }



    }

    private void setupHeaderLayout()
    {
        mShowPicturesSection = findViewById(R.id.show_pictures_section);
        mShowPictures = false;

        mDownloadRemainder = (Button) findViewById(R.id.download_remainder);
        mMessageContentView.configure();

        mTopView = mToggleScrollView = (ToggleScrollView) findViewById(R.id.top_view);

        mAttachments.setVisibility(View.GONE);

    }


    private void setupDecryptLayout()
    {
        mDecryptLayout = (View) findViewById(R.id.layout_decrypt);
        mDecryptButton = (Button) findViewById(R.id.btn_decrypt);
        mDecryptButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                try
                {
                    String data = null;
                    Part part = MimeUtility.findFirstPartByMimeType(mMessage, "text/plain");
                    if (part == null)
                    {
                        part = MimeUtility.findFirstPartByMimeType(mMessage, "text/html");
                    }
                    if (part != null)
                    {
                        data = MimeUtility.getTextFromPart(part);
                    }
                    mAccount.getCryptoProvider().decrypt(MessageView.this, data, mPgpData);
                }
                catch (MessagingException me)
                {
                    Log.e(K9.LOG_TAG, "Unable to decrypt email.", me);
                }
            }
        });
        mCryptoSignatureLayout = (LinearLayout) findViewById(R.id.crypto_signature);
        mCryptoSignatureStatusImage = (ImageView) findViewById(R.id.ic_crypto_signature_status);
        mCryptoSignatureUserId = (TextView) findViewById(R.id.userId);
        mCryptoSignatureUserIdRest = (TextView) findViewById(R.id.userIdRest);
        mCryptoSignatureLayout.setVisibility(View.INVISIBLE);
    }

    private boolean isScreenReaderActive()
    {
        final String SCREENREADER_INTENT_ACTION = "android.accessibilityservice.AccessibilityService";
        final String SCREENREADER_INTENT_CATEGORY = "android.accessibilityservice.category.FEEDBACK_SPOKEN";
        // Restrict the set of intents to only accessibility services that have
        // the category FEEDBACK_SPOKEN (aka, screen readers).
        Intent screenReaderIntent = new Intent(SCREENREADER_INTENT_ACTION);
        screenReaderIntent.addCategory(SCREENREADER_INTENT_CATEGORY);
        List<ResolveInfo> screenReaders = getPackageManager().queryIntentServices(
                                              screenReaderIntent, 0);
        ContentResolver cr = getContentResolver();
        Cursor cursor = null;
        int status = 0;
        for (ResolveInfo screenReader : screenReaders)
        {
            // All screen readers are expected to implement a content provider
            // that responds to
            // content://<nameofpackage>.providers.StatusProvider
            cursor = cr.query(Uri.parse("content://" + screenReader.serviceInfo.packageName
                                        + ".providers.StatusProvider"), null, null, null, null);
            if (cursor != null)
            {
                cursor.moveToFirst();
                // These content providers use a special cursor that only has
                // one element,
                // an integer that is 1 if the screen reader is running.
                status = cursor.getInt(0);
                cursor.close();
                if (status == 1)
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        outState.putSerializable(EXTRA_MESSAGE_REFERENCE, mMessageReference);
        outState.putSerializable(EXTRA_MESSAGE_REFERENCES, mMessageReferences);
        outState.putSerializable(STATE_PGP_DATA, mPgpData);
        outState.putBoolean(SHOW_PICTURES, mShowPictures);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        setLoadPictures(savedInstanceState.getBoolean(SHOW_PICTURES));
        initializeCrypto((PgpData) savedInstanceState.getSerializable(STATE_PGP_DATA));
        updateDecryptLayout();
    }

    private void displayMessage(MessageReference ref)
    {
        mMessageReference = ref;
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "MessageView displaying message " + mMessageReference);
        mAccount = Preferences.getPreferences(this).getAccount(mMessageReference.accountUuid);
        mTopView.setVisibility(View.GONE);
        mTopView.scrollTo(0, 0);
        mMessageContentView.scrollTo(0, 0);
        mHeaderContainer.setVisibility(View.GONE);

        mMessageContentView.clearView();
        setLoadPictures(false);
        mAttachments.removeAllViews();
        findSurroundingMessagesUid();
        // start with fresh, empty PGP data
        initializeCrypto(null);
        mTopView.setVisibility(View.VISIBLE);
        mController.loadMessageForView(
            mAccount,
            mMessageReference.folderName,
            mMessageReference.uid,
            mListener);
        setupDisplayMessageButtons();
    }

    private void setupDisplayMessageButtons()
    {
        mDelete.setEnabled(true);
        next.setEnabled(mNextMessage != null);
        previous.setEnabled(mPreviousMessage != null);
        // If moving isn't support at all, then all of them must be disabled anyway.
        if (mController.isMoveCapable(mAccount))
        {
            // Only enable the button if the Archive folder is not the current folder and not NONE.
            mArchive.setEnabled(!mMessageReference.folderName.equals(mAccount.getArchiveFolderName()) &&
                                !K9.FOLDER_NONE.equalsIgnoreCase(mAccount.getArchiveFolderName()));
            // Only enable the button if the Spam folder is not the current folder and not NONE.
            mSpam.setEnabled(!mMessageReference.folderName.equals(mAccount.getSpamFolderName()) &&
                             !K9.FOLDER_NONE.equalsIgnoreCase(mAccount.getSpamFolderName()));
            mMove.setEnabled(true);
        }
        else
        {
            disableMoveButtons();
        }
    }

    private void staticButtons()
    {
        View buttons = findViewById(R.id.scrolling_buttons);
        if (buttons != null)
        {
            buttons.setVisibility(View.GONE);
        }
        next = findViewById(R.id.next);
        previous = findViewById(R.id.previous);
        mDelete = findViewById(R.id.delete);
    }

    private void scrollButtons()
    {
        View buttons = findViewById(R.id.bottom_buttons);
        if (buttons != null)
        {
            buttons.setVisibility(View.GONE);
        }
        next = findViewById(R.id.next_scrolling);
        previous = findViewById(R.id.previous_scrolling);
        mDelete = findViewById(R.id.delete_scrolling);
    }

    private void staticMoveButtons()
    {
        View buttons = findViewById(R.id.scrolling_move_buttons);
        if (buttons != null)
        {
            buttons.setVisibility(View.GONE);
        }
        mArchive = findViewById(R.id.archive);
        mMove = findViewById(R.id.move);
        mSpam = findViewById(R.id.spam);
    }

    private void scrollMoveButtons()
    {
        View buttons = findViewById(R.id.move_buttons);
        if (buttons != null)
        {
            buttons.setVisibility(View.GONE);
        }
        mArchive = findViewById(R.id.archive_scrolling);
        mMove = findViewById(R.id.move_scrolling);
        mSpam = findViewById(R.id.spam_scrolling);
    }

    private void disableButtons()
    {
        setLoadPictures(false);
        disableMoveButtons();
        next.setEnabled(false);
        previous.setEnabled(false);
        mDelete.setEnabled(false);
    }

    private void disableMoveButtons()
    {
        mArchive.setEnabled(false);
        mMove.setEnabled(false);
        mSpam.setEnabled(false);
    }

    private void setOnClickListener(int viewCode)
    {
        View thisView = findViewById(viewCode);
        if (thisView != null)
        {
            thisView.setOnClickListener(this);
        }
    }

    private void findSurroundingMessagesUid()
    {
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
    public void onResume()
    {
        super.onResume();
        if (!mAccount.isAvailable(this))
        {
            onAccountUnavailable();
            return;
        }
        StorageManager.getInstance(getApplication()).addListener(mStorageListener);
    }

    @Override
    protected void onPause()
    {
        StorageManager.getInstance(getApplication()).removeListener(mStorageListener);
        super.onPause();
    }

    protected void onAccountUnavailable()
    {
        finish();
        // TODO inform user about account unavailability using Toast
        Accounts.listAccounts(this);
    }

    /**
     * Called from UI thread when user select Delete
     */
    private void onDelete()
    {
        if (K9.confirmDelete())
        {
            showDialog(R.id.dialog_confirm_delete);
        }
        else
        {
            delete();
        }
    }

    /**
     * @param id
     * @return Never <code>null</code>
     */
    protected Dialog createConfirmDeleteDialog(final int id)
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_confirm_delete_title);
        builder.setMessage(R.string.dialog_confirm_delete_message);
        builder.setPositiveButton(R.string.dialog_confirm_delete_confirm_button,
                                  new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dismissDialog(id);
                delete();
            }
        });
        builder.setNegativeButton(R.string.dialog_confirm_delete_cancel_button,
                                  new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dismissDialog(id);
            }
        });
        return builder.create();
    }

    private void delete()
    {
        if (mMessage != null)
        {
            // Disable the delete button after it's tapped (to try to prevent
            // accidental clicks)
            disableButtons();
            Message messageToDelete = mMessage;
            showNextMessageOrReturn();
            mController.deleteMessages(
                new Message[] {messageToDelete},
                null);
        }
    }

    private void onRefile(String dstFolder)
    {
        if (!mController.isMoveCapable(mAccount))
        {
            return;
        }
        if (!mController.isMoveCapable(mMessage))
        {
            Toast toast = Toast.makeText(this, R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        String srcFolder = mMessageReference.folderName;
        Message messageToMove = mMessage;
        if (K9.FOLDER_NONE.equalsIgnoreCase(dstFolder))
        {
            return;
        }
        showNextMessageOrReturn();
        mController
        .moveMessage(mAccount, srcFolder, messageToMove, dstFolder, null);
    }



    private void showNextMessageOrReturn()
    {
        if (K9.messageViewReturnToList())
        {
            finish();
        }
        else
        {
            showNextMessage();
        }
    }

    private void showNextMessage()
    {
        findSurroundingMessagesUid();
        mMessageReferences.remove(mMessageReference);
        if (mLastDirection == NEXT && mNextMessage != null)
        {
            onNext();
        }
        else if (mLastDirection == PREVIOUS && mPreviousMessage != null)
        {
            onPrevious();
        }
        else if (mNextMessage != null)
        {
            onNext();
        }
        else if (mPreviousMessage != null)
        {
            onPrevious();
        }
        else
        {
            finish();
        }
    }


    private void onReply()
    {
        if (mMessage != null)
        {
            MessageCompose.actionReply(this, mAccount, mMessage, false, mPgpData.getDecryptedData());
            finish();
        }
    }

    private void onReplyAll()
    {
        if (mMessage != null)
        {
            MessageCompose.actionReply(this, mAccount, mMessage, true, mPgpData.getDecryptedData());
            finish();
        }
    }

    private void onForward()
    {
        if (mMessage != null)
        {
            MessageCompose.actionForward(this, mAccount, mMessage, mPgpData.getDecryptedData());
            finish();
        }
    }

    private void onFlag()
    {
        if (mMessage != null)
        {
            mController.setFlag(mAccount,
                                mMessage.getFolder().getName(), new String[] {mMessage.getUid()}, Flag.FLAGGED, !mMessage.isSet(Flag.FLAGGED));
            try
            {
                mMessage.setFlag(Flag.FLAGGED, !mMessage.isSet(Flag.FLAGGED));
                mHandler.setHeaders( mMessage);
                prepareMenuItems();
            }
            catch (MessagingException me)
            {
                Log.e(K9.LOG_TAG, "Could not set flag on local message", me);
            }
        }
    }

    private void onMove()
    {
        if ((!mController.isMoveCapable(mAccount))
                || (mMessage == null))
        {
            return;
        }
        if (!mController.isMoveCapable(mMessage))
        {
            Toast toast = Toast.makeText(this, R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        startRefileActivity(ACTIVITY_CHOOSE_FOLDER_MOVE);
    }

    private void onCopy()
    {
        if ((!mController.isCopyCapable(mAccount))
                || (mMessage == null))
        {
            return;
        }
        if (!mController.isCopyCapable(mMessage))
        {
            Toast toast = Toast.makeText(this, R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        startRefileActivity(ACTIVITY_CHOOSE_FOLDER_COPY);
    }

    private void startRefileActivity(int activity)
    {
        Intent intent = new Intent(this, ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount.getUuid());
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, mMessageReference.folderName);
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, mAccount.getLastSelectedFolderName());
        intent.putExtra(ChooseFolder.EXTRA_MESSAGE, mMessageReference);
        startActivityForResult(intent, activity);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (mAccount.getCryptoProvider().onActivityResult(this, requestCode, resultCode, data, mPgpData))
        {
            return;
        }
        if (resultCode != RESULT_OK)
            return;
        switch (requestCode)
        {
            case ACTIVITY_CHOOSE_FOLDER_MOVE:
            case ACTIVITY_CHOOSE_FOLDER_COPY:
                if (data == null)
                    return;
                String destFolderName = data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER);
                String srcFolderName = data.getStringExtra(ChooseFolder.EXTRA_CUR_FOLDER);
                MessageReference ref = (MessageReference) data.getSerializableExtra(ChooseFolder.EXTRA_MESSAGE);
                if (mMessageReference.equals(ref))
                {
                    mAccount.setLastSelectedFolderName(destFolderName);
                    switch (requestCode)
                    {
                        case ACTIVITY_CHOOSE_FOLDER_MOVE:
                            Message messageToMove = mMessage;
                            showNextMessageOrReturn();
                            mController.moveMessage(mAccount,
                                                    srcFolderName, messageToMove, destFolderName, null);
                            break;
                        case ACTIVITY_CHOOSE_FOLDER_COPY:
                            mController.copyMessage(mAccount,
                                                    srcFolderName, mMessage, destFolderName, null);
                            break;
                    }
                }
                break;
        }
    }

    private void onSendAlternate()
    {
        if (mMessage != null)
        {
            mController.sendAlternate(this, mAccount, mMessage);
        }
    }

    @Override
    protected void onNext()
    {
        if (mNextMessage == null)
        {
            Toast.makeText(this, getString(R.string.end_of_folder), Toast.LENGTH_SHORT).show();
            return;
        }
        mLastDirection = NEXT;
        disableButtons();
        if (K9.showAnimations())
        {
            mTopView.startAnimation(outToLeftAnimation());
        }
        displayMessage(mNextMessage);
        next.requestFocus();
    }

    @Override
    protected void onPrevious()
    {
        if (mPreviousMessage == null)
        {
            Toast.makeText(this, getString(R.string.end_of_folder), Toast.LENGTH_SHORT).show();
            return;
        }
        mLastDirection = PREVIOUS;
        disableButtons();
        if (K9.showAnimations())
        {
            mTopView.startAnimation(inFromRightAnimation());
        }
        displayMessage(mPreviousMessage);
        previous.requestFocus();
    }

    private void onMarkAsUnread()
    {
        if (mMessage != null)
        {
            mController.setFlag(
                mAccount,
                mMessageReference.folderName,
                new String[] { mMessage.getUid() },
                Flag.SEEN,
                false);
            try
            {
                mMessage.setFlag(Flag.SEEN, false);
                mHandler.setHeaders(mMessage);
                String subject = mMessage.getSubject();
                setTitle(subject);
            }
            catch (Exception e)
            {
                Log.e(K9.LOG_TAG, "Unable to unset SEEN flag on message", e);
            }
        }
    }


    private void onDownloadRemainder()
    {
        if (mMessage.isSet(Flag.X_DOWNLOADED_FULL))
        {
            return;
        }
        mDownloadRemainder.setEnabled(false);
        mController.loadMessageForViewRemote(
            mAccount,
            mMessageReference.folderName,
            mMessageReference.uid,
            mListener);
    }

    private void onShowPictures()
    {
        // TODO: Download attachments that are used as inline image
        setLoadPictures(true);
    }

    /**
     * Enable/disable image loading of the WebView. But always hide the
     * "Show pictures" button!
     *
     * @param enable true, if (network) images should be loaded.
     *               false, otherwise.
     */
    private void setLoadPictures(boolean enable)
    {
        mMessageContentView.blockNetworkData(!enable);
        mShowPictures = enable;
        mHandler.showShowPictures(false);
    }

    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.reply:
            case R.id.reply_scrolling:
                onReply();
                break;
            case R.id.reply_all:
                onReplyAll();
                break;
            case R.id.delete:
            case R.id.delete_scrolling:
                onDelete();
                break;
            case R.id.forward:
            case R.id.forward_scrolling:
                onForward();
                break;
            case R.id.archive:
            case R.id.archive_scrolling:
                onRefile(mAccount.getArchiveFolderName());
                break;
            case R.id.spam:
            case R.id.spam_scrolling:
                onRefile(mAccount.getSpamFolderName());
                break;
            case R.id.move:
            case R.id.move_scrolling:
                onMove();
                break;
            case R.id.next:
            case R.id.next_scrolling:
                onNext();
                break;
            case R.id.previous:
            case R.id.previous_scrolling:
                onPrevious();
                break;
            case R.id.download:
                ((AttachmentView)view).saveFile();
                break;
            case R.id.show_pictures:
                onShowPictures();
                break;
            case R.id.download_remainder:
                onDownloadRemainder();
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
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
                runOnUiThread(new Runnable()
                {
                    @Override public void run()
                    {
                        mHeaderContainer.onShowAdditionalHeaders();
                    }
                });
                break;
            case R.id.select_text:
                mToggleScrollView.setScrolling(false);
                mMessageContentView.emulateShiftHeld();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.message_view_option, menu);
        optionsMenu = menu;
        prepareMenuItems();
        if (!mController.isCopyCapable(mAccount))
        {
            menu.findItem(R.id.copy).setVisible(false);
        }
        if (!mController.isMoveCapable(mAccount))
        {
            menu.findItem(R.id.move).setVisible(false);
            menu.findItem(R.id.archive).setVisible(false);
            menu.findItem(R.id.spam).setVisible(false);
        }
        if (K9.FOLDER_NONE.equalsIgnoreCase(mAccount.getArchiveFolderName()))
        {
            menu.findItem(R.id.archive).setVisible(false);
        }
        if (K9.FOLDER_NONE.equalsIgnoreCase(mAccount.getSpamFolderName()))
        {
            menu.findItem(R.id.spam).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        prepareMenuItems();
        return super.onPrepareOptionsMenu(menu);
    }
    // TODO: when switching to API version 8, override onCreateDialog(int, Bundle)

    /**
     * @param id The id of the dialog.
     * @return The dialog. If you return null, the dialog will not be created.
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(final int id)
    {
        switch (id)
        {
            case R.id.dialog_confirm_delete:
            {
                return createConfirmDeleteDialog(id);
            }
        }
        return super.onCreateDialog(id);
    }

    private void prepareMenuItems()
    {
        Menu menu = optionsMenu;
        if (menu != null)
        {
            MenuItem flagItem = menu.findItem(R.id.flag);
            if (flagItem != null && mMessage != null)
            {
                flagItem.setTitle((mMessage.isSet(Flag.FLAGGED) ? R.string.unflag_action : R.string.flag_action));
            }
            MenuItem additionalHeadersItem = menu.findItem(R.id.show_full_header);
            if (additionalHeadersItem != null)
            {
                additionalHeadersItem.setTitle(mHeaderContainer.additionalHeadersVisible() ?
                                               R.string.hide_full_header_action : R.string.show_full_header_action);
            }
        }
    }

    public void displayMessage(Account account, String folder, String uid, Message message)
    {
        try
        {
            if (MessageView.this.mMessage != null
                    && MessageView.this.mMessage.isSet(Flag.X_DOWNLOADED_PARTIAL)
                    && message.isSet(Flag.X_DOWNLOADED_FULL))
            {
                mHandler.setHeaders(message);
            }
            MessageView.this.mMessage = message;
            mHandler.removeAllAttachments();
            String text, type;
            if (mPgpData.getDecryptedData() != null)
            {
                text = mPgpData.getDecryptedData();
                type = "text/plain";
            }
            else
            {
                // getTextForDisplay() always returns HTML-ified content.
                text = ((LocalMessage) mMessage).getTextForDisplay();
                type = "text/html";
            }
            if (text != null)
            {
                final String emailText = text;
                final String contentType = type;
                mHandler.post(new Runnable()
                {
                    public void run()
                    {
                        mTopView.scrollTo(0, 0);
                        if (mScreenReaderEnabled)
                        {
                            mAccessibleMessageContentView.loadDataWithBaseURL("http://",
                                    emailText, contentType, "utf-8", null);
                        }
                        else
                        {
                            mMessageContentView.loadDataWithBaseURL("http://", emailText,
                                                                    contentType, "utf-8", null);
                            mMessageContentView.scrollTo(0, 0);
                        }
                        updateDecryptLayout();
                    }
                });
                // If the message contains external pictures and the "Show pictures"
                // button wasn't already pressed, see if the user's preferences has us
                // showing them anyway.
                if (Utility.hasExternalImages(text) && !mShowPictures)
                {
                    if ((account.getShowPictures() == Account.ShowPictures.ALWAYS) ||
                            ((account.getShowPictures() == Account.ShowPictures.ONLY_FROM_CONTACTS) &&
                             mContacts.isInContacts(message.getFrom()[0].getAddress())))
                    {
                        onShowPictures();
                    }
                    else
                    {
                        mHandler.showShowPictures(true);
                    }
                }
            }
            else
            {
                mHandler.post(new Runnable()
                {
                    public void run()
                    {
                        mMessageContentView.loadUrl("file:///android_asset/empty.html");
                        updateDecryptLayout();
                    }
                });
            }
            renderAttachments(mMessage, 0);
        }
        catch (Exception e)
        {
            if (Config.LOGV)
            {
                Log.v(K9.LOG_TAG, "loadMessageForViewBodyAvailable", e);
            }
        }
    }

    private void renderAttachments(Part part, int depth) throws MessagingException
    {
        if (part.getBody() instanceof Multipart)
        {
            Multipart mp = (Multipart) part.getBody();
            for (int i = 0; i < mp.getCount(); i++)
            {
                renderAttachments(mp.getBodyPart(i), depth + 1);
            }
        }
        else if (part instanceof LocalAttachmentBodyPart)
        {
            String contentDisposition = MimeUtility.unfoldAndDecode(part.getDisposition());
            // Inline parts with a content-id are almost certainly components of an HTML message
            // not attachments. Don't show attachment download buttons for them.
            if (contentDisposition != null &&
                    MimeUtility.getHeaderParameter(contentDisposition, null).matches("^(?i:inline)")
                    && part.getHeader("Content-ID") != null)
            {
                return;
            }
            renderPartAsAttachment(part);
        }
    }

    private void renderPartAsAttachment(Part part) throws MessagingException
    {
        LayoutInflater inflater = getLayoutInflater();
        AttachmentView view = (AttachmentView)inflater.inflate(R.layout.message_view_attachment, null);
        if (view.populateFromPart(part, mMessage, mAccount, mController, mListener))
        {
            mHandler.addAttachment((View)view);
        }
        return;
    }

    class Listener extends MessagingListener
    {
        @Override
        public void loadMessageForViewHeadersAvailable(Account account, String folder, String uid,
                final Message message)
        {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid()))
            {
                return;
            }
            MessageView.this.mMessage = message;
            if (!message.isSet(Flag.X_DOWNLOADED_FULL)
                    && !message.isSet(Flag.X_DOWNLOADED_PARTIAL))
            {
                mHandler.post(new Runnable()
                {
                    public void run()
                    {
                        mMessageContentView.loadUrl("file:///android_asset/downloading.html");
                        updateDecryptLayout();
                    }
                });
            }
            mHandler.setHeaders(message);
        }

        @Override
        public void loadMessageForViewBodyAvailable(Account account, String folder, String uid,
                Message message)
        {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid()))
            {
                return;
            }

            displayMessage(account, folder, uid, message);
        }//loadMessageForViewBodyAvailable



        @Override
        public void loadMessageForViewFailed(Account account, String folder, String uid,
                                             final Throwable t)
        {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid()))
            {
                return;
            }
            mHandler.post(new Runnable()
            {
                public void run()
                {
                    setProgressBarIndeterminateVisibility(false);
                    if (t instanceof IllegalArgumentException)
                    {
                        mHandler.invalidIdError();
                    }
                    else
                    {
                        mHandler.networkError();
                    }
                    if ((MessageView.this.mMessage == null) ||
                            !MessageView.this.mMessage.isSet(Flag.X_DOWNLOADED_PARTIAL))
                    {
                        mMessageContentView.loadUrl("file:///android_asset/empty.html");
                        updateDecryptLayout();
                    }
                }
            });
        }

        @Override
        public void loadMessageForViewFinished(Account account, String folder, String uid,
                                               Message message)
        {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid()))
            {
                return;
            }
            mHandler.post(new Runnable()
            {
                public void run()
                {
                    setProgressBarIndeterminateVisibility(false);
                }
            });
        }

        @Override
        public void loadMessageForViewStarted(Account account, String folder, String uid)
        {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid()))
            {
                return;
            }
            mHandler.post(new Runnable()
            {
                public void run()
                {
                    updateDecryptLayout();
                    setProgressBarIndeterminateVisibility(true);
                }
            });
        }

        @Override
        public void loadAttachmentStarted(Account account, Message message,
                                          Part part, Object tag, boolean requiresDownload)
        {
            if (mMessage != message)
            {
                return;
            }
            mHandler.setAttachmentsEnabled(false);
            mHandler.progress(true);
            if (requiresDownload)
            {
                mHandler.fetchingAttachment();
            }
        }

        @Override
        public void loadAttachmentFinished(Account account, Message message,
                                           Part part, Object tag)
        {
            if (mMessage != message)
            {
                return;
            }
            mHandler.setAttachmentsEnabled(true);
            mHandler.progress(false);
            Object[] params = (Object[]) tag;
            boolean download = (Boolean) params[0];
            AttachmentView attachment = (AttachmentView) params[1];
            if (download)
            {
                attachment.writeFile();

            }
            else
            {
                attachment.showFile();
            }
        }

        @Override
        public void loadAttachmentFailed(Account account, Message message, Part part,
                                         Object tag, String reason)
        {
            if (mMessage != message)
            {
                return;
            }
            mHandler.setAttachmentsEnabled(true);
            mHandler.progress(false);
            mHandler.networkError();
        }
    }


    private void initializeCrypto(PgpData data)
    {
        if (data == null)
        {
            if (mAccount == null)
            {
                mAccount = Preferences.getPreferences(this).getAccount(mMessageReference.accountUuid);
            }
            mPgpData = new PgpData();
        }
        else
        {
            mPgpData = data;
        }
    }

    /**
     * Fill the decrypt layout with signature data, if known, make controls visible, if
     * they should be visible.
     */
    public void updateDecryptLayout()
    {
        if (mPgpData.getSignatureKeyId() != 0)
        {
            mCryptoSignatureUserIdRest.setText(
                getString(R.string.key_id, Long.toHexString(mPgpData.getSignatureKeyId() & 0xffffffffL)));
            String userId = mPgpData.getSignatureUserId();
            if (userId == null)
            {
                userId = getString(R.string.unknown_crypto_signature_user_id);
            }
            String chunks[] = userId.split(" <", 2);
            String name = chunks[0];
            if (chunks.length > 1)
            {
                mCryptoSignatureUserIdRest.setText("<" + chunks[1]);
            }
            mCryptoSignatureUserId.setText(name);
            if (mPgpData.getSignatureSuccess())
            {
                mCryptoSignatureStatusImage.setImageResource(R.drawable.overlay_ok);
            }
            else if (mPgpData.getSignatureUnknown())
            {
                mCryptoSignatureStatusImage.setImageResource(R.drawable.overlay_error);
            }
            else
            {
                mCryptoSignatureStatusImage.setImageResource(R.drawable.overlay_error);
            }
            mCryptoSignatureLayout.setVisibility(View.VISIBLE);
            mDecryptLayout.setVisibility(View.VISIBLE);
        }
        else
        {
            mCryptoSignatureLayout.setVisibility(View.INVISIBLE);
        }
        if (false || ((mMessage == null) && (mPgpData.getDecryptedData() == null)))
        {
            mDecryptLayout.setVisibility(View.GONE);
            return;
        }
        if (mPgpData.getDecryptedData() != null)
        {
            if (mPgpData.getSignatureKeyId() == 0)
            {
                mDecryptLayout.setVisibility(View.GONE);
            }
            else
            {
                // no need to show this after decryption/verification
                mDecryptButton.setVisibility(View.GONE);
            }
            return;
        }
        mDecryptButton.setVisibility(View.VISIBLE);
        CryptoProvider crypto = mAccount.getCryptoProvider();
        if (crypto.isEncrypted(mMessage))
        {
            mDecryptButton.setText(R.string.btn_decrypt);
            mDecryptLayout.setVisibility(View.VISIBLE);
        }
        else if (crypto.isSigned(mMessage))
        {
            mDecryptButton.setText(R.string.btn_verify);
            mDecryptLayout.setVisibility(View.VISIBLE);
        }
        else
        {
            mDecryptLayout.setVisibility(View.GONE);
            try
            {
                // check for PGP/MIME encryption
                Part pgp = MimeUtility.findFirstPartByMimeType(mMessage, "application/pgp-encrypted");
                if (pgp != null)
                {
                    Toast.makeText(this, R.string.pgp_mime_unsupported, Toast.LENGTH_LONG).show();
                }
            }
            catch (MessagingException e)
            {
                // nothing to do...
            }
        }
    }

    public void onDecryptDone()
    {
        // TODO: this might not be enough if the orientation was changed while in APG,
        // sometimes shows the original encrypted content
        mMessageContentView.loadDataWithBaseURL("email://", mPgpData.getDecryptedData(), "text/plain", "utf-8", null);
        updateDecryptLayout();
    }
}
