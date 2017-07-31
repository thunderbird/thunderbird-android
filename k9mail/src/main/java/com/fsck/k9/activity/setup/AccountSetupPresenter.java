package com.fsck.k9.activity.setup;


import android.content.res.XmlResourceParser;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.fsck.k9.Account;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.EmailAddressValidator;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.account.AccountCreator;
import com.fsck.k9.activity.setup.AccountSetupContract.View;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.helper.EmailHelper;
import com.fsck.k9.helper.UrlEncodingHelper;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.NetworkType;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.TransportProvider;
import com.fsck.k9.mail.filter.Hex;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mail.store.imap.ImapStoreSettings;
import com.fsck.k9.mail.store.webdav.WebDavStore;
import com.fsck.k9.mail.store.webdav.WebDavStoreSettings;
import com.fsck.k9.service.MailService;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fsck.k9.setup.ServerNameSuggester;
import timber.log.Timber;

import static com.fsck.k9.mail.ServerSettings.Type.SMTP;
import static com.fsck.k9.mail.ServerSettings.Type.WebDAV;


public class AccountSetupPresenter implements AccountSetupContract.Presenter {

    private ServerSettings incomingSettings;
    private ServerSettings outgoingSettings;
    private boolean makeDefault;

    enum Stage {
        BASICS,
        AUTOCONFIGURATION,
        AUTOCONFIGURATION_INCOMING_CHECKING,
        AUTOCONFIGURATION_OUTGOING_CHECKING,
        INCOMING,
        INCOMING_CHECKING,
        OUTGOING,
        OUTGOING_CHECKING,
        ACCOUNT_TYPE,
        ACCOUNT_OPTIONS,
        ACCOUNT_NAMES,
    }

    private View view;

    private Account account;

    private CheckDirection currentDirection;
    private CheckDirection direction;
    private boolean editSettings;

    private String password;

    private ConnectionSecurity currentSecurityType;
    private AuthType currentAuthType;
    private String currentPort;

    /* private Type incomingType;
    private String incomingHost;
    private int incomingPort;
    private ConnectionSecurity incomingSecurity;
    private AuthType incomingAuthType;
    private String incomingUsername;
    private String incomingPassword;
    private String incomingCertificateAlias;

    private Type outgoingType;
    private String outgoingHost;
    private int outgoingPort;
    private ConnectionSecurity outgoingSecurity;
    private AuthType outgoingAuthType;
    private String outgoingUsername;
    private String outgoingPassword;
    private String outgoingCertificateAlias;*/

    private Stage stage;

    private boolean restoring;

    public AccountSetupPresenter(View view) {
        this.view = view;
    }

    @Override
    public void onInputChangedInBasics(String email, String password) {
        EmailAddressValidator emailValidator = new EmailAddressValidator();

        boolean valid = email != null && email.length() > 0
                && password != null && password.length() > 0
                && emailValidator.isValidAddressOnly(email);

        view.setNextButtonInBasicsEnabled(valid);
    }

    @Override
    public void onManualSetupButtonClicked(String email, String password) {
        if (account == null) {
            account = Preferences.getPreferences(K9.app).newAccount();
        }

        account.init(email, password);

        view.goToAccountType();
    }

    @Override
    public void onNextButtonInBasicViewClicked(String email, String password) {
        if (account == null) {
            account = Preferences.getPreferences(K9.app).newAccount();
        }
        account.setEmail(email);

        this.password = password;

        view.goToAutoConfiguration();
    }

    @Override
    public void setAccount(Account account) {
        this.account = account;
    }

    @Override
    public Account getAccount() {
        return account;
    }

    @Override
    public void onNegativeClickedInConfirmationDialog() {
        if (direction == CheckDirection.BOTH && currentDirection == CheckDirection.INCOMING) {
            checkOutgoing();
        } else if (currentDirection == CheckDirection.OUTGOING){
            if (editSettings) {
                account.save(Preferences.getPreferences(K9.app));
                view.end();
            } else {
                view.goToOptions();
            }
        } else {
            if (editSettings) {
                account.save(Preferences.getPreferences(K9.app));
                view.end();
            } else {
                view.goToOutgoing();
            }
        }
    }


    private void autoConfiguration() {
        view.setMessage(R.string.account_setup_check_settings_retr_info_msg);

        EmailHelper emailHelper = new EmailHelper();
        String domain = emailHelper.splitEmail(account.getEmail())[1];
        Provider provider = findProviderForDomain(domain);
        if (provider == null) {
            account.init(account.getEmail(), password);
            view.goToAccountType();
            return;
        }

        try {
            modifyAccount(account.getEmail(), password, provider);

            checkIncomingAndOutgoing();
        } catch (URISyntaxException e) {
            Preferences.getPreferences(K9.app).deleteAccount(account);
            view.goToAccountType();
        }
    }

    private void checkIncomingAndOutgoing() {
        direction = CheckDirection.BOTH;
        currentDirection = CheckDirection.INCOMING;
        new CheckIncomingTask(account, new CheckSettingsSuccessCallback() {
            @Override
            public void onCheckSuccess() {
                checkOutgoing();
            }
        }).execute();
    }

