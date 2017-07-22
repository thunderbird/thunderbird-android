package com.fsck.k9.activity.setup.outgoing;


import java.net.URI;
import java.net.URISyntaxException;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.account.AccountCreator;
import com.fsck.k9.activity.setup.IncomingAndOutgoingState;
import com.fsck.k9.activity.setup.checksettings.CheckSettingsPresenter.CheckDirection;
import com.fsck.k9.activity.setup.outgoing.OutgoingContract.View;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.Transport;


class OutgoingPresenter implements OutgoingContract.Presenter {
    private View view;
    private Account account;
    private ServerSettings settings;

    private ConnectionSecurity currentSecurityType;
    private AuthType currentAuthType;
    private String currentPort;

    OutgoingPresenter(View view, String accountUuid) {
        this.view = view;
        setAccount(accountUuid);
        view.setPresenter(this);
    }

    OutgoingPresenter(View view, Account account) {
        this.view = view;
        setAccount(account);
        view.setPresenter(this);
    }

    @Override
    public void onAccountEdited() {
        account.save(Preferences.getPreferences(K9.app));
    }

    @Override
    public Account getAccount() {
        return account;
    }

    private void setAccount(Account account) {
        this.account = account;

        try {
            if (new URI(account.getStoreUri()).getScheme().startsWith("webdav")) {
                account.setTransportUri(account.getStoreUri());
                view.next(account.getUuid());

                return;
            }
        } catch (URISyntaxException e) {
            view.onAccountLoadFailure(e);
        }

        try {
            settings = Transport.decodeTransportUri(account.getTransportUri());

            currentAuthType = settings.authenticationType;
            setAuthType(currentAuthType);

            currentSecurityType = settings.connectionSecurity;
            setSecurityType(currentSecurityType);

            if (settings.username != null && !settings.username.isEmpty()) {
                view.setUsername(settings.username);
            }

            if (settings.password != null) {
                view.setPassword(settings.password);
            }

            if (settings.clientCertificateAlias != null) {
                view.setCertificateAlias(settings.clientCertificateAlias);
            }

            if (settings.host != null) {
                view.setServer(settings.host);
            }

            if (settings.port != -1) {
                currentPort = String.valueOf(settings.port);
                view.setPort(currentPort);
            }
        } catch (Exception e) {
            view.onAccountLoadFailure(e);
        }
    }

    @Override
    public void setAccount(String accountUuid) {
        Account account = Preferences.getPreferences(K9.app).getAccount(accountUuid);

        setAccount(account);
    }

    private void setAuthType(AuthType authType) {
        view.setAuthType(authType);

        updateViewFromAuthType(authType);
    }

    private void setSecurityType(ConnectionSecurity securityType) {
        view.setSecurityType(securityType);

        updateViewFromSecurityType(securityType);
    }

    @Override
    public void setState(IncomingAndOutgoingState state) {
        view.setAuthType(state.getAuthType());
        view.setSecurityType(state.getConnectionSecurity());

        currentAuthType = state.getAuthType();
        currentSecurityType = state.getConnectionSecurity();
    }

    private void onSecuritySelected(ConnectionSecurity securityType) {
        if (securityType != currentSecurityType) {
            updateViewFromSecurityType(securityType);
        }
    }

    private void onAuthTypeSelected(AuthType authType) {
        if (authType != currentAuthType) {
            setAuthType(authType);
        }
    }

    @Override
    public void onNext(String username, String password, String clientCertificateAlias,
                       String host, int port, ConnectionSecurity connectionSecurity,
                       AuthType authType, boolean requireLogin) {

        if (!requireLogin) {
            username = null;
            password = null;
            authType = null;
            clientCertificateAlias = null;
        }

        ServerSettings server = new ServerSettings(Type.SMTP, host, port, connectionSecurity,
                authType, username, password, clientCertificateAlias);
        String uri = Transport.createTransportUri(server);
        account.deleteCertificate(host, port, CheckDirection.OUTGOING);
        account.setTransportUri(uri);

        view.next(account.getUuid());
    }

    @Override
    public void onInputChanged(String certificateAlias, String server, String port, String username,
                               String password, AuthType authType,
                               ConnectionSecurity connectionSecurity, boolean requireLogin) {

        if (currentSecurityType != connectionSecurity) {
            boolean isAuthTypeExternal = (AuthType.EXTERNAL == authType);

            boolean hasConnectionSecurity = (connectionSecurity != ConnectionSecurity.NONE);

            if (isAuthTypeExternal && !hasConnectionSecurity && !requireLogin) {
                authType = AuthType.PLAIN;
                view.setAuthType(authType);
                updateViewFromAuthType(authType);
            }
        }

        revokeInvalidSettingsAndUpdateView(authType, connectionSecurity, port);
        validateFields(certificateAlias, server, port, username, password, authType,
                connectionSecurity, requireLogin);
    }

    private void validateFields(String certificateAlias, String server, String port,
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

        view.setNextButtonEnabled(enabled);
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

    private void updateViewFromSecurityType(ConnectionSecurity securityType) {
        view.updateAuthPlainText(securityType == ConnectionSecurity.NONE);

        String port = String.valueOf(AccountCreator.getDefaultPort(securityType, Type.SMTP));
        view.setPort(port);
        currentPort = port;
    }

    private void updateViewFromAuthType(AuthType authType) {
        if (authType == AuthType.EXTERNAL) {
            view.onAuthTypeIsExternal();
        } else {
            view.onAuthTypeIsNotExternal();
        }
    }

    @Override
    public IncomingAndOutgoingState getState() {
        return new IncomingAndOutgoingState(currentAuthType, currentSecurityType);
    }
}
