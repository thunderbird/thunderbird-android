package com.fsck.k9.account;


public interface OAuth2ErrorHandler {
    void onError(String error);
}
