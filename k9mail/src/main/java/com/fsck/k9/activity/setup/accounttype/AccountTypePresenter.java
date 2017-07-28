package com.fsck.k9.activity.setup.accounttype;


import java.net.URI;
import java.net.URISyntaxException;

import com.fsck.k9.Account;
import com.fsck.k9.activity.setup.AbstractAccountSetup.AccountState;
import com.fsck.k9.activity.setup.accounttype.AccountTypeContract.View;
import com.fsck.k9.helper.EmailHelper;
import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.setup.ServerNameSuggester;

import static com.fsck.k9.mail.ServerSettings.Type.SMTP;
import static com.fsck.k9.mail.ServerSettings.Type.WebDAV;


class AccountTypePresenter implements AccountTypeContract.Presenter {
    private AccountState state;
    private AccountTypeContract.View view;
    private Account account;
    private ServerNameSuggester serverNameSuggester;

    AccountTypePresenter(View view, AccountState state) {
        this.view = view;
        view.setPresenter(this);
        this.state = state;
        this.account = state.getAccount();
        serverNameSuggester = new ServerNameSuggester();
    }

    @Override
    public void setupStoreAndSmtpTransport(Type serverType, String schemePrefix) throws URISyntaxException {
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

        view.onSetupFinished();
    }

    @Override
    public void setupDav() throws URISyntaxException {
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

        view.onSetupFinished();
    }

}
