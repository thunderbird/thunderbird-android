
package com.fsck.k9.activity.setup.outgoing;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;

import com.fsck.k9.activity.setup.AccountSetupOptions;
import com.fsck.k9.activity.setup.AuthTypeAdapter;
import com.fsck.k9.activity.setup.AuthTypeHolder;
import com.fsck.k9.activity.setup.ConnectionSecurityAdapter;
import com.fsck.k9.activity.setup.ConnectionSecurityHolder;
import com.fsck.k9.activity.setup.checksettings.AccountSetupCheckSettings;
import com.fsck.k9.activity.setup.checksettings.CheckSettingsPresenter.CheckDirection;
import com.fsck.k9.activity.setup.outgoing.OutgoingContract.Presenter;
import timber.log.Timber;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.fsck.k9.*;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.view.ClientCertificateSpinner;
import com.fsck.k9.view.ClientCertificateSpinner.OnClientCertificateChangedListener;


public class AccountSetupOutgoing extends K9Activity implements OnClickListener,
    OnCheckedChangeListener, OutgoingContract.View {
    private static final String EXTRA_ACCOUNT = "account";

    private static final String EXTRA_MAKE_DEFAULT = "makeDefault";

    private static final String STATE = "state";

    private EditText usernameView;
    private EditText passwordView;
    private ClientCertificateSpinner clientCertificateSpinner;
    private TextView clientCertificateLabelView;
    private TextView passwordLabelView;
    private EditText serverView;
    private EditText portView;
    private CheckBox requireLoginView;
    private ViewGroup requireLoginSettingsView;
    private Spinner securityTypeView;
    private Spinner authTypeView;
    private AuthTypeAdapter authTypeAdapter;
    private Button nextButton;
    private boolean makeDefault;

    private Presenter presenter;

    public static void actionOutgoingSettings(Context context, Account account, boolean makeDefault) {
        Intent i = new Intent(context, AccountSetupOutgoing.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_MAKE_DEFAULT, makeDefault);
        context.startActivity(i);
    }

    public static void actionEditOutgoingSettings(Context context, Account account) {
        context.startActivity(intentActionEditOutgoingSettings(context, account));
    }

    public static Intent intentActionEditOutgoingSettings(Context context, Account account) {
        Intent i = new Intent(context, AccountSetupOutgoing.class);
        i.setAction(Intent.ACTION_EDIT);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        return i;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_outgoing);

        usernameView = (EditText)findViewById(R.id.account_username);
        passwordView = (EditText)findViewById(R.id.account_password);
        clientCertificateSpinner = (ClientCertificateSpinner)findViewById(R.id.account_client_certificate_spinner);
        clientCertificateLabelView = (TextView)findViewById(R.id.account_client_certificate_label);
        passwordLabelView = (TextView)findViewById(R.id.account_password_label);
        serverView = (EditText)findViewById(R.id.account_server);
        portView = (EditText)findViewById(R.id.account_port);
        requireLoginView = (CheckBox)findViewById(R.id.account_require_login);
        requireLoginSettingsView = (ViewGroup)findViewById(R.id.account_require_login_settings);
        securityTypeView = (Spinner)findViewById(R.id.account_security_type);
        authTypeView = (Spinner)findViewById(R.id.account_auth_type);
        nextButton = (Button)findViewById(R.id.next);

        nextButton.setOnClickListener(this);

        securityTypeView.setAdapter(ConnectionSecurityAdapter.get(this));

        authTypeAdapter = AuthTypeAdapter.get(this);
        authTypeView.setAdapter(authTypeAdapter);

        portView.setKeyListener(DigitsKeyListener.getInstance("0123456789"));

        makeDefault = getIntent().getBooleanExtra(EXTRA_MAKE_DEFAULT, false);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);

        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
            accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
        }

        presenter = new OutgoingPresenter(this, accountUuid);

        if (savedInstanceState != null && savedInstanceState.containsKey(STATE)) {
            presenter.setState((OutgoingState) savedInstanceState.getParcelable(STATE));
        }

    }

    /**
     * Called at the end of either {@code onCreate()} or
     * {@code onRestoreInstanceState()}, after the views have been initialized,
     * so that the listeners are not triggered during the view initialization.
     * This avoids needless calls to {@code onInputChanged()} which is called
     * immediately after this is called.
     */
    private void initializeViewListeners() {

        /*
         * Updates the port when the user changes the security type. This allows
         * us to show a reasonable default which the user can change.
         */
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

        requireLoginView.setOnCheckedChangeListener(this);
        clientCertificateSpinner.setOnClientCertificateChangedListener(clientCertificateChangedListener);
        usernameView.addTextChangedListener(validationTextWatcher);
        passwordView.addTextChangedListener(validationTextWatcher);
        serverView.addTextChangedListener(validationTextWatcher);
        portView.addTextChangedListener(validationTextWatcher);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_ACCOUNT, presenter.getAccount().getUuid());
        outState.putParcelable(STATE, presenter.getState());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (requireLoginView.isChecked()) {
            requireLoginSettingsView.setVisibility(View.VISIBLE);
        } else {
            requireLoginSettingsView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        /*
         * We didn't want the listeners active while the state was being restored
         * because they could overwrite the restored port with a default port when
         * the security type was restored.
         */
        initializeViewListeners();
        onInputChanged();
    }

    /**
     * This is invoked only when the user makes changes to a widget, not when
     * widgets are changed programmatically.  (The logic is simpler when you know
     * that this is the last thing called after an input change.)
     */
    private void onInputChanged() {
        if (presenter == null) return;

        presenter.onInputChanged(clientCertificateSpinner.getAlias(),
                serverView.getText().toString(),
                portView.getText().toString(), usernameView.getText().toString(),
                passwordView.getText().toString(), getSelectedAuthType(), getSelectedSecurity(),
                requireLoginView.isChecked());

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (Intent.ACTION_EDIT.equals(getIntent().getAction())) {
                presenter.onAccountEdited();
                finish();
            } else {
                AccountSetupOptions.actionOptions(this, presenter.getAccount(), makeDefault);
                finish();
            }
        }
    }

    protected void onNext() {
        ConnectionSecurity securityType = getSelectedSecurity();
        String username = usernameView.getText().toString();
        String password = passwordView.getText().toString();
        String clientCertificateAlias = clientCertificateSpinner.getAlias();
        AuthType authType = getSelectedAuthType();

        String newHost = serverView.getText().toString();
        int newPort = Integer.parseInt(portView.getText().toString());

        boolean requireLogin = requireLoginView.isChecked();
        presenter.onNext(username, password, clientCertificateAlias, newHost, newPort, securityType,
                authType, requireLogin);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.next:
                onNext();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        requireLoginSettingsView.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        onInputChanged();
    }

    private void failure(Exception use) {
        Timber.e(use, "Failure");
        String toastText = getString(R.string.account_setup_bad_uri, use.getMessage());

        Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_LONG);
        toast.show();
    }

    /*
     * Calls onInputChanged() which enables or disables the Next button
     * based on the fields' validity.
     */
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
    public void setNextButtonEnabled(boolean enabled) {
        nextButton.setEnabled(enabled);
        Utility.setCompoundDrawablesAlpha(nextButton, nextButton.isEnabled() ? 255 : 128);
    }

    @Override
    public void next(String accountUuid) {
        AccountSetupCheckSettings.startChecking(this, accountUuid, CheckDirection.OUTGOING);
    }

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
        requireLoginView.setChecked(true);
        requireLoginSettingsView.setVisibility(View.VISIBLE);
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
    public void showInvalidSettingsToast() {
        String toastText = getString(R.string.account_setup_outgoing_invalid_setting_combo_notice,
                getString(R.string.account_setup_incoming_auth_type_label),
                AuthType.EXTERNAL.toString(),
                getString(R.string.account_setup_incoming_security_label),
                ConnectionSecurity.NONE.toString());
        Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void updateAuthPlainText(boolean insecure) {
        authTypeAdapter.useInsecureText(insecure);
    }

    @Override
    public void onAuthTypeIsNotExternal() {
        // show password fields, hide client certificate fields
        passwordView.setVisibility(View.VISIBLE);
        passwordLabelView.setVisibility(View.VISIBLE);
        clientCertificateLabelView.setVisibility(View.GONE);
        clientCertificateSpinner.setVisibility(View.GONE);

        passwordView.requestFocus();
    }

    @Override
    public void onAuthTypeIsExternal() {
        // hide password fields, show client certificate fields
        passwordView.setVisibility(View.GONE);
        passwordLabelView.setVisibility(View.GONE);
        clientCertificateLabelView.setVisibility(View.VISIBLE);
        clientCertificateSpinner.setVisibility(View.VISIBLE);

        // This may again invoke onInputChanged()
        clientCertificateSpinner.chooseCertificate();
    }
}
