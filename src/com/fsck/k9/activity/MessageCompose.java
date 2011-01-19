
package com.fsck.k9.activity;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.*;
import android.webkit.WebViewClient;
import com.fsck.k9.helper.HtmlConverter;
import com.fsck.k9.mail.*;
import com.fsck.k9.view.MessageWebView;
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
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.webkit.WebView;
import android.widget.AutoCompleteTextView.Validator;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.Account.QuoteStyle;
import com.fsck.k9.Account.MessageFormat;
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
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.mail.store.LocalStore.LocalAttachmentBody;

public class MessageCompose extends K9Activity implements OnClickListener, OnFocusChangeListener
{
    private static final int DIALOG_SAVE_OR_DISCARD_DRAFT_MESSAGE = 1;

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
    private static final String STATE_KEY_HTML_QUOTE = "com.fsck.k9.activity.MessageCompose.HTMLQuote";
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
    private MessageWebView mQuotedHTML;
    private InsertableHtmlContent mQuotedHtmlContent;   // Container for HTML reply as it's being built.
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
        mSourceMessageBody = intent.getStringExtra(EXTRA_MESSAGE_BODY);

        if(K9.DEBUG && mSourceMessageBody != null)
            Log.d(K9.LOG_TAG, "Composing message with explicitly specified message body.");

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
        mMessageContentView.getInputExtras(true).putBoolean("allowEmoji", true);
        mAttachments = (LinearLayout)findViewById(R.id.attachments);
        mQuotedTextBar = findViewById(R.id.quoted_text_bar);
        mQuotedTextDelete = (ImageButton)findViewById(R.id.quoted_text_delete);
        mQuotedText = (EditText)findViewById(R.id.quoted_text);
        mQuotedText.getInputExtras(true).putBoolean("allowEmoji", true);

