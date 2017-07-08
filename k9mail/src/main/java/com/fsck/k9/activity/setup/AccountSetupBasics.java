
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;

import com.fsck.k9.mail.autoconfiguration.AutoConfigure;
import com.fsck.k9.mail.autoconfiguration.AutoConfigure.ProviderInfo;
import com.fsck.k9.mail.autoconfiguration.AutoConfigureAutodiscover;
import com.fsck.k9.mail.autoconfiguration.AutoconfigureMozilla;
import com.fsck.k9.mail.autoconfiguration.AutoconfigureSrv;
import timber.log.Timber;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

import com.fsck.k9.Account;
import com.fsck.k9.EmailAddressValidator;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.account.AccountCreator;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.activity.setup.AccountSetupCheckSettings.CheckDirection;
import com.fsck.k9.helper.UrlEncodingHelper;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.TransportUris;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.view.ClientCertificateSpinner;
import com.fsck.k9.view.ClientCertificateSpinner.OnClientCertificateChangedListener;
import timber.log.Timber;

/**
 * Prompts the user for the email address and password.
 * Attempts to lookup default settings for the domain the user specified. If the
 * domain is known the settings are handed off to the AccountSetupCheckSettings
 * activity. If no settings are found the settings are handed off to the
 * AccountSetupAccountType activity.
 */
