package com.fsck.k9.activity;


import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.Loader;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.Account.MessageFormat;
import com.fsck.k9.Account.QuoteStyle;
import com.fsck.k9.FontSizes;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.compose.ComposeCryptoStatus;
import com.fsck.k9.activity.compose.ComposeCryptoStatus.SendErrorState;
import com.fsck.k9.activity.compose.CryptoSettingsDialog.OnCryptoModeChangedListener;
import com.fsck.k9.activity.compose.RecipientMvpView;
import com.fsck.k9.activity.compose.RecipientPresenter;
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoMode;
import com.fsck.k9.activity.loader.AttachmentContentLoader;
import com.fsck.k9.activity.loader.AttachmentInfoLoader;
import com.fsck.k9.activity.misc.Attachment;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.fragment.ProgressDialogFragment;
import com.fsck.k9.fragment.ProgressDialogFragment.CancelListener;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.HtmlConverter;
import com.fsck.k9.helper.IdentityHelper;
import com.fsck.k9.helper.MailTo;
import com.fsck.k9.helper.SimpleTextWatcher;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.LocalBodyPart;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.message.IdentityField;
import com.fsck.k9.message.IdentityHeaderParser;
import com.fsck.k9.message.InsertableHtmlContent;
import com.fsck.k9.message.MessageBuilder;
import com.fsck.k9.message.PgpMessageBuilder;
import com.fsck.k9.message.QuotedTextMode;
import com.fsck.k9.message.SimpleMessageBuilder;
import com.fsck.k9.message.SimpleMessageFormat;
import com.fsck.k9.provider.AttachmentProvider;
import com.fsck.k9.ui.EolConvertingEditText;
import com.fsck.k9.view.MessageWebView;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleHtmlSerializer;
import org.htmlcleaner.TagNode;
import org.openintents.openpgp.IOpenPgpService2;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.openintents.openpgp.util.OpenPgpServiceConnection.OnBound;


