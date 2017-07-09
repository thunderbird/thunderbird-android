package com.fsck.k9.activity.setup.outgoing;


import java.net.URI;
import java.net.URISyntaxException;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
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

    OutgoingPresenter(View view, Account account) {
        this.view = view;
        this.account = account;
        view.setPresenter(this);
    }

    @Override
    public void updateAccount() {
        account.save(Preferences.getPreferences(K9.app));
    }

    @Override
    public Account getAccount() {
        return account;
    }

    @Override
    public void setAccount(Account account) {
        this.account = account;
    }

    @Override
    public void checkWebdav() {
        try {
            if (new URI(account.getStoreUri()).getScheme().startsWith("webdav")) {
                account.setTransportUri(account.getStoreUri());
                view.checkAccount(account);
            }
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void next(String username, String password, String clientCertificateAlias,
            String host, int port, ConnectionSecurity connectionSecurity, AuthType authType, boolean requireLogin) {

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

        view.checkAccount(account);
    }

    @Override
    public void validateFields(String certificateAlias, String server, String port,
            String username, String password, AuthType authType, ConnectionSecurity connectionSecurity,
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

        view.enableNext(enabled);
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
}
