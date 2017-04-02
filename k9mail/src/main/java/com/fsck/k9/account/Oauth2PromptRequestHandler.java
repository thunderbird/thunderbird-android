package com.fsck.k9.account;


import android.content.Intent;


public interface Oauth2PromptRequestHandler {
    void handleGmailXOAuth2Intent(Intent intent);
    void handleGmailRedirectUrl(String url);
    void handleOutlookRedirectUrl(String url);
    void onErrorWhenGettingOAuthCode(String errorMessage);
    void onOAuthCodeGot(String code);
}
