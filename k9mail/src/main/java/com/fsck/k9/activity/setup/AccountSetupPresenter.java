package com.fsck.k9.activity.setup;


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

import android.app.Activity;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fsck.k9.Account;
import com.fsck.k9.EmailAddressValidator;
import com.fsck.k9.Globals;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.account.AccountCreator;
import com.fsck.k9.account.Oauth2PromptRequestHandler;
import com.fsck.k9.activity.AccountConfig;
import com.fsck.k9.activity.setup.AccountSetupContract.AccountSetupView;
import com.fsck.k9.activity.setup.AccountSetupContract.Presenter;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.helper.EmailHelper;
import com.fsck.k9.helper.UrlEncodingHelper;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.NetworkType;
import com.fsck.k9.mail.OAuth2NeedUserPromptException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.TransportProvider;
import com.fsck.k9.mail.TransportUris;
import com.fsck.k9.mail.autoconfiguration.AutoConfigure.ProviderInfo;
import com.fsck.k9.mail.filter.Hex;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mail.store.imap.ImapStoreSettings;
import com.fsck.k9.mail.store.webdav.WebDavStore;
import com.fsck.k9.mail.store.webdav.WebDavStoreSettings;
import com.fsck.k9.setup.ServerNameSuggester;
import timber.log.Timber;


public class AccountSetupPresenter implements Presenter, Oauth2PromptRequestHandler, Observer<ProviderInfo> {

    private Context context;
    private LifecycleOwner lifecycleOwner;
    private Preferences preferences;

    private ServerSettings incomingSettings;
    private ServerSettings outgoingSettings;
    private boolean makeDefault;

    private boolean isManualSetup;

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

    private AccountSetupView accountSetupView;
    private AccountSetupViewModel viewModel;

    private CheckDirection currentDirection;
    private CheckDirection direction;

    private boolean editSettings;

    private String password;
    private String clientCertificateAlias;

    private ConnectionSecurity currentIncomingSecurityType;
    private AuthType currentIncomingAuthType;
    private String currentIncomingPort;

    private ConnectionSecurity currentOutgoingSecurityType;
    private AuthType currentOutgoingAuthType;
    private String currentOutgoingPort;

    private Stage stage;

    private Handler handler;

    public AccountSetupPresenter(Context context, LifecycleOwner lifecycleOwner, Preferences preferences,
            AccountSetupView accountSetupView, AccountSetupViewModel viewModel) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.preferences = preferences;
        this.accountSetupView = accountSetupView;
        this.viewModel = viewModel;
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

        accountSetupView.setPasswordInBasicsEnabled(true);
        accountSetupView.setPasswordHintInBasics(context.getString(R.string.account_setup_basics_password_hint));
        accountSetupView.setManualSetupButtonInBasicsVisibility(false);

        /*
        if (!canOAuth2(email)) {
            view.setPasswordInBasicsEnabled(true);
            view.setManualSetupButtonInBasicsVisibility(true);
            view.setPasswordHintInBasics(context.getString(R.string.account_setup_basics_password_hint));
        } else {
            view.setPasswordInBasicsEnabled(false);
            view.setPasswordHintInBasics(context.getString(
                    R.string.account_setup_basics_password_no_password_needed_hint
                    )
            );
            view.setManualSetupButtonInBasicsVisibility(false);
        }*/

        boolean valid = email != null && email.length() > 0
                && (canOAuth2(email) || (password != null && password.length() > 0))
                && emailValidator.isValidAddressOnly(email);

        accountSetupView.setNextButtonInBasicsEnabled(valid);
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
        if (viewModel.accountConfig == null) {
            viewModel.accountConfig = new ManualSetupInfo();
        }

        viewModel.accountConfig.setEmail(email);

        this.password = password;

