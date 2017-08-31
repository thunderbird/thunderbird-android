package com.fsck.k9.account;


import java.util.HashMap;
import java.util.Map;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.OAuth2NeedUserPromptException;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;


public class K9OAuth2TokenProvider extends OAuth2TokenProvider {

    private static final String GOOGLE_ACCOUNT_TYPE = "com.google";

    private AccountManager accountManager;

    private AndroidAccountOAuth2TokenStore gmailTokenProviderWithAccountSystem;
    private K9OAuth2AuthorizationCodeFlowTokenProvider authorizationCodeFlowTokenProvider;
    private Oauth2PromptRequestHandler promptRequestHandler;

    public K9OAuth2TokenProvider(Context context) {
        accountManager = AccountManager.get(context);
        gmailTokenProviderWithAccountSystem = new AndroidAccountOAuth2TokenStore(context);
        authorizationCodeFlowTokenProvider = new K9OAuth2AuthorizationCodeFlowTokenProvider(context);
    }

    public K9OAuth2AuthorizationCodeFlowTokenProvider getAuthorizationCodeFlowTokenProvider() {
        return authorizationCodeFlowTokenProvider;
    }

    private Account getAccountFromManager(String emailAddress) {
        android.accounts.Account[] accounts = accountManager.getAccountsByType(GOOGLE_ACCOUNT_TYPE);
        for (android.accounts.Account account : accounts) {
            if (account.name.equals(emailAddress)) {
                return account;
            }
        }
        return null;
    }


    @Override
    public String getToken(String email, long timeoutMillis)
            throws AuthenticationFailedException, OAuth2NeedUserPromptException {
        Account gmailAccount = getAccountFromManager(email);

        if (gmailAccount != null) {
            return gmailTokenProviderWithAccountSystem.getToken(email, gmailAccount, timeoutMillis);
        }

        return authorizationCodeFlowTokenProvider.getToken(email, timeoutMillis);
    }

    @Override
    public void invalidateToken(String email) {
        Account gmailAccount = getAccountFromManager(email);

        if (gmailAccount != null) {
            gmailTokenProviderWithAccountSystem.invalidateAccessToken(email);
        } else {
            authorizationCodeFlowTokenProvider.invalidateAccessToken(email);
        }
    }

    @Override
    public void disconnectEmailWithK9(String email) {
        Account gmailAccount = getAccountFromManager(email);

        if (gmailAccount != null) {
            gmailTokenProviderWithAccountSystem.invalidateAccessToken(email);
        } else {
            authorizationCodeFlowTokenProvider.invalidateRefreshToken(email);
        }
    }

    public void setPromptRequestHandler(Oauth2PromptRequestHandler promptRequestHandler) {
        this.promptRequestHandler = promptRequestHandler;
        gmailTokenProviderWithAccountSystem.setPromptRequestHandler(promptRequestHandler);
        authorizationCodeFlowTokenProvider.setPromptRequestHandler(promptRequestHandler);
    }
}