    private void checkIncoming() {
        direction = CheckDirection.INCOMING;
        currentDirection = CheckDirection.INCOMING;
        new CheckIncomingTask(account, new CheckSettingsSuccessCallback() {
            @Override
            public void onCheckSuccess() {
                if (editSettings) {
                    updateAccount();
                    view.end();
                } else {
                    account.save(Preferences.getPreferences(K9.app));

                    account.setDescription(account.getEmail());
                    K9.setServicesEnabled(K9.app);

                    view.goToOutgoing();
                }
            }
        }).execute();
    }

    private void checkOutgoing() {
        direction = CheckDirection.OUTGOING;
        currentDirection = CheckDirection.OUTGOING;

        new CheckOutgoingTask(account, new CheckSettingsSuccessCallback() {
            @Override
            public void onCheckSuccess() {
                if (!editSettings) {
                    //We've successfully checked outgoing as well.
                    account.setDescription(account.getEmail());
                    account.save(Preferences.getPreferences(K9.app));

                    K9.setServicesEnabled(K9.app);

                    view.goToOptions();
                } else {
                    updateAccount();

                    view.end();
                }
            }
        }).execute();
    }

    @Override
    public void onCheckingStart(Stage stage) {
        this.stage = stage;

        switch (stage) {
            case AUTOCONFIGURATION:
                autoConfiguration();
                break;
            case INCOMING_CHECKING:
                checkIncoming();
                break;
            case OUTGOING_CHECKING:
                checkOutgoing();
                break;
        }
    }


    private class CheckOutgoingTask extends CheckAccountTask {

        private CheckOutgoingTask(Account account) {
            super(account);
        }

        private CheckOutgoingTask(Account account, CheckSettingsSuccessCallback callback) {
            super(account, callback);
        }

        @Override
        void checkSettings() throws Exception {
            clearCertificateErrorNotifications(CheckDirection.OUTGOING);

            if (!(account.getRemoteStore() instanceof WebDavStore)) {
                publishProgress(R.string.account_setup_check_settings_check_outgoing_msg);
            }
            Transport transport = TransportProvider.getInstance().getTransport(K9.app, account);
            transport.close();
            try {
                transport.open();
            } finally {
                transport.close();
            }
        }
    }

    private class CheckIncomingTask extends CheckAccountTask {
        private CheckIncomingTask(Account account) {
            super(account);
        }

        private CheckIncomingTask(Account account, CheckSettingsSuccessCallback callback) {
            super(account, callback);
        }

        @Override
        void checkSettings() throws Exception {

            clearCertificateErrorNotifications(CheckDirection.INCOMING);

            Store store = account.getRemoteStore();
            if (store instanceof WebDavStore) {
                publishProgress(R.string.account_setup_check_settings_authenticate);
            } else {
                publishProgress(R.string.account_setup_check_settings_check_incoming_msg);
            }
            store.checkSettings();

            if (store instanceof WebDavStore) {
                publishProgress(R.string.account_setup_check_settings_fetch);
            }
            MessagingController.getInstance(K9.app).listFoldersSynchronous(account, true, null);
            MessagingController.getInstance(K9.app)
                    .synchronizeMailbox(account, account.getInboxFolderName(), null, null);
        }
    }

    /**
     * FIXME: Don't use an AsyncTask to perform network operations.
     * See also discussion in https://github.com/k9mail/k-9/pull/560
     */
    private abstract class CheckAccountTask extends AsyncTask<CheckDirection, Integer, Boolean> {
        private final Account account;
        private CheckSettingsSuccessCallback callback;

        private CheckAccountTask(Account account) {
            this(account, null);
        }

        private CheckAccountTask(Account account, CheckSettingsSuccessCallback callback) {
            this.account = account;
            this.callback = callback;
        }

        abstract void checkSettings() throws Exception;

        @Override
        protected Boolean doInBackground(CheckDirection... params) {
            try {
                /*
                 * This task could be interrupted at any point, but network operations can block,
                 * so relying on InterruptedException is not enough. Instead, check after
                 * each potentially long-running operation.
                 */
                if (cancelled()) {
                    return false;
                }

                checkSettings();

                if (cancelled()) {
                    return false;
                }

                return true;

            } catch (AuthenticationFailedException afe) {
                Timber.e(afe, "Error while testing settings");
                view.showErrorDialog(
                        R.string.account_setup_failed_dlg_auth_message_fmt,
                        afe.getMessage() == null ? "" : afe.getMessage());
            } catch (CertificateValidationException cve) {
                handleCertificateValidationException(cve);
            } catch (Exception e) {
                Timber.e(e, "Error while testing settings");
                String message = e.getMessage() == null ? "" : e.getMessage();
                view.showErrorDialog(R.string.account_setup_failed_dlg_server_message_fmt, message);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            super.onPostExecute(bool);

            if (bool && callback != null) {
                callback.onCheckSuccess();
            }
        }

        void clearCertificateErrorNotifications(CheckDirection direction) {
            final MessagingController ctrl = MessagingController.getInstance(K9.app);
            ctrl.clearCertificateErrorNotifications(account, direction);
        }

        private boolean cancelled() {
            return view.canceled();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            view.setMessage(values[0]);
        }


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
        Account account = Preferences.getPreferences(K9.app).getDefaultAccount();
        if (account != null) {
            name = account.getName();
        }
        return name;
    }

    private String getXmlAttribute(XmlResourceParser xml, String name) {
        int resId = xml.getAttributeResourceValue(null, name, 0);
        if (resId == 0) {
            return xml.getAttributeValue(null, name);
        } else {
            return K9.app.getString(resId);
        }
    }