public class AccountSetupBasics extends K9Activity
    implements OnClickListener, TextWatcher, OnCheckedChangeListener, OnClientCertificateChangedListener {
    private final static String EXTRA_ACCOUNT = "com.fsck.k9.AccountSetupBasics.account";
    private final static int DIALOG_NOTE = 1;
    private final static String STATE_KEY_PROVIDER =
            "com.fsck.k9.AccountSetupBasics.provider";
    private final static String STATE_KEY_CHECKED_INCOMING =
            "com.fsck.k9.AccountSetupBasics.checkedIncoming";

    private EditText emailView;
    private EditText passwordView;
    private CheckBox clientCertificateCheckBox;
    private ClientCertificateSpinner clientCertificateSpinner;
    private Button nextButton;
    private Button manualSetupButton;
    private Account account;
    private Provider provider;

    private EmailAddressValidator emailValidator = new EmailAddressValidator();
    private boolean checkedIncoming = false;
    private CheckBox showPasswordCheckBox;

    public static void actionNewAccount(Context context) {
        Intent i = new Intent(context, AccountSetupBasics.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_basics);
        emailView = (EditText)findViewById(R.id.account_email);
        passwordView = (EditText)findViewById(R.id.account_password);
        clientCertificateCheckBox = (CheckBox)findViewById(R.id.account_client_certificate);
        clientCertificateSpinner = (ClientCertificateSpinner)findViewById(R.id.account_client_certificate_spinner);
        nextButton = (Button)findViewById(R.id.next);
        manualSetupButton = (Button)findViewById(R.id.manual_setup);
        showPasswordCheckBox = (CheckBox) findViewById(R.id.show_password);
        nextButton.setOnClickListener(this);
        manualSetupButton.setOnClickListener(this);
    }

    private void initializeViewListeners() {
        emailView.addTextChangedListener(this);
        passwordView.addTextChangedListener(this);
        clientCertificateCheckBox.setOnCheckedChangeListener(this);
        clientCertificateSpinner.setOnClientCertificateChangedListener(this);
        showPasswordCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showPassword(isChecked);
            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (account != null) {
            outState.putString(EXTRA_ACCOUNT, account.getUuid());
        }
        if (provider != null) {
            outState.putSerializable(STATE_KEY_PROVIDER, provider);
        }
        outState.putBoolean(STATE_KEY_CHECKED_INCOMING, checkedIncoming);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
            String accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
            account = Preferences.getPreferences(this).getAccount(accountUuid);
        }

        if (savedInstanceState.containsKey(STATE_KEY_PROVIDER)) {
            provider = (Provider) savedInstanceState.getSerializable(STATE_KEY_PROVIDER);
        }

        checkedIncoming = savedInstanceState.getBoolean(STATE_KEY_CHECKED_INCOMING);

        updateViewVisibility(clientCertificateCheckBox.isChecked());

        showPassword(showPasswordCheckBox.isChecked());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        /*
         * We wait until now to initialize the listeners because we didn't want
         * the OnCheckedChangeListener active while the
         * clientCertificateCheckBox state was being restored because it could
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

    /**
     * Called when checking the client certificate CheckBox
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        updateViewVisibility(isChecked);
        validateFields();

        // Have the user select (or confirm) the client certificate
        if (isChecked) {
            clientCertificateSpinner.chooseCertificate();
        }
    }

    private void updateViewVisibility(boolean usingCertificates) {
        if (usingCertificates) {
            // hide password fields, show client certificate spinner
            passwordView.setVisibility(View.GONE);
            showPasswordCheckBox.setVisibility(View.GONE);
            clientCertificateSpinner.setVisibility(View.VISIBLE);
        } else {
            // show password fields, hide client certificate spinner
            passwordView.setVisibility(View.VISIBLE);
            showPasswordCheckBox.setVisibility(View.VISIBLE);
            clientCertificateSpinner.setVisibility(View.GONE);
        }
    }

    private void showPassword(boolean show) {
        int cursorPosition = passwordView.getSelectionStart();
        if (show) {
            passwordView.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            passwordView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        passwordView.setSelection(cursorPosition);
    }

    private void validateFields() {
        boolean clientCertificateChecked = clientCertificateCheckBox.isChecked();
        String clientCertificateAlias = clientCertificateSpinner.getAlias();
        String email = emailView.getText().toString();

        boolean valid = Utility.requiredFieldValid(emailView)
                && ((!clientCertificateChecked && Utility.requiredFieldValid(passwordView))
                        || (clientCertificateChecked && clientCertificateAlias != null))
                && emailValidator.isValidAddressOnly(email);

        nextButton.setEnabled(valid);
        manualSetupButton.setEnabled(valid);
        /*
         * Dim the next button's icon to 50% if the button is disabled.
         * TODO this can probably be done with a stateful drawable. Check into it.
         * android:state_enabled
         */
        Utility.setCompoundDrawablesAlpha(nextButton, nextButton.isEnabled() ? 255 : 128);
    }

    private String getOwnerName() {
        String name = null;
        try {
            name = getDefaultAccountName();
        } catch (Exception e) {
            Timber.e(e, "Could not get default account name");
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
            if (provider != null && provider.note != null) {
                return new AlertDialog.Builder(this)
                       .setMessage(provider.note)
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
        String email = emailView.getText().toString();
        String password = passwordView.getText().toString();
        String[] emailParts = splitEmail(email);
        String user = emailParts[0];
        String domain = emailParts[1];
        try {
            String userEnc = UrlEncodingHelper.encodeUtf8(user);
            String passwordEnc = UrlEncodingHelper.encodeUtf8(password);
            String incomingUsername;
            if (provider.incomingUsernameTemplate.equals(ProviderInfo.USERNAME_TEMPLATE_SRV)) {
                incomingUsername = email;
            } else {
                incomingUsername = provider.incomingUsernameTemplate;
                incomingUsername = incomingUsername.replaceAll("\\$email", email);
                incomingUsername = incomingUsername.replaceAll("\\$user", userEnc);
                incomingUsername = incomingUsername.replaceAll("\\$domain", domain);
            }

            URI incomingUriTemplate = provider.incomingUriTemplate;
            URI incomingUri = new URI(incomingUriTemplate.getScheme(), incomingUsername + ":" + passwordEnc,
                    incomingUriTemplate.getHost(), incomingUriTemplate.getPort(), null, null, null);

            String outgoingUsername = provider.outgoingUsernameTemplate;

            URI outgoingUriTemplate = provider.outgoingUriTemplate;


            URI outgoingUri;
            if (outgoingUsername != null) {
                if (provider.outgoingUsernameTemplate.equals(ProviderInfo.USERNAME_TEMPLATE_SRV)) {
                    outgoingUsername = email;
                } else {
                    outgoingUsername = outgoingUsername.replaceAll("\\$email", email);
                    outgoingUsername = outgoingUsername.replaceAll("\\$user", userEnc);
                    outgoingUsername = outgoingUsername.replaceAll("\\$domain", domain);
                }

                outgoingUri = new URI(outgoingUriTemplate.getScheme(), outgoingUsername + ":"
                                      + passwordEnc, outgoingUriTemplate.getHost(), outgoingUriTemplate.getPort(), null,
                                      null, null);

            } else {
                outgoingUri = new URI(outgoingUriTemplate.getScheme(),
                                      null, outgoingUriTemplate.getHost(), outgoingUriTemplate.getPort(), null,
                                      null, null);


            }
            if (account == null) {
                account = Preferences.getPreferences(this).newAccount();
            }
            account.setName(getOwnerName());
            account.setEmail(email);
            account.setStoreUri(incomingUri.toString());
            account.setTransportUri(outgoingUri.toString());

            setupFolderNames(incomingUriTemplate.getHost().toLowerCase(Locale.US));

            ServerSettings incomingSettings = RemoteStore.decodeStoreUri(incomingUri.toString());
            account.setDeletePolicy(AccountCreator.getDefaultDeletePolicy(incomingSettings.type));

            // Check incoming here.  Then check outgoing in onActivityResult()
            AccountSetupCheckSettings.actionCheckSettings(this, account, CheckDirection.INCOMING);
        } catch (URISyntaxException use) {
            /*
             * If there is some problem with the URI we give up and go on to
             * manual setup.
             */
            onManualSetup();
        }
    }

    private void onNext() {
        if (clientCertificateCheckBox.isChecked()) {

            // Auto-setup doesn't support client certificates.
            onManualSetup();
            return;
        }

        String email = emailView.getText().toString();
        // String[] emailParts = splitEmail(email);
        // String domain = emailParts[1];

        findProvider(email);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (!checkedIncoming) {
                //We've successfully checked incoming.  Now check outgoing.
                checkedIncoming = true;
                AccountSetupCheckSettings.actionCheckSettings(this, account, CheckDirection.OUTGOING);
            } else {
                //We've successfully checked outgoing as well.
                account.setDescription(account.getEmail());
                account.save(Preferences.getPreferences(this));
                K9.setServicesEnabled(this);
                AccountSetupNames.actionSetNames(this, account);
                finish();
            }
        }
    }

    private void onManualSetup() {
        String email = emailView.getText().toString();
        String[] emailParts = splitEmail(email);
        String user = email;
        String domain = emailParts[1];

        String password = null;
        String clientCertificateAlias = null;
        AuthType authenticationType;
        if (clientCertificateCheckBox.isChecked()) {
            authenticationType = AuthType.EXTERNAL;
            clientCertificateAlias = clientCertificateSpinner.getAlias();
        } else {
            authenticationType = AuthType.PLAIN;
            password = passwordView.getText().toString();
        }

        if (account == null) {
            account = Preferences.getPreferences(this).newAccount();
        }
        account.setName(getOwnerName());
        account.setEmail(email);

        // set default uris
        // NOTE: they will be changed again in AccountSetupAccountType!
        ServerSettings storeServer = new ServerSettings(ServerSettings.Type.IMAP, "mail." + domain, -1,
                ConnectionSecurity.SSL_TLS_REQUIRED, authenticationType, user, password, clientCertificateAlias);
        ServerSettings transportServer = new ServerSettings(ServerSettings.Type.SMTP, "mail." + domain, -1,
                ConnectionSecurity.SSL_TLS_REQUIRED, authenticationType, user, password, clientCertificateAlias);
        String storeUri = RemoteStore.createStoreUri(storeServer);
        String transportUri = TransportUris.createTransportUri(transportServer);
        account.setStoreUri(storeUri);
        account.setTransportUri(transportUri);

        setupFolderNames(domain);

        AccountSetupAccountType.actionSelectAccountType(this, account, false);

        finish();
    }

    private void setupFolderNames(String domain) {
        account.setDraftsFolderName(getString(R.string.special_mailbox_name_drafts));
        account.setTrashFolderName(getString(R.string.special_mailbox_name_trash));
        account.setSentFolderName(getString(R.string.special_mailbox_name_sent));
        account.setArchiveFolderName(getString(R.string.special_mailbox_name_archive));

        // Yahoo! has a special folder for Spam, called "Bulk Mail".
        if (domain.endsWith(".yahoo.com")) {
            account.setSpamFolderName("Bulk Mail");
        } else {
            account.setSpamFolderName(getString(R.string.special_mailbox_name_spam));
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
     * Attempts to get the given attribute as a String resource first, and if it fails
     * returns the attribute as a simple String value.
     * @param xml
     * @param name
     * @return
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
            Timber.e(e, "Error while trying to load provider settings.");
        }
        return null;
    }

    private void findProvider(final String email) {
        new AsyncTask<Void, Void, ProviderInfo>() {
            @Override
            protected ProviderInfo doInBackground(Void... params) {
                String[] emailParts = splitEmail(email);
                final String domain = emailParts[1];

                ProviderInfo providerInfo;
                AutoconfigureMozilla autoconfigureMozilla = new AutoconfigureMozilla();
                AutoconfigureSrv autoconfigureSrv = new AutoconfigureSrv();
                AutoConfigureAutodiscover autodiscover = new AutoConfigureAutodiscover();

                provider = findProviderForDomain(domain);

                if (provider != null) return null;

                providerInfo = autoconfigureMozilla.findProviderInfo(email);
                if (providerInfo != null) return providerInfo;

                providerInfo = autoconfigureSrv.findProviderInfo(email);
                if (providerInfo != null) return providerInfo;

                providerInfo = autodiscover.findProviderInfo(email);

                return providerInfo;
            }

            @Override
            protected void onPostExecute(ProviderInfo providerInfo) {
                super.onPostExecute(providerInfo);

                if (providerInfo != null) {
                    try {
                        provider = Provider.newInstanceFromProviderInfo(providerInfo);
                    } catch (URISyntaxException e) {
                        Timber.e(e, "Error while converting providerInfo to provider");
                    }
                }

                if (provider == null) {
                    /*
                     * We don't have default settings for this account, start the manual
                     * setup process.
                     */
                    onManualSetup();
                    return;
                }

                if (provider.note != null) {
                    showDialog(DIALOG_NOTE);
                } else {
                    finishAutoSetup();
                }
            }
        }.execute();
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

        public static Provider newInstanceFromProviderInfo(@Nullable AutoConfigure.ProviderInfo providerInfo) throws URISyntaxException {
            if (providerInfo == null) return null;

            Provider provider = new Provider();

            provider.incomingUsernameTemplate = providerInfo.incomingUsernameTemplate;
            provider.outgoingUsernameTemplate = providerInfo.outgoingUsernameTemplate;

            provider.incomingUriTemplate = new URI(providerInfo.incomingType + "+"
                    + ("".equals(providerInfo.incomingSocketType) ? "" : (providerInfo.incomingSocketType + "+")),
                    null,
                    providerInfo.incomingAddr,
                    providerInfo.incomingPort,
                    null, null, null);
            provider.outgoingUriTemplate = new URI(providerInfo.outgoingType + "+"
                    + ("".equals(providerInfo.outgoingSocketType) ? "" : (providerInfo.outgoingSocketType + "+")),
                    null,
                    providerInfo.outgoingAddr,
                    providerInfo.outgoingPort,
                    null, null, null);

            return provider;
        }
    }

}
