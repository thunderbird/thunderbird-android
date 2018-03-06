package com.fsck.k9.activity.setup;


import android.content.Context;
import android.support.annotation.NonNull;

import com.fsck.k9.AsyncTaskLiveData;
import com.fsck.k9.mail.autoconfiguration.AutoConfigure.AuthInfo;
import com.fsck.k9.mail.autoconfiguration.AutoConfigure.ProviderInfo;
import com.fsck.k9.mail.autoconfiguration.AutoConfigureAuthChecker;


class AuthInfoLiveData extends AsyncTaskLiveData<AuthInfo> {
    private final AutoConfigureAuthChecker autoConfigureAuthChecker = new AutoConfigureAuthChecker();

    private final ProviderInfo providerInfo;
    private final String email;
    private final String password;

    AuthInfoLiveData(@NonNull Context context, ProviderInfo providerInfo, String email, String password) {
        super(context, null);

        this.providerInfo = providerInfo;
        this.email = email;
        this.password = password;
    }

    @Override
    protected AuthInfo asyncLoadData() {
        return autoConfigureAuthChecker.checkAuthInfo(providerInfo, email, password);
    }

    boolean isForProviderInfo(ProviderInfo providerInfo, String email, String password) {
        return providerInfo.equals(this.providerInfo) && email.equals(this.email) && password.equals(this.password);
    }
}
