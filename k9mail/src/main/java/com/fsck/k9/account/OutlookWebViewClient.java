package com.fsck.k9.account;

import android.net.Uri;

import com.fsck.k9.activity.setup.AccountSetupPresenter;


public class OutlookWebViewClient extends OAuth2WebViewClient {
    public OutlookWebViewClient(AccountSetupPresenter presenter) {
        super(presenter);
    }

    @Override
    protected boolean arrivedAtRedirectUri(Uri uri) {
        return ("msal" + "a41aa976-c5ad-405f-a8e3-ed18c07bb13a").equals(uri.getScheme());
    }

    @Override
    protected boolean getOutOfDomain(Uri uri) {
        return !uri.getHost().contains("live.com"); // TODO: 8/18/17 how to improve it?
    }
}
