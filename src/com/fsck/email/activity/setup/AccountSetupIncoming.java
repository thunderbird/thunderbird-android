
package com.android.email.activity.setup;

import java.net.URI;
import java.net.URISyntaxException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.email.Account;
import com.android.email.Preferences;
import com.android.email.R;
import com.android.email.Utility;

public class AccountSetupIncoming extends Activity implements OnClickListener {
    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_MAKE_DEFAULT = "makeDefault";

    private static final int popPorts[] = {
            110, 995, 995, 110, 110
    };
    private static final String popSchemes[] = {
            "pop3", "pop3+ssl", "pop3+ssl+", "pop3+tls", "pop3+tls+"
    };
    private static final int imapPorts[] = {
            143, 993, 993, 143, 143
    };
    private static final String imapSchemes[] = {
            "imap", "imap+ssl", "imap+ssl+", "imap+tls", "imap+tls+"
    };

    private int mAccountPorts[];
    private String mAccountSchemes[];
    private EditText mUsernameView;
    private EditText mPasswordView;
    private EditText mServerView;
    private EditText mPortView;
    private Spinner mSecurityTypeView;
    private Spinner mDeletePolicyView;
    private EditText mImapPathPrefixView;
    private Button mNextButton;
    private Account mAccount;
    private boolean mMakeDefault;

    public static void actionIncomingSettings(Activity context, Account account, boolean makeDefault) {
        Intent i = new Intent(context, AccountSetupIncoming.class);
        i.putExtra(EXTRA_ACCOUNT, account);
        i.putExtra(EXTRA_MAKE_DEFAULT, makeDefault);
        context.startActivity(i);
    }

    public static void actionEditIncomingSettings(Activity context, Account account) {
        Intent i = new Intent(context, AccountSetupIncoming.class);
        i.setAction(Intent.ACTION_EDIT);
        i.putExtra(EXTRA_ACCOUNT, account);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_incoming);

        mUsernameView = (EditText)findViewById(R.id.account_username);
        mPasswordView = (EditText)findViewById(R.id.account_password);
        TextView serverLabelView = (TextView) findViewById(R.id.account_server_label);
        mServerView = (EditText)findViewById(R.id.account_server);
        mPortView = (EditText)findViewById(R.id.account_port);
        mSecurityTypeView = (Spinner)findViewById(R.id.account_security_type);
        mDeletePolicyView = (Spinner)findViewById(R.id.account_delete_policy);
        mImapPathPrefixView = (EditText)findViewById(R.id.imap_path_prefix);
        mNextButton = (Button)findViewById(R.id.next);

        mNextButton.setOnClickListener(this);

        SpinnerOption securityTypes[] = {
                new SpinnerOption(0, getString(R.string.account_setup_incoming_security_none_label)),
                new SpinnerOption(1,
                        getString(R.string.account_setup_incoming_security_ssl_optional_label)),
                new SpinnerOption(2, getString(R.string.account_setup_incoming_security_ssl_label)),
                new SpinnerOption(3,
                        getString(R.string.account_setup_incoming_security_tls_optional_label)),
                new SpinnerOption(4, getString(R.string.account_setup_incoming_security_tls_label)),
        };

        SpinnerOption deletePolicies[] = {
                new SpinnerOption(0,
                        getString(R.string.account_setup_incoming_delete_policy_never_label)),
                new SpinnerOption(1,
                        getString(R.string.account_setup_incoming_delete_policy_7days_label)),
                new SpinnerOption(2,
                        getString(R.string.account_setup_incoming_delete_policy_delete_label)),
        };

        ArrayAdapter<SpinnerOption> securityTypesAdapter = new ArrayAdapter<SpinnerOption>(this,
                android.R.layout.simple_spinner_item, securityTypes);
        securityTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSecurityTypeView.setAdapter(securityTypesAdapter);

        ArrayAdapter<SpinnerOption> deletePoliciesAdapter = new ArrayAdapter<SpinnerOption>(this,
                android.R.layout.simple_spinner_item, deletePolicies);
        deletePoliciesAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDeletePolicyView.setAdapter(deletePoliciesAdapter);

