package com.fsck.k9;


import android.content.Context;

import com.fsck.k9.account.K9OAuth2TokenProvider;


public class GlobalsHelper {
    public static void setContext(Context context) {
        Globals.setContext(context);
    }

    public static void setOAuth2TokenProvider(K9OAuth2TokenProvider provider) {
        Globals.setOAuth2TokenProvider(provider);
    }
}
