package com.fsck.k9.account;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.fsck.k9.activity.setup.AccountSetupPresenter;

import timber.log.Timber;

/**
 * bass class for standard authorization code flow like Google's or Microsoft's
 */
abstract class OAuth2WebViewClient extends WebViewClient {
    private Oauth2PromptRequestHandler requestHandler;
    private OAuth2ErrorHandler errorHandler;

    public OAuth2WebViewClient(Oauth2PromptRequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    protected abstract boolean arrivedAtRedirectUri(Uri uri);
    protected abstract boolean getOutOfDomain(Uri uri);

    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Uri uri = Uri.parse(url);

        if (arrivedAtRedirectUri(uri)) {
            final String error = uri.getQueryParameter("error");
            if (error != null) {
                Timber.i("got oauth error: " + error);
                errorHandler.onError(error);
                requestHandler.onErrorWhenGettingOAuthCode(error);
                return true;
            }

            String oAuthCode = uri.getQueryParameter("code");
            requestHandler.onOAuthCodeGot(oAuthCode);
            return true;
        }

        // if (!uri.getHost().contains("google")) {
        if (getOutOfDomain(uri)) {
            requestHandler.onErrorWhenGettingOAuthCode("Don't surf away"); // TODO: 2017/8/19 better error message
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
