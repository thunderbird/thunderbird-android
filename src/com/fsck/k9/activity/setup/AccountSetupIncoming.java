
package com.fsck.k9.activity.setup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.fsck.k9.*;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.store.ImapStore;
import com.fsck.k9.mail.store.Pop3Store;
import com.fsck.k9.mail.store.WebDavStore;
import com.fsck.k9.mail.store.ImapStore.ImapStoreSettings;
import com.fsck.k9.mail.store.WebDavStore.WebDavStoreSettings;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class AccountSetupIncoming extends K9Activity implements OnClickListener {
    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_MAKE_DEFAULT = "makeDefault";

    private static final int[] POP3_PORTS = {
        110, 995, 995, 110, 110
    };

    private static final int[] IMAP_PORTS = {
        143, 993, 993, 143, 143
    };

    private static final int[] WEBDAV_PORTS = {
        80, 443, 443, 443, 443
    };

    private static final ConnectionSecurity[] CONNECTION_SECURITY_TYPES = {
        ConnectionSecurity.NONE,
        ConnectionSecurity.SSL_TLS_OPTIONAL,
        ConnectionSecurity.SSL_TLS_REQUIRED,
        ConnectionSecurity.STARTTLS_OPTIONAL,
        ConnectionSecurity.STARTTLS_REQUIRED
    };

    private static final String[] AUTH_TYPES = {
        "PLAIN", "CRAM_MD5"
    };


    private int[] mAccountPorts;
    private String mStoreType;
    private EditText mUsernameView;
    private EditText mPasswordView;
    private EditText mServerView;
    private EditText mPortView;
    private Spinner mSecurityTypeView;
    private Spinner mAuthTypeView;
    private CheckBox mImapAutoDetectNamespaceView;
    private EditText mImapPathPrefixView;
    private EditText mWebdavPathPrefixView;
    private EditText mWebdavAuthPathView;
    private EditText mWebdavMailboxPathView;
    private Button mNextButton;
    private Account mAccount;
    private boolean mMakeDefault;
    private CheckBox mCompressionMobile;
    private CheckBox mCompressionWifi;
    private CheckBox mCompressionOther;
    private CheckBox mSubscribedFoldersOnly;

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

        mUsernameView = (EditText)findViewById(R.id.account_username);
        mPasswordView = (EditText)findViewById(R.id.account_password);
        TextView serverLabelView = (TextView) findViewById(R.id.account_server_label);
        mServerView = (EditText)findViewById(R.id.account_server);
        mPortView = (EditText)findViewById(R.id.account_port);
        mSecurityTypeView = (Spinner)findViewById(R.id.account_security_type);
        mAuthTypeView = (Spinner)findViewById(R.id.account_auth_type);
        mImapAutoDetectNamespaceView = (CheckBox)findViewById(R.id.imap_autodetect_namespace);
        mImapPathPrefixView = (EditText)findViewById(R.id.imap_path_prefix);
        mWebdavPathPrefixView = (EditText)findViewById(R.id.webdav_path_prefix);
        mWebdavAuthPathView = (EditText)findViewById(R.id.webdav_auth_path);
        mWebdavMailboxPathView = (EditText)findViewById(R.id.webdav_mailbox_path);
        mNextButton = (Button)findViewById(R.id.next);
        mCompressionMobile = (CheckBox)findViewById(R.id.compression_mobile);
        mCompressionWifi = (CheckBox)findViewById(R.id.compression_wifi);
        mCompressionOther = (CheckBox)findViewById(R.id.compression_other);
        mSubscribedFoldersOnly = (CheckBox)findViewById(R.id.subscribed_folders_only);

        mNextButton.setOnClickListener(this);

        mImapAutoDetectNamespaceView.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mImapPathPrefixView.setEnabled(!isChecked);
                if (isChecked && mImapPathPrefixView.hasFocus()) {
                    mImapPathPrefixView.focusSearch(View.FOCUS_UP).requestFocus();
                } else if (!isChecked) {
                    mImapPathPrefixView.requestFocus();
                }
            }
        });

        SpinnerOption securityTypes[] = {
            new SpinnerOption(0, getString(R.string.account_setup_incoming_security_none_label)),
            new SpinnerOption(1,
            getString(R.string.account_setup_incoming_security_ssl_optional_label)),
            new SpinnerOption(2, getString(R.string.account_setup_incoming_security_ssl_label)),
            new SpinnerOption(3,
            getString(R.string.account_setup_incoming_security_tls_optional_label)),
            new SpinnerOption(4, getString(R.string.account_setup_incoming_security_tls_label)),
        };

        // This needs to be kept in sync with the list at the top of the file.
        // that makes me somewhat unhappy
        SpinnerOption authTypeSpinnerOptions[] = {
            new SpinnerOption(0, AUTH_TYPES[0]),
            new SpinnerOption(1, AUTH_TYPES[1])
        };

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
                /* unused */
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                /* unused */
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

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
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
            ServerSettings settings = Store.decodeStoreUri(mAccount.getStoreUri());

            if (settings.username != null) {
                mUsernameView.setText(settings.username);
            }

            if (settings.password != null) {
                mPasswordView.setText(settings.password);
            }

            if (settings.authenticationType != null) {
                for (int i = 0; i < AUTH_TYPES.length; i++) {
                    if (AUTH_TYPES[i].equals(settings.authenticationType)) {
                        SpinnerOption.setSpinnerOptionValue(mAuthTypeView, i);
                    }
                }
            }

            mStoreType = settings.type;
            if (Pop3Store.STORE_TYPE.equals(settings.type)) {
                serverLabelView.setText(R.string.account_setup_incoming_pop_server_label);
                mAccountPorts = POP3_PORTS;
                findViewById(R.id.imap_path_prefix_section).setVisibility(View.GONE);
                findViewById(R.id.webdav_advanced_header).setVisibility(View.GONE);
                findViewById(R.id.webdav_mailbox_alias_section).setVisibility(View.GONE);
                findViewById(R.id.webdav_owa_path_section).setVisibility(View.GONE);
                findViewById(R.id.webdav_auth_path_section).setVisibility(View.GONE);
                findViewById(R.id.compression_section).setVisibility(View.GONE);
                findViewById(R.id.compression_label).setVisibility(View.GONE);
                mSubscribedFoldersOnly.setVisibility(View.GONE);
                mAccount.setDeletePolicy(Account.DELETE_POLICY_NEVER);
            } else if (ImapStore.STORE_TYPE.equals(settings.type)) {
                serverLabelView.setText(R.string.account_setup_incoming_imap_server_label);
                mAccountPorts = IMAP_PORTS;

                ImapStoreSettings imapSettings = (ImapStoreSettings) settings;

                mImapAutoDetectNamespaceView.setChecked(imapSettings.autoDetectNamespace);
                if (imapSettings.pathPrefix != null) {
                    mImapPathPrefixView.setText(imapSettings.pathPrefix);
                }

                findViewById(R.id.webdav_advanced_header).setVisibility(View.GONE);
                findViewById(R.id.webdav_mailbox_alias_section).setVisibility(View.GONE);
                findViewById(R.id.webdav_owa_path_section).setVisibility(View.GONE);
                findViewById(R.id.webdav_auth_path_section).setVisibility(View.GONE);
                mAccount.setDeletePolicy(Account.DELETE_POLICY_ON_DELETE);

                if (!Intent.ACTION_EDIT.equals(getIntent().getAction())) {
                    findViewById(R.id.imap_folder_setup_section).setVisibility(View.GONE);
                }
            } else if (WebDavStore.STORE_TYPE.equals(settings.type)) {
                serverLabelView.setText(R.string.account_setup_incoming_webdav_server_label);
                mAccountPorts = WEBDAV_PORTS;

                // Hide the unnecessary fields
                findViewById(R.id.imap_path_prefix_section).setVisibility(View.GONE);
                findViewById(R.id.account_auth_type_label).setVisibility(View.GONE);
                findViewById(R.id.account_auth_type).setVisibility(View.GONE);
                findViewById(R.id.compression_section).setVisibility(View.GONE);
                findViewById(R.id.compression_label).setVisibility(View.GONE);
                mSubscribedFoldersOnly.setVisibility(View.GONE);

                WebDavStoreSettings webDavSettings = (WebDavStoreSettings) settings;

                if (webDavSettings.path != null) {
                    mWebdavPathPrefixView.setText(webDavSettings.path);
                }

                if (webDavSettings.authPath != null) {
                    mWebdavAuthPathView.setText(webDavSettings.authPath);
                }

                if (webDavSettings.mailboxPath != null) {
                    mWebdavMailboxPathView.setText(webDavSettings.mailboxPath);
                }
                mAccount.setDeletePolicy(Account.DELETE_POLICY_ON_DELETE);
            } else {
                throw new Exception("Unknown account type: " + mAccount.getStoreUri());
            }

            // Select currently configured security type
            for (int i = 0; i < CONNECTION_SECURITY_TYPES.length; i++) {
                if (CONNECTION_SECURITY_TYPES[i] == settings.connectionSecurity) {
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

            mCompressionMobile.setChecked(mAccount.useCompression(Account.TYPE_MOBILE));
            mCompressionWifi.setChecked(mAccount.useCompression(Account.TYPE_WIFI));
            mCompressionOther.setChecked(mAccount.useCompression(Account.TYPE_OTHER));

            if (settings.host != null) {
                mServerView.setText(settings.host);
            }

            if (settings.port != -1) {
                mPortView.setText(Integer.toString(settings.port));
            } else {
                updatePortFromSecurityType();
            }

            mSubscribedFoldersOnly.setChecked(mAccount.subscribedFoldersOnly());

            validateFields();
        } catch (Exception e) {
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
        .setEnabled(Utility.requiredFieldValid(mUsernameView)
                    && Utility.requiredFieldValid(mPasswordView)
                    && Utility.domainFieldValid(mServerView)
                    && Utility.requiredFieldValid(mPortView));
        Utility.setCompoundDrawablesAlpha(mNextButton, mNextButton.isEnabled() ? 255 : 128);
    }

    private void updatePortFromSecurityType() {
        if (mAccountPorts != null) {
            int securityType = (Integer)((SpinnerOption)mSecurityTypeView.getSelectedItem()).value;
            mPortView.setText(Integer.toString(mAccountPorts[securityType]));
        }
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
                    String usernameEnc = URLEncoder.encode(mUsernameView.getText().toString(), "UTF-8");
                    String passwordEnc = URLEncoder.encode(mPasswordView.getText().toString(), "UTF-8");
                    URI oldUri = new URI(mAccount.getTransportUri());
                    URI uri = new URI(
                        oldUri.getScheme(),
                        usernameEnc + ":" + passwordEnc,
                        oldUri.getHost(),
                        oldUri.getPort(),
                        null,
                        null,
                        null);
                    mAccount.setTransportUri(uri.toString());
                } catch (UnsupportedEncodingException enc) {
                    // This really shouldn't happen since the encoding is hardcoded to UTF-8
                    Log.e(K9.LOG_TAG, "Couldn't urlencode username or password.", enc);
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

    protected void onNext() {
        try {
            ConnectionSecurity connectionSecurity = CONNECTION_SECURITY_TYPES[
                    (Integer)((SpinnerOption)mSecurityTypeView.getSelectedItem()).value];

            String username = mUsernameView.getText().toString();
            String password = mPasswordView.getText().toString();
            String authType = ((SpinnerOption)mAuthTypeView.getSelectedItem()).label;
            String host = mServerView.getText().toString();
            int port = Integer.parseInt(mPortView.getText().toString());

            Map<String, String> extra = null;
            if (ImapStore.STORE_TYPE.equals(mStoreType)) {
                extra = new HashMap<String, String>();
                extra.put(ImapStoreSettings.AUTODETECT_NAMESPACE_KEY,
                        Boolean.toString(mImapAutoDetectNamespaceView.isChecked()));
                extra.put(ImapStoreSettings.PATH_PREFIX_KEY,
                        mImapPathPrefixView.getText().toString());
            } else if (WebDavStore.STORE_TYPE.equals(mStoreType)) {
                extra = new HashMap<String, String>();
                extra.put(WebDavStoreSettings.PATH_KEY,
                        mWebdavPathPrefixView.getText().toString());
                extra.put(WebDavStoreSettings.AUTH_PATH_KEY,
                        mWebdavAuthPathView.getText().toString());
                extra.put(WebDavStoreSettings.MAILBOX_PATH_KEY,
                        mWebdavMailboxPathView.getText().toString());
            }

            ServerSettings settings = new ServerSettings(mStoreType, host, port,
                    connectionSecurity, authType, username, password, extra);

            mAccount.setStoreUri(Store.createStoreUri(settings));

            mAccount.setCompression(Account.TYPE_MOBILE, mCompressionMobile.isChecked());
            mAccount.setCompression(Account.TYPE_WIFI, mCompressionWifi.isChecked());
            mAccount.setCompression(Account.TYPE_OTHER, mCompressionOther.isChecked());
            mAccount.setSubscribedFoldersOnly(mSubscribedFoldersOnly.isChecked());

            AccountSetupCheckSettings.actionCheckSettings(this, mAccount, true, false);
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
        Log.e(K9.LOG_TAG, "Failure", use);
        String toastText = getString(R.string.account_setup_bad_uri, use.getMessage());

        Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_LONG);
        toast.show();
    }
}
