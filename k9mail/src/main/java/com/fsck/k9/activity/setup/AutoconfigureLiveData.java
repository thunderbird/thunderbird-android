package com.fsck.k9.activity.setup;


import android.content.Context;
import android.support.annotation.NonNull;

import com.fsck.k9.AsyncTaskLiveData;
import com.fsck.k9.mail.autoconfiguration.AutoConfigure.ProviderInfo;
import com.fsck.k9.mail.autoconfiguration.AutoConfigureAggregator;


class AutoconfigureLiveData extends AsyncTaskLiveData<ProviderInfo> {
    private final String email;
    private final AutoConfigureAggregator autoConfigureAggregator;

    AutoconfigureLiveData(@NonNull Context context, String email) {
        super(context, null);

        this.email = email;
        this.autoConfigureAggregator = new AutoConfigureAggregator();
    }

    @Override
    protected ProviderInfo asyncLoadData() {
        return autoConfigureAggregator.findProviderInfo(email);
    }

    boolean isForEmail(String email) {
        return email.equalsIgnoreCase(this.email);
    }
}