@SuppressWarnings("deprecation")
public class MessageCompose extends K9Activity implements OnClickListener,
        CancelListener, OnFocusChangeListener, OnCryptoModeChangedListener, MessageBuilder.Callback {

    private static final int DIALOG_SAVE_OR_DISCARD_DRAFT_MESSAGE = 1;
    private static final int DIALOG_CONFIRM_DISCARD_ON_BACK = 2;
    private static final int DIALOG_CHOOSE_IDENTITY = 3;
    private static final int DIALOG_CONFIRM_DISCARD = 4;

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
    private static final String STATE_IN_REPLY_TO = "com.fsck.k9.activity.MessageCompose.inReplyTo";
    private static final String STATE_REFERENCES = "com.fsck.k9.activity.MessageCompose.references";
    private static final String STATE_KEY_READ_RECEIPT = "com.fsck.k9.activity.MessageCompose.messageReadReceipt";
    private static final String STATE_KEY_DRAFT_NEEDS_SAVING = "com.fsck.k9.activity.MessageCompose.draftNeedsSaving";
    private static final String STATE_KEY_FORCE_PLAIN_TEXT =
            "com.fsck.k9.activity.MessageCompose.forcePlainText";
    private static final String STATE_KEY_QUOTED_TEXT_FORMAT =
            "com.fsck.k9.activity.MessageCompose.quotedTextFormat";
    private static final String STATE_KEY_NUM_ATTACHMENTS_LOADING = "numAttachmentsLoading";
    private static final String STATE_KEY_WAITING_FOR_ATTACHMENTS = "waitingForAttachments";
    private static final String STATE_ALREADY_NOTIFIED_USER_OF_EMPTY_SUBJECT = "alreadyNotifiedUserOfEmptySubject";

    private static final String LOADER_ARG_ATTACHMENT = "attachment";

    private static final String FRAGMENT_WAITING_FOR_ATTACHMENT = "waitingForAttachment";

    private static final int MSG_PROGRESS_ON = 1;
    private static final int MSG_PROGRESS_OFF = 2;
    private static final int MSG_SKIPPED_ATTACHMENTS = 3;
    private static final int MSG_SAVED_DRAFT = 4;
    private static final int MSG_DISCARDED_DRAFT = 5;
    private static final int MSG_PERFORM_STALLED_ACTION = 6;

    private static final int ACTIVITY_REQUEST_PICK_ATTACHMENT = 1;

    private static final int REQUEST_MASK_RECIPIENT_PRESENTER = (1<<8);
    private static final int REQUEST_MASK_MESSAGE_BUILDER = (2<<8);

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
    private int mMaxLoaderId = 0;

    private RecipientPresenter recipientPresenter;
    private MessageBuilder currentMessageBuilder;
    private boolean mFinishAfterDraftSaved;
    private boolean alreadyNotifiedUserOfEmptySubject = false;

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        switch(v.getId()) {
            case R.id.message_content:
            case R.id.subject:
                if (hasFocus) {
                    recipientPresenter.onNonRecipientFieldFocused();
                }
                break;
        }
    }

    @Override
    public void onCryptoModeChanged(CryptoMode cryptoMode) {
        recipientPresenter.onCryptoModeChanged(cryptoMode);
    }

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

    private TextView mChooseIdentityButton;
    private EditText mSubjectView;
    private EolConvertingEditText mSignatureView;
    private EolConvertingEditText mMessageContentView;
    private LinearLayout mAttachments;
    private Button mQuotedTextShow;
    private View mQuotedTextBar;
    private ImageButton mQuotedTextEdit;
    private EolConvertingEditText mQuotedText;
    private MessageWebView mQuotedHTML;
    private InsertableHtmlContent mQuotedHtmlContent;   // Container for HTML reply as it's being built.

    private String mReferences;
    private String mInReplyTo;

    private boolean mSourceProcessed = false;

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

    private boolean draftNeedsSaving = false;
    private boolean isInSubActivity = false;

    /**
     * The database ID of this message's draft. This is used when saving drafts so the message in
     * the database is updated instead of being created anew. This property is INVALID_DRAFT_ID
     * until the first save.
     */
    private long mDraftId = INVALID_DRAFT_ID;

    /**
     * Number of attachments currently being fetched.
     */
    private int mNumAttachmentsLoading = 0;

    private enum WaitingAction {
        NONE,
        SEND,
        SAVE
    }

    /**
     * Specifies what action to perform once attachments have been fetched.
     */
    private WaitingAction mWaitingForAttachments = WaitingAction.NONE;


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_PROGRESS_ON:
                    setProgressBarIndeterminateVisibility(true);
                    break;
                case MSG_PROGRESS_OFF:
                    setProgressBarIndeterminateVisibility(false);
                    break;
                case MSG_SKIPPED_ATTACHMENTS:
                    Toast.makeText(
                        MessageCompose.this,
                        getString(R.string.message_compose_attachments_skipped_toast),
                        Toast.LENGTH_LONG).show();
                    break;
                case MSG_SAVED_DRAFT:
                    mDraftId = (Long) msg.obj;
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
                case MSG_PERFORM_STALLED_ACTION:
                    performStalledAction();
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    private Listener mListener = new Listener();

    private FontSizes mFontSizes = K9.getFontSizes();


    /**
     * Compose a new message using the given account. If account is null the default account
     * will be used.
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
     * @param messageBody optional, for decrypted messages, null if it should be grabbed from the given message
     */
    public static Intent getActionReplyIntent(
            Context context,
            LocalMessage message,
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

    public static Intent getActionReplyIntent(Context context, MessageReference messageReference) {
        Intent intent = new Intent(context, MessageCompose.class);
        intent.setAction(ACTION_REPLY);
        intent.putExtra(EXTRA_MESSAGE_REFERENCE, messageReference);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }

    /**
     * Compose a new message as a reply to the given message. If replyAll is true the function
     * is reply all instead of simply reply.
     * @param messageBody optional, for decrypted messages, null if it should be grabbed from the given message
     */
    public static void actionReply(
        Context context,
        LocalMessage message,
        boolean replyAll,
        String messageBody) {
        context.startActivity(getActionReplyIntent(context, message, replyAll, messageBody));
    }

    /**
     * Compose a new message as a forward of the given message.
     * @param messageBody optional, for decrypted messages, null if it should be grabbed from the given message
     */
    public static void actionForward(
            Context context,
            LocalMessage message,
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
     */
    public static void actionEditDraft(Context context, MessageReference messageReference) {
        Intent i = new Intent(context, MessageCompose.class);
        i.putExtra(EXTRA_MESSAGE_REFERENCE, messageReference);
        i.setAction(ACTION_EDIT_DRAFT);
        context.startActivity(i);
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
            ContextThemeWrapper themeContext = new ContextThemeWrapper(this,
                    K9.getK9ThemeResourceId(K9.getK9ComposerTheme()));
            @SuppressLint("InflateParams") // this is the top level activity element, it has no root
            View v = LayoutInflater.from(themeContext).inflate(R.layout.message_compose, null);
            TypedValue outValue = new TypedValue();
            // background color needs to be forced
            themeContext.getTheme().resolveAttribute(R.attr.messageViewBackgroundColor, outValue, true);
            v.setBackgroundColor(outValue.data);
            setContentView(v);
        } else {
            setContentView(R.layout.message_compose);
        }

        final Intent intent = getIntent();

        mMessageReference = intent.getParcelableExtra(EXTRA_MESSAGE_REFERENCE);
        mSourceMessageBody = intent.getStringExtra(EXTRA_MESSAGE_BODY);

        if (K9.DEBUG && mSourceMessageBody != null) {
            Log.d(K9.LOG_TAG, "Composing message with explicitly specified message body.");
        }

        final String accountUuid = (mMessageReference != null) ?
                                   mMessageReference.getAccountUuid() :
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
            draftNeedsSaving = false;
            finish();
            return;
        }

        mContacts = Contacts.getInstance(MessageCompose.this);

        mChooseIdentityButton = (TextView) findViewById(R.id.identity);
        mChooseIdentityButton.setOnClickListener(this);

        RecipientMvpView recipientMvpView = new RecipientMvpView(this);
        recipientPresenter = new RecipientPresenter(this, recipientMvpView, mAccount);

        mSubjectView = (EditText) findViewById(R.id.subject);
        mSubjectView.getInputExtras(true).putBoolean("allowEmoji", true);

        EolConvertingEditText upperSignature = (EolConvertingEditText)findViewById(R.id.upper_signature);
        EolConvertingEditText lowerSignature = (EolConvertingEditText)findViewById(R.id.lower_signature);

        mMessageContentView = (EolConvertingEditText)findViewById(R.id.message_content);
        mMessageContentView.getInputExtras(true).putBoolean("allowEmoji", true);

        mAttachments = (LinearLayout)findViewById(R.id.attachments);
        mQuotedTextShow = (Button)findViewById(R.id.quoted_text_show);
        mQuotedTextBar = findViewById(R.id.quoted_text_bar);
        mQuotedTextEdit = (ImageButton)findViewById(R.id.quoted_text_edit);
        ImageButton mQuotedTextDelete = (ImageButton) findViewById(R.id.quoted_text_delete);
        mQuotedText = (EolConvertingEditText)findViewById(R.id.quoted_text);
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

        TextWatcher draftNeedsChangingTextWatcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                draftNeedsSaving = true;
            }
        };

        TextWatcher signTextWatcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                draftNeedsSaving = true;
                mSignatureChanged = true;
            }
        };

        recipientMvpView.addTextChangedListener(draftNeedsChangingTextWatcher);

        mSubjectView.addTextChangedListener(draftNeedsChangingTextWatcher);

        mMessageContentView.addTextChangedListener(draftNeedsChangingTextWatcher);
        mQuotedText.addTextChangedListener(draftNeedsChangingTextWatcher);

        /*
         * We set this to invisible by default. Other methods will turn it back on if it's
         * needed.
         */

        showOrHideQuotedText(QuotedTextMode.NONE);

        mSubjectView.setOnFocusChangeListener(this);
        mMessageContentView.setOnFocusChangeListener(this);

        mQuotedTextShow.setOnClickListener(this);
        mQuotedTextEdit.setOnClickListener(this);
        mQuotedTextDelete.setOnClickListener(this);

        if (savedInstanceState != null) {
            /*
             * This data gets used in onCreate, so grab it here instead of onRestoreInstanceState
             */
            mSourceMessageProcessed = savedInstanceState.getBoolean(STATE_KEY_SOURCE_MESSAGE_PROCED, false);
        }


        if (initFromIntent(intent)) {
            mAction = Action.COMPOSE;
            draftNeedsSaving = true;
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
        updateSignature();
        mSignatureView.addTextChangedListener(signTextWatcher);

        if (!mIdentity.getSignatureUse()) {
            mSignatureView.setVisibility(View.GONE);
        }

        mReadReceipt = mAccount.isMessageReadReceiptAlways();
        mQuoteStyle = mAccount.getQuoteStyle();

        updateFrom();

        if (!mSourceMessageProcessed) {
            if (mAction == Action.REPLY || mAction == Action.REPLY_ALL ||
                    mAction == Action.FORWARD || mAction == Action.EDIT_DRAFT) {
                /*
                 * If we need to load the message we add ourself as a message listener here
                 * so we can kick it off. Normally we add in onResume but we don't
                 * want to reload the message every time the activity is resumed.
                 * There is no harm in adding twice.
                 */
                MessagingController.getInstance(getApplication()).addListener(mListener);

                final Account account = Preferences.getPreferences(this).getAccount(mMessageReference.getAccountUuid());
                final String folderName = mMessageReference.getFolderName();
                final String sourceMessageUid = mMessageReference.getUid();
                MessagingController.getInstance(getApplication()).loadMessageForView(account, folderName, sourceMessageUid, null);
            }

            if (mAction != Action.EDIT_DRAFT) {
                String alwaysBccString = mAccount.getAlwaysBcc();
                if (!TextUtils.isEmpty(alwaysBccString)) {
                    recipientPresenter.addBccAddresses(Address.parse(alwaysBccString));
                }
            }
        }

        if (mAction == Action.REPLY || mAction == Action.REPLY_ALL) {
            mMessageReference = mMessageReference.withModifiedFlag(Flag.ANSWERED);
        }

        if (mAction == Action.REPLY || mAction == Action.REPLY_ALL ||
                mAction == Action.EDIT_DRAFT) {
            //change focus to message body.
            mMessageContentView.requestFocus();
        } else {
            // Explicitly set focus to "To:" input field (see issue 2998)
            recipientMvpView.requestFocusOnToField();
        }

        if (mAction == Action.FORWARD) {
            mMessageReference = mMessageReference.withModifiedFlag(Flag.FORWARDED);
        }

        updateMessageFormat();

        // Set font size of input controls
        int fontSize = mFontSizes.getMessageComposeInput();
        recipientMvpView.setFontSizes(mFontSizes, fontSize);
        mFontSizes.setViewTextSize(mSubjectView, fontSize);
        mFontSizes.setViewTextSize(mMessageContentView, fontSize);
        mFontSizes.setViewTextSize(mQuotedText, fontSize);
        mFontSizes.setViewTextSize(mSignatureView, fontSize);


        updateMessageFormat();

        setTitle();

        currentMessageBuilder = (MessageBuilder) getLastNonConfigurationInstance();
        if (currentMessageBuilder != null) {
            setProgressBarIndeterminateVisibility(true);
            currentMessageBuilder.reattachCallback(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        recipientPresenter.onActivityDestroy();
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
            /*
             * Someone has clicked a mailto: link. The address is in the URI.
             */
            if (intent.getData() != null) {
                Uri uri = intent.getData();
                if (MailTo.isMailTo(uri)) {
                    MailTo mailTo = MailTo.parse(uri);
                    initializeFromMailto(mailTo);
                }
            }

            /*
             * Note: According to the documentation ACTION_VIEW and ACTION_SENDTO don't accept
             * EXTRA_* parameters.
             * And previously we didn't process these EXTRAs. But it looks like nobody bothers to
             * read the official documentation and just copies wrong sample code that happens to
             * work with the AOSP Email application. And because even big players get this wrong,
             * we're now finally giving in and read the EXTRAs for those actions (below).
             */
        }

        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action) ||
                Intent.ACTION_SENDTO.equals(action) || Intent.ACTION_VIEW.equals(action)) {
            startedByExternalIntent = true;

            /*
             * Note: Here we allow a slight deviation from the documented behavior.
             * EXTRA_TEXT is used as message body (if available) regardless of the MIME
             * type of the intent. In addition one or multiple attachments can be added
             * using EXTRA_STREAM.
             */
            CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            // Only use EXTRA_TEXT if the body hasn't already been set by the mailto URI
            if (text != null && mMessageContentView.getText().length() == 0) {
                mMessageContentView.setCharacters(text);
            }

            String type = intent.getType();
            if (Intent.ACTION_SEND.equals(action)) {
                Uri stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (stream != null) {
                    addAttachment(stream, type);
                }
            } else {
                List<Parcelable> list = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
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

            recipientPresenter.initFromSendOrViewIntent(intent);

        }

        return startedByExternalIntent;
    }

    @Override
    public void onResume() {
        super.onResume();
        MessagingController.getInstance(getApplication()).addListener(mListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        MessagingController.getInstance(getApplication()).removeListener(mListener);

        boolean isPausingOnConfigurationChange = (getChangingConfigurations() & ActivityInfo.CONFIG_ORIENTATION)
                == ActivityInfo.CONFIG_ORIENTATION;
        boolean isCurrentlyBuildingMessage = currentMessageBuilder != null;

        if (isPausingOnConfigurationChange || isCurrentlyBuildingMessage || isInSubActivity) {
            return;
        }

        checkToSaveDraftImplicitly();
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

        outState.putInt(STATE_KEY_NUM_ATTACHMENTS_LOADING, mNumAttachmentsLoading);
        outState.putString(STATE_KEY_WAITING_FOR_ATTACHMENTS, mWaitingForAttachments.name());
        outState.putParcelableArrayList(STATE_KEY_ATTACHMENTS, createAttachmentList());
        outState.putSerializable(STATE_KEY_QUOTED_TEXT_MODE, mQuotedTextMode);
        outState.putBoolean(STATE_KEY_SOURCE_MESSAGE_PROCED, mSourceMessageProcessed);
        outState.putLong(STATE_KEY_DRAFT_ID, mDraftId);
        outState.putSerializable(STATE_IDENTITY, mIdentity);
        outState.putBoolean(STATE_IDENTITY_CHANGED, mIdentityChanged);
        outState.putString(STATE_IN_REPLY_TO, mInReplyTo);
        outState.putString(STATE_REFERENCES, mReferences);
        outState.putSerializable(STATE_KEY_HTML_QUOTE, mQuotedHtmlContent);
        outState.putBoolean(STATE_KEY_READ_RECEIPT, mReadReceipt);
        outState.putBoolean(STATE_KEY_DRAFT_NEEDS_SAVING, draftNeedsSaving);
        outState.putBoolean(STATE_KEY_FORCE_PLAIN_TEXT, mForcePlainText);
        outState.putSerializable(STATE_KEY_QUOTED_TEXT_FORMAT, mQuotedTextFormat);
        outState.putBoolean(STATE_ALREADY_NOTIFIED_USER_OF_EMPTY_SUBJECT, alreadyNotifiedUserOfEmptySubject);

        recipientPresenter.onSaveInstanceState(outState);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (currentMessageBuilder != null) {
            currentMessageBuilder.detachCallback();
        }
        return currentMessageBuilder;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mAttachments.removeAllViews();
        mMaxLoaderId = 0;

        mNumAttachmentsLoading = savedInstanceState.getInt(STATE_KEY_NUM_ATTACHMENTS_LOADING);
        mWaitingForAttachments = WaitingAction.NONE;
        try {
            String waitingFor = savedInstanceState.getString(STATE_KEY_WAITING_FOR_ATTACHMENTS);
            mWaitingForAttachments = WaitingAction.valueOf(waitingFor);
        } catch (Exception e) {
            Log.w(K9.LOG_TAG, "Couldn't read value \" + STATE_KEY_WAITING_FOR_ATTACHMENTS +" +
                    "\" from saved instance state", e);
        }

        List<Attachment> attachments = savedInstanceState.getParcelableArrayList(STATE_KEY_ATTACHMENTS);
        // noinspection ConstantConditions, we know this is set in onSaveInstanceState
        for (Attachment attachment : attachments) {
            addAttachmentView(attachment);
            if (attachment.loaderId > mMaxLoaderId) {
                mMaxLoaderId = attachment.loaderId;
            }

            if (attachment.state == Attachment.LoadingState.URI_ONLY) {
                initAttachmentInfoLoader(attachment);
            } else if (attachment.state == Attachment.LoadingState.METADATA) {
                initAttachmentContentLoader(attachment);
            }
        }

        mReadReceipt = savedInstanceState.getBoolean(STATE_KEY_READ_RECEIPT);

        recipientPresenter.onRestoreInstanceState(savedInstanceState);

        mQuotedHtmlContent =
                (InsertableHtmlContent) savedInstanceState.getSerializable(STATE_KEY_HTML_QUOTE);
        if (mQuotedHtmlContent != null && mQuotedHtmlContent.getQuotedContent() != null) {
            mQuotedHTML.setText(mQuotedHtmlContent.getQuotedContent());
        }

        mDraftId = savedInstanceState.getLong(STATE_KEY_DRAFT_ID);
        mIdentity = (Identity)savedInstanceState.getSerializable(STATE_IDENTITY);
        mIdentityChanged = savedInstanceState.getBoolean(STATE_IDENTITY_CHANGED);
        mInReplyTo = savedInstanceState.getString(STATE_IN_REPLY_TO);
        mReferences = savedInstanceState.getString(STATE_REFERENCES);
        draftNeedsSaving = savedInstanceState.getBoolean(STATE_KEY_DRAFT_NEEDS_SAVING);
        mForcePlainText = savedInstanceState.getBoolean(STATE_KEY_FORCE_PLAIN_TEXT);
        alreadyNotifiedUserOfEmptySubject = savedInstanceState.getBoolean(STATE_ALREADY_NOTIFIED_USER_OF_EMPTY_SUBJECT);
        mQuotedTextFormat = (SimpleMessageFormat) savedInstanceState.getSerializable(
                STATE_KEY_QUOTED_TEXT_FORMAT);

        showOrHideQuotedText(
                (QuotedTextMode) savedInstanceState.getSerializable(STATE_KEY_QUOTED_TEXT_MODE));

        updateFrom();

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

    @Nullable
    private MessageBuilder createMessageBuilder(boolean isDraft) {
        MessageBuilder builder;

        recipientPresenter.updateCryptoStatus();
        ComposeCryptoStatus cryptoStatus = recipientPresenter.getCurrentCryptoStatus();
        // TODO encrypt drafts for storage
        if(!isDraft && cryptoStatus.shouldUsePgpMessageBuilder()) {
            SendErrorState maybeSendErrorState = cryptoStatus.getSendErrorStateOrNull();
            if (maybeSendErrorState != null) {
                recipientPresenter.showPgpSendError(maybeSendErrorState);
                return null;
            }

            OpenPgpApi openPgpApi = recipientPresenter.getOpenPgpApi();
            PgpMessageBuilder pgpBuilder = new PgpMessageBuilder(getApplicationContext(), openPgpApi);
            pgpBuilder.setCryptoStatus(cryptoStatus);
            builder = pgpBuilder;
        } else {
            builder = new SimpleMessageBuilder(getApplicationContext());
        }

        builder.setSubject(mSubjectView.getText().toString())
                .setTo(recipientPresenter.getToAddresses())
                .setCc(recipientPresenter.getCcAddresses())
                .setBcc(recipientPresenter.getBccAddresses())
                .setInReplyTo(mInReplyTo)
                .setReferences(mReferences)
                .setRequestReadReceipt(mReadReceipt)
                .setIdentity(mIdentity)
                .setMessageFormat(mMessageFormat)
                .setText(mMessageContentView.getCharacters())
                .setAttachments(createAttachmentList())
                .setSignature(mSignatureView.getCharacters())
                .setQuoteStyle(mQuoteStyle)
                .setQuotedTextMode(mQuotedTextMode)
                .setQuotedText(mQuotedText.getCharacters())
                .setQuotedHtmlContent(mQuotedHtmlContent)
                .setReplyAfterQuote(mAccount.isReplyAfterQuote())
                .setSignatureBeforeQuotedText(mAccount.isSignatureBeforeQuotedText())
                .setIdentityChanged(mIdentityChanged)
                .setSignatureChanged(mSignatureChanged)
                .setCursorPosition(mMessageContentView.getSelectionStart())
                .setMessageReference(mMessageReference)
                .setDraft(isDraft);

        return builder;
    }

    private void checkToSendMessage() {
        if (mSubjectView.getText().length() == 0 && !alreadyNotifiedUserOfEmptySubject) {
            Toast.makeText(this, R.string.empty_subject, Toast.LENGTH_LONG).show();
            alreadyNotifiedUserOfEmptySubject = true;
            return;
        }

        if (recipientPresenter.checkRecipientsOkForSending()) {
            return;
        }

        if (mWaitingForAttachments != WaitingAction.NONE) {
            return;
        }

        if (mNumAttachmentsLoading > 0) {
            mWaitingForAttachments = WaitingAction.SEND;
            showWaitingForAttachmentDialog();
            return;
        }

        performSendAfterChecks();
    }

    private void checkToSaveDraftAndSave() {
        if (!mAccount.hasDraftsFolder()) {
            Toast.makeText(this, R.string.compose_error_no_draft_folder, Toast.LENGTH_SHORT).show();
            return;
        }

        if (mWaitingForAttachments != WaitingAction.NONE) {
            return;
        }

        if (mNumAttachmentsLoading > 0) {
            mWaitingForAttachments = WaitingAction.SAVE;
            showWaitingForAttachmentDialog();
            return;
        }

        mFinishAfterDraftSaved = true;
        performSaveAfterChecks();
    }

    private void checkToSaveDraftImplicitly() {
        if (!mAccount.hasDraftsFolder()) {
            return;
        }

        if (!draftNeedsSaving) {
            return;
        }

        mFinishAfterDraftSaved = false;
        performSaveAfterChecks();
    }

    private void performSaveAfterChecks() {
        currentMessageBuilder = createMessageBuilder(true);
        if (currentMessageBuilder != null) {
            setProgressBarIndeterminateVisibility(true);
            currentMessageBuilder.buildAsync(this);
        }
    }

    public void performSendAfterChecks() {
        currentMessageBuilder = createMessageBuilder(false);
        if (currentMessageBuilder != null) {
            draftNeedsSaving = false;
            setProgressBarIndeterminateVisibility(true);
            currentMessageBuilder.buildAsync(this);
        }
    }

    private void onDiscard() {
        if (mDraftId != INVALID_DRAFT_ID) {
            MessagingController.getInstance(getApplication()).deleteDraft(mAccount, mDraftId);
            mDraftId = INVALID_DRAFT_ID;
        }
        mHandler.sendEmptyMessage(MSG_DISCARDED_DRAFT);
        draftNeedsSaving = false;
        finish();
    }

    private void onReadReceipt() {
        CharSequence txt;
        if (!mReadReceipt) {
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

    private ArrayList<Attachment> createAttachmentList() {
        ArrayList<Attachment> attachments = new ArrayList<>();
        for (int i = 0, count = mAttachments.getChildCount(); i < count; i++) {
            View view = mAttachments.getChildAt(i);
            Attachment attachment = (Attachment) view.getTag();
            attachments.add(attachment);
        }
        return attachments;
    }

    /**
     * Kick off a picker for the specified MIME type and let Android take over.
     */
    @SuppressLint("InlinedApi")
    private void onAddAttachment() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        isInSubActivity = true;
        startActivityForResult(Intent.createChooser(i, null), ACTIVITY_REQUEST_PICK_ATTACHMENT);
    }

    private void addAttachment(Uri uri) {
        addAttachment(uri, null);
    }

    private void addAttachment(Uri uri, String contentType) {
        Attachment attachment = new Attachment();
        attachment.state = Attachment.LoadingState.URI_ONLY;
        attachment.uri = uri;
        attachment.contentType = contentType;
        attachment.loaderId = ++mMaxLoaderId;

        addAttachmentView(attachment);

        initAttachmentInfoLoader(attachment);
    }

    private void initAttachmentInfoLoader(Attachment attachment) {
        LoaderManager loaderManager = getLoaderManager();
        Bundle bundle = new Bundle();
        bundle.putParcelable(LOADER_ARG_ATTACHMENT, attachment);
        loaderManager.initLoader(attachment.loaderId, bundle, mAttachmentInfoLoaderCallback);
    }

    private void initAttachmentContentLoader(Attachment attachment) {
        LoaderManager loaderManager = getLoaderManager();
        Bundle bundle = new Bundle();
        bundle.putParcelable(LOADER_ARG_ATTACHMENT, attachment);
        loaderManager.initLoader(attachment.loaderId, bundle, mAttachmentContentLoaderCallback);
    }

    private void addAttachmentView(Attachment attachment) {
        boolean hasMetadata = (attachment.state != Attachment.LoadingState.URI_ONLY);
        boolean isLoadingComplete = (attachment.state == Attachment.LoadingState.COMPLETE);

        View view = getLayoutInflater().inflate(R.layout.message_compose_attachment, mAttachments, false);
        TextView nameView = (TextView) view.findViewById(R.id.attachment_name);
        View progressBar = view.findViewById(R.id.progressBar);

        if (hasMetadata) {
            nameView.setText(attachment.name);
        } else {
            nameView.setText(R.string.loading_attachment);
        }

        progressBar.setVisibility(isLoadingComplete ? View.GONE : View.VISIBLE);

        ImageButton delete = (ImageButton) view.findViewById(R.id.attachment_delete);
        delete.setOnClickListener(MessageCompose.this);
        delete.setTag(view);

        view.setTag(attachment);
        mAttachments.addView(view);
    }

    private View getAttachmentView(int loaderId) {
        for (int i = 0, childCount = mAttachments.getChildCount(); i < childCount; i++) {
            View view = mAttachments.getChildAt(i);
            Attachment tag = (Attachment) view.getTag();
            if (tag != null && tag.loaderId == loaderId) {
                return view;
            }
        }

        return null;
    }

    private LoaderManager.LoaderCallbacks<Attachment> mAttachmentInfoLoaderCallback =
            new LoaderManager.LoaderCallbacks<Attachment>() {
        @Override
        public Loader<Attachment> onCreateLoader(int id, Bundle args) {
            onFetchAttachmentStarted();
            Attachment attachment = args.getParcelable(LOADER_ARG_ATTACHMENT);
            return new AttachmentInfoLoader(MessageCompose.this, attachment);
        }

        @Override
        public void onLoadFinished(Loader<Attachment> loader, Attachment attachment) {
            int loaderId = loader.getId();

            View view = getAttachmentView(loaderId);
            if (view != null) {
                view.setTag(attachment);

                TextView nameView = (TextView) view.findViewById(R.id.attachment_name);
                nameView.setText(attachment.name);

                attachment.loaderId = ++mMaxLoaderId;
                initAttachmentContentLoader(attachment);
            } else {
                onFetchAttachmentFinished();
            }

            getLoaderManager().destroyLoader(loaderId);
        }

        @Override
        public void onLoaderReset(Loader<Attachment> loader) {
            onFetchAttachmentFinished();
        }
    };

    private LoaderManager.LoaderCallbacks<Attachment> mAttachmentContentLoaderCallback =
            new LoaderManager.LoaderCallbacks<Attachment>() {
        @Override
        public Loader<Attachment> onCreateLoader(int id, Bundle args) {
            Attachment attachment = args.getParcelable(LOADER_ARG_ATTACHMENT);
            return new AttachmentContentLoader(MessageCompose.this, attachment);
        }

        @Override
        public void onLoadFinished(Loader<Attachment> loader, Attachment attachment) {
            int loaderId = loader.getId();

            View view = getAttachmentView(loaderId);
            if (view != null) {
                if (attachment.state == Attachment.LoadingState.COMPLETE) {
                    view.setTag(attachment);

                    View progressBar = view.findViewById(R.id.progressBar);
                    progressBar.setVisibility(View.GONE);
                } else {
                    mAttachments.removeView(view);
                }
            }

            onFetchAttachmentFinished();

            getLoaderManager().destroyLoader(loaderId);
        }

        @Override
        public void onLoaderReset(Loader<Attachment> loader) {
            onFetchAttachmentFinished();
        }
    };

    private void onFetchAttachmentStarted() {
        mNumAttachmentsLoading += 1;
    }

    private void onFetchAttachmentFinished() {
        // We're not allowed to perform fragment transactions when called from onLoadFinished().
        // So we use the Handler to call performStalledAction().
        mHandler.sendEmptyMessage(MSG_PERFORM_STALLED_ACTION);
    }

    private void performStalledAction() {
        mNumAttachmentsLoading -= 1;

        WaitingAction waitingFor = mWaitingForAttachments;
        mWaitingForAttachments = WaitingAction.NONE;

        if (waitingFor != WaitingAction.NONE) {
            dismissWaitingForAttachmentDialog();
        }

        switch (waitingFor) {
            case SEND: {
                performSendAfterChecks();
                break;
            }
            case SAVE: {
                performSaveAfterChecks();
                break;
            }
            case NONE:
                break;
        }
    }

    public void showContactPicker(int requestCode) {
        requestCode |= REQUEST_MASK_RECIPIENT_PRESENTER;
        isInSubActivity = true;
        startActivityForResult(mContacts.contactPickerIntent(), requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        isInSubActivity = false;

        if ((requestCode & REQUEST_MASK_MESSAGE_BUILDER) == REQUEST_MASK_MESSAGE_BUILDER) {
            requestCode ^= REQUEST_MASK_MESSAGE_BUILDER;
            if (currentMessageBuilder == null) {
                Log.e(K9.LOG_TAG, "Got a message builder activity result for no message builder, " +
                        "this is an illegal state!");
                return;
            }
            currentMessageBuilder.onActivityResult(this, requestCode, resultCode, data);
            return;
        }

        if ((requestCode & REQUEST_MASK_RECIPIENT_PRESENTER) == REQUEST_MASK_RECIPIENT_PRESENTER) {
            requestCode ^= REQUEST_MASK_RECIPIENT_PRESENTER;
            recipientPresenter.onActivityResult(resultCode, requestCode, data);
            return;
        }

        if (resultCode != RESULT_OK) {
            return;
        }

        if (data == null) {
            return;
        }

        switch (requestCode) {
            case ACTIVITY_REQUEST_PICK_ATTACHMENT:
                addAttachmentsFromResultIntent(data);
                draftNeedsSaving = true;
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void addAttachmentsFromResultIntent(Intent data) {
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
            if (draftNeedsSaving || (mDraftId != INVALID_DRAFT_ID)) {
                final long previousDraftId = mDraftId;
                final Account previousAccount = mAccount;

                // make current message appear as new
                mDraftId = INVALID_DRAFT_ID;

                // actual account switch
                mAccount = account;

                if (K9.DEBUG) {
                    Log.v(K9.LOG_TAG, "Account switch, saving new draft in new account");
                }
                checkToSaveDraftImplicitly();

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
            recipientPresenter.onSwitchAccount(mAccount);

            // not sure how to handle mFolder, mSourceMessage?
        }

        switchToIdentity(identity);
    }

    private void switchToIdentity(Identity identity) {
        mIdentity = identity;
        mIdentityChanged = true;
        draftNeedsSaving = true;
        updateFrom();
        updateSignature();
        updateMessageFormat();
        recipientPresenter.onSwitchIdentity(identity);
    }

    private void updateFrom() {
        mChooseIdentityButton.setText(mIdentity.getEmail());
    }

    private void updateSignature() {
        if (mIdentity.getSignatureUse()) {
            mSignatureView.setCharacters(mIdentity.getSignature());
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
                draftNeedsSaving = true;
                break;
            case R.id.quoted_text_show:
                showOrHideQuotedText(QuotedTextMode.SHOW);
                updateMessageFormat();
                draftNeedsSaving = true;
                break;
            case R.id.quoted_text_delete:
                showOrHideQuotedText(QuotedTextMode.HIDE);
                updateMessageFormat();
                draftNeedsSaving = true;
                break;
            case R.id.quoted_text_edit:
                mForcePlainText = true;
                if (mMessageReference != null) { // shouldn't happen...
                    // TODO - Should we check if mSourceMessageBody is already present and bypass the MessagingController call?
                    MessagingController.getInstance(getApplication()).addListener(mListener);
                    final Account account = Preferences.getPreferences(this).getAccount(mMessageReference.getAccountUuid());
                    final String folderName = mMessageReference.getFolderName();
                    final String sourceMessageUid = mMessageReference.getUid();
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

    private void askBeforeDiscard(){
        if (K9.confirmDiscardMessage()) {
            showDialog(DIALOG_CONFIRM_DISCARD);
        } else {
            onDiscard();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.send:
                checkToSendMessage();
                break;
            case R.id.save:
                checkToSaveDraftAndSave();
                break;
            case R.id.discard:
                askBeforeDiscard();
                break;
            case R.id.add_from_contacts:
                recipientPresenter.onMenuAddFromContacts();
                break;
            case R.id.add_attachment:
                onAddAttachment();
                break;
            case R.id.read_receipt:
                onReadReceipt();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.message_compose_option, menu);

        // Disable the 'Save' menu option if Drafts folder is set to -NONE-
        if (!mAccount.hasDraftsFolder()) {
            menu.findItem(R.id.save).setEnabled(false);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        recipientPresenter.onPrepareOptionsMenu(menu);

        return true;
    }

    @Override
    public void onBackPressed() {
        if (draftNeedsSaving) {
            if (!mAccount.hasDraftsFolder()) {
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

    private void showWaitingForAttachmentDialog() {
        String title;

        switch (mWaitingForAttachments) {
            case SEND: {
                title = getString(R.string.fetching_attachment_dialog_title_send);
                break;
            }
            case SAVE: {
                title = getString(R.string.fetching_attachment_dialog_title_save);
                break;
            }
            default: {
                return;
            }
        }

        ProgressDialogFragment fragment = ProgressDialogFragment.newInstance(title,
                getString(R.string.fetching_attachment_dialog_message));
        fragment.show(getFragmentManager(), FRAGMENT_WAITING_FOR_ATTACHMENT);
    }

    public void onCancel(ProgressDialogFragment fragment) {
        attachmentProgressDialogCancelled();
    }

    void attachmentProgressDialogCancelled() {
        mWaitingForAttachments = WaitingAction.NONE;
    }

    private void dismissWaitingForAttachmentDialog() {
        ProgressDialogFragment fragment = (ProgressDialogFragment)
                getFragmentManager().findFragmentByTag(FRAGMENT_WAITING_FOR_ATTACHMENT);

        if (fragment != null) {
            fragment.dismiss();
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
                        checkToSaveDraftAndSave();
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
            case DIALOG_CONFIRM_DISCARD: {
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_confirm_delete_title)
                        .setMessage(R.string.dialog_confirm_delete_message)
                        .setPositiveButton(R.string.dialog_confirm_delete_confirm_button,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        onDiscard();
                                    }
                                })
                        .setNegativeButton(R.string.dialog_confirm_delete_cancel_button,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                        .create();
            }
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

    /**
     * Pull out the parts of the now loaded source message and apply them to the new message
     * depending on the type of message being composed.
     *
     * @param message
     *         The source message used to populate the various text fields.
     */
    private void processSourceMessage(LocalMessage message) {
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
            draftNeedsSaving = false;
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
        recipientPresenter.initFromReplyToMessage(message);

        if (message.getMessageId() != null && message.getMessageId().length() > 0) {
            mInReplyTo = message.getMessageId();

            String[] refs = message.getReferences();
            if (refs != null && refs.length > 0) {
                mReferences = TextUtils.join("", refs) + " " + mInReplyTo;
            } else {
                mReferences = mInReplyTo;
            }

        } else {
            if (K9.DEBUG) {
                Log.d(K9.LOG_TAG, "could not get Message-ID.");
            }
        }

        // Quote the message and setup the UI.
        populateUIWithQuotedMessage(mAccount.isDefaultQuotedTextShown());

        if (mAction == Action.REPLY || mAction == Action.REPLY_ALL) {
            Identity useIdentity = IdentityHelper.getRecipientIdentityFromMessage(mAccount, message);
            Identity defaultIdentity = mAccount.getIdentity(0);
            if (useIdentity != defaultIdentity) {
                switchToIdentity(useIdentity);
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
        if (!TextUtils.isEmpty(message.getMessageId())) {
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
            if (message.isSet(Flag.X_DOWNLOADED_PARTIAL) || !loadAttachments(message, 0)) {
                mHandler.sendEmptyMessage(MSG_SKIPPED_ATTACHMENTS);
            }
        }
    }

    private void processDraftMessage(LocalMessage message) throws MessagingException {
        String showQuotedTextMode = "NONE";

        mDraftId = MessagingController.getInstance(getApplication()).getId(message);
        mSubjectView.setText(message.getSubject());

        recipientPresenter.initFromDraftMessage(message);

        // Read In-Reply-To header from draft
        final String[] inReplyTo = message.getHeader("In-Reply-To");
        if (inReplyTo.length >= 1) {
            mInReplyTo = inReplyTo[0];
        }

        // Read References header from draft
        final String[] references = message.getHeader("References");
        if (references.length >= 1) {
            mReferences = references[0];
        }

        if (!mSourceMessageProcessed) {
            loadAttachments(message, 0);
        }

        // Decode the identity header when loading a draft.
        // See buildIdentityHeader(TextBody) for a detailed description of the composition of this blob.
        Map<IdentityField, String> k9identity = new HashMap<>();
        String[] identityHeaders = message.getHeader(K9.IDENTITY_HEADER);

        if (identityHeaders.length > 0 && identityHeaders[0] != null) {
            k9identity = IdentityHeaderParser.parse(identityHeaders[0]);
        }

        Identity newIdentity = new Identity();
        if (k9identity.containsKey(IdentityField.SIGNATURE)) {
            newIdentity.setSignatureUse(true);
            newIdentity.setSignature(k9identity.get(IdentityField.SIGNATURE));
            mSignatureChanged = true;
        } else {
            newIdentity.setSignatureUse(message.getFolder().getSignatureUse());
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
                Account account = prefs.getAccount(messageReference.getAccountUuid());
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
                cursorPosition = Integer.parseInt(k9identity.get(IdentityField.CURSOR_POSITION));
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
            mMessageContentView.setCharacters(getBodyTextFromMessage(message, SimpleMessageFormat.TEXT));
            mForcePlainText = true;

            showOrHideQuotedText(quotedMode);
            return;
        }


        if (messageFormat == MessageFormat.HTML) {
            Part part = MimeUtility.findFirstPartByMimeType(message, "text/html");
            if (part != null) { // Shouldn't happen if we were the one who saved it.
                mQuotedTextFormat = SimpleMessageFormat.HTML;
                String text = MessageExtractor.getTextFromPart(part);
                if (K9.DEBUG) {
                    Log.d(K9.LOG_TAG, "Loading message with offset " + bodyOffset + ", length " + bodyLength + ". Text length is " + text.length() + ".");
                }

                if (bodyOffset + bodyLength > text.length()) {
                    // The draft was edited outside of K-9 Mail?
                    Log.d(K9.LOG_TAG, "The identity field from the draft contains an invalid LENGTH/OFFSET");
                    bodyOffset = 0;
                    bodyLength = 0;
                }
                // Grab our reply text.
                String bodyText = text.substring(bodyOffset, bodyOffset + bodyLength);
                mMessageContentView.setCharacters(HtmlConverter.htmlToText(bodyText));

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

    /**
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
            String text = MessageExtractor.getTextFromPart(textPart);
            if (K9.DEBUG) {
                Log.d(K9.LOG_TAG, "Loading message with offset " + bodyOffset + ", length " + bodyLength + ". Text length is " + text.length() + ".");
            }

            // If we had a body length (and it was valid), separate the composition from the quoted text
            // and put them in their respective places in the UI.
            if (bodyLength > 0) {
                try {
                    String bodyText = text.substring(bodyOffset, bodyOffset + bodyLength);

                    // Regenerate the quoted text without our user content in it nor added newlines.
                    StringBuilder quotedText = new StringBuilder();
                    if (bodyOffset == 0 && text.substring(bodyLength, bodyLength + 4).equals("\r\n\r\n")) {
                        // top-posting: ignore two newlines at start of quote
                        quotedText.append(text.substring(bodyLength + 4));
                    } else if (bodyOffset + bodyLength == text.length() &&
                            text.substring(bodyOffset - 2, bodyOffset).equals("\r\n")) {
                        // bottom-posting: ignore newline at end of quote
                        quotedText.append(text.substring(0, bodyOffset - 2));
                    } else {
                        quotedText.append(text.substring(0, bodyOffset));   // stuff before the reply
                        quotedText.append(text.substring(bodyOffset + bodyLength));
                    }

                    if (viewMessageContent) {
                        mMessageContentView.setCharacters(bodyText);
                    }

                    mQuotedText.setCharacters(quotedText);
                } catch (IndexOutOfBoundsException e) {
                    // Invalid bodyOffset or bodyLength.  The draft was edited outside of K-9 Mail?
                    Log.d(K9.LOG_TAG, "The identity field from the draft contains an invalid bodyOffset/bodyLength");
                    if (viewMessageContent) {
                        mMessageContentView.setCharacters(text);
                    }
                }
            } else {
                if (viewMessageContent) {
                    mMessageContentView.setCharacters(text);
                }
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
                    List<Integer> start = new ArrayList<>();
                    List<Integer> end = new ArrayList<>();

                    while (blockquoteStart.find()) {
                        start.add(blockquoteStart.start());
                    }
                    while (blockquoteEnd.find()) {
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
                                if (end.get(i) < start.get(i + 1)) {
                                    dashSignatureHtml.region(end.get(i), start.get(i + 1));
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
                content = htmlSerialized.getAsString(node, "UTF8");
            }

            // Add the HTML reply header to the top of the content.
            mQuotedHtmlContent = quoteOriginalHtmlMessage(mSourceMessage, content, mQuoteStyle);

            // Load the message with the reply header.
            mQuotedHTML.setText(mQuotedHtmlContent.getQuotedContent());

            // TODO: Also strip the signature from the text/plain part
            mQuotedText.setCharacters(quoteOriginalTextMessage(mSourceMessage,
                    getBodyTextFromMessage(mSourceMessage, SimpleMessageFormat.TEXT), mQuoteStyle));

        } else if (mQuotedTextFormat == SimpleMessageFormat.TEXT) {
            if (mAccount.isStripSignature() &&
                    (mAction == Action.REPLY || mAction == Action.REPLY_ALL)) {
                if (DASH_SIGNATURE_PLAIN.matcher(content).find()) {
                    content = DASH_SIGNATURE_PLAIN.matcher(content).replaceFirst("\r\n");
                }
            }

            mQuotedText.setCharacters(quoteOriginalTextMessage(mSourceMessage, content, mQuoteStyle));
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
                if (K9.DEBUG) {
                    Log.d(K9.LOG_TAG, "getBodyTextFromMessage: HTML requested, HTML found.");
                }
                return MessageExtractor.getTextFromPart(part);
            }

            part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
            if (part != null) {
                if (K9.DEBUG) {
                    Log.d(K9.LOG_TAG, "getBodyTextFromMessage: HTML requested, text found.");
                }
                String text = MessageExtractor.getTextFromPart(part);
                return HtmlConverter.textToHtml(text);
            }
        } else if (format == SimpleMessageFormat.TEXT) {
            // Text takes precedence, then html.
            part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
            if (part != null) {
                if (K9.DEBUG) {
                    Log.d(K9.LOG_TAG, "getBodyTextFromMessage: Text requested, text found.");
                }
                return MessageExtractor.getTextFromPart(part);
            }

            part = MimeUtility.findFirstPartByMimeType(message, "text/html");
            if (part != null) {
                if (K9.DEBUG) {
                    Log.d(K9.LOG_TAG, "getBodyTextFromMessage: Text requested, HTML found.");
                }
                String text = MessageExtractor.getTextFromPart(part);
                return HtmlConverter.htmlToText(text);
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
    private static final String FIND_INSERTION_POINT_HTML_CONTENT = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\r\n<html>";
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

        if (K9.DEBUG) {
            Log.d(K9.LOG_TAG, "Open: hasHtmlTag:" + hasHtmlTag + " hasHeadTag:" + hasHeadTag + " hasBodyTag:" + hasBodyTag);
        }

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

        if (K9.DEBUG) {
            Log.d(K9.LOG_TAG, "Close: hasHtmlEndTag:" + hasHtmlEndTag + " hasBodyEndTag:" + hasBodyEndTag);
        }

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

    static class SendMessageTask extends AsyncTask<Void, Void, Void> {
        final Context context;
        final Account account;
        final Contacts contacts;
        final Message message;
        final Long draftId;
        final MessageReference messageReference;

        SendMessageTask(Context context, Account account, Contacts contacts, Message message,
                        Long draftId, MessageReference messageReference) {
            this.context = context;
            this.account = account;
            this.contacts = contacts;
            this.message = message;
            this.draftId = draftId;
            this.messageReference = messageReference;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                contacts.markAsContacted(message.getRecipients(RecipientType.TO));
                contacts.markAsContacted(message.getRecipients(RecipientType.CC));
                contacts.markAsContacted(message.getRecipients(RecipientType.BCC));
                updateReferencedMessage();
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Failed to mark contact as contacted.", e);
            }

            MessagingController.getInstance(context).sendMessage(account, message, null);
            if (draftId != null) {
                // TODO set draft id to invalid in MessageCompose!
                MessagingController.getInstance(context).deleteDraft(account, draftId);
            }

            return null;
        }

        /**
         * Set the flag on the referenced message(indicated we replied / forwarded the message)
         **/
        private void updateReferencedMessage() {
            if (messageReference != null && messageReference.getFlag() != null) {
                if (K9.DEBUG) {
                    Log.d(K9.LOG_TAG, "Setting referenced message (" +
                            messageReference.getFolderName() + ", " +
                            messageReference.getUid() + ") flag to " +
                            messageReference.getFlag());
                }
                final Account account = Preferences.getPreferences(context)
                        .getAccount(messageReference.getAccountUuid());
                final String folderName = messageReference.getFolderName();
                final String sourceMessageUid = messageReference.getUid();
                MessagingController.getInstance(context).setFlag(account, folderName,
                        sourceMessageUid, messageReference.getFlag(), true);
            }
        }
    }

    class Listener extends MessagingListener {
        @Override
        public void loadMessageForViewStarted(Account account, String folder, String uid) {
            if ((mMessageReference == null) || !mMessageReference.getUid().equals(uid)) {
                return;
            }

            mHandler.sendEmptyMessage(MSG_PROGRESS_ON);
        }

        @Override
        public void loadMessageForViewFinished(Account account, String folder, String uid, LocalMessage message) {
            if ((mMessageReference == null) || !mMessageReference.getUid().equals(uid)) {
                return;
            }

            mHandler.sendEmptyMessage(MSG_PROGRESS_OFF);
        }

        @Override
        public void loadMessageForViewBodyAvailable(Account account, String folder, String uid, final Message message) {
            if ((mMessageReference == null) || !mMessageReference.getUid().equals(uid)) {
                return;
            }

            mSourceMessage = message;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadLocalMessageForDisplay((LocalMessage) message);
                }
            });
        }

        @Override
        public void loadMessageForViewFailed(Account account, String folder, String uid, Throwable t) {
            if ((mMessageReference == null) || !mMessageReference.getUid().equals(uid)) {
                return;
            }
            mHandler.sendEmptyMessage(MSG_PROGRESS_OFF);
            // TODO show network error
        }

        @Override
        public void messageUidChanged(Account account, String folder, String oldUid, String newUid) {
            // Track UID changes of the source message
            if (mMessageReference != null) {
                final Account sourceAccount = Preferences.getPreferences(MessageCompose.this).getAccount(mMessageReference.getAccountUuid());
                final String sourceFolder = mMessageReference.getFolderName();
                final String sourceMessageUid = mMessageReference.getUid();

                if (account.equals(sourceAccount) && (folder.equals(sourceFolder))) {
                    if (oldUid.equals(sourceMessageUid)) {
                        mMessageReference = mMessageReference.withModifiedUid(newUid);
                    }
                    if ((mSourceMessage != null) && (oldUid.equals(mSourceMessage.getUid()))) {
                        mSourceMessage.setUid(newUid);
                    }
                }
            }
        }
    }

    private void loadLocalMessageForDisplay(LocalMessage message) {
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

    /**
     * When we are launched with an intent that includes a mailto: URI, we can actually
     * gather quite a few of our message fields from it.
     *
     * @param mailTo
     *         The MailTo object we use to initialize message field
     */
    private void initializeFromMailto(MailTo mailTo) {
        recipientPresenter.initFromMailto(mailTo);

        String subject = mailTo.getSubject();
        if (subject != null && !subject.isEmpty()) {
            mSubjectView.setText(subject);
        }

        String body = mailTo.getBody();
        if (body != null && !subject.isEmpty()) {
            mMessageContentView.setCharacters(body);
        }
    }

    private static class SaveMessageTask extends AsyncTask<Void, Void, Void> {
        Context context;
        Account account;
        Contacts contacts;
        Handler handler;
        Message message;
        long draftId;
        boolean saveRemotely;

        SaveMessageTask(Context context, Account account, Contacts contacts,
                Handler handler, Message message, long draftId, boolean saveRemotely) {
            this.context = context;
            this.account = account;
            this.contacts = contacts;
            this.handler = handler;
            this.message = message;
            this.draftId = draftId;
            this.saveRemotely = saveRemotely;
        }

        @Override
        protected Void doInBackground(Void... params) {
            final MessagingController messagingController = MessagingController.getInstance(context);
            Message draftMessage = messagingController.saveDraft(account, message, draftId, saveRemotely);
            draftId = messagingController.getId(draftMessage);

            android.os.Message msg = android.os.Message.obtain(handler, MSG_SAVED_DRAFT, draftId);
            handler.sendMessage(msg);
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
        String sentDate = getSentDateText(originalMessage);
        if (quoteStyle == QuoteStyle.PREFIX) {
            StringBuilder quotedText = new StringBuilder(body.length() + QUOTE_BUFFER_LENGTH);
            if (sentDate.length() != 0) {
                quotedText.append(String.format(
                        getString(R.string.message_compose_reply_header_fmt_with_date) + "\r\n",
                        sentDate,
                        Address.toString(originalMessage.getFrom())));
            } else {
                quotedText.append(String.format(
                                      getString(R.string.message_compose_reply_header_fmt) + "\r\n",
                                      Address.toString(originalMessage.getFrom()))
                                 );
            }

            final String prefix = mAccount.getQuotePrefix();
            final String wrappedText = Utility.wrap(body, REPLY_WRAP_LINE_WIDTH - prefix.length());

            // "$" and "\" in the quote prefix have to be escaped for
            // the replaceAll() invocation.
            final String escapedPrefix = prefix.replaceAll("(\\\\|\\$)", "\\\\$1");
            quotedText.append(wrappedText.replaceAll("(?m)^", escapedPrefix));

            return quotedText.toString().replaceAll("\\\r", "");
        } else if (quoteStyle == QuoteStyle.HEADER) {
            StringBuilder quotedText = new StringBuilder(body.length() + QUOTE_BUFFER_LENGTH);
            quotedText.append("\r\n");
            quotedText.append(getString(R.string.message_compose_quote_header_separator)).append("\r\n");
            if (originalMessage.getFrom() != null && Address.toString(originalMessage.getFrom()).length() != 0) {
                quotedText.append(getString(R.string.message_compose_quote_header_from)).append(" ").append(Address.toString(originalMessage.getFrom())).append("\r\n");
            }
            if (sentDate.length() != 0) {
                quotedText.append(getString(R.string.message_compose_quote_header_send_date)).append(" ").append(sentDate).append("\r\n");
            }
            if (originalMessage.getRecipients(RecipientType.TO) != null && originalMessage.getRecipients(RecipientType.TO).length != 0) {
                quotedText.append(getString(R.string.message_compose_quote_header_to)).append(" ").append(Address.toString(originalMessage.getRecipients(RecipientType.TO))).append("\r\n");
            }
            if (originalMessage.getRecipients(RecipientType.CC) != null && originalMessage.getRecipients(RecipientType.CC).length != 0) {
                quotedText.append(getString(R.string.message_compose_quote_header_cc)).append(" ").append(Address.toString(originalMessage.getRecipients(RecipientType.CC))).append("\r\n");
            }
            if (originalMessage.getSubject() != null) {
                quotedText.append(getString(R.string.message_compose_quote_header_subject)).append(" ").append(originalMessage.getSubject()).append("\r\n");
            }
            quotedText.append("\r\n");

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

        String sentDate = getSentDateText(originalMessage);
        if (quoteStyle == QuoteStyle.PREFIX) {
            StringBuilder header = new StringBuilder(QUOTE_BUFFER_LENGTH);
            header.append("<div class=\"gmail_quote\">");
            if (sentDate.length() != 0) {
                header.append(HtmlConverter.textToHtmlFragment(String.format(
                        getString(R.string.message_compose_reply_header_fmt_with_date),
                        sentDate,
                        Address.toString(originalMessage.getFrom()))
                                                    ));
            } else {
                header.append(HtmlConverter.textToHtmlFragment(String.format(
                                  getString(R.string.message_compose_reply_header_fmt),
                                  Address.toString(originalMessage.getFrom()))
                                                              ));
            }
            header.append("<blockquote class=\"gmail_quote\" " +
                          "style=\"margin: 0pt 0pt 0pt 0.8ex; border-left: 1px solid rgb(204, 204, 204); padding-left: 1ex;\">\r\n");

            String footer = "</blockquote></div>";

            insertable.insertIntoQuotedHeader(header.toString());
            insertable.insertIntoQuotedFooter(footer);
        } else if (quoteStyle == QuoteStyle.HEADER) {

            StringBuilder header = new StringBuilder();
            header.append("<div style='font-size:10.0pt;font-family:\"Tahoma\",\"sans-serif\";padding:3.0pt 0in 0in 0in'>\r\n");
            header.append("<hr style='border:none;border-top:solid #E1E1E1 1.0pt'>\r\n"); // This gets converted into a horizontal line during html to text conversion.
            if (originalMessage.getFrom() != null && Address.toString(originalMessage.getFrom()).length() != 0) {
                header.append("<b>").append(getString(R.string.message_compose_quote_header_from)).append("</b> ")
                    .append(HtmlConverter.textToHtmlFragment(Address.toString(originalMessage.getFrom())))
                    .append("<br>\r\n");
            }
            if (sentDate.length() != 0) {
                header.append("<b>").append(getString(R.string.message_compose_quote_header_send_date)).append("</b> ")
                    .append(sentDate)
                    .append("<br>\r\n");
            }
            if (originalMessage.getRecipients(RecipientType.TO) != null && originalMessage.getRecipients(RecipientType.TO).length != 0) {
                header.append("<b>").append(getString(R.string.message_compose_quote_header_to)).append("</b> ")
                    .append(HtmlConverter.textToHtmlFragment(Address.toString(originalMessage.getRecipients(RecipientType.TO))))
                    .append("<br>\r\n");
            }
            if (originalMessage.getRecipients(RecipientType.CC) != null && originalMessage.getRecipients(RecipientType.CC).length != 0) {
                header.append("<b>").append(getString(R.string.message_compose_quote_header_cc)).append("</b> ")
                    .append(HtmlConverter.textToHtmlFragment(Address.toString(originalMessage.getRecipients(RecipientType.CC))))
                    .append("<br>\r\n");
            }
            if (originalMessage.getSubject() != null) {
                header.append("<b>").append(getString(R.string.message_compose_quote_header_subject)).append("</b> ")
                    .append(HtmlConverter.textToHtmlFragment(originalMessage.getSubject()))
                    .append("<br>\r\n");
            }
            header.append("</div>\r\n");
            header.append("<br>\r\n");

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

            List<Object> items = new ArrayList<>();
            Preferences prefs = Preferences.getPreferences(context.getApplicationContext());
            Collection<Account> accounts = prefs.getAvailableAccounts();
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
        } else if (recipientPresenter.isForceTextMessageFormat()) {
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

    /**
     * Extract the date from a message and convert it into a locale-specific
     * date string suitable for use in a header for a quoted message.
     *
     * @return A string with the formatted date/time
     */
    private String getSentDateText(Message message) {
        try {
            final int dateStyle = DateFormat.LONG;
            final int timeStyle = DateFormat.LONG;
            Date date = message.getSentDate();
            Locale locale = getResources().getConfiguration().locale;
            return DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale)
                    .format(date);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void onMessageBuildSuccess(MimeMessage message, boolean isDraft) {
        if (isDraft) {
            draftNeedsSaving = false;
            currentMessageBuilder = null;

            if (mAction == Action.EDIT_DRAFT && mMessageReference != null) {
                message.setUid(mMessageReference.getUid());
            }

            boolean saveRemotely = recipientPresenter.isAllowSavingDraftRemotely();
            new SaveMessageTask(getApplicationContext(), mAccount, mContacts, mHandler,
                    message, mDraftId, saveRemotely).execute();
            if (mFinishAfterDraftSaved) {
                finish();
            } else {
                setProgressBarIndeterminateVisibility(false);
            }
        } else {
            currentMessageBuilder = null;
            new SendMessageTask(getApplicationContext(), mAccount, mContacts, message,
                    mDraftId != INVALID_DRAFT_ID ? mDraftId : null, mMessageReference).execute();
            finish();
        }
    }

    @Override
    public void onMessageBuildCancel() {
        currentMessageBuilder = null;
        setProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onMessageBuildException(MessagingException me) {
        Log.e(K9.LOG_TAG, "Error sending message", me);
        Toast.makeText(MessageCompose.this,
                getString(R.string.send_failed_reason, me.getLocalizedMessage()), Toast.LENGTH_LONG).show();
        currentMessageBuilder = null;
        setProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onMessageBuildReturnPendingIntent(PendingIntent pendingIntent, int requestCode) {
        requestCode |= REQUEST_MASK_MESSAGE_BUILDER;
        try {
            startIntentSenderForResult(pendingIntent.getIntentSender(), requestCode, null, 0, 0, 0);
        } catch (SendIntentException e) {
            Log.e(K9.LOG_TAG, "Error starting pending intent from builder!", e);
        }
    }

    public void launchUserInteractionPendingIntent(PendingIntent pendingIntent, int requestCode) {
        requestCode |= REQUEST_MASK_RECIPIENT_PRESENTER;
        try {
            startIntentSenderForResult(pendingIntent.getIntentSender(), requestCode, null, 0, 0, 0);
        } catch (SendIntentException e) {
            e.printStackTrace();
        }
    }
}
