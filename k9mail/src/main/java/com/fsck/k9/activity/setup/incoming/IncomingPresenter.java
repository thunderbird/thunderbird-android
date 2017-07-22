package com.fsck.k9.activity.setup.incoming;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.fsck.k9.Account;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.account.AccountCreator;
import com.fsck.k9.activity.setup.IncomingAndOutgoingState;
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
    private Type storeType;

    private AuthType currentAuthType;
    private ConnectionSecurity currentSecurityType;
    private String currentPort;

    IncomingPresenter(View view, String accountUuid, boolean editSettings) {
        this.view = view;
        this.account = Preferences.getPreferences(K9.app).getAccount(accountUuid);
        view.setPresenter(this);

        ConnectionSecurity[] connectionSecurityChoices = ConnectionSecurity.values();
        try {
            settings = RemoteStore.decodeStoreUri(account.getStoreUri());

            /* if (savedInstanceState == null) {
                // The first item is selected if settings.authenticationType is null or is not in authTypeAdapter
                currentAuthTypeViewPosition = authTypeAdapter.getAuthPosition(settings.authenticationType);
            } else {
                currentAuthTypeViewPosition = savedInstanceState.getInt(STATE_AUTH_TYPE_POSITION);
            } */

            currentAuthType = settings.authenticationType;

            view.setAuthType(currentAuthType);

            updateViewFromAuthType(currentAuthType);

            currentSecurityType = settings.connectionSecurity;

            if (settings.username != null) {
                view.setUsername(settings.username);
            }

            if (settings.password != null) {
                view.setPassword(settings.password);
            }

            if (settings.clientCertificateAlias != null) {
                view.setCertificateAlias(settings.clientCertificateAlias);
            }

            storeType = settings.type;
            if (Type.POP3 == settings.type) {
                view.setServerLabel(getString(R.string.account_setup_incoming_pop_server_label));

                view.hideViewsWhenPop3();
            } else if (Type.IMAP == settings.type) {
                view.setServerLabel(getString(R.string.account_setup_incoming_imap_server_label));

                ImapStoreSettings imapSettings = (ImapStoreSettings) settings;

                view.setImapAutoDetectNamespace(imapSettings.autoDetectNamespace);
                if (imapSettings.pathPrefix != null) {
                    view.setImapPathPrefix(imapSettings.pathPrefix);
                }

                view.hideViewsWhenImap();

                if (!editSettings) {
                    view.hideViewsWhenImapAndNotEdit();
                }
            } else if (Type.WebDAV == settings.type) {
                view.setServerLabel(getString(R.string.account_setup_incoming_webdav_server_label));
                connectionSecurityChoices = new ConnectionSecurity[] {
                        ConnectionSecurity.NONE,
                        ConnectionSecurity.SSL_TLS_REQUIRED };

                // Hide the unnecessary fields
                view.hideViewsWhenWebDav();
                WebDavStoreSettings webDavSettings = (WebDavStoreSettings) settings;

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
                account.setDeletePolicy(AccountCreator.getDefaultDeletePolicy(settings.type));
            }

            view.setSecurityChoices(connectionSecurityChoices);

            updateAuthPlainTextFromSecurityType(currentSecurityType);

            if (settings.host != null) {
                view.setServer(settings.host);
            }

            if (settings.port != -1) {
                String port = String.valueOf(settings.port);
                view.setPort(port);
                currentPort = port;
            } else {
                updatePortFromSecurityType(currentSecurityType);
            }

            view.setCompressionMobile(account.useCompression(NetworkType.MOBILE));
            view.setCompressionWifi(account.useCompression(NetworkType.WIFI));
            view.setCompressionOther(account.useCompression(NetworkType.OTHER));

            view.setSubscribedFoldersOnly(account.subscribedFoldersOnly());

        } catch (IllegalArgumentException e) {
            view.onAccountLoadFailure(e);
        }
    }

    private void updatePortFromSecurityType(ConnectionSecurity securityType) {
        String port = String.valueOf(AccountCreator.getDefaultPort(securityType, storeType));
        view.setPort(port);
        currentPort = port;
    }

    private void updateAuthPlainTextFromSecurityType(ConnectionSecurity securityType) {
        view.setAuthTypeInsecureText(securityType == ConnectionSecurity.NONE);
    }

    @Override
    public Account getAccount() {
        return account;
    }

    @Override
    public IncomingAndOutgoingState getState() {
        return new IncomingAndOutgoingState(currentAuthType, currentSecurityType);
    }

    @Override
    public void setState(IncomingAndOutgoingState state) {
        view.setAuthType(state.getAuthType());
        view.setSecurityType(state.getConnectionSecurity());

        currentAuthType = state.getAuthType();
        currentSecurityType = state.getConnectionSecurity();
    }

    @Override
    public void onInputChanged(String certificateAlias, String server, String port,
                               String username, String password, AuthType authType,
                               ConnectionSecurity connectionSecurity) {

        revokeInvalidSettingsAndUpdateView(authType, connectionSecurity, port);
        validateFields(certificateAlias, server, currentPort, username, password, currentAuthType,
                currentSecurityType);
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
            extra = new HashMap<>();
            extra.put(ImapStoreSettings.AUTODETECT_NAMESPACE_KEY,
                    Boolean.toString(autoDetectNamespace));
            extra.put(ImapStoreSettings.PATH_PREFIX_KEY,
                    imapPathPrefix);
        } else if (Type.WebDAV == storeType) {
            extra = new HashMap<>();
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

    private void revokeInvalidSettingsAndUpdateView(AuthType authType,
                                                    ConnectionSecurity connectionSecurity,
                                                    String port) {
        boolean isAuthTypeExternal = (AuthType.EXTERNAL == authType);

        boolean hasConnectionSecurity = (connectionSecurity != ConnectionSecurity.NONE);

        if (isAuthTypeExternal && !hasConnectionSecurity) {
            view.showInvalidSettingsToast();
            view.setAuthType(currentAuthType);
            view.setSecurityType(currentSecurityType);
            view.setPort(currentPort);
        } else {
            onAuthTypeSelected(authType);
            onSecuritySelected(connectionSecurity);
            currentAuthType = authType;
            currentSecurityType = connectionSecurity;
            currentPort = port;
        }
    }

    private void onSecuritySelected(ConnectionSecurity securityType) {
        if (securityType != currentSecurityType) {
            setSecurityType(securityType);
        }
    }

    private void onAuthTypeSelected(AuthType authType) {
        if (authType != currentAuthType) {
            setAuthType(authType);
        }
    }

    private void setSecurityType(ConnectionSecurity securityType) {
        view.setSecurityType(securityType);

        updatePortFromSecurityType(securityType);
        updateAuthPlainTextFromSecurityType(securityType);
    }

    private void setAuthType(AuthType authType) {
        view.setAuthType(authType);

        updateViewFromAuthType(authType);
    }


    private void validateFields(String certificateAlias, String server, String port,
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

        view.setNextEnabled(enabled);
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

    private void updateViewFromAuthType(AuthType authType) {
        if (authType == AuthType.EXTERNAL) {
            view.onAuthTypeIsExternal();
        } else {
            view.onAuthTypeIsNotExternal();
        }
    }
    private String getString(int id) {
        return K9.app.getString(id);
    }
}
