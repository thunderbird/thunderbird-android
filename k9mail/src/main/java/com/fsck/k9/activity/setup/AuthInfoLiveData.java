package com.fsck.k9.activity.setup;


import android.content.Context;
import android.net.ConnectivityManager;
import android.support.annotation.NonNull;

import com.fsck.k9.AsyncTaskLiveData;
import com.fsck.k9.mail.autoconfiguration.AutoConfigure.AuthInfo;
import com.fsck.k9.mail.autoconfiguration.AutoConfigure.ProviderInfo;
import com.fsck.k9.mail.autoconfiguration.AutoConfigureAuthChecker;
import com.fsck.k9.mail.helpers.VeryTrustingTrustManager;
import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;
import com.fsck.k9.mail.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;


class AuthInfoLiveData extends AsyncTaskLiveData<AuthInfo> {
    private final AutoConfigureAuthChecker autoConfigureAuthChecker;

    private final ProviderInfo providerInfo;
    private final String email;
    private final String password;

    AuthInfoLiveData(@NonNull Context context, ProviderInfo providerInfo, String email, String password) {
        super(context, null);

        this.providerInfo = providerInfo;
        this.email = email;
        this.password = password;

        autoConfigureAuthChecker = new AutoConfigureAuthChecker(
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE),
                new DefaultTrustedSocketFactory(context));
        // , new TrustManagerFactory() {
        // @Override
        // public X509TrustManager get(String host, int port) {
            // return new VeryTrustingTrustManager(TrustManagerFactory.defaultTrustManager.getAcceptedIssuers());
        // }
    }

    @Override
    protected AuthInfo asyncLoadData() {
        return autoConfigureAuthChecker.checkAuthInfo(providerInfo, email, password);
    }

    boolean isForProviderInfo(ProviderInfo providerInfo, String email, String password) {
        return providerInfo.equals(this.providerInfo) && email.equals(this.email) && password.equals(this.password);
    }
}