        mQuotedHTML = (MessageWebView) findViewById(R.id.quoted_html);
        mQuotedHTML.configure();
        // Disable the ability to click links in the quoted HTML page. I think this is a nice feature, but if someone
        // feels this should be a preference (or should go away all together), I'm ok with that too. -achen 20101130
        mQuotedHTML.setWebViewClient(new WebViewClient()
        {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                return true;
            }
        });

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
        mQuotedHTML.setVisibility(View.GONE);

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
             * This data gets used in onCreate, so grab it here instead of onRestoreInstanceState
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

        mEncryptLayout = findViewById(R.id.layout_encrypt);
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
        outState.putSerializable(STATE_KEY_HTML_QUOTE, mQuotedHtmlContent);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        ArrayList<Parcelable> attachments = savedInstanceState.getParcelableArrayList(STATE_KEY_ATTACHMENTS);
        mAttachments.removeAllViews();
        for (Parcelable p : attachments)
        {
            Uri uri = (Uri) p;
            addAttachment(uri);
        }

        mCcView.setVisibility(savedInstanceState.getBoolean(STATE_KEY_CC_SHOWN) ? View.VISIBLE : View.GONE);
        mBccView.setVisibility(savedInstanceState.getBoolean(STATE_KEY_BCC_SHOWN) ? View.VISIBLE : View.GONE);
        if (mAccount.getMessageFormat() == MessageFormat.HTML)
        {
            mQuotedHtmlContent = (InsertableHtmlContent) savedInstanceState.getSerializable(STATE_KEY_HTML_QUOTE);
            mQuotedTextBar.setVisibility(savedInstanceState.getBoolean(STATE_KEY_QUOTED_TEXT_SHOWN) ? View.VISIBLE : View.GONE);
            mQuotedHTML.setVisibility(savedInstanceState.getBoolean(STATE_KEY_QUOTED_TEXT_SHOWN) ? View.VISIBLE : View.GONE);
            if (mQuotedHtmlContent.getQuotedContent() != null)
            {
                mQuotedHTML.loadDataWithBaseURL("http://", mQuotedHtmlContent.getQuotedContent(), "text/html", "utf-8", null);
            }
        }
        else
        {
            mQuotedTextBar.setVisibility(savedInstanceState.getBoolean(STATE_KEY_QUOTED_TEXT_SHOWN) ? View.VISIBLE : View.GONE);
            mQuotedText.setVisibility(savedInstanceState.getBoolean(STATE_KEY_QUOTED_TEXT_SHOWN) ? View.VISIBLE : View.GONE);
        }
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
     * include it later. Draft messages are treated somewhat differently in that signatures are not
     * appended and HTML separators between composed text and quoted text are not added.
     * @param isDraft If we should build a message that will be saved as a draft (as opposed to sent).
     */
    private TextBody buildText(boolean isDraft)
    {
        boolean replyAfterQuote = false;
        String action = getIntent().getAction();
        if (mAccount.isReplyAfterQuote() &&
                (ACTION_REPLY.equals(action) || ACTION_REPLY_ALL.equals(action)))
        {
            replyAfterQuote = true;
        }

        String text = mMessageContentView.getText().toString();

        // Handle HTML separate from the rest of the text content. HTML mode doesn't allow signature after the quoted
        // text, nor does it allow reply after quote. Users who want that functionality will need to stick with text
        // mode.
        if (mAccount.getMessageFormat() == MessageFormat.HTML)
        {
            // Add the signature.
            if (!isDraft)
            {
                text = appendSignature(text);
            }
            text = HtmlConverter.textToHtmlFragment(text);
            // Insert it into the existing content object.
            if (K9.DEBUG && mQuotedHtmlContent != null)
                Log.d(K9.LOG_TAG, "insertable: " + mQuotedHtmlContent.toDebugString());
            if (mQuotedHtmlContent != null)
            {
                // Remove the quoted part if it's no longer visible.
                if (mQuotedTextBar.getVisibility() != View.VISIBLE)
                {
                    mQuotedHtmlContent.clearQuotedContent();
                }

                // If we're building a message to be sent, add some extra separators between the
                // composed message and the quoted message.
                if (!isDraft)
                {
                    text += "<br>";
                }

                mQuotedHtmlContent.setUserContent(text);
                // All done.  Build the body.
                TextBody body = new TextBody(mQuotedHtmlContent.toString());
                // Save length of the body and its offset.  This is used when thawing drafts.
                body.setComposedMessageLength(text.length());
                body.setComposedMessageOffset(mQuotedHtmlContent.getHeaderInsertionPoint());
                return body;
            }
            else
            {
                TextBody body = new TextBody(text);
                body.setComposedMessageLength(text.length());
                // Not in reply to anything so the message starts at the beginning (0).
                body.setComposedMessageOffset(0);
                return body;
            }
        }
        else if (mAccount.getMessageFormat() == MessageFormat.TEXT)
        {
            // Capture composed message length before we start attaching quoted parts and signatures.
            Integer composedMessageLength = text.length();
            Integer composedMessageOffset = 0;

            // Placing the signature before the quoted text does not make sense if replyAfterQuote is true.
            if (!replyAfterQuote && !isDraft && mAccount.isSignatureBeforeQuotedText())
            {
                text = appendSignature(text);
            }

            if (mQuotedTextBar.getVisibility() == View.VISIBLE)
            {
                if (replyAfterQuote)
                {
                    composedMessageOffset = mQuotedText.getText().toString().length() + "\n".length();
                    text = mQuotedText.getText().toString() + "\n" + text;
                }
                else
                {
                    text += "\n\n" + mQuotedText.getText().toString();
                }
            }

            // Note: If user has selected reply after quote AND signature before quote, ignore the
            // latter setting and append the signature at the end.
            if (!isDraft && (!mAccount.isSignatureBeforeQuotedText() || replyAfterQuote))
            {
                text = appendSignature(text);
            }

            // Build the body.
            TextBody body = new TextBody(text);
            body.setComposedMessageLength(composedMessageLength);
            body.setComposedMessageOffset(composedMessageOffset);

            return body;
        }
        else
        {
            // Shouldn't happen.
            return new TextBody("");
        }
    }

    /**
     * Build the final message to be sent (or saved). If there is another message quoted in this one, it will be baked
     * into the final message here.
     * @param isDraft Indicates if this message is a draft or not. Drafts do not have signatures
     *  appended and have some extra metadata baked into their header for use during thawing.
     * @return Message to be sent.
     * @throws MessagingException
     */
    private MimeMessage createMessage(boolean isDraft) throws MessagingException
    {
        MimeMessage message = new MimeMessage();
        message.addSentDate(new Date());
        Address from = new Address(mIdentity.getEmail(), mIdentity.getName());
        message.setFrom(from);
        message.setRecipients(RecipientType.TO, getAddresses(mToView));
        message.setRecipients(RecipientType.CC, getAddresses(mCcView));
        message.setRecipients(RecipientType.BCC, getAddresses(mBccView));
        message.setSubject(mSubjectView.getText().toString());
        message.setHeader("User-Agent", getString(R.string.message_header_mua));

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

        // Build the body.
        // TODO FIXME - body can be either an HTML or Text part, depending on whether we're in HTML mode or not.  Should probably fix this so we don't mix up html and text parts.
        TextBody body = null;
        if (mPgpData.getEncryptedData() != null)
        {
            String text = mPgpData.getEncryptedData();
            body = new TextBody(text);
        }
        else
        {
            body = buildText(isDraft);
        }

        final boolean hasAttachments = mAttachments.getChildCount() > 0;

        if (mAccount.getMessageFormat() == MessageFormat.HTML)
        {
            // HTML message (with alternative text part)

            // This is the compiled MIME part for an HTML message.
            MimeMultipart composedMimeMessage = new MimeMultipart();
            composedMimeMessage.setSubType("alternative");   // Let the receiver select either the text or the HTML part.
            composedMimeMessage.addBodyPart(new MimeBodyPart(body, "text/html"));
            composedMimeMessage.addBodyPart(new MimeBodyPart(new TextBody(HtmlConverter.htmlToText(body.getText())), "text/plain"));

            if (hasAttachments)
            {
                // If we're HTML and have attachments, we have a MimeMultipart container to hold the
                // whole message (mp here), of which one part is a MimeMultipart container
                // (composedMimeMessage) with the user's composed messages, and subsequent parts for
                // the attachments.
                MimeMultipart mp = new MimeMultipart();
                mp.addBodyPart(new MimeBodyPart(composedMimeMessage));
                addAttachmentsToMessage(mp);
                message.setBody(mp);
            }
            else
            {
                // If no attachments, our multipart/alternative part is the only one we need.
                message.setBody(composedMimeMessage);
            }
        }
        else
        {
            // Text-only message.
            if (hasAttachments)
            {
                MimeMultipart mp = new MimeMultipart();
                mp.addBodyPart(new MimeBodyPart(body, "text/plain"));
                addAttachmentsToMessage(mp);
                message.setBody(mp);
            }
            else
            {
                // No attachments to include, just stick the text body in the message and call it good.
                message.setBody(body);
            }
        }

        // If this is a draft, add metadata for thawing.
        if (isDraft)
        {
            // Add the identity to the message.
            message.addHeader(K9.IDENTITY_HEADER, buildIdentityHeader(body));
        }

        return message;
    }

    /**
     * Add attachments as parts into a MimeMultipart container.
     * @param mp MimeMultipart container in which to insert parts.
     * @throws MessagingException
     */
    private void addAttachmentsToMessage(final MimeMultipart mp) throws MessagingException
    {
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
    }

    // FYI, there's nothing in the code that requires these variables to one letter. They're one
    // letter simply to save space.  This name sucks.  It's too similar to Account.Identity.
    private enum IdentityField
    {
        LENGTH("l"),
        OFFSET("o"),
        MESSAGE_FORMAT("f"),
        SIGNATURE("s"),
        NAME("n"),
        EMAIL("e"),
        // TODO - store a reference to the message being replied so we can mark it at the time of send.
        ORIGINAL_MESSAGE("m");

        private final String value;

        IdentityField(String value)
        {
            this.value = value;
        }

        public String value()
        {
            return value;
        }

        /**
         * Get the list of IdentityFields that should be integer values. These values are sanity
         * checked for integer-ness during decoding.
         * @return
         */
        public static IdentityField[] getIntegerFields()
        {
            return new IdentityField[] { LENGTH, OFFSET };
        }
    }

    /**
     * Build the identity header string. This string contains metadata about a draft message to be
     * used upon loading a draft for composition. This should be generated at the time of saving a
     * draft.<br>
     * <br>
     * This is a URL-encoded key/value pair string.  The list of possible values are in {@link IdentityField}.
     * @param body {@link TextBody} to analyze for body length and offset.
     * @return Identity string.
     */
    private String buildIdentityHeader(final TextBody body)
    {
        Uri.Builder uri = new Uri.Builder();
        if(body.getComposedMessageLength() != null && body.getComposedMessageOffset() != null)
        {
            // See if the message body length is already in the TextBody.
            uri.appendQueryParameter(IdentityField.LENGTH.value(), body.getComposedMessageLength().toString());
            uri.appendQueryParameter(IdentityField.OFFSET.value(), body.getComposedMessageOffset().toString());
        }
        else
        {
            // If not, calculate it now.
            uri.appendQueryParameter(IdentityField.LENGTH.value(), Integer.toString(body.getText().length()));
            uri.appendQueryParameter(IdentityField.OFFSET.value(), Integer.toString(0));
        }
        // Save the message format for this offset.
        uri.appendQueryParameter(IdentityField.MESSAGE_FORMAT.value(), mAccount.getMessageFormat().name());

        // If we're not using the standard identity of signature, append it on to the identity blob.
        if (mSignatureChanged)
        {
            uri.appendQueryParameter(IdentityField.SIGNATURE.value(), mSignatureView.getText().toString());
        }

        if (mIdentityChanged)
        {
            uri.appendQueryParameter(IdentityField.NAME.value(), mIdentity.getName());
            uri.appendQueryParameter(IdentityField.EMAIL.value(), mIdentity.getEmail());
        }

        // Tag this is as a "new style" identity. ! is an impossible value in base64 encoding, so we
        // use that to determine which version we're in.
        String k9identity = "!" + uri.build().getEncodedQuery();

        if (K9.DEBUG)
        {
            Log.d(K9.LOG_TAG, "Generated identity: " + k9identity);
        }

        return k9identity;
    }

    /**
     * Parse an identity string.  Handles both legacy and new (!) style identities.
     * @param identityString
     * @return
     */
    private Map<IdentityField, String> parseIdentityHeader(final String identityString)
    {
        Map<IdentityField, String> identity = new HashMap<IdentityField, String>();

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "Decoding identity: " + identityString);

        if (identityString == null || identityString.length() < 1)
        {
            return identity;
        }

        if (identityString.charAt(0) == '!' && identityString.length() > 2)
        {
            Uri.Builder builder = new Uri.Builder();
            builder.encodedQuery(identityString.substring(1));  // Need to cut off the ! at the beginning.
            Uri uri = builder.build();
            for (IdentityField key : IdentityField.values())
            {
                String value = uri.getQueryParameter(key.value());
                if (value != null)
                {
                    identity.put(key, value);
                }
            }

            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "Decoded identity: " + identity.toString());

            // Sanity check our Integers so that recipients of this result don't have to.
            for (IdentityField key : IdentityField.getIntegerFields())
            {
                if (identity.get(key) != null)
                {
                    try
                    {
                        Integer.parseInt(identity.get(key));
                    }
                    catch (NumberFormatException e)
                    {
                        Log.e(K9.LOG_TAG, "Invalid " + key.name() + " field in identity: " + identity.get(key));
                    }
                }
            }
        }
        else
        {
            // Legacy identity

            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "Got a saved legacy identity: " + identityString);
            StringTokenizer tokens = new StringTokenizer(identityString, ":", false);

            // First item is the body length. We use this to separate the composed reply from the quoted text.
            if (tokens.hasMoreTokens())
            {
                String bodyLengthS = Utility.base64Decode(tokens.nextToken());
                try
                {
                    identity.put(IdentityField.LENGTH, Integer.valueOf(bodyLengthS).toString());
                }
                catch (Exception e)
                {
                    Log.e(K9.LOG_TAG, "Unable to parse bodyLength '" + bodyLengthS + "'");
                }
            }
            if (tokens.hasMoreTokens())
            {
                identity.put(IdentityField.SIGNATURE, Utility.base64Decode(tokens.nextToken()));
            }
            if (tokens.hasMoreTokens())
            {
                identity.put(IdentityField.NAME, Utility.base64Decode(tokens.nextToken()));
            }
            if (tokens.hasMoreTokens())
            {
                identity.put(IdentityField.EMAIL, Utility.base64Decode(tokens.nextToken()));
            }
        }

        return identity;
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
                String text = buildText(false).getText();
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
        switchToIdentity((Identity) bundle.getSerializable(ChooseIdentity.EXTRA_IDENTITY));
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
                mQuotedHTML.setVisibility(View.GONE);
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
                        dismissDialog(DIALOG_SAVE_OR_DISCARD_DRAFT_MESSAGE);
                        onSave();
                    }
                })
                       .setNegativeButton(R.string.discard_action, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        dismissDialog(DIALOG_SAVE_OR_DISCARD_DRAFT_MESSAGE);
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
        try
        {
            if (ACTION_REPLY.equals(action) || ACTION_REPLY_ALL.equals(action))
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

                // Quote the message and setup the UI.
                populateUIWithQuotedMessage();

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
            else if (ACTION_FORWARD.equals(action))
            {
                if (message.getSubject() != null && !message.getSubject().toLowerCase().startsWith("fwd:"))
                {
                    mSubjectView.setText("Fwd: " + message.getSubject());
                }
                else
                {
                    mSubjectView.setText(message.getSubject());
                }

                // Quote the message and setup the UI.
                populateUIWithQuotedMessage();

                if (!mSourceMessageProcessed)
                {
                    if (!loadAttachments(message, 0))
                    {
                        mHandler.sendEmptyMessage(MSG_SKIPPED_ATTACHMENTS);
                    }
                }
            }
            else if (ACTION_EDIT_DRAFT.equals(action))
            {
                mDraftUid = message.getUid();
                mSubjectView.setText(message.getSubject());
                addAddresses(mToView, message.getRecipients(RecipientType.TO));
                if (message.getRecipients(RecipientType.CC).length > 0)
                {
                    addAddresses(mCcView, message.getRecipients(RecipientType.CC));
                    mCcView.setVisibility(View.VISIBLE);
                }

                Address[] bccRecipients = message.getRecipients(RecipientType.BCC);
                if (bccRecipients.length > 0)
                {
                    addAddresses(mBccView, bccRecipients);
                    String bccAddress = mAccount.getAlwaysBcc();
                    if (bccRecipients.length == 1 && bccAddress != null && bccAddress.equals(bccRecipients[0].toString()))
                    {
                        // If the auto-bcc is the only entry in the BCC list, don't show the Bcc fields.
                        mBccView.setVisibility(View.GONE);
                    }
                    else
                    {
                        mBccView.setVisibility(View.VISIBLE);
                    }
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

                // Decode the identity header when loading a draft.
                // See buildIdentityHeader(TextBody) for a detailed description of the composition of this blob.
                Map<IdentityField, String> k9identity = new HashMap<IdentityField, String>();
                if (message.getHeader(K9.IDENTITY_HEADER) != null && message.getHeader(K9.IDENTITY_HEADER).length > 0 && message.getHeader(K9.IDENTITY_HEADER)[0] != null)
                {
                    k9identity = parseIdentityHeader(message.getHeader(K9.IDENTITY_HEADER)[0]);
                    if(K9.DEBUG)
                        Log.d(K9.LOG_TAG, "Parsed identity: " + k9identity.toString());
                }

                Identity newIdentity = new Identity();
                if (k9identity.containsKey(IdentityField.SIGNATURE))
                {
                    newIdentity.setSignatureUse(true);
                    newIdentity.setSignature(k9identity.get(IdentityField.SIGNATURE));
                    mSignatureChanged = true;
                }
                else
                {
                    newIdentity.setSignatureUse(message.getFolder().getAccount().getSignatureUse());
                    newIdentity.setSignature(mIdentity.getSignature());
                }

                if (k9identity.containsKey(IdentityField.NAME))
                {
                    newIdentity.setName(k9identity.get(IdentityField.NAME));
                    mIdentityChanged = true;
                }
                else
                {
                    newIdentity.setName(mIdentity.getName());
                }

                if (k9identity.containsKey(IdentityField.EMAIL))
                {
                    newIdentity.setEmail(k9identity.get(IdentityField.EMAIL));
                    mIdentityChanged = true;
                }
                else
                {
                    newIdentity.setEmail(mIdentity.getEmail());
                }

                mIdentity = newIdentity;

                updateSignature();
                updateFrom();

                Integer bodyLength = k9identity.get(IdentityField.LENGTH) != null
                                     ? Integer.parseInt(k9identity.get(IdentityField.LENGTH))
                                     : 0;
                Integer bodyOffset = k9identity.get(IdentityField.OFFSET) != null
                                     ? Integer.parseInt(k9identity.get(IdentityField.OFFSET))
                                     : 0;
                // Always respect the user's current composition format preference, even if the
                // draft was saved in a different format.
                // TODO - The current implementation doesn't allow a user in HTML mode to edit a draft that wasn't saved with K9mail.
                if (mAccount.getMessageFormat() == MessageFormat.HTML)
                {
                    if (k9identity.get(IdentityField.MESSAGE_FORMAT) == null || !MessageFormat.valueOf(k9identity.get(IdentityField.MESSAGE_FORMAT)).equals(MessageFormat.HTML))
                    {
                        // This message probably wasn't created by us. The exception is legacy
                        // drafts created before the advent of HTML composition. In those cases,
                        // we'll display the whole message (including the quoted part) in the
                        // composition window. If that's the case, try and convert it to text to
                        // match the behavior in text mode.
                        mMessageContentView.setText(getBodyTextFromMessage(message, MessageFormat.TEXT));
                    }
                    else
                    {
                        Part part = MimeUtility.findFirstPartByMimeType(message, "text/html");
                        if (part != null)   // Shouldn't happen if we were the one who saved it.
                        {
                            String text = MimeUtility.getTextFromPart(part);
                            if (K9.DEBUG)
                            {
                                Log.d(K9.LOG_TAG, "Loading message with offset " + bodyOffset + ", length " + bodyLength + ". Text length is " + text.length() + ".");
                            }

                            // Grab our reply text.
                            String bodyText = text.substring(bodyOffset, bodyOffset + bodyLength);
                            mMessageContentView.setText(HtmlConverter.htmlToText(bodyText));

                            // Regenerate the quoted html without our user content in it.
                            StringBuilder quotedHTML = new StringBuilder();
                            quotedHTML.append(text.substring(0, bodyOffset));   // stuff before the reply
                            quotedHTML.append(text.substring(bodyOffset + bodyLength));
                            if (quotedHTML.length() > 0)
                            {
                                mQuotedHtmlContent = new InsertableHtmlContent();
                                mQuotedHtmlContent.setQuotedContent(quotedHTML);
                                mQuotedHtmlContent.setHeaderInsertionPoint(bodyOffset);
                                mQuotedHTML.loadDataWithBaseURL("http://", mQuotedHtmlContent.getQuotedContent(), "text/html", "utf-8", null);
                                mQuotedHTML.setVisibility(View.VISIBLE);
                                mQuotedTextBar.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                }
                else if (mAccount.getMessageFormat() == MessageFormat.TEXT)
                {
                    MessageFormat format = k9identity.get(IdentityField.MESSAGE_FORMAT) != null
                                           ? MessageFormat.valueOf(k9identity.get(IdentityField.MESSAGE_FORMAT))
                                           : null;
                    if (format == null)
                    {
                        mMessageContentView.setText(getBodyTextFromMessage(message, MessageFormat.TEXT));
                    }
                    else if (format.equals(MessageFormat.HTML))
                    {
                        // We are in text mode, but have an HTML message.
                        Part htmlPart = MimeUtility.findFirstPartByMimeType(message, "text/html");
                        if (htmlPart != null)   // Shouldn't happen if we were the one who saved it.
                        {
                            String text = MimeUtility.getTextFromPart(htmlPart);
                            if (K9.DEBUG)
                            {
                                Log.d(K9.LOG_TAG, "Loading message with offset " + bodyOffset + ", length " + bodyLength + ". Text length is " + text.length() + ".");
                            }

                            // Grab our reply text.
                            String bodyText = text.substring(bodyOffset, bodyOffset + bodyLength);
                            mMessageContentView.setText(Html.fromHtml(bodyText).toString());

                            // Regenerate the quoted html without out content in it.
                            StringBuilder quotedHTML = new StringBuilder();
                            quotedHTML.append(text.substring(0, bodyOffset));   // stuff before the reply
                            quotedHTML.append(text.substring(bodyOffset + bodyLength));
                            // Convert it to text.
                            mQuotedText.setText(HtmlConverter.htmlToText(quotedHTML.toString()));

                            mQuotedTextBar.setVisibility(View.VISIBLE);
                            mQuotedText.setVisibility(View.VISIBLE);
                        }
                        else
                        {
                            Log.e(K9.LOG_TAG, "Found an HTML draft but couldn't find the HTML part!  Something's wrong.");
                        }
                    }
                    else if (format.equals(MessageFormat.TEXT))
                    {
                        Part textPart = MimeUtility.findFirstPartByMimeType(message, "text/plain");
                        if (textPart != null)
                        {
                            String text = MimeUtility.getTextFromPart(textPart);
                            // If we had a body length (and it was valid), separate the composition from the quoted text
                            // and put them in their respective places in the UI.
                            if (bodyLength != null && bodyLength + 1 < text.length())   // + 1 to get rid of the newline we added when saving the draft
                            {
                                String bodyText = text.substring(0, bodyLength);
                                String quotedText = text.substring(bodyLength + 1, text.length());

                                mMessageContentView.setText(bodyText);
                                mQuotedText.setText(quotedText);

                                mQuotedTextBar.setVisibility(View.VISIBLE);
                                mQuotedText.setVisibility(View.VISIBLE);
                                mQuotedHTML.setVisibility(View.VISIBLE);
                            }
                            else
                            {
                                mMessageContentView.setText(text);
                            }
                        }
                    }
                    else
                    {
                        Log.e(K9.LOG_TAG, "Unhandled message format.");
                    }
                }
            }
        }
        catch (MessagingException me)
        {
            /**
             * Let the user continue composing their message even if we have a problem processing
             * the source message. Log it as an error, though.
             */
            Log.e(K9.LOG_TAG, "Error while processing source message: ", me);
        }
        mSourceMessageProcessed = true;
        mDraftNeedsSaving = false;
    }

    /**
     * Build and populate the UI with the quoted message.
     * @throws MessagingException
     */
    private void populateUIWithQuotedMessage() throws MessagingException
    {
        // TODO -- I am assuming that mSourceMessageBody will always be a text part.  Is this a safe assumption?

        // Handle the original message in the reply
        // If we already have mSourceMessageBody, use that.  It's pre-populated if we've got crypto going on.
        String content = mSourceMessageBody != null
                         ? mSourceMessageBody
                         : getBodyTextFromMessage(mSourceMessage, mAccount.getMessageFormat());
        if (mAccount.getMessageFormat() == MessageFormat.HTML)
        {
            // Add the HTML reply header to the top of the content.
            mQuotedHtmlContent = quoteOriginalHtmlMessage(mSourceMessage, content, mAccount.getQuoteStyle());
            // Load the message with the reply header.
            mQuotedHTML.loadDataWithBaseURL("http://", mQuotedHtmlContent.getQuotedContent(), "text/html", "utf-8", null);

            mQuotedTextBar.setVisibility(View.VISIBLE);
            mQuotedHTML.setVisibility(View.VISIBLE);
        }
        else if (mAccount.getMessageFormat() == MessageFormat.TEXT)
        {
            mQuotedText.setText(quoteOriginalTextMessage(mSourceMessage, content, mAccount.getQuoteStyle()));

            mQuotedTextBar.setVisibility(View.VISIBLE);
            mQuotedText.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Fetch the body text from a message in the desired message format. This method handles
     * conversions between formats (html to text and vice versa) if necessary.
     * @param message Message to analyze for body part.
     * @param format Desired format.
     * @return Text in desired format.
     * @throws MessagingException
     */
    private String getBodyTextFromMessage(final Message message, final MessageFormat format) throws MessagingException
    {
        Part part;
        if (format == MessageFormat.HTML)
        {
            // HTML takes precedence, then text.
            part = MimeUtility.findFirstPartByMimeType(message, "text/html");
            if (part != null)
            {
                if(K9.DEBUG)
                    Log.d(K9.LOG_TAG, "getBodyTextFromMessage: HTML requested, HTML found.");
                return MimeUtility.getTextFromPart(part);
            }

            part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
            if (part != null)
            {
                if(K9.DEBUG)
                    Log.d(K9.LOG_TAG, "getBodyTextFromMessage: HTML requested, text found.");
                return HtmlConverter.textToHtml(MimeUtility.getTextFromPart(part));
            }
        }
        else if (format == MessageFormat.TEXT)
        {
            // Text takes precedence, then html.
            part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
            if (part != null)
            {
                if(K9.DEBUG)
                    Log.d(K9.LOG_TAG, "getBodyTextFromMessage: Text requested, text found.");
                return MimeUtility.getTextFromPart(part);
            }

            part = MimeUtility.findFirstPartByMimeType(message, "text/html");
            if (part != null)
            {
                if(K9.DEBUG)
                    Log.d(K9.LOG_TAG, "getBodyTextFromMessage: Text requested, HTML found.");
                return HtmlConverter.htmlToText(MimeUtility.getTextFromPart(part));
            }
        }

        // If we had nothing interesting, return an empty string.
        return "";
    }

    // Regular expressions to look for various HTML tags. This is no HTML::Parser, but hopefully it's good enough for
    // our purposes.
    private static final Pattern FIND_INSERTION_POINT_HTML = Pattern.compile("(?si:.*?(<html(?:>|\\s+[^>]*>)).*)");
    private static final Pattern FIND_INSERTION_POINT_HEAD = Pattern.compile("(?si:.*?(<head(?:>|\\s+[^>]*>)).*)");
    private static final Pattern FIND_INSERTION_POINT_BODY = Pattern.compile("(?si:.*?(<body(?:>|\\s+[^>]*>)).*)");
    private static final Pattern FIND_INSERTION_POINT_HTML_END = Pattern.compile("(?si:.*(</html>).*?)");
    private static final Pattern FIND_INSERTION_POINT_BODY_END = Pattern.compile("(?si:.*(</body>).*?)");
    // The first group in a Matcher contains the first capture group. We capture the tag found in the above REs so that
    // we can locate the *end* of that tag.
    private static final int FIND_INSERTION_POINT_FIRST_GROUP = 1;
    // HTML bits to insert as appropriate
    // TODO is it safe to assume utf-8 here?
    private static final String FIND_INSERTION_POINT_HTML_CONTENT = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n<html>";
    private static final String FIND_INSERTION_POINT_HTML_END_CONTENT = "</html>";
    private static final String FIND_INSERTION_POINT_HEAD_CONTENT = "<head><meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\"></head>";
    // Index of the start of the beginning of a String.
    private static final int FIND_INSERTION_POINT_START_OF_STRING = 0;
    /**
     * <p>Find the start and end positions of the HTML in the string. This should be the very top
     * and bottom of the displayable message. It returns a {@link InsertableHtmlContent}, which
     * contains both the insertion points and potentially modified HTML. The modified HTML should be
     * used in place of the HTML in the original message.</p>
     *
     * <p>This method loosely mimics the HTML forward/reply behavior of BlackBerry OS 4.5/BIS 2.5, which in turn mimics
     * Outlook 2003 (as best I can tell).</p>
     *
     * @param content Content to examine for HTML insertion points
     * @return Insertion points and HTML to use for insertion.
     */
    private InsertableHtmlContent findInsertionPoints(final String content)
    {
        InsertableHtmlContent insertable = new InsertableHtmlContent();

        // If there is no content, don't bother doing any of the regex dancing.
        if (content == null || content.equals(""))
        {
            return insertable;
        }

        // Search for opening tags.
        boolean hasHtmlTag = false;
        boolean hasHeadTag = false;
        boolean hasBodyTag = false;
        // First see if we have an opening HTML tag.  If we don't find one, we'll add one later.
        Matcher htmlMatcher = FIND_INSERTION_POINT_HTML.matcher(content);
        if (htmlMatcher.matches())
        {
            hasHtmlTag = true;
        }
        // Look for a HEAD tag.  If we're missing a BODY tag, we'll use the close of the HEAD to start our content.
        Matcher headMatcher = FIND_INSERTION_POINT_HEAD.matcher(content);
        if (headMatcher.matches())
        {
            hasHeadTag = true;
        }
        // Look for a BODY tag.  This is the ideal place for us to start our content.
        Matcher bodyMatcher = FIND_INSERTION_POINT_BODY.matcher(content);
        if (bodyMatcher.matches())
        {
            hasBodyTag = true;
        }

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "Open: hasHtmlTag:" + hasHtmlTag + " hasHeadTag:" + hasHeadTag + " hasBodyTag:" + hasBodyTag);

        // Given our inspections, let's figure out where to start our content.
        // This is the ideal case -- there's a BODY tag and we insert ourselves just after it.
        if (hasBodyTag)
        {
            insertable.setQuotedContent(new StringBuilder(content));
            insertable.setHeaderInsertionPoint(bodyMatcher.end(FIND_INSERTION_POINT_FIRST_GROUP));
        }
        else if (hasHeadTag)
        {
            // Now search for a HEAD tag.  We can insert after there.

            // If BlackBerry sees a HEAD tag, it inserts right after that, so long as there is no BODY tag. It doesn't
            // try to add BODY, either.  Right or wrong, it seems to work fine.
            insertable.setQuotedContent(new StringBuilder(content));
            insertable.setHeaderInsertionPoint(headMatcher.end(FIND_INSERTION_POINT_FIRST_GROUP));
        }
        else if (hasHtmlTag)
        {
            // Lastly, check for an HTML tag.
            // In this case, it will add a HEAD, but no BODY.
            StringBuilder newContent = new StringBuilder(content);
            // Insert the HEAD content just after the HTML tag.
            newContent.insert(htmlMatcher.end(FIND_INSERTION_POINT_FIRST_GROUP), FIND_INSERTION_POINT_HEAD_CONTENT);
            insertable.setQuotedContent(newContent);
            // The new insertion point is the end of the HTML tag, plus the length of the HEAD content.
            insertable.setHeaderInsertionPoint(htmlMatcher.end(FIND_INSERTION_POINT_FIRST_GROUP) + FIND_INSERTION_POINT_HEAD_CONTENT.length());
        }
        else
        {
            // If we have none of the above, we probably have a fragment of HTML.  Yahoo! and Gmail both do this.
            // Again, we add a HEAD, but not BODY.
            StringBuilder newContent = new StringBuilder(content);
            // Add the HTML and HEAD tags.
            newContent.insert(FIND_INSERTION_POINT_START_OF_STRING, FIND_INSERTION_POINT_HEAD_CONTENT);
            newContent.insert(FIND_INSERTION_POINT_START_OF_STRING, FIND_INSERTION_POINT_HTML_CONTENT);
            // Append the </HTML> tag.
            newContent.append(FIND_INSERTION_POINT_HTML_END_CONTENT);
            insertable.setQuotedContent(newContent);
            insertable.setHeaderInsertionPoint(FIND_INSERTION_POINT_HTML_CONTENT.length() + FIND_INSERTION_POINT_HEAD_CONTENT.length());
        }

        // Search for closing tags. We have to do this after we deal with opening tags since it may
        // have modified the message.
        boolean hasHtmlEndTag = false;
        boolean hasBodyEndTag = false;
        // First see if we have an opening HTML tag.  If we don't find one, we'll add one later.
        Matcher htmlEndMatcher = FIND_INSERTION_POINT_HTML_END.matcher(insertable.getQuotedContent());
        if (htmlEndMatcher.matches())
        {
            hasHtmlEndTag = true;
        }
        // Look for a BODY tag.  This is the ideal place for us to place our footer.
        Matcher bodyEndMatcher = FIND_INSERTION_POINT_BODY_END.matcher(insertable.getQuotedContent());
        if (bodyEndMatcher.matches())
        {
            hasBodyEndTag = true;
        }

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "Close: hasHtmlEndTag:" + hasHtmlEndTag + " hasBodyEndTag:" + hasBodyEndTag);

        // Now figure out where to put our footer.
        // This is the ideal case -- there's a BODY tag and we insert ourselves just before it.
        if (hasBodyEndTag)
        {
            insertable.setFooterInsertionPoint(bodyEndMatcher.start(FIND_INSERTION_POINT_FIRST_GROUP));
        }
        else if (hasHtmlEndTag)
        {
            // Check for an HTML tag.  Add ourselves just before it.
            insertable.setFooterInsertionPoint(htmlEndMatcher.start(FIND_INSERTION_POINT_FIRST_GROUP));
        }
        else
        {
            // If we have none of the above, we probably have a fragment of HTML.
            // Set our footer insertion point as the end of the string.
            insertable.setFooterInsertionPoint(insertable.getQuotedContent().length());
        }

        return insertable;
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
                message = createMessage(false);  // isDraft = true
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
                message = createMessage(true);  // isDraft = true
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

            final MessagingController messagingController = MessagingController.getInstance(getApplication());
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

    private static final int REPLY_WRAP_LINE_WIDTH = 72;
    private static final int QUOTE_BUFFER_LENGTH = 512; // amount of extra buffer to allocate to accommodate quoting headers or prefixes

    /**
     * Add quoting markup to a text message.
     * @param originalMessage Metadata for message being quoted.
     * @param messageBody Text of the message to be quoted.
     * @param quoteStyle Style of quoting.
     * @return Quoted text.
     * @throws MessagingException
     */
    private String quoteOriginalTextMessage(final Message originalMessage, final String messageBody, final QuoteStyle quoteStyle) throws MessagingException
    {
        String body = messageBody == null ? "" : messageBody;
        if (quoteStyle == QuoteStyle.PREFIX)
        {
            StringBuilder quotedText = new StringBuilder(body.length() + QUOTE_BUFFER_LENGTH);
            quotedText.append(String.format(
                                  getString(R.string.message_compose_reply_header_fmt),
                                  Address.toString(originalMessage.getFrom()))
                             );

            final String prefix = mAccount.getQuotePrefix();
            final String wrappedText = Utility.wrap(body, REPLY_WRAP_LINE_WIDTH - prefix.length());

            // "$" and "\" in the quote prefix have to be escaped for
            // the replaceAll() invocation.
            final String escapedPrefix = prefix.replaceAll("(\\\\|\\$)", "\\\\$1");
            quotedText.append(wrappedText.replaceAll("(?m)^", escapedPrefix));

            return quotedText.toString().replaceAll("\\\r", "");
        }
        else if (quoteStyle == QuoteStyle.HEADER)
        {
            StringBuilder quotedText = new StringBuilder(body.length() + QUOTE_BUFFER_LENGTH);
            quotedText.append("\n");
            quotedText.append(getString(R.string.message_compose_quote_header_separator)).append("\n");
            if (originalMessage.getFrom() != null && Address.toString(originalMessage.getFrom()).length() != 0)
            {
                quotedText.append(getString(R.string.message_compose_quote_header_from)).append(" ").append(Address.toString(originalMessage.getFrom())).append("\n");
            }
            if (originalMessage.getSentDate() != null)
            {
                quotedText.append(getString(R.string.message_compose_quote_header_send_date)).append(" ").append(originalMessage.getSentDate()).append("\n");
            }
            if (originalMessage.getRecipients(RecipientType.TO) != null && originalMessage.getRecipients(RecipientType.TO).length != 0)
            {
                quotedText.append(getString(R.string.message_compose_quote_header_to)).append(" ").append(Address.toString(originalMessage.getRecipients(RecipientType.TO))).append("\n");
            }
            if (originalMessage.getRecipients(RecipientType.CC) != null && originalMessage.getRecipients(RecipientType.CC).length != 0)
            {
                quotedText.append(getString(R.string.message_compose_quote_header_cc)).append(" ").append(Address.toString(originalMessage.getRecipients(RecipientType.CC))).append("\n");
            }
            if (originalMessage.getSubject() != null)
            {
                quotedText.append(getString(R.string.message_compose_quote_header_subject)).append(" ").append(originalMessage.getSubject()).append("\n");
            }
            quotedText.append("\n");

            quotedText.append(body);

            return quotedText.toString();
        }
        else
        {
            // Shouldn't ever happen.
            return body;
        }
    }

    /**
     * Add quoting markup to a HTML message.
     * @param originalMessage Metadata for message being quoted.
     * @param messageBody Text of the message to be quoted.
     * @param quoteStyle Style of quoting.
     * @return Modified insertable message.
     * @throws MessagingException
     */
    private InsertableHtmlContent quoteOriginalHtmlMessage(final Message originalMessage, final String messageBody, final QuoteStyle quoteStyle) throws MessagingException
    {
        InsertableHtmlContent insertable = findInsertionPoints(messageBody);

        if (quoteStyle == QuoteStyle.PREFIX)
        {
            StringBuilder header = new StringBuilder(QUOTE_BUFFER_LENGTH);
            header.append("<br><div class=\"gmail_quote\">");
            // Remove all trailing newlines so that the quote starts immediately after the header.  "Be like Gmail!"
            header.append(HtmlConverter.textToHtmlFragment(String.format(
                              getString(R.string.message_compose_reply_header_fmt).replaceAll("\n$", ""),
                              Address.toString(originalMessage.getFrom()))
                                                          ));
            header.append("<blockquote class=\"gmail_quote\" " +
                          "style=\"margin: 0pt 0pt 0pt 0.8ex; border-left: 1px solid rgb(204, 204, 204); padding-left: 1ex;\">\n");

            String footer = "</blockquote></div>";

            insertable.insertIntoQuotedHeader(header.toString());
            insertable.insertIntoQuotedFooter(footer);
        }
        else if (quoteStyle == QuoteStyle.HEADER)
        {

            StringBuilder header = new StringBuilder();
            header.append("<div style='font-size:10.0pt;font-family:\"Tahoma\",\"sans-serif\";padding:3.0pt 0in 0in 0in'>\n");
            header.append("<hr style='border:none;border-top:solid #B5C4DF 1.0pt'>\n"); // This gets converted into a horizontal line during html to text conversion.
            if (mSourceMessage.getFrom() != null && Address.toString(mSourceMessage.getFrom()).length() != 0)
            {
                header.append("<b>").append(getString(R.string.message_compose_quote_header_from)).append("</b> ").append(HtmlConverter.textToHtmlFragment(Address.toString(mSourceMessage.getFrom()))).append("<br>\n");
            }
            if (mSourceMessage.getSentDate() != null)
            {
                header.append("<b>").append(getString(R.string.message_compose_quote_header_send_date)).append("</b> ").append(mSourceMessage.getSentDate()).append("<br>\n");
            }
            if (mSourceMessage.getRecipients(RecipientType.TO) != null && mSourceMessage.getRecipients(RecipientType.TO).length != 0)
            {
                header.append("<b>").append(getString(R.string.message_compose_quote_header_to)).append("</b> ").append(HtmlConverter.textToHtmlFragment(Address.toString(mSourceMessage.getRecipients(RecipientType.TO)))).append("<br>\n");
            }
            if (mSourceMessage.getRecipients(RecipientType.CC) != null && mSourceMessage.getRecipients(RecipientType.CC).length != 0)
            {
                header.append("<b>").append(getString(R.string.message_compose_quote_header_cc)).append("</b> ").append(HtmlConverter.textToHtmlFragment(Address.toString(mSourceMessage.getRecipients(RecipientType.CC)))).append("<br>\n");
            }
            if (mSourceMessage.getSubject() != null)
            {
                header.append("<b>").append(getString(R.string.message_compose_quote_header_subject)).append("</b> ").append(HtmlConverter.textToHtmlFragment(mSourceMessage.getSubject())).append("<br>\n");
            }
            header.append("</div>\n");
            header.append("<br>\n");

            insertable.insertIntoQuotedHeader(header.toString());
        }

        return insertable;
    }
}
