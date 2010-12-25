
package com.fsck.k9.activity;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.james.mime4j.codec.EncoderUtil;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.OpenableColumns;
import android.text.Html;
import android.text.TextWatcher;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.widget.AutoCompleteTextView.Validator;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.EmailAddressAdapter;
import com.fsck.k9.EmailAddressValidator;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.crypto.CryptoProvider;
import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.mail.store.LocalStore.LocalAttachmentBody;
import com.fsck.k9.mail.store.UnavailableStorageException;

public class MessageCompose extends K9Activity implements OnClickListener, OnFocusChangeListener
{
    private static final int DIALOG_SAVE_OR_DISCARD_DRAFT_MESSAGE = 1;
    private static final int REPLY_WRAP_LINE_WIDTH = 72;

    private static final String ACTION_REPLY = "com.fsck.k9.intent.action.REPLY";
    private static final String ACTION_REPLY_ALL = "com.fsck.k9.intent.action.REPLY_ALL";
    private static final String ACTION_FORWARD = "com.fsck.k9.intent.action.FORWARD";
    private static final String ACTION_EDIT_DRAFT = "com.fsck.k9.intent.action.EDIT_DRAFT";

    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_MESSAGE_BODY  = "messageBody";
    private static final String EXTRA_MESSAGE_REFERENCE = "message_reference";

    private static final String STATE_KEY_ATTACHMENTS =
        "com.fsck.k9.activity.MessageCompose.attachments";
    private static final String STATE_KEY_CC_SHOWN =
        "com.fsck.k9.activity.MessageCompose.ccShown";
    private static final String STATE_KEY_BCC_SHOWN =
        "com.fsck.k9.activity.MessageCompose.bccShown";
    private static final String STATE_KEY_QUOTED_TEXT_SHOWN =
        "com.fsck.k9.activity.MessageCompose.quotedTextShown";
    private static final String STATE_KEY_SOURCE_MESSAGE_PROCED =
        "com.fsck.k9.activity.MessageCompose.stateKeySourceMessageProced";
    private static final String STATE_KEY_DRAFT_UID =
        "com.fsck.k9.activity.MessageCompose.draftUid";
    private static final String STATE_IDENTITY_CHANGED =
        "com.fsck.k9.activity.MessageCompose.identityChanged";
    private static final String STATE_IDENTITY =
        "com.fsck.k9.activity.MessageCompose.identity";
    private static final String STATE_PGP_DATA = "pgpData";
    private static final String STATE_IN_REPLY_TO = "com.fsck.k9.activity.MessageCompose.inReplyTo";
    private static final String STATE_REFERENCES = "com.fsck.k9.activity.MessageCompose.references";

    private static final int MSG_PROGRESS_ON = 1;
    private static final int MSG_PROGRESS_OFF = 2;
    private static final int MSG_UPDATE_TITLE = 3;
    private static final int MSG_SKIPPED_ATTACHMENTS = 4;
    private static final int MSG_SAVED_DRAFT = 5;
    private static final int MSG_DISCARDED_DRAFT = 6;

    private static final int ACTIVITY_REQUEST_PICK_ATTACHMENT = 1;
    private static final int ACTIVITY_CHOOSE_IDENTITY = 2;
    private static final int ACTIVITY_CHOOSE_ACCOUNT = 3;

    /**
     * Regular expression to remove the first localized "Re:" prefix in subjects.
     *
     * Currently:
     * - "Aw:" (german: abbreviation for "Antwort")
     */
    private static final Pattern prefix = Pattern.compile("^AW[:\\s]\\s*", Pattern.CASE_INSENSITIVE);

    /**
     * The account used for message composition.
     */
    private Account mAccount;

    /**
     * This identity's settings are used for message composition.
     * Note: This has to be an identity of the account {@link #mAccount}.
     */
    private Identity mIdentity;

    private boolean mIdentityChanged = false;
    private boolean mSignatureChanged = false;

    /**
     * Reference to the source message (in case of reply, forward, or edit
     * draft actions).
     */
    private MessageReference mMessageReference;

    private Message mSourceMessage;
    private String mSourceMessageBody;

    /**
     * Indicates that the source message has been processed at least once and should not
     * be processed on any subsequent loads. This protects us from adding attachments that
     * have already been added from the restore of the view state.
     */
    private boolean mSourceMessageProcessed = false;


    private TextView mFromView;
    private MultiAutoCompleteTextView mToView;
    private MultiAutoCompleteTextView mCcView;
    private MultiAutoCompleteTextView mBccView;
    private EditText mSubjectView;
    private EditText mSignatureView;
    private EditText mMessageContentView;
    private LinearLayout mAttachments;
    private View mQuotedTextBar;
    private ImageButton mQuotedTextDelete;
    private EditText mQuotedText;
    private View mEncryptLayout;
    private CheckBox mCryptoSignatureCheckbox;
    private CheckBox mEncryptCheckbox;
    private TextView mCryptoSignatureUserId;
    private TextView mCryptoSignatureUserIdRest;

    private PgpData mPgpData = null;

    private String mReferences;
    private String mInReplyTo;

    private boolean mDraftNeedsSaving = false;
    private boolean mPreventDraftSaving = false;

