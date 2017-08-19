package com.fsck.k9;


import android.content.Context;
import android.support.annotation.VisibleForTesting;

import com.fsck.k9.account.K9OAuth2TokenProvider;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;


public class Globals {
    private static Context context;
    private static K9OAuth2TokenProvider oAuth2TokenProvider;

    static void setContext(Context context) {
        Globals.context = context;
    }

    static void setOAuth2TokenProvider(K9OAuth2TokenProvider oAuth2TokenProvider) {
        Globals.oAuth2TokenProvider = oAuth2TokenProvider;
    }

    public static Context getContext() {
        if (context == null) {
            throw new IllegalStateException("No context provided");
        }

        return context;
    }

    public static K9OAuth2TokenProvider getOAuth2TokenProvider() {
        if (oAuth2TokenProvider == null) {
            throw new IllegalStateException("No OAuth 2.0 Token Provider provided");
        }

        return oAuth2TokenProvider;
    }
}
