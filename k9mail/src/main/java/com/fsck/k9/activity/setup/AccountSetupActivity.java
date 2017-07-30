package com.fsck.k9.activity.setup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.fsck.k9.R;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.setup.AccountSetupPresenter.Stage;
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;

import android.support.annotation.StringRes;

import com.fsck.k9.Account;

import java.security.cert.X509Certificate;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.fsck.k9.fragment.ConfirmationDialogFragment;
import com.fsck.k9.view.ClientCertificateSpinner;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import timber.log.Timber;

import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.view.ClientCertificateSpinner.OnClientCertificateChangedListener;

import static com.fsck.k9.mail.ServerSettings.Type.IMAP;
import static com.fsck.k9.mail.ServerSettings.Type.POP3;


public class AccountSetupActivity extends AppCompatActivity implements AccountSetupContract.View,
        ConfirmationDialogFragmentListener, OnClickListener, OnCheckedChangeListener {

    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_STAGE = "stage";
    private static final String EXTRA_EDIT_SETTINGS = "edit_settings";
    private static final String STATE_STAGE = "state_stage";
    private static final String STATE_ACCOUNT = "state_account";
    private static final String STATE_EDIT_SETTINGS = "state_edit_settings";
    private static final String TAG = "Accou";

    private boolean canceled;
    private boolean destroyed;

    private AccountSetupPresenter presenter;

    private TextView messageView;
    private Handler handler;

    private TextView serverLabelView;
    private EditText usernameView;
    private EditText passwordView;
    private ClientCertificateSpinner clientCertificateSpinner;
    private TextView clientCertificateLabelView;
    private TextView passwordLabelView;
    private EditText serverView;
    private EditText portView;
    private Spinner securityTypeView;
    private Spinner authTypeView;
    private CheckBox imapAutoDetectNamespaceView;
    private EditText imapPathPrefixView;
    private EditText webdavPathPrefixView;
    private EditText webdavAuthPathView;
    private EditText webdavMailboxPathView;
    private Button nextButton;
    private CheckBox compressionMobile;
    private CheckBox compressionWifi;
    private CheckBox compressionOther;
    private CheckBox subscribedFoldersOnly;
    private AuthTypeAdapter authTypeAdapter;

    private CheckBox requireLoginView;
    private ViewGroup requireLoginSettingsView;

    private Spinner checkFrequencyView;

    private Spinner displayCountView;

    private CheckBox notifyView;
    private CheckBox notifySyncView;
    private CheckBox pushEnable;

    private EditText description;
    private EditText name;
    private Button doneButton;

    private ViewFlipper flipper;

    private int position;

    int[] layoutIds = new int[]{R.layout.account_setup_basics,
            R.layout.account_setup_check_settings, R.layout.account_setup_account_type,
            R.layout.account_setup_incoming, R.layout.account_setup_outgoing,
            R.layout.account_setup_options, R.layout.account_setup_names};
    private EditText emailView;
    private Button manualSetupButton;

    private MaterialProgressBar progressBar;

    boolean editSettings;

    Stage stage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.account_setup);

        flipper = (ViewFlipper) findViewById(R.id.view_flipper);

        presenter = new AccountSetupPresenter(this);

        Intent intent = getIntent();

        stage = (Stage) intent.getSerializableExtra(EXTRA_STAGE);
        String accountUuid = intent.getStringExtra(EXTRA_ACCOUNT);
        editSettings = intent.getBooleanExtra(EXTRA_EDIT_SETTINGS, false);

        if (savedInstanceState != null) {
            stage = (Stage) savedInstanceState.getSerializable(STATE_STAGE);
            accountUuid = savedInstanceState.getString(STATE_ACCOUNT, accountUuid);
            editSettings = savedInstanceState.getBoolean(STATE_EDIT_SETTINGS, editSettings);
        }

        if (stage == null) {
            stage = Stage.BASICS;
        }

        if (accountUuid != null) {
            presenter.onGetAccountUuid(accountUuid);
        }

        switch (stage) {
            case BASICS:
                goToBasics();
                break;
            case ACCOUNT_TYPE:
                goToAccountType();
                break;
            case AUTOCONFIGURATION:
            case AUTOCONFIGURATION_INCOMING_CHECKING:
            case AUTOCONFIGURATION_OUTGOING_CHECKING:
                goToAutoConfiguration();
                break;
            case INCOMING:
                goToIncoming();
                break;
            case INCOMING_CHECKING:
                goToIncomingChecking();
                break;
            case OUTGOING:
                goToOutgoing();
                break;
            case OUTGOING_CHECKING:
                goToOutgoingChecking();
                break;
            case ACCOUNT_OPTIONS:
                goToOptions();
                break;
            case ACCOUNT_NAMES:
                goToAccountNames();
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(STATE_STAGE, stage);
        outState.putString(STATE_ACCOUNT, presenter.getAccount().getUuid());
        outState.putBoolean(STATE_EDIT_SETTINGS, presenter.isEditSettings());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        AdapterView.OnItemSelectedListener authTypeSelectedListener = null;
        AdapterView.OnItemSelectedListener securityTypeSelectedListener = null;
        if (authTypeView != null) {
            authTypeSelectedListener = authTypeView.getOnItemSelectedListener();
            authTypeView.setOnItemSelectedListener(null);
        }
        if (securityTypeView != null) {
            securityTypeSelectedListener = securityTypeView.getOnItemSelectedListener();
            securityTypeView.setOnItemSelectedListener(null);
        }

        super.onRestoreInstanceState(savedInstanceState);

        if (authTypeView != null) {
            authTypeView.setOnItemSelectedListener(authTypeSelectedListener);
        }
        if (securityTypeView != null) {
            securityTypeView.setOnItemSelectedListener(securityTypeSelectedListener);
        }
    }

    private void basicsStart() {
        emailView = (EditText) findViewById(R.id.account_email);
        passwordView = (EditText) findViewById(R.id.account_password);
        manualSetupButton = (Button) findViewById(R.id.manual_setup);
        nextButton = (Button) findViewById(R.id.basics_next);
        nextButton.setOnClickListener(this);
        manualSetupButton.setOnClickListener(this);

        initializeViewListenersInBasics();

        presenter.onBasicsStart();
        onInputChangedInBasics();
    }

    private void onInputChangedInBasics() {
        if (presenter == null) return;

        presenter.onInputChangedInBasics(emailView.getText().toString(), passwordView.getText().toString());
    }

    private TextWatcher validationTextWatcherInBasics = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            presenter.onInputChangedInBasics(emailView.getText().toString(), passwordView.getText().toString());
        }
    };

    private void initializeViewListenersInBasics() {
        emailView.addTextChangedListener(validationTextWatcherInBasics);
        passwordView.addTextChangedListener(validationTextWatcherInBasics);
    }

    private void checkingStart() {
        messageView = (TextView) findViewById(R.id.message);
        progressBar = (MaterialProgressBar) findViewById(R.id.progress);

        progressBar.setIndeterminate(true);

        handler = new Handler(Looper.getMainLooper());
    }

    private void accountTypeStart() {
        findViewById(R.id.pop).setOnClickListener(this);
        findViewById(R.id.imap).setOnClickListener(this);
        findViewById(R.id.webdav).setOnClickListener(this);
    }

    private int getPositionFromLayoutId(@LayoutRes int layoutId) {
        for (int i = 0; i < layoutIds.length; i++) {
            if (layoutIds[i] == layoutId) {
                return i;
            }
        }
        return -1;
    }


    public static void actionNewAccount(Context context) {
        Intent i = new Intent(context, AccountSetupActivity.class);
        context.startActivity(i);
    }

    public void goToNext() {
        setSelection(position + 1);
    }


    public void goToPrevious() {
        setSelection(position - 1);
    }


    public void goToBasics() {
        stage = Stage.BASICS;
        setSelection(getPositionFromLayoutId(R.layout.account_setup_basics));
        basicsStart();
    }


    public void goToOutgoing() {
        stage = Stage.OUTGOING;
        setSelection(getPositionFromLayoutId(R.layout.account_setup_outgoing));
        outgoingStart();
    }


    @Override
    public void goToIncoming() {
        stage = Stage.INCOMING;
        setSelection(getPositionFromLayoutId(R.layout.account_setup_incoming));
        incomingStart();
    }


    @Override
    public void goToAutoConfiguration() {
        stage = Stage.AUTOCONFIGURATION;
        setSelection(getPositionFromLayoutId(R.layout.account_setup_check_settings));
        checkingStart();
        presenter.onCheckingStart(Stage.AUTOCONFIGURATION);
    }


    @Override
    public void goToAccountType() {
        stage = Stage.ACCOUNT_TYPE;
        setSelection(getPositionFromLayoutId(R.layout.account_setup_account_type));
        accountTypeStart();
    }


    @Override
    public void goToOptions() {
        stage = Stage.ACCOUNT_OPTIONS;
        setSelection(getPositionFromLayoutId(R.layout.account_setup_options));
        optionsStart();
    }


    @Override
    public void goToAccountNames() {
        stage = Stage.ACCOUNT_NAMES;
        setSelection(getPositionFromLayoutId(R.layout.account_setup_names));
        namesStart();
    }

    @Override
    public void goToOutgoingChecking() {
        stage = Stage.OUTGOING_CHECKING;
        setSelection(getPositionFromLayoutId(R.layout.account_setup_check_settings));
        checkingStart();
        presenter.onCheckingStart(stage);
    }

    @Override
    public void end() {
        finish();
    }

    @Override
    public void goToIncomingChecking() {
        stage = Stage.INCOMING_CHECKING;
        setSelection(getPositionFromLayoutId(R.layout.account_setup_check_settings));
        checkingStart();
        presenter.onCheckingStart(stage);
    }

    public void listAccounts() {
        Accounts.listAccounts(this);
    }

    private void setSelection(int position) {
        if (position == -1) return;

        this.position = position;
        flipper.setDisplayedChild(position);

    }


    @Override
    public void showAcceptKeyDialog(final int msgResId, final String exMessage, final String message,
            final X509Certificate certificate) {

        handler.post(new Runnable() {
            @Override
            public void run() {
                // TODO: refactor with DialogFragment.
                // This is difficult because we need to pass through chain[0] for onClick()
                new AlertDialog.Builder(AccountSetupActivity.this)
                        .setTitle(getString(R.string.account_setup_failed_dlg_invalid_certificate_title))
                        .setMessage(getString(msgResId, exMessage)
                                + " " + message
                        )
                        .setCancelable(true)
                        .setPositiveButton(
                                getString(R.string.account_setup_failed_dlg_invalid_certificate_accept),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        presenter.onCertificateAccepted(certificate);
                                    }
                                })
                        .setNegativeButton(
                                getString(R.string.account_setup_failed_dlg_invalid_certificate_reject),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        presenter.onCertificateRefused();
                                    }
                                })
                        .show();
            }
        });
    }

    @Override
    public void showErrorDialog(@StringRes final int msgResId, final Object... args) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                showDialogFragment(R.id.dialog_account_setup_error, getString(msgResId, args));
            }
        });
    }

    @Override
    public boolean canceled() {
        return canceled;
    }

    @Override
    public void setMessage(@StringRes int id) {
        messageView.setText(getString(id));
    }

    @Override
    public Context getContext() {
        return this;
    }

    private void showDialogFragment(int dialogId, String customMessage) {
        if (destroyed) {
            return;
        }

        DialogFragment fragment;
        switch (dialogId) {
            case R.id.dialog_account_setup_error: {
                fragment = ConfirmationDialogFragment.newInstance(dialogId,
                        getString(R.string.account_setup_failed_dlg_title),
                        customMessage,
                        getString(R.string.account_setup_failed_dlg_edit_details_action),
                        getString(R.string.account_setup_failed_dlg_continue_action),
                        this
                );
                break;
            }
            default: {
                throw new RuntimeException("Called showDialog(int) with unknown dialog id.");
            }
        }

        FragmentTransaction ta = getFragmentManager().beginTransaction();
        ta.add(fragment, getDialogTag(dialogId));
        ta.commitAllowingStateLoss();

        // TODO: commitAllowingStateLoss() is used to prevent https://code.google.com/p/android/issues/detail?id=23761
        // but is a bad...
        //fragment.show(ta, getDialogTag(dialogId));
    }

    private String getDialogTag(int dialogId) {
        return String.format(Locale.US, "dialog-%d", dialogId);
    }

    @Override
    public void doPositiveClick(int dialogId) {
        presenter.onPositiveClickedInConfirmationDialog();
    }

    @Override
    public void doNegativeClick(int dialogId) {
        presenter.onNegativeClickedInConfirmationDialog();
    }

    @Override
    public void dialogCancelled(int dialogId) {

    }

    // ------

    private void initializeViewListenersInIncoming() {
        securityTypeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position,
                    long id) {

                onInputChangedInIncoming();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* unused */ }
        });

        authTypeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position,
                    long id) {

                onInputChangedInIncoming();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* unused */ }
        });

        clientCertificateSpinner.setOnClientCertificateChangedListener(clientCertificateChangedListenerInIncoming);
        usernameView.addTextChangedListener(validationTextWatcherInIncoming);
        passwordView.addTextChangedListener(validationTextWatcherInIncoming);
        serverView.addTextChangedListener(validationTextWatcherInIncoming);
        portView.addTextChangedListener(validationTextWatcherInIncoming);
    }

    private void onInputChangedInIncoming() {
        if (presenter == null) return;

        final AuthType selectedAuthType = getSelectedAuthType();
        final ConnectionSecurity selectedSecurity = getSelectedSecurity();
        if (selectedAuthType == null || selectedSecurity == null) return;

        presenter.onInputChangedInIncoming(clientCertificateSpinner.getAlias(),
                serverView.getText().toString(),
                portView.getText().toString(), usernameView.getText().toString(),
                passwordView.getText().toString(), selectedAuthType, selectedSecurity);

    }


    protected void onNextInIncoming() {
        try {
            ConnectionSecurity connectionSecurity = getSelectedSecurity();

            String username = usernameView.getText().toString();
            String password = passwordView.getText().toString();
            String clientCertificateAlias = clientCertificateSpinner.getAlias();
            boolean autoDetectNamespace = imapAutoDetectNamespaceView.isChecked();
            String imapPathPrefix = imapPathPrefixView.getText().toString();
            String webdavPathPrefix = webdavPathPrefixView.getText().toString();
            String webdavAuthPath = webdavAuthPathView.getText().toString();
            String webdavMailboxPath = webdavMailboxPathView.getText().toString();

            AuthType authType = getSelectedAuthType();

            String host = serverView.getText().toString();
            int port = Integer.parseInt(portView.getText().toString());

            boolean compressMobile = compressionMobile.isChecked();
            boolean compressWifi = compressionWifi.isChecked();
            boolean compressOther = compressionOther.isChecked();
            boolean subscribeFoldersOnly = subscribedFoldersOnly.isChecked();

            presenter.onNextInIncomingClicked(username, password, clientCertificateAlias, autoDetectNamespace,
                    imapPathPrefix, webdavPathPrefix, webdavAuthPath, webdavMailboxPath, host, port,
                    connectionSecurity, authType, compressMobile, compressWifi, compressOther,
                    subscribeFoldersOnly);

            goToIncomingChecking();

        } catch (Exception e) {
            failure(e);
        }
    }

    public void onClick(View v) {
        try {
            switch (v.getId()) {
                case R.id.basics_next:
                    presenter.onNextButtonInBasicViewClicked(emailView.getText().toString(),
                            passwordView.getText().toString());
                    break;
                case R.id.incoming_next:
                    onNextInIncoming();
                    break;
                case R.id.outgoing_next:
                    onNextInOutgoing();
                    break;
                case R.id.options_next:
                    presenter.onNextButtonInOptionsClicked(notifyView.isChecked(), notifySyncView.isChecked(),
                            (int)((SpinnerOption) checkFrequencyView .getSelectedItem()).value,
                            (int)((SpinnerOption) displayCountView .getSelectedItem()).value,
                            pushEnable.isChecked());
                    break;
                case R.id.done:
                    presenter.onNextButtonInNamesClicked(name.getText().toString(), description.getText().toString());
                    break;

                case R.id.pop:
                    presenter.onImapOrPop3Selected(POP3, "pop3+ssl+");
                    break;
                case R.id.imap:
                    presenter.onImapOrPop3Selected(IMAP, "imap+ssl+");
                    break;
                case R.id.webdav:
                    presenter.onWebdavSelected();
                    break;

                case R.id.manual_setup:
                    presenter.onManualSetupButtonClicked(emailView.getText().toString(),
                            passwordView.getText().toString());
                    break;
            }
        } catch (Exception e) {
            failure(e);
        }
    }

    private void failure(Exception use) {
        Timber.e(use, "Failure");
        String toastText = getString(R.string.account_setup_bad_uri, use.getMessage());

        Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_LONG);
        toast.show();
    }


    private TextWatcher validationTextWatcherInIncoming = new TextWatcher() {
        public void afterTextChanged(Editable s) {
            onInputChangedInIncoming();
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    };

    private OnClientCertificateChangedListener clientCertificateChangedListenerInIncoming = new OnClientCertificateChangedListener() {
        @Override
        public void onClientCertificateChanged(String alias) {
            onInputChangedInIncoming();
        }
    };

    @Override
    public void setAuthType(AuthType authType) {
        OnItemSelectedListener onItemSelectedListener = authTypeView.getOnItemSelectedListener();
        authTypeView.setOnItemSelectedListener(null);
        authTypeView.setSelection(authTypeAdapter.getAuthPosition(authType), false);
        authTypeView.setOnItemSelectedListener(onItemSelectedListener);
    }

    @Override
    public void setSecurityType(ConnectionSecurity security) {
        OnItemSelectedListener onItemSelectedListener = securityTypeView.getOnItemSelectedListener();
        securityTypeView.setOnItemSelectedListener(null);
        securityTypeView.setSelection(security.ordinal(), false);
        securityTypeView.setOnItemSelectedListener(onItemSelectedListener);
    }

    @Override
    public void setUsernameInIncoming(String username) {
        usernameView.removeTextChangedListener(validationTextWatcherInIncoming);
        usernameView.setText(username);
        usernameView.addTextChangedListener(validationTextWatcherInIncoming);
    }

    @Override
    public void setPasswordInIncoming(String password) {
        passwordView.removeTextChangedListener(validationTextWatcherInIncoming);
        passwordView.setText(password);
        passwordView.addTextChangedListener(validationTextWatcherInIncoming);
    }

    @Override
    public void setCertificateAliasInIncoming(String alias) {
        clientCertificateSpinner.setOnClientCertificateChangedListener(null);
        clientCertificateSpinner.setAlias(alias);
        clientCertificateSpinner.
                setOnClientCertificateChangedListener(clientCertificateChangedListenerInIncoming);
    }

    @Override
    public void setServerInIncoming(String server) {
        serverView.removeTextChangedListener(validationTextWatcherInIncoming);
        serverView.setText(server);
        serverView.addTextChangedListener(validationTextWatcherInIncoming);
    }

    @Override
    public void setPortInIncoming(String port) {
        portView.removeTextChangedListener(validationTextWatcherInIncoming);
        portView.setText(port);
        portView.addTextChangedListener(validationTextWatcherInIncoming);
    }

    @Override
    public void setServerLabel(String label) {
        serverLabelView.setText(label);
    }

    @Override
    public void hideViewsWhenPop3() {
        findViewById(R.id.imap_path_prefix_section).setVisibility(View.GONE);
        findViewById(R.id.webdav_advanced_header).setVisibility(View.GONE);
        findViewById(R.id.webdav_mailbox_alias_section).setVisibility(View.GONE);
        findViewById(R.id.webdav_owa_path_section).setVisibility(View.GONE);
        findViewById(R.id.webdav_auth_path_section).setVisibility(View.GONE);
        findViewById(R.id.compression_section).setVisibility(View.GONE);
        findViewById(R.id.compression_label).setVisibility(View.GONE);
        subscribedFoldersOnly.setVisibility(View.GONE);
    }

    @Override
    public void hideViewsWhenImap() {
        findViewById(R.id.webdav_advanced_header).setVisibility(View.GONE);
        findViewById(R.id.webdav_mailbox_alias_section).setVisibility(View.GONE);
        findViewById(R.id.webdav_owa_path_section).setVisibility(View.GONE);
        findViewById(R.id.webdav_auth_path_section).setVisibility(View.GONE);
    }

    @Override
    public void hideViewsWhenImapAndNotEdit() {
        findViewById(R.id.imap_folder_setup_section).setVisibility(View.GONE);
    }

    @Override
    public void hideViewsWhenWebDav() {
        findViewById(R.id.imap_path_prefix_section).setVisibility(View.GONE);
        findViewById(R.id.account_auth_type_label).setVisibility(View.GONE);
        findViewById(R.id.incoming_account_auth_type).setVisibility(View.GONE);
        findViewById(R.id.compression_section).setVisibility(View.GONE);
        findViewById(R.id.compression_label).setVisibility(View.GONE);
        subscribedFoldersOnly.setVisibility(View.GONE);
    }

    @Override
    public void setImapAutoDetectNamespace(boolean autoDetectNamespace) {
        imapAutoDetectNamespaceView.setChecked(autoDetectNamespace);
    }

    @Override
    public void setImapPathPrefix(String imapPathPrefix) {
        imapPathPrefixView.setText(imapPathPrefix);
    }

    @Override
    public void setWebDavPathPrefix(String webDavPathPrefix) {
        webdavPathPrefixView.setText(webDavPathPrefix);
    }

    @Override
    public void setWebDavAuthPath(String authPath) {
        webdavAuthPathView.setText(authPath);
    }

    @Override
    public void setWebDavMailboxPath(String mailboxPath) {
        webdavMailboxPathView.setText(mailboxPath);
    }


    private void incomingStart() {
        View incomingView = findViewById(R.id.account_setup_incoming);
        usernameView = (EditText) incomingView.findViewById(R.id.incoming_account_username);
        passwordView = (EditText) incomingView.findViewById(R.id.incoming_account_password);
        clientCertificateSpinner = (ClientCertificateSpinner) incomingView.findViewById(R.id.incoming_account_client_certificate_spinner);
        clientCertificateLabelView = (TextView) incomingView.findViewById(R.id.account_client_certificate_label);
        passwordLabelView = (TextView) incomingView.findViewById(R.id.account_password_label);
        serverLabelView = (TextView)  incomingView.findViewById(R.id.account_server_label);
        serverView = (EditText) incomingView.findViewById(R.id.incoming_account_server);
        portView = (EditText) incomingView.findViewById(R.id.incoming_account_port);
        securityTypeView = (Spinner) incomingView.findViewById(R.id.incoming_account_security_type);
        authTypeView = (Spinner) incomingView.findViewById(R.id.incoming_account_auth_type);
        imapAutoDetectNamespaceView = (CheckBox) incomingView.findViewById(R.id.imap_autodetect_namespace);
        imapPathPrefixView = (EditText) incomingView.findViewById(R.id.imap_path_prefix);
        webdavPathPrefixView = (EditText) incomingView.findViewById(R.id.webdav_path_prefix);
        webdavAuthPathView = (EditText) incomingView.findViewById(R.id.webdav_auth_path);
        webdavMailboxPathView = (EditText) incomingView.findViewById(R.id.webdav_mailbox_path);
        nextButton = (Button) incomingView.findViewById(R.id.incoming_next);
        compressionMobile = (CheckBox) incomingView.findViewById(R.id.compression_mobile);
        compressionWifi = (CheckBox) incomingView.findViewById(R.id.compression_wifi);
        compressionOther = (CheckBox) incomingView.findViewById(R.id.compression_other);
        subscribedFoldersOnly = (CheckBox) incomingView.findViewById(R.id.subscribed_folders_only);

        nextButton.setOnClickListener(this);

        imapAutoDetectNamespaceView.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                imapPathPrefixView.setEnabled(!isChecked);
                if (isChecked && imapPathPrefixView.hasFocus()) {
                    imapPathPrefixView.focusSearch(View.FOCUS_UP).requestFocus();
                } else if (!isChecked) {
                    imapPathPrefixView.requestFocus();
                }
            }
        });

        authTypeAdapter = AuthTypeAdapter.get(this);
        authTypeView.setAdapter(authTypeAdapter);

        portView.setKeyListener(DigitsKeyListener.getInstance("0123456789"));

        initializeViewListenersInIncoming();

        boolean editSettings = false;
        if (getIntent().getAction() != null) {
            editSettings = getIntent().getAction().equals(Intent.ACTION_EDIT);
        }
        presenter.onIncomingStart(editSettings);
    }


    @Override
    public void setNextButtonInIncomingEnabled(boolean enabled) {
        nextButton.setEnabled(enabled);
    }

    @Override
    public void goToIncomingSettings() {
        goToIncoming();
    }

    @Override
    public void setNextButtonInBasicsEnabled(boolean enabled) {
        nextButton.setEnabled(enabled);
        Utility.setCompoundDrawablesAlpha(nextButton, nextButton.isEnabled() ? 255 : 128);
    }

    @Override
    public void setSecurityChoices(ConnectionSecurity[] choices) {
        // Note that connectionSecurityChoices is configured above based on server type
        ConnectionSecurityAdapter securityTypesAdapter =
                ConnectionSecurityAdapter.get(this, choices);
        securityTypeView.setAdapter(securityTypesAdapter);
    }

    @Override
    public void setAuthTypeInsecureText(boolean insecure) {
        authTypeAdapter.useInsecureText(insecure);
    }

    @Override
    public void setViewNotExternalInIncoming() {
        passwordView.setVisibility(View.VISIBLE);
        passwordLabelView.setVisibility(View.VISIBLE);
        clientCertificateLabelView.setVisibility(View.GONE);
        clientCertificateSpinner.setVisibility(View.GONE);

        passwordView.requestFocus();
    }

    @Override
    public void setViewExternalInIncoming() {
        passwordView.setVisibility(View.GONE);
        passwordLabelView.setVisibility(View.GONE);
        clientCertificateLabelView.setVisibility(View.VISIBLE);
        clientCertificateSpinner.setVisibility(View.VISIBLE);

        clientCertificateSpinner.chooseCertificate();
    }

    @Override
    public void showFailureToast(Exception use) {
        failure(use);
    }

    @Override
    public void setCompressionMobile(boolean compressionMobileBoolean) {
        compressionMobile.setChecked(compressionMobileBoolean);
    }

    @Override
    public void setCompressionWifi(boolean compressionWifiBoolean) {
        compressionWifi.setChecked(compressionWifiBoolean);
    }

    @Override
    public void setCompressionOther(boolean compressionOtherBoolean) {
        compressionOther.setChecked(compressionOtherBoolean);
    }

    @Override
    public void setSubscribedFoldersOnly(boolean subscribedFoldersOnlyBoolean) {
        subscribedFoldersOnly.setChecked(subscribedFoldersOnlyBoolean);
    }

    @Override
    public void showInvalidSettingsToast() {
        String toastText = getString(R.string.account_setup_outgoing_invalid_setting_combo_notice,
                getString(R.string.account_setup_incoming_auth_type_label),
                AuthType.EXTERNAL.toString(),
                getString(R.string.account_setup_incoming_security_label),
                ConnectionSecurity.NONE.toString());
        Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
    }

    // names

    public void namesStart() {
        description = (EditText) findViewById(R.id.account_description);
        name = (EditText) findViewById(R.id.account_name);
        doneButton = (Button) findViewById(R.id.done);
        doneButton.setOnClickListener(this);

        presenter.onNamesStart();
    }

    // options

    public void optionsStart() {
        checkFrequencyView = (Spinner) findViewById(R.id.account_check_frequency);
        displayCountView = (Spinner) findViewById(R.id.account_display_count);
        notifyView = (CheckBox) findViewById(R.id.account_notify);
        notifySyncView = (CheckBox) findViewById(R.id.account_notify_sync);
        pushEnable = (CheckBox) findViewById(R.id.account_enable_push);

        findViewById(R.id.account_setup_options).findViewById(R.id.options_next).setOnClickListener(this);

        SpinnerOption checkFrequencies[] = {
                new SpinnerOption(-1,
                        getString(R.string.account_setup_options_mail_check_frequency_never)),
                new SpinnerOption(1,
                        getString(R.string.account_setup_options_mail_check_frequency_1min)),
                new SpinnerOption(5,
                        getString(R.string.account_setup_options_mail_check_frequency_5min)),
                new SpinnerOption(10,
                        getString(R.string.account_setup_options_mail_check_frequency_10min)),
                new SpinnerOption(15,
                        getString(R.string.account_setup_options_mail_check_frequency_15min)),
                new SpinnerOption(30,
                        getString(R.string.account_setup_options_mail_check_frequency_30min)),
                new SpinnerOption(60,
                        getString(R.string.account_setup_options_mail_check_frequency_1hour)),
                new SpinnerOption(120,
                        getString(R.string.account_setup_options_mail_check_frequency_2hour)),
                new SpinnerOption(180,
                        getString(R.string.account_setup_options_mail_check_frequency_3hour)),
                new SpinnerOption(360,
                        getString(R.string.account_setup_options_mail_check_frequency_6hour)),
                new SpinnerOption(720,
                        getString(R.string.account_setup_options_mail_check_frequency_12hour)),
                new SpinnerOption(1440,
                        getString(R.string.account_setup_options_mail_check_frequency_24hour)),

        };

        ArrayAdapter<SpinnerOption> checkFrequenciesAdapter = new ArrayAdapter<SpinnerOption>(this,
                android.R.layout.simple_spinner_item, checkFrequencies);
        checkFrequenciesAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        checkFrequencyView.setAdapter(checkFrequenciesAdapter);

        SpinnerOption displayCounts[] = {
                new SpinnerOption(10, getString(R.string.account_setup_options_mail_display_count_10)),
                new SpinnerOption(25, getString(R.string.account_setup_options_mail_display_count_25)),
                new SpinnerOption(50, getString(R.string.account_setup_options_mail_display_count_50)),
                new SpinnerOption(100, getString(R.string.account_setup_options_mail_display_count_100)),
                new SpinnerOption(250, getString(R.string.account_setup_options_mail_display_count_250)),
                new SpinnerOption(500, getString(R.string.account_setup_options_mail_display_count_500)),
                new SpinnerOption(1000, getString(R.string.account_setup_options_mail_display_count_1000)),
        };

        ArrayAdapter<SpinnerOption> displayCountsAdapter = new ArrayAdapter<SpinnerOption>(this,
                android.R.layout.simple_spinner_item, displayCounts);
        displayCountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        displayCountView.setAdapter(displayCountsAdapter);

        presenter.onOptionsStart();
    }

    @Override
    public void goToListAccounts() {
        listAccounts();
    }

    @Override
    public void setNotifyViewChecked(boolean checked) {
        notifyView.setChecked(checked);
    }

    @Override
    public void setNotifySyncViewChecked(boolean checked) {
        notifySyncView.setChecked(checked);
    }

    @Override
    public void setCheckFrequencyViewValue(int value) {
        SpinnerOption.setSpinnerOptionValue(checkFrequencyView, value);
    }

    @Override
    public void setDisplayCountViewValue(int value) {
        SpinnerOption.setSpinnerOptionValue(displayCountView, value);
    }

    @Override
    public void setPushEnableChecked(boolean checked) {
        pushEnable.setChecked(checked);
    }

    @Override
    public void setPushEnableVisibility(int visibility) {
        pushEnable.setVisibility(visibility);
    }

    // outgoing

    /**
     * Called at the end of either {@code onCreate()} or
     * {@code onRestoreInstanceState()}, after the views have been initialized,
     * so that the listeners are not triggered during the view initialization.
     * This avoids needless calls to {@code onInputChangedInOutgoing()} which is called
     * immediately after this is called.
     */
    private void initializeViewListenersInOutgoing() {

        /*
         * Updates the port when the user changes the security type. This allows
         * us to show a reasonable default which the user can change.
         */
        securityTypeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position,
                    long id) {

                onInputChangedInOutgoing();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* unused */ }
        });

        authTypeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position,
                    long id) {

                onInputChangedInOutgoing();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* unused */ }
        });

        requireLoginView.setOnCheckedChangeListener(this);
        clientCertificateSpinner
                .setOnClientCertificateChangedListener(clientCertificateChangedListenerInOutgoing);
        usernameView.addTextChangedListener(validationTextWatcherInOutgoing);
        passwordView.addTextChangedListener(validationTextWatcherInOutgoing);
        serverView.addTextChangedListener(validationTextWatcherInOutgoing);
        portView.addTextChangedListener(validationTextWatcherInOutgoing);
    }

    /**
     * This is invoked only when the user makes changes to a widget, not when
     * widgets are changed programmatically.  (The logic is simpler when you know
     * that this is the last thing called after an input change.)
     */
    private void onInputChangedInOutgoing() {
        if (presenter == null) return;

        presenter.onInputChangedInOutgoing(clientCertificateSpinner.getAlias(),
                serverView.getText().toString(),
                portView.getText().toString(), usernameView.getText().toString(),
                passwordView.getText().toString(), getSelectedAuthType(), getSelectedSecurity(),
                requireLoginView.isChecked());

    }

    protected void onNextInOutgoing() {
        ConnectionSecurity securityType = getSelectedSecurity();
        String username = usernameView.getText().toString();
        String password = passwordView.getText().toString();
        String clientCertificateAlias = clientCertificateSpinner.getAlias();
        AuthType authType = getSelectedAuthType();

        String newHost = serverView.getText().toString();
        int newPort = Integer.parseInt(portView.getText().toString());

        boolean requireLogin = requireLoginView.isChecked();
        presenter.onNextInOutgoingClicked(username, password, clientCertificateAlias, newHost, newPort, securityType,
                authType, requireLogin);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        requireLoginSettingsView.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        onInputChangedInOutgoing();
    }


    /*
     * Calls onInputChangedInOutgoing() which enables or disables the Next button
     * based on the fields' validity.
     */
    private TextWatcher validationTextWatcherInOutgoing = new TextWatcher() {
        public void afterTextChanged(Editable s) {
            onInputChangedInOutgoing();
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    };

    private OnClientCertificateChangedListener clientCertificateChangedListenerInOutgoing = new OnClientCertificateChangedListener() {
        @Override
        public void onClientCertificateChanged(String alias) {
            onInputChangedInOutgoing();
        }
    };

    private AuthType getSelectedAuthType() {
        AuthTypeHolder holder = (AuthTypeHolder) authTypeView.getSelectedItem();
        if (holder == null) return null;
        return holder.getAuthType();
    }

    private ConnectionSecurity getSelectedSecurity() {
        ConnectionSecurityHolder holder = (ConnectionSecurityHolder) securityTypeView.getSelectedItem();
        if (holder == null) return null;
        return holder.getConnectionSecurity();
    }

    private void outgoingStart() {
        final View outgoingView = findViewById(R.id.account_setup_outgoing);
        usernameView = (EditText) outgoingView.findViewById(R.id.outgoing_account_username);
        passwordView = (EditText) outgoingView.findViewById(R.id.outgoing_account_password);
        clientCertificateSpinner = (ClientCertificateSpinner) outgoingView.findViewById(R.id.outgoing_account_client_certificate_spinner);
        clientCertificateLabelView = (TextView) outgoingView.findViewById(R.id.account_client_certificate_label);
        passwordLabelView = (TextView) outgoingView.findViewById(R.id.account_password_label);
        serverView = (EditText) outgoingView.findViewById(R.id.outgoing_account_server);
        portView = (EditText) outgoingView.findViewById(R.id.outgoing_account_port);
        requireLoginView = (CheckBox) outgoingView.findViewById(R.id.account_require_login);
        requireLoginSettingsView = (ViewGroup) outgoingView.findViewById(R.id.account_require_login_settings);
        securityTypeView = (Spinner) outgoingView.findViewById(R.id.outgoing_account_security_type);
        authTypeView = (Spinner) outgoingView.findViewById(R.id.outgoing_account_auth_type);
        nextButton = (Button) outgoingView.findViewById(R.id.outgoing_next);

        nextButton.setOnClickListener(this);

        securityTypeView.setAdapter(ConnectionSecurityAdapter.get(this));

        authTypeAdapter = AuthTypeAdapter.get(this);
        authTypeView.setAdapter(authTypeAdapter);

        portView.setKeyListener(DigitsKeyListener.getInstance("0123456789"));

        // TODO: 7/25/2017 please guarantee the state is already restored so I can remove the following safely
        /* if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
            accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
        } */

        if (requireLoginView.isChecked()) {
            requireLoginSettingsView.setVisibility(View.VISIBLE);
        } else {
            requireLoginSettingsView.setVisibility(View.GONE);
        }

        boolean editSettings = false;
        if (getIntent().getAction() != null) {
            editSettings = getIntent().getAction().equals(Intent.ACTION_EDIT);
        }
        presenter.onOutgoingStart(editSettings);

        initializeViewListenersInOutgoing();
        onInputChangedInOutgoing();
    }

    @Override
    public void setNextButtonInOutgoingEnabled(boolean enabled) {
        nextButton.setEnabled(enabled);
        Utility.setCompoundDrawablesAlpha(nextButton, nextButton.isEnabled() ? 255 : 128);
    }

    @Override
    public void setAuthTypeInOutgoing(AuthType authType) {
        OnItemSelectedListener onItemSelectedListener = authTypeView.getOnItemSelectedListener();
        authTypeView.setOnItemSelectedListener(null);
        authTypeView.setSelection(authTypeAdapter.getAuthPosition(authType), false);
        authTypeView.setOnItemSelectedListener(onItemSelectedListener);
    }

    @Override
    public void setSecurityTypeInOutgoing(ConnectionSecurity security) {
        OnItemSelectedListener onItemSelectedListener = securityTypeView.getOnItemSelectedListener();
        securityTypeView.setOnItemSelectedListener(null);
        securityTypeView.setSelection(security.ordinal(), false);
        securityTypeView.setOnItemSelectedListener(onItemSelectedListener);
    }

    @Override
    public void setUsernameInOutgoing(String username) {
        usernameView.removeTextChangedListener(validationTextWatcherInOutgoing);
        usernameView.setText(username);
        requireLoginView.setChecked(true);
        requireLoginSettingsView.setVisibility(View.VISIBLE);
        usernameView.addTextChangedListener(validationTextWatcherInOutgoing);
    }

    @Override
    public void setPasswordInOutgoing(String password) {
        passwordView.removeTextChangedListener(validationTextWatcherInOutgoing);
        passwordView.setText(password);
        passwordView.addTextChangedListener(validationTextWatcherInOutgoing);
    }

    @Override
    public void setCertificateAliasInOutgoing(String alias) {
        clientCertificateSpinner.setOnClientCertificateChangedListener(null);
        clientCertificateSpinner.setAlias(alias);
        clientCertificateSpinner.
                setOnClientCertificateChangedListener(clientCertificateChangedListenerInOutgoing);
    }

    @Override
    public void setServerInOutgoing(String server) {
        serverView.removeTextChangedListener(validationTextWatcherInOutgoing);
        serverView.setText(server);
        serverView.addTextChangedListener(validationTextWatcherInOutgoing);
    }

    @Override
    public void setPortInOutgoing(String port) {
        portView.removeTextChangedListener(validationTextWatcherInOutgoing);
        portView.setText(port);
        portView.addTextChangedListener(validationTextWatcherInOutgoing);
    }

    @Override
    public void showInvalidSettingsToastInOutgoing() {
        String toastText = getString(R.string.account_setup_outgoing_invalid_setting_combo_notice,
                getString(R.string.account_setup_incoming_auth_type_label),
                AuthType.EXTERNAL.toString(),
                getString(R.string.account_setup_incoming_security_label),
                ConnectionSecurity.NONE.toString());
        Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void updateAuthPlainTextInOutgoing(boolean insecure) {
        authTypeAdapter.useInsecureText(insecure);
    }

    @Override
    public void setViewNotExternalInOutgoing() {
        // show password fields, hide client certificate fields
        passwordView.setVisibility(View.VISIBLE);
        passwordLabelView.setVisibility(View.VISIBLE);
        clientCertificateLabelView.setVisibility(View.GONE);
        clientCertificateSpinner.setVisibility(View.GONE);

        passwordView.requestFocus();
    }

    @Override
    public void setViewExternalInOutgoing() {
        // hide password fields, show client certificate fields
        passwordView.setVisibility(View.GONE);
        passwordLabelView.setVisibility(View.GONE);
        clientCertificateLabelView.setVisibility(View.VISIBLE);
        clientCertificateSpinner.setVisibility(View.VISIBLE);

        // This may again invoke onInputChangedInOutgoing()
        clientCertificateSpinner.chooseCertificate();
    }

    public static void actionEditIncomingSettings(Activity context, Account account) {
        context.startActivity(intentActionEditIncomingSettings(context, account));
    }

    public static Intent intentActionEditIncomingSettings(Context context, Account account) {
        Intent i = new Intent(context, AccountSetupActivity.class);
        i.setAction(Intent.ACTION_EDIT);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_STAGE, Stage.INCOMING);
        return i;
    }

    public static void actionEditOutgoingSettings(Context context, Account account) {
        context.startActivity(intentActionEditOutgoingSettings(context, account));
    }

    public static Intent intentActionEditOutgoingSettings(Context context, Account account) {
        Intent i = new Intent(context, AccountSetupActivity.class);
        i.setAction(Intent.ACTION_EDIT);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_STAGE, Stage.OUTGOING);
        return i;
    }

    @Override
    protected void onResume() {
        super.onResume();
        canceled = false;
        destroyed = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        canceled = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        canceled = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        canceled = true;
        destroyed = true;
    }
}