    private Provider findProviderForDomain(String domain) {
        try {
            XmlResourceParser xml = K9.app.getResources().getXml(R.xml.providers);
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

    private void modifyAccount(String email, String password, @NonNull Provider provider) throws URISyntaxException {
        EmailHelper emailHelper = new EmailHelper();
        String[] emailParts = emailHelper.splitEmail(email);
        String user = emailParts[0];
        String domain = emailParts[1];
        String userEnc = UrlEncodingHelper.encodeUtf8(user);
        String passwordEnc = UrlEncodingHelper.encodeUtf8(password);

        String incomingUsername = provider.incomingUsernameTemplate;
        incomingUsername = incomingUsername.replaceAll("\\$email", email);
        incomingUsername = incomingUsername.replaceAll("\\$user", userEnc);
        incomingUsername = incomingUsername.replaceAll("\\$domain", domain);

        URI incomingUriTemplate = provider.incomingUriTemplate;
        URI incomingUri = new URI(incomingUriTemplate.getScheme(), incomingUsername + ":" + passwordEnc,
                incomingUriTemplate.getHost(), incomingUriTemplate.getPort(), null, null, null);

        String outgoingUsername = provider.outgoingUsernameTemplate;

        URI outgoingUriTemplate = provider.outgoingUriTemplate;


        URI outgoingUri;
        if (outgoingUsername != null) {
            outgoingUsername = outgoingUsername.replaceAll("\\$email", email);
            outgoingUsername = outgoingUsername.replaceAll("\\$user", userEnc);
            outgoingUsername = outgoingUsername.replaceAll("\\$domain", domain);
            outgoingUri = new URI(outgoingUriTemplate.getScheme(), outgoingUsername + ":"
                    + passwordEnc, outgoingUriTemplate.getHost(), outgoingUriTemplate.getPort(), null,
                    null, null);

        } else {
            outgoingUri = new URI(outgoingUriTemplate.getScheme(),
                    null, outgoingUriTemplate.getHost(), outgoingUriTemplate.getPort(), null,
                    null, null);

        }

        account.setName(getOwnerName());
        account.setEmail(email);
        account.setStoreUri(incomingUri.toString());
        account.setTransportUri(outgoingUri.toString());

        setupFolderNames(incomingUriTemplate.getHost().toLowerCase(Locale.US));

        ServerSettings incomingSettings = RemoteStore.decodeStoreUri(incomingUri.toString());
        account.setDeletePolicy(AccountCreator.getDefaultDeletePolicy(incomingSettings.type));
    }

    private void setupFolderNames(String domain) {
        account.setDraftsFolderName(K9.getK9String(R.string.special_mailbox_name_drafts));
        account.setTrashFolderName(K9.getK9String(R.string.special_mailbox_name_trash));
        account.setSentFolderName(K9.getK9String(R.string.special_mailbox_name_sent));
        account.setArchiveFolderName(K9.getK9String(R.string.special_mailbox_name_archive));

        // Yahoo! has a special folder for Spam, called "Bulk Mail".
        if (domain.endsWith(".yahoo.com")) {
            account.setSpamFolderName("Bulk Mail");
        } else {
            account.setSpamFolderName(K9.getK9String(R.string.special_mailbox_name_spam));
        }
    }

    @Override
    public void onCertificateAccepted(X509Certificate certificate) {
        try {
            account.addCertificate(currentDirection, certificate);
        } catch (CertificateException e) {
            view.showErrorDialog(
                    R.string.account_setup_failed_dlg_certificate_message_fmt,
                    e.getMessage() == null ? "" : e.getMessage());
        }

        replayChecking();
    }

    @Override
    public void onCertificateRefused() {
        if (stage == Stage.INCOMING_CHECKING) {
            view.goToIncoming();
        } else if (stage == Stage.OUTGOING_CHECKING) {
            view.goToOutgoing();
        }
    }

    @Override
    public void onGetAccountUuid(String accountUuid) {
        account = Preferences.getPreferences(K9.app).getAccount(accountUuid);
    }

    @Override
    public void onRestoreStart() {
        restoring = true;
    }

    @Override
    public void onRestoreEnd() {
        restoring = false;
    }

    @Override
    public void onMakeDefault() {
        makeDefault = true;
    }

    @Override
    public void onPositiveClickedInConfirmationDialog() {
        if (stage == Stage.INCOMING_CHECKING) {
            view.goToIncoming();
        } else if (stage == Stage.OUTGOING_CHECKING){
            view.goToOutgoing();
        } else {
            view.goToBasics();
        }
    }

    private void replayChecking() {
        if (direction == CheckDirection.BOTH && currentDirection == CheckDirection.INCOMING) {
            checkIncomingAndOutgoing();
        } else if (currentDirection == CheckDirection.INCOMING) {
            checkIncoming();
        } else {
            checkOutgoing();
        }
    }

    private String errorMessageForCertificateException(CertificateValidationException e) {
        switch (e.getReason()) {
            case Expired: return K9.getK9String(R.string.client_certificate_expired, e.getAlias(), e.getMessage());
            case MissingCapability: return K9.getK9String(R.string.auth_external_error);
            case RetrievalFailure: return K9.getK9String(R.string.client_certificate_retrieval_failure, e.getAlias());
            case UseMessage: return e.getMessage();
            case Unknown:
            default: return "";
        }
    }

    private void acceptKeyDialog(final int msgResId, final CertificateValidationException ex) {
        String exMessage = "Unknown Error";

        if (ex != null) {
            if (ex.getCause() != null) {
                if (ex.getCause().getCause() != null) {
                    exMessage = ex.getCause().getCause().getMessage();

                } else {
                    exMessage = ex.getCause().getMessage();
                }
            } else {
                exMessage = ex.getMessage();
            }
        }

        StringBuilder chainInfo = new StringBuilder(100);
        MessageDigest sha1 = null;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            Timber.e(e, "Error while initializing MessageDigest");
        }

        final X509Certificate[] chain = ex.getCertChain();
        // We already know chain != null (tested before calling this method)
        for (int i = 0; i < chain.length; i++) {
            // display certificate chain information
            //TODO: localize this strings
            chainInfo.append("Certificate chain[").append(i).append("]:\n");
            chainInfo.append("Subject: ").append(chain[i].getSubjectDN().toString()).append("\n");

            // display SubjectAltNames too
            // (the user may be mislead into mistrusting a certificate
            //  by a subjectDN not matching the server even though a
            //  SubjectAltName matches)
            try {
                final Collection<List<? >> subjectAlternativeNames = chain[i].getSubjectAlternativeNames();
                if (subjectAlternativeNames != null) {
                    // The list of SubjectAltNames may be very long
                    //TODO: localize this string
                    StringBuilder altNamesText = new StringBuilder();
                    altNamesText.append("Subject has ").append(subjectAlternativeNames.size()).append(" alternative names\n");

                    // we need these for matching

                    for (List<?> subjectAlternativeName : subjectAlternativeNames) {
                        Integer type = (Integer)subjectAlternativeName.get(0);
                        Object value = subjectAlternativeName.get(1);
                        String name;
                        switch (type.intValue()) {
                            case 0:
                                Timber.w("SubjectAltName of type OtherName not supported.");
                                continue;
                            case 1: // RFC822Name
                                name = (String)value;
                                break;
                            case 2:  // DNSName
                                name = (String)value;
                                break;
                            case 3:
                                Timber.w("unsupported SubjectAltName of type x400Address");
                                continue;
                            case 4:
                                Timber.w("unsupported SubjectAltName of type directoryName");
                                continue;
                            case 5:
                                Timber.w("unsupported SubjectAltName of type ediPartyName");
                                continue;
                            case 6:  // Uri
                                name = (String)value;
                                break;
                            case 7: // ip-address
                                name = (String)value;
                                break;
                            default:
                                Timber.w("unsupported SubjectAltName of unknown type");
                                continue;
                        }

                        // if some of the SubjectAltNames match the store or transport -host,
                        // display them
                        if (name.equalsIgnoreCase(incomingSettings.host) || name.equalsIgnoreCase(outgoingSettings.host)) {
                            //TODO: localize this string
                            altNamesText.append("Subject(alt): ").append(name).append(",...\n");
                        } else if (name.startsWith("*.") && (
                                (incomingSettings != null && incomingSettings.host.endsWith(name.substring(2))) ||
                                        (outgoingSettings != null && outgoingSettings.host.endsWith(name.substring(2))))) {
                            //TODO: localize this string
                            altNamesText.append("Subject(alt): ").append(name).append(",...\n");
                        }
                    }
                    chainInfo.append(altNamesText);
                }
            } catch (Exception e1) {
                // don't fail just because of subjectAltNames
                Timber.w(e1, "cannot display SubjectAltNames in dialog");
            }

            chainInfo.append("Issuer: ").append(chain[i].getIssuerDN().toString()).append("\n");
            if (sha1 != null) {
                sha1.reset();
                try {
                    String sha1sum = Hex.encodeHex(sha1.digest(chain[i].getEncoded()));
                    chainInfo.append("Fingerprint (SHA-1): ").append(sha1sum).append("\n");
                } catch (CertificateEncodingException e) {
                    Timber.e(e, "Error while encoding certificate");
                }
            }

        }

        view.showAcceptKeyDialog(msgResId, exMessage, chainInfo.toString(), chain[0]);
    }

    private void handleCertificateValidationException(CertificateValidationException cve) {
        Timber.e(cve, "Error while testing settings");

        X509Certificate[] chain = cve.getCertChain();
        // Avoid NullPointerException in acceptKeyDialog()
        if (chain != null) {
            acceptKeyDialog(
                    R.string.account_setup_failed_dlg_certificate_message_fmt,
                    cve);
        } else {
            view.showErrorDialog(
                    R.string.account_setup_failed_dlg_server_message_fmt,
                    errorMessageForCertificateException(cve));
        }
    }

    private static class Provider implements Serializable {
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

    private interface CheckSettingsSuccessCallback {
        void onCheckSuccess();
    }

    // incoming

    @Override
    public void onIncomingStart(boolean editSettings) {
        this.editSettings = editSettings;

        stage = Stage.INCOMING;
        ConnectionSecurity[] connectionSecurityChoices = ConnectionSecurity.values();
        try {
            incomingSettings = RemoteStore.decodeStoreUri(account.getStoreUri());

            /* if (savedInstanceState == null) {
                // The first item is selected if settings.authenticationType is null or is not in authTypeAdapter
                currentAuthTypeViewPosition = authTypeAdapter.getAuthPosition(settings.authenticationType);
            } else {
                currentAuthTypeViewPosition = savedInstanceState.getInt(STATE_AUTH_TYPE_POSITION);
            } */

            currentAuthType = incomingSettings.authenticationType;

            view.setAuthType(currentAuthType);

            updateViewFromAuthTypeInIncoming(currentAuthType);

            currentSecurityType = incomingSettings.connectionSecurity;

            if (incomingSettings.username != null) {
                view.setUsernameInIncoming(incomingSettings.username);
            }

            if (incomingSettings.password != null) {
                view.setPasswordInIncoming(incomingSettings.password);
            }

            if (incomingSettings.clientCertificateAlias != null) {
                view.setCertificateAliasInIncoming(incomingSettings.clientCertificateAlias);
            }

            if (Type.POP3 == incomingSettings.type) {
                view.setServerLabel(getString(R.string.account_setup_incoming_pop_server_label));

                view.hideViewsWhenPop3();
            } else if (Type.IMAP == incomingSettings.type) {
                view.setServerLabel(getString(R.string.account_setup_incoming_imap_server_label));

                ImapStoreSettings imapSettings = (ImapStoreSettings) incomingSettings;

                view.setImapAutoDetectNamespace(imapSettings.autoDetectNamespace);
                if (imapSettings.pathPrefix != null) {
                    view.setImapPathPrefix(imapSettings.pathPrefix);
                }

                view.hideViewsWhenImap();

                if (!editSettings) {
                    view.hideViewsWhenImapAndNotEdit();
                }
            } else if (Type.WebDAV == incomingSettings.type) {
                view.setServerLabel(getString(R.string.account_setup_incoming_webdav_server_label));
                connectionSecurityChoices = new ConnectionSecurity[] {
                        ConnectionSecurity.NONE,
                        ConnectionSecurity.SSL_TLS_REQUIRED };

                // Hide the unnecessary fields
                view.hideViewsWhenWebDav();
                WebDavStoreSettings webDavSettings = (WebDavStoreSettings) incomingSettings;

                if (webDavSettings.path != null) {
                    view.setWebDavPathPrefix(webDavSettings.path);
                }

                if (webDavSettings.authPath != null) {
                    view.setWebDavAuthPath(webDavSettings.authPath);
                }

                if (webDavSettings.mailboxPath != null) {
                    view.setWebDavMailboxPath(webDavSettings.mailboxPath);
                }
            } else {
                throw new IllegalArgumentException("Unknown account type: " + account.getStoreUri());
            }

            if (!editSettings) {
                account.setDeletePolicy(AccountCreator.getDefaultDeletePolicy(incomingSettings.type));
            }

            view.setSecurityChoices(connectionSecurityChoices);

            view.setSecurityTypeInIncoming(currentSecurityType);
            updateAuthPlainTextFromSecurityType(currentSecurityType);

            if (incomingSettings.host != null) {
                view.setServerInIncoming(incomingSettings.host);
            }

            if (incomingSettings.port != -1) {
                String port = String.valueOf(incomingSettings.port);
                view.setPortInIncoming(port);
                currentPort = port;
            } else {
                updatePortFromSecurityTypeInIncoming(currentSecurityType);
            }

            view.setCompressionMobile(account.useCompression(NetworkType.MOBILE));
            view.setCompressionWifi(account.useCompression(NetworkType.WIFI));
            view.setCompressionOther(account.useCompression(NetworkType.OTHER));

            view.setSubscribedFoldersOnly(account.subscribedFoldersOnly());

        } catch (IllegalArgumentException e) {
            view.showFailureToast(e);
        }
    }

    @Override
    public void onIncomingStart() {
        onIncomingStart(false);
    }

    private void updatePortFromSecurityTypeInIncoming(ConnectionSecurity securityType) {
        if (restoring) return;

        String port = String.valueOf(AccountCreator.getDefaultPort(securityType, incomingSettings.type));
        view.setPortInIncoming(port);
        currentPort = port;
    }

    private void updateAuthPlainTextFromSecurityType(ConnectionSecurity securityType) {
        view.setAuthTypeInsecureText(securityType == ConnectionSecurity.NONE);
    }

    @Override
    public IncomingAndOutgoingState getState() {
        return new IncomingAndOutgoingState(currentAuthType, currentSecurityType);
    }

    @Override
    public void setState(IncomingAndOutgoingState state) {
        view.setAuthType(state.getAuthType());
        view.setSecurityTypeInIncoming(state.getConnectionSecurity());

        currentAuthType = state.getAuthType();
        currentSecurityType = state.getConnectionSecurity();
    }


    @Override
    public void onInputChangedInIncoming(String certificateAlias, String server, String port,
                                         String username, String password, AuthType authType,
                                         ConnectionSecurity connectionSecurity) {

        revokeInvalidSettingsAndUpdateViewInIncoming(authType, connectionSecurity, port);
        validateFieldInIncoming(certificateAlias, server, currentPort, username, password, currentAuthType,
                currentSecurityType);
    }

    @Override
    public void onNextInIncomingClicked(String username, String password, String clientCertificateAlias,
                              boolean autoDetectNamespace, String imapPathPrefix, String webdavPathPrefix, String webdavAuthPath,
                              String webdavMailboxPath, String host, int port, ConnectionSecurity connectionSecurity,
                              AuthType authType, boolean compressMobile, boolean compressWifi, boolean compressOther,
                              boolean subscribedFoldersOnly) {

        if (authType == AuthType.EXTERNAL) {
            password = null;
        } else {
            clientCertificateAlias = null;
        }

        Map<String, String> extra = null;
        if (Type.IMAP == incomingSettings.type) {
            extra = new HashMap<>();
            extra.put(ImapStoreSettings.AUTODETECT_NAMESPACE_KEY,
                    Boolean.toString(autoDetectNamespace));
            extra.put(ImapStoreSettings.PATH_PREFIX_KEY,
                    imapPathPrefix);
        } else if (WebDAV == incomingSettings.type) {
            extra = new HashMap<>();
            extra.put(WebDavStoreSettings.PATH_KEY,
                    webdavPathPrefix);
            extra.put(WebDavStoreSettings.AUTH_PATH_KEY,
                    webdavAuthPath);
            extra.put(WebDavStoreSettings.MAILBOX_PATH_KEY,
                    webdavMailboxPath);
        }

        account.deleteCertificate(host, port, CheckDirection.INCOMING);
        incomingSettings = new ServerSettings(incomingSettings.type, host, port,
                connectionSecurity, authType, username, password, clientCertificateAlias, extra);

        account.setStoreUri(RemoteStore.createStoreUri(incomingSettings));

        account.setCompression(NetworkType.MOBILE, compressMobile);
        account.setCompression(NetworkType.WIFI, compressWifi);
        account.setCompression(NetworkType.OTHER, compressOther);
        account.setSubscribedFoldersOnly(subscribedFoldersOnly);
    }

    private void revokeInvalidSettingsAndUpdateViewInIncoming(AuthType authType,
                                                              ConnectionSecurity connectionSecurity,
                                                              String port) {
        boolean isAuthTypeExternal = (AuthType.EXTERNAL == authType);

        boolean hasConnectionSecurity = (connectionSecurity != ConnectionSecurity.NONE);

        if (isAuthTypeExternal && !hasConnectionSecurity) {
            view.showInvalidSettingsToast();
            view.setAuthType(currentAuthType);
            view.setSecurityTypeInIncoming(currentSecurityType);
            view.setPortInIncoming(currentPort);
        } else {
            onAuthTypeSelected(authType);
            onSecuritySelectedInIncoming(connectionSecurity);
            currentAuthType = authType;
            currentSecurityType = connectionSecurity;
            currentPort = port;
        }
    }

    private void onSecuritySelectedInIncoming(ConnectionSecurity securityType) {
        if (securityType != currentSecurityType) {
            setSecurityTypeInIncoming(securityType);
        }
    }

    private void setSecurityTypeInIncoming(ConnectionSecurity securityType) {
        view.setSecurityTypeInIncoming(securityType);

        updatePortFromSecurityTypeInIncoming(securityType);
        updateAuthPlainTextFromSecurityType(securityType);
    }


    private void validateFieldInIncoming(String certificateAlias, String server, String port,
                                String username, String password, AuthType authType, ConnectionSecurity connectionSecurity) {
        boolean isAuthTypeExternal = (AuthType.EXTERNAL == authType);
        boolean hasConnectionSecurity = (connectionSecurity != ConnectionSecurity.NONE);

        boolean hasValidCertificateAlias = certificateAlias != null;
        boolean hasValidUserName = Utility.requiredFieldValid(username);

        boolean hasValidPasswordSettings = hasValidUserName
                && !isAuthTypeExternal
                && Utility.requiredFieldValid(password);

        boolean hasValidExternalAuthSettings = hasValidUserName
                && isAuthTypeExternal
                && hasConnectionSecurity
                && hasValidCertificateAlias;

        final boolean enabled = Utility.domainFieldValid(server)
                && Utility.requiredFieldValid(port)
                && (hasValidPasswordSettings || hasValidExternalAuthSettings);

        view.setNextButtonInIncomingEnabled(enabled);
    }

    private void updateAccount() {
        boolean isPushCapable = false;
        try {
            Store store = account.getRemoteStore();
            isPushCapable = store.isPushCapable();
        } catch (Exception e) {
            Timber.e(e, "Could not get remote store");
        }
        if (isPushCapable && account.getFolderPushMode() != FolderMode.NONE) {
            MailService.actionRestartPushers(view.getContext(), null);
        }
        account.save(Preferences.getPreferences(K9.app));
    }

    private void updateViewFromAuthTypeInIncoming(AuthType authType) {
        if (authType == AuthType.EXTERNAL) {
            view.setViewExternalInIncoming();
        } else {
            view.setViewNotExternalInIncoming();
        }
    }
    private String getString(int id) {
        return K9.app.getString(id);
    }

    // names

    @Override
    public void onNamesStart() {
        stage = Stage.ACCOUNT_NAMES;
    }

    @Override
    public void onNextButtonInNamesClicked(String name, String description) {
        if (Utility.requiredFieldValid(description)) {
            account.setDescription(description);
        }

        account.setName(name);
        account.save(Preferences.getPreferences(K9.app));

        view.goToListAccounts();
    }

    @Override
    public void onOptionsStart() {
        stage = Stage.ACCOUNT_OPTIONS;

        view.setNotifyViewChecked(account.isNotifyNewMail());
        view.setNotifySyncViewChecked(account.isShowOngoing());
        view.setCheckFrequencyViewValue(account.getAutomaticCheckIntervalMinutes());
        view.setDisplayCountViewValue(account.getDisplayCount());

        boolean isPushCapable = false;
        try {
            Store store = account.getRemoteStore();
            isPushCapable = store.isPushCapable();
        } catch (Exception e) {
            Timber.e(e, "Could not get remote store");
        }

        if (!isPushCapable) {
            view.setPushEnableVisibility(android.view.View.GONE);
        } else {
            view.setPushEnableChecked(true);
        }
    }

    // options
    @Override
    public void onNextButtonInOptionsClicked(boolean isNotifyViewChecked, boolean isNotifySyncViewClicked,
                                    int checkFrequencyViewSelectedValue, int displayCountViewSelectedValue,
                                    boolean isPushEnableClicked) {
        account.setDescription(account.getEmail());
        account.setNotifyNewMail(isNotifyViewChecked);
        account.setShowOngoing(isNotifySyncViewClicked);
        account.setAutomaticCheckIntervalMinutes(checkFrequencyViewSelectedValue);
        account.setDisplayCount(displayCountViewSelectedValue);

        if (isPushEnableClicked) {
            account.setFolderPushMode(Account.FolderMode.FIRST_CLASS);
        } else {
            account.setFolderPushMode(Account.FolderMode.NONE);
        }

        account.save(Preferences.getPreferences(K9.app));
        if (account.equals(Preferences.getPreferences(K9.app).getDefaultAccount()) ||
                makeDefault) {
            Preferences.getPreferences(K9.app).setDefaultAccount(account);
        }
        K9.setServicesEnabled(K9.app);

        view.goToAccountNames();
    }


    // outgoing
    @Override
    public void onOutgoingStart() {
        onOutgoingStart(false);
    }

    @Override
    public void onOutgoingStart(boolean editSettings) {
        this.editSettings = editSettings;
        stage = Stage.OUTGOING;
        analysisAccount();
    }

    private void analysisAccount() {
        try {
            if (new URI(account.getStoreUri()).getScheme().startsWith("webdav")) {
                account.setTransportUri(account.getStoreUri());
                view.goToOutgoingChecking();

                return;
            }
        } catch (URISyntaxException e) {
            view.showFailureToast(e);
        }

        try {
            outgoingSettings = Transport.decodeTransportUri(account.getTransportUri());

            currentAuthType = outgoingSettings.authenticationType;
            setAuthType(currentAuthType);

            currentSecurityType = outgoingSettings.connectionSecurity;
            setSecurityTypeInOutgoing(currentSecurityType);

            if (outgoingSettings.username != null && !outgoingSettings.username.isEmpty()) {
                view.setUsernameInOutgoing(outgoingSettings.username);
            }

            if (outgoingSettings.password != null) {
                view.setPasswordInOutgoing(outgoingSettings.password);
            }

            if (outgoingSettings.clientCertificateAlias != null) {
                view.setCertificateAliasInOutgoing(outgoingSettings.clientCertificateAlias);
            }

            if (outgoingSettings.host != null) {
                view.setServerInOutgoing(outgoingSettings.host);
            }

            if (outgoingSettings.port != -1) {
                currentPort = String.valueOf(outgoingSettings.port);
                view.setPortInOutgoing(currentPort);
            }
        } catch (Exception e) {
            view.showFailureToast(e);
        }
    }


    private void setAuthType(AuthType authType) {
        view.setAuthTypeInOutgoing(authType);

        updateViewFromAuthTypeInOutgoing(authType);
    }

    private void setSecurityTypeInOutgoing(ConnectionSecurity securityType) {
        view.setSecurityTypeInOutgoing(securityType);

        updateViewFromSecurityTypeInOutgoing(securityType);
    }

    private void onSecuritySelectedInOutgoing(ConnectionSecurity securityType) {
        if (securityType != currentSecurityType) {
            updateViewFromSecurityTypeInOutgoing(securityType);
        }
    }

    private void onAuthTypeSelected(AuthType authType) {
        if (authType != currentAuthType) {
            setAuthType(authType);
        }
    }

    @Override
    public void onNextInOutgoingClicked(String username, String password, String clientCertificateAlias,
                       String host, int port, ConnectionSecurity connectionSecurity,
                       AuthType authType, boolean requireLogin) {

        if (!requireLogin) {
            username = null;
            password = null;
            authType = null;
            clientCertificateAlias = null;
        }

        account.deleteCertificate(host, port, CheckDirection.OUTGOING);
        ServerSettings server = new ServerSettings(Type.SMTP, host, port, connectionSecurity,
                authType, username, password, clientCertificateAlias);
        String uri = Transport.createTransportUri(server);
        account.setTransportUri(uri);

        view.goToOutgoingChecking();
    }

    @Override
    public void onInputChangedInOutgoing(String certificateAlias, String server, String port, String username,
                                         String password, AuthType authType,
                                         ConnectionSecurity connectionSecurity, boolean requireLogin) {

        if (currentSecurityType != connectionSecurity) {
            boolean isAuthTypeExternal = (AuthType.EXTERNAL == authType);

            boolean hasConnectionSecurity = (connectionSecurity != ConnectionSecurity.NONE);

            if (isAuthTypeExternal && !hasConnectionSecurity && !requireLogin) {
                authType = AuthType.PLAIN;
                view.setAuthTypeInOutgoing(authType);
                updateViewFromAuthTypeInOutgoing(authType);
            }
        }

        revokeInvalidSettingsAndUpdateViewInOutgoing(authType, connectionSecurity, port);
        validateFieldInOutgoing(certificateAlias, server, currentPort, username, password, currentAuthType,
                currentSecurityType, requireLogin);
    }

    private void validateFieldInOutgoing(String certificateAlias, String server, String port,
                                String username, String password, AuthType authType,
                                ConnectionSecurity connectionSecurity,
                                boolean requireLogin) {

        boolean isAuthTypeExternal = (AuthType.EXTERNAL == authType);
        boolean hasConnectionSecurity = (connectionSecurity != ConnectionSecurity.NONE);

        boolean hasValidCertificateAlias = certificateAlias != null;
        boolean hasValidUserName = Utility.requiredFieldValid(username);

        boolean hasValidPasswordSettings = hasValidUserName
                && !isAuthTypeExternal
                && Utility.requiredFieldValid(password);

        boolean hasValidExternalAuthSettings = hasValidUserName
                && isAuthTypeExternal
                && hasConnectionSecurity
                && hasValidCertificateAlias;

        boolean enabled = Utility.domainFieldValid(server)
                && Utility.requiredFieldValid(port)
                && (!requireLogin
                || hasValidPasswordSettings || hasValidExternalAuthSettings);

        view.setNextButtonInOutgoingEnabled(enabled);
    }

    private void revokeInvalidSettingsAndUpdateViewInOutgoing(AuthType authType,
                                                    ConnectionSecurity connectionSecurity,
                                                    String port) {
        boolean isAuthTypeExternal = (AuthType.EXTERNAL == authType);

        boolean hasConnectionSecurity = (connectionSecurity != ConnectionSecurity.NONE);

        if (isAuthTypeExternal && !hasConnectionSecurity) {
            view.showInvalidSettingsToastInOutgoing();
            view.setAuthTypeInOutgoing(currentAuthType);
            view.setSecurityTypeInOutgoing(currentSecurityType);
            view.setPortInOutgoing(currentPort);
        } else {
            onAuthTypeSelected(authType);
            onSecuritySelectedInOutgoing(connectionSecurity);
            currentAuthType = authType;
            currentSecurityType = connectionSecurity;
            currentPort = port;
        }
    }

    private void updateViewFromSecurityTypeInOutgoing(ConnectionSecurity securityType) {
        view.updateAuthPlainTextInOutgoing(securityType == ConnectionSecurity.NONE);

        if (restoring) return;
        String port = String.valueOf(AccountCreator.getDefaultPort(securityType, SMTP));
        view.setPortInOutgoing(port);
        currentPort = port;
    }

    private void updateViewFromAuthTypeInOutgoing(AuthType authType) {
        if (authType == AuthType.EXTERNAL) {
            view.setViewExternalInOutgoing();
        } else {
            view.setViewNotExternalInOutgoing();
        }
    }

    // account type
    @Override
    public void onAccountTypeStart() {
        stage = Stage.ACCOUNT_TYPE;
    }

    @Override
    public void onImapOrPop3Selected(Type serverType, String schemePrefix) throws URISyntaxException {
        ServerNameSuggester serverNameSuggester = new ServerNameSuggester();

        String domainPart = EmailHelper.getDomainFromEmailAddress(account.getEmail());

        String suggestedStoreServerName = serverNameSuggester.suggestServerName(serverType, domainPart);
        URI storeUriForDecode = new URI(account.getStoreUri());
        URI storeUri = new URI(schemePrefix, storeUriForDecode.getUserInfo(), suggestedStoreServerName,
                storeUriForDecode.getPort(), null, null, null);
        account.setStoreUri(storeUri.toString());

        String suggestedTransportServerName = serverNameSuggester.suggestServerName(SMTP, domainPart);
        URI transportUriForDecode = new URI(account.getTransportUri());
        URI transportUri = new URI("smtp+tls+", transportUriForDecode.getUserInfo(), suggestedTransportServerName,
                transportUriForDecode.getPort(), null, null, null);
        account.setTransportUri(transportUri.toString());

        view.goToIncomingSettings();
    }

    @Override
    public void onWebdavSelected() throws URISyntaxException {
        ServerNameSuggester serverNameSuggester = new ServerNameSuggester();

        URI uriForDecode = new URI(account.getStoreUri());

        /*
         * The user info we have been given from
         * BasicsView.onManualSetup() is encoded as an IMAP store
         * URI: AuthType:UserName:Password (no fields should be empty).
         * However, AuthType is not applicable to WebDAV nor to its store
         * URI. Re-encode without it, using just the UserName and Password.
         */
        String userPass = "";
        String[] userInfo = uriForDecode.getUserInfo().split(":");
        if (userInfo.length > 1) {
            userPass = userInfo[1];
        }
        if (userInfo.length > 2) {
            userPass = userPass + ":" + userInfo[2];
        }

        String domainPart = EmailHelper.getDomainFromEmailAddress(account.getEmail());
        String suggestedServerName = serverNameSuggester.suggestServerName(WebDAV, domainPart);
        URI uri = new URI("webdav+ssl+", userPass, suggestedServerName, uriForDecode.getPort(), null, null, null);
        account.setStoreUri(uri.toString());

        view.goToIncomingSettings();
    }

    @Override
    public void onBasicsStart() {
        stage = Stage.BASICS;
    }

    public boolean isEditSettings() {
        return editSettings;
    }

}
