
package com.fsck.k9.activity.setup.outgoing;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;

import com.fsck.k9.activity.setup.AbstractAccountSetup;
import com.fsck.k9.activity.setup.AuthTypeAdapter;
import com.fsck.k9.activity.setup.AuthTypeHolder;
import com.fsck.k9.activity.setup.ConnectionSecurityAdapter;
import com.fsck.k9.activity.setup.ConnectionSecurityHolder;
import com.fsck.k9.activity.setup.outgoing.OutgoingContract.Presenter;
import timber.log.Timber;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.fsck.k9.*;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.view.ClientCertificateSpinner;
import com.fsck.k9.view.ClientCertificateSpinner.OnClientCertificateChangedListener;


public class OutgoingView implements OnClickListener,
    OnCheckedChangeListener, OutgoingContract.View {
    private static final String EXTRA_ACCOUNT = "account";

    private static final String EXTRA_MAKE_DEFAULT = "makeDefault";

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

    private AbstractAccountSetup activity;

    private Presenter presenter;

    public static void actionEditOutgoingSettings(Context context, Account account) {
        context.startActivity(intentActionEditOutgoingSettings(context, account));
    }

    public static Intent intentActionEditOutgoingSettings(Context context, Account account) {
        Intent i = new Intent(context, OutgoingView.class);
        i.setAction(Intent.ACTION_EDIT);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        return i;
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
        clientCertificateSpinner
                .setOnClientCertificateChangedListener(clientCertificateChangedListener);
        usernameView.addTextChangedListener(validationTextWatcher);
        passwordView.addTextChangedListener(validationTextWatcher);
        serverView.addTextChangedListener(validationTextWatcher);
        portView.addTextChangedListener(validationTextWatcher);
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

    @Override
    public void onAccountLoadFailure(Exception use) {
        Timber.e(use, "Failure");
        String toastText = activity.getString(R.string.account_setup_bad_uri, use.getMessage());

        Toast toast = Toast.makeText(activity, toastText, Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void next() {
        activity.goToAccountNames();
    }

    /*
     * Calls onInputChanged() which enables or disables the Next button
     * based on the fields' validity.
     */
    private TextWatcher validationTextWatcher = new TextWatcher() {
        public void afterTextChanged(Editable s) {
            onInputChanged();
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    };

    private OnClientCertificateChangedListener clientCertificateChangedListener = new OnClientCertificateChangedListener() {
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
    public void setActivity(AbstractAccountSetup activity) {
        this.activity = activity;
    }

    @Override
    public void start() {
        usernameView = (EditText) activity.findViewById(R.id.account_username);
        passwordView = (EditText) activity.findViewById(R.id.account_password);
        clientCertificateSpinner = (ClientCertificateSpinner) activity.findViewById(R.id.account_client_certificate_spinner);
        clientCertificateLabelView = (TextView) activity.findViewById(R.id.account_client_certificate_label);
        passwordLabelView = (TextView) activity.findViewById(R.id.account_password_label);
        serverView = (EditText) activity.findViewById(R.id.account_server);
        portView = (EditText) activity.findViewById(R.id.account_port);
        requireLoginView = (CheckBox) activity.findViewById(R.id.account_require_login);
        requireLoginSettingsView = (ViewGroup) activity.findViewById(R.id.account_require_login_settings);
        securityTypeView = (Spinner) activity.findViewById(R.id.account_security_type);
        authTypeView = (Spinner) activity.findViewById(R.id.account_auth_type);
        nextButton = (Button) activity.findViewById(R.id.next);

        nextButton.setOnClickListener(this);

        securityTypeView.setAdapter(ConnectionSecurityAdapter.get(activity));

        authTypeAdapter = AuthTypeAdapter.get(activity);
        authTypeView.setAdapter(authTypeAdapter);

        portView.setKeyListener(DigitsKeyListener.getInstance("0123456789"));

        Account account = activity.getState().getAccount();

        // TODO: 7/25/2017 please guarantee the state is already restored so I can remove the following safely
        /* if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
            accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
        } */

        presenter = new OutgoingPresenter(this, account);

        initializeViewListeners();
        onInputChanged();

        if (requireLoginView.isChecked()) {
            requireLoginSettingsView.setVisibility(View.VISIBLE);
        } else {
            requireLoginSettingsView.setVisibility(View.GONE);
        }
    }

    @Override
    public void setNextButtonEnabled(boolean enabled) {
        nextButton.setEnabled(enabled);
        Utility.setCompoundDrawablesAlpha(nextButton, nextButton.isEnabled() ? 255 : 128);
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
        String toastText = activity.getString(R.string.account_setup_outgoing_invalid_setting_combo_notice,
                activity.getString(R.string.account_setup_incoming_auth_type_label),
                AuthType.EXTERNAL.toString(),
                activity.getString(R.string.account_setup_incoming_security_label),
                ConnectionSecurity.NONE.toString());
        Toast.makeText(activity, toastText, Toast.LENGTH_LONG).show();
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
