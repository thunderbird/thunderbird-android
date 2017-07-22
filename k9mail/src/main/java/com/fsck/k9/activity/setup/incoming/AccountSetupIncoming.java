
package com.fsck.k9.activity.setup.incoming;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;

import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.activity.setup.IncomingAndOutgoingState;
import com.fsck.k9.activity.setup.outgoing.AccountSetupOutgoing;
import com.fsck.k9.activity.setup.AuthTypeAdapter;
import com.fsck.k9.activity.setup.AuthTypeHolder;
import com.fsck.k9.activity.setup.ConnectionSecurityAdapter;
import com.fsck.k9.activity.setup.ConnectionSecurityHolder;
import com.fsck.k9.activity.setup.checksettings.AccountSetupCheckSettings;
import com.fsck.k9.activity.setup.checksettings.CheckSettingsPresenter.CheckDirection;
import com.fsck.k9.activity.setup.incoming.IncomingContract.Presenter;
import timber.log.Timber;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.fsck.k9.*;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.view.ClientCertificateSpinner;
import com.fsck.k9.view.ClientCertificateSpinner.OnClientCertificateChangedListener;


public class AccountSetupIncoming extends K9Activity implements OnClickListener, IncomingContract.View {
    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_MAKE_DEFAULT = "makeDefault";

    private static final String STATE = "state";

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
    private boolean makeDefault;
    private CheckBox compressionMobile;
    private CheckBox compressionWifi;
    private CheckBox compressionOther;
    private CheckBox subscribedFoldersOnly;
    private AuthTypeAdapter authTypeAdapter;
    private Presenter presenter;

    public static void actionIncomingSettings(Activity context, Account account, boolean makeDefault) {
        Intent i = new Intent(context, AccountSetupIncoming.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_MAKE_DEFAULT, makeDefault);
        context.startActivity(i);
    }

    public static void actionEditIncomingSettings(Activity context, Account account) {
        context.startActivity(intentActionEditIncomingSettings(context, account));
    }

    public static Intent intentActionEditIncomingSettings(Context context, Account account) {
        Intent i = new Intent(context, AccountSetupIncoming.class);
        i.setAction(Intent.ACTION_EDIT);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        return i;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_incoming);