        /*
         * Updates the port when the user changes the security type. This allows
         * us to show a reasonable default which the user can change.
         */
        mSecurityTypeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView arg0, View arg1, int arg2, long arg3) {
                updatePortFromSecurityType();
            }

            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

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

        mAccount = (Account)getIntent().getSerializableExtra(EXTRA_ACCOUNT);
        mMakeDefault = (boolean)getIntent().getBooleanExtra(EXTRA_MAKE_DEFAULT, false);

        /*
         * If we're being reloaded we override the original account with the one
         * we saved
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
            mAccount = (Account)savedInstanceState.getSerializable(EXTRA_ACCOUNT);
        }

        try {
            URI uri = new URI(mAccount.getStoreUri());
            String username = null;
            String password = null;
            if (uri.getUserInfo() != null) {
                String[] userInfoParts = uri.getUserInfo().split(":", 2);
                username = userInfoParts[0];
                if (userInfoParts.length > 1) {
                    password = userInfoParts[1];
                }
            }

            if (username != null) {
                mUsernameView.setText(username);
            }

            if (password != null) {
                mPasswordView.setText(password);
            }

            if (uri.getScheme().startsWith("pop3")) {
                serverLabelView.setText(R.string.account_setup_incoming_pop_server_label);
                mAccountPorts = popPorts;
                mAccountSchemes = popSchemes;

                findViewById(R.id.imap_path_prefix_section).setVisibility(View.GONE);
            } else if (uri.getScheme().startsWith("imap")) {
                serverLabelView.setText(R.string.account_setup_incoming_imap_server_label);
                mAccountPorts = imapPorts;
                mAccountSchemes = imapSchemes;

                findViewById(R.id.account_delete_policy_label).setVisibility(View.GONE);
                mDeletePolicyView.setVisibility(View.GONE);
                if (uri.getPath() != null && uri.getPath().length() > 0) {
                    mImapPathPrefixView.setText(uri.getPath().substring(1));
                }
            } else {
                throw new Error("Unknown account type: " + mAccount.getStoreUri());
            }

            for (int i = 0; i < mAccountSchemes.length; i++) {
                if (mAccountSchemes[i].equals(uri.getScheme())) {
                    SpinnerOption.setSpinnerOptionValue(mSecurityTypeView, i);
                }
            }

            SpinnerOption.setSpinnerOptionValue(mDeletePolicyView, mAccount.getDeletePolicy());

            if (uri.getHost() != null) {
                mServerView.setText(uri.getHost());
            }

            if (uri.getPort() != -1) {
                mPortView.setText(Integer.toString(uri.getPort()));
            } else {
                updatePortFromSecurityType();
            }
        } catch (URISyntaxException use) {
            /*
             * We should always be able to parse our own settings.
             */
            throw new Error(use);
        }

        validateFields();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_ACCOUNT, mAccount);
    }

    private void validateFields() {
        mNextButton
                .setEnabled(Utility.requiredFieldValid(mUsernameView)
                        && Utility.requiredFieldValid(mPasswordView)
                        && Utility.requiredFieldValid(mServerView)
                        && Utility.requiredFieldValid(mPortView));
        Utility.setCompoundDrawablesAlpha(mNextButton, mNextButton.isEnabled() ? 255 : 128);
    }

    private void updatePortFromSecurityType() {
        int securityType = (Integer)((SpinnerOption)mSecurityTypeView.getSelectedItem()).value;
        mPortView.setText(Integer.toString(mAccountPorts[securityType]));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (Intent.ACTION_EDIT.equals(getIntent().getAction())) {
                mAccount.save(Preferences.getPreferences(this));
                finish();
            } else {
                /*
                 * Set the username and password for the outgoing settings to the username and
                 * password the user just set for incoming.
                 */
                try {
                    URI oldUri = new URI(mAccount.getTransportUri());
                    URI uri = new URI(
                            oldUri.getScheme(),
                            mUsernameView.getText() + ":" + mPasswordView.getText(),
                            oldUri.getHost(),
                            oldUri.getPort(),
                            null,
                            null,
                            null);
                    mAccount.setTransportUri(uri.toString());
                } catch (URISyntaxException use) {
                    /*
                     * If we can't set up the URL we just continue. It's only for
                     * convenience.
                     */
                }


                AccountSetupOutgoing.actionOutgoingSettings(this, mAccount, mMakeDefault);
                finish();
            }
        }
    }

    private void onNext() {
        int securityType = (Integer)((SpinnerOption)mSecurityTypeView.getSelectedItem()).value;
        try {
            String path = null;
            if (mAccountSchemes[securityType].startsWith("imap")) {
                path = "/" + mImapPathPrefixView.getText();
            }
            URI uri = new URI(
                    mAccountSchemes[securityType],
                    mUsernameView.getText() + ":" + mPasswordView.getText(),
                    mServerView.getText().toString(),
                    Integer.parseInt(mPortView.getText().toString()),
                    path, // path
                    null, // query
                    null);
            mAccount.setStoreUri(uri.toString());
        } catch (URISyntaxException use) {
            /*
             * It's unrecoverable if we cannot create a URI from components that
             * we validated to be safe.
             */
            throw new Error(use);
        }

        mAccount.setDeletePolicy((Integer)((SpinnerOption)mDeletePolicyView.getSelectedItem()).value);
        AccountSetupCheckSettings.actionCheckSettings(this, mAccount, true, false);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.next:
                onNext();
                break;
        }
    }
}
