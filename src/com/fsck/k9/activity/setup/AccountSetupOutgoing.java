
package com.fsck.k9.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.fsck.k9.*;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.transport.SmtpTransport;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class AccountSetupOutgoing extends K9Activity implements OnClickListener,
    OnCheckedChangeListener {
    private static final String EXTRA_ACCOUNT = "account";

    private static final String EXTRA_MAKE_DEFAULT = "makeDefault";

    private static final int smtpPorts[] = {
        587, 465, 465, 587, 587
    };

    private static final String smtpSchemes[] = {
        "smtp", "smtp+ssl", "smtp+ssl+", "smtp+tls", "smtp+tls+"
    };
    /*
    private static final int webdavPorts[] =
    {
        80, 443, 443, 443, 443
    };
    private static final String webdavSchemes[] =
    {
        "webdav", "webdav+ssl", "webdav+ssl+", "webdav+tls", "webdav+tls+"
    };
    */
    private static final String authTypes[] = {
        SmtpTransport.AUTH_AUTOMATIC,
        SmtpTransport.AUTH_LOGIN,
        SmtpTransport.AUTH_PLAIN,
        SmtpTransport.AUTH_CRAM_MD5,
    };

    private EditText mUsernameView;
    private EditText mPasswordView;
    private EditText mServerView;
    private EditText mPortView;
    private CheckBox mRequireLoginView;
    private ViewGroup mRequireLoginSettingsView;
    private Spinner mSecurityTypeView;
    private Spinner mAuthTypeView;
    private Button mNextButton;
    private Account mAccount;
    private boolean mMakeDefault;

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

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        try {
            if (new URI(mAccount.getStoreUri()).getScheme().startsWith("webdav")) {
                mAccount.setTransportUri(mAccount.getStoreUri());
                AccountSetupCheckSettings.actionCheckSettings(this, mAccount, false, true);
            }
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        mUsernameView = (EditText)findViewById(R.id.account_username);
        mPasswordView = (EditText)findViewById(R.id.account_password);
        mServerView = (EditText)findViewById(R.id.account_server);
        mPortView = (EditText)findViewById(R.id.account_port);
        mRequireLoginView = (CheckBox)findViewById(R.id.account_require_login);
        mRequireLoginSettingsView = (ViewGroup)findViewById(R.id.account_require_login_settings);
        mSecurityTypeView = (Spinner)findViewById(R.id.account_security_type);
        mAuthTypeView = (Spinner)findViewById(R.id.account_auth_type);
        mNextButton = (Button)findViewById(R.id.next);

        mNextButton.setOnClickListener(this);
        mRequireLoginView.setOnCheckedChangeListener(this);

        SpinnerOption securityTypes[] = {
            new SpinnerOption(0, getString(R.string.account_setup_incoming_security_none_label)),
            new SpinnerOption(1,
            getString(R.string.account_setup_incoming_security_ssl_optional_label)),
            new SpinnerOption(2, getString(R.string.account_setup_incoming_security_ssl_label)),
            new SpinnerOption(3,
            getString(R.string.account_setup_incoming_security_tls_optional_label)),
            new SpinnerOption(4, getString(R.string.account_setup_incoming_security_tls_label)),
        };

        SpinnerOption authTypeSpinnerOptions[] = new SpinnerOption[authTypes.length];
        for (int i = 0; i < authTypes.length; i++) {
            authTypeSpinnerOptions[i] = new SpinnerOption(i, authTypes[i]);
        }

        ArrayAdapter<SpinnerOption> securityTypesAdapter = new ArrayAdapter<SpinnerOption>(this,
                android.R.layout.simple_spinner_item, securityTypes);
        securityTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSecurityTypeView.setAdapter(securityTypesAdapter);

        ArrayAdapter<SpinnerOption> authTypesAdapter = new ArrayAdapter<SpinnerOption>(this,
                android.R.layout.simple_spinner_item, authTypeSpinnerOptions);
        authTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAuthTypeView.setAdapter(authTypesAdapter);

        /*
         * Calls validateFields() which enables or disables the Next button
         * based on the fields' validity.
         */
        TextWatcher validationTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {
                validateFields();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
        mUsernameView.addTextChangedListener(validationTextWatcher);
        mPasswordView.addTextChangedListener(validationTextWatcher);
        mServerView.addTextChangedListener(validationTextWatcher);
        mPortView.addTextChangedListener(validationTextWatcher);

        /*
         * Only allow digits in the port field.
         */
        mPortView.setKeyListener(DigitsKeyListener.getInstance("0123456789"));

        //FIXME: get Account object again?
        accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        mMakeDefault = getIntent().getBooleanExtra(EXTRA_MAKE_DEFAULT, false);

        /*
         * If we're being reloaded we override the original account with the one
         * we saved
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
            accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
            mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        }

        try {
            URI uri = new URI(mAccount.getTransportUri());
            String username = null;
            String password = null;
            String authType = null;
            if (uri.getUserInfo() != null) {
                String[] userInfoParts = uri.getUserInfo().split(":");

                username = URLDecoder.decode(userInfoParts[0], "UTF-8");
                if (userInfoParts.length > 1) {
                    password = URLDecoder.decode(userInfoParts[1], "UTF-8");
                }
                if (userInfoParts.length > 2) {
                    authType = userInfoParts[2];
                }
            }

            if (username != null) {
                mUsernameView.setText(username);
                mRequireLoginView.setChecked(true);
            }

            if (password != null) {
                mPasswordView.setText(password);
            }

            if (authType != null) {
                for (int i = 0; i < authTypes.length; i++) {
                    if (authTypes[i].equals(authType)) {
                        SpinnerOption.setSpinnerOptionValue(mAuthTypeView, i);
                    }
                }
            }

            // Select currently configured security type
            for (int i = 0; i < smtpSchemes.length; i++) {
                if (smtpSchemes[i].equals(uri.getScheme())) {
                    SpinnerOption.setSpinnerOptionValue(mSecurityTypeView, i);
                }
            }

            /*
             * Updates the port when the user changes the security type. This allows
             * us to show a reasonable default which the user can change.
             *
             * Note: It's important that we set the listener *after* an initial option has been
             *       selected by the code above. Otherwise the listener might be called after
             *       onCreate() has been processed and the current port value set later in this
             *       method is overridden with the default port for the selected security type.
             */
            mSecurityTypeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position,
                        long id) {
                    updatePortFromSecurityType();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { /* unused */ }
            });

            if (uri.getHost() != null) {
                mServerView.setText(uri.getHost());
            }

            if (uri.getPort() != -1) {
                mPortView.setText(Integer.toString(uri.getPort()));
            } else {
                updatePortFromSecurityType();
            }

            validateFields();
        } catch (Exception e) {
            /*
             * We should always be able to parse our own settings.
             */
            failure(e);
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_ACCOUNT, mAccount.getUuid());
    }

    private void validateFields() {
        mNextButton
        .setEnabled(
            Utility.domainFieldValid(mServerView) &&
            Utility.requiredFieldValid(mPortView) &&
            (!mRequireLoginView.isChecked() ||
             (Utility.requiredFieldValid(mUsernameView) &&
              Utility.requiredFieldValid(mPasswordView))));
        Utility.setCompoundDrawablesAlpha(mNextButton, mNextButton.isEnabled() ? 255 : 128);
    }

    private void updatePortFromSecurityType() {
        int securityType = (Integer)((SpinnerOption)mSecurityTypeView.getSelectedItem()).value;
        mPortView.setText(Integer.toString(smtpPorts[securityType]));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (Intent.ACTION_EDIT.equals(getIntent().getAction())) {
                mAccount.save(Preferences.getPreferences(this));
                finish();
            } else {
                AccountSetupOptions.actionOptions(this, mAccount, mMakeDefault);
                finish();
            }
        }
    }

    protected void onNext() {
        int securityType = (Integer)((SpinnerOption)mSecurityTypeView.getSelectedItem()).value;
        URI uri;
        try {
            String usernameEnc = URLEncoder.encode(mUsernameView.getText().toString(), "UTF-8");
            String passwordEnc = URLEncoder.encode(mPasswordView.getText().toString(), "UTF-8");

            String userInfo = null;
            String authType = ((SpinnerOption)mAuthTypeView.getSelectedItem()).label;
            if (mRequireLoginView.isChecked()) {
                userInfo = usernameEnc + ":" + passwordEnc + ":" + authType;
            }
            uri = new URI(smtpSchemes[securityType], userInfo, mServerView.getText().toString(),
                          Integer.parseInt(mPortView.getText().toString()), null, null, null);
            mAccount.setTransportUri(uri.toString());
            AccountSetupCheckSettings.actionCheckSettings(this, mAccount, false, true);
        } catch (UnsupportedEncodingException enc) {
            // This really shouldn't happen since the encoding is hardcoded to UTF-8
            Log.e(K9.LOG_TAG, "Couldn't urlencode username or password.", enc);
        } catch (Exception e) {
            /*
             * It's unrecoverable if we cannot create a URI from components that
             * we validated to be safe.
             */
            failure(e);
        }

    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.next:
            onNext();
            break;
        }
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mRequireLoginSettingsView.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        validateFields();
    }
    private void failure(Exception use) {
        Log.e(K9.LOG_TAG, "Failure", use);
        String toastText = getString(R.string.account_setup_bad_uri, use.getMessage());

        Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_LONG);
        toast.show();
    }
}
