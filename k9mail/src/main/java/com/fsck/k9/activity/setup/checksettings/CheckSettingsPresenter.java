package com.fsck.k9.activity.setup.checksettings;


import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.account.AccountCreator;
import com.fsck.k9.activity.setup.AbstractAccountSetup.AccountState;
import com.fsck.k9.activity.setup.checksettings.CheckSettingsContract.View;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.helper.EmailHelper;
import com.fsck.k9.helper.UrlEncodingHelper;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.TransportProvider;
import com.fsck.k9.mail.filter.Hex;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mail.store.webdav.WebDavStore;
import timber.log.Timber;


public class CheckSettingsPresenter implements CheckSettingsContract.Presenter {
    private View view;
    private Account account;
    private CheckDirection currentDirection;
    private CheckDirection direction;
    private AccountState state;

    public enum CheckDirection {
        INCOMING,
        OUTGOING,
        BOTH
    }

    CheckSettingsPresenter(View view) {
        this.view = view;
        view.setPresenter(this);
    }

    CheckSettingsPresenter(View view, Account account) {
        this(view, account, CheckDirection.BOTH);
    }

    CheckSettingsPresenter(View view, Account account, CheckDirection direction) {
        this.view = view;
        view.setPresenter(this);
        this.direction = direction;
        if (direction == CheckDirection.BOTH) {
            currentDirection = CheckDirection.INCOMING;
        } else {
            currentDirection = direction;
        }
        this.account = account;
    }

    @Override
    public void skip() {
        if (direction == CheckDirection.BOTH && currentDirection == CheckDirection.INCOMING) {
            checkOutgoing();
        } else if (currentDirection == CheckDirection.OUTGOING){
            view.goToNames();
        } else {
            view.goToOutgoing();
        }
    }


    private void autoConfiguration(final String email, final String password) {
        view.setMessage(R.string.account_setup_check_settings_retr_info_msg);

        EmailHelper emailHelper = new EmailHelper();
        String domain = emailHelper.splitEmail(email)[1];
        Provider provider = findProviderForDomain(domain);
        if (provider == null) {
            view.autoConfigurationFail();
            return;
        }

        try {
            modifyAccount(email, password, provider);

            checkIncomingAndOutgoing();
        } catch (URISyntaxException e) {
            Preferences.getPreferences(K9.app).deleteAccount(account);
            view.autoConfigurationFail();
        }
    }

    private void checkIncomingAndOutgoing() {
        currentDirection = CheckDirection.INCOMING;
        new CheckIncomingTask(account, new CheckSettingsSuccessCallback() {
            @Override
            public void onCheckSuccess() {
                checkOutgoing();
            }
        }).execute();
    }

    private void checkIncoming() {
        currentDirection = CheckDirection.INCOMING;
        new CheckIncomingTask(account, new CheckSettingsSuccessCallback() {
            @Override
            public void onCheckSuccess() {
                // TODO: 7/23/2017 should it be changed?
                account.setDescription(account.getEmail());
                account.save(Preferences.getPreferences(K9.app));
                K9.setServicesEnabled(K9.app);

                view.goToOutgoing();
            }
        }).execute();
    }

    private void checkOutgoing() {
        currentDirection = CheckDirection.OUTGOING;

        new CheckOutgoingTask(account, new CheckSettingsSuccessCallback() {
            @Override
            public void onCheckSuccess() {
                //We've successfully checked outgoing as well.
                account.setDescription(account.getEmail());
                account.save(Preferences.getPreferences(K9.app));
                K9.setServicesEnabled(K9.app);

                view.goToNames();
            }
        }).execute();
    }

    @Override
    public void onViewStart(AccountState state) {
        this.state = state;

        int step = state.getStep();

        switch (step) {
            case AccountState.STEP_AUTO_CONFIGURATION:
                autoConfiguration(state.getEmail(), state.getPassword());
                break;
            case AccountState.STEP_CHECK_INCOMING:
                checkIncoming();
                break;
            case AccountState.STEP_CHECK_OUTGOING:
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
    private abstract class CheckAccountTask extends AsyncTask<CheckDirection, Integer, Void> {
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
        protected Void doInBackground(CheckDirection... params) {
            try {
                /*
                 * This task could be interrupted at any point, but network operations can block,
                 * so relying on InterruptedException is not enough. Instead, check after
                 * each potentially long-running operation.
                 */
                if (cancelled()) {
                    return null;
                }

                checkSettings();

                if (cancelled()) {
                    return null;
                }

                if (callback != null) {
                    callback.onCheckSuccess();
                }


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
            return null;
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
            account.addCertificate(direction, certificate);
        } catch (CertificateException e) {
            view.showErrorDialog(
                    R.string.account_setup_failed_dlg_certificate_message_fmt,
                    e.getMessage() == null ? "" : e.getMessage());
        }

        replayChecking();
    }

    @Override
    public void onError() {
        if (state.getStep() == AccountState.STEP_AUTO_CONFIGURATION) {
            view.goToBasics();
        } else if (state.getStep() == AccountState.STEP_CHECK_INCOMING) {
            view.goToIncoming();
        } else {
            view.goToOutgoing();
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
                    String storeURIHost = (Uri.parse(account.getStoreUri())).getHost();
                    String transportURIHost = (Uri.parse(account.getTransportUri())).getHost();

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
                        if (name.equalsIgnoreCase(storeURIHost) || name.equalsIgnoreCase(transportURIHost)) {
                            //TODO: localize this string
                            altNamesText.append("Subject(alt): ").append(name).append(",...\n");
                        } else if (name.startsWith("*.") && (
                                storeURIHost.endsWith(name.substring(2)) ||
                                        transportURIHost.endsWith(name.substring(2)))) {
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
}


