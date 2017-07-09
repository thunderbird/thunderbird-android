package com.fsck.k9.activity.setup.incoming;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.fsck.k9.Account;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.setup.checksettings.CheckSettingsPresenter.CheckDirection;
import com.fsck.k9.activity.setup.incoming.IncomingContract.View;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.NetworkType;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mail.store.imap.ImapStoreSettings;
import com.fsck.k9.mail.store.webdav.WebDavStoreSettings;
import com.fsck.k9.service.MailService;
import timber.log.Timber;


class IncomingPresenter implements IncomingContract.Presenter {
    private View view;
    private Account account;
    private ServerSettings settings;

    IncomingPresenter(View view, Account account) {
        this.view = view;
        this.account = account;
        view.setPresenter(this);

        settings = RemoteStore.decodeStoreUri(account.getStoreUri());
    }

    @Override
    public Account getAccount() {
        return account;
    }

    @Override
    public void modifyAccount(String username, String password, String clientCertificateAlias,
            boolean autoDetectNamespace, String imapPathPrefix, String webdavPathPrefix, String webdavAuthPath,
            String webdavMailboxPath, String host, int port, ConnectionSecurity connectionSecurity,
            AuthType authType, boolean compressMobile, boolean compressWifi, boolean compressOther,
            boolean subscribedFoldersOnly) {

        if (authType == AuthType.EXTERNAL) {
            password = null;
        } else {
            clientCertificateAlias = null;
        }

        Type storeType = settings.type;

        Map<String, String> extra = null;
        if (Type.IMAP == storeType) {
            extra = new HashMap<String, String>();
            extra.put(ImapStoreSettings.AUTODETECT_NAMESPACE_KEY,
                    Boolean.toString(autoDetectNamespace));
            extra.put(ImapStoreSettings.PATH_PREFIX_KEY,
                    imapPathPrefix);
        } else if (Type.WebDAV == storeType) {
            extra = new HashMap<String, String>();
            extra.put(WebDavStoreSettings.PATH_KEY,
                    webdavPathPrefix);
            extra.put(WebDavStoreSettings.AUTH_PATH_KEY,
                    webdavAuthPath);
            extra.put(WebDavStoreSettings.MAILBOX_PATH_KEY,
                    webdavMailboxPath);
        }

        account.deleteCertificate(host, port, CheckDirection.INCOMING);
        ServerSettings settings = new ServerSettings(storeType, host, port,
                connectionSecurity, authType, username, password, clientCertificateAlias, extra);

        account.setStoreUri(RemoteStore.createStoreUri(settings));

        account.setCompression(NetworkType.MOBILE, compressMobile);
        account.setCompression(NetworkType.WIFI, compressWifi);
        account.setCompression(NetworkType.OTHER, compressOther);
        account.setSubscribedFoldersOnly(subscribedFoldersOnly);
    }

    @Override
    public void revokeInvalidSettings(AuthType authType, ConnectionSecurity connectionSecurity) {
        boolean isAuthTypeExternal = (AuthType.EXTERNAL == authType);

        boolean hasConnectionSecurity = (connectionSecurity != ConnectionSecurity.NONE);

        if (isAuthTypeExternal && !hasConnectionSecurity) {
            view.resetPreviousSettings();
        } else {
            view.saveSettings();
        }
    }

    @Override
    public void validateFields(String certificateAlias, String server, String port,
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

        view.enableNext(enabled);
    }

    @Override
    public void prepareForOutgoing(String username, String password, String certificateAlias, AuthType authType) {
        if (AuthType.EXTERNAL == authType) {
            password = null;
        } else {
            certificateAlias = null;
        }

        try {
            URI oldUri = new URI(account.getTransportUri());
            ServerSettings transportServer = new ServerSettings(Type.SMTP, oldUri.getHost(), oldUri.getPort(),
                    ConnectionSecurity.SSL_TLS_REQUIRED, authType, username, password, certificateAlias);
            String transportUri = Transport.createTransportUri(transportServer);
            account.setTransportUri(transportUri);
        } catch (URISyntaxException e) {
            Timber.e("Error while getting transport uri");
        }

        view.goToOutgoingSettings(account);
    }

    @Override
    public void updateAccount() {
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
}
