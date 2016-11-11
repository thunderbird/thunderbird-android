package com.fsck.k9.activity;


import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
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
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.Account.MessageFormat;
import com.fsck.k9.FontSizes;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageLoaderHelper.MessageLoaderCallbacks;
import com.fsck.k9.activity.compose.AttachmentPresenter;
import com.fsck.k9.activity.compose.AttachmentPresenter.AttachmentMvpView;
import com.fsck.k9.activity.compose.AttachmentPresenter.WaitingAction;
import com.fsck.k9.activity.compose.ComposeCryptoStatus;
import com.fsck.k9.activity.compose.ComposeCryptoStatus.SendErrorState;
import com.fsck.k9.activity.compose.CryptoSettingsDialog.OnCryptoModeChangedListener;
import com.fsck.k9.activity.compose.IdentityAdapter;
import com.fsck.k9.activity.compose.IdentityAdapter.IdentityContainer;
import com.fsck.k9.activity.compose.PgpInlineDialog.OnOpenPgpInlineChangeListener;
import com.fsck.k9.activity.compose.PgpSignOnlyDialog.OnOpenPgpSignOnlyChangeListener;
import com.fsck.k9.activity.compose.RecipientMvpView;
import com.fsck.k9.activity.compose.RecipientPresenter;
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoMode;
import com.fsck.k9.activity.compose.SaveMessageTask;
import com.fsck.k9.activity.misc.Attachment;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.fragment.ProgressDialogFragment;
import com.fsck.k9.fragment.ProgressDialogFragment.CancelListener;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.IdentityHelper;
import com.fsck.k9.helper.MailTo;
import com.fsck.k9.helper.ReplyToParser;
import com.fsck.k9.helper.SimpleTextWatcher;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.message.ComposePgpInlineDecider;
import com.fsck.k9.message.IdentityField;
import com.fsck.k9.message.IdentityHeaderParser;
import com.fsck.k9.message.MessageBuilder;
import com.fsck.k9.message.PgpMessageBuilder;
import com.fsck.k9.message.QuotedTextMode;
import com.fsck.k9.message.SimpleMessageBuilder;
import com.fsck.k9.message.SimpleMessageFormat;
import com.fsck.k9.ui.EolConvertingEditText;
import com.fsck.k9.ui.compose.QuotedMessageMvpView;
import com.fsck.k9.ui.compose.QuotedMessagePresenter;


