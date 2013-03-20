package com.fsck.k9.activity;


import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.OpenableColumns;
import android.text.TextWatcher;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AutoCompleteTextView.Validator;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import com.fsck.k9.Account;
import com.fsck.k9.Account.MessageFormat;
import com.fsck.k9.Account.QuoteStyle;
import com.fsck.k9.EmailAddressAdapter;
import com.fsck.k9.EmailAddressValidator;
import com.fsck.k9.FontSizes;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.crypto.CryptoProvider;
import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.helper.ContactItem;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.HtmlConverter;
import com.fsck.k9.helper.StringUtils;
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
import com.fsck.k9.view.MessageWebView;
import org.apache.james.mime4j.codec.EncoderUtil;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleHtmlSerializer;
import org.htmlcleaner.TagNode;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageCompose extends K9Activity implements OnClickListener {
    private static final int DIALOG_SAVE_OR_DISCARD_DRAFT_MESSAGE = 1;
    private static final int DIALOG_REFUSE_TO_SAVE_DRAFT_MARKED_ENCRYPTED = 2;
    private static final int DIALOG_CONTINUE_WITHOUT_PUBLIC_KEY = 3;
    private static final int DIALOG_CONFIRM_DISCARD_ON_BACK = 4;
    private static final int DIALOG_CHOOSE_IDENTITY = 5;

    private static final long INVALID_DRAFT_ID = MessagingController.INVALID_MESSAGE_ID;

    private static final String ACTION_COMPOSE = "com.fsck.k9.intent.action.COMPOSE";
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
    private static final String STATE_KEY_QUOTED_TEXT_MODE =
        "com.fsck.k9.activity.MessageCompose.QuotedTextShown";
    private static final String STATE_KEY_SOURCE_MESSAGE_PROCED =
        "com.fsck.k9.activity.MessageCompose.stateKeySourceMessageProced";
    private static final String STATE_KEY_DRAFT_ID = "com.fsck.k9.activity.MessageCompose.draftId";
    private static final String STATE_KEY_HTML_QUOTE = "com.fsck.k9.activity.MessageCompose.HTMLQuote";
    private static final String STATE_IDENTITY_CHANGED =
        "com.fsck.k9.activity.MessageCompose.identityChanged";
    private static final String STATE_IDENTITY =
        "com.fsck.k9.activity.MessageCompose.identity";
    private static final String STATE_PGP_DATA = "pgpData";
    private static final String STATE_IN_REPLY_TO = "com.fsck.k9.activity.MessageCompose.inReplyTo";
    private static final String STATE_REFERENCES = "com.fsck.k9.activity.MessageCompose.references";
    private static final String STATE_KEY_READ_RECEIPT = "com.fsck.k9.activity.MessageCompose.messageReadReceipt";
    private static final String STATE_KEY_DRAFT_NEEDS_SAVING = "com.fsck.k9.activity.MessageCompose.mDraftNeedsSaving";
    private static final String STATE_KEY_FORCE_PLAIN_TEXT =
            "com.fsck.k9.activity.MessageCompose.forcePlainText";
    private static final String STATE_KEY_QUOTED_TEXT_FORMAT =
            "com.fsck.k9.activity.MessageCompose.quotedTextFormat";

    private static final int MSG_PROGRESS_ON = 1;
    private static final int MSG_PROGRESS_OFF = 2;
    private static final int MSG_SKIPPED_ATTACHMENTS = 3;
    private static final int MSG_SAVED_DRAFT = 4;
    private static final int MSG_DISCARDED_DRAFT = 5;

    private static final int ACTIVITY_REQUEST_PICK_ATTACHMENT = 1;
    private static final int CONTACT_PICKER_TO = 4;
    private static final int CONTACT_PICKER_CC = 5;
    private static final int CONTACT_PICKER_BCC = 6;
    private static final int CONTACT_PICKER_TO2 = 7;
    private static final int CONTACT_PICKER_CC2 = 8;
    private static final int CONTACT_PICKER_BCC2 = 9;

    private static final Account[] EMPTY_ACCOUNT_ARRAY = new Account[0];

    /**
     * Regular expression to remove the first localized "Re:" prefix in subjects.
     *
     * Currently:
     * - "Aw:" (german: abbreviation for "Antwort")
     */
    private static final Pattern PREFIX = Pattern.compile("^AW[:\\s]\\s*", Pattern.CASE_INSENSITIVE);

    /**
     * The account used for message composition.
     */
    private Account mAccount;


    private Contacts mContacts;

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

    /**
     * "Original" message body
     *
     * <p>
     * The contents of this string will be used instead of the body of a referenced message when
     * replying to or forwarding a message.<br>
     * Right now this is only used when replying to a signed or encrypted message. It then contains
     * the stripped/decrypted body of that message.
     * </p>
     * <p><strong>Note:</strong>
     * When this field is not {@code null} we assume that the message we are composing right now
     * should be encrypted.
     * </p>
     */
    private String mSourceMessageBody;

    /**
     * Indicates that the source message has been processed at least once and should not
     * be processed on any subsequent loads. This protects us from adding attachments that
     * have already been added from the restore of the view state.
     */
    private boolean mSourceMessageProcessed = false;

    enum Action {
        COMPOSE,
        REPLY,
        REPLY_ALL,
        FORWARD,
        EDIT_DRAFT
    }

    /**
     * Contains the action we're currently performing (e.g. replying to a message)
     */
    private Action mAction;

    private enum QuotedTextMode {
        NONE,
        SHOW,
        HIDE
    };

    private boolean mReadReceipt = false;

    private QuotedTextMode mQuotedTextMode = QuotedTextMode.NONE;

    /**
     * Contains the format of the quoted text (text vs. HTML).
     */
    private SimpleMessageFormat mQuotedTextFormat;

    /**
     * When this it {@code true} the message format setting is ignored and we're always sending
     * a text/plain message.
     */
    private boolean mForcePlainText = false;

    private Button mChooseIdentityButton;
    private LinearLayout mCcWrapper;
    private LinearLayout mBccWrapper;
    private MultiAutoCompleteTextView mToView;
    private MultiAutoCompleteTextView mCcView;
    private MultiAutoCompleteTextView mBccView;
    private EditText mSubjectView;
    private EditText mSignatureView;
    private EditText mMessageContentView;
    private LinearLayout mAttachments;
    private Button mQuotedTextShow;
    private View mQuotedTextBar;
    private ImageButton mQuotedTextEdit;
    private ImageButton mQuotedTextDelete;
    private EditText mQuotedText;
    private MessageWebView mQuotedHTML;
    private InsertableHtmlContent mQuotedHtmlContent;   // Container for HTML reply as it's being built.
    private View mEncryptLayout;
    private CheckBox mCryptoSignatureCheckbox;
    private CheckBox mEncryptCheckbox;
    private TextView mCryptoSignatureUserId;
    private TextView mCryptoSignatureUserIdRest;

    private ImageButton mAddToFromContacts;
    private ImageButton mAddCcFromContacts;
    private ImageButton mAddBccFromContacts;

    private PgpData mPgpData = null;
    private boolean mAutoEncrypt = false;
    private boolean mContinueWithoutPublicKey = false;

    private String mReferences;
    private String mInReplyTo;
    private Menu mMenu;

    private boolean mSourceProcessed = false;

    enum SimpleMessageFormat {
        TEXT,
        HTML
    }

    /**
     * The currently used message format.
     *
     * <p>
     * <strong>Note:</strong>
     * Don't modify this field directly. Use {@link #updateMessageFormat()}.
     * </p>
     */
    private SimpleMessageFormat mMessageFormat;

    private QuoteStyle mQuoteStyle;

    private boolean mDraftNeedsSaving = false;
    private boolean mPreventDraftSaving = false;

    /**
     * If this is {@code true} we don't save the message as a draft in {@link #onPause()}.
     */
    private boolean mIgnoreOnPause = false;

    /**
     * The database ID of this message's draft. This is used when saving drafts so the message in
     * the database is updated instead of being created anew. This property is INVALID_DRAFT_ID
     * until the first save.
     */
    private long mDraftId = INVALID_DRAFT_ID;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case MSG_PROGRESS_ON:
                setSupportProgressBarIndeterminateVisibility(true);
                break;
            case MSG_PROGRESS_OFF:
                setSupportProgressBarIndeterminateVisibility(false);
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

    private FontSizes mFontSizes = K9.getFontSizes();


    static class Attachment implements Serializable {
        private static final long serialVersionUID = 3642382876618963734L;
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
    public static void actionCompose(Context context, Account account) {
        String accountUuid = (account == null) ?
                Preferences.getPreferences(context).getDefaultAccount().getUuid() :
                account.getUuid();

        Intent i = new Intent(context, MessageCompose.class);
        i.putExtra(EXTRA_ACCOUNT, accountUuid);
        i.setAction(ACTION_COMPOSE);
        context.startActivity(i);
    }

    /**
     * Get intent for composing a new message as a reply to the given message. If replyAll is true
     * the function is reply all instead of simply reply.
     * @param context
     * @param account
     * @param message
     * @param replyAll
     * @param messageBody optional, for decrypted messages, null if it should be grabbed from the given message
     */
    public static Intent getActionReplyIntent(
        Context context,
        Account account,
        Message message,
        boolean replyAll,
        String messageBody) {
        Intent i = new Intent(context, MessageCompose.class);
        i.putExtra(EXTRA_MESSAGE_BODY, messageBody);
        i.putExtra(EXTRA_MESSAGE_REFERENCE, message.makeMessageReference());
        if (replyAll) {
            i.setAction(ACTION_REPLY_ALL);
        } else {
            i.setAction(ACTION_REPLY);
        }
        return i;
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
        String messageBody) {
        context.startActivity(getActionReplyIntent(context, account, message, replyAll, messageBody));
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
        String messageBody) {
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
     * @param message
     */
    public static void actionEditDraft(Context context, MessageReference messageReference) {
        Intent i = new Intent(context, MessageCompose.class);
        i.putExtra(EXTRA_MESSAGE_REFERENCE, messageReference);
        i.setAction(ACTION_EDIT_DRAFT);
        context.startActivity(i);
    }

    /*
     * This is a workaround for an annoying ( temporarly? ) issue:
     * https://github.com/JakeWharton/ActionBarSherlock/issues/449
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setSupportProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (UpgradeDatabases.actionUpgradeDatabases(this, getIntent())) {
            finish();
            return;
        }
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        if (K9.getK9ComposerThemeSetting() != K9.Theme.USE_GLOBAL) {
            // theme the whole content according to the theme (except the action bar)
            ContextThemeWrapper wrapper = new ContextThemeWrapper(this,
                    K9.getK9ThemeResourceId(K9.getK9ComposerTheme()));
            View v = ((LayoutInflater) wrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).
                    inflate(R.layout.message_compose, null);
            TypedValue outValue = new TypedValue();
            // background color needs to be forced
            wrapper.getTheme().resolveAttribute(R.attr.messageViewHeaderBackgroundColor, outValue, true);
            v.setBackgroundColor(outValue.data);
            setContentView(v);
        } else {
            setContentView(R.layout.message_compose);
        }

        final Intent intent = getIntent();

        mMessageReference = intent.getParcelableExtra(EXTRA_MESSAGE_REFERENCE);
        mSourceMessageBody = intent.getStringExtra(EXTRA_MESSAGE_BODY);

        if (K9.DEBUG && mSourceMessageBody != null)
            Log.d(K9.LOG_TAG, "Composing message with explicitly specified message body.");

        final String accountUuid = (mMessageReference != null) ?
                                   mMessageReference.accountUuid :
                                   intent.getStringExtra(EXTRA_ACCOUNT);

        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        if (mAccount == null) {
            mAccount = Preferences.getPreferences(this).getDefaultAccount();
        }

        if (mAccount == null) {
            /*
             * There are no accounts set up. This should not have happened. Prompt the
             * user to set up an account as an acceptable bailout.
             */
            startActivity(new Intent(this, Accounts.class));
            mDraftNeedsSaving = false;
            finish();
            return;
        }

        mContacts = Contacts.getInstance(MessageCompose.this);

        mAddressAdapter = new EmailAddressAdapter(this);
        mAddressValidator = new EmailAddressValidator();

        mChooseIdentityButton = (Button) findViewById(R.id.identity);
        mChooseIdentityButton.setOnClickListener(this);

        if (mAccount.getIdentities().size() == 1 &&
                Preferences.getPreferences(this).getAvailableAccounts().size() == 1) {
            mChooseIdentityButton.setVisibility(View.GONE);
        }

        mToView = (MultiAutoCompleteTextView) findViewById(R.id.to);
        mCcView = (MultiAutoCompleteTextView) findViewById(R.id.cc);
        mBccView = (MultiAutoCompleteTextView) findViewById(R.id.bcc);
        mSubjectView = (EditText) findViewById(R.id.subject);
        mSubjectView.getInputExtras(true).putBoolean("allowEmoji", true);

        mAddToFromContacts = (ImageButton) findViewById(R.id.add_to);
        mAddCcFromContacts = (ImageButton) findViewById(R.id.add_cc);
        mAddBccFromContacts = (ImageButton) findViewById(R.id.add_bcc);
        mCcWrapper = (LinearLayout) findViewById(R.id.cc_wrapper);
        mBccWrapper = (LinearLayout) findViewById(R.id.bcc_wrapper);

        if (mAccount.isAlwaysShowCcBcc()) {
            onAddCcBcc();
        }

        EditText upperSignature = (EditText)findViewById(R.id.upper_signature);
        EditText lowerSignature = (EditText)findViewById(R.id.lower_signature);

        mMessageContentView = (EditText)findViewById(R.id.message_content);
        mMessageContentView.getInputExtras(true).putBoolean("allowEmoji", true);

        mAttachments = (LinearLayout)findViewById(R.id.attachments);
        mQuotedTextShow = (Button)findViewById(R.id.quoted_text_show);
        mQuotedTextBar = findViewById(R.id.quoted_text_bar);
        mQuotedTextEdit = (ImageButton)findViewById(R.id.quoted_text_edit);
        mQuotedTextDelete = (ImageButton)findViewById(R.id.quoted_text_delete);
        mQuotedText = (EditText)findViewById(R.id.quoted_text);
        mQuotedText.getInputExtras(true).putBoolean("allowEmoji", true);

        mQuotedHTML = (MessageWebView) findViewById(R.id.quoted_html);
        mQuotedHTML.configure();
        // Disable the ability to click links in the quoted HTML page. I think this is a nice feature, but if someone
        // feels this should be a preference (or should go away all together), I'm ok with that too. -achen 20101130
        mQuotedHTML.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true;
            }
        });

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int before, int after) {
                /* do nothing */
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mDraftNeedsSaving = true;
            }

            @Override
            public void afterTextChanged(android.text.Editable s) { /* do nothing */ }
        };

        // For watching changes to the To:, Cc:, and Bcc: fields for auto-encryption on a matching
        // address.
        TextWatcher recipientWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int before, int after) {
                /* do nothing */
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mDraftNeedsSaving = true;
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                final CryptoProvider crypto = mAccount.getCryptoProvider();
                if (mAutoEncrypt && crypto.isAvailable(getApplicationContext())) {
                    for (Address address : getRecipientAddresses()) {
                        if (crypto.hasPublicKeyForEmail(getApplicationContext(),
                                address.getAddress())) {
                            mEncryptCheckbox.setChecked(true);
                            mContinueWithoutPublicKey = false;
                            break;
                        }
                    }
                }
            }
        };

        TextWatcher sigwatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int before, int after) {
                /* do nothing */
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mDraftNeedsSaving = true;
                mSignatureChanged = true;
            }

            @Override
            public void afterTextChanged(android.text.Editable s) { /* do nothing */ }
        };

        mToView.addTextChangedListener(recipientWatcher);
        mCcView.addTextChangedListener(recipientWatcher);
        mBccView.addTextChangedListener(recipientWatcher);
        mSubjectView.addTextChangedListener(watcher);

        mMessageContentView.addTextChangedListener(watcher);
        mQuotedText.addTextChangedListener(watcher);

        /* Yes, there really are poeple who ship versions of android without a contact picker */
        if (mContacts.hasContactPicker()) {
            mAddToFromContacts.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    doLaunchContactPicker(CONTACT_PICKER_TO);
                }
            });
            mAddCcFromContacts.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    doLaunchContactPicker(CONTACT_PICKER_CC);
                }
            });
            mAddBccFromContacts.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    doLaunchContactPicker(CONTACT_PICKER_BCC);
                }
            });
        } else {
            mAddToFromContacts.setVisibility(View.GONE);
            mAddCcFromContacts.setVisibility(View.GONE);
            mAddBccFromContacts.setVisibility(View.GONE);
        }
        /*
         * We set this to invisible by default. Other methods will turn it back on if it's
         * needed.
         */

        showOrHideQuotedText(QuotedTextMode.NONE);

        mQuotedTextShow.setOnClickListener(this);
        mQuotedTextEdit.setOnClickListener(this);
        mQuotedTextDelete.setOnClickListener(this);

        mToView.setAdapter(mAddressAdapter);
        mToView.setTokenizer(new Rfc822Tokenizer());
        mToView.setValidator(mAddressValidator);

        mCcView.setAdapter(mAddressAdapter);
        mCcView.setTokenizer(new Rfc822Tokenizer());
        mCcView.setValidator(mAddressValidator);

        mBccView.setAdapter(mAddressAdapter);
        mBccView.setTokenizer(new Rfc822Tokenizer());
        mBccView.setValidator(mAddressValidator);

        if (savedInstanceState != null) {
            /*
             * This data gets used in onCreate, so grab it here instead of onRestoreInstanceState
             */
            mSourceMessageProcessed = savedInstanceState.getBoolean(STATE_KEY_SOURCE_MESSAGE_PROCED, false);
        }


        if (initFromIntent(intent)) {
            mAction = Action.COMPOSE;
        } else {
            String action = intent.getAction();
            if (ACTION_COMPOSE.equals(action)) {
                mAction = Action.COMPOSE;
            } else if (ACTION_REPLY.equals(action)) {
                mAction = Action.REPLY;
            } else if (ACTION_REPLY_ALL.equals(action)) {
                mAction = Action.REPLY_ALL;
            } else if (ACTION_FORWARD.equals(action)) {
                mAction = Action.FORWARD;
            } else if (ACTION_EDIT_DRAFT.equals(action)) {
                mAction = Action.EDIT_DRAFT;
            } else {
                // This shouldn't happen
                Log.w(K9.LOG_TAG, "MessageCompose was started with an unsupported action");
                mAction = Action.COMPOSE;
            }
        }

        if (mIdentity == null) {
            mIdentity = mAccount.getIdentity(0);
        }

        if (mAccount.isSignatureBeforeQuotedText()) {
            mSignatureView = upperSignature;
            lowerSignature.setVisibility(View.GONE);
        } else {
            mSignatureView = lowerSignature;
            upperSignature.setVisibility(View.GONE);
        }
        mSignatureView.addTextChangedListener(sigwatcher);

        if (!mIdentity.getSignatureUse()) {
            mSignatureView.setVisibility(View.GONE);
        }

        mReadReceipt = mAccount.isMessageReadReceiptAlways();
        mQuoteStyle = mAccount.getQuoteStyle();

        updateFrom();

        if (!mSourceMessageProcessed) {
            updateSignature();

            if (mAction == Action.REPLY || mAction == Action.REPLY_ALL ||
                    mAction == Action.FORWARD || mAction == Action.EDIT_DRAFT) {
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

            if (mAction != Action.EDIT_DRAFT) {
                addAddresses(mBccView, mAccount.getAlwaysBcc());
            }
        }

        if (mAction == Action.REPLY || mAction == Action.REPLY_ALL) {
            mMessageReference.flag = Flag.ANSWERED;
        }

        if (mAction == Action.REPLY || mAction == Action.REPLY_ALL ||
                mAction == Action.EDIT_DRAFT) {
            //change focus to message body.
            mMessageContentView.requestFocus();
        } else {
            // Explicitly set focus to "To:" input field (see issue 2998)
            mToView.requestFocus();
        }

        if (mAction == Action.FORWARD) {
            mMessageReference.flag = Flag.FORWARDED;
        }

        mEncryptLayout = findViewById(R.id.layout_encrypt);
        mCryptoSignatureCheckbox = (CheckBox)findViewById(R.id.cb_crypto_signature);
        mCryptoSignatureUserId = (TextView)findViewById(R.id.userId);
        mCryptoSignatureUserIdRest = (TextView)findViewById(R.id.userIdRest);
        mEncryptCheckbox = (CheckBox)findViewById(R.id.cb_encrypt);
        mEncryptCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateMessageFormat();
            }
        });

        if (mSourceMessageBody != null) {
            // mSourceMessageBody is set to something when replying to and forwarding decrypted
            // messages, so the sender probably wants the message to be encrypted.
            mEncryptCheckbox.setChecked(true);
        }

        initializeCrypto();
        final CryptoProvider crypto = mAccount.getCryptoProvider();
        if (crypto.isAvailable(this)) {
            mEncryptLayout.setVisibility(View.VISIBLE);
            mCryptoSignatureCheckbox.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    CheckBox checkBox = (CheckBox) v;
                    if (checkBox.isChecked()) {
                        mPreventDraftSaving = true;
                        if (!crypto.selectSecretKey(MessageCompose.this, mPgpData)) {
                            mPreventDraftSaving = false;
                        }
                        checkBox.setChecked(false);
                    } else {
                        mPgpData.setSignatureKeyId(0);
                        updateEncryptLayout();
                    }
                }
            });

            if (mAccount.getCryptoAutoSignature()) {
                long ids[] = crypto.getSecretKeyIdsFromEmail(this, mIdentity.getEmail());
                if (ids != null && ids.length > 0) {
                    mPgpData.setSignatureKeyId(ids[0]);
                    mPgpData.setSignatureUserId(crypto.getUserId(this, ids[0]));
                } else {
                    mPgpData.setSignatureKeyId(0);
                    mPgpData.setSignatureUserId(null);
                }
            }
            updateEncryptLayout();
            mAutoEncrypt = mAccount.isCryptoAutoEncrypt();
        } else {
            mEncryptLayout.setVisibility(View.GONE);
        }

        mDraftNeedsSaving = false;

        // Set font size of input controls
        int fontSize = mFontSizes.getMessageComposeInput();
        mFontSizes.setViewTextSize(mToView, fontSize);
        mFontSizes.setViewTextSize(mCcView, fontSize);
        mFontSizes.setViewTextSize(mBccView, fontSize);
        mFontSizes.setViewTextSize(mSubjectView, fontSize);
        mFontSizes.setViewTextSize(mMessageContentView, fontSize);
        mFontSizes.setViewTextSize(mQuotedText, fontSize);
        mFontSizes.setViewTextSize(mSignatureView, fontSize);


        updateMessageFormat();

        setTitle();
    }

    /**
     * Handle external intents that trigger the message compose activity.
     *
     * <p>
     * Supported external intents:
     * <ul>
     *   <li>{@link Intent#ACTION_VIEW}</li>
     *   <li>{@link Intent#ACTION_SENDTO}</li>
     *   <li>{@link Intent#ACTION_SEND}</li>
     *   <li>{@link Intent#ACTION_SEND_MULTIPLE}</li>
     * </ul>
     * </p>
     *
     * @param intent
     *         The (external) intent that started the activity.
     *
     * @return {@code true}, if this activity was started by an external intent. {@code false},
     *         otherwise.
     */
    private boolean initFromIntent(final Intent intent) {
        boolean startedByExternalIntent = false;
        final String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action) || Intent.ACTION_SENDTO.equals(action)) {
            startedByExternalIntent = true;

            /*
             * Someone has clicked a mailto: link. The address is in the URI.
             */
            if (intent.getData() != null) {
                Uri uri = intent.getData();
                if ("mailto".equals(uri.getScheme())) {
                    initializeFromMailto(uri);
                }
            }

            /*
             * Note: According to the documenation ACTION_VIEW and ACTION_SENDTO don't accept
             * EXTRA_* parameters.
             * And previously we didn't process these EXTRAs. But it looks like nobody bothers to
             * read the official documentation and just copies wrong sample code that happens to
             * work with the AOSP Email application. And because even big players get this wrong,
             * we're now finally giving in and read the EXTRAs for ACTION_SENDTO (below).
             */
        }

        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action) ||
                Intent.ACTION_SENDTO.equals(action)) {
            startedByExternalIntent = true;

            /*
             * Note: Here we allow a slight deviation from the documentated behavior.
             * EXTRA_TEXT is used as message body (if available) regardless of the MIME
             * type of the intent. In addition one or multiple attachments can be added
             * using EXTRA_STREAM.
             */
            CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            // Only use EXTRA_TEXT if the body hasn't already been set by the mailto URI
            if (text != null && mMessageContentView.getText().length() == 0) {
                mMessageContentView.setText(text);
            }

            String type = intent.getType();
            if (Intent.ACTION_SEND.equals(action)) {
                Uri stream = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (stream != null) {
                    addAttachment(stream, type);
                }
            } else {
                ArrayList<Parcelable> list = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (list != null) {
                    for (Parcelable parcelable : list) {
                        Uri stream = (Uri) parcelable;
                        if (stream != null) {
                            addAttachment(stream, type);
                        }
                    }
                }
            }

            String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            // Only use EXTRA_SUBJECT if the subject hasn't already been set by the mailto URI
            if (subject != null && mSubjectView.getText().length() == 0) {
                mSubjectView.setText(subject);
            }

            String[] extraEmail = intent.getStringArrayExtra(Intent.EXTRA_EMAIL);
            String[] extraCc = intent.getStringArrayExtra(Intent.EXTRA_CC);
            String[] extraBcc = intent.getStringArrayExtra(Intent.EXTRA_BCC);

            if (extraEmail != null) {
                addRecipients(mToView, Arrays.asList(extraEmail));
            }

            boolean ccOrBcc = false;
            if (extraCc != null) {
                ccOrBcc |= addRecipients(mCcView, Arrays.asList(extraCc));
            }

            if (extraBcc != null) {
                ccOrBcc |= addRecipients(mBccView, Arrays.asList(extraBcc));
            }

            if (ccOrBcc) {
                // Display CC and BCC text fields if CC or BCC recipients were set by the intent.
                onAddCcBcc();
            }
        }

        return startedByExternalIntent;
    }

    private boolean addRecipients(TextView view, List<String> recipients) {
        if (recipients == null || recipients.size() == 0) {
            return false;
        }

        StringBuilder addressList = new StringBuilder();

        // Read current contents of the TextView
        String text = view.getText().toString();
        addressList.append(text);

        // Add comma if necessary
        if (text.length() != 0 && !(text.endsWith(", ") || text.endsWith(","))) {
            addressList.append(", ");
        }

        // Add recipients
        for (String recipient : recipients) {
            addressList.append(recipient);
            addressList.append(", ");
        }

        view.setText(addressList);

        return true;
    }

    private void initializeCrypto() {
        if (mPgpData != null) {
            return;
        }
        mPgpData = new PgpData();
    }

    /**
     * Fill the encrypt layout with the latest data about signature key and encryption keys.
     */
    public void updateEncryptLayout() {
        if (!mPgpData.hasSignatureKey()) {
            mCryptoSignatureCheckbox.setText(R.string.btn_crypto_sign);
            mCryptoSignatureCheckbox.setChecked(false);
            mCryptoSignatureUserId.setVisibility(View.INVISIBLE);
            mCryptoSignatureUserIdRest.setVisibility(View.INVISIBLE);
        } else {
            // if a signature key is selected, then the checkbox itself has no text
            mCryptoSignatureCheckbox.setText("");
            mCryptoSignatureCheckbox.setChecked(true);
            mCryptoSignatureUserId.setVisibility(View.VISIBLE);
            mCryptoSignatureUserIdRest.setVisibility(View.VISIBLE);
            mCryptoSignatureUserId.setText(R.string.unknown_crypto_signature_user_id);
            mCryptoSignatureUserIdRest.setText("");

            String userId = mPgpData.getSignatureUserId();
            if (userId == null) {
                userId = mAccount.getCryptoProvider().getUserId(this, mPgpData.getSignatureKeyId());
                mPgpData.setSignatureUserId(userId);
            }

            if (userId != null) {
                String chunks[] = mPgpData.getSignatureUserId().split(" <", 2);
                mCryptoSignatureUserId.setText(chunks[0]);
                if (chunks.length > 1) {
                    mCryptoSignatureUserIdRest.setText("<" + chunks[1]);
                }
            }
        }

        updateMessageFormat();
    }

    @Override
    public void onResume() {
        super.onResume();
        mIgnoreOnPause = false;
        MessagingController.getInstance(getApplication()).addListener(mListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        MessagingController.getInstance(getApplication()).removeListener(mListener);
        // Save email as draft when activity is changed (go to home screen, call received) or screen locked
        // don't do this if only changing orientations
        if (!mIgnoreOnPause && (getChangingConfigurations() & ActivityInfo.CONFIG_ORIENTATION) == 0) {
            saveIfNeeded();
        }
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ArrayList<Uri> attachments = new ArrayList<Uri>();
        for (int i = 0, count = mAttachments.getChildCount(); i < count; i++) {
            View view = mAttachments.getChildAt(i);
            Attachment attachment = (Attachment) view.getTag();
            attachments.add(attachment.uri);
        }
        outState.putParcelableArrayList(STATE_KEY_ATTACHMENTS, attachments);
        outState.putBoolean(STATE_KEY_CC_SHOWN, mCcWrapper.getVisibility() == View.VISIBLE);
        outState.putBoolean(STATE_KEY_BCC_SHOWN, mBccWrapper.getVisibility() == View.VISIBLE);
        outState.putSerializable(STATE_KEY_QUOTED_TEXT_MODE, mQuotedTextMode);
        outState.putBoolean(STATE_KEY_SOURCE_MESSAGE_PROCED, mSourceMessageProcessed);
        outState.putLong(STATE_KEY_DRAFT_ID, mDraftId);
        outState.putSerializable(STATE_IDENTITY, mIdentity);
        outState.putBoolean(STATE_IDENTITY_CHANGED, mIdentityChanged);
        outState.putSerializable(STATE_PGP_DATA, mPgpData);
        outState.putString(STATE_IN_REPLY_TO, mInReplyTo);
        outState.putString(STATE_REFERENCES, mReferences);
        outState.putSerializable(STATE_KEY_HTML_QUOTE, mQuotedHtmlContent);
        outState.putBoolean(STATE_KEY_READ_RECEIPT, mReadReceipt);
        outState.putBoolean(STATE_KEY_DRAFT_NEEDS_SAVING, mDraftNeedsSaving);
        outState.putBoolean(STATE_KEY_FORCE_PLAIN_TEXT, mForcePlainText);
        outState.putSerializable(STATE_KEY_QUOTED_TEXT_FORMAT, mQuotedTextFormat);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        ArrayList<Parcelable> attachments = savedInstanceState.getParcelableArrayList(STATE_KEY_ATTACHMENTS);
        mAttachments.removeAllViews();
        for (Parcelable p : attachments) {
            Uri uri = (Uri) p;
            addAttachment(uri);
        }

        mReadReceipt = savedInstanceState
                       .getBoolean(STATE_KEY_READ_RECEIPT);
        mCcWrapper.setVisibility(savedInstanceState.getBoolean(STATE_KEY_CC_SHOWN) ? View.VISIBLE
                                 : View.GONE);
        mBccWrapper.setVisibility(savedInstanceState
                                  .getBoolean(STATE_KEY_BCC_SHOWN) ? View.VISIBLE : View.GONE);

        // This method is called after the action bar menu has already been created and prepared.
        // So compute the visibility of the "Add Cc/Bcc" menu item again.
        computeAddCcBccVisibility();

        showOrHideQuotedText(
                (QuotedTextMode) savedInstanceState.getSerializable(STATE_KEY_QUOTED_TEXT_MODE));

        mQuotedHtmlContent =
                (InsertableHtmlContent) savedInstanceState.getSerializable(STATE_KEY_HTML_QUOTE);
        if (mQuotedHtmlContent != null && mQuotedHtmlContent.getQuotedContent() != null) {
            mQuotedHTML.setText(mQuotedHtmlContent.getQuotedContent());
        }

        mDraftId = savedInstanceState.getLong(STATE_KEY_DRAFT_ID);
        mIdentity = (Identity)savedInstanceState.getSerializable(STATE_IDENTITY);
        mIdentityChanged = savedInstanceState.getBoolean(STATE_IDENTITY_CHANGED);
        mPgpData = (PgpData) savedInstanceState.getSerializable(STATE_PGP_DATA);
        mInReplyTo = savedInstanceState.getString(STATE_IN_REPLY_TO);
        mReferences = savedInstanceState.getString(STATE_REFERENCES);
        mDraftNeedsSaving = savedInstanceState.getBoolean(STATE_KEY_DRAFT_NEEDS_SAVING);
        mForcePlainText = savedInstanceState.getBoolean(STATE_KEY_FORCE_PLAIN_TEXT);
        mQuotedTextFormat = (SimpleMessageFormat) savedInstanceState.getSerializable(
                STATE_KEY_QUOTED_TEXT_FORMAT);

        initializeCrypto();
        updateFrom();
        updateSignature();
        updateEncryptLayout();

        updateMessageFormat();
    }

    private void setTitle() {
        switch (mAction) {
            case REPLY: {
                setTitle(R.string.compose_title_reply);
                break;
            }
            case REPLY_ALL: {
                setTitle(R.string.compose_title_reply_all);
                break;
            }
            case FORWARD: {
                setTitle(R.string.compose_title_forward);
                break;
            }
            case COMPOSE:
            default: {
                setTitle(R.string.compose_title_compose);
                break;
            }
        }
    }

    private void addAddresses(MultiAutoCompleteTextView view, String addresses) {
        if (StringUtils.isNullOrEmpty(addresses)) {
            return;
        }
        for (String address : addresses.split(",")) {
            addAddress(view, new Address(address, ""));
        }
    }

    private void addAddresses(MultiAutoCompleteTextView view, Address[] addresses) {
        if (addresses == null) {
            return;
        }
        for (Address address : addresses) {
            addAddress(view, address);
        }
    }

    private void addAddress(MultiAutoCompleteTextView view, Address address) {
        view.append(address + ", ");
    }

    private Address[] getAddresses(MultiAutoCompleteTextView view) {

        return Address.parseUnencoded(view.getText().toString().trim());
    }

    /*
     * Returns an Address array of recipients this email will be sent to.
     * @return Address array of recipients this email will be sent to.
     */
    private Address[] getRecipientAddresses() {
        String addresses = mToView.getText().toString() + mCcView.getText().toString()
                + mBccView.getText().toString();
        return Address.parseUnencoded(addresses.trim());
    }

    /*
     * Build the Body that will contain the text of the message. We'll decide where to
     * include it later. Draft messages are treated somewhat differently in that signatures are not
     * appended and HTML separators between composed text and quoted text are not added.
     * @param isDraft If we should build a message that will be saved as a draft (as opposed to sent).
     */
    private TextBody buildText(boolean isDraft) {
        return buildText(isDraft, mMessageFormat);
    }

    /**
     * Build the {@link Body} that will contain the text of the message.
     *
     * <p>
     * Draft messages are treated somewhat differently in that signatures are not appended and HTML
     * separators between composed text and quoted text are not added.
     * </p>
     *
     * @param isDraft
     *         If {@code true} we build a message that will be saved as a draft (as opposed to
     *         sent).
     * @param messageFormat
     *         Specifies what type of message to build ({@code text/plain} vs. {@code text/html}).
     *
     * @return {@link TextBody} instance that contains the entered text and possibly the quoted
     *         original message.
     */
    private TextBody buildText(boolean isDraft, SimpleMessageFormat messageFormat) {
        // The length of the formatted version of the user-supplied text/reply
        int composedMessageLength;

        // The offset of the user-supplied text/reply in the final text body
        int composedMessageOffset;

        /*
         * Find out if we need to include the original message as quoted text.
         *
         * We include the quoted text in the body if the user didn't choose to hide it. We always
         * include the quoted text when we're saving a draft. That's so the user is able to
         * "un-hide" the quoted text if (s)he opens a saved draft.
         */
        boolean includeQuotedText = (mQuotedTextMode.equals(QuotedTextMode.SHOW) || isDraft);

        // Reply after quote makes no sense for HEADER style replies
        boolean replyAfterQuote = (mQuoteStyle == QuoteStyle.HEADER) ?
                false : mAccount.isReplyAfterQuote();

        boolean signatureBeforeQuotedText = mAccount.isSignatureBeforeQuotedText();

        // Get the user-supplied text
        String text = mMessageContentView.getText().toString();

        // Handle HTML separate from the rest of the text content
        if (messageFormat == SimpleMessageFormat.HTML) {

            // Do we have to modify an existing message to include our reply?
            if (includeQuotedText && mQuotedHtmlContent != null) {
                if (K9.DEBUG) {
                    Log.d(K9.LOG_TAG, "insertable: " + mQuotedHtmlContent.toDebugString());
                }

                if (!isDraft) {
                    // Append signature to the reply
                    if (replyAfterQuote || signatureBeforeQuotedText) {
                        text = appendSignature(text);
                    }
                }

                // Convert the text to HTML
                text = HtmlConverter.textToHtmlFragment(text);

                /*
                 * Set the insertion location based upon our reply after quote setting.
                 * Additionally, add some extra separators between the composed message and quoted
                 * message depending on the quote location. We only add the extra separators when
                 * we're sending, that way when we load a draft, we don't have to know the length
                 * of the separators to remove them before editing.
                 */
                if (replyAfterQuote) {
                    mQuotedHtmlContent.setInsertionLocation(
                            InsertableHtmlContent.InsertionLocation.AFTER_QUOTE);
                    if (!isDraft) {
                        text = "<br clear=\"all\">" + text;
                    }
                } else {
                    mQuotedHtmlContent.setInsertionLocation(
                            InsertableHtmlContent.InsertionLocation.BEFORE_QUOTE);
                    if (!isDraft) {
                        text += "<br><br>";
                    }
                }

                if (!isDraft) {
                    // Place signature immediately after the quoted text
                    if (!(replyAfterQuote || signatureBeforeQuotedText)) {
                        mQuotedHtmlContent.insertIntoQuotedFooter(getSignatureHtml());
                    }
                }

                mQuotedHtmlContent.setUserContent(text);

                // Save length of the body and its offset.  This is used when thawing drafts.
                composedMessageLength = text.length();
                composedMessageOffset = mQuotedHtmlContent.getInsertionPoint();
                text = mQuotedHtmlContent.toString();

            } else {
                // There is no text to quote so simply append the signature if available
                if (!isDraft) {
                    text = appendSignature(text);
                }

                // Convert the text to HTML
                text = HtmlConverter.textToHtmlFragment(text);

                //TODO: Wrap this in proper HTML tags

                composedMessageLength = text.length();
                composedMessageOffset = 0;
            }

        } else {
            // Capture composed message length before we start attaching quoted parts and signatures.
            composedMessageLength = text.length();
            composedMessageOffset = 0;

            if (!isDraft) {
                // Append signature to the text/reply
                if (replyAfterQuote || signatureBeforeQuotedText) {
                    text = appendSignature(text);
                }
            }

            if (includeQuotedText) {
                String quotedText = mQuotedText.getText().toString();
                if (replyAfterQuote) {
                    composedMessageOffset = quotedText.length() + "\n".length();
                    text = quotedText + "\n" + text;
                } else {
                    text += "\n\n" + quotedText.toString();
                }
            }

            if (!isDraft) {
                // Place signature immediately after the quoted text
                if (!(replyAfterQuote || signatureBeforeQuotedText)) {
                    text = appendSignature(text);
                }
            }
        }

        TextBody body = new TextBody(text);
        body.setComposedMessageLength(composedMessageLength);
        body.setComposedMessageOffset(composedMessageOffset);

        return body;
    }

    /**
     * Build the final message to be sent (or saved). If there is another message quoted in this one, it will be baked
     * into the final message here.
     * @param isDraft Indicates if this message is a draft or not. Drafts do not have signatures
     *  appended and have some extra metadata baked into their header for use during thawing.
     * @return Message to be sent.
     * @throws MessagingException
     */
    private MimeMessage createMessage(boolean isDraft) throws MessagingException {
        MimeMessage message = new MimeMessage();
        message.addSentDate(new Date());
        Address from = new Address(mIdentity.getEmail(), mIdentity.getName());
        message.setFrom(from);
        message.setRecipients(RecipientType.TO, getAddresses(mToView));
        message.setRecipients(RecipientType.CC, getAddresses(mCcView));
        message.setRecipients(RecipientType.BCC, getAddresses(mBccView));
        message.setSubject(mSubjectView.getText().toString());
        if (mReadReceipt) {
            message.setHeader("Disposition-Notification-To", from.toEncodedString());
            message.setHeader("X-Confirm-Reading-To", from.toEncodedString());
            message.setHeader("Return-Receipt-To", from.toEncodedString());
        }
        message.setHeader("User-Agent", getString(R.string.message_header_mua));

        final String replyTo = mIdentity.getReplyTo();
        if (replyTo != null) {
            message.setReplyTo(new Address[] { new Address(replyTo) });
        }

        if (mInReplyTo != null) {
            message.setInReplyTo(mInReplyTo);
        }

        if (mReferences != null) {
            message.setReferences(mReferences);
        }

        // Build the body.
        // TODO FIXME - body can be either an HTML or Text part, depending on whether we're in
        // HTML mode or not.  Should probably fix this so we don't mix up html and text parts.
        TextBody body = null;
        if (mPgpData.getEncryptedData() != null) {
            String text = mPgpData.getEncryptedData();
            body = new TextBody(text);
        } else {
            body = buildText(isDraft);
        }

        // text/plain part when mMessageFormat == MessageFormat.HTML
        TextBody bodyPlain = null;

        final boolean hasAttachments = mAttachments.getChildCount() > 0;

        if (mMessageFormat == SimpleMessageFormat.HTML) {
            // HTML message (with alternative text part)

            // This is the compiled MIME part for an HTML message.
            MimeMultipart composedMimeMessage = new MimeMultipart();
            composedMimeMessage.setSubType("alternative");   // Let the receiver select either the text or the HTML part.
            composedMimeMessage.addBodyPart(new MimeBodyPart(body, "text/html"));
            bodyPlain = buildText(isDraft, SimpleMessageFormat.TEXT);
            composedMimeMessage.addBodyPart(new MimeBodyPart(bodyPlain, "text/plain"));

            if (hasAttachments) {
                // If we're HTML and have attachments, we have a MimeMultipart container to hold the
                // whole message (mp here), of which one part is a MimeMultipart container
                // (composedMimeMessage) with the user's composed messages, and subsequent parts for
                // the attachments.
                MimeMultipart mp = new MimeMultipart();
                mp.addBodyPart(new MimeBodyPart(composedMimeMessage));
                addAttachmentsToMessage(mp);
                message.setBody(mp);
            } else {
                // If no attachments, our multipart/alternative part is the only one we need.
                message.setBody(composedMimeMessage);
            }
        } else if (mMessageFormat == SimpleMessageFormat.TEXT) {
            // Text-only message.
            if (hasAttachments) {
                MimeMultipart mp = new MimeMultipart();
                mp.addBodyPart(new MimeBodyPart(body, "text/plain"));
                addAttachmentsToMessage(mp);
                message.setBody(mp);
            } else {
                // No attachments to include, just stick the text body in the message and call it good.
                message.setBody(body);
            }
        }

        // If this is a draft, add metadata for thawing.
        if (isDraft) {
            // Add the identity to the message.
            message.addHeader(K9.IDENTITY_HEADER, buildIdentityHeader(body, bodyPlain));
        }

        return message;
    }

    /**
     * Add attachments as parts into a MimeMultipart container.
     * @param mp MimeMultipart container in which to insert parts.
     * @throws MessagingException
     */
    private void addAttachmentsToMessage(final MimeMultipart mp) throws MessagingException {
        for (int i = 0, count = mAttachments.getChildCount(); i < count; i++) {
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
    private enum IdentityField {
        LENGTH("l"),
        OFFSET("o"),
        FOOTER_OFFSET("fo"),
        PLAIN_LENGTH("pl"),
        PLAIN_OFFSET("po"),
        MESSAGE_FORMAT("f"),
        MESSAGE_READ_RECEIPT("r"),
        SIGNATURE("s"),
        NAME("n"),
        EMAIL("e"),
        // TODO - store a reference to the message being replied so we can mark it at the time of send.
        ORIGINAL_MESSAGE("m"),
        CURSOR_POSITION("p"),   // Where in the message your cursor was when you saved.
        QUOTED_TEXT_MODE("q"),
        QUOTE_STYLE("qs");

        private final String value;

        IdentityField(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        /**
         * Get the list of IdentityFields that should be integer values.
         *
         * <p>
         * These values are sanity checked for integer-ness during decoding.
         * </p>
         *
         * @return The list of integer {@link IdentityField}s.
         */
        public static IdentityField[] getIntegerFields() {
            return new IdentityField[] { LENGTH, OFFSET, FOOTER_OFFSET, PLAIN_LENGTH, PLAIN_OFFSET };
        }
    }

    // Version identifier for "new style" identity. ! is an impossible value in base64 encoding, so we
    // use that to determine which version we're in.
    private static final String IDENTITY_VERSION_1 = "!";

    /**
     * Build the identity header string. This string contains metadata about a draft message to be
     * used upon loading a draft for composition. This should be generated at the time of saving a
     * draft.<br>
     * <br>
     * This is a URL-encoded key/value pair string.  The list of possible values are in {@link IdentityField}.
     * @param body {@link TextBody} to analyze for body length and offset.
     * @param bodyPlain {@link TextBody} to analyze for body length and offset. May be null.
     * @return Identity string.
     */
    private String buildIdentityHeader(final TextBody body, final TextBody bodyPlain) {
        Uri.Builder uri = new Uri.Builder();
        if (body.getComposedMessageLength() != null && body.getComposedMessageOffset() != null) {
            // See if the message body length is already in the TextBody.
            uri.appendQueryParameter(IdentityField.LENGTH.value(), body.getComposedMessageLength().toString());
            uri.appendQueryParameter(IdentityField.OFFSET.value(), body.getComposedMessageOffset().toString());
        } else {
            // If not, calculate it now.
            uri.appendQueryParameter(IdentityField.LENGTH.value(), Integer.toString(body.getText().length()));
            uri.appendQueryParameter(IdentityField.OFFSET.value(), Integer.toString(0));
        }
        if (mQuotedHtmlContent != null) {
            uri.appendQueryParameter(IdentityField.FOOTER_OFFSET.value(),
                    Integer.toString(mQuotedHtmlContent.getFooterInsertionPoint()));
        }
        if (bodyPlain != null) {
            if (bodyPlain.getComposedMessageLength() != null && bodyPlain.getComposedMessageOffset() != null) {
                // See if the message body length is already in the TextBody.
                uri.appendQueryParameter(IdentityField.PLAIN_LENGTH.value(), bodyPlain.getComposedMessageLength().toString());
                uri.appendQueryParameter(IdentityField.PLAIN_OFFSET.value(), bodyPlain.getComposedMessageOffset().toString());
            } else {
                // If not, calculate it now.
                uri.appendQueryParameter(IdentityField.PLAIN_LENGTH.value(), Integer.toString(body.getText().length()));
                uri.appendQueryParameter(IdentityField.PLAIN_OFFSET.value(), Integer.toString(0));
            }
        }
        // Save the quote style (useful for forwards).
        uri.appendQueryParameter(IdentityField.QUOTE_STYLE.value(), mQuoteStyle.name());

        // Save the message format for this offset.
        uri.appendQueryParameter(IdentityField.MESSAGE_FORMAT.value(), mMessageFormat.name());

        // If we're not using the standard identity of signature, append it on to the identity blob.
        if (mSignatureChanged) {
            uri.appendQueryParameter(IdentityField.SIGNATURE.value(), mSignatureView.getText().toString());
        }

        if (mIdentityChanged) {
            uri.appendQueryParameter(IdentityField.NAME.value(), mIdentity.getName());
            uri.appendQueryParameter(IdentityField.EMAIL.value(), mIdentity.getEmail());
        }

        if (mMessageReference != null) {
            uri.appendQueryParameter(IdentityField.ORIGINAL_MESSAGE.value(), mMessageReference.toIdentityString());
        }

        uri.appendQueryParameter(IdentityField.CURSOR_POSITION.value(), Integer.toString(mMessageContentView.getSelectionStart()));

        uri.appendQueryParameter(IdentityField.QUOTED_TEXT_MODE.value(), mQuotedTextMode.name());

        String k9identity = IDENTITY_VERSION_1 + uri.build().getEncodedQuery();

        if (K9.DEBUG) {
            Log.d(K9.LOG_TAG, "Generated identity: " + k9identity);
        }

        return k9identity;
    }

    /**
     * Parse an identity string.  Handles both legacy and new (!) style identities.
     *
     * @param identityString
     *         The encoded identity string that was saved in a drafts header.
     *
     * @return A map containing the value for each {@link IdentityField} in the identity string.
     */
    private Map<IdentityField, String> parseIdentityHeader(final String identityString) {
        Map<IdentityField, String> identity = new HashMap<IdentityField, String>();

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "Decoding identity: " + identityString);

        if (identityString == null || identityString.length() < 1) {
            return identity;
        }

        // Check to see if this is a "next gen" identity.
        if (identityString.charAt(0) == IDENTITY_VERSION_1.charAt(0) && identityString.length() > 2) {
            Uri.Builder builder = new Uri.Builder();
            builder.encodedQuery(identityString.substring(1));  // Need to cut off the ! at the beginning.
            Uri uri = builder.build();
            for (IdentityField key : IdentityField.values()) {
                String value = uri.getQueryParameter(key.value());
                if (value != null) {
                    identity.put(key, value);
                }
            }

            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "Decoded identity: " + identity.toString());

            // Sanity check our Integers so that recipients of this result don't have to.
            for (IdentityField key : IdentityField.getIntegerFields()) {
                if (identity.get(key) != null) {
                    try {
                        Integer.parseInt(identity.get(key));
                    } catch (NumberFormatException e) {
                        Log.e(K9.LOG_TAG, "Invalid " + key.name() + " field in identity: " + identity.get(key));
                    }
                }
            }
        } else {
            // Legacy identity

            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "Got a saved legacy identity: " + identityString);
            StringTokenizer tokens = new StringTokenizer(identityString, ":", false);

            // First item is the body length. We use this to separate the composed reply from the quoted text.
            if (tokens.hasMoreTokens()) {
                String bodyLengthS = Utility.base64Decode(tokens.nextToken());
                try {
                    identity.put(IdentityField.LENGTH, Integer.valueOf(bodyLengthS).toString());
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Unable to parse bodyLength '" + bodyLengthS + "'");
                }
            }
            if (tokens.hasMoreTokens()) {
                identity.put(IdentityField.SIGNATURE, Utility.base64Decode(tokens.nextToken()));
            }
            if (tokens.hasMoreTokens()) {
                identity.put(IdentityField.NAME, Utility.base64Decode(tokens.nextToken()));
            }
            if (tokens.hasMoreTokens()) {
                identity.put(IdentityField.EMAIL, Utility.base64Decode(tokens.nextToken()));
            }
            if (tokens.hasMoreTokens()) {
                identity.put(IdentityField.QUOTED_TEXT_MODE, Utility.base64Decode(tokens.nextToken()));
            }
        }

        return identity;
    }


    private String appendSignature(String originalText) {
        String text = originalText;
        if (mIdentity.getSignatureUse()) {
            String signature = mSignatureView.getText().toString();

            if (signature != null && !signature.contentEquals("")) {
                text += "\n" + signature;
            }
        }

        return text;
    }

    /**
     * Get an HTML version of the signature in the #mSignatureView, if any.
     * @return HTML version of signature.
     */
    private String getSignatureHtml() {
        String signature = "";
        if (mIdentity.getSignatureUse()) {
            signature = mSignatureView.getText().toString();
            if(!StringUtils.isNullOrEmpty(signature)) {
                signature = HtmlConverter.textToHtmlFragment("\n" + signature);
            }
        }
        return signature;
    }

    private void sendMessage() {
        new SendMessageTask().execute();
    }

    private void saveMessage() {
        new SaveMessageTask().execute();
    }

    private void saveIfNeeded() {
        if (!mDraftNeedsSaving || mPreventDraftSaving || mPgpData.hasEncryptionKeys() ||
                mEncryptCheckbox.isChecked() || !mAccount.hasDraftsFolder()) {
            return;
        }

        mDraftNeedsSaving = false;
        saveMessage();
    }

    public void onEncryptionKeySelectionDone() {
        if (mPgpData.hasEncryptionKeys()) {
            onSend();
        } else {
            Toast.makeText(this, R.string.send_aborted, Toast.LENGTH_SHORT).show();
        }
    }

    public void onEncryptDone() {
        if (mPgpData.getEncryptedData() != null) {
            onSend();
        } else {
            Toast.makeText(this, R.string.send_aborted, Toast.LENGTH_SHORT).show();
        }
    }

    private void onSend() {
        if (getAddresses(mToView).length == 0 && getAddresses(mCcView).length == 0 && getAddresses(mBccView).length == 0) {
            mToView.setError(getString(R.string.message_compose_error_no_recipients));
            Toast.makeText(this, getString(R.string.message_compose_error_no_recipients), Toast.LENGTH_LONG).show();
            return;
        }
        final CryptoProvider crypto = mAccount.getCryptoProvider();
        if (mEncryptCheckbox.isChecked() && !mPgpData.hasEncryptionKeys()) {
            // key selection before encryption
            StringBuilder emails = new StringBuilder();
            for (Address address : getRecipientAddresses()) {
                if (emails.length() != 0) {
                    emails.append(',');
                }
                emails.append(address.getAddress());
                if (!mContinueWithoutPublicKey &&
                        !crypto.hasPublicKeyForEmail(this, address.getAddress())) {
                    showDialog(DIALOG_CONTINUE_WITHOUT_PUBLIC_KEY);
                    return;
                }
            }
            if (emails.length() != 0) {
                emails.append(',');
            }
            emails.append(mIdentity.getEmail());

            mPreventDraftSaving = true;
            if (!crypto.selectEncryptionKeys(MessageCompose.this, emails.toString(), mPgpData)) {
                mPreventDraftSaving = false;
            }
            return;
        }
        if (mPgpData.hasEncryptionKeys() || mPgpData.hasSignatureKey()) {
            if (mPgpData.getEncryptedData() == null) {
                String text = buildText(false).getText();
                mPreventDraftSaving = true;
                if (!crypto.encrypt(this, text, mPgpData)) {
                    mPreventDraftSaving = false;
                }
                return;
            }
        }
        sendMessage();

        if (mMessageReference != null && mMessageReference.flag != null) {
            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "Setting referenced message (" + mMessageReference.folderName + ", " + mMessageReference.uid + ") flag to " + mMessageReference.flag);

            final Account account = Preferences.getPreferences(this).getAccount(mMessageReference.accountUuid);
            final String folderName = mMessageReference.folderName;
            final String sourceMessageUid = mMessageReference.uid;
            MessagingController.getInstance(getApplication()).setFlag(account, folderName, sourceMessageUid, mMessageReference.flag, true);
        }

        mDraftNeedsSaving = false;
        finish();
    }

    private void onDiscard() {
        if (mDraftId != INVALID_DRAFT_ID) {
            MessagingController.getInstance(getApplication()).deleteDraft(mAccount, mDraftId);
            mDraftId = INVALID_DRAFT_ID;
        }
        mHandler.sendEmptyMessage(MSG_DISCARDED_DRAFT);
        mDraftNeedsSaving = false;
        finish();
    }

    private void onSave() {
        saveIfNeeded();
        finish();
    }

    private void onAddCcBcc() {
        mCcWrapper.setVisibility(View.VISIBLE);
        mBccWrapper.setVisibility(View.VISIBLE);
        computeAddCcBccVisibility();
    }

    /**
     * Hide the 'Add Cc/Bcc' menu item when both fields are visible.
     */
    private void computeAddCcBccVisibility() {
        if (mMenu != null && mCcWrapper.getVisibility() == View.VISIBLE &&
                mBccWrapper.getVisibility() == View.VISIBLE) {
            mMenu.findItem(R.id.add_cc_bcc).setVisible(false);
        }
    }

    private void onReadReceipt() {
        CharSequence txt;
        if (mReadReceipt == false) {
            txt = getString(R.string.read_receipt_enabled);
            mReadReceipt = true;
        } else {
            txt = getString(R.string.read_receipt_disabled);
            mReadReceipt = false;
        }
        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context, txt, Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * Kick off a picker for whatever kind of MIME types we'll accept and let Android take over.
     */
    private void onAddAttachment() {
        if (K9.isGalleryBuggy()) {
            if (K9.useGalleryBugWorkaround()) {
                Toast.makeText(MessageCompose.this,
                               getString(R.string.message_compose_use_workaround),
                               Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MessageCompose.this,
                               getString(R.string.message_compose_buggy_gallery),
                               Toast.LENGTH_LONG).show();
            }
        }

        onAddAttachment2("*/*");
    }

    /**
     * Kick off a picker for the specified MIME type and let Android take over.
     *
     * @param mime_type
     *         The MIME type we want our attachment to have.
     */
    private void onAddAttachment2(final String mime_type) {
        if (mAccount.getCryptoProvider().isAvailable(this)) {
            Toast.makeText(this, R.string.attachment_encryption_unsupported, Toast.LENGTH_LONG).show();
        }
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(mime_type);
        mIgnoreOnPause = true;
        startActivityForResult(Intent.createChooser(i, null), ACTIVITY_REQUEST_PICK_ATTACHMENT);
    }

    private void addAttachment(Uri uri) {
        addAttachment(uri, null);
    }

    private void addAttachment(Uri uri, String contentType) {
        long size = -1;
        String name = null;

        ContentResolver contentResolver = getContentResolver();

        Cursor metadataCursor = contentResolver.query(
                                    uri,
                                    new String[] { OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE },
                                    null,
                                    null,
                                    null);

        if (metadataCursor != null) {
            try {
                if (metadataCursor.moveToFirst()) {
                    name = metadataCursor.getString(0);
                    size = metadataCursor.getInt(1);
                }
            } finally {
                metadataCursor.close();
            }
        }

        if (name == null) {
            name = uri.getLastPathSegment();
        }

        String usableContentType = contentType;
        if ((usableContentType == null) || (usableContentType.indexOf('*') != -1)) {
            usableContentType = contentResolver.getType(uri);
        }
        if (usableContentType == null) {
            usableContentType = MimeUtility.getMimeTypeByExtension(name);
        }

        if (size <= 0) {
            String uriString = uri.toString();
            if (uriString.startsWith("file://")) {
                Log.v(K9.LOG_TAG, uriString.substring("file://".length()));
                File f = new File(uriString.substring("file://".length()));
                size = f.length();
            } else {
                Log.v(K9.LOG_TAG, "Not a file: " + uriString);
            }
        } else {
            Log.v(K9.LOG_TAG, "old attachment.size: " + size);
        }
        Log.v(K9.LOG_TAG, "new attachment.size: " + size);

        Attachment attachment = new Attachment();
        attachment.uri = uri;
        attachment.contentType = usableContentType;
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if a CryptoSystem activity is returning, then mPreventDraftSaving was set to true
        mPreventDraftSaving = false;

        if (mAccount.getCryptoProvider().onActivityResult(this, requestCode, resultCode, data, mPgpData)) {
            return;
        }

        if (resultCode != RESULT_OK)
            return;
        if (data == null) {
            return;
        }
        switch (requestCode) {
        case ACTIVITY_REQUEST_PICK_ATTACHMENT:
            addAttachment(data.getData());
            mDraftNeedsSaving = true;
            break;
        case CONTACT_PICKER_TO:
        case CONTACT_PICKER_CC:
        case CONTACT_PICKER_BCC:
            ContactItem contact = mContacts.extractInfoFromContactPickerIntent(data);
            if (contact == null) {
                Toast.makeText(this, getString(R.string.error_contact_address_not_found), Toast.LENGTH_LONG).show();
                return;
            }
            if (contact.emailAddresses.size() > 1) {
                Intent i = new Intent(this, EmailAddressList.class);
                i.putExtra(EmailAddressList.EXTRA_CONTACT_ITEM, contact);

                if (requestCode == CONTACT_PICKER_TO) {
                    startActivityForResult(i, CONTACT_PICKER_TO2);
                } else if (requestCode == CONTACT_PICKER_CC) {
                    startActivityForResult(i, CONTACT_PICKER_CC2);
                } else if (requestCode == CONTACT_PICKER_BCC) {
                    startActivityForResult(i, CONTACT_PICKER_BCC2);
                }
                return;
            }
            if (K9.DEBUG) {
                List<String> emails = contact.emailAddresses;
                for (int i = 0; i < emails.size(); i++) {
                    Log.v(K9.LOG_TAG, "email[" + i + "]: " + emails.get(i));
                }
            }


            String email = contact.emailAddresses.get(0);
            if (requestCode == CONTACT_PICKER_TO) {
                addAddress(mToView, new Address(email, ""));
            } else if (requestCode == CONTACT_PICKER_CC) {
                addAddress(mCcView, new Address(email, ""));
            } else if (requestCode == CONTACT_PICKER_BCC) {
                addAddress(mBccView, new Address(email, ""));
            } else {
                return;
            }



            break;
        case CONTACT_PICKER_TO2:
        case CONTACT_PICKER_CC2:
        case CONTACT_PICKER_BCC2:
            String emailAddr = data.getStringExtra(EmailAddressList.EXTRA_EMAIL_ADDRESS);
            if (requestCode == CONTACT_PICKER_TO2) {
                addAddress(mToView, new Address(emailAddr, ""));
            } else if (requestCode == CONTACT_PICKER_CC2) {
                addAddress(mCcView, new Address(emailAddr, ""));
            } else if (requestCode == CONTACT_PICKER_BCC2) {
                addAddress(mBccView, new Address(emailAddr, ""));
            }
            break;
        }
    }

    public void doLaunchContactPicker(int resultId) {
        mIgnoreOnPause = true;
        startActivityForResult(mContacts.contactPickerIntent(), resultId);
    }

    private void onAccountChosen(Account account, Identity identity) {
        if (!mAccount.equals(account)) {
            if (K9.DEBUG) {
                Log.v(K9.LOG_TAG, "Switching account from " + mAccount + " to " + account);
            }

            // on draft edit, make sure we don't keep previous message UID
            if (mAction == Action.EDIT_DRAFT) {
                mMessageReference = null;
            }

            // test whether there is something to save
            if (mDraftNeedsSaving || (mDraftId != INVALID_DRAFT_ID)) {
                final long previousDraftId = mDraftId;
                final Account previousAccount = mAccount;

                // make current message appear as new
                mDraftId = INVALID_DRAFT_ID;

                // actual account switch
                mAccount = account;

                if (K9.DEBUG) {
                    Log.v(K9.LOG_TAG, "Account switch, saving new draft in new account");
                }
                saveMessage();

                if (previousDraftId != INVALID_DRAFT_ID) {
                    if (K9.DEBUG) {
                        Log.v(K9.LOG_TAG, "Account switch, deleting draft from previous account: "
                              + previousDraftId);
                    }
                    MessagingController.getInstance(getApplication()).deleteDraft(previousAccount,
                            previousDraftId);
                }
            } else {
                mAccount = account;
            }

            // Show CC/BCC text input field when switching to an account that always wants them
            // displayed.
            // Please note that we're not hiding the fields if the user switches back to an account
            // that doesn't have this setting checked.
            if (mAccount.isAlwaysShowCcBcc()) {
                onAddCcBcc();
            }

            // not sure how to handle mFolder, mSourceMessage?
        }

        switchToIdentity(identity);
    }

    private void switchToIdentity(Identity identity) {
        mIdentity = identity;
        mIdentityChanged = true;
        mDraftNeedsSaving = true;
        updateFrom();
        updateBcc();
        updateSignature();
        updateMessageFormat();
    }

    private void updateFrom() {
        mChooseIdentityButton.setText(mIdentity.getEmail());
    }

    private void updateBcc() {
        if (mIdentityChanged) {
            mBccWrapper.setVisibility(View.VISIBLE);
        }
        mBccView.setText("");
        addAddresses(mBccView, mAccount.getAlwaysBcc());
    }

    private void updateSignature() {
        if (mIdentity.getSignatureUse()) {
            mSignatureView.setText(mIdentity.getSignature());
            mSignatureView.setVisibility(View.VISIBLE);
        } else {
            mSignatureView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.attachment_delete:
            /*
             * The view is the delete button, and we have previously set the tag of
             * the delete button to the view that owns it. We don't use parent because the
             * view is very complex and could change in the future.
             */
            mAttachments.removeView((View) view.getTag());
            mDraftNeedsSaving = true;
            break;
        case R.id.quoted_text_show:
            showOrHideQuotedText(QuotedTextMode.SHOW);
            updateMessageFormat();
            mDraftNeedsSaving = true;
            break;
        case R.id.quoted_text_delete:
            showOrHideQuotedText(QuotedTextMode.HIDE);
            updateMessageFormat();
            mDraftNeedsSaving = true;
            break;
        case R.id.quoted_text_edit:
            mForcePlainText = true;
            if (mMessageReference != null) { // shouldn't happen...
                // TODO - Should we check if mSourceMessageBody is already present and bypass the MessagingController call?
                MessagingController.getInstance(getApplication()).addListener(mListener);
                final Account account = Preferences.getPreferences(this).getAccount(mMessageReference.accountUuid);
                final String folderName = mMessageReference.folderName;
                final String sourceMessageUid = mMessageReference.uid;
                MessagingController.getInstance(getApplication()).loadMessageForView(account, folderName, sourceMessageUid, null);
            }
            break;
        case R.id.identity:
            showDialog(DIALOG_CHOOSE_IDENTITY);
            break;
        }
    }

    /**
     * Show or hide the quoted text.
     *
     * @param mode
     *         The value to set {@link #mQuotedTextMode} to.
     */
    private void showOrHideQuotedText(QuotedTextMode mode) {
        mQuotedTextMode = mode;
        switch (mode) {
            case NONE:
            case HIDE: {
                if (mode == QuotedTextMode.NONE) {
                    mQuotedTextShow.setVisibility(View.GONE);
                } else {
                    mQuotedTextShow.setVisibility(View.VISIBLE);
                }
                mQuotedTextBar.setVisibility(View.GONE);
                mQuotedText.setVisibility(View.GONE);
                mQuotedHTML.setVisibility(View.GONE);
                mQuotedTextEdit.setVisibility(View.GONE);
                break;
            }
            case SHOW: {
                mQuotedTextShow.setVisibility(View.GONE);
                mQuotedTextBar.setVisibility(View.VISIBLE);

                if (mQuotedTextFormat == SimpleMessageFormat.HTML) {
                    mQuotedText.setVisibility(View.GONE);
                    mQuotedHTML.setVisibility(View.VISIBLE);
                    mQuotedTextEdit.setVisibility(View.VISIBLE);
                } else {
                    mQuotedText.setVisibility(View.VISIBLE);
                    mQuotedHTML.setVisibility(View.GONE);
                    mQuotedTextEdit.setVisibility(View.GONE);
                }
                break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.send:
            mPgpData.setEncryptionKeys(null);
            onSend();
            break;
        case R.id.save:
            if (mEncryptCheckbox.isChecked()) {
                showDialog(DIALOG_REFUSE_TO_SAVE_DRAFT_MARKED_ENCRYPTED);
            } else {
                onSave();
            }
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
        case R.id.read_receipt:
            onReadReceipt();
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getSupportMenuInflater().inflate(R.menu.message_compose_option, menu);

        mMenu = menu;

        // Disable the 'Save' menu option if Drafts folder is set to -NONE-
        if (!mAccount.hasDraftsFolder()) {
            menu.findItem(R.id.save).setEnabled(false);
        }

        /*
         * Show the menu items "Add attachment (Image)" and "Add attachment (Video)"
         * if the work-around for the Gallery bug is enabled (see Issue 1186).
         */
        menu.findItem(R.id.add_attachment_image).setVisible(K9.useGalleryBugWorkaround());
        menu.findItem(R.id.add_attachment_video).setVisible(K9.useGalleryBugWorkaround());

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        computeAddCcBccVisibility();

        return true;
    }

    @Override
    public void onBackPressed() {
        if (mDraftNeedsSaving) {
            if (mEncryptCheckbox.isChecked()) {
                showDialog(DIALOG_REFUSE_TO_SAVE_DRAFT_MARKED_ENCRYPTED);
            } else if (!mAccount.hasDraftsFolder()) {
                showDialog(DIALOG_CONFIRM_DISCARD_ON_BACK);
            } else {
                showDialog(DIALOG_SAVE_OR_DISCARD_DRAFT_MESSAGE);
            }
        } else {
            // Check if editing an existing draft.
            if (mDraftId == INVALID_DRAFT_ID) {
                onDiscard();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_SAVE_OR_DISCARD_DRAFT_MESSAGE:
            return new AlertDialog.Builder(this)
                   .setTitle(R.string.save_or_discard_draft_message_dlg_title)
                   .setMessage(R.string.save_or_discard_draft_message_instructions_fmt)
            .setPositiveButton(R.string.save_draft_action, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    dismissDialog(DIALOG_SAVE_OR_DISCARD_DRAFT_MESSAGE);
                    onSave();
                }
            })
            .setNegativeButton(R.string.discard_action, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    dismissDialog(DIALOG_SAVE_OR_DISCARD_DRAFT_MESSAGE);
                    onDiscard();
                }
            })
                   .create();
        case DIALOG_REFUSE_TO_SAVE_DRAFT_MARKED_ENCRYPTED:
            return new AlertDialog.Builder(this)
                   .setTitle(R.string.refuse_to_save_draft_marked_encrypted_dlg_title)
                   .setMessage(R.string.refuse_to_save_draft_marked_encrypted_instructions_fmt)
            .setNeutralButton(R.string.okay_action, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    dismissDialog(DIALOG_REFUSE_TO_SAVE_DRAFT_MARKED_ENCRYPTED);
                }
            })
                   .create();
        case DIALOG_CONTINUE_WITHOUT_PUBLIC_KEY:
            return new AlertDialog.Builder(this)
                   .setTitle(R.string.continue_without_public_key_dlg_title)
                   .setMessage(R.string.continue_without_public_key_instructions_fmt)
            .setPositiveButton(R.string.continue_action, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    dismissDialog(DIALOG_CONTINUE_WITHOUT_PUBLIC_KEY);
                    mContinueWithoutPublicKey = true;
                    onSend();
                }
            })
            .setNegativeButton(R.string.back_action, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    dismissDialog(DIALOG_CONTINUE_WITHOUT_PUBLIC_KEY);
                    mContinueWithoutPublicKey = false;
                }
            })
                   .create();
        case DIALOG_CONFIRM_DISCARD_ON_BACK:
            return new AlertDialog.Builder(this)
                   .setTitle(R.string.confirm_discard_draft_message_title)
                   .setMessage(R.string.confirm_discard_draft_message)
            .setPositiveButton(R.string.cancel_action, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    dismissDialog(DIALOG_CONFIRM_DISCARD_ON_BACK);
                }
            })
            .setNegativeButton(R.string.discard_action, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    dismissDialog(DIALOG_CONFIRM_DISCARD_ON_BACK);
                    Toast.makeText(MessageCompose.this,
                                   getString(R.string.message_discarded_toast),
                                   Toast.LENGTH_LONG).show();
                    onDiscard();
                }
            })
            .create();
        case DIALOG_CHOOSE_IDENTITY:
            Context context = new ContextThemeWrapper(this,
                    (K9.getK9Theme() == K9.Theme.LIGHT) ?
                            R.style.Theme_K9_Dialog_Light :
                            R.style.Theme_K9_Dialog_Dark);
            Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.send_as);
            final IdentityAdapter adapter = new IdentityAdapter(context);
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    IdentityContainer container = (IdentityContainer) adapter.getItem(which);
                    onAccountChosen(container.account, container.identity);
                }
            });

            return builder.create();
        }
        return super.onCreateDialog(id);
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
    private boolean loadAttachments(Part part, int depth) throws MessagingException {
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

        String contentType = MimeUtility.unfoldAndDecode(part.getContentType());
        String name = MimeUtility.getHeaderParameter(contentType, "name");
        if (name != null) {
            Body body = part.getBody();
            if (body != null && body instanceof LocalAttachmentBody) {
                final Uri uri = ((LocalAttachmentBody) body).getContentUri();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        addAttachment(uri);
                    }
                });
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Pull out the parts of the now loaded source message and apply them to the new message
     * depending on the type of message being composed.
     *
     * @param message
     *         The source message used to populate the various text fields.
     */
    private void processSourceMessage(Message message) {
        try {
            switch (mAction) {
                case REPLY:
                case REPLY_ALL: {
                    processMessageToReplyTo(message);
                    break;
                }
                case FORWARD: {
                    processMessageToForward(message);
                    break;
                }
                case EDIT_DRAFT: {
                    processDraftMessage(message);
                    break;
                }
                default: {
                    Log.w(K9.LOG_TAG, "processSourceMessage() called with unsupported action");
                    break;
                }
            }
        } catch (MessagingException me) {
            /**
             * Let the user continue composing their message even if we have a problem processing
             * the source message. Log it as an error, though.
             */
            Log.e(K9.LOG_TAG, "Error while processing source message: ", me);
        } finally {
            mSourceMessageProcessed = true;
            mDraftNeedsSaving = false;
        }

        updateMessageFormat();
    }

    private void processMessageToReplyTo(Message message) throws MessagingException {
        if (message.getSubject() != null) {
            final String subject = PREFIX.matcher(message.getSubject()).replaceFirst("");

            if (!subject.toLowerCase(Locale.US).startsWith("re:")) {
                mSubjectView.setText("Re: " + subject);
            } else {
                mSubjectView.setText(subject);
            }
        } else {
            mSubjectView.setText("");
        }

        /*
         * If a reply-to was included with the message use that, otherwise use the from
         * or sender address.
         */
        Address[] replyToAddresses;
        if (message.getReplyTo().length > 0) {
            replyToAddresses = message.getReplyTo();
        } else {
            replyToAddresses = message.getFrom();
        }

        // if we're replying to a message we sent, we probably meant
        // to reply to the recipient of that message
        if (mAccount.isAnIdentity(replyToAddresses)) {
            replyToAddresses = message.getRecipients(RecipientType.TO);
        }

        addAddresses(mToView, replyToAddresses);



        if (message.getMessageId() != null && message.getMessageId().length() > 0) {
            mInReplyTo = message.getMessageId();

            if (message.getReferences() != null && message.getReferences().length > 0) {
                StringBuilder buffy = new StringBuilder();
                for (int i = 0; i < message.getReferences().length; i++)
                    buffy.append(message.getReferences()[i]);

                mReferences = buffy.toString() + " " + mInReplyTo;
            } else {
                mReferences = mInReplyTo;
            }

        } else {
            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "could not get Message-ID.");
        }

        // Quote the message and setup the UI.
        populateUIWithQuotedMessage(mAccount.isDefaultQuotedTextShown());

        if (mAction == Action.REPLY || mAction == Action.REPLY_ALL) {
            Identity useIdentity = null;
            for (Address address : message.getRecipients(RecipientType.TO)) {
                Identity identity = mAccount.findIdentity(address);
                if (identity != null) {
                    useIdentity = identity;
                    break;
                }
            }
            if (useIdentity == null) {
                if (message.getRecipients(RecipientType.CC).length > 0) {
                    for (Address address : message.getRecipients(RecipientType.CC)) {
                        Identity identity = mAccount.findIdentity(address);
                        if (identity != null) {
                            useIdentity = identity;
                            break;
                        }
                    }
                }
            }
            if (useIdentity != null) {
                Identity defaultIdentity = mAccount.getIdentity(0);
                if (useIdentity != defaultIdentity) {
                    switchToIdentity(useIdentity);
                }
            }
        }

        if (mAction == Action.REPLY_ALL) {
            if (message.getReplyTo().length > 0) {
                for (Address address : message.getFrom()) {
                    if (!mAccount.isAnIdentity(address)) {
                        addAddress(mToView, address);
                    }
                }
            }
            for (Address address : message.getRecipients(RecipientType.TO)) {
                if (!mAccount.isAnIdentity(address) && !Utility.arrayContains(replyToAddresses, address)) {
                    addAddress(mToView, address);
                }

            }
            if (message.getRecipients(RecipientType.CC).length > 0) {
                for (Address address : message.getRecipients(RecipientType.CC)) {
                    if (!mAccount.isAnIdentity(address) && !Utility.arrayContains(replyToAddresses, address)) {
                        addAddress(mCcView, address);
                    }

                }
                mCcWrapper.setVisibility(View.VISIBLE);
            }
        }
    }

    private void processMessageToForward(Message message) throws MessagingException {
        String subject = message.getSubject();
        if (subject != null && !subject.toLowerCase(Locale.US).startsWith("fwd:")) {
            mSubjectView.setText("Fwd: " + subject);
        } else {
            mSubjectView.setText(subject);
        }
        mQuoteStyle = QuoteStyle.HEADER;

        // "Be Like Thunderbird" - on forwarded messages, set the message ID
        // of the forwarded message in the references and the reply to.  TB
        // only includes ID of the message being forwarded in the reference,
        // even if there are multiple references.
        if (!StringUtils.isNullOrEmpty(message.getMessageId())) {
            mInReplyTo = message.getMessageId();
            mReferences = mInReplyTo;
        } else {
            if (K9.DEBUG) {
                Log.d(K9.LOG_TAG, "could not get Message-ID.");
            }
        }

        // Quote the message and setup the UI.
        populateUIWithQuotedMessage(true);

        if (!mSourceMessageProcessed) {
            if (!loadAttachments(message, 0)) {
                mHandler.sendEmptyMessage(MSG_SKIPPED_ATTACHMENTS);
            }
        }
    }

    private void processDraftMessage(Message message) throws MessagingException {
        String showQuotedTextMode = "NONE";

        mDraftId = MessagingController.getInstance(getApplication()).getId(message);
        mSubjectView.setText(message.getSubject());
        addAddresses(mToView, message.getRecipients(RecipientType.TO));
        if (message.getRecipients(RecipientType.CC).length > 0) {
            addAddresses(mCcView, message.getRecipients(RecipientType.CC));
            mCcWrapper.setVisibility(View.VISIBLE);
        }

        Address[] bccRecipients = message.getRecipients(RecipientType.BCC);
        if (bccRecipients.length > 0) {
            addAddresses(mBccView, bccRecipients);
            String bccAddress = mAccount.getAlwaysBcc();
            if (bccRecipients.length == 1 && bccAddress != null && bccAddress.equals(bccRecipients[0].toString())) {
                // If the auto-bcc is the only entry in the BCC list, don't show the Bcc fields.
                mBccWrapper.setVisibility(View.GONE);
            } else {
                mBccWrapper.setVisibility(View.VISIBLE);
            }
        }

        // Read In-Reply-To header from draft
        final String[] inReplyTo = message.getHeader("In-Reply-To");
        if ((inReplyTo != null) && (inReplyTo.length >= 1)) {
            mInReplyTo = inReplyTo[0];
        }

        // Read References header from draft
        final String[] references = message.getHeader("References");
        if ((references != null) && (references.length >= 1)) {
            mReferences = references[0];
        }

        if (!mSourceMessageProcessed) {
            loadAttachments(message, 0);
        }

        // Decode the identity header when loading a draft.
        // See buildIdentityHeader(TextBody) for a detailed description of the composition of this blob.
        Map<IdentityField, String> k9identity = new HashMap<IdentityField, String>();
        if (message.getHeader(K9.IDENTITY_HEADER) != null && message.getHeader(K9.IDENTITY_HEADER).length > 0 && message.getHeader(K9.IDENTITY_HEADER)[0] != null) {
            k9identity = parseIdentityHeader(message.getHeader(K9.IDENTITY_HEADER)[0]);
        }

        Identity newIdentity = new Identity();
        if (k9identity.containsKey(IdentityField.SIGNATURE)) {
            newIdentity.setSignatureUse(true);
            newIdentity.setSignature(k9identity.get(IdentityField.SIGNATURE));
            mSignatureChanged = true;
        } else {
            newIdentity.setSignatureUse(message.getFolder().getAccount().getSignatureUse());
            newIdentity.setSignature(mIdentity.getSignature());
        }

        if (k9identity.containsKey(IdentityField.NAME)) {
            newIdentity.setName(k9identity.get(IdentityField.NAME));
            mIdentityChanged = true;
        } else {
            newIdentity.setName(mIdentity.getName());
        }

        if (k9identity.containsKey(IdentityField.EMAIL)) {
            newIdentity.setEmail(k9identity.get(IdentityField.EMAIL));
            mIdentityChanged = true;
        } else {
            newIdentity.setEmail(mIdentity.getEmail());
        }

        if (k9identity.containsKey(IdentityField.ORIGINAL_MESSAGE)) {
            mMessageReference = null;
            try {
                String originalMessage = k9identity.get(IdentityField.ORIGINAL_MESSAGE);
                MessageReference messageReference = new MessageReference(originalMessage);

                // Check if this is a valid account in our database
                Preferences prefs = Preferences.getPreferences(getApplicationContext());
                Account account = prefs.getAccount(messageReference.accountUuid);
                if (account != null) {
                    mMessageReference = messageReference;
                }
            } catch (MessagingException e) {
                Log.e(K9.LOG_TAG, "Could not decode message reference in identity.", e);
            }
        }

        int cursorPosition = 0;
        if (k9identity.containsKey(IdentityField.CURSOR_POSITION)) {
            try {
                cursorPosition = Integer.valueOf(k9identity.get(IdentityField.CURSOR_POSITION)).intValue();
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Could not parse cursor position for MessageCompose; continuing.", e);
            }
        }

        if (k9identity.containsKey(IdentityField.QUOTED_TEXT_MODE)) {
            showQuotedTextMode = k9identity.get(IdentityField.QUOTED_TEXT_MODE);
        }

        mIdentity = newIdentity;

        updateSignature();
        updateFrom();

        Integer bodyLength = k9identity.get(IdentityField.LENGTH) != null
                             ? Integer.valueOf(k9identity.get(IdentityField.LENGTH))
                             : 0;
        Integer bodyOffset = k9identity.get(IdentityField.OFFSET) != null
                             ? Integer.valueOf(k9identity.get(IdentityField.OFFSET))
                             : 0;
        Integer bodyFooterOffset = k9identity.get(IdentityField.FOOTER_OFFSET) != null
                ? Integer.valueOf(k9identity.get(IdentityField.FOOTER_OFFSET))
                : null;
        Integer bodyPlainLength = k9identity.get(IdentityField.PLAIN_LENGTH) != null
                ? Integer.valueOf(k9identity.get(IdentityField.PLAIN_LENGTH))
                : null;
        Integer bodyPlainOffset = k9identity.get(IdentityField.PLAIN_OFFSET) != null
                ? Integer.valueOf(k9identity.get(IdentityField.PLAIN_OFFSET))
                : null;
        mQuoteStyle = k9identity.get(IdentityField.QUOTE_STYLE) != null
                ? QuoteStyle.valueOf(k9identity.get(IdentityField.QUOTE_STYLE))
                : mAccount.getQuoteStyle();


        QuotedTextMode quotedMode;
        try {
            quotedMode = QuotedTextMode.valueOf(showQuotedTextMode);
        } catch (Exception e) {
            quotedMode = QuotedTextMode.NONE;
        }

        // Always respect the user's current composition format preference, even if the
        // draft was saved in a different format.
        // TODO - The current implementation doesn't allow a user in HTML mode to edit a draft that wasn't saved with K9mail.
        String messageFormatString = k9identity.get(IdentityField.MESSAGE_FORMAT);

        MessageFormat messageFormat = null;
        if (messageFormatString != null) {
            try {
                messageFormat = MessageFormat.valueOf(messageFormatString);
            } catch (Exception e) { /* do nothing */ }
        }

        if (messageFormat == null) {
            // This message probably wasn't created by us. The exception is legacy
            // drafts created before the advent of HTML composition. In those cases,
            // we'll display the whole message (including the quoted part) in the
            // composition window. If that's the case, try and convert it to text to
            // match the behavior in text mode.
            mMessageContentView.setText(getBodyTextFromMessage(message, SimpleMessageFormat.TEXT));
            mForcePlainText = true;

            showOrHideQuotedText(quotedMode);
            return;
        }


        if (messageFormat == MessageFormat.HTML) {
            Part part = MimeUtility.findFirstPartByMimeType(message, "text/html");
            if (part != null) { // Shouldn't happen if we were the one who saved it.
                mQuotedTextFormat = SimpleMessageFormat.HTML;
                String text = MimeUtility.getTextFromPart(part);
                if (K9.DEBUG) {
                    Log.d(K9.LOG_TAG, "Loading message with offset " + bodyOffset + ", length " + bodyLength + ". Text length is " + text.length() + ".");
                }

                // Grab our reply text.
                String bodyText = text.substring(bodyOffset, bodyOffset + bodyLength);
                mMessageContentView.setText(HtmlConverter.htmlToText(bodyText));

                // Regenerate the quoted html without our user content in it.
                StringBuilder quotedHTML = new StringBuilder();
                quotedHTML.append(text.substring(0, bodyOffset));   // stuff before the reply
                quotedHTML.append(text.substring(bodyOffset + bodyLength));
                if (quotedHTML.length() > 0) {
                    mQuotedHtmlContent = new InsertableHtmlContent();
                    mQuotedHtmlContent.setQuotedContent(quotedHTML);
                    // We don't know if bodyOffset refers to the header or to the footer
                    mQuotedHtmlContent.setHeaderInsertionPoint(bodyOffset);
                    if (bodyFooterOffset != null) {
                        mQuotedHtmlContent.setFooterInsertionPoint(bodyFooterOffset);
                    } else {
                        mQuotedHtmlContent.setFooterInsertionPoint(bodyOffset);
                    }
                    mQuotedHTML.setText(mQuotedHtmlContent.getQuotedContent());
                }
            }
            if (bodyPlainOffset != null && bodyPlainLength != null) {
                processSourceMessageText(message, bodyPlainOffset, bodyPlainLength, false);
            }
        } else if (messageFormat == MessageFormat.TEXT) {
            mQuotedTextFormat = SimpleMessageFormat.TEXT;
            processSourceMessageText(message, bodyOffset, bodyLength, true);
        } else {
            Log.e(K9.LOG_TAG, "Unhandled message format.");
        }

        // Set the cursor position if we have it.
        try {
            mMessageContentView.setSelection(cursorPosition);
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Could not set cursor position in MessageCompose; ignoring.", e);
        }

        showOrHideQuotedText(quotedMode);
    }

    /*
     * Pull out the parts of the now loaded source message and apply them to the new message
     * depending on the type of message being composed.
     * @param message Source message
     * @param bodyOffset Insertion point for reply.
     * @param bodyLength Length of reply.
     * @param viewMessageContent Update mMessageContentView or not.
     * @throws MessagingException
     */
    private void processSourceMessageText(Message message, Integer bodyOffset, Integer bodyLength,
            boolean viewMessageContent) throws MessagingException {
        Part textPart = MimeUtility.findFirstPartByMimeType(message, "text/plain");
        if (textPart != null) {
            String text = MimeUtility.getTextFromPart(textPart);
            if (K9.DEBUG) {
                Log.d(K9.LOG_TAG, "Loading message with offset " + bodyOffset + ", length " + bodyLength + ". Text length is " + text.length() + ".");
            }

            // If we had a body length (and it was valid), separate the composition from the quoted text
            // and put them in their respective places in the UI.
            if (bodyLength != null && bodyLength + 1 < text.length()) { // + 1 to get rid of the newline we added when saving the draft
                String bodyText = text.substring(bodyOffset, bodyOffset + bodyLength);

                // Regenerate the quoted text without our user content in it nor added newlines.
                StringBuilder quotedText = new StringBuilder();
                if (bodyOffset == 0 && text.substring(bodyLength, bodyLength + 2).equals("\n\n")) {
                    // top-posting: ignore two newlines at start of quote
                    quotedText.append(text.substring(bodyLength + 2));
                } else if (bodyOffset + bodyLength == text.length() &&
                        text.substring(bodyOffset - 1, bodyOffset).equals("\n")) {
                    // bottom-posting: ignore newline at end of quote
                    quotedText.append(text.substring(0, bodyOffset - 1));
                } else {
                    quotedText.append(text.substring(0, bodyOffset));   // stuff before the reply
                    quotedText.append(text.substring(bodyOffset + bodyLength));
                }

                if (viewMessageContent) mMessageContentView.setText(bodyText);
                mQuotedText.setText(quotedText.toString());
            } else {
                if (viewMessageContent) mMessageContentView.setText(text);
            }
        }
    }

    // Regexes to check for signature.
    private static final Pattern DASH_SIGNATURE_PLAIN = Pattern.compile("\r\n-- \r\n.*", Pattern.DOTALL);
    private static final Pattern DASH_SIGNATURE_HTML = Pattern.compile("(<br( /)?>|\r?\n)-- <br( /)?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern BLOCKQUOTE_START = Pattern.compile("<blockquote", Pattern.CASE_INSENSITIVE);
    private static final Pattern BLOCKQUOTE_END = Pattern.compile("</blockquote>", Pattern.CASE_INSENSITIVE);

    /**
     * Build and populate the UI with the quoted message.
     *
     * @param showQuotedText
     *         {@code true} if the quoted text should be shown, {@code false} otherwise.
     *
     * @throws MessagingException
     */
    private void populateUIWithQuotedMessage(boolean showQuotedText) throws MessagingException {
        MessageFormat origMessageFormat = mAccount.getMessageFormat();
        if (mForcePlainText || origMessageFormat == MessageFormat.TEXT) {
            // Use plain text for the quoted message
            mQuotedTextFormat = SimpleMessageFormat.TEXT;
        } else if (origMessageFormat == MessageFormat.AUTO) {
            // Figure out which message format to use for the quoted text by looking if the source
            // message contains a text/html part. If it does, we use that.
            mQuotedTextFormat =
                    (MimeUtility.findFirstPartByMimeType(mSourceMessage, "text/html") == null) ?
                            SimpleMessageFormat.TEXT : SimpleMessageFormat.HTML;
        } else {
            mQuotedTextFormat = SimpleMessageFormat.HTML;
        }

        // TODO -- I am assuming that mSourceMessageBody will always be a text part.  Is this a safe assumption?

        // Handle the original message in the reply
        // If we already have mSourceMessageBody, use that.  It's pre-populated if we've got crypto going on.
        String content = (mSourceMessageBody != null) ?
                mSourceMessageBody :
                getBodyTextFromMessage(mSourceMessage, mQuotedTextFormat);

        if (mQuotedTextFormat == SimpleMessageFormat.HTML) {
            // Strip signature.
            // closing tags such as </div>, </span>, </table>, </pre> will be cut off.
            if (mAccount.isStripSignature() &&
                    (mAction == Action.REPLY || mAction == Action.REPLY_ALL)) {
                Matcher dashSignatureHtml = DASH_SIGNATURE_HTML.matcher(content);
                if (dashSignatureHtml.find()) {
                    Matcher blockquoteStart = BLOCKQUOTE_START.matcher(content);
                    Matcher blockquoteEnd = BLOCKQUOTE_END.matcher(content);
                    List<Integer> start = new ArrayList<Integer>();
                    List<Integer> end = new ArrayList<Integer>();

                    while(blockquoteStart.find()) {
                        start.add(blockquoteStart.start());
                    }
                    while(blockquoteEnd.find()) {
                        end.add(blockquoteEnd.start());
                    }
                    if (start.size() != end.size()) {
                        Log.d(K9.LOG_TAG, "There are " + start.size() + " <blockquote> tags, but " +
                                end.size() + " </blockquote> tags. Refusing to strip.");
                    } else if (start.size() > 0) {
                        // Ignore quoted signatures in blockquotes.
                        dashSignatureHtml.region(0, start.get(0));
                        if (dashSignatureHtml.find()) {
                            // before first <blockquote>.
                            content = content.substring(0, dashSignatureHtml.start());
                        } else {
                            for (int i = 0; i < start.size() - 1; i++) {
                                // within blockquotes.
                                if (end.get(i) < start.get(i+1)) {
                                    dashSignatureHtml.region(end.get(i), start.get(i+1));
                                    if (dashSignatureHtml.find()) {
                                        content = content.substring(0, dashSignatureHtml.start());
                                        break;
                                    }
                                }
                            }
                            if (end.get(end.size() - 1) < content.length()) {
                                // after last </blockquote>.
                                dashSignatureHtml.region(end.get(end.size() - 1), content.length());
                                if (dashSignatureHtml.find()) {
                                    content = content.substring(0, dashSignatureHtml.start());
                                }
                            }
                        }
                    } else {
                        // No blockquotes found.
                        content = content.substring(0, dashSignatureHtml.start());
                    }
                }

                // Fix the stripping off of closing tags if a signature was stripped,
                // as well as clean up the HTML of the quoted message.
                HtmlCleaner cleaner = new HtmlCleaner();
                CleanerProperties properties = cleaner.getProperties();

                // see http://htmlcleaner.sourceforge.net/parameters.php for descriptions
                properties.setNamespacesAware(false);
                properties.setAdvancedXmlEscape(false);
                properties.setOmitXmlDeclaration(true);
                properties.setOmitDoctypeDeclaration(false);
                properties.setTranslateSpecialEntities(false);
                properties.setRecognizeUnicodeChars(false);

                TagNode node = cleaner.clean(content);
                SimpleHtmlSerializer htmlSerialized = new SimpleHtmlSerializer(properties);
                try {
                    content = htmlSerialized.getAsString(node, "UTF8");
                } catch (java.io.IOException ioe) {
                    // Can't imagine this happening.
                    Log.e(K9.LOG_TAG, "Problem cleaning quoted message.", ioe);
                }
            }

            // Add the HTML reply header to the top of the content.
            mQuotedHtmlContent = quoteOriginalHtmlMessage(mSourceMessage, content, mQuoteStyle);

            // Load the message with the reply header.
            mQuotedHTML.setText(mQuotedHtmlContent.getQuotedContent());

            // TODO: Also strip the signature from the text/plain part
            mQuotedText.setText(quoteOriginalTextMessage(mSourceMessage,
                    getBodyTextFromMessage(mSourceMessage, SimpleMessageFormat.TEXT), mQuoteStyle));

        } else if (mQuotedTextFormat == SimpleMessageFormat.TEXT) {
            if (mAccount.isStripSignature() &&
                    (mAction == Action.REPLY || mAction == Action.REPLY_ALL)) {
                if (DASH_SIGNATURE_PLAIN.matcher(content).find()) {
                    content = DASH_SIGNATURE_PLAIN.matcher(content).replaceFirst("\r\n");
                }
            }

            mQuotedText.setText(quoteOriginalTextMessage(mSourceMessage, content, mQuoteStyle));
        }

        if (showQuotedText) {
            showOrHideQuotedText(QuotedTextMode.SHOW);
        } else {
            showOrHideQuotedText(QuotedTextMode.HIDE);
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
    private String getBodyTextFromMessage(final Message message, final SimpleMessageFormat format)
            throws MessagingException {
        Part part;
        if (format == SimpleMessageFormat.HTML) {
            // HTML takes precedence, then text.
            part = MimeUtility.findFirstPartByMimeType(message, "text/html");
            if (part != null) {
                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "getBodyTextFromMessage: HTML requested, HTML found.");
                return MimeUtility.getTextFromPart(part);
            }

            part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
            if (part != null) {
                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "getBodyTextFromMessage: HTML requested, text found.");
                return HtmlConverter.textToHtml(MimeUtility.getTextFromPart(part));
            }
        } else if (format == SimpleMessageFormat.TEXT) {
            // Text takes precedence, then html.
            part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
            if (part != null) {
                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "getBodyTextFromMessage: Text requested, text found.");
                return MimeUtility.getTextFromPart(part);
            }

            part = MimeUtility.findFirstPartByMimeType(message, "text/html");
            if (part != null) {
                if (K9.DEBUG)
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
    private InsertableHtmlContent findInsertionPoints(final String content) {
        InsertableHtmlContent insertable = new InsertableHtmlContent();

        // If there is no content, don't bother doing any of the regex dancing.
        if (content == null || content.equals("")) {
            return insertable;
        }

        // Search for opening tags.
        boolean hasHtmlTag = false;
        boolean hasHeadTag = false;
        boolean hasBodyTag = false;
        // First see if we have an opening HTML tag.  If we don't find one, we'll add one later.
        Matcher htmlMatcher = FIND_INSERTION_POINT_HTML.matcher(content);
        if (htmlMatcher.matches()) {
            hasHtmlTag = true;
        }
        // Look for a HEAD tag.  If we're missing a BODY tag, we'll use the close of the HEAD to start our content.
        Matcher headMatcher = FIND_INSERTION_POINT_HEAD.matcher(content);
        if (headMatcher.matches()) {
            hasHeadTag = true;
        }
        // Look for a BODY tag.  This is the ideal place for us to start our content.
        Matcher bodyMatcher = FIND_INSERTION_POINT_BODY.matcher(content);
        if (bodyMatcher.matches()) {
            hasBodyTag = true;
        }

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "Open: hasHtmlTag:" + hasHtmlTag + " hasHeadTag:" + hasHeadTag + " hasBodyTag:" + hasBodyTag);

        // Given our inspections, let's figure out where to start our content.
        // This is the ideal case -- there's a BODY tag and we insert ourselves just after it.
        if (hasBodyTag) {
            insertable.setQuotedContent(new StringBuilder(content));
            insertable.setHeaderInsertionPoint(bodyMatcher.end(FIND_INSERTION_POINT_FIRST_GROUP));
        } else if (hasHeadTag) {
            // Now search for a HEAD tag.  We can insert after there.

            // If BlackBerry sees a HEAD tag, it inserts right after that, so long as there is no BODY tag. It doesn't
            // try to add BODY, either.  Right or wrong, it seems to work fine.
            insertable.setQuotedContent(new StringBuilder(content));
            insertable.setHeaderInsertionPoint(headMatcher.end(FIND_INSERTION_POINT_FIRST_GROUP));
        } else if (hasHtmlTag) {
            // Lastly, check for an HTML tag.
            // In this case, it will add a HEAD, but no BODY.
            StringBuilder newContent = new StringBuilder(content);
            // Insert the HEAD content just after the HTML tag.
            newContent.insert(htmlMatcher.end(FIND_INSERTION_POINT_FIRST_GROUP), FIND_INSERTION_POINT_HEAD_CONTENT);
            insertable.setQuotedContent(newContent);
            // The new insertion point is the end of the HTML tag, plus the length of the HEAD content.
            insertable.setHeaderInsertionPoint(htmlMatcher.end(FIND_INSERTION_POINT_FIRST_GROUP) + FIND_INSERTION_POINT_HEAD_CONTENT.length());
        } else {
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
        if (htmlEndMatcher.matches()) {
            hasHtmlEndTag = true;
        }
        // Look for a BODY tag.  This is the ideal place for us to place our footer.
        Matcher bodyEndMatcher = FIND_INSERTION_POINT_BODY_END.matcher(insertable.getQuotedContent());
        if (bodyEndMatcher.matches()) {
            hasBodyEndTag = true;
        }

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "Close: hasHtmlEndTag:" + hasHtmlEndTag + " hasBodyEndTag:" + hasBodyEndTag);

        // Now figure out where to put our footer.
        // This is the ideal case -- there's a BODY tag and we insert ourselves just before it.
        if (hasBodyEndTag) {
            insertable.setFooterInsertionPoint(bodyEndMatcher.start(FIND_INSERTION_POINT_FIRST_GROUP));
        } else if (hasHtmlEndTag) {
            // Check for an HTML tag.  Add ourselves just before it.
            insertable.setFooterInsertionPoint(htmlEndMatcher.start(FIND_INSERTION_POINT_FIRST_GROUP));
        } else {
            // If we have none of the above, we probably have a fragment of HTML.
            // Set our footer insertion point as the end of the string.
            insertable.setFooterInsertionPoint(insertable.getQuotedContent().length());
        }

        return insertable;
    }

    class Listener extends MessagingListener {
        @Override
        public void loadMessageForViewStarted(Account account, String folder, String uid) {
            if ((mMessageReference == null) || !mMessageReference.uid.equals(uid)) {
                return;
            }

            mHandler.sendEmptyMessage(MSG_PROGRESS_ON);
        }

        @Override
        public void loadMessageForViewFinished(Account account, String folder, String uid, Message message) {
            if ((mMessageReference == null) || !mMessageReference.uid.equals(uid)) {
                return;
            }

            mHandler.sendEmptyMessage(MSG_PROGRESS_OFF);
        }

        @Override
        public void loadMessageForViewBodyAvailable(Account account, String folder, String uid, final Message message) {
            if ((mMessageReference == null) || !mMessageReference.uid.equals(uid)) {
                return;
            }

            mSourceMessage = message;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // We check to see if we've previously processed the source message since this
                    // could be called when switching from HTML to text replies. If that happens, we
                    // only want to update the UI with quoted text (which picks the appropriate
                    // part).
                    if (mSourceProcessed) {
                        try {
                            populateUIWithQuotedMessage(true);
                        } catch (MessagingException e) {
                            // Hm, if we couldn't populate the UI after source reprocessing, let's just delete it?
                            showOrHideQuotedText(QuotedTextMode.HIDE);
                            Log.e(K9.LOG_TAG, "Could not re-process source message; deleting quoted text to be safe.", e);
                        }
                        updateMessageFormat();
                    } else {
                        processSourceMessage(message);
                        mSourceProcessed = true;
                    }
                }
            });
        }

        @Override
        public void loadMessageForViewFailed(Account account, String folder, String uid, Throwable t) {
            if ((mMessageReference == null) || !mMessageReference.uid.equals(uid)) {
                return;
            }
            mHandler.sendEmptyMessage(MSG_PROGRESS_OFF);
            // TODO show network error
        }

        @Override
        public void messageUidChanged(Account account, String folder, String oldUid, String newUid) {
            // Track UID changes of the source message
            if (mMessageReference != null) {
                final Account sourceAccount = Preferences.getPreferences(MessageCompose.this).getAccount(mMessageReference.accountUuid);
                final String sourceFolder = mMessageReference.folderName;
                final String sourceMessageUid = mMessageReference.uid;

                if (account.equals(sourceAccount) && (folder.equals(sourceFolder))) {
                    if (oldUid.equals(sourceMessageUid)) {
                        mMessageReference.uid = newUid;
                    }
                    if ((mSourceMessage != null) && (oldUid.equals(mSourceMessage.getUid()))) {
                        mSourceMessage.setUid(newUid);
                    }
                }
            }
        }
    }

    /**
     * When we are launched with an intent that includes a mailto: URI, we can actually
     * gather quite a few of our message fields from it.
     *
     * @param mailtoUri
     *         The mailto: URI we use to initialize the message fields.
     */
    private void initializeFromMailto(Uri mailtoUri) {
        String schemaSpecific = mailtoUri.getSchemeSpecificPart();
        int end = schemaSpecific.indexOf('?');
        if (end == -1) {
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
        CaseInsensitiveParamWrapper uri = new CaseInsensitiveParamWrapper(
                Uri.parse("foo://bar?" + mailtoUri.getEncodedQuery()));

        // Read additional recipients from the "to" parameter.
        List<String> to = uri.getQueryParameters("to");
        if (recipient.length() != 0) {
            to = new ArrayList<String>(to);
            to.add(0, recipient);
        }
        addRecipients(mToView, to);

        // Read carbon copy recipients from the "cc" parameter.
        boolean ccOrBcc = addRecipients(mCcView, uri.getQueryParameters("cc"));

        // Read blind carbon copy recipients from the "bcc" parameter.
        ccOrBcc |= addRecipients(mBccView, uri.getQueryParameters("bcc"));

        if (ccOrBcc) {
            // Display CC and BCC text fields if CC or BCC recipients were set by the intent.
            onAddCcBcc();
        }

        // Read subject from the "subject" parameter.
        List<String> subject = uri.getQueryParameters("subject");
        if (!subject.isEmpty()) {
            mSubjectView.setText(subject.get(0));
        }

        // Read message body from the "body" parameter.
        List<String> body = uri.getQueryParameters("body");
        if (!body.isEmpty()) {
            mMessageContentView.setText(body.get(0));
        }
    }

    private static class CaseInsensitiveParamWrapper {
        private final Uri uri;
        private Set<String> mParamNames;

        public CaseInsensitiveParamWrapper(Uri uri) {
            this.uri = uri;
        }

        public List<String> getQueryParameters(String key) {
            final List<String> params = new ArrayList<String>();
            for (String paramName : getQueryParameterNames()) {
                if (paramName.equalsIgnoreCase(key)) {
                    params.addAll(uri.getQueryParameters(paramName));
                }
            }
            return params;
        }

        @TargetApi(11)
        private Set<String> getQueryParameterNames() {
            if (Build.VERSION.SDK_INT >= 11) {
                return uri.getQueryParameterNames();
            }

            return getQueryParameterNamesPreSdk11();
        }

        private Set<String> getQueryParameterNamesPreSdk11() {
            if (mParamNames == null) {
                String query = uri.getQuery();
                Set<String> paramNames = new HashSet<String>();
                Collections.addAll(paramNames, query.split("(=[^&]*(&|$))|&"));
                mParamNames = paramNames;
            }

            return mParamNames;
        }
    }

    private class SendMessageTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            /*
             * Create the message from all the data the user has entered.
             */
            MimeMessage message;
            try {
                message = createMessage(false);  // isDraft = true
            } catch (MessagingException me) {
                Log.e(K9.LOG_TAG, "Failed to create new message for send or save.", me);
                throw new RuntimeException("Failed to create a new message for send or save.", me);
            }

            try {
                mContacts.markAsContacted(message.getRecipients(RecipientType.TO));
                mContacts.markAsContacted(message.getRecipients(RecipientType.CC));
                mContacts.markAsContacted(message.getRecipients(RecipientType.BCC));
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Failed to mark contact as contacted.", e);
            }

            MessagingController.getInstance(getApplication()).sendMessage(mAccount, message, null);
            long draftId = mDraftId;
            if (draftId != INVALID_DRAFT_ID) {
                mDraftId = INVALID_DRAFT_ID;
                MessagingController.getInstance(getApplication()).deleteDraft(mAccount, draftId);
            }

            return null;
        }
    }

    private class SaveMessageTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            /*
             * Create the message from all the data the user has entered.
             */
            MimeMessage message;
            try {
                message = createMessage(true);  // isDraft = true
            } catch (MessagingException me) {
                Log.e(K9.LOG_TAG, "Failed to create new message for send or save.", me);
                throw new RuntimeException("Failed to create a new message for send or save.", me);
            }

            /*
             * Save a draft
             */
            if (mAction == Action.EDIT_DRAFT) {
                /*
                 * We're saving a previously saved draft, so update the new message's uid
                 * to the old message's uid.
                 */
                if (mMessageReference != null) {
                    message.setUid(mMessageReference.uid);
                }
            }

            final MessagingController messagingController = MessagingController.getInstance(getApplication());
            Message draftMessage = messagingController.saveDraft(mAccount, message, mDraftId);
            mDraftId = messagingController.getId(draftMessage);

            mHandler.sendEmptyMessage(MSG_SAVED_DRAFT);
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
    private String quoteOriginalTextMessage(final Message originalMessage, final String messageBody, final QuoteStyle quoteStyle) throws MessagingException {
        String body = messageBody == null ? "" : messageBody;
        if (quoteStyle == QuoteStyle.PREFIX) {
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
        } else if (quoteStyle == QuoteStyle.HEADER) {
            StringBuilder quotedText = new StringBuilder(body.length() + QUOTE_BUFFER_LENGTH);
            quotedText.append("\n");
            quotedText.append(getString(R.string.message_compose_quote_header_separator)).append("\n");
            if (originalMessage.getFrom() != null && Address.toString(originalMessage.getFrom()).length() != 0) {
                quotedText.append(getString(R.string.message_compose_quote_header_from)).append(" ").append(Address.toString(originalMessage.getFrom())).append("\n");
            }
            if (originalMessage.getSentDate() != null) {
                quotedText.append(getString(R.string.message_compose_quote_header_send_date)).append(" ").append(originalMessage.getSentDate()).append("\n");
            }
            if (originalMessage.getRecipients(RecipientType.TO) != null && originalMessage.getRecipients(RecipientType.TO).length != 0) {
                quotedText.append(getString(R.string.message_compose_quote_header_to)).append(" ").append(Address.toString(originalMessage.getRecipients(RecipientType.TO))).append("\n");
            }
            if (originalMessage.getRecipients(RecipientType.CC) != null && originalMessage.getRecipients(RecipientType.CC).length != 0) {
                quotedText.append(getString(R.string.message_compose_quote_header_cc)).append(" ").append(Address.toString(originalMessage.getRecipients(RecipientType.CC))).append("\n");
            }
            if (originalMessage.getSubject() != null) {
                quotedText.append(getString(R.string.message_compose_quote_header_subject)).append(" ").append(originalMessage.getSubject()).append("\n");
            }
            quotedText.append("\n");

            quotedText.append(body);

            return quotedText.toString();
        } else {
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
    private InsertableHtmlContent quoteOriginalHtmlMessage(final Message originalMessage, final String messageBody, final QuoteStyle quoteStyle) throws MessagingException {
        InsertableHtmlContent insertable = findInsertionPoints(messageBody);

        if (quoteStyle == QuoteStyle.PREFIX) {
            StringBuilder header = new StringBuilder(QUOTE_BUFFER_LENGTH);
            header.append("<div class=\"gmail_quote\">");
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
        } else if (quoteStyle == QuoteStyle.HEADER) {

            StringBuilder header = new StringBuilder();
            header.append("<div style='font-size:10.0pt;font-family:\"Tahoma\",\"sans-serif\";padding:3.0pt 0in 0in 0in'>\n");
            header.append("<hr style='border:none;border-top:solid #E1E1E1 1.0pt'>\n"); // This gets converted into a horizontal line during html to text conversion.
            if (mSourceMessage.getFrom() != null && Address.toString(mSourceMessage.getFrom()).length() != 0) {
                header.append("<b>").append(getString(R.string.message_compose_quote_header_from)).append("</b> ")
                    .append(HtmlConverter.textToHtmlFragment(Address.toString(mSourceMessage.getFrom())))
                    .append("<br>\n");
            }
            if (mSourceMessage.getSentDate() != null) {
                header.append("<b>").append(getString(R.string.message_compose_quote_header_send_date)).append("</b> ")
                    .append(mSourceMessage.getSentDate())
                    .append("<br>\n");
            }
            if (mSourceMessage.getRecipients(RecipientType.TO) != null && mSourceMessage.getRecipients(RecipientType.TO).length != 0) {
                header.append("<b>").append(getString(R.string.message_compose_quote_header_to)).append("</b> ")
                    .append(HtmlConverter.textToHtmlFragment(Address.toString(mSourceMessage.getRecipients(RecipientType.TO))))
                    .append("<br>\n");
            }
            if (mSourceMessage.getRecipients(RecipientType.CC) != null && mSourceMessage.getRecipients(RecipientType.CC).length != 0) {
                header.append("<b>").append(getString(R.string.message_compose_quote_header_cc)).append("</b> ")
                    .append(HtmlConverter.textToHtmlFragment(Address.toString(mSourceMessage.getRecipients(RecipientType.CC))))
                    .append("<br>\n");
            }
            if (mSourceMessage.getSubject() != null) {
                header.append("<b>").append(getString(R.string.message_compose_quote_header_subject)).append("</b> ")
                    .append(HtmlConverter.textToHtmlFragment(mSourceMessage.getSubject()))
                    .append("<br>\n");
            }
            header.append("</div>\n");
            header.append("<br>\n");

            insertable.insertIntoQuotedHeader(header.toString());
        }

        return insertable;
    }

    /**
     * Used to store an {@link Identity} instance together with the {@link Account} it belongs to.
     *
     * @see IdentityAdapter
     */
    static class IdentityContainer {
        public final Identity identity;
        public final Account account;

        IdentityContainer(Identity identity, Account account) {
            this.identity = identity;
            this.account = account;
        }
    }

    /**
     * Adapter for the <em>Choose identity</em> list view.
     *
     * <p>
     * Account names are displayed as section headers, identities as selectable list items.
     * </p>
     */
    static class IdentityAdapter extends BaseAdapter {
        private LayoutInflater mLayoutInflater;
        private List<Object> mItems;

        public IdentityAdapter(Context context) {
            mLayoutInflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);

            List<Object> items = new ArrayList<Object>();
            Preferences prefs = Preferences.getPreferences(context.getApplicationContext());
            Account[] accounts = prefs.getAvailableAccounts().toArray(EMPTY_ACCOUNT_ARRAY);
            for (Account account : accounts) {
                items.add(account);
                List<Identity> identities = account.getIdentities();
                for (Identity identity : identities) {
                    items.add(new IdentityContainer(identity, account));
                }
            }
            mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return (mItems.get(position) instanceof Account) ? 0 : 1;
        }

        @Override
        public boolean isEnabled(int position) {
            return (mItems.get(position) instanceof IdentityContainer);
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Object item = mItems.get(position);

            View view = null;
            if (item instanceof Account) {
                if (convertView != null && convertView.getTag() instanceof AccountHolder) {
                    view = convertView;
                } else {
                    view = mLayoutInflater.inflate(R.layout.choose_account_item, parent, false);
                    AccountHolder holder = new AccountHolder();
                    holder.name = (TextView) view.findViewById(R.id.name);
                    holder.chip = view.findViewById(R.id.chip);
                    view.setTag(holder);
                }

                Account account = (Account) item;
                AccountHolder holder = (AccountHolder) view.getTag();
                holder.name.setText(account.getDescription());
                holder.chip.setBackgroundColor(account.getChipColor());
            } else if (item instanceof IdentityContainer) {
                if (convertView != null && convertView.getTag() instanceof IdentityHolder) {
                    view = convertView;
                } else {
                    view = mLayoutInflater.inflate(R.layout.choose_identity_item, parent, false);
                    IdentityHolder holder = new IdentityHolder();
                    holder.name = (TextView) view.findViewById(R.id.name);
                    holder.description = (TextView) view.findViewById(R.id.description);
                    view.setTag(holder);
                }

                IdentityContainer identityContainer = (IdentityContainer) item;
                Identity identity = identityContainer.identity;
                IdentityHolder holder = (IdentityHolder) view.getTag();
                holder.name.setText(identity.getDescription());
                holder.description.setText(getIdentityDescription(identity));
            }

            return view;
        }

        static class AccountHolder {
            public TextView name;
            public View chip;
        }

        static class IdentityHolder {
            public TextView name;
            public TextView description;
        }
    }

    private static String getIdentityDescription(Identity identity) {
        return String.format("%s <%s>", identity.getName(), identity.getEmail());
    }

    private void setMessageFormat(SimpleMessageFormat format) {
        // This method will later be used to enable/disable the rich text editing mode.

        mMessageFormat = format;
    }

    private void updateMessageFormat() {
        MessageFormat origMessageFormat = mAccount.getMessageFormat();
        SimpleMessageFormat messageFormat;
        if (origMessageFormat == MessageFormat.TEXT) {
            // The user wants to send text/plain messages. We don't override that choice under
            // any circumstances.
            messageFormat = SimpleMessageFormat.TEXT;
        } else if (mForcePlainText && includeQuotedText()) {
            // Right now we send a text/plain-only message when the quoted text was edited, no
            // matter what the user selected for the message format.
            messageFormat = SimpleMessageFormat.TEXT;
        } else if (mEncryptCheckbox.isChecked() || mCryptoSignatureCheckbox.isChecked()) {
            // Right now we only support PGP inline which doesn't play well with HTML. So force
            // plain text in those cases.
            messageFormat = SimpleMessageFormat.TEXT;
        } else if (origMessageFormat == MessageFormat.AUTO) {
            if (mAction == Action.COMPOSE || mQuotedTextFormat == SimpleMessageFormat.TEXT ||
                    !includeQuotedText()) {
                // If the message format is set to "AUTO" we use text/plain whenever possible. That
                // is, when composing new messages and replying to or forwarding text/plain
                // messages.
                messageFormat = SimpleMessageFormat.TEXT;
            } else {
                messageFormat = SimpleMessageFormat.HTML;
            }
        } else {
            // In all other cases use HTML
            messageFormat = SimpleMessageFormat.HTML;
        }

        setMessageFormat(messageFormat);
    }

    private boolean includeQuotedText() {
        return (mQuotedTextMode == QuotedTextMode.SHOW);
    }
}
