package com.fsck.k9.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.fsck.k9.R;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.oauth.AuthorizationException;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * An interface between the OAuth2 requirements used for authentication and the AccountManager.
 */
public class AndroidAccountOAuth2TokenStore implements OAuth2TokenProvider {
    private static final String GMAIL_AUTH_TOKEN_TYPE = "oauth2:https://mail.google.com/";
    private static final String GOOGLE_ACCOUNT_TYPE = "com.google";

    private Map<String,String> authTokens = new HashMap<>();
    private AccountManager accountManager;

    public AndroidAccountOAuth2TokenStore(Context applicationContext) {
        this.accountManager = AccountManager.get(applicationContext);
    }

    @Override
    public void authorizeAPI(final String emailAddress, final Activity activity,
                             final OAuth2TokenProviderAuthCallback callback) {
        Account account = getAccountFromManager(emailAddress);
        if (account == null) {
            callback.failure(new AuthorizationException(activity.getString(R.string.xoauth2_account_doesnt_exist)));
            return;
        }
        if (account.name.equals(emailAddress)) {
            accountManager.getAuthToken(account, GMAIL_AUTH_TOKEN_TYPE, null, activity,
                new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> future) {
                        try {
                            Bundle bundle = future.getResult();
                            Object keyAccountName = bundle.get(AccountManager.KEY_ACCOUNT_NAME);
                            if (keyAccountName == null) {
                                callback.failure(new AuthorizationException(activity.getString(
                                        R.string.xoauth2_no_account)));
                                return;
                            }
                            if (keyAccountName.equals(emailAddress)) {
                                callback.success();
                            } else {
                                callback.failure(new AuthorizationException(activity.getString(
                                        R.string.xoauth2_incorrect_auth_info_provided)));
                            }
                        } catch (OperationCanceledException e) {
                            callback.failure(new AuthorizationException(activity.getString(
                                    R.string.xoauth2_auth_cancelled_by_user), e));
                        } catch (IOException e) {
                            callback.failure(new AuthorizationException(activity.getString(
                                    R.string.xoauth2_unable_to_contact_auth_server), e));
                        } catch (AuthenticatorException e) {
                            callback.failure(new AuthorizationException(activity.getString(
                                    R.string.xoauth2_error_contacting_auth_server), e));
                        }
                    }
                }, null);
        }
    }

    @Override
    public String getToken(String username, long timeoutMillis) throws AuthenticationFailedException {
        if(authTokens.get(username) == null) {
            Account account = getAccountFromManager(username);
            if (account == null) {
                throw new AuthenticationFailedException("Account not available");
            }
            fetchNewAuthToken(username, account, timeoutMillis);
        }
        return authTokens.get(username);
    }

    private Account getAccountFromManager(String username) {
        android.accounts.Account[] accounts = accountManager.getAccountsByType(GOOGLE_ACCOUNT_TYPE);
        for (android.accounts.Account account : accounts) {
            if (account.name.equals(username)) {
                return account;
            }
        }
        return null;
    }

    private void fetchNewAuthToken(String username, Account account, long timeoutMillis)
            throws AuthenticationFailedException {
        try {
            AccountManagerFuture<Bundle> future = accountManager
                    .getAuthToken(account, GMAIL_AUTH_TOKEN_TYPE, false, null, null);
            Bundle bundle = future.getResult(timeoutMillis, TimeUnit.MILLISECONDS);
            if (bundle == null)
                throw new AuthenticationFailedException("No token provided");
            if (bundle.get(AccountManager.KEY_ACCOUNT_NAME) == null)
                throw new AuthenticationFailedException("No account information provided");
            if (bundle.get(AccountManager.KEY_ACCOUNT_NAME).equals(username))
                authTokens.put(username, bundle.get(AccountManager.KEY_AUTHTOKEN).toString());
            else
                throw new AuthenticationFailedException("Unexpected account information provided");
        } catch (Exception e) {
            throw new AuthenticationFailedException(e.getMessage());
        }
    }

    @Override
    public void invalidateToken(String username) {
        accountManager.invalidateAuthToken("com.google", authTokens.get(username));
        authTokens.remove(username);
    }

    @Override
    public List<String> getAccounts() {
        Account[] accounts = accountManager.getAccountsByType(GOOGLE_ACCOUNT_TYPE);
        ArrayList<String> accountNames = new ArrayList<>();
        for (Account account: accounts) {
            accountNames.add(account.name);
        }
        return accountNames;
    }
}
