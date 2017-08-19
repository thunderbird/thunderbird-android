package com.fsck.k9.account;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.fsck.k9.activity.setup.AccountSetupPresenter;

import timber.log.Timber;

public abstract class OAuth2WebViewClient extends WebViewClient {
    AccountSetupPresenter presenter;

    public OAuth2WebViewClient(AccountSetupPresenter presenter) {
        this.presenter = presenter;
    }

    protected abstract boolean arrivedAtRedirectUri(Uri uri);
    protected abstract boolean getOutOfDomain(Uri uri);

    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Uri uri = Uri.parse(url);

        if (arrivedAtRedirectUri(uri)) {
            if (uri.getQueryParameter("error") != null) {
                Timber.i("got oauth error: " + uri.getQueryParameter("error"));
                presenter.onErrorWhenGettingOAuthCode(uri.getQueryParameter("error"));
                return true;
            }

            String oAuthCode = uri.getQueryParameter("code");
            presenter.onOAuthCodeGot(oAuthCode);
            return true;
        }

        // if (!uri.getHost().contains("google")) {
        if (getOutOfDomain(uri)) {
            presenter.onErrorWhenGettingOAuthCode("Don't surf away"); // TODO: 2017/8/19 better error message
            return true;
        }
        return false;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.N)
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return shouldOverrideUrlLoading(view, request.getUrl().toString());
    }
}
