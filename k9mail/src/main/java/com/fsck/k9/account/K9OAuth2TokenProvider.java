package com.fsck.k9.account;

import android.content.Context;
import android.content.SharedPreferences;

import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.oauth.SpecificOAuth2TokenProvider;

public class K9OAuth2TokenProvider extends OAuth2TokenProvider {
    private Context context;
    private static final String REFRESH_TOKEN_SP = "refresh_token";

    private enum TYPE {
        GMAIL
    }

    private GmailOAuth2TokenStore gmailOAuth2TokenStore;

    public K9OAuth2TokenProvider(Context context) {
        this.context = context;
        gmailOAuth2TokenStore = new GmailOAuth2TokenStore();
    }

    public void setPromptRequestHandler(XOauth2PromptRequestHandler promptRequestHandler) {
        gmailOAuth2TokenStore.setPromptRequestHandler(promptRequestHandler);
    }

    @Override
    protected void saveRefreshToken(String email, String refreshToken) {
        getSharedPreference().edit().putString(email, refreshToken).apply();
    }

    @Override
    public void showAuthDialog(String email) {
        SpecificOAuth2TokenProvider provider = getSpecificProviderFromEmail(email);
        if (provider == null) return;
        provider.showAuthDialog();
    }

    @Override
    protected String getRefreshToken(String email) {
        return getSharedPreference().getString(email, null);
    }

    @Override
    public void invalidateRefreshToken(String email) {
        getSharedPreference().edit().remove(email).apply();
    }

    @Override
    protected SpecificOAuth2TokenProvider getSpecificProviderFromEmail(String email) {
        TYPE type = getServerTypeFromEmail(email);
        if (type == null) return null;
        switch (type) {
            case GMAIL:
                return gmailOAuth2TokenStore;
        }
        return null;
    }

    private TYPE getServerTypeFromEmail(String email) {
        String domain = email.split("@")[1];
        switch (domain) {
            case "gmail.com":
                return TYPE.GMAIL;
        }
        return null;
    }

    private SharedPreferences getSharedPreference() {
        return context.getSharedPreferences(REFRESH_TOKEN_SP, Context.MODE_PRIVATE);
    }

}