@SuppressWarnings("deprecation")
public class MessageCompose extends K9Activity implements OnClickListener,
        CancelListener, OnFocusChangeListener, OnCryptoModeChangedListener,
        OnOpenPgpInlineChangeListener, OnOpenPgpSignOnlyChangeListener, MessageBuilder.Callback {

    private static final int DIALOG_SAVE_OR_DISCARD_DRAFT_MESSAGE = 1;
    private static final int DIALOG_CONFIRM_DISCARD_ON_BACK = 2;
    private static final int DIALOG_CHOOSE_IDENTITY = 3;
    private static final int DIALOG_CONFIRM_DISCARD = 4;

    private static final long INVALID_DRAFT_ID = MessagingController.INVALID_MESSAGE_ID;

    public static final String ACTION_COMPOSE = "com.fsck.k9.intent.action.COMPOSE";
    public static final String ACTION_REPLY = "com.fsck.k9.intent.action.REPLY";
    public static final String ACTION_REPLY_ALL = "com.fsck.k9.intent.action.REPLY_ALL";
    public static final String ACTION_FORWARD = "com.fsck.k9.intent.action.FORWARD";
    public static final String ACTION_EDIT_DRAFT = "com.fsck.k9.intent.action.EDIT_DRAFT";

    public static final String EXTRA_ACCOUNT = "account";
    public static final String EXTRA_MESSAGE_REFERENCE = "message_reference";
    public static final String EXTRA_MESSAGE_DECRYPTION_RESULT  = "message_decryption_result";

    private static final String STATE_KEY_SOURCE_MESSAGE_PROCED =
        "com.fsck.k9.activity.MessageCompose.stateKeySourceMessageProced";
    private static final String STATE_KEY_DRAFT_ID = "com.fsck.k9.activity.MessageCompose.draftId";
    private static final String STATE_IDENTITY_CHANGED =
        "com.fsck.k9.activity.MessageCompose.identityChanged";
    private static final String STATE_IDENTITY =
        "com.fsck.k9.activity.MessageCompose.identity";
    private static final String STATE_IN_REPLY_TO = "com.fsck.k9.activity.MessageCompose.inReplyTo";
    private static final String STATE_REFERENCES = "com.fsck.k9.activity.MessageCompose.references";
    private static final String STATE_KEY_READ_RECEIPT = "com.fsck.k9.activity.MessageCompose.messageReadReceipt";
    private static final String STATE_KEY_DRAFT_NEEDS_SAVING = "com.fsck.k9.activity.MessageCompose.draftNeedsSaving";
    private static final String STATE_ALREADY_NOTIFIED_USER_OF_EMPTY_SUBJECT = "alreadyNotifiedUserOfEmptySubject";

    private static final String FRAGMENT_WAITING_FOR_ATTACHMENT = "waitingForAttachment";

    private static final int MSG_PROGRESS_ON = 1;
    private static final int MSG_PROGRESS_OFF = 2;
    public static final int MSG_SAVED_DRAFT = 4;
    private static final int MSG_DISCARDED_DRAFT = 5;

    private static final int REQUEST_MASK_RECIPIENT_PRESENTER = (1<<8);
    private static final int REQUEST_MASK_LOADER_HELPER = (1<<9);
    private static final int REQUEST_MASK_ATTACHMENT_PRESENTER = (1<<10);
    private static final int REQUEST_MASK_MESSAGE_BUILDER = (1<<11);

    /**
     * Regular expression to remove the first localized "Re:" prefix in subjects.
     *
     * Currently:
     * - "Aw:" (german: abbreviation for "Antwort")
     */
    private static final Pattern PREFIX = Pattern.compile("^AW[:\\s]\\s*", Pattern.CASE_INSENSITIVE);

    private QuotedMessagePresenter quotedMessagePresenter;
    private MessageLoaderHelper messageLoaderHelper;
    private AttachmentPresenter attachmentPresenter;

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

    /**
     * Indicates that the source message has been processed at least once and should not
     * be processed on any subsequent loads. This protects us from adding attachments that
     * have already been added from the restore of the view state.
     */
    private boolean mSourceMessageProcessed = false;

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

    @Override
    public void onOpenPgpInlineChange(boolean enabled) {
        recipientPresenter.onCryptoPgpInlineChanged(enabled);
    }

    @Override
    public void onOpenPgpSignOnlyChange(boolean enabled) {
        recipientPresenter.onCryptoPgpSignOnlyDisabled();
    }

    public enum Action {
        COMPOSE(R.string.compose_title_compose),
        REPLY(R.string.compose_title_reply),
        REPLY_ALL(R.string.compose_title_reply_all),
        FORWARD(R.string.compose_title_forward),
        EDIT_DRAFT(R.string.compose_title_compose);

        private final int titleResource;

        Action(@StringRes int titleResource) {
            this.titleResource = titleResource;
        }

        @StringRes
        public int getTitleResource() {
            return titleResource;
        }
    }

    /**
     * Contains the action we're currently performing (e.g. replying to a message)
     */
    private Action mAction;

    private boolean mReadReceipt = false;

    private TextView mChooseIdentityButton;
    private EditText mSubjectView;
    private EolConvertingEditText mSignatureView;
    private EolConvertingEditText mMessageContentView;
    private LinearLayout mAttachments;

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

    private boolean draftNeedsSaving = false;
    private boolean isInSubActivity = false;

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
                    setProgressBarIndeterminateVisibility(true);
                    break;
                case MSG_PROGRESS_OFF:
                    setProgressBarIndeterminateVisibility(false);
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
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    private FontSizes mFontSizes = K9.getFontSizes();


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

        // on api level 15, setContentView() shows the progress bar for some reason...
        setProgressBarIndeterminateVisibility(false);

        final Intent intent = getIntent();

        mMessageReference = intent.getParcelableExtra(EXTRA_MESSAGE_REFERENCE);

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
        ComposePgpInlineDecider composePgpInlineDecider = new ComposePgpInlineDecider();
        recipientPresenter = new RecipientPresenter(getApplicationContext(), getLoaderManager(), recipientMvpView,
                mAccount, composePgpInlineDecider, new ReplyToParser());
        recipientPresenter.updateCryptoStatus();


        mSubjectView = (EditText) findViewById(R.id.subject);
        mSubjectView.getInputExtras(true).putBoolean("allowEmoji", true);

        EolConvertingEditText upperSignature = (EolConvertingEditText)findViewById(R.id.upper_signature);
        EolConvertingEditText lowerSignature = (EolConvertingEditText)findViewById(R.id.lower_signature);

        QuotedMessageMvpView quotedMessageMvpView = new QuotedMessageMvpView(this);
        quotedMessagePresenter = new QuotedMessagePresenter(this, quotedMessageMvpView, mAccount);
        attachmentPresenter = new AttachmentPresenter(getApplicationContext(), attachmentMvpView, getLoaderManager());

        mMessageContentView = (EolConvertingEditText)findViewById(R.id.message_content);
        mMessageContentView.getInputExtras(true).putBoolean("allowEmoji", true);

        mAttachments = (LinearLayout)findViewById(R.id.attachments);

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
        quotedMessageMvpView.addTextChangedListener(draftNeedsChangingTextWatcher);

        mSubjectView.addTextChangedListener(draftNeedsChangingTextWatcher);

        mMessageContentView.addTextChangedListener(draftNeedsChangingTextWatcher);

        /*
         * We set this to invisible by default. Other methods will turn it back on if it's
         * needed.
         */

        quotedMessagePresenter.showOrHideQuotedText(QuotedTextMode.NONE);

        mSubjectView.setOnFocusChangeListener(this);
        mMessageContentView.setOnFocusChangeListener(this);

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

        updateFrom();

        if (!mSourceMessageProcessed) {
            if (mAction == Action.REPLY || mAction == Action.REPLY_ALL ||
                    mAction == Action.FORWARD || mAction == Action.EDIT_DRAFT) {
                messageLoaderHelper = new MessageLoaderHelper(this, getLoaderManager(), getFragmentManager(),
                        messageLoaderCallbacks);
                mHandler.sendEmptyMessage(MSG_PROGRESS_ON);

                Parcelable cachedDecryptionResult = intent.getParcelableExtra(EXTRA_MESSAGE_DECRYPTION_RESULT);
                messageLoaderHelper.asyncStartOrResumeLoadingMessage(mMessageReference, cachedDecryptionResult);
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
        quotedMessageMvpView.setFontSizes(mFontSizes, fontSize);
        mFontSizes.setViewTextSize(mSubjectView, fontSize);
        mFontSizes.setViewTextSize(mMessageContentView, fontSize);
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
                    attachmentPresenter.addAttachment(stream, type);
                }
            } else {
                List<Parcelable> list = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (list != null) {
                    for (Parcelable parcelable : list) {
                        Uri stream = (Uri) parcelable;
                        if (stream != null) {
                            attachmentPresenter.addAttachment(stream, type);
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
    protected void onResume() {
        super.onResume();
        MessagingController.getInstance(this).addListener(messagingListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        MessagingController.getInstance(this).removeListener(messagingListener);

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

        outState.putBoolean(STATE_KEY_SOURCE_MESSAGE_PROCED, mSourceMessageProcessed);
        outState.putLong(STATE_KEY_DRAFT_ID, mDraftId);
        outState.putSerializable(STATE_IDENTITY, mIdentity);
        outState.putBoolean(STATE_IDENTITY_CHANGED, mIdentityChanged);
        outState.putString(STATE_IN_REPLY_TO, mInReplyTo);
        outState.putString(STATE_REFERENCES, mReferences);
        outState.putBoolean(STATE_KEY_READ_RECEIPT, mReadReceipt);
        outState.putBoolean(STATE_KEY_DRAFT_NEEDS_SAVING, draftNeedsSaving);
        outState.putBoolean(STATE_ALREADY_NOTIFIED_USER_OF_EMPTY_SUBJECT, alreadyNotifiedUserOfEmptySubject);

        recipientPresenter.onSaveInstanceState(outState);
        quotedMessagePresenter.onSaveInstanceState(outState);
        attachmentPresenter.onSaveInstanceState(outState);
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

        mReadReceipt = savedInstanceState.getBoolean(STATE_KEY_READ_RECEIPT);

        recipientPresenter.onRestoreInstanceState(savedInstanceState);
        quotedMessagePresenter.onRestoreInstanceState(savedInstanceState);
        attachmentPresenter.onRestoreInstanceState(savedInstanceState);

        mDraftId = savedInstanceState.getLong(STATE_KEY_DRAFT_ID);
        mIdentity = (Identity)savedInstanceState.getSerializable(STATE_IDENTITY);
        mIdentityChanged = savedInstanceState.getBoolean(STATE_IDENTITY_CHANGED);
        mInReplyTo = savedInstanceState.getString(STATE_IN_REPLY_TO);
        mReferences = savedInstanceState.getString(STATE_REFERENCES);
        draftNeedsSaving = savedInstanceState.getBoolean(STATE_KEY_DRAFT_NEEDS_SAVING);
        alreadyNotifiedUserOfEmptySubject = savedInstanceState.getBoolean(STATE_ALREADY_NOTIFIED_USER_OF_EMPTY_SUBJECT);

        updateFrom();

        updateMessageFormat();
    }

    private void setTitle() {
        setTitle(mAction.getTitleResource());
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

            PgpMessageBuilder pgpBuilder = PgpMessageBuilder.newInstance();
            recipientPresenter.builderSetProperties(pgpBuilder);
            builder = pgpBuilder;
        } else {
            builder = SimpleMessageBuilder.newInstance();
        }

        builder.setSubject(mSubjectView.getText().toString())
                .setSentDate(new Date())
                .setHideTimeZone(K9.hideTimeZone())
                .setTo(recipientPresenter.getToAddresses())
                .setCc(recipientPresenter.getCcAddresses())
                .setBcc(recipientPresenter.getBccAddresses())
                .setInReplyTo(mInReplyTo)
                .setReferences(mReferences)
                .setRequestReadReceipt(mReadReceipt)
                .setIdentity(mIdentity)
                .setMessageFormat(mMessageFormat)
                .setText(mMessageContentView.getCharacters())
                .setAttachments(attachmentPresenter.createAttachmentList())
                .setSignature(mSignatureView.getCharacters())
                .setSignatureBeforeQuotedText(mAccount.isSignatureBeforeQuotedText())
                .setIdentityChanged(mIdentityChanged)
                .setSignatureChanged(mSignatureChanged)
                .setCursorPosition(mMessageContentView.getSelectionStart())
                .setMessageReference(mMessageReference)
                .setDraft(isDraft)
                .setIsPgpInlineEnabled(cryptoStatus.isPgpInlineModeEnabled());

        quotedMessagePresenter.builderSetProperties(builder);

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

        if (attachmentPresenter.checkOkForSendingOrDraftSaving()) {
            return;
        }

        performSendAfterChecks();
    }

    private void checkToSaveDraftAndSave() {
        if (!mAccount.hasDraftsFolder()) {
            Toast.makeText(this, R.string.compose_error_no_draft_folder, Toast.LENGTH_SHORT).show();
            return;
        }

        if (attachmentPresenter.checkOkForSendingOrDraftSaving()) {
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
            currentMessageBuilder.onActivityResult(requestCode, resultCode, data, this);
            return;
        }

        if ((requestCode & REQUEST_MASK_RECIPIENT_PRESENTER) == REQUEST_MASK_RECIPIENT_PRESENTER) {
            requestCode ^= REQUEST_MASK_RECIPIENT_PRESENTER;
            recipientPresenter.onActivityResult(requestCode, resultCode, data);
            return;
        }

        if ((requestCode & REQUEST_MASK_LOADER_HELPER) == REQUEST_MASK_LOADER_HELPER) {
            requestCode ^= REQUEST_MASK_LOADER_HELPER;
            messageLoaderHelper.onActivityResult(requestCode, resultCode, data);
            return;
        }

        if ((requestCode & REQUEST_MASK_ATTACHMENT_PRESENTER) == REQUEST_MASK_ATTACHMENT_PRESENTER) {
            requestCode ^= REQUEST_MASK_ATTACHMENT_PRESENTER;
            attachmentPresenter.onActivityResult(resultCode, requestCode, data);
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
            quotedMessagePresenter.onSwitchAccount(mAccount);

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
            case R.id.identity:
                showDialog(DIALOG_CHOOSE_IDENTITY);
                break;
        }
    }

    private void askBeforeDiscard() {
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
            case R.id.openpgp_inline_enable:
                recipientPresenter.onMenuSetPgpInline(true);
                updateMessageFormat();
                break;
            case R.id.openpgp_inline_disable:
                recipientPresenter.onMenuSetPgpInline(false);
                updateMessageFormat();
                break;
            case R.id.openpgp_sign_only:
                recipientPresenter.onMenuSetSignOnly(true);
                break;
            case R.id.openpgp_sign_only_disable:
                recipientPresenter.onMenuSetSignOnly(false);
                break;
            case R.id.add_attachment:
                attachmentPresenter.onClickAddAttachment(recipientPresenter);
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

    public void onProgressCancel(ProgressDialogFragment fragment) {
        attachmentPresenter.attachmentProgressDialogCancelled();
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

    public void saveDraftEventually() {
        draftNeedsSaving = true;
    }

    public void loadQuotedTextForEdit() {
        if (mMessageReference == null) { // shouldn't happen...
            throw new IllegalStateException("tried to edit quoted message with no referenced message");
        }

        messageLoaderHelper.asyncStartOrResumeLoadingMessage(mMessageReference, null);
    }

    /**
     * Pull out the parts of the now loaded source message and apply them to the new message
     * depending on the type of message being composed.
     *
     * @param messageViewInfo
     *         The source message used to populate the various text fields.
     */
    private void processSourceMessage(MessageViewInfo messageViewInfo) {
        try {
            switch (mAction) {
                case REPLY:
                case REPLY_ALL: {
                    processMessageToReplyTo(messageViewInfo);
                    break;
                }
                case FORWARD: {
                    processMessageToForward(messageViewInfo);
                    break;
                }
                case EDIT_DRAFT: {
                    processDraftMessage(messageViewInfo);
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

    private void processMessageToReplyTo(MessageViewInfo messageViewInfo) throws MessagingException {
        Message message = messageViewInfo.message;

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
        boolean isReplyAll = mAction == Action.REPLY_ALL;
        recipientPresenter.initFromReplyToMessage(message, isReplyAll);

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
        quotedMessagePresenter.initFromReplyToMessage(messageViewInfo, mAction);

        if (mAction == Action.REPLY || mAction == Action.REPLY_ALL) {
            Identity useIdentity = IdentityHelper.getRecipientIdentityFromMessage(mAccount, message);
            Identity defaultIdentity = mAccount.getIdentity(0);
            if (useIdentity != defaultIdentity) {
                switchToIdentity(useIdentity);
            }
        }

    }

    private void processMessageToForward(MessageViewInfo messageViewInfo) throws MessagingException {
        Message message = messageViewInfo.message;

        String subject = message.getSubject();
        if (subject != null && !subject.toLowerCase(Locale.US).startsWith("fwd:")) {
            mSubjectView.setText("Fwd: " + subject);
        } else {
            mSubjectView.setText(subject);
        }

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
        quotedMessagePresenter.processMessageToForward(messageViewInfo);
        attachmentPresenter.processMessageToForward(messageViewInfo);
    }

    private void processDraftMessage(MessageViewInfo messageViewInfo) {
        Message message = messageViewInfo.message;
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
            attachmentPresenter.loadNonInlineAttachments(messageViewInfo);
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
            if (message instanceof LocalMessage) {
                newIdentity.setSignatureUse(((LocalMessage) message).getFolder().getSignatureUse());
            }
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

        mIdentity = newIdentity;

        updateSignature();
        updateFrom();

        quotedMessagePresenter.processDraftMessage(messageViewInfo, k9identity);
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
        if (body != null && !body.isEmpty()) {
            mMessageContentView.setCharacters(body);
        }
    }

    private void setMessageFormat(SimpleMessageFormat format) {
        // This method will later be used to enable/disable the rich text editing mode.

        mMessageFormat = format;
    }

    public void updateMessageFormat() {
        MessageFormat origMessageFormat = mAccount.getMessageFormat();
        SimpleMessageFormat messageFormat;
        if (origMessageFormat == MessageFormat.TEXT) {
            // The user wants to send text/plain messages. We don't override that choice under
            // any circumstances.
            messageFormat = SimpleMessageFormat.TEXT;
        } else if (quotedMessagePresenter.isForcePlainText()
                && quotedMessagePresenter.includeQuotedText()) {
            // Right now we send a text/plain-only message when the quoted text was edited, no
            // matter what the user selected for the message format.
            messageFormat = SimpleMessageFormat.TEXT;
        } else if (recipientPresenter.isForceTextMessageFormat()) {
            // Right now we only support PGP inline which doesn't play well with HTML. So force
            // plain text in those cases.
            messageFormat = SimpleMessageFormat.TEXT;
        } else if (origMessageFormat == MessageFormat.AUTO) {
            if (mAction == Action.COMPOSE || quotedMessagePresenter.isQuotedTextText() ||
                    !quotedMessagePresenter.includeQuotedText()) {
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

    public void loadLocalMessageForDisplay(MessageViewInfo messageViewInfo, Action action) {
        // We check to see if we've previously processed the source message since this
        // could be called when switching from HTML to text replies. If that happens, we
        // only want to update the UI with quoted text (which picks the appropriate
        // part).
        if (mSourceMessageProcessed) {
            try {
                quotedMessagePresenter.populateUIWithQuotedMessage(messageViewInfo, true, action);
            } catch (MessagingException e) {
                // Hm, if we couldn't populate the UI after source reprocessing, let's just delete it?
                quotedMessagePresenter.showOrHideQuotedText(QuotedTextMode.HIDE);
                Log.e(K9.LOG_TAG, "Could not re-process source message; deleting quoted text to be safe.", e);
            }
            updateMessageFormat();
        } else {
            processSourceMessage(messageViewInfo);
            mSourceMessageProcessed = true;
        }
    }

    private MessageLoaderCallbacks messageLoaderCallbacks = new MessageLoaderCallbacks() {
        @Override
        public void onMessageDataLoadFinished(LocalMessage message) {
            // nothing to do here, we don't care about message headers
        }

        @Override
        public void onMessageDataLoadFailed() {
            mHandler.sendEmptyMessage(MSG_PROGRESS_OFF);
            Toast.makeText(MessageCompose.this, R.string.status_invalid_id_error, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onMessageViewInfoLoadFinished(MessageViewInfo messageViewInfo) {
            mHandler.sendEmptyMessage(MSG_PROGRESS_OFF);
            loadLocalMessageForDisplay(messageViewInfo, mAction);
        }

        @Override
        public void onMessageViewInfoLoadFailed(MessageViewInfo messageViewInfo) {
            mHandler.sendEmptyMessage(MSG_PROGRESS_OFF);
            Toast.makeText(MessageCompose.this, R.string.status_invalid_id_error, Toast.LENGTH_LONG).show();
        }

        @Override
        public void setLoadingProgress(int current, int max) {
            // nvm - we don't have a progress bar
        }

        @Override
        public void startIntentSenderForMessageLoaderHelper(IntentSender si, int requestCode, Intent fillIntent,
                int flagsMask, int flagValues, int extraFlags) {
            try {
                requestCode |= REQUEST_MASK_LOADER_HELPER;
                startIntentSenderForResult(si, requestCode, fillIntent, flagsMask, flagValues, extraFlags);
            } catch (SendIntentException e) {
                Log.e(K9.LOG_TAG, "Irrecoverable error calling PendingIntent!", e);
            }
        }

        @Override
        public void onDownloadErrorMessageNotFound() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MessageCompose.this, R.string.status_invalid_id_error, Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onDownloadErrorNetworkError() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MessageCompose.this, R.string.status_network_error, Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    // TODO We miss callbacks for this listener if they happens while we are paused!
    public MessagingListener messagingListener = new MessagingListener() {

        @Override
        public void messageUidChanged(Account account, String folder, String oldUid, String newUid) {
            if (mMessageReference == null) {
                return;
            }

            Account sourceAccount = Preferences.getPreferences(MessageCompose.this)
                    .getAccount(mMessageReference.getAccountUuid());
            String sourceFolder = mMessageReference.getFolderName();
            String sourceMessageUid = mMessageReference.getUid();

            boolean changedMessageIsCurrent =
                    account.equals(sourceAccount) && folder.equals(sourceFolder) && oldUid.equals(sourceMessageUid);
            if (changedMessageIsCurrent) {
                mMessageReference = mMessageReference.withModifiedUid(newUid);
            }
        }

    };

    AttachmentMvpView attachmentMvpView = new AttachmentMvpView() {
        private HashMap<Uri,View> attachmentViews = new HashMap<>();

        @Override
        public void showWaitingForAttachmentDialog(WaitingAction waitingAction) {
            String title;

            switch (waitingAction) {
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

        @Override
        public void dismissWaitingForAttachmentDialog() {
            ProgressDialogFragment fragment = (ProgressDialogFragment)
                    getFragmentManager().findFragmentByTag(FRAGMENT_WAITING_FOR_ATTACHMENT);

            if (fragment != null) {
                fragment.dismiss();
            }
        }

        @Override
        @SuppressLint("InlinedApi")
        public void showPickAttachmentDialog(int requestCode) {
            requestCode |= REQUEST_MASK_ATTACHMENT_PRESENTER;

            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            isInSubActivity = true;

            startActivityForResult(Intent.createChooser(i, null), requestCode);
        }

        @Override
        public void addAttachmentView(final Attachment attachment) {
            View view = getLayoutInflater().inflate(R.layout.message_compose_attachment, mAttachments, false);
            attachmentViews.put(attachment.uri, view);

            View deleteButton = view.findViewById(R.id.attachment_delete);
            deleteButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    attachmentPresenter.onClickRemoveAttachment(attachment.uri);
                }
            });

            updateAttachmentView(attachment);
            mAttachments.addView(view);
        }

        @Override
        public void updateAttachmentView(Attachment attachment) {
            View view = attachmentViews.get(attachment.uri);
            if (view == null) {
                throw new IllegalArgumentException();
            }

            TextView nameView = (TextView) view.findViewById(R.id.attachment_name);
            boolean hasMetadata = (attachment.state != Attachment.LoadingState.URI_ONLY);
            if (hasMetadata) {
                nameView.setText(attachment.name);
            } else {
                nameView.setText(R.string.loading_attachment);
            }

            View progressBar = view.findViewById(R.id.progressBar);
            boolean isLoadingComplete = (attachment.state == Attachment.LoadingState.COMPLETE);
            progressBar.setVisibility(isLoadingComplete ? View.GONE : View.VISIBLE);
        }

        @Override
        public void removeAttachmentView(Attachment attachment) {
            View view = attachmentViews.get(attachment.uri);
            mAttachments.removeView(view);
            attachmentViews.remove(attachment.uri);
        }

        @Override
        public void performSendAfterChecks() {
            MessageCompose.this.performSendAfterChecks();
        }

        @Override
        public void performSaveAfterChecks() {
            MessageCompose.this.performSaveAfterChecks();
        }

        @Override
        public void showMissingAttachmentsPartialMessageWarning() {
            Toast.makeText(MessageCompose.this,
                    getString(R.string.message_compose_attachments_skipped_toast), Toast.LENGTH_LONG).show();
        }
    };

}