    /**
     * The draft uid of this message. This is used when saving drafts so that the same draft is
     * overwritten instead of being created anew. This property is null until the first save.
     */
    private String mDraftUid;

    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(android.os.Message msg)
        {
            switch (msg.what)
            {
                case MSG_PROGRESS_ON:
                    setProgressBarIndeterminateVisibility(true);
                    break;
                case MSG_PROGRESS_OFF:
                    setProgressBarIndeterminateVisibility(false);
                    break;
                case MSG_UPDATE_TITLE:
                    updateTitle();
                    break;
                case MSG_SKIPPED_ATTACHMENTS:
                    Toast.makeText(
                        MessageCompose.this,
                        getString(R.string.message_compose_attachments_skipped_toast),
                        Toast.LENGTH_LONG).show();
                    break;
                case MSG_SAVED_DRAFT:
                    Toast.makeText(
                        MessageCompose.this,
                        getString(R.string.message_saved_toast),
                        Toast.LENGTH_LONG).show();
                    break;
                case MSG_DISCARDED_DRAFT:
                    Toast.makeText(
                        MessageCompose.this,
                        getString(R.string.message_discarded_toast),
                        Toast.LENGTH_LONG).show();
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    private Listener mListener = new Listener();
    private EmailAddressAdapter mAddressAdapter;
    private Validator mAddressValidator;


    static class Attachment implements Serializable
    {
        public String name;
        public String contentType;
        public long size;
        public Uri uri;
    }

    /**
     * Compose a new message using the given account. If account is null the default account
     * will be used.
     * @param context
     * @param account
     */
    public static void actionCompose(Context context, Account account)
    {
        if (account == null)
        {
            account = Preferences.getPreferences(context).getDefaultAccount();
        }
        Intent i = new Intent(context, MessageCompose.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }

    /**
     * Compose a new message as a reply to the given message. If replyAll is true the function
     * is reply all instead of simply reply.
     * @param context
     * @param account
     * @param message
     * @param replyAll
     * @param messageBody optional, for decrypted messages, null if it should be grabbed from the given message
     */
    public static void actionReply(
        Context context,
        Account account,
        Message message,
        boolean replyAll,
        String messageBody)
    {
        Intent i = new Intent(context, MessageCompose.class);
        i.putExtra(EXTRA_MESSAGE_BODY, messageBody);
        i.putExtra(EXTRA_MESSAGE_REFERENCE, message.makeMessageReference());
        if (replyAll)
        {
            i.setAction(ACTION_REPLY_ALL);
        }
        else
        {
            i.setAction(ACTION_REPLY);
        }
        context.startActivity(i);
    }

    /**
     * Compose a new message as a forward of the given message.
     * @param context
     * @param account
     * @param message
     * @param messageBody optional, for decrypted messages, null if it should be grabbed from the given message
     */
    public static void actionForward(
        Context context,
        Account account,
        Message message,
        String messageBody)
    {
        Intent i = new Intent(context, MessageCompose.class);
        i.putExtra(EXTRA_MESSAGE_BODY, messageBody);
        i.putExtra(EXTRA_MESSAGE_REFERENCE, message.makeMessageReference());
        i.setAction(ACTION_FORWARD);
        context.startActivity(i);
    }

    /**
     * Continue composition of the given message. This action modifies the way this Activity
     * handles certain actions.
     * Save will attempt to replace the message in the given folder with the updated version.
     * Discard will delete the message from the given folder.
     * @param context
     * @param account
     * @param folder
     * @param message
     */
    public static void actionEditDraft(Context context, Account account, Message message)
    {
        Intent i = new Intent(context, MessageCompose.class);
        i.putExtra(EXTRA_MESSAGE_REFERENCE, message.makeMessageReference());
        i.setAction(ACTION_EDIT_DRAFT);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.message_compose);

        final Intent intent = getIntent();

        mMessageReference = (MessageReference) intent.getSerializableExtra(EXTRA_MESSAGE_REFERENCE);
        mSourceMessageBody = (String) intent.getStringExtra(EXTRA_MESSAGE_BODY);

        final String accountUuid = (mMessageReference != null) ?
                                   mMessageReference.accountUuid :
                                   intent.getStringExtra(EXTRA_ACCOUNT);

        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        if (mAccount == null)
        {
            mAccount = Preferences.getPreferences(this).getDefaultAccount();
        }

        if (mAccount == null)
        {
            /*
             * There are no accounts set up. This should not have happened. Prompt the
             * user to set up an account as an acceptable bailout.
             */
            startActivity(new Intent(this, Accounts.class));
            mDraftNeedsSaving = false;
            finish();
            return;
        }

        mAddressAdapter = EmailAddressAdapter.getInstance(this);
        mAddressValidator = new EmailAddressValidator();

        mFromView = (TextView)findViewById(R.id.from);
        mToView = (MultiAutoCompleteTextView)findViewById(R.id.to);
        mCcView = (MultiAutoCompleteTextView)findViewById(R.id.cc);
        mBccView = (MultiAutoCompleteTextView)findViewById(R.id.bcc);
        mSubjectView = (EditText)findViewById(R.id.subject);

        EditText upperSignature = (EditText)findViewById(R.id.upper_signature);
        EditText lowerSignature = (EditText)findViewById(R.id.lower_signature);

        mMessageContentView = (EditText)findViewById(R.id.message_content);
        mAttachments = (LinearLayout)findViewById(R.id.attachments);
        mQuotedTextBar = findViewById(R.id.quoted_text_bar);
        mQuotedTextDelete = (ImageButton)findViewById(R.id.quoted_text_delete);
        mQuotedText = (EditText)findViewById(R.id.quoted_text);

        TextWatcher watcher = new TextWatcher()
        {
            public void beforeTextChanged(CharSequence s, int start,
            int before, int after) { }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count)
            {
                mDraftNeedsSaving = true;
            }

            public void afterTextChanged(android.text.Editable s) { }
        };

        TextWatcher sigwatcher = new TextWatcher()
        {
            public void beforeTextChanged(CharSequence s, int start,
            int before, int after) { }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count)
            {
                mDraftNeedsSaving = true;
                mSignatureChanged = true;
            }

            public void afterTextChanged(android.text.Editable s) { }
        };

        mToView.addTextChangedListener(watcher);
        mCcView.addTextChangedListener(watcher);
        mBccView.addTextChangedListener(watcher);
        mSubjectView.addTextChangedListener(watcher);

        mMessageContentView.addTextChangedListener(watcher);
        mQuotedText.addTextChangedListener(watcher);

        /*
         * We set this to invisible by default. Other methods will turn it back on if it's
         * needed.
         */
        mQuotedTextBar.setVisibility(View.GONE);
        mQuotedText.setVisibility(View.GONE);

        mQuotedTextDelete.setOnClickListener(this);

        mFromView.setVisibility(View.GONE);

        mToView.setAdapter(mAddressAdapter);
        mToView.setTokenizer(new Rfc822Tokenizer());
        mToView.setValidator(mAddressValidator);

        mCcView.setAdapter(mAddressAdapter);
        mCcView.setTokenizer(new Rfc822Tokenizer());
        mCcView.setValidator(mAddressValidator);

        mBccView.setAdapter(mAddressAdapter);
        mBccView.setTokenizer(new Rfc822Tokenizer());
        mBccView.setValidator(mAddressValidator);


        mSubjectView.setOnFocusChangeListener(this);

        if (savedInstanceState != null)
        {
            /*
             * This data gets used in onCreate, so grab it here instead of onRestoreIntstanceState
             */
            mSourceMessageProcessed = savedInstanceState.getBoolean(STATE_KEY_SOURCE_MESSAGE_PROCED, false);
        }


        final String action = intent.getAction();
        initFromIntent(intent);

        if (mIdentity == null)
        {
            mIdentity = mAccount.getIdentity(0);
        }

        if (mAccount.isSignatureBeforeQuotedText())
        {
            mSignatureView = upperSignature;
            lowerSignature.setVisibility(View.GONE);
        }
        else
        {
            mSignatureView = lowerSignature;
            upperSignature.setVisibility(View.GONE);
        }
        mSignatureView.addTextChangedListener(sigwatcher);

        if (!mIdentity.getSignatureUse())
        {
            mSignatureView.setVisibility(View.GONE);
        }

        if (!mSourceMessageProcessed)
        {
            updateFrom();
            updateSignature();

            if (ACTION_REPLY.equals(action) ||
                    ACTION_REPLY_ALL.equals(action) ||
                    ACTION_FORWARD.equals(action) ||
                    ACTION_EDIT_DRAFT.equals(action))
            {
                /*
                 * If we need to load the message we add ourself as a message listener here
                 * so we can kick it off. Normally we add in onResume but we don't
                 * want to reload the message every time the activity is resumed.
                 * There is no harm in adding twice.
                 */
                MessagingController.getInstance(getApplication()).addListener(mListener);

                final Account account = Preferences.getPreferences(this).getAccount(mMessageReference.accountUuid);
                final String folderName = mMessageReference.folderName;
                final String sourceMessageUid = mMessageReference.uid;
                MessagingController.getInstance(getApplication()).loadMessageForView(account, folderName, sourceMessageUid, null);
            }

            if (!ACTION_EDIT_DRAFT.equals(action))
            {
                String bccAddress = mAccount.getAlwaysBcc();
                if ((bccAddress != null) && !("".equals(bccAddress)))
                {
                    addAddress(mBccView, new Address(bccAddress, ""));
                }
            }

            /*
            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "action = " + action + ", account = " + mMessageReference.accountUuid + ", folder = " + mMessageReference.folderName + ", sourceMessageUid = " + mMessageReference.uid);
            */

            if (ACTION_REPLY.equals(action) ||
                    ACTION_REPLY_ALL.equals(action))
            {
                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "Setting message ANSWERED flag to true");

                // TODO: Really, we should wait until we send the message, but that would require saving the original
                // message info along with a Draft copy, in case it is left in Drafts for a while before being sent

                final Account account = Preferences.getPreferences(this).getAccount(mMessageReference.accountUuid);
                final String folderName = mMessageReference.folderName;
                final String sourceMessageUid = mMessageReference.uid;
                MessagingController.getInstance(getApplication()).setFlag(account, folderName, new String[] { sourceMessageUid }, Flag.ANSWERED, true);
            }

            updateTitle();
        }

        if (ACTION_REPLY.equals(action) ||
                ACTION_REPLY_ALL.equals(action) ||
                ACTION_EDIT_DRAFT.equals(action))
        {
            //change focus to message body.
            mMessageContentView.requestFocus();
        }

        mEncryptLayout = (View)findViewById(R.id.layout_encrypt);
        mCryptoSignatureCheckbox = (CheckBox)findViewById(R.id.cb_crypto_signature);
        mCryptoSignatureUserId = (TextView)findViewById(R.id.userId);
        mCryptoSignatureUserIdRest = (TextView)findViewById(R.id.userIdRest);
        mEncryptCheckbox = (CheckBox)findViewById(R.id.cb_encrypt);

