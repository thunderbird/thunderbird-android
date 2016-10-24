
package com.fsck.k9.activity.setup;


import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

import com.fsck.k9.Account;
import com.fsck.k9.EmailAddressValidator;
import com.fsck.k9.Globals;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.account.AndroidAccountOAuth2TokenStore;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.activity.setup.AccountSetupCheckSettings.CheckDirection;
import com.fsck.k9.helper.UrlEncodingHelper;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.account.AccountCreator;
import com.fsck.k9.view.ClientCertificateSpinner;
import com.fsck.k9.view.ClientCertificateSpinner.OnClientCertificateChangedListener;

/**
 * Prompts the user for the email address and password.
 * Attempts to lookup default settings for the domain the user specified. If the
 * domain is known the settings are handed off to the AccountSetupCheckSettings
 * activity. If no settings are found the settings are handed off to the
 * AccountSetupAccountType activity.
 */
public class AccountSetupBasics extends K9Activity
    implements OnClickListener, TextWatcher, OnClientCertificateChangedListener {
    private final static String EXTRA_ACCOUNT = "com.fsck.k9.AccountSetupBasics.account";
    private final static int DIALOG_NOTE = 1;
    private final static String STATE_KEY_PROVIDER =
            "com.fsck.k9.AccountSetupBasics.provider";
    private final static String STATE_KEY_CHECKED_INCOMING =
            "com.fsck.k9.AccountSetupBasics.checkedIncoming";

    private EditText mEmailView;
    private EditText mPasswordView;
    private CheckBox mClientCertificateCheckBox;
    private ClientCertificateSpinner mClientCertificateSpinner;
    private CheckBox mOAuth2CheckBox;
    private Spinner mAccountSpinner;
    private Button mNextButton;
    private Button mManualSetupButton;
    private Account mAccount;
    private Provider mProvider;

    private EmailAddressValidator mEmailValidator = new EmailAddressValidator();
    private boolean mCheckedIncoming = false;
    private CheckBox mShowPasswordCheckBox;

    public static void actionNewAccount(Context context) {
        Intent i = new Intent(context, AccountSetupBasics.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_basics);
        mEmailView = (EditText)findViewById(R.id.account_email);
        mPasswordView = (EditText)findViewById(R.id.account_password);
        mClientCertificateCheckBox = (CheckBox)findViewById(R.id.account_client_certificate);
        mClientCertificateSpinner = (ClientCertificateSpinner)findViewById(R.id.account_client_certificate_spinner);
        mOAuth2CheckBox = (CheckBox)findViewById(R.id.account_oauth2);
        mAccountSpinner = (Spinner)findViewById(R.id.account_spinner);
        OAuth2TokenProvider oAuth2TokenProvider = Globals.getOAuth2TokenProvider();
        ArrayAdapter<String>  adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, oAuth2TokenProvider.getAccounts());
        mAccountSpinner.setAdapter(adapter);
        mNextButton = (Button)findViewById(R.id.next);
        mManualSetupButton = (Button)findViewById(R.id.manual_setup);
        mShowPasswordCheckBox = (CheckBox) findViewById(R.id.show_password);
        mNextButton.setOnClickListener(this);
        mManualSetupButton.setOnClickListener(this);
    }

    private void initializeViewListeners() {
        mEmailView.addTextChangedListener(this);
        mPasswordView.addTextChangedListener(this);
        mClientCertificateCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                updateViewVisibility(mClientCertificateCheckBox.isChecked(), mOAuth2CheckBox.isChecked());
                validateFields();

                if (isChecked) {
                    // Have the user select (or confirm) the client certificate
                    mClientCertificateSpinner.chooseCertificate();
                }
            }
        });
        mClientCertificateSpinner.setOnClientCertificateChangedListener(this);

        mOAuth2CheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                updateViewVisibility(mClientCertificateCheckBox.isChecked(), mOAuth2CheckBox.isChecked());
                validateFields();
            }
        });

        mShowPasswordCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showPassword(isChecked);
            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAccount != null) {
            outState.putString(EXTRA_ACCOUNT, mAccount.getUuid());
        }
        if (mProvider != null) {
            outState.putSerializable(STATE_KEY_PROVIDER, mProvider);
        }
        outState.putBoolean(STATE_KEY_CHECKED_INCOMING, mCheckedIncoming);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
            String accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
            mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        }

        if (savedInstanceState.containsKey(STATE_KEY_PROVIDER)) {
            mProvider = (Provider) savedInstanceState.getSerializable(STATE_KEY_PROVIDER);
        }

        mCheckedIncoming = savedInstanceState.getBoolean(STATE_KEY_CHECKED_INCOMING);

        updateViewVisibility(mClientCertificateCheckBox.isChecked(), mOAuth2CheckBox.isChecked());

        showPassword(mShowPasswordCheckBox.isChecked());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        /*
         * We wait until now to initialize the listeners because we didn't want
         * the OnCheckedChangeListener active while the
         * mClientCertificateCheckBox state was being restored because it could
         * trigger the pop-up of a ClientCertificateSpinner.chooseCertificate()
         * dialog.
         */
        initializeViewListeners();
        validateFields();
    }

    public void afterTextChanged(Editable s) {
        validateFields();
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void onClientCertificateChanged(String alias) {
        validateFields();
    }

    private void updateViewVisibility(boolean usingCertificates, boolean usingXoauth) {
        if (usingCertificates) {
            // hide password fields, show client certificate spinner
            mPasswordView.setVisibility(View.GONE);
            mShowPasswordCheckBox.setVisibility(View.GONE);
            mClientCertificateSpinner.setVisibility(View.VISIBLE);
            mOAuth2CheckBox.setEnabled(false);
        } else if (usingXoauth) {
            // hide username and password fields, show account spinner
            mEmailView.setVisibility(View.GONE);
            mAccountSpinner.setVisibility(View.VISIBLE);
            mClientCertificateCheckBox.setEnabled(false);
            mShowPasswordCheckBox.setVisibility(View.GONE);
            mPasswordView.setVisibility(View.GONE);
        } else {
            // show username & password fields, hide client certificate spinner
            mEmailView.setVisibility(View.VISIBLE);
            mAccountSpinner.setVisibility(View.GONE);
            mPasswordView.setVisibility(View.VISIBLE);
            mShowPasswordCheckBox.setVisibility(View.VISIBLE);
            mClientCertificateSpinner.setVisibility(View.GONE);
            mClientCertificateCheckBox.setEnabled(true);
            mOAuth2CheckBox.setEnabled(true);
        }
    }

    private void showPassword(boolean show) {
        int cursorPosition = mPasswordView.getSelectionStart();
        if (show) {
            mPasswordView.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            mPasswordView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        mPasswordView.setSelection(cursorPosition);
    }

    private void validateFields() {
        boolean clientCertificateChecked = mClientCertificateCheckBox.isChecked();
        boolean oauth2Checked = mOAuth2CheckBox.isChecked();
        String clientCertificateAlias = mClientCertificateSpinner.getAlias();
        String email = mEmailView.getText().toString();

        boolean valid =
                (oauth2Checked
                        && mAccountSpinner.getSelectedItem() != null
                        && mAccountSpinner.getSelectedItem().toString() != null
                        && !mAccountSpinner.getSelectedItem().toString().isEmpty()) ||
                ( Utility.requiredFieldValid(mEmailView)
                && ((!clientCertificateChecked && Utility.requiredFieldValid(mPasswordView))
                        || (clientCertificateChecked && clientCertificateAlias != null)));

        mNextButton.setEnabled(valid);
        mManualSetupButton.setEnabled(valid);
        /*
         * Dim the next button's icon to 50% if the button is disabled.
         * TODO this can probably be done with a stateful drawable. Check into it.
         * android:state_enabled
         */
        Utility.setCompoundDrawablesAlpha(mNextButton, mNextButton.isEnabled() ? 255 : 128);
    }

    private String getOwnerName() {
        String name = null;
        try {
            name = getDefaultAccountName();
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Could not get default account name", e);
        }

        if (name == null) {
            name = "";
        }
        return name;
    }

    private String getDefaultAccountName() {
        String name = null;
        Account account = Preferences.getPreferences(this).getDefaultAccount();
        if (account != null) {
            name = account.getName();
        }
        return name;
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_NOTE) {
            if (mProvider != null && mProvider.note != null) {
                return new AlertDialog.Builder(this)
                       .setMessage(mProvider.note)
                       .setPositiveButton(
                           getString(R.string.okay_action),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finishAutoSetup();
                    }
                })
                       .setNegativeButton(
                           getString(R.string.cancel_action),
                           null)
                       .create();
            }
        }
        return null;
    }

    private void finishAutoSetup() {
        boolean usingXOAuth2 = mOAuth2CheckBox.isChecked();

        String email;
        if (usingXOAuth2) {
            email = mAccountSpinner.getSelectedItem().toString();
        } else {
            email = mEmailView.getText().toString();
        }
        String password = mPasswordView.getText().toString();
        String[] emailParts = splitEmail(email);
        String user = emailParts[0];
        String domain = emailParts[1];
        try {
            String userEnc = UrlEncodingHelper.encodeUtf8(user);
            String passwordEnc = UrlEncodingHelper.encodeUtf8(password);

            String incomingUsername = mProvider.incomingUsernameTemplate;
            incomingUsername = incomingUsername.replaceAll("\\$email", email);
            incomingUsername = incomingUsername.replaceAll("\\$user", userEnc);
            incomingUsername = incomingUsername.replaceAll("\\$domain", domain);

            URI incomingUriTemplate = mProvider.incomingUriTemplate;
            String incomingUserInfo = incomingUsername + ":" + passwordEnc;
            if (usingXOAuth2)
                incomingUserInfo = AuthType.XOAUTH2 + ":"+incomingUserInfo;
            URI incomingUri = new URI(incomingUriTemplate.getScheme(), incomingUserInfo,
                    incomingUriTemplate.getHost(), incomingUriTemplate.getPort(),
                    null, null, null);

            String outgoingUsername = mProvider.outgoingUsernameTemplate;

            URI outgoingUriTemplate = mProvider.outgoingUriTemplate;


            URI outgoingUri;
            if (outgoingUsername != null) {
                outgoingUsername = outgoingUsername.replaceAll("\\$email", email);
                outgoingUsername = outgoingUsername.replaceAll("\\$user", userEnc);
                outgoingUsername = outgoingUsername.replaceAll("\\$domain", domain);

                String outgoingUserInfo = outgoingUsername + ":" + passwordEnc;
                if (usingXOAuth2) {
                    outgoingUserInfo = outgoingUserInfo + ":" + AuthType.XOAUTH2;
                }
                outgoingUri = new URI(outgoingUriTemplate.getScheme(), outgoingUserInfo,
                        outgoingUriTemplate.getHost(), outgoingUriTemplate.getPort(), null,
                        null, null);

            } else {
                outgoingUri = new URI(outgoingUriTemplate.getScheme(),
                                      null, outgoingUriTemplate.getHost(), outgoingUriTemplate.getPort(), null,
                                      null, null);


            }
            if (mAccount == null) {
                mAccount = Preferences.getPreferences(this).newAccount();
            }
            mAccount.setName(getOwnerName());
            mAccount.setEmail(email);
            mAccount.setStoreUri(incomingUri.toString());
            mAccount.setTransportUri(outgoingUri.toString());

            setupFolderNames(incomingUriTemplate.getHost().toLowerCase(Locale.US));

            ServerSettings incomingSettings = RemoteStore.decodeStoreUri(incomingUri.toString());
            mAccount.setDeletePolicy(AccountCreator.getDefaultDeletePolicy(incomingSettings.type));

            // Check incoming here.  Then check outgoing in onActivityResult()
            AccountSetupCheckSettings.actionCheckSettings(this, mAccount, CheckDirection.INCOMING);
        } catch (URISyntaxException use) {
            /*
             * If there is some problem with the URI we give up and go on to
             * manual setup.
             */
            onManualSetup();
        }
    }

    private void onNext() {
        if (mClientCertificateCheckBox.isChecked()) {
            // Auto-setup doesn't support client certificates.
            onManualSetup();
            return;
        }

        String email;

        if (mEmailView.getVisibility() == View.VISIBLE) {
            email = mEmailView.getText().toString();
        } else {
            email = mAccountSpinner.getSelectedItem().toString();
        }
        String[] emailParts = splitEmail(email);
        String domain = emailParts[1];
        mProvider = findProviderForDomain(domain);
        if (mProvider == null) {
            /*
             * We don't have default settings for this account, start the manual
             * setup process.
             */
            onManualSetup();
            return;
        }
        Log.i(K9.LOG_TAG, "Provider found, using automatic set-up");

        if (mProvider.note != null) {
            showDialog(DIALOG_NOTE);
        } else {
            finishAutoSetup();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (!mCheckedIncoming) {
                //We've successfully checked incoming.  Now check outgoing.
                mCheckedIncoming = true;
                AccountSetupCheckSettings.actionCheckSettings(this, mAccount, CheckDirection.OUTGOING);
            } else {
                //We've successfully checked outgoing as well.
                mAccount.setDescription(mAccount.getEmail());
                mAccount.save(Preferences.getPreferences(this));
                K9.setServicesEnabled(this);
                AccountSetupNames.actionSetNames(this, mAccount);
                finish();
            }
        }
    }

    private void onManualSetup() {
        String email;
        if (mOAuth2CheckBox.isChecked()) {
            email = mAccountSpinner.getSelectedItem().toString();
        } else {
            email = mEmailView.getText().toString();
        }
        String[] emailParts = splitEmail(email);
        String user = email;
        String domain = emailParts[1];

        String password = null;
        String clientCertificateAlias = null;
        AuthType authenticationType;
        if (mClientCertificateCheckBox.isChecked()) {
            authenticationType = AuthType.EXTERNAL;
            clientCertificateAlias = mClientCertificateSpinner.getAlias();
        } else if (mOAuth2CheckBox.isChecked()) {
            authenticationType = AuthType.XOAUTH2;
        } else {
            authenticationType = AuthType.PLAIN;
            password = mPasswordView.getText().toString();
        }

        if (mAccount == null) {
            mAccount = Preferences.getPreferences(this).newAccount();
        }
        mAccount.setName(getOwnerName());
        mAccount.setEmail(email);

        // set default uris
        // NOTE: they will be changed again in AccountSetupAccountType!
        ServerSettings storeServer = new ServerSettings(ServerSettings.Type.IMAP, "mail." + domain, -1,
                ConnectionSecurity.SSL_TLS_REQUIRED, authenticationType, user, password, clientCertificateAlias);
        ServerSettings transportServer = new ServerSettings(ServerSettings.Type.SMTP, "mail." + domain, -1,
                ConnectionSecurity.SSL_TLS_REQUIRED, authenticationType, user, password, clientCertificateAlias);
        String storeUri = RemoteStore.createStoreUri(storeServer);
        String transportUri = Transport.createTransportUri(transportServer);
        mAccount.setStoreUri(storeUri);
        mAccount.setTransportUri(transportUri);

        setupFolderNames(domain);

        AccountSetupAccountType.actionSelectAccountType(this, mAccount, false);

        finish();
    }

    private void setupFolderNames(String domain) {
        mAccount.setDraftsFolderName(getString(R.string.special_mailbox_name_drafts));
        mAccount.setTrashFolderName(getString(R.string.special_mailbox_name_trash));
        mAccount.setSentFolderName(getString(R.string.special_mailbox_name_sent));
        mAccount.setArchiveFolderName(getString(R.string.special_mailbox_name_archive));

        // Yahoo! has a special folder for Spam, called "Bulk Mail".
        if (domain.endsWith(".yahoo.com")) {
            mAccount.setSpamFolderName("Bulk Mail");
        } else {
            mAccount.setSpamFolderName(getString(R.string.special_mailbox_name_spam));
        }
    }


    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.next:
            onNext();
            break;
        case R.id.manual_setup:
            onManualSetup();
            break;
        }
    }

    /**
     * @param xml XML Parser
     * @param name Attribute name
     * @return the given attribute as a String resource first, or if it fails,
     * the attribute as a simple String value.
     */
    private String getXmlAttribute(XmlResourceParser xml, String name) {
        int resId = xml.getAttributeResourceValue(null, name, 0);
        if (resId == 0) {
            return xml.getAttributeValue(null, name);
        } else {
            return getString(resId);
        }
    }

    private Provider findProviderForDomain(String domain) {
        try {
            XmlResourceParser xml = getResources().getXml(R.xml.providers);
            int xmlEventType;
            Provider provider = null;
            while ((xmlEventType = xml.next()) != XmlResourceParser.END_DOCUMENT) {
                if (xmlEventType == XmlResourceParser.START_TAG
                        && "provider".equals(xml.getName())
                        && domain.equalsIgnoreCase(getXmlAttribute(xml, "domain"))) {
                    provider = new Provider();
                    provider.id = getXmlAttribute(xml, "id");
                    provider.label = getXmlAttribute(xml, "label");
                    provider.domain = getXmlAttribute(xml, "domain");
                    provider.note = getXmlAttribute(xml, "note");
                } else if (xmlEventType == XmlResourceParser.START_TAG
                           && "incoming".equals(xml.getName())
                           && provider != null) {
                    provider.incomingUriTemplate = new URI(getXmlAttribute(xml, "uri"));
                    provider.incomingUsernameTemplate = getXmlAttribute(xml, "username");
                } else if (xmlEventType == XmlResourceParser.START_TAG
                           && "outgoing".equals(xml.getName())
                           && provider != null) {
                    provider.outgoingUriTemplate = new URI(getXmlAttribute(xml, "uri"));
                    provider.outgoingUsernameTemplate = getXmlAttribute(xml, "username");
                } else if (xmlEventType == XmlResourceParser.END_TAG
                           && "provider".equals(xml.getName())
                           && provider != null) {
                    return provider;
                }
            }
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Error while trying to load provider settings.", e);
        }
        return null;
    }

    private String[] splitEmail(String email) {
        String[] retParts = new String[2];
        String[] emailParts = email.split("@");
        retParts[0] = (emailParts.length > 0) ? emailParts[0] : "";
        retParts[1] = (emailParts.length > 1) ? emailParts[1] : "";
        return retParts;
    }

    static class Provider implements Serializable {
        private static final long serialVersionUID = 8511656164616538989L;

        public String id;

        public String label;

        public String domain;

        public URI incomingUriTemplate;

        public String incomingUsernameTemplate;

        public URI outgoingUriTemplate;

        public String outgoingUsernameTemplate;

        public String note;
    }

}