        accountSetupView.goToAutoConfiguration();
        startAutoConfiguration();
    }

    // endregion basics

    // region checking

    @Override
    public void onNegativeClickedInConfirmationDialog() {
        if (direction == CheckDirection.BOTH && currentDirection == CheckDirection.INCOMING) {
            checkOutgoing();
        } else if (currentDirection == CheckDirection.OUTGOING) {
            if (editSettings) {
                // ((Account) viewModel.accountConfig).save(preferences);
                accountSetupView.end();
            } else {
                accountSetupView.goToAccountNames();
            }
        } else {
            if (editSettings) {
                // ((Account) viewModel.accountConfig).save(preferences);
                accountSetupView.end();
            } else {
                accountSetupView.goToOutgoing();
            }
        }
    }


    private void startAutoConfiguration() {
        LiveData<ProviderInfo> autoconfigureLiveData =
                viewModel.getAutoconfigureLiveData(context, viewModel.accountConfig.getEmail());
        autoconfigureLiveData.observe(lifecycleOwner, this);
    }

    @Override
    public void onChanged(@Nullable ProviderInfo providerInfo) {
        viewModel.setupInfo = viewModel.setupInfo.withProviderInfo(providerInfo);
        accountSetupView.goToAccountNames();

        //                 if (incomingReady && outgoingReady) {
        // } else if (incomingReady) {
    //                    view.goToOutgoing();
    //                    return;
        // }
        /*
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
                    */
    }

    private void checkIncomingAndOutgoing() {
        direction = CheckDirection.BOTH;
        currentDirection = CheckDirection.INCOMING;
        new CheckIncomingTask(viewModel.accountConfig, new CheckSettingsSuccessCallback() {
            @Override
            public void onCheckSuccess() {
                checkOutgoing();
            }
        }).execute();
    }

    private void checkIncoming() {
        direction = CheckDirection.INCOMING;
        currentDirection = CheckDirection.INCOMING;
        new CheckIncomingTask(viewModel.accountConfig, new CheckSettingsSuccessCallback() {
            @Override
            public void onCheckSuccess() {
                if (editSettings) {
                    updateAccount();
                    accountSetupView.end();
                } else {
                    /*if (outgoingReady) {
                        accountConfig.setDescription(accountConfig.getEmail());
                        view.goToAccountNames();
                        return;
                    }*/

                    ServerSettings transportServer = TransportUris.decodeTransportUri(viewModel.accountConfig.getTransportUri());
                    if (AuthType.EXTERNAL == incomingSettings.authenticationType) {
                        clientCertificateAlias = incomingSettings.clientCertificateAlias;
                        transportServer.newClientCertificateAlias(clientCertificateAlias);
                    } else {
                        password = incomingSettings.password;
                        transportServer = transportServer.newPassword(password);
                    }

                    String transportUri = TransportUris.createTransportUri(transportServer);
                    viewModel.accountConfig.setTransportUri(transportUri);

                    accountSetupView.goToOutgoing();
                }
            }
        }).execute();
    }

    private void checkOutgoing() {
        direction = CheckDirection.OUTGOING;
        currentDirection = CheckDirection.OUTGOING;

        new CheckOutgoingTask(viewModel.accountConfig, new CheckSettingsSuccessCallback() {
            @Override
            public void onCheckSuccess() {
                if (!editSettings) {
                    //We've successfully checked outgoing as well.
                    viewModel.accountConfig.setDescription(viewModel.accountConfig.getEmail());

                    accountSetupView.goToAccountNames();
                } else {
                    updateAccount();

                    accountSetupView.end();
                }
            }
        }).execute();
    }

    @Override
    public void onCheckingStart(Stage stage) {
        this.stage = stage;

        switch (stage) {
            case AUTOCONFIGURATION:
                startAutoConfiguration();
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
            if (!(viewModel.accountConfig.getRemoteStore() instanceof WebDavStore)) {
                publishProgress(R.string.account_setup_check_settings_check_outgoing_msg);
            }

            transport = TransportProvider.getInstance().getTransport(context, viewModel.accountConfig,
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
        private CheckIncomingTask(AccountConfig accountConfig) {
            super(accountConfig);
        }

        private CheckIncomingTask(AccountConfig accountConfig, CheckSettingsSuccessCallback callback) {
            super(accountConfig, callback);
        }

        @Override
        void checkSettings() throws Exception {
            checkIncomingSettings();
        }

        private void checkIncomingSettings() throws MessagingException {
            RemoteStore store;

            if (editSettings) {
                clearCertificateErrorNotifications(CheckDirection.INCOMING);
            }

            store = viewModel.accountConfig.getRemoteStore();

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
                // Account account = (Account) viewModel.accountConfig;
                // MessagingController.getInstance(context).listFoldersSynchronous(account, true, null);
                // MessagingController.getInstance(context)
                // .synchronizeMailbox(account, account.getInboxFolderName(), null, null);
            }
        }
    }

    /**
     * FIXME: Don't use an AsyncTask to perform network operations.
     * See also discussion in https://github.com/k9mail/k-9/pull/560
     */
    private abstract class CheckAccountTask extends AsyncTask<CheckDirection, Integer, Boolean> {
        private final AccountConfig accountConfig;
        private CheckSettingsSuccessCallback callback;

        private CheckAccountTask(AccountConfig accountConfig) {
            this(accountConfig, null);
        }

        private CheckAccountTask(AccountConfig accountConfig, CheckSettingsSuccessCallback callback) {
            this.accountConfig = accountConfig;
            this.callback = callback;
        }

        abstract void checkSettings() throws Exception;

        @Override
        protected Boolean doInBackground(CheckDirection... params) {
            try {
                checkSettings();

                return true;

            } catch (OAuth2NeedUserPromptException ignored) {
            } catch (final AuthenticationFailedException afe) {
                Timber.e(afe, "Error while testing settings");
                if (afe.getMessage().equals(AuthenticationFailedException.OAUTH2_ERROR_INVALID_REFRESH_TOKEN)) {
                    Globals.getOAuth2TokenProvider().disconnectEmailWithK9(accountConfig.getEmail());
                    replayChecking();
                } else {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            accountSetupView.goBack();
                            accountSetupView.showErrorDialog(R.string.account_setup_failed_auth_message);
                        }
                    });
                }
            } catch (CertificateValidationException cve) {
                handleCertificateValidationException(cve);
            } catch (final Exception e) {
                Timber.e(e, "Error while testing settings");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        accountSetupView.goBack();
                        accountSetupView.showErrorDialog(R.string.account_setup_failed_server_message);
                    }
                });
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            super.onPostExecute(bool);

            /*
             * This task could be interrupted at any point, but network operations can block,
             * so relying on InterruptedException is not enough. Instead, check after
             * each potentially long-running operation.
             */
            if (bool && callback != null) {
                callback.onCheckSuccess();
            }
        }

        void clearCertificateErrorNotifications(CheckDirection direction) {
            final MessagingController ctrl = MessagingController.getInstance(context);
            ctrl.clearCertificateErrorNotifications((Account) accountConfig, direction);

        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            accountSetupView.setMessage(values[0]);
        }


    }


    private void modifyAccount(String email, String password, @NonNull ProviderInfo providerInfo, boolean usingOAuth2) {
        viewModel.accountConfig.init(email, password);

        String[] emailParts = EmailHelper.splitEmail(email);
        String user = emailParts[0];
        String domain = emailParts[1];
        String userEnc = UrlEncodingHelper.encodeUtf8(user);
        String passwordEnc = UrlEncodingHelper.encodeUtf8(password);

        String incomingUsername = providerInfo.incomingUsernameTemplate;
        incomingUsername = incomingUsername.replaceAll("\\$email", email);
        incomingUsername = incomingUsername.replaceAll("\\$user", userEnc);
        incomingUsername = incomingUsername.replaceAll("\\$domain", domain);

        String incomingUserInfo = incomingUsername + ":" + passwordEnc;
        if (usingOAuth2) {
            incomingUserInfo = AuthType.XOAUTH2 + ":" + incomingUserInfo;
        }
        String incomingUri =
                providerInfo.incomingType + incomingUserInfo + providerInfo.incomingAddr + providerInfo.incomingPort;

        String outgoingUsername = providerInfo.outgoingUsernameTemplate;

        String outgoingUri;
        if (outgoingUsername != null) {
            outgoingUsername = outgoingUsername.replaceAll("\\$email", email);
            outgoingUsername = outgoingUsername.replaceAll("\\$user", userEnc);
            outgoingUsername = outgoingUsername.replaceAll("\\$domain", domain);

            String outgoingUserInfo = outgoingUsername + ":" + passwordEnc;
            if (usingOAuth2) {
                outgoingUserInfo = outgoingUserInfo + ":" + AuthType.XOAUTH2;
            }
            outgoingUri = providerInfo.outgoingType + outgoingUserInfo + providerInfo.outgoingAddr + ":" +
                    providerInfo.outgoingPort;

        } else {
            outgoingUri = providerInfo.outgoingType + providerInfo.outgoingAddr + ":" + providerInfo.outgoingPort;
        }

        viewModel.accountConfig.setStoreUri(incomingUri);
        viewModel.accountConfig.setTransportUri(outgoingUri);

        setupFolderNames(providerInfo.incomingAddr.toLowerCase(Locale.US));

        ServerSettings incomingSettings = RemoteStore.decodeStoreUri(incomingUri);
        viewModel.accountConfig.setDeletePolicy(AccountCreator.getDefaultDeletePolicy(incomingSettings.type));
    }

    private void setupFolderNames(String domain) {
        viewModel.accountConfig.setDraftsFolderName(K9.getK9String(R.string.special_mailbox_name_drafts));
        viewModel.accountConfig.setTrashFolderName(K9.getK9String(R.string.special_mailbox_name_trash));
        viewModel.accountConfig.setSentFolderName(K9.getK9String(R.string.special_mailbox_name_sent));
        viewModel.accountConfig.setArchiveFolderName(K9.getK9String(R.string.special_mailbox_name_archive));

        // Yahoo! has a special folder for Spam, called "Bulk Mail".
        if (domain.endsWith(".yahoo.com")) {
            viewModel.accountConfig.setSpamFolderName("Bulk Mail");
        } else {
            viewModel.accountConfig.setSpamFolderName(K9.getK9String(R.string.special_mailbox_name_spam));
        }
    }

    @Override
    public void onCertificateAccepted(X509Certificate certificate) {
        try {
            viewModel.accountConfig.addCertificate(currentDirection, certificate);
        } catch (CertificateException e) {
            accountSetupView.showErrorDialog(
                    R.string.account_setup_failed_dlg_certificate_message_fmt,
                    e.getMessage() == null ? "" : e.getMessage());
        }

        replayChecking();
    }

    @Override
    public void onCertificateRefused() {
        if (stage == Stage.INCOMING_CHECKING) {
            accountSetupView.goToIncoming();
        } else if (stage == Stage.OUTGOING_CHECKING) {
            accountSetupView.goToOutgoing();
        }
    }

    @Override
    public void onPositiveClickedInConfirmationDialog() {
        if (stage == Stage.INCOMING_CHECKING) {
            accountSetupView.goToIncoming();
        } else if (stage == Stage.OUTGOING_CHECKING){
            accountSetupView.goToOutgoing();
        } else {
            accountSetupView.goToBasics();
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
                accountSetupView.showAcceptKeyDialog(msgResId, finalExMessage, chainInfo.toString(), chain[0]);
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
            accountSetupView.showErrorDialog(
                    R.string.account_setup_failed_dlg_server_message_fmt,
                    errorMessageForCertificateException(cve));
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
            incomingSettings = RemoteStore.decodeStoreUri(viewModel.accountConfig.getStoreUri());

            currentIncomingAuthType = incomingSettings.authenticationType;

            accountSetupView.setAuthTypeInIncoming(currentIncomingAuthType);

            updateViewFromAuthTypeInIncoming(currentIncomingAuthType);

            currentIncomingSecurityType = incomingSettings.connectionSecurity;

            if (incomingSettings.username != null) {
                accountSetupView.setUsernameInIncoming(incomingSettings.username);
            }

            if (incomingSettings.password != null) {
                accountSetupView.setPasswordInIncoming(incomingSettings.password);
            }

            if (incomingSettings.clientCertificateAlias != null) {
                accountSetupView.setCertificateAliasInIncoming(incomingSettings.clientCertificateAlias);
            }

            if (Type.POP3 == incomingSettings.type) {
                accountSetupView.setServerLabel(getString(R.string.account_setup_incoming_pop_server_label));

                accountSetupView.hideViewsWhenPop3();
            } else if (Type.IMAP == incomingSettings.type) {
                accountSetupView.setServerLabel(getString(R.string.account_setup_incoming_imap_server_label));

                ImapStoreSettings imapSettings = (ImapStoreSettings) incomingSettings;

                accountSetupView.setImapAutoDetectNamespace(imapSettings.autoDetectNamespace);
                if (imapSettings.pathPrefix != null) {
                    accountSetupView.setImapPathPrefix(imapSettings.pathPrefix);
                }

                accountSetupView.hideViewsWhenImap();

                if (!editSettings) {
                    accountSetupView.hideViewsWhenImapAndNotEdit();
                }
            } else if (Type.WebDAV == incomingSettings.type) {
                accountSetupView.setServerLabel(getString(R.string.account_setup_incoming_webdav_server_label));
                connectionSecurityChoices = new ConnectionSecurity[] {
                        ConnectionSecurity.NONE,
                        ConnectionSecurity.SSL_TLS_REQUIRED };

                accountSetupView.hideViewsWhenWebDav();
                WebDavStoreSettings webDavSettings = (WebDavStoreSettings) incomingSettings;

                if (webDavSettings.path != null) {
                    accountSetupView.setWebDavPathPrefix(webDavSettings.path);
                }

                if (webDavSettings.authPath != null) {
                    accountSetupView.setWebDavAuthPath(webDavSettings.authPath);
                }

                if (webDavSettings.mailboxPath != null) {
                    accountSetupView.setWebDavMailboxPath(webDavSettings.mailboxPath);
                }
            } else {
                throw new IllegalArgumentException("Unknown account type: " + viewModel.accountConfig.getStoreUri());
            }

            if (!editSettings) {
                viewModel.accountConfig.setDeletePolicy(AccountCreator.getDefaultDeletePolicy(incomingSettings.type));
            }

            accountSetupView.setSecurityChoices(connectionSecurityChoices);

            accountSetupView.setSecurityTypeInIncoming(currentIncomingSecurityType);
            updateAuthPlainTextFromSecurityType(currentIncomingSecurityType);

            if (incomingSettings.host != null) {
                accountSetupView.setServerInIncoming(incomingSettings.host);
            }

            if (incomingSettings.port != -1) {
                String port = String.valueOf(incomingSettings.port);
                accountSetupView.setPortInIncoming(port);
                currentIncomingPort = port;
            } else {
                updatePortFromSecurityTypeInIncoming(currentIncomingSecurityType);
            }

            if (editSettings) {
                accountSetupView.setCompressionSectionVisibility(android.view.View.VISIBLE);
                accountSetupView.setImapPathPrefixSectionVisibility(android.view.View.VISIBLE);
            }
            accountSetupView.setCompressionMobile(viewModel.accountConfig.useCompression(NetworkType.MOBILE));
            accountSetupView.setCompressionWifi(viewModel.accountConfig.useCompression(NetworkType.WIFI));
            accountSetupView.setCompressionOther(viewModel.accountConfig.useCompression(NetworkType.OTHER));

            accountSetupView.setSubscribedFoldersOnly(viewModel.accountConfig.subscribedFoldersOnly());

        } catch (IllegalArgumentException e) {
            accountSetupView.showFailureToast(e);
        }
    }

    @Override
    public void onIncomingStart() {
        onIncomingStart(editSettings);
    }

    private void updatePortFromSecurityTypeInIncoming(ConnectionSecurity securityType) {
        String port = String.valueOf(AccountCreator.getDefaultPort(securityType, incomingSettings.type));
        accountSetupView.setPortInIncoming(port);
        currentIncomingPort = port;
    }

    private void updateAuthPlainTextFromSecurityType(ConnectionSecurity securityType) {
        accountSetupView.setAuthTypeInsecureText(securityType == ConnectionSecurity.NONE);
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
        } else if (Type.WebDAV == incomingSettings.type) {
            extra = new HashMap<>();
            extra.put(WebDavStoreSettings.PATH_KEY,
                    webdavPathPrefix);
            extra.put(WebDavStoreSettings.AUTH_PATH_KEY,
                    webdavAuthPath);
            extra.put(WebDavStoreSettings.MAILBOX_PATH_KEY,
                    webdavMailboxPath);
        }

        viewModel.accountConfig.deleteCertificate(host, port, CheckDirection.INCOMING);
        incomingSettings = new ServerSettings(incomingSettings.type, host, port,
                connectionSecurity, authType, username, password, clientCertificateAlias, extra);

        viewModel.accountConfig.setStoreUri(RemoteStore.createStoreUri(incomingSettings));

        viewModel.accountConfig.setCompression(NetworkType.MOBILE, compressMobile);
        viewModel.accountConfig.setCompression(NetworkType.WIFI, compressWifi);
        viewModel.accountConfig.setCompression(NetworkType.OTHER, compressOther);
        viewModel.accountConfig.setSubscribedFoldersOnly(subscribedFoldersOnly);

        accountSetupView.goToIncomingChecking();
    }

    private void revokeInvalidSettingsAndUpdateViewInIncoming(AuthType authType,
            ConnectionSecurity connectionSecurity,
            String port) {
        boolean isAuthTypeExternal = (AuthType.EXTERNAL == authType);
        boolean hasConnectionSecurity = (connectionSecurity != ConnectionSecurity.NONE);

        if (isAuthTypeExternal && !hasConnectionSecurity) {
            accountSetupView.showInvalidSettingsToast();
            accountSetupView.setAuthTypeInIncoming(currentIncomingAuthType);
            accountSetupView.setSecurityTypeInIncoming(currentIncomingSecurityType);
            accountSetupView.setPortInIncoming(currentIncomingPort);
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
        accountSetupView.setAuthTypeInIncoming(authType);

        updateViewFromAuthTypeInIncoming(authType);
    }

    private void setSecurityTypeInIncoming(ConnectionSecurity securityType) {
        accountSetupView.setSecurityTypeInIncoming(securityType);

        updatePortFromSecurityTypeInIncoming(securityType);
        updateAuthPlainTextFromSecurityType(securityType);
    }


    private void validateFieldInIncoming(String certificateAlias, String server, String port,
            String username, String password, AuthType authType, ConnectionSecurity connectionSecurity) {
        boolean isAuthTypeOAuth = (AuthType.XOAUTH2 == authType);
        boolean isOAuthValid = canOAuth2(username);
        boolean isAuthTypeExternal = (AuthType.EXTERNAL == authType);
        boolean hasConnectionSecurity = (connectionSecurity != ConnectionSecurity.NONE);

        boolean hasValidCertificateAlias = certificateAlias != null;
        boolean hasValidUserName = Utility.requiredFieldValid(username);

        boolean hasValidPasswordSettings = hasValidUserName
                && !isAuthTypeExternal && !isAuthTypeOAuth
                && Utility.requiredFieldValid(password);

        boolean hasValidExternalAuthSettings = hasValidUserName
                && isAuthTypeExternal
                && hasConnectionSecurity
                && hasValidCertificateAlias;

        boolean hasValidOAuth2Settings = hasValidUserName
                && isAuthTypeOAuth
                && isOAuthValid;

        final boolean enabled = Utility.domainFieldValid(server)
                && Utility.requiredFieldValid(port)
                && (hasValidPasswordSettings || hasValidExternalAuthSettings
                || hasValidOAuth2Settings);

        checkInvalidOAuthError(isAuthTypeOAuth, isOAuthValid);
        accountSetupView.setNextButtonInIncomingEnabled(enabled);
    }

    private void checkInvalidOAuthError(boolean isAuthTypeOAuth, boolean isOAuthValid) {
        if (isAuthTypeOAuth && !isOAuthValid) {
            accountSetupView.showInvalidOAuthError();
        } else {
            accountSetupView.clearInvalidOAuthError();
        }
    }

    private void updateAccount() {
        // Account account = (Account) viewModel.accountConfig;

        /*
        boolean isPushCapable = false;
        try {
            RemoteStore store = account.getRemoteStore();
            isPushCapable = store.isPushCapable();
        } catch (Exception e) {
            Timber.e(e, "Could not get remote store");
        }
        if (isPushCapable && account.getFolderPushMode() != FolderMode.NONE) {
            MailService.actionRestartPushers(accountSetupView.getContext(), null);
        }
        account.save(preferences);
        */
    }

    private void updateViewFromAuthTypeInIncoming(AuthType authType) {
        if (authType == AuthType.EXTERNAL) {
            accountSetupView.setViewExternalInIncoming();
        } else if (authType == AuthType.XOAUTH2) {
            accountSetupView.setViewOAuth2InIncoming();
        } else {
            accountSetupView.setViewNotExternalInIncoming();
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
            accountSetupView.setDoneButtonInNamesEnabled(true);
        } else {
            accountSetupView.setDoneButtonInNamesEnabled(false);
        }
    }

    @Override
    public void onNextButtonInNamesClicked(String name, String description) {
        if (Utility.requiredFieldValid(description)) {
            viewModel.accountConfig.setDescription(description);
        }

        viewModel.accountConfig.setName(name);

        Account account = preferences.newAccount();
        account.loadConfig(viewModel.accountConfig);

        MessagingController.getInstance(context).listFoldersSynchronous(account, true, null);
        MessagingController.getInstance(context)
                .synchronizeMailbox(account, account.getInboxFolderName(), null, null);

        account.save(preferences);

        if (account.equals(preferences.getDefaultAccount()) || makeDefault) {
            preferences.setDefaultAccount(account);
        }

        K9.setServicesEnabled(context);

        accountSetupView.goToListAccounts();
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
        if (viewModel.accountConfig.getStoreUri().startsWith("webdav")) {
            viewModel.accountConfig.setTransportUri(viewModel.accountConfig.getStoreUri());
            accountSetupView.goToOutgoingChecking();
            return;
        }

        try {
            outgoingSettings = TransportUris.decodeTransportUri(viewModel.accountConfig.getTransportUri());

            currentOutgoingAuthType = outgoingSettings.authenticationType;
            setAuthTypeInOutgoing(currentOutgoingAuthType);

            currentOutgoingSecurityType = outgoingSettings.connectionSecurity;
            setSecurityTypeInOutgoing(currentOutgoingSecurityType);

            if (outgoingSettings.username != null && !outgoingSettings.username.isEmpty()) {
                accountSetupView.setUsernameInOutgoing(outgoingSettings.username);
            }

            if (outgoingSettings.password != null) {
                accountSetupView.setPasswordInOutgoing(outgoingSettings.password);
            }

            if (outgoingSettings.clientCertificateAlias != null) {
                accountSetupView.setCertificateAliasInOutgoing(outgoingSettings.clientCertificateAlias);
            }

            if (outgoingSettings.host != null) {
                accountSetupView.setServerInOutgoing(outgoingSettings.host);
            }

            if (outgoingSettings.port != -1) {
                currentOutgoingPort = String.valueOf(outgoingSettings.port);
                accountSetupView.setPortInOutgoing(currentOutgoingPort);
            }
        } catch (Exception e) {
            accountSetupView.showFailureToast(e);
        }
    }


    private void setAuthTypeInOutgoing(AuthType authType) {
        accountSetupView.setAuthTypeInOutgoing(authType);

        updateViewFromAuthTypeInOutgoing(authType);
    }

    private void setSecurityTypeInOutgoing(ConnectionSecurity securityType) {
        accountSetupView.setSecurityTypeInOutgoing(securityType);

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

        viewModel.accountConfig.deleteCertificate(host, port, CheckDirection.OUTGOING);
        ServerSettings server = new ServerSettings(Type.SMTP, host, port, connectionSecurity,
                authType, username, password, clientCertificateAlias);
        String uri = TransportUris.createTransportUri(server);
        viewModel.accountConfig.setTransportUri(uri);

        accountSetupView.goToOutgoingChecking();
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
                accountSetupView.setAuthTypeInOutgoing(authType);
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

        boolean isAuthTypeOAuth = (AuthType.XOAUTH2 == authType);
        boolean isAuthTypeExternal = (AuthType.EXTERNAL == authType);
        boolean hasConnectionSecurity = (connectionSecurity != ConnectionSecurity.NONE);

        boolean hasValidCertificateAlias = certificateAlias != null;
        boolean hasValidUserName = Utility.requiredFieldValid(username);
        boolean isOAuthValid = hasValidUserName && canOAuth2(username);

        boolean hasValidPasswordSettings = hasValidUserName
                && !isAuthTypeExternal
                && Utility.requiredFieldValid(password);

        boolean hasValidExternalAuthSettings = hasValidUserName
                && isAuthTypeExternal
                && hasConnectionSecurity
                && hasValidCertificateAlias;

        boolean hasValidOAuth2Settings = hasValidUserName
                && isAuthTypeOAuth
                && isOAuthValid;

        boolean enabled = Utility.domainFieldValid(server)
                && Utility.requiredFieldValid(port)
                && (!requireLogin
                || hasValidPasswordSettings || hasValidExternalAuthSettings
                || hasValidOAuth2Settings);

        checkInvalidOAuthError(isAuthTypeOAuth, isOAuthValid);

        accountSetupView.setNextButtonInOutgoingEnabled(enabled);
    }

    private void revokeInvalidSettingsAndUpdateViewInOutgoing(AuthType authType,
            ConnectionSecurity connectionSecurity,
            String port) {
        boolean isAuthTypeExternal = (AuthType.EXTERNAL == authType);

        boolean hasConnectionSecurity = (connectionSecurity != ConnectionSecurity.NONE);

        if (isAuthTypeExternal && !hasConnectionSecurity) {
            accountSetupView.showInvalidSettingsToastInOutgoing();
            accountSetupView.setAuthTypeInOutgoing(currentOutgoingAuthType);
            accountSetupView.setSecurityTypeInOutgoing(currentOutgoingSecurityType);
            accountSetupView.setPortInOutgoing(currentOutgoingPort);
        } else {
            currentOutgoingPort = port;

            onAuthTypeSelectedInOutgoing(authType);
            onSecuritySelectedInOutgoing(connectionSecurity);

            currentOutgoingAuthType = authType;
            currentOutgoingSecurityType = connectionSecurity;
        }
    }

    private void updateViewFromSecurityTypeInOutgoing(ConnectionSecurity securityType) {
        accountSetupView.updateAuthPlainTextInOutgoing(securityType == ConnectionSecurity.NONE);

        String port = String.valueOf(AccountCreator.getDefaultPort(securityType, Type.SMTP));
        accountSetupView.setPortInOutgoing(port);
        currentOutgoingPort = port;
    }

    private void updateViewFromAuthTypeInOutgoing(AuthType authType) {
        if (authType == AuthType.EXTERNAL) {
            accountSetupView.setViewExternalInOutgoing();
        } else if (authType == AuthType.XOAUTH2) {
            accountSetupView.setViewOAuth2InOutgoing();
        } else {
            accountSetupView.setViewNotExternalInOutgoing();
        }
    }

    // endregion outgoing

    // region account type
    @Override
    public void onAccountTypeStart() {
        stage = Stage.ACCOUNT_TYPE;
    }

    @Override
    public void onNextButtonInAccountTypeClicked(Type serverType) {
        switch (serverType) {
            case IMAP:
                onImapOrPop3Selected(Type.IMAP);
                break;
            case POP3:
                onImapOrPop3Selected(Type.POP3);
                break;
            case WebDAV:
                onWebdavSelected();
                break;
        }
    }

    private void onImapOrPop3Selected(Type serverType) {
        ServerNameSuggester serverNameSuggester = new ServerNameSuggester();

        /*
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
        */

        accountSetupView.goToIncomingSettings();
    }

    private void onWebdavSelected() {
        ServerNameSuggester serverNameSuggester = new ServerNameSuggester();

        String storeUri = viewModel.accountConfig.getStoreUri();

        /*
        /*
         * The user info we have been given from
         * BasicsView.onManualSetup() is encoded as an IMAP store
         * URI: AuthType:UserName:Password (no fields should be empty).
         * However, AuthType is not applicable to WebDAV nor to its store
         * URI. Re-encode without it, using just the UserName and Password.
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
        */

        accountSetupView.goToIncomingSettings();
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
                accountSetupView.goToBasics();
                break;
            case INCOMING:
                if (!editSettings) {
                    stage = Stage.ACCOUNT_TYPE;
                    accountSetupView.goToAccountType();
                } else {
                    accountSetupView.end();
                }
                break;
            case INCOMING_CHECKING:
                stage = Stage.INCOMING;
                accountSetupView.goToIncoming();
                break;
            case OUTGOING:
                if (!editSettings) {
                    stage = Stage.INCOMING;
                    accountSetupView.goToIncoming();
                } else {
                    accountSetupView.end();
                }
                break;
            case OUTGOING_CHECKING:
            case ACCOUNT_NAMES:
                if (isManualSetup) {
                    stage = Stage.OUTGOING;
                    accountSetupView.goToOutgoing();
                } else {
                    stage = Stage.BASICS;
                    accountSetupView.goToBasics();
                }
                break;
            default:
                accountSetupView.end();
                break;
        }
    }

    @Override
    public Account getAccount() {
        return null;
        // TODO
        // return (Account) viewModel.accountConfig;
    }

    public boolean isEditSettings() {
        return editSettings;
    }

    @Override
    public void onGetAccountConfig(@Nullable ManualSetupInfo accountConfig) {
        this.viewModel.accountConfig = accountConfig;
    }

    @Override
    public AccountConfig getAccountConfig() {
        return viewModel.accountConfig;
    }

    @Override
    public void onGetMakeDefault(boolean makeDefault) {
        this.makeDefault = makeDefault;
    }

    private void manualSetup(String email, String password) {
        isManualSetup = true;

        if (viewModel.accountConfig == null) {
            viewModel.accountConfig = new ManualSetupInfo();
        }

        viewModel.accountConfig.init(email, password);

        accountSetupView.goToAccountType();
    }

    @Override
    public void handleGmailXOAuth2Intent(Intent intent) {
        accountSetupView.startIntentForResult(intent, REQUEST_CODE_GMAIL);
    }

    @Override
    public void handleGmailRedirectUrl(final String url) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                accountSetupView.openGmailUrl(url);
            }
        });
    }

    @Override
    public void handleOutlookRedirectUrl(final String url) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                accountSetupView.openOutlookUrl(url);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_GMAIL) {
            if (resultCode == Activity.RESULT_OK) {
                checkIncomingAndOutgoing();
            } else {
                accountSetupView.goBack();
            }
        }
    }

    @Override
    public void onOAuthCodeGot(final String code) {
        oAuth2CodeGotten = true;
        accountSetupView.closeAuthDialog();
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    Globals.getOAuth2TokenProvider().getAuthorizationCodeFlowTokenProvider().exchangeCode(viewModel.accountConfig.getEmail(), code);
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
                    accountSetupView.goToBasics();
                    accountSetupView.showErrorDialog("Error when exchanging code");
                }
            }
        }.execute();
    }

    @Override
    public void onErrorWhenGettingOAuthCode(String errorMessage) {
        oAuth2CodeGotten = false;
        accountSetupView.closeAuthDialog();
        accountSetupView.goToBasics();
        accountSetupView.showErrorDialog(errorMessage);
    }

    @Override
    public void onWebViewDismiss() {
        if (!oAuth2CodeGotten) {
            accountSetupView.goToBasics();
            accountSetupView.showErrorDialog("Please connect us with Gmail"); // TODO: 8/18/17 A better error message?
        }
    }
}