        initializeCrypto();
        final CryptoProvider crypto = mAccount.getCryptoProvider();
        if (crypto.isAvailable(this))
        {
            mEncryptLayout.setVisibility(View.VISIBLE);
            mCryptoSignatureCheckbox.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    CheckBox checkBox = (CheckBox) v;
                    if (checkBox.isChecked())
                    {
                        mPreventDraftSaving = true;
                        if (!crypto.selectSecretKey(MessageCompose.this, mPgpData))
                        {
                            mPreventDraftSaving = false;
                        }
                        checkBox.setChecked(false);
                    }
                    else
                    {
                        mPgpData.setSignatureKeyId(0);
                        updateEncryptLayout();
                    }
                }
            });

            if (mAccount.getCryptoAutoSignature())
            {
                long ids[] = crypto.getSecretKeyIdsFromEmail(this, mIdentity.getEmail());
                if (ids != null && ids.length > 0)
                {
                    mPgpData.setSignatureKeyId(ids[0]);
                    mPgpData.setSignatureUserId(crypto.getUserId(this, ids[0]));
                }
                else
                {
                    mPgpData.setSignatureKeyId(0);
                    mPgpData.setSignatureUserId(null);
                }
            }
            updateEncryptLayout();
        }
        else
        {
            mEncryptLayout.setVisibility(View.GONE);
        }

        mDraftNeedsSaving = false;
    }

    /**
     * Handle external intents that trigger the message compose activity.
     *
     * @param intent The (external) intent that started the activity.
     */
    private void initFromIntent(final Intent intent)
    {
        final String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action) || Intent.ACTION_SENDTO.equals(action))
        {
            /*
             * Someone has clicked a mailto: link. The address is in the URI.
             */
            if (intent.getData() != null)
            {
                Uri uri = intent.getData();
                if ("mailto".equals(uri.getScheme()))
                {
                    initializeFromMailto(uri);
                }
            }

            /*
             * Note: According to the documenation ACTION_VIEW and ACTION_SENDTO
             * don't accept EXTRA_* parameters. Contrary to the AOSP Email application
             * we don't accept those EXTRAs.
             * Dear developer, if your application is using those EXTRAs you're doing
             * it wrong! So go fix your program or get AOSP to change the documentation.
             */
        }
        //TODO: Use constant Intent.ACTION_SEND_MULTIPLE once we drop Android 1.5 support
        else if (Intent.ACTION_SEND.equals(action) ||
                 "android.intent.action.SEND_MULTIPLE".equals(action))
        {
            /*
             * Note: Here we allow a slight deviation from the documentated behavior.
             * EXTRA_TEXT is used as message body (if available) regardless of the MIME
             * type of the intent. In addition one or multiple attachments can be added
             * using EXTRA_STREAM.
             */
            CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (text != null)
            {
                mMessageContentView.setText(text);
            }

            String type = intent.getType();
            if (Intent.ACTION_SEND.equals(action))
            {
                Uri stream = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (stream != null)
                {
                    addAttachment(stream, type);
                }
            }
            else
            {
                ArrayList<Parcelable> list = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (list != null)
                {
                    for (Parcelable parcelable : list)
                    {
                        Uri stream = (Uri) parcelable;
                        if (stream != null)
                        {
                            addAttachment(stream, type);
                        }
                    }
                }
            }

            String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            if (subject != null)
            {
                mSubjectView.setText(subject);
            }

            String[] extraEmail = intent.getStringArrayExtra(Intent.EXTRA_EMAIL);
            String[] extraCc = intent.getStringArrayExtra(Intent.EXTRA_CC);
            String[] extraBcc = intent.getStringArrayExtra(Intent.EXTRA_BCC);

            if (extraEmail != null)
            {
                setRecipients(mToView, Arrays.asList(extraEmail));
            }

            boolean ccOrBcc = false;
            if (extraCc != null)
            {
                ccOrBcc |= setRecipients(mCcView, Arrays.asList(extraCc));
            }

            if (extraBcc != null)
            {
                ccOrBcc |= setRecipients(mBccView, Arrays.asList(extraBcc));
            }

            if (ccOrBcc)
            {
                // Display CC and BCC text fields if CC or BCC recipients were set by the intent.
                onAddCcBcc();
            }
        }
    }

    private boolean setRecipients(TextView view, List<String> recipients)
    {
        boolean recipientAdded = false;
        if (recipients != null)
        {
            StringBuffer addressList = new StringBuffer();
            for (String recipient : recipients)
            {
                addressList.append(recipient);
                addressList.append(", ");
                recipientAdded = true;
            }
            view.setText(addressList);
        }

        return recipientAdded;
    }

    private void initializeCrypto()
    {
        if (mPgpData != null)
        {
            return;
        }
        mPgpData = new PgpData();
    }

    /**
     * Fill the encrypt layout with the latest data about signature key and encryption keys.
     */
    public void updateEncryptLayout()
    {
        if (!mPgpData.hasSignatureKey())
        {
            mCryptoSignatureCheckbox.setText(R.string.btn_crypto_sign);
            mCryptoSignatureCheckbox.setChecked(false);
            mCryptoSignatureUserId.setVisibility(View.INVISIBLE);
            mCryptoSignatureUserIdRest.setVisibility(View.INVISIBLE);
        }
        else
        {
            // if a signature key is selected, then the checkbox itself has no text
            mCryptoSignatureCheckbox.setText("");
            mCryptoSignatureCheckbox.setChecked(true);
            mCryptoSignatureUserId.setVisibility(View.VISIBLE);
            mCryptoSignatureUserIdRest.setVisibility(View.VISIBLE);
            mCryptoSignatureUserId.setText(R.string.unknown_crypto_signature_user_id);
            mCryptoSignatureUserIdRest.setText("");

            String userId = mPgpData.getSignatureUserId();
            if (userId == null)
            {
                userId = mAccount.getCryptoProvider().getUserId(this, mPgpData.getSignatureKeyId());
                mPgpData.setSignatureUserId(userId);
            }

            if (userId != null)
            {
                String chunks[] = mPgpData.getSignatureUserId().split(" <", 2);
                mCryptoSignatureUserId.setText(chunks[0]);
                if (chunks.length > 1)
                {
                    mCryptoSignatureUserIdRest.setText("<" + chunks[1]);
                }
            }
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        MessagingController.getInstance(getApplication()).addListener(mListener);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        saveIfNeeded();
        MessagingController.getInstance(getApplication()).removeListener(mListener);
    }

    /**
     * The framework handles most of the fields, but we need to handle stuff that we
     * dynamically show and hide:
     * Attachment list,
     * Cc field,
     * Bcc field,
     * Quoted text,
     */
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        saveIfNeeded();
        ArrayList<Uri> attachments = new ArrayList<Uri>();
        for (int i = 0, count = mAttachments.getChildCount(); i < count; i++)
        {
            View view = mAttachments.getChildAt(i);
            Attachment attachment = (Attachment) view.getTag();
            attachments.add(attachment.uri);
        }
        outState.putParcelableArrayList(STATE_KEY_ATTACHMENTS, attachments);
        outState.putBoolean(STATE_KEY_CC_SHOWN, mCcView.getVisibility() == View.VISIBLE);
        outState.putBoolean(STATE_KEY_BCC_SHOWN, mBccView.getVisibility() == View.VISIBLE);
        outState.putBoolean(STATE_KEY_QUOTED_TEXT_SHOWN, mQuotedTextBar.getVisibility() == View.VISIBLE);
        outState.putBoolean(STATE_KEY_SOURCE_MESSAGE_PROCED, mSourceMessageProcessed);
        outState.putString(STATE_KEY_DRAFT_UID, mDraftUid);
        outState.putSerializable(STATE_IDENTITY, mIdentity);
        outState.putBoolean(STATE_IDENTITY_CHANGED, mIdentityChanged);
        outState.putSerializable(STATE_PGP_DATA, mPgpData);
        outState.putString(STATE_IN_REPLY_TO, mInReplyTo);
        outState.putString(STATE_REFERENCES, mReferences);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        ArrayList<Parcelable> attachments = (ArrayList<Parcelable>) savedInstanceState.getParcelableArrayList(STATE_KEY_ATTACHMENTS);
        mAttachments.removeAllViews();
        for (Parcelable p : attachments)
        {
            Uri uri = (Uri) p;
            addAttachment(uri);
        }

        mCcView.setVisibility(savedInstanceState.getBoolean(STATE_KEY_CC_SHOWN) ?  View.VISIBLE : View.GONE);
        mBccView.setVisibility(savedInstanceState.getBoolean(STATE_KEY_BCC_SHOWN) ?  View.VISIBLE : View.GONE);
        mQuotedTextBar.setVisibility(savedInstanceState.getBoolean(STATE_KEY_QUOTED_TEXT_SHOWN) ?  View.VISIBLE : View.GONE);
        mQuotedText.setVisibility(savedInstanceState.getBoolean(STATE_KEY_QUOTED_TEXT_SHOWN) ?  View.VISIBLE : View.GONE);
        mDraftUid = savedInstanceState.getString(STATE_KEY_DRAFT_UID);
        mIdentity = (Identity)savedInstanceState.getSerializable(STATE_IDENTITY);
        mIdentityChanged = savedInstanceState.getBoolean(STATE_IDENTITY_CHANGED);
        mPgpData = (PgpData) savedInstanceState.getSerializable(STATE_PGP_DATA);
        mInReplyTo = savedInstanceState.getString(STATE_IN_REPLY_TO);
        mReferences = savedInstanceState.getString(STATE_REFERENCES);

        initializeCrypto();
        updateFrom();
        updateSignature();
        updateEncryptLayout();

        mDraftNeedsSaving = false;
    }

    private void updateTitle()
    {
        if (mSubjectView.getText().length() == 0)
        {
            setTitle(R.string.compose_title);
        }
        else
        {
            setTitle(mSubjectView.getText().toString());
        }
    }

    public void onFocusChange(View view, boolean focused)
    {
        if (!focused)
        {
            updateTitle();
        }
    }

    private void addAddresses(MultiAutoCompleteTextView view, Address[] addresses)
    {
        if (addresses == null)
        {
            return;
        }
        for (Address address : addresses)
        {
            addAddress(view, address);
        }
    }

    private void addAddress(MultiAutoCompleteTextView view, Address address)
    {
        view.append(address + ", ");
    }

    private Address[] getAddresses(MultiAutoCompleteTextView view)
    {

        return Address.parseUnencoded(view.getText().toString().trim());
    }

    /*
     * Build the Body that will contain the text of the message. We'll decide where to
     * include it later.
     *
     * @param appendSig If true, append the user's signature to the message.
     */
    private String buildText(boolean appendSig)
    {
        boolean replyAfterQuote = false;
        String action = getIntent().getAction();
        if (mAccount.isReplyAfterQuote() &&
                (ACTION_REPLY.equals(action) || ACTION_REPLY_ALL.equals(action)))
        {
            replyAfterQuote = true;
        }

        String text = mMessageContentView.getText().toString();
        // Placing the signature before the quoted text does not make sense if replyAfterQuote is true.
        if (!replyAfterQuote && appendSig && mAccount.isSignatureBeforeQuotedText())
        {
            text = appendSignature(text);
        }

        if (mQuotedTextBar.getVisibility() == View.VISIBLE)
        {
            if (replyAfterQuote)
            {
                text = mQuotedText.getText().toString() + "\n" + text;
            }
            else
            {
                text += "\n\n" + mQuotedText.getText().toString();
            }
        }

        // Note: If user has selected reply after quote AND signature before quote, ignore the
        // latter setting and append the signature at the end.
        if (appendSig && (!mAccount.isSignatureBeforeQuotedText() || replyAfterQuote))
        {
            text = appendSignature(text);
        }

        return text;
    }

    private MimeMessage createMessage(boolean appendSig) throws MessagingException
    {
        MimeMessage message = new MimeMessage();
        message.addSentDate(new Date());
        Address from = new Address(mIdentity.getEmail(), mIdentity.getName());
        message.setFrom(from);
        message.setRecipients(RecipientType.TO, getAddresses(mToView));
        message.setRecipients(RecipientType.CC, getAddresses(mCcView));
        message.setRecipients(RecipientType.BCC, getAddresses(mBccView));
        message.setSubject(mSubjectView.getText().toString());
        message.setHeader("X-User-Agent", getString(R.string.message_header_mua));

        final String replyTo = mIdentity.getReplyTo();
        if (replyTo != null)
        {
            message.setReplyTo(new Address[] { new Address(replyTo) });
        }

        if (mInReplyTo != null)
        {
            message.setInReplyTo(mInReplyTo);
        }

        if (mReferences != null)
        {
            message.setReferences(mReferences);
        }

        String text = null;
        if (mPgpData.getEncryptedData() != null)
        {
            text = mPgpData.getEncryptedData();
        }
        else
        {
            text = buildText(appendSig);
        }

        TextBody body = new TextBody(text);

        if (mAttachments.getChildCount() > 0)
        {
            /*
             * The message has attachments that need to be included. First we add the part
             * containing the text that will be sent and then we include each attachment.
             */

            MimeMultipart mp;

            mp = new MimeMultipart();
            mp.addBodyPart(new MimeBodyPart(body, "text/plain"));

            for (int i = 0, count = mAttachments.getChildCount(); i < count; i++)
            {
                Attachment attachment = (Attachment) mAttachments.getChildAt(i).getTag();

                MimeBodyPart bp = new MimeBodyPart(
                    new LocalStore.LocalAttachmentBody(attachment.uri, getApplication()));

                /*
                 * Correctly encode the filename here. Otherwise the whole
                 * header value (all parameters at once) will be encoded by
                 * MimeHeader.writeTo().
                 */
                bp.addHeader(MimeHeader.HEADER_CONTENT_TYPE, String.format("%s;\n name=\"%s\"",
                             attachment.contentType,
                             EncoderUtil.encodeIfNecessary(attachment.name,
                                     EncoderUtil.Usage.WORD_ENTITY, 7)));

                bp.addHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING, "base64");

                /*
                 * TODO: Oh the joys of MIME...
                 *
                 * From RFC 2183 (The Content-Disposition Header Field):
                 * "Parameter values longer than 78 characters, or which
                 *  contain non-ASCII characters, MUST be encoded as specified
                 *  in [RFC 2184]."
                 *
                 * Example:
                 *
                 * Content-Type: application/x-stuff
                 *  title*1*=us-ascii'en'This%20is%20even%20more%20
                 *  title*2*=%2A%2A%2Afun%2A%2A%2A%20
                 *  title*3="isn't it!"
                 */
                bp.addHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, String.format(
                                 "attachment;\n filename=\"%s\";\n size=%d",
                                 attachment.name, attachment.size));

                mp.addBodyPart(bp);
            }

            message.setBody(mp);
        }
        else
        {
            /*
             * No attachments to include, just stick the text body in the message and call
             * it good.
             */
            message.setBody(body);
        }

        return message;
    }

    private String appendSignature(String text)
    {
        if (mIdentity.getSignatureUse())
        {
            String signature = mSignatureView.getText().toString();

            if (signature != null && !signature.contentEquals(""))
            {
                text += "\n" + signature;
            }
        }

        return text;
    }


    private void sendMessage()
    {
        new SendMessageTask().execute();
    }
    private void saveMessage()
    {
        new SaveMessageTask().execute();
    }

    private void saveIfNeeded()
    {
        if (!mDraftNeedsSaving || mPreventDraftSaving || mPgpData.hasEncryptionKeys())
        {
            return;
        }

        mDraftNeedsSaving = false;
        saveMessage();
    }

    public void onEncryptionKeySelectionDone()
    {
        if (mPgpData.hasEncryptionKeys())
        {
            onSend();
        }
        else
        {
            Toast.makeText(this, R.string.send_aborted, Toast.LENGTH_SHORT).show();
        }
    }

    public void onEncryptDone()
    {
        if (mPgpData.getEncryptedData() != null)
        {
            onSend();
        }
        else
        {
            Toast.makeText(this, R.string.send_aborted, Toast.LENGTH_SHORT).show();
        }
    }

    private void onSend()
    {
        if (getAddresses(mToView).length == 0 && getAddresses(mCcView).length == 0 && getAddresses(mBccView).length == 0)
        {
            mToView.setError(getString(R.string.message_compose_error_no_recipients));
            Toast.makeText(this, getString(R.string.message_compose_error_no_recipients), Toast.LENGTH_LONG).show();
            return;
        }
        if (mEncryptCheckbox.isChecked() && !mPgpData.hasEncryptionKeys())
        {
            // key selection before encryption
            String emails = "";
            Address[][] addresses = new Address[][] { getAddresses(mToView),
                    getAddresses(mCcView),
                    getAddresses(mBccView)
                                                    };
            for (Address[] addressArray : addresses)
            {
                for (Address address : addressArray)
                {
                    if (emails.length() != 0)
                    {
                        emails += ",";
                    }
                    emails += address.getAddress();
                }
            }
            if (emails.length() != 0)
            {
                emails += ",";
            }
            emails += mIdentity.getEmail();

            mPreventDraftSaving = true;
            if (!mAccount.getCryptoProvider().selectEncryptionKeys(MessageCompose.this, emails, mPgpData))
            {
                mPreventDraftSaving = false;
            }
            return;
        }
        if (mPgpData.hasEncryptionKeys() || mPgpData.hasSignatureKey())
        {
            if (mPgpData.getEncryptedData() == null)
            {
                String text = buildText(true);
                mPreventDraftSaving = true;
                if (!mAccount.getCryptoProvider().encrypt(this, text, mPgpData))
                {
                    mPreventDraftSaving = false;
                }
                return;
            }
        }
        sendMessage();
        mDraftNeedsSaving = false;
        finish();
    }

    private void onDiscard()
    {
        if (mDraftUid != null)
        {
            MessagingController.getInstance(getApplication()).deleteDraft(mAccount, mDraftUid);
            mDraftUid = null;
        }
        mHandler.sendEmptyMessage(MSG_DISCARDED_DRAFT);
        mDraftNeedsSaving = false;
        finish();
    }

    private void onSave()
    {
        mDraftNeedsSaving = true;
        saveIfNeeded();
        finish();
    }

    private void onAddCcBcc()
    {
        mCcView.setVisibility(View.VISIBLE);
        mBccView.setVisibility(View.VISIBLE);
    }

    /**
     * Kick off a picker for whatever kind of MIME types we'll accept and let Android take over.
     */
    private void onAddAttachment()
    {
        if (K9.isGalleryBuggy())
        {
            if (K9.useGalleryBugWorkaround())
            {
                Toast.makeText(MessageCompose.this,
                               getString(R.string.message_compose_use_workaround),
                               Toast.LENGTH_LONG).show();
            }
            else
            {
                Toast.makeText(MessageCompose.this,
                               getString(R.string.message_compose_buggy_gallery),
                               Toast.LENGTH_LONG).show();
            }
        }

        onAddAttachment2("*/*");
    }

    /**
     * Kick off a picker for the specified MIME type and let Android take over.
     */
    private void onAddAttachment2(final String mime_type)
    {
        if (mAccount.getCryptoProvider().isAvailable(this))
        {
            Toast.makeText(this, R.string.attachment_encryption_unsupported, Toast.LENGTH_LONG).show();
        }
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(mime_type);
        startActivityForResult(Intent.createChooser(i, null), ACTIVITY_REQUEST_PICK_ATTACHMENT);
    }

    private void addAttachment(Uri uri)
    {
        addAttachment(uri, null);
    }

    private void addAttachment(Uri uri, String contentType)
    {
        long size = -1;
        String name = null;

        ContentResolver contentResolver = getContentResolver();

        Cursor metadataCursor = contentResolver.query(
                                    uri,
                                    new String[] { OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE },
                                    null,
                                    null,
                                    null);

        if (metadataCursor != null)
        {
            try
            {
                if (metadataCursor.moveToFirst())
                {
                    name = metadataCursor.getString(0);
                    size = metadataCursor.getInt(1);
                }
            }
            finally
            {
                metadataCursor.close();
            }
        }

        if (name == null)
        {
            name = uri.getLastPathSegment();
        }

        if ((contentType == null) || (contentType.indexOf('*') != -1))
        {
            contentType = contentResolver.getType(uri);
        }
        if (contentType == null)
        {
            contentType = MimeUtility.getMimeTypeByExtension(name);
        }

        if (size <= 0)
        {
            String uriString = uri.toString();
            if (uriString.startsWith("file://"))
            {
                Log.v(K9.LOG_TAG, uriString.substring("file://".length()));
                File f = new File(uriString.substring("file://".length()));
                size = f.length();
            }
            else
            {
                Log.v(K9.LOG_TAG, "Not a file: " + uriString);
            }
        }
        else
        {
            Log.v(K9.LOG_TAG, "old attachment.size: " + size);
        }
        Log.v(K9.LOG_TAG, "new attachment.size: " + size);

        Attachment attachment = new Attachment();
        attachment.uri = uri;
        attachment.contentType = contentType;
        attachment.name = name;
        attachment.size = size;

        View view = getLayoutInflater().inflate(R.layout.message_compose_attachment, mAttachments, false);
        TextView nameView = (TextView)view.findViewById(R.id.attachment_name);
        ImageButton delete = (ImageButton)view.findViewById(R.id.attachment_delete);
        nameView.setText(attachment.name);
        delete.setOnClickListener(this);
        delete.setTag(view);
        view.setTag(attachment);
        mAttachments.addView(view);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // if a CryptoSystem activity is returning, then mPreventDraftSaving was set to true
        mPreventDraftSaving = false;

        if (mAccount.getCryptoProvider().onActivityResult(this, requestCode, resultCode, data, mPgpData))
        {
            return;
        }

        if (resultCode != RESULT_OK)
            return;
        if (data == null)
        {
            return;
        }
        switch (requestCode)
        {
            case ACTIVITY_REQUEST_PICK_ATTACHMENT:
                addAttachment(data.getData());
                mDraftNeedsSaving = true;
                break;
            case ACTIVITY_CHOOSE_IDENTITY:
                onIdentityChosen(data);
                break;
            case ACTIVITY_CHOOSE_ACCOUNT:
                onAccountChosen(data);
                break;
        }
    }

    private void onAccountChosen(final Intent intent)
    {
        final Bundle extras = intent.getExtras();
        final String uuid = extras.getString(ChooseAccount.EXTRA_ACCOUNT);
        final Identity identity = (Identity) extras.getSerializable(ChooseAccount.EXTRA_IDENTITY);

        final Account account = Preferences.getPreferences(this).getAccount(uuid);

        if (!mAccount.equals(account))
        {
            if (K9.DEBUG)
            {
                Log.v(K9.LOG_TAG, "Switching account from " + mAccount + " to " + account);
            }

            // on draft edit, make sure we don't keep previous message UID
            if (ACTION_EDIT_DRAFT.equals(getIntent().getAction()))
            {
                mMessageReference = null;
            }

            // test whether there is something to save
            if (mDraftNeedsSaving || (mDraftUid != null))
            {
                final String previousDraftUid = mDraftUid;
                final Account previousAccount = mAccount;

                // make current message appear as new
                mDraftUid = null;

                // actual account switch
                mAccount = account;

                if (K9.DEBUG)
                {
                    Log.v(K9.LOG_TAG, "Account switch, saving new draft in new account");
                }
                saveMessage();

                if (previousDraftUid != null)
                {
                    if (K9.DEBUG)
                    {
                        Log.v(K9.LOG_TAG, "Account switch, deleting draft from previous account: "
                              + previousDraftUid);
                    }
                    MessagingController.getInstance(getApplication()).deleteDraft(previousAccount,
                            previousDraftUid);
                }
            }
            else
            {
                mAccount = account;
            }
            // not sure how to handle mFolder, mSourceMessage?
        }

        switchToIdentity(identity);
    }

    private void onIdentityChosen(Intent intent)
    {
        Bundle bundle = intent.getExtras();
        switchToIdentity((Identity)bundle.getSerializable(ChooseIdentity.EXTRA_IDENTITY));
    }

    private void switchToIdentity(Identity identity)
    {
        mIdentity = identity;
        mIdentityChanged = true;
        mDraftNeedsSaving = true;
        updateFrom();
        updateSignature();
    }

    private void updateFrom()
    {
        if (mIdentityChanged)
        {
            mFromView.setVisibility(View.VISIBLE);
        }
        mFromView.setText(getString(R.string.message_view_from_format, mIdentity.getName(), mIdentity.getEmail()));
    }

    private void updateSignature()
    {
        if (mIdentity.getSignatureUse())
        {
            mSignatureView.setText(mIdentity.getSignature());
            mSignatureView.setVisibility(View.VISIBLE);
        }
        else
        {
            mSignatureView.setVisibility(View.GONE);
        }
    }

    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.attachment_delete:
                /*
                 * The view is the delete button, and we have previously set the tag of
                 * the delete button to the view that owns it. We don't use parent because the
                 * view is very complex and could change in the future.
                 */
                mAttachments.removeView((View) view.getTag());
                mDraftNeedsSaving = true;
                break;
            case R.id.quoted_text_delete:
                mQuotedTextBar.setVisibility(View.GONE);
                mQuotedText.setVisibility(View.GONE);
                mDraftNeedsSaving = true;
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.send:
                mPgpData.setEncryptionKeys(null);
                onSend();
                break;
            case R.id.save:
                onSave();
                break;
            case R.id.discard:
                onDiscard();
                break;
            case R.id.add_cc_bcc:
                onAddCcBcc();
                break;
            case R.id.add_attachment:
                onAddAttachment();
                break;
            case R.id.add_attachment_image:
                onAddAttachment2("image/*");
                break;
            case R.id.add_attachment_video:
                onAddAttachment2("video/*");
                break;
            case R.id.choose_identity:
                onChooseIdentity();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void onChooseIdentity()
    {
        // keep things simple: trigger account choice only if there are more
        // than 1 account
        if (Preferences.getPreferences(this).getAvailableAccounts().size() > 1)
        {
            final Intent intent = new Intent(this, ChooseAccount.class);
            intent.putExtra(ChooseAccount.EXTRA_ACCOUNT, mAccount.getUuid());
            intent.putExtra(ChooseAccount.EXTRA_IDENTITY, mIdentity);
            startActivityForResult(intent, ACTIVITY_CHOOSE_ACCOUNT);
        }
        else if (mAccount.getIdentities().size() > 1)
        {
            Intent intent = new Intent(this, ChooseIdentity.class);
            intent.putExtra(ChooseIdentity.EXTRA_ACCOUNT, mAccount.getUuid());
            startActivityForResult(intent, ACTIVITY_CHOOSE_IDENTITY);
        }
        else
        {
            Toast.makeText(this, getString(R.string.no_identities),
                           Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.message_compose_option, menu);

        /*
         * Show the menu items "Add attachment (Image)" and "Add attachment (Video)"
         * if the work-around for the Gallery bug is enabled (see Issue 1186).
         */
        int found = 0;
        for (int i = menu.size() - 1; i >= 0; i--)
        {
            MenuItem item = menu.getItem(i);
            int id = item.getItemId();
            if ((id == R.id.add_attachment_image) ||
                    (id == R.id.add_attachment_video))
            {
                item.setVisible(K9.useGalleryBugWorkaround());
                found++;
            }

            // We found all the menu items we were looking for. So stop here.
            if (found == 2) break;
        }

        return true;
    }

    @Override
    public void onBackPressed()
    {
        // This will be called either automatically for you on 2.0
        // or later, or by the code above on earlier versions of the
        // platform.
        if (mDraftNeedsSaving)
        {
            showDialog(DIALOG_SAVE_OR_DISCARD_DRAFT_MESSAGE);
        }
        else
        {
            finish();
        }
    }

    @Override
    public Dialog onCreateDialog(int id)
    {
        switch (id)
        {
            case DIALOG_SAVE_OR_DISCARD_DRAFT_MESSAGE:
                return new AlertDialog.Builder(this)
                       .setTitle(R.string.save_or_discard_draft_message_dlg_title)
                       .setMessage(R.string.save_or_discard_draft_message_instructions_fmt)
                       .setPositiveButton(R.string.save_draft_action, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        dismissDialog(1);
                        onSave();
                    }
                })
                       .setNegativeButton(R.string.discard_action, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        dismissDialog(1);
                        onDiscard();
                    }
                })
                       .create();
        }
        return super.onCreateDialog(id);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (
            // TODO - when we move to android 2.0, uncomment this.
            // android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR &&

            keyCode == KeyEvent.KEYCODE_BACK
            && event.getRepeatCount() == 0
            && K9.manageBack())
        {
            // Take care of calling this method on earlier versions of
            // the platform where it doesn't exist.
            onBackPressed();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Returns true if all attachments were able to be attached, otherwise returns false.
     */
    private boolean loadAttachments(Part part, int depth) throws MessagingException
    {
        if (part.getBody() instanceof Multipart)
        {
            Multipart mp = (Multipart) part.getBody();
            boolean ret = true;
            for (int i = 0, count = mp.getCount(); i < count; i++)
            {
                if (!loadAttachments(mp.getBodyPart(i), depth + 1))
                {
                    ret = false;
                }
            }
            return ret;
        }
        else
        {
            String contentType = MimeUtility.unfoldAndDecode(part.getContentType());
            String name = MimeUtility.getHeaderParameter(contentType, "name");
            if (name != null)
            {
                Body body = part.getBody();
                if (body != null && body instanceof LocalAttachmentBody)
                {
                    final Uri uri = ((LocalAttachmentBody) body).getContentUri();
                    mHandler.post(new Runnable()
                    {
                        public void run()
                        {
                            addAttachment(uri);
                        }
                    });
                }
                else
                {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Pull out the parts of the now loaded source message and apply them to the new message
     * depending on the type of message being composed.
     * @param message Source message
     */
    private void processSourceMessage(Message message)
    {
        String action = getIntent().getAction();
        if (ACTION_REPLY.equals(action) || ACTION_REPLY_ALL.equals(action))
        {
            try
            {
                if (message.getSubject() != null)
                {
                    final String subject = prefix.matcher(message.getSubject()).replaceFirst("");

                    if (!subject.toLowerCase().startsWith("re:"))
                    {
                        mSubjectView.setText("Re: " + subject);
                    }
                    else
                    {
                        mSubjectView.setText(subject);
                    }
                }
                else
                {
                    mSubjectView.setText("");
                }

                /*
                 * If a reply-to was included with the message use that, otherwise use the from
                 * or sender address.
                 */
                Address[] replyToAddresses;
                if (message.getReplyTo().length > 0)
                {
                    replyToAddresses = message.getReplyTo();
                }
                else
                {
                    replyToAddresses = message.getFrom();
                }

                // if we're replying to a message we sent, we probably meant
                // to reply to the recipient of that message
                if (mAccount.isAnIdentity(replyToAddresses))
                {
                    replyToAddresses = message.getRecipients(RecipientType.TO);
                }

                addAddresses(mToView, replyToAddresses);



                if (message.getMessageId() != null && message.getMessageId().length() > 0)
                {
                    mInReplyTo = message.getMessageId();

                    if (message.getReferences() != null && message.getReferences().length > 0)
                    {
                        StringBuffer buffy = new StringBuffer();
                        for (int i=0; i < message.getReferences().length; i++)
                            buffy.append(message.getReferences()[i]);

                        mReferences = buffy.toString() + " " + mInReplyTo;
                    }
                    else
                    {
                        mReferences = mInReplyTo;
                    }

                }
                else
                {
                    if (K9.DEBUG)
                        Log.d(K9.LOG_TAG, "could not get Message-ID.");
                }

                Part part = MimeUtility.findFirstPartByMimeType(mSourceMessage,
                            "text/plain");
                if (part != null || mSourceMessageBody != null)
                {
                    String quotedText = String.format(
                                            getString(R.string.message_compose_reply_header_fmt),
                                            Address.toString(mSourceMessage.getFrom()));

                    final String prefix = mAccount.getQuotePrefix();
                    // "$" and "\" in the quote prefix have to be escaped for
                    // the replaceAll() invocation.
                    final String escapedPrefix = prefix.replaceAll("(\\\\|\\$)", "\\\\$1");

                    final String text = (mSourceMessageBody != null) ?
                                        mSourceMessageBody :
                                        MimeUtility.getTextFromPart(part);

                    final String wrappedText = Utility.wrap(text, REPLY_WRAP_LINE_WIDTH - prefix.length());

                    quotedText += wrappedText.replaceAll("(?m)^", escapedPrefix);

                    quotedText = quotedText.replaceAll("\\\r", "");
                    mQuotedText.setText(quotedText);

                    mQuotedTextBar.setVisibility(View.VISIBLE);
                    mQuotedText.setVisibility(View.VISIBLE);
                }

                if (ACTION_REPLY_ALL.equals(action) || ACTION_REPLY.equals(action))
                {
                    Identity useIdentity = null;
                    for (Address address : message.getRecipients(RecipientType.TO))
                    {
                        Identity identity = mAccount.findIdentity(address);
                        if (identity != null)
                        {
                            useIdentity = identity;
                            break;
                        }
                    }
                    if (useIdentity == null)
                    {
                        if (message.getRecipients(RecipientType.CC).length > 0)
                        {
                            for (Address address : message.getRecipients(RecipientType.CC))
                            {
                                Identity identity = mAccount.findIdentity(address);
                                if (identity != null)
                                {
                                    useIdentity = identity;
                                    break;
                                }
                            }
                        }
                    }
                    if (useIdentity != null)
                    {
                        Identity defaultIdentity = mAccount.getIdentity(0);
                        if (useIdentity != defaultIdentity)
                        {
                            switchToIdentity(useIdentity);
                        }
                    }
                }

                if (ACTION_REPLY_ALL.equals(action))
                {
                    for (Address address : message.getRecipients(RecipientType.TO))
                    {
                        if (!mAccount.isAnIdentity(address))
                        {
                            addAddress(mToView, address);
                        }

                    }
                    if (message.getRecipients(RecipientType.CC).length > 0)
                    {
                        for (Address address : message.getRecipients(RecipientType.CC))
                        {
                            if (!mAccount.isAnIdentity(address) && !Utility.arrayContains(replyToAddresses, address))
                            {
                                addAddress(mCcView, address);
                            }

                        }
                        mCcView.setVisibility(View.VISIBLE);
                    }
                }
            }
            catch (MessagingException me)
            {
                /*
                 * This really should not happen at this point but if it does it's okay.
                 * The user can continue composing their message.
                 */
            }
        }
        else if (ACTION_FORWARD.equals(action))
        {
            try
            {
                if (message.getSubject() != null && !message.getSubject().toLowerCase().startsWith("fwd:"))
                {
                    mSubjectView.setText("Fwd: " + message.getSubject());
                }
                else
                {
                    mSubjectView.setText(message.getSubject());
                }

                String quotedText = null;
                Part part = null;

                if ( mSourceMessageBody != null)
                {
                    quotedText = mSourceMessageBody;
                }

                if (quotedText == null)
                {
                    part =  MimeUtility.findFirstPartByMimeType(message, "text/plain");
                    if (part != null)
                    {
                        quotedText = MimeUtility.getTextFromPart(part);
                    }
                }
                if (quotedText == null)
                {
                    part =  MimeUtility.findFirstPartByMimeType(message, "text/html");
                    if (part != null)
                    {
                        quotedText = MimeUtility.getTextFromPart(part);
                        if (quotedText != null)
                        {
                            quotedText = (Html.fromHtml(quotedText)).toString();
                        }
                    }
                }


                if (quotedText != null)
                {
                    String text = String.format(
                                      getString(R.string.message_compose_fwd_header_fmt),
                                      mSourceMessage.getSubject(),
                                      Address.toString(mSourceMessage.getFrom()),
                                      Address.toString(
                                          mSourceMessage.getRecipients(RecipientType.TO)),
                                      Address.toString(
                                          mSourceMessage.getRecipients(RecipientType.CC)));
                    if (quotedText != null)
                    {

                        quotedText = quotedText.replaceAll("\\\r", "");
                        mQuotedText.setText(text);
                        mQuotedText.append(quotedText);
                    }
                    mQuotedTextBar.setVisibility(View.VISIBLE);
                    mQuotedText.setVisibility(View.VISIBLE);
                }
                if (!mSourceMessageProcessed)
                {
                    if (!loadAttachments(message, 0))
                    {
                        mHandler.sendEmptyMessage(MSG_SKIPPED_ATTACHMENTS);
                    }
                }
            }
            catch (MessagingException me)
            {
                /*
                 * This really should not happen at this point but if it does it's okay.
                 * The user can continue composing their message.
                 */
            }
        }
        else if (ACTION_EDIT_DRAFT.equals(action))
        {
            try
            {
                mDraftUid = message.getUid();
                mSubjectView.setText(message.getSubject());
                addAddresses(mToView, message.getRecipients(RecipientType.TO));
                if (message.getRecipients(RecipientType.CC).length > 0)
                {
                    addAddresses(mCcView, message.getRecipients(RecipientType.CC));
                    mCcView.setVisibility(View.VISIBLE);
                }
                if (message.getRecipients(RecipientType.BCC).length > 0)
                {
                    addAddresses(mBccView, message.getRecipients(RecipientType.BCC));
                    mBccView.setVisibility(View.VISIBLE);
                }

                // Read In-Reply-To header from draft
                final String[] inReplyTo = message.getHeader("In-Reply-To");
                if ((inReplyTo != null) && (inReplyTo.length >= 1))
                {
                    mInReplyTo = inReplyTo[0];
                }

                // Read References header from draft
                final String[] references = message.getHeader("References");
                if ((references != null) && (references.length >= 1))
                {
                    mReferences = references[0];
                }

                if (!mSourceMessageProcessed)
                {
                    loadAttachments(message, 0);
                }
                Integer bodyLength = null;
                String[] k9identities = message.getHeader(K9.K9MAIL_IDENTITY);
                if (k9identities != null && k9identities.length > 0)
                {
                    String k9identity = k9identities[0];

                    if (k9identity != null)
                    {
                        if (K9.DEBUG)
                            Log.d(K9.LOG_TAG, "Got a saved identity: " + k9identity);
                        StringTokenizer tokens = new StringTokenizer(k9identity, ":", false);

                        String bodyLengthS = null;
                        String name = null;
                        String email = null;
                        String signature = null;
                        boolean signatureUse = message.getFolder().getAccount().getSignatureUse();
                        if (tokens.hasMoreTokens())
                        {
                            bodyLengthS = Utility.base64Decode(tokens.nextToken());
                            try
                            {
                                bodyLength = Integer.parseInt(bodyLengthS);
                            }
                            catch (Exception e)
                            {
                                Log.e(K9.LOG_TAG, "Unable to parse bodyLength '" + bodyLengthS + "'");
                            }
                        }
                        if (tokens.hasMoreTokens())
                        {
                            signatureUse = true;
                            signature = Utility.base64Decode(tokens.nextToken());
                        }
                        if (tokens.hasMoreTokens())
                        {
                            name = Utility.base64Decode(tokens.nextToken());
                        }
                        if (tokens.hasMoreTokens())
                        {
                            email = Utility.base64Decode(tokens.nextToken());
                        }

                        Identity newIdentity = new Identity();
                        newIdentity.setSignatureUse(signatureUse);
                        if (signature != null)
                        {
                            newIdentity.setSignature(signature);
                            mSignatureChanged = true;
                        }
                        else
                        {
                            newIdentity.setSignature(mIdentity.getSignature());
                        }

                        if (name != null)
                        {
                            newIdentity.setName(name);
                            mIdentityChanged = true;
                        }
                        else
                        {
                            newIdentity.setName(mIdentity.getName());
                        }

                        if (email != null)
                        {
                            newIdentity.setEmail(email);
                            mIdentityChanged = true;
                        }
                        else
                        {
                            newIdentity.setEmail(mIdentity.getEmail());
                        }

                        mIdentity = newIdentity;

                        updateSignature();
                        updateFrom();

                    }
                }
                Part part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
                if (part != null)
                {
                    String text = MimeUtility.getTextFromPart(part);
                    if (bodyLength != null && bodyLength + 1 < text.length())   // + 1 to get rid of the newline we added when saving the draft
                    {
                        String bodyText = text.substring(0, bodyLength);
                        String quotedText = text.substring(bodyLength + 1, text.length());

                        mMessageContentView.setText(bodyText);
                        mQuotedText.setText(quotedText);

                        mQuotedTextBar.setVisibility(View.VISIBLE);
                        mQuotedText.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        mMessageContentView.setText(text);
                    }
                }
            }
            catch (MessagingException me)
            {
                // TODO
            }
        }
        mSourceMessageProcessed = true;
        mDraftNeedsSaving = false;
    }

    class Listener extends MessagingListener
    {
        @Override
        public void loadMessageForViewStarted(Account account, String folder, String uid)
        {
            if ((mMessageReference == null) || !mMessageReference.uid.equals(uid))
            {
                return;
            }

            mHandler.sendEmptyMessage(MSG_PROGRESS_ON);
        }

        @Override
        public void loadMessageForViewFinished(Account account, String folder, String uid, Message message)
        {
            if ((mMessageReference == null) || !mMessageReference.uid.equals(uid))
            {
                return;
            }

            mHandler.sendEmptyMessage(MSG_PROGRESS_OFF);
        }

        @Override
        public void loadMessageForViewBodyAvailable(Account account, String folder, String uid, final Message message)
        {
            if ((mMessageReference == null) || !mMessageReference.uid.equals(uid))
            {
                return;
            }

            mSourceMessage = message;
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    processSourceMessage(message);
                }
            });
        }

        @Override
        public void loadMessageForViewFailed(Account account, String folder, String uid, Throwable t)
        {
            if ((mMessageReference == null) || !mMessageReference.uid.equals(uid))
            {
                return;
            }
            mHandler.sendEmptyMessage(MSG_PROGRESS_OFF);
            // TODO show network error
        }

        @Override
        public void messageUidChanged(Account account, String folder, String oldUid, String newUid)
        {
            //TODO: is this really necessary here? mDraftUid is update after the call to MessagingController.saveDraft()
            // Track UID changes of the draft message
            if (account.equals(mAccount) &&
                    folder.equals(mAccount.getDraftsFolderName()) &&
                    oldUid.equals(mDraftUid))
            {
                mDraftUid = newUid;
            }

            // Track UID changes of the source message
            if (mMessageReference != null)
            {
                final Account sourceAccount = Preferences.getPreferences(MessageCompose.this).getAccount(mMessageReference.accountUuid);
                final String sourceFolder = mMessageReference.folderName;
                final String sourceMessageUid = mMessageReference.uid;

                if (account.equals(sourceAccount) && (folder.equals(sourceFolder)))
                {
                    if (oldUid.equals(sourceMessageUid))
                    {
                        mMessageReference.uid = newUid;
                    }
                    if ((mSourceMessage != null) && (oldUid.equals(mSourceMessage.getUid())))
                    {
                        mSourceMessage.setUid(newUid);
                    }
                }
            }
        }
    }

    /**
     * When we are launched with an intent that includes a mailto: URI, we can actually
     * gather quite a few of our message fields from it.
     */
    private void initializeFromMailto(Uri mailtoUri)
    {
        String schemaSpecific = mailtoUri.getSchemeSpecificPart();
        int end = schemaSpecific.indexOf('?');
        if (end == -1)
        {
            end = schemaSpecific.length();
        }

        // Extract the recipient's email address from the mailto URI if there's one.
        String recipient = Uri.decode(schemaSpecific.substring(0, end));

        /*
         * mailto URIs are not hierarchical. So calling getQueryParameters()
         * will throw an UnsupportedOperationException. We avoid this by
         * creating a new hierarchical dummy Uri object with the query
         * parameters of the original URI.
         */
        Uri uri = Uri.parse("foo://bar?" + mailtoUri.getEncodedQuery());

        // Read additional recipients from the "to" parameter.
        List<String> to = uri.getQueryParameters("to");
        if (recipient.length() != 0)
        {
            to = new ArrayList<String>(to);
            to.add(0, recipient);
        }
        setRecipients(mToView, to);

        // Read carbon copy recipients from the "cc" parameter.
        boolean ccOrBcc = setRecipients(mCcView, uri.getQueryParameters("cc"));

        // Read blind carbon copy recipients from the "bcc" parameter.
        ccOrBcc |= setRecipients(mBccView, uri.getQueryParameters("bcc"));

        if (ccOrBcc)
        {
            // Display CC and BCC text fields if CC or BCC recipients were set by the intent.
            onAddCcBcc();
        }

        // Read subject from the "subject" parameter.
        List<String> subject = uri.getQueryParameters("subject");
        if (subject.size() > 0)
        {
            mSubjectView.setText(subject.get(0));
        }

        // Read message body from the "body" parameter.
        List<String> body = uri.getQueryParameters("body");
        if (body.size() > 0)
        {
            mMessageContentView.setText(body.get(0));
        }
    }

    private class SendMessageTask extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... params)
        {
            /*
             * Create the message from all the data the user has entered.
             */
            MimeMessage message;
            try
            {
                message = createMessage(true);  // Only append sig on save
            }
            catch (MessagingException me)
            {
                Log.e(K9.LOG_TAG, "Failed to create new message for send or save.", me);
                throw new RuntimeException("Failed to create a new message for send or save.", me);
            }

            try
            {
                final Contacts contacts = Contacts.getInstance(MessageCompose.this);
                contacts.markAsContacted(message.getRecipients(RecipientType.TO));
                contacts.markAsContacted(message.getRecipients(RecipientType.CC));
                contacts.markAsContacted(message.getRecipients(RecipientType.BCC));
            }
            catch (Exception e)
            {
                Log.e(K9.LOG_TAG, "Failed to mark contact as contacted.", e);
            }

            MessagingController.getInstance(getApplication()).sendMessage(mAccount, message, null);
            if (mDraftUid != null)
            {
                MessagingController.getInstance(getApplication()).deleteDraft(mAccount, mDraftUid);
                mDraftUid = null;
            }

            return null;
        }
    }

    private class SaveMessageTask extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... params)
        {

            /*
             * Create the message from all the data the user has entered.
             */
            MimeMessage message;
            try
            {
                message = createMessage(false);  // Only append sig on save
            }
            catch (MessagingException me)
            {
                Log.e(K9.LOG_TAG, "Failed to create new message for send or save.", me);
                throw new RuntimeException("Failed to create a new message for send or save.", me);
            }

            /*
             * Save a draft
             */
            if (mDraftUid != null)
            {
                message.setUid(mDraftUid);
            }
            else if (ACTION_EDIT_DRAFT.equals(getIntent().getAction()))
            {
                /*
                 * We're saving a previously saved draft, so update the new message's uid
                 * to the old message's uid.
                 */
                if (mMessageReference != null)
                {
                    message.setUid(mMessageReference.uid);
                }
            }

            String k9identity = Utility.base64Encode("" + mMessageContentView.getText().toString().length());

            if (mIdentityChanged || mSignatureChanged)
            {
                String signature  = mSignatureView.getText().toString();
                k9identity += ":" + Utility.base64Encode(signature);
                if (mIdentityChanged)
                {

                    String name = mIdentity.getName();
                    String email = mIdentity.getEmail();

                    k9identity +=  ":" + Utility.base64Encode(name) + ":" + Utility.base64Encode(email);
                }
            }

            final MessagingController messagingController = MessagingController.getInstance(getApplication());

            if (K9.DEBUG)
            {
                Log.d(K9.LOG_TAG, "Saving identity: " + k9identity);
            }
            try
            {
                message.addHeader(K9.K9MAIL_IDENTITY, k9identity);
            }
            catch (UnavailableStorageException e)
            {
                messagingController.addErrorMessage(mAccount, "Unable to save identity", e);
            }

            Message draftMessage = messagingController.saveDraft(mAccount, message);
            mDraftUid = draftMessage.getUid();

            // Don't display the toast if the user is just changing the orientation
            if ((getChangingConfigurations() & ActivityInfo.CONFIG_ORIENTATION) == 0)
            {
                mHandler.sendEmptyMessage(MSG_SAVED_DRAFT);
            }
            return null;
        }
    }

}
