package com.fsck.k9.account;

public interface Oauth2PromptRequestHandler {
    // void handleGMailXOAuth2Intent(Intent intent);
    void handleGmailRedirectUrl(String url);
    void handleOutlookRedirectUrl(String url);
    void onErrorWhenGettingOAuthCode(String errorMessage);
    void onOAuthCodeGot(String code);
}
