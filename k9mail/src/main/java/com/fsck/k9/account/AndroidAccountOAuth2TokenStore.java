package com.fsck.k9.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.OAuth2NeedUserPromptException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * An interface between the OAuth2 requirements used for authentication and the AccountManager.
 * It's not used for the time being because we have {@link GmailOAuth2TokenStore} that handle all Gmail account
 */
// TODO: 2017/8/19 maybe we can use it when google account is in Android account system (don't know whether it need effort)
public class AndroidAccountOAuth2TokenStore {
    private static final String GMAIL_AUTH_TOKEN_TYPE = "oauth2:https://mail.google.com/";
    private static final String GOOGLE_ACCOUNT_TYPE = "com.google";

    private Map<String,String> authTokens = new HashMap<>();
    private AccountManager accountManager;
    private Oauth2PromptRequestHandler promptRequestHandler;

    public AndroidAccountOAuth2TokenStore(Context applicationContext) {
        this.accountManager = AccountManager.get(applicationContext);
    }

    public void setPromptRequestHandler(Oauth2PromptRequestHandler promptRequestHandler) {
        this.promptRequestHandler = promptRequestHandler;
    }

    public String getToken(String username, Account account, long timeoutMillis) throws
            AuthenticationFailedException, OAuth2NeedUserPromptException {
        if(authTokens.get(username) == null) {
            if (account == null) {
                throw new AuthenticationFailedException("Account not available");
            }
            fetchNewAuthToken(username, account, timeoutMillis);
        }
        return authTokens.get(username);
    }

    private void fetchNewAuthToken(String username, Account account, long timeoutMillis)
            throws AuthenticationFailedException, OAuth2NeedUserPromptException {
        try {
            AccountManagerFuture<Bundle> future = accountManager
                    .getAuthToken(account, GMAIL_AUTH_TOKEN_TYPE, null, false, null, null);
            Bundle bundle = future.getResult(timeoutMillis, TimeUnit.MILLISECONDS);
            if (bundle == null)
                throw new AuthenticationFailedException("No token provided");
            if (bundle.get(AccountManager.KEY_INTENT) != null) {
                promptRequestHandler.handleGmailXOAuth2Intent((Intent) bundle.get(AccountManager.KEY_INTENT));
                throw new OAuth2NeedUserPromptException();
            } else {
                if (bundle.get(AccountManager.KEY_ACCOUNT_NAME) == null)
                    throw new AuthenticationFailedException("No account information provided");
                if (bundle.get(AccountManager.KEY_ACCOUNT_NAME).equals(username))
                    authTokens.put(username, bundle.get(AccountManager.KEY_AUTHTOKEN).toString());
                else
                    throw new AuthenticationFailedException("Unexpected account information provided");
            }
        } catch (OAuth2NeedUserPromptException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthenticationFailedException(e.getMessage());
        }
    }

    public void invalidateAccessToken(String username) {
        accountManager.invalidateAuthToken("com.google", authTokens.get(username));
        authTokens.remove(username);
    }

    public List<String> getAccounts() {
        Account[] accounts = accountManager.getAccountsByType(GOOGLE_ACCOUNT_TYPE);
        ArrayList<String> accountNames = new ArrayList<>();
        for (Account account: accounts) {
            accountNames.add(account.name);
        }
        return accountNames;
    }
}
