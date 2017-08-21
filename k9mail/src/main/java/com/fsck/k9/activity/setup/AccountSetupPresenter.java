package com.fsck.k9.activity.setup;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fsck.k9.Account;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.EmailAddressValidator;
import com.fsck.k9.Globals;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.account.AccountCreator;
import com.fsck.k9.account.Oauth2PromptRequestHandler;
import com.fsck.k9.activity.AccountConfig;
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
import com.fsck.k9.mail.OAuth2NeedUserPromptException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.TransportProvider;
import com.fsck.k9.mail.autoconfiguration.AutoConfigure;
import com.fsck.k9.mail.autoconfiguration.AutoConfigure.ProviderInfo;
import com.fsck.k9.mail.autoconfiguration.AutoConfigureAutodiscover;
import com.fsck.k9.mail.autoconfiguration.AutoconfigureMozilla;
import com.fsck.k9.mail.autoconfiguration.AutoconfigureSrv;
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

import static com.fsck.k9.mail.ServerSettings.Type.IMAP;
import static com.fsck.k9.mail.ServerSettings.Type.POP3;
import static com.fsck.k9.mail.ServerSettings.Type.SMTP;
import static com.fsck.k9.mail.ServerSettings.Type.WebDAV;


public class AccountSetupPresenter implements AccountSetupContract.Presenter,
        Oauth2PromptRequestHandler {

    private Context context;
    private Preferences preferences;

    private ServerSettings incomingSettings;
    private ServerSettings outgoingSettings;
    private boolean makeDefault;
    private Provider provider;

    private boolean autoconfiguration;

    private static final int REQUEST_CODE_GMAIL = 1;

    private boolean oAuth2CodeGotten = false;

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
        ACCOUNT_NAMES,
    }

    private View view;

    private CheckDirection currentDirection;
    private CheckDirection direction;

    private boolean editSettings;

    private String password;

    private ConnectionSecurity currentIncomingSecurityType;
    private AuthType currentIncomingAuthType;
    private String currentIncomingPort;

    private ConnectionSecurity currentOutgoingSecurityType;
    private AuthType currentOutgoingAuthType;
    private String currentOutgoingPort;

    private Stage stage;

    private boolean restoring;

    private AccountConfig accountConfig;

    private Handler handler;

    public AccountSetupPresenter(Context context, Preferences preferences, View view) {
        this.context = context;
        this.preferences = preferences;
        this.view = view;
        this.handler = new Handler(Looper.getMainLooper());
        Globals.getOAuth2TokenProvider().setPromptRequestHandler(this);
    }

    // region basics

    @Override
    public void onBasicsStart() {
        stage = Stage.BASICS;
    }

    @Override
    public void onInputChangedInBasics(String email, String password) {
        EmailAddressValidator emailValidator = new EmailAddressValidator();

        boolean valid = email != null && email.length() > 0
                && (canOAuth2(email) || (password != null && password.length() > 0))
                && emailValidator.isValidAddressOnly(email);

        if (!canOAuth2(email)) {
            view.setPasswordInBasicsEnabled(true);
            view.setManualSetupButtonInBasicsVisibility(android.view.View.VISIBLE);
            view.setPasswordHintInBasics(context.getString(R.string.account_setup_basics_password_hint));
        } else {
            view.setPasswordInBasicsEnabled(false);
            view.setPasswordHintInBasics(context.getString(
                    R.string.account_setup_basics_password_no_password_needed_hint,
                    EmailHelper.getProviderNameFromEmailAddress(email))
            );
            view.setManualSetupButtonInBasicsVisibility(android.view.View.INVISIBLE);
        }

        view.setNextButtonInBasicsEnabled(valid);
    }

    private boolean canOAuth2(String email) {
        String domain = EmailHelper.getDomainFromEmailAddress(email);
        return domain != null && (domain.equals("gmail.com") || domain.equals("outlook.com"));
    }

    @Override
    public void onManualSetupButtonClicked(String email, String password) {
        manualSetup(email, password);
    }

    @Override
    public void onNextButtonInBasicViewClicked(String email, String password) {
        if (accountConfig == null) {
            accountConfig = new AccountConfigImpl(preferences);
        }

        accountConfig.setEmail(email);

        this.password = password;

        view.goToAutoConfiguration();
    }

    // endregion basics

    // region checking

    @Override
    public void onNegativeClickedInConfirmationDialog() {
        if (direction == CheckDirection.BOTH && currentDirection == CheckDirection.INCOMING) {
            checkOutgoing();
        } else if (currentDirection == CheckDirection.OUTGOING) {
            if (editSettings) {
                ((Account) accountConfig).save(preferences);
                view.end();
            } else {
                view.goToAccountNames();
            }
        } else {
            if (editSettings) {
                ((Account) accountConfig).save(preferences);
                view.end();
            } else {
                view.goToOutgoing();
            }
        }
    }


    private void autoConfiguration() {
        view.setMessage(R.string.account_setup_check_settings_retr_info_msg);

        findProvider(accountConfig.getEmail());
    }

    private void findProvider(final String email) {
        new AsyncTask<Void, Void, ProviderInfo>() {
            @Override
            protected ProviderInfo doInBackground(Void... params) {
                String[] emailParts = EmailHelper.splitEmail(email);
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

                try {
                    if (providerInfo != null) {
                        provider = Provider.newInstanceFromProviderInfo(providerInfo);
                    }

                    if (provider != null) {
                        autoconfiguration = true;

                        boolean usingOAuth2 = false;
                        if (canOAuth2(email)) {
                            usingOAuth2 = true;
                        }
                        modifyAccount(accountConfig.getEmail(), password, provider, usingOAuth2);

                        checkIncomingAndOutgoing();
                    }
                } catch (URISyntaxException e) {
                    Timber.e(e, "Error while converting providerInfo to provider");
                    provider = null;
                }

                if (provider == null) {
                    autoconfiguration = false;
                    manualSetup(accountConfig.getEmail(), password);
                }
            }
        }.execute();
    }

    private void checkIncomingAndOutgoing() {
        direction = CheckDirection.BOTH;
        currentDirection = CheckDirection.INCOMING;
        new CheckIncomingTask(accountConfig, new CheckSettingsSuccessCallback() {
            @Override
            public void onCheckSuccess() {
                checkOutgoing();
            }
        }).execute();
    }

    private void checkIncoming() {
        direction = CheckDirection.INCOMING;
        currentDirection = CheckDirection.INCOMING;
        new CheckIncomingTask(accountConfig, new CheckSettingsSuccessCallback() {
            @Override
            public void onCheckSuccess() {
                if (editSettings) {
                    updateAccount();
                    view.end();
                } else {
                    try {
                        String password = null;
                        String clientCertificateAlias = null;
                        if (AuthType.EXTERNAL == incomingSettings.authenticationType) {
                            clientCertificateAlias = incomingSettings.clientCertificateAlias;
                        } else {
                            password = incomingSettings.password;
                        }

                        URI oldUri = new URI(accountConfig.getTransportUri());
                        ServerSettings transportServer = new ServerSettings(Type.SMTP,
                                oldUri.getHost(), oldUri.getPort(),
                                ConnectionSecurity.SSL_TLS_REQUIRED, currentIncomingAuthType,
                                incomingSettings.username, password, clientCertificateAlias);
                        String transportUri = Transport.createTransportUri(transportServer);
                        accountConfig.setTransportUri(transportUri);
                    } catch (URISyntaxException use) {
                    /*
                     * If we can't set up the URL we just continue. It's only for
                     * convenience.
                     */
                    }

                    view.goToOutgoing();
                }
            }
        }).execute();
    }

    private void checkOutgoing() {
        direction = CheckDirection.OUTGOING;
        currentDirection = CheckDirection.OUTGOING;

        new CheckOutgoingTask(accountConfig, new CheckSettingsSuccessCallback() {
            @Override
            public void onCheckSuccess() {
                if (!editSettings) {
                    //We've successfully checked outgoing as well.
                    accountConfig.setDescription(accountConfig.getEmail());
                    // account.save(preferences);

                    // K9.setServicesEnabled(context);

                    view.goToAccountNames();
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

        private CheckOutgoingTask(AccountConfig accountConfig) {
            super(accountConfig);
        }

        private CheckOutgoingTask(AccountConfig accountConfig, CheckSettingsSuccessCallback callback) {
            super(accountConfig, callback);
        }

        @Override
        void checkSettings() throws Exception {
            Transport transport;

            if (editSettings) {
                clearCertificateErrorNotifications(CheckDirection.OUTGOING);
            }
            if (!(accountConfig.getRemoteStore() instanceof WebDavStore)) {
                publishProgress(R.string.account_setup_check_settings_check_outgoing_msg);
            }

            transport = TransportProvider.getInstance().getTransport(context, accountConfig,
                    Globals.getOAuth2TokenProvider());

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

        private CheckIncomingTask(AccountConfig accountConfig) {
            super(accountConfig);
        }

        private CheckIncomingTask(AccountConfig accountConfig, CheckSettingsSuccessCallback callback) {
            super(accountConfig, callback);
        }

        @Override
        void checkSettings() throws Exception {
            Store store;

            if (editSettings) {
                clearCertificateErrorNotifications(CheckDirection.INCOMING);
            }

            store = accountConfig.getRemoteStore();

            if (store instanceof WebDavStore) {
                publishProgress(R.string.account_setup_check_settings_authenticate);
            } else {
                publishProgress(R.string.account_setup_check_settings_check_incoming_msg);
            }
            store.checkSettings();

            if (store instanceof WebDavStore) {
                publishProgress(R.string.account_setup_check_settings_fetch);
            }

            if (editSettings) {
                Account account = (Account) accountConfig;
                MessagingController.getInstance(context).listFoldersSynchronous(account, true, null);
                MessagingController.getInstance(context)
                        .synchronizeMailbox(account, account.getInboxFolderName(), null, null);
            }
        }
    }

    /**
     * FIXME: Don't use an AsyncTask to perform network operations.
     * See also discussion in https://github.com/k9mail/k-9/pull/560
     */
    private abstract class CheckAccountTask extends AsyncTask<CheckDirection, Integer, Boolean> {
        private final AccountConfig accountConfig;
        private final Account account;
        private CheckSettingsSuccessCallback callback;

        private CheckAccountTask(Account account) {
            this(account, null);
        }

        private CheckAccountTask(Account account, CheckSettingsSuccessCallback callback) {
            this.account = account;
            this.accountConfig = null;
            this.callback = callback;
        }

        private CheckAccountTask(AccountConfig accountConfig) {
            this(accountConfig, null);
        }

        private CheckAccountTask(AccountConfig accountConfig, CheckSettingsSuccessCallback callback) {
            this.account = null;
            this.accountConfig = accountConfig;
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

            } catch (OAuth2NeedUserPromptException ignored) {
            } catch (final AuthenticationFailedException afe) {
                Timber.e(afe, "Error while testing settings");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        view.goBack();
                        view.showErrorDialog(R.string.account_setup_failed_auth_message);
                    }
                });
            } catch (CertificateValidationException cve) {
                handleCertificateValidationException(cve);
            } catch (final Exception e) {
                Timber.e(e, "Error while testing settings");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        view.goBack();
                        view.showErrorDialog(R.string.account_setup_failed_server_message);
                    }
                });
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
            final MessagingController ctrl = MessagingController.getInstance(context);
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


    private String getXmlAttribute(XmlResourceParser xml, String name) {
        int resId = xml.getAttributeResourceValue(null, name, 0);
        if (resId == 0) {
            return xml.getAttributeValue(null, name);
        } else {
            return context.getString(resId);
        }
    }

    private Provider findProviderForDomain(String domain) {
        try {
            XmlResourceParser xml = context.getResources().getXml(R.xml.providers);
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

    private void modifyAccount(String email, String password, @NonNull Provider provider,
                               boolean usingOAuth2) throws URISyntaxException {

        accountConfig.init(email, password);

        String[] emailParts = EmailHelper.splitEmail(email);
        String user = emailParts[0];
        String domain = emailParts[1];
        String userEnc = UrlEncodingHelper.encodeUtf8(user);
        String passwordEnc = UrlEncodingHelper.encodeUtf8(password);

        String incomingUsername = provider.incomingUsernameTemplate;
        incomingUsername = incomingUsername.replaceAll("\\$email", email);
        incomingUsername = incomingUsername.replaceAll("\\$user", userEnc);
        incomingUsername = incomingUsername.replaceAll("\\$domain", domain);

        URI incomingUriTemplate = provider.incomingUriTemplate;
        String incomingUserInfo = incomingUsername + ":" + passwordEnc;
        if (usingOAuth2) {
            incomingUserInfo = AuthType.XOAUTH2 + ":" + incomingUserInfo;
        }
        URI incomingUri = new URI(incomingUriTemplate.getScheme(), incomingUserInfo,
                incomingUriTemplate.getHost(), incomingUriTemplate.getPort(), null, null, null);

        String outgoingUsername = provider.outgoingUsernameTemplate;

        URI outgoingUriTemplate = provider.outgoingUriTemplate;


        URI outgoingUri;
        if (outgoingUsername != null) {
            outgoingUsername = outgoingUsername.replaceAll("\\$email", email);
            outgoingUsername = outgoingUsername.replaceAll("\\$user", userEnc);
            outgoingUsername = outgoingUsername.replaceAll("\\$domain", domain);

            String outgoingUserInfo = outgoingUsername + ":" + passwordEnc;
            if (usingOAuth2) {
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

        accountConfig.setStoreUri(incomingUri.toString());
        accountConfig.setTransportUri(outgoingUri.toString());

        setupFolderNames(incomingUriTemplate.getHost().toLowerCase(Locale.US));

        ServerSettings incomingSettings = RemoteStore.decodeStoreUri(incomingUri.toString());
        accountConfig.setDeletePolicy(AccountCreator.getDefaultDeletePolicy(incomingSettings.type));
    }

    private void setupFolderNames(String domain) {
        accountConfig.setDraftsFolderName(K9.getK9String(R.string.special_mailbox_name_drafts));
        accountConfig.setTrashFolderName(K9.getK9String(R.string.special_mailbox_name_trash));
        accountConfig.setSentFolderName(K9.getK9String(R.string.special_mailbox_name_sent));
        accountConfig.setArchiveFolderName(K9.getK9String(R.string.special_mailbox_name_archive));

        // Yahoo! has a special folder for Spam, called "Bulk Mail".
        if (domain.endsWith(".yahoo.com")) {
            accountConfig.setSpamFolderName("Bulk Mail");
        } else {
            accountConfig.setSpamFolderName(K9.getK9String(R.string.special_mailbox_name_spam));
        }
    }

    @Override
    public void onCertificateAccepted(X509Certificate certificate) {
        try {
            accountConfig.addCertificate(currentDirection, certificate);
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

        final StringBuilder chainInfo = new StringBuilder(100);
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

        final String finalExMessage = exMessage;
        handler.post(new Runnable() {
            @Override
            public void run() {
                view.showAcceptKeyDialog(msgResId, finalExMessage, chainInfo.toString(), chain[0]);
            }
        });
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

    private interface CheckSettingsSuccessCallback {
        void onCheckSuccess();
    }

    // endregion checking

    // region incoming

    @Override
    public void onIncomingStart(boolean editSettings) {
        this.editSettings = editSettings;

        stage = Stage.INCOMING;
        ConnectionSecurity[] connectionSecurityChoices = ConnectionSecurity.values();
        try {
            incomingSettings = RemoteStore.decodeStoreUri(accountConfig.getStoreUri());

            currentIncomingAuthType = incomingSettings.authenticationType;

            view.setAuthTypeInIncoming(currentIncomingAuthType);

            updateViewFromAuthTypeInIncoming(currentIncomingAuthType);

            currentIncomingSecurityType = incomingSettings.connectionSecurity;

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
                throw new IllegalArgumentException("Unknown account type: " + accountConfig.getStoreUri());
            }

            if (!editSettings) {
                accountConfig.setDeletePolicy(AccountCreator.getDefaultDeletePolicy(incomingSettings.type));
            }

            view.setSecurityChoices(connectionSecurityChoices);

            view.setSecurityTypeInIncoming(currentIncomingSecurityType);
            updateAuthPlainTextFromSecurityType(currentIncomingSecurityType);

            if (incomingSettings.host != null) {
                view.setServerInIncoming(incomingSettings.host);
            }

            if (incomingSettings.port != -1) {
                String port = String.valueOf(incomingSettings.port);
                view.setPortInIncoming(port);
                currentIncomingPort = port;
            } else {
                updatePortFromSecurityTypeInIncoming(currentIncomingSecurityType);
            }

            if (editSettings) {
                view.setCompressionSectionVisibility(android.view.View.VISIBLE);
                view.setImapPathPrefixSectionVisibility(android.view.View.VISIBLE);
            }
            view.setCompressionMobile(accountConfig.useCompression(NetworkType.MOBILE));
            view.setCompressionWifi(accountConfig.useCompression(NetworkType.WIFI));
            view.setCompressionOther(accountConfig.useCompression(NetworkType.OTHER));

            view.setSubscribedFoldersOnly(accountConfig.subscribedFoldersOnly());

        } catch (IllegalArgumentException e) {
            view.showFailureToast(e);
        }
    }

    @Override
    public void onIncomingStart() {
        onIncomingStart(editSettings);
    }

    private void updatePortFromSecurityTypeInIncoming(ConnectionSecurity securityType) {
        if (restoring) return;

        String port = String.valueOf(AccountCreator.getDefaultPort(securityType, incomingSettings.type));
        view.setPortInIncoming(port);
        currentIncomingPort = port;
    }

    private void updateAuthPlainTextFromSecurityType(ConnectionSecurity securityType) {
        view.setAuthTypeInsecureText(securityType == ConnectionSecurity.NONE);
    }

    @Override
    public void onInputChangedInIncoming(String certificateAlias, String server, String port,
            String username, String password, AuthType authType,
            ConnectionSecurity connectionSecurity) {

        revokeInvalidSettingsAndUpdateViewInIncoming(authType, connectionSecurity, port);
        validateFieldInIncoming(certificateAlias, server, currentIncomingPort, username, password,
                currentIncomingAuthType,
                currentIncomingSecurityType);
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

        accountConfig.deleteCertificate(host, port, CheckDirection.INCOMING);
        incomingSettings = new ServerSettings(incomingSettings.type, host, port,
                connectionSecurity, authType, username, password, clientCertificateAlias, extra);

        accountConfig.setStoreUri(RemoteStore.createStoreUri(incomingSettings));

        accountConfig.setCompression(NetworkType.MOBILE, compressMobile);
        accountConfig.setCompression(NetworkType.WIFI, compressWifi);
        accountConfig.setCompression(NetworkType.OTHER, compressOther);
        accountConfig.setSubscribedFoldersOnly(subscribedFoldersOnly);

        view.goToIncomingChecking();
    }

    private void revokeInvalidSettingsAndUpdateViewInIncoming(AuthType authType,
            ConnectionSecurity connectionSecurity,
            String port) {
        boolean isAuthTypeExternal = (AuthType.EXTERNAL == authType);

        boolean hasConnectionSecurity = (connectionSecurity != ConnectionSecurity.NONE);

        if (isAuthTypeExternal && !hasConnectionSecurity) {
            view.showInvalidSettingsToast();
            view.setAuthTypeInIncoming(currentIncomingAuthType);
            view.setSecurityTypeInIncoming(currentIncomingSecurityType);
            view.setPortInIncoming(currentIncomingPort);
        } else {
            currentIncomingPort = port;

            onAuthTypeSelectedInIncoming(authType);
            onSecuritySelectedInIncoming(connectionSecurity);

            currentIncomingAuthType = authType;
            currentIncomingSecurityType = connectionSecurity;
        }
    }

    private void onAuthTypeSelectedInIncoming(AuthType authType) {
        if (authType != currentIncomingAuthType) {
            setAuthTypeInIncoming(authType);
        }
    }

    private void onSecuritySelectedInIncoming(ConnectionSecurity securityType) {
        if (securityType != currentIncomingSecurityType) {
            setSecurityTypeInIncoming(securityType);
        }
    }

    private void setAuthTypeInIncoming(AuthType authType) {
        view.setAuthTypeInIncoming(authType);

        updateViewFromAuthTypeInIncoming(authType);
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
        Account account = (Account) accountConfig;

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
        account.save(preferences);
    }

    private void updateViewFromAuthTypeInIncoming(AuthType authType) {
        if (authType == AuthType.EXTERNAL) {
            view.setViewExternalInIncoming();
        } else {
            view.setViewNotExternalInIncoming();
        }
    }

    private String getString(int id) {
        return context.getString(id);
    }

    // endregion incoming

    // region names

    @Override
    public void onNamesStart() {
        stage = Stage.ACCOUNT_NAMES;
    }

    @Override
    public void onInputChangedInNames(String name, String description) {
        if (Utility.requiredFieldValid(name)) {
            view.setDoneButtonInNamesEnabled(true);
        } else {
            view.setDoneButtonInNamesEnabled(false);
        }
    }

    @Override
    public void onNextButtonInNamesClicked(String name, String description) {
        if (Utility.requiredFieldValid(description)) {
            accountConfig.setDescription(description);
        }

        accountConfig.setName(name);

        Account account = preferences.newAccount();
        account.loadConfig(accountConfig);

        MessagingController.getInstance(context).listFoldersSynchronous(account, true, null);
        MessagingController.getInstance(context)
                .synchronizeMailbox(account, account.getInboxFolderName(), null, null);

        account.save(preferences);

        if (account.equals(preferences.getDefaultAccount()) || makeDefault) {
            preferences.setDefaultAccount(account);
        }

        K9.setServicesEnabled(context);

        view.goToListAccounts();
    }

    // endregion names

    // region outgoing
    @Override
    public void onOutgoingStart() {
        onOutgoingStart(editSettings);
    }

    @Override
    public void onOutgoingStart(boolean editSettings) {
        this.editSettings = editSettings;
        stage = Stage.OUTGOING;
        analysisAccount();
    }

    private void analysisAccount() {
        try {
            if (new URI(accountConfig.getStoreUri()).getScheme().startsWith("webdav")) {
                accountConfig.setTransportUri(accountConfig.getStoreUri());
                view.goToOutgoingChecking();

                return;
            }
        } catch (URISyntaxException e) {
            view.showFailureToast(e);
        }

        try {
            outgoingSettings = Transport.decodeTransportUri(accountConfig.getTransportUri());

            currentOutgoingAuthType = outgoingSettings.authenticationType;
            setAuthTypeInOutgoing(currentOutgoingAuthType);

            currentOutgoingSecurityType = outgoingSettings.connectionSecurity;
            setSecurityTypeInOutgoing(currentOutgoingSecurityType);

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
                currentOutgoingPort = String.valueOf(outgoingSettings.port);
                view.setPortInOutgoing(currentOutgoingPort);
            }
        } catch (Exception e) {
            view.showFailureToast(e);
        }
    }


    private void setAuthTypeInOutgoing(AuthType authType) {
        view.setAuthTypeInOutgoing(authType);

        updateViewFromAuthTypeInOutgoing(authType);
    }

    private void setSecurityTypeInOutgoing(ConnectionSecurity securityType) {
        view.setSecurityTypeInOutgoing(securityType);

        updateViewFromSecurityTypeInOutgoing(securityType);
    }

    private void onSecuritySelectedInOutgoing(ConnectionSecurity securityType) {
        if (securityType != currentOutgoingSecurityType) {
            updateViewFromSecurityTypeInOutgoing(securityType);
        }
    }

    private void onAuthTypeSelectedInOutgoing(AuthType authType) {
        if (authType != currentOutgoingAuthType) {
            setAuthTypeInOutgoing(authType);
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

        accountConfig.deleteCertificate(host, port, CheckDirection.OUTGOING);
        ServerSettings server = new ServerSettings(Type.SMTP, host, port, connectionSecurity,
                authType, username, password, clientCertificateAlias);
        String uri = Transport.createTransportUri(server);
        accountConfig.setTransportUri(uri);

        view.goToOutgoingChecking();
    }

    @Override
    public void onInputChangedInOutgoing(String certificateAlias, String server, String port, String username,
            String password, AuthType authType,
            ConnectionSecurity connectionSecurity, boolean requireLogin) {

        if (currentOutgoingSecurityType != connectionSecurity) {
            boolean isAuthTypeExternal = (AuthType.EXTERNAL == authType);

            boolean hasConnectionSecurity = (connectionSecurity != ConnectionSecurity.NONE);

            if (isAuthTypeExternal && !hasConnectionSecurity && !requireLogin) {
                authType = AuthType.PLAIN;
                view.setAuthTypeInOutgoing(authType);
                updateViewFromAuthTypeInOutgoing(authType);
            }
        }

        revokeInvalidSettingsAndUpdateViewInOutgoing(authType, connectionSecurity, port);
        validateFieldInOutgoing(certificateAlias, server, currentOutgoingPort, username, password, currentOutgoingAuthType,
                currentOutgoingSecurityType, requireLogin);
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
            view.setAuthTypeInOutgoing(currentOutgoingAuthType);
            view.setSecurityTypeInOutgoing(currentOutgoingSecurityType);
            view.setPortInOutgoing(currentOutgoingPort);
        } else {
            currentOutgoingPort = port;

            onAuthTypeSelectedInOutgoing(authType);
            onSecuritySelectedInOutgoing(connectionSecurity);

            currentOutgoingAuthType = authType;
            currentOutgoingSecurityType = connectionSecurity;
        }
    }

    private void updateViewFromSecurityTypeInOutgoing(ConnectionSecurity securityType) {
        view.updateAuthPlainTextInOutgoing(securityType == ConnectionSecurity.NONE);

        if (restoring) return;
        String port = String.valueOf(AccountCreator.getDefaultPort(securityType, SMTP));
        view.setPortInOutgoing(port);
        currentOutgoingPort = port;
    }

    private void updateViewFromAuthTypeInOutgoing(AuthType authType) {
        if (authType == AuthType.EXTERNAL) {
            view.setViewExternalInOutgoing();
        } else {
            view.setViewNotExternalInOutgoing();
        }
    }

    // endregion outgoing

    // region account type
    @Override
    public void onAccountTypeStart() {
        stage = Stage.ACCOUNT_TYPE;
    }

    @Override
    public void onNextButtonInAccountTypeClicked(Type serverType) throws URISyntaxException {
        switch (serverType) {
            case IMAP:
                onImapOrPop3Selected(IMAP, "imap+ssl+");
                break;
            case POP3:
                onImapOrPop3Selected(POP3, "pop3+ssl+");
                break;
            case WebDAV:
                onWebdavSelected();
                break;
        }
    }

    private void onImapOrPop3Selected(Type serverType, String schemePrefix) throws URISyntaxException {
        ServerNameSuggester serverNameSuggester = new ServerNameSuggester();

        String domainPart = EmailHelper.getDomainFromEmailAddress(accountConfig.getEmail());

        String suggestedStoreServerName = serverNameSuggester.suggestServerName(serverType, domainPart);
        URI storeUriForDecode = new URI(accountConfig.getStoreUri());
        URI storeUri = new URI(schemePrefix, storeUriForDecode.getUserInfo(), suggestedStoreServerName,
                storeUriForDecode.getPort(), null, null, null);
        accountConfig.setStoreUri(storeUri.toString());

        String suggestedTransportServerName = serverNameSuggester.suggestServerName(SMTP, domainPart);
        URI transportUriForDecode = new URI(accountConfig.getTransportUri());
        URI transportUri = new URI("smtp+tls+", transportUriForDecode.getUserInfo(), suggestedTransportServerName,
                transportUriForDecode.getPort(), null, null, null);
        accountConfig.setTransportUri(transportUri.toString());

        view.goToIncomingSettings();
    }

    private void onWebdavSelected() throws URISyntaxException {
        ServerNameSuggester serverNameSuggester = new ServerNameSuggester();

        URI uriForDecode = new URI(accountConfig.getStoreUri());

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

        String domainPart = EmailHelper.getDomainFromEmailAddress(accountConfig.getEmail());
        String suggestedServerName = serverNameSuggester.suggestServerName(WebDAV, domainPart);
        URI uri = new URI("webdav+ssl+", userPass, suggestedServerName, uriForDecode.getPort(), null, null, null);
        accountConfig.setStoreUri(uri.toString());

        view.goToIncomingSettings();
    }

    // endregion account type

    @Override
    public void onBackPressed() {
        switch (stage) {
            case AUTOCONFIGURATION:
            case AUTOCONFIGURATION_INCOMING_CHECKING:
            case AUTOCONFIGURATION_OUTGOING_CHECKING:
            case ACCOUNT_TYPE:
                stage = Stage.BASICS;
                view.goToBasics();
                break;
            case INCOMING:
                if (!editSettings) {
                    stage = Stage.ACCOUNT_TYPE;
                    view.goToAccountType();
                } else {
                    view.end();
                }
                break;
            case INCOMING_CHECKING:
                stage = Stage.INCOMING;
                view.goToIncoming();
                break;
            case OUTGOING:
                if (!editSettings) {
                    stage = Stage.INCOMING;
                    view.goToIncoming();
                } else {
                    view.end();
                }
                break;
            case OUTGOING_CHECKING:
            case ACCOUNT_NAMES:
                if (autoconfiguration) {
                    stage = Stage.BASICS;
                    view.goToBasics();
                } else {
                    stage = Stage.OUTGOING;
                    view.goToOutgoing();
                }
                break;
            default:
                view.end();
                break;
        }
    }

    @Override
    public void setAccount(Account account) {
        this.accountConfig = account;
    }

    @Override
    public Account getAccount() {
        return (Account) accountConfig;
    }

    public boolean isEditSettings() {
        return editSettings;
    }

    @Override
    public void onGetAccountUuid(@Nullable String accountUuid) {
        accountConfig = preferences.getAccount(accountUuid);
    }

    @Override
    public void onGetAccountConfig(@Nullable AccountConfigImpl accountConfig) {
        this.accountConfig = accountConfig;
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
    public AccountSetupStatus getStatus() {
        return new AccountSetupStatus(currentIncomingSecurityType, currentIncomingAuthType,
                currentIncomingPort, currentOutgoingSecurityType, currentOutgoingAuthType,
                currentOutgoingPort, accountConfig.isNotifyNewMail(), accountConfig.isShowOngoing(),
                accountConfig.getAutomaticCheckIntervalMinutes(), accountConfig.getDisplayCount(), accountConfig.getFolderPushMode(),
                accountConfig.getName(), accountConfig.getDescription());
    }

    @Override
    public AccountConfig getAccountConfig() {
        return accountConfig;
    }

    @Override
    public void onGetMakeDefault(boolean makeDefault) {
        this.makeDefault = makeDefault;
    }

    private void manualSetup(String email, String password) {
        autoconfiguration = false;

        if (accountConfig == null) {
            accountConfig = new AccountConfigImpl(preferences);
        }

        accountConfig.init(email, password);

        view.goToAccountType();
    }

    @Override
    public void handleGmailXOAuth2Intent(Intent intent) {
        view.startIntentForResult(intent, REQUEST_CODE_GMAIL);
    }

    @Override
    public void handleGmailRedirectUrl(final String url) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                view.openGmailUrl(url);
            }
        });
    }

    @Override
    public void handleOutlookRedirectUrl(final String url) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                view.openOutlookUrl(url);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_GMAIL) {
            if (resultCode == Activity.RESULT_OK) {
                checkIncomingAndOutgoing();
            } else {
                view.goBack();
            }
        }
    }

    @Override
    public void onOAuthCodeGot(final String code) {
        oAuth2CodeGotten = true;
        view.closeAuthDialog();
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    Globals.getOAuth2TokenProvider().getAuthorizationCodeFlowTokenProvider().exchangeCode(accountConfig.getEmail(), code);
                } catch (AuthenticationFailedException e) {
                    return false;
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    checkIncomingAndOutgoing();
                } else {
                    oAuth2CodeGotten = false;
                    view.goToBasics();
                    view.showErrorDialog("Error when exchanging code");
                }
            }
        }.execute();
    }

    @Override
    public void onErrorWhenGettingOAuthCode(String errorMessage) {
        oAuth2CodeGotten = false;
        view.closeAuthDialog();
        view.goToBasics();
        view.showErrorDialog(errorMessage);
    }

    @Override
    public void onWebViewDismiss() {
        if (!oAuth2CodeGotten) {
            view.goToBasics();
            view.showErrorDialog("Please connect us with Gmail"); // TODO: 8/18/17 A better error message?
        }
    }

    static class AccountSetupStatus {
        private ConnectionSecurity incomingSecurityType;
        private AuthType incomingAuthType;
        private String incomingPort;

        private ConnectionSecurity outgoingSecurityType;
        private AuthType outgoingAuthType;
        private String outgoingPort;

        private boolean notifyNewMail;
        private boolean showOngoing;
        private int automaticCheckIntervalMinutes;
        private int displayCount;
        private Account.FolderMode folderPushMode;

        private String name;
        private String description;

        AccountSetupStatus(ConnectionSecurity incomingSecurityType, AuthType incomingAuthType,
                String incomingPort, ConnectionSecurity outgoingSecurityType, AuthType outgoingAuthType,
                String outgoingPort, boolean notifyNewMail, boolean showOngoing, int automaticCheckIntervalMinutes,
                int displayCount, Account.FolderMode folderPushMode,
                String name, String description) {
            this.incomingSecurityType = incomingSecurityType;
            this.incomingAuthType = incomingAuthType;
            this.incomingPort = incomingPort;
            this.outgoingSecurityType = outgoingSecurityType;
            this.outgoingAuthType = outgoingAuthType;
            this.outgoingPort = outgoingPort;
            this.notifyNewMail = notifyNewMail;
            this.showOngoing = showOngoing;
            this.automaticCheckIntervalMinutes = automaticCheckIntervalMinutes;
            this.displayCount = displayCount;
            this.folderPushMode = folderPushMode;
            this.name = name;
            this.description = description;
        }

        public ConnectionSecurity getIncomingSecurityType() {
            return incomingSecurityType;
        }

        public AuthType getIncomingAuthType() {
            return incomingAuthType;
        }

        public String getIncomingPort() {
            return incomingPort;
        }

        public ConnectionSecurity getOutgoingSecurityType() {
            return outgoingSecurityType;
        }

        public AuthType getOutgoingAuthType() {
            return outgoingAuthType;
        }

        public String getOutgoingPort() {
            return outgoingPort;
        }

        public boolean isNotifyNewMail() {
            return notifyNewMail;
        }

        public boolean isShowOngoing() {
            return showOngoing;
        }

        public int getAutomaticCheckIntervalMinutes() {
            return automaticCheckIntervalMinutes;
        }

        public int getDisplayCount() {
            return displayCount;
        }

        public FolderMode getFolderPushMode() {
            return folderPushMode;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }


}

