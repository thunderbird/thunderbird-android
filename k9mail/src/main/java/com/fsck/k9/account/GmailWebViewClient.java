package com.fsck.k9.account;

import android.net.Uri;

import com.fsck.k9.activity.setup.AccountSetupPresenter;

public class GmailWebViewClient extends OAuth2WebViewClient {
    public GmailWebViewClient(AccountSetupPresenter presenter) {
        super(presenter);
    }

    @Override
    protected boolean arrivedAtRedirectUri(Uri uri) {
        return "com.fsck.k9.debug".equals(uri.getScheme());
    }

    @Override
    protected boolean getOutOfDomain(Uri uri) {
        return !uri.getHost().contains("google"); // TODO: 8/18/17 how to improve it?
    }

}
