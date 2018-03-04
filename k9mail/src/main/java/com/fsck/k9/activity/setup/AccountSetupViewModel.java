package com.fsck.k9.activity.setup;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;

import com.fsck.k9.mail.autoconfiguration.AutoConfigure.ProviderInfo;


class AccountSetupViewModel extends ViewModel {
    private AutoconfigureLiveData providerInfoLiveData;

    IncrementalSetupInfo setupInfo = IncrementalSetupInfo.createEmptySetupInfo();
    ManualSetupInfo accountConfig = new ManualSetupInfo();

    LiveData<ProviderInfo> getAutoconfigureLiveData(Context context, String email) {
        if (providerInfoLiveData == null || !providerInfoLiveData.isForEmail(email)) {
            providerInfoLiveData = new AutoconfigureLiveData(context, email);
        }
        return providerInfoLiveData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        providerInfoLiveData = null;
    }


    static class IncrementalSetupInfo {
        public final SetupState state;
        public final String email;
        public final String password;

        private IncrementalSetupInfo(SetupState state, String email, String password, ProviderInfo providerInfo) {
            this.state = state;
            this.email = email;
            this.password = password;
        }

        static IncrementalSetupInfo createEmptySetupInfo() {
            return new IncrementalSetupInfo(SetupState.EMPTY, null, null, null);
        }

        IncrementalSetupInfo withCredentials(String email, String password) {
            return new IncrementalSetupInfo(SetupState.CREDENTIALS, email, password, null);
        }

        IncrementalSetupInfo withProviderInfo(ProviderInfo providerInfo) {
            return new IncrementalSetupInfo(SetupState.DETECTED, email, password, providerInfo);
        }
    }

    enum SetupState {
        EMPTY, CREDENTIALS, DETECTED, VERIFIED, DONE
    }
}