        usernameView = (EditText)findViewById(R.id.account_username);
        passwordView = (EditText)findViewById(R.id.account_password);
        clientCertificateSpinner = (ClientCertificateSpinner)findViewById(R.id.account_client_certificate_spinner);
        clientCertificateLabelView = (TextView)findViewById(R.id.account_client_certificate_label);
        passwordLabelView = (TextView)findViewById(R.id.account_password_label);
        serverLabelView = (TextView) findViewById(R.id.account_server_label);
        serverView = (EditText)findViewById(R.id.account_server);
        portView = (EditText)findViewById(R.id.account_port);
        securityTypeView = (Spinner)findViewById(R.id.account_security_type);
        authTypeView = (Spinner)findViewById(R.id.account_auth_type);
        imapAutoDetectNamespaceView = (CheckBox)findViewById(R.id.imap_autodetect_namespace);
        imapPathPrefixView = (EditText)findViewById(R.id.imap_path_prefix);
        webdavPathPrefixView = (EditText)findViewById(R.id.webdav_path_prefix);
        webdavAuthPathView = (EditText)findViewById(R.id.webdav_auth_path);
        webdavMailboxPathView = (EditText)findViewById(R.id.webdav_mailbox_path);
        nextButton = (Button)findViewById(R.id.next);
        compressionMobile = (CheckBox)findViewById(R.id.compression_mobile);
        compressionWifi = (CheckBox)findViewById(R.id.compression_wifi);
        compressionOther = (CheckBox)findViewById(R.id.compression_other);
        subscribedFoldersOnly = (CheckBox)findViewById(R.id.subscribed_folders_only);

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

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        makeDefault = getIntent().getBooleanExtra(EXTRA_MAKE_DEFAULT, false);

        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
            accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
        }

        boolean editSettings = Intent.ACTION_EDIT.equals(getIntent().getAction());

        presenter = new IncomingPresenter(this, accountUuid, editSettings);

        if (savedInstanceState != null && savedInstanceState.containsKey(STATE)) {
            presenter.setState((IncomingAndOutgoingState) savedInstanceState.getParcelable(STATE));
        }
    }

    private void initializeViewListeners() {
        securityTypeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position,
                    long id) {

                onInputChanged();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* unused */ }
        });

        authTypeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position,
                    long id) {

                onInputChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* unused */ }
        });

        clientCertificateSpinner.setOnClientCertificateChangedListener(clientCertificateChangedListener);
        usernameView.addTextChangedListener(validationTextWatcher);
        passwordView.addTextChangedListener(validationTextWatcher);
        serverView.addTextChangedListener(validationTextWatcher);
        portView.addTextChangedListener(validationTextWatcher);
    }

    private void onInputChanged() {
        if (presenter == null) return;

        presenter.onInputChanged(clientCertificateSpinner.getAlias(),
                serverView.getText().toString(),
                portView.getText().toString(), usernameView.getText().toString(),
                passwordView.getText().toString(), getSelectedAuthType(), getSelectedSecurity());

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_ACCOUNT, presenter.getAccount().getUuid());

        outState.putParcelable(STATE, presenter.getState());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        initializeViewListeners();
        onInputChanged();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (Intent.ACTION_EDIT.equals(getIntent().getAction())) {
                presenter.updateAccount();
                finish();
            } else {
                /*
                 * Set the username and password for the outgoing settings to the username and
                 * password the user just set for incoming.
                 */
                String username = usernameView.getText().toString();
                String password = passwordView.getText().toString();
                String clientCertificateAlias = clientCertificateSpinner.getAlias();
                AuthType authType = getSelectedAuthType();

                presenter.prepareForOutgoing(username, password, clientCertificateAlias, authType);

                finish();
            }
        }
    }

    protected void onNext() {
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

            presenter.modifyAccount(username, password, clientCertificateAlias, autoDetectNamespace,
                    imapPathPrefix, webdavPathPrefix, webdavAuthPath, webdavMailboxPath, host, port,
                    connectionSecurity, authType, compressMobile, compressWifi, compressOther,
                    subscribeFoldersOnly);

            AccountSetupCheckSettings.startChecking(this, presenter.getAccount().getUuid(),
                    CheckDirection.INCOMING);
        } catch (Exception e) {
            failure(e);
        }

    }

    public void onClick(View v) {
        try {
            switch (v.getId()) {
            case R.id.next:
                onNext();
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


    TextWatcher validationTextWatcher = new TextWatcher() {
        public void afterTextChanged(Editable s) {
            onInputChanged();
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    };

    OnClientCertificateChangedListener clientCertificateChangedListener = new OnClientCertificateChangedListener() {
        @Override
        public void onClientCertificateChanged(String alias) {
            onInputChanged();
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
    public void setUsername(String username) {
        usernameView.removeTextChangedListener(validationTextWatcher);
        usernameView.setText(username);
        usernameView.addTextChangedListener(validationTextWatcher);
    }

    @Override
    public void setPassword(String password) {
        passwordView.removeTextChangedListener(validationTextWatcher);
        passwordView.setText(password);
        passwordView.addTextChangedListener(validationTextWatcher);
    }

    @Override
    public void setCertificateAlias(String alias) {
        clientCertificateSpinner.setOnClientCertificateChangedListener(null);
        clientCertificateSpinner.setAlias(alias);
        clientCertificateSpinner.setOnClientCertificateChangedListener(clientCertificateChangedListener);
    }

    @Override
    public void setServer(String server) {
        serverView.removeTextChangedListener(validationTextWatcher);
        serverView.setText(server);
        serverView.addTextChangedListener(validationTextWatcher);
    }

    @Override
    public void setPort(String port) {
        portView.removeTextChangedListener(validationTextWatcher);
        portView.setText(port);
        portView.addTextChangedListener(validationTextWatcher);
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
        findViewById(R.id.account_auth_type).setVisibility(View.GONE);
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

    private AuthType getSelectedAuthType() {
        AuthTypeHolder holder = (AuthTypeHolder) authTypeView.getSelectedItem();
        return holder.getAuthType();
    }

    private ConnectionSecurity getSelectedSecurity() {
        ConnectionSecurityHolder holder = (ConnectionSecurityHolder) securityTypeView.getSelectedItem();
        return holder.getConnectionSecurity();
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void goToOutgoingSettings(Account account) {
        AccountSetupOutgoing.actionOutgoingSettings(this, account, makeDefault);
    }

    @Override
    public void setNextEnabled(boolean enabled) {
        nextButton.setEnabled(enabled);
        Utility.setCompoundDrawablesAlpha(nextButton, nextButton.isEnabled() ? 255 : 128);
    }

    @Override
    public Context getContext() {
        return this;
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
    public void onAuthTypeIsNotExternal() {
        passwordView.setVisibility(View.VISIBLE);
        passwordLabelView.setVisibility(View.VISIBLE);
        clientCertificateLabelView.setVisibility(View.GONE);
        clientCertificateSpinner.setVisibility(View.GONE);

        passwordView.requestFocus();
    }

    @Override
    public void onAuthTypeIsExternal() {
        passwordView.setVisibility(View.GONE);
        passwordLabelView.setVisibility(View.GONE);
        clientCertificateLabelView.setVisibility(View.VISIBLE);
        clientCertificateSpinner.setVisibility(View.VISIBLE);

        clientCertificateSpinner.chooseCertificate();
    }

    @Override
    public void onAccountLoadFailure(Exception use) {
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
}
