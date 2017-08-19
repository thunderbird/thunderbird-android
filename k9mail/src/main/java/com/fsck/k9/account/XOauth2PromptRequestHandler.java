package com.fsck.k9.account;

public interface XOauth2PromptRequestHandler {
    // void handleGMailXOAuth2Intent(Intent intent);
    void handleGmailRedirectUrl(String url);
}
