package com.fsck.k9.activity.setup;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;

import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.TransportUris;
import com.fsck.k9.mail.autoconfiguration.AutoConfigure.AuthInfo;
import com.fsck.k9.mail.autoconfiguration.AutoConfigure.ProviderInfo;
import com.fsck.k9.mail.store.imap.ImapStore;
import com.fsck.k9.mail.store.pop3.Pop3Store;


public class AccountSetupViewModel extends ViewModel {
    private ProviderInfoLiveData providerInfoLiveData;
    private AuthInfoLiveData authInfoLiveData;

    IncrementalSetupInfo setupInfo = IncrementalSetupInfo.createEmptySetupInfo();
    ManualSetupInfo accountConfig = new ManualSetupInfo();

    LiveData<ProviderInfo> getProviderInfoLiveData(Context context, String email) {
        if (providerInfoLiveData == null || !providerInfoLiveData.isForEmail(email)) {
            providerInfoLiveData = new ProviderInfoLiveData(context, email);
        }
        return providerInfoLiveData;
    }

    LiveData<AuthInfo> getAuthInfoLiveData(Context context, ProviderInfo providerInfo, String email, String password) {
        if (authInfoLiveData == null || !authInfoLiveData.isForProviderInfo(providerInfo, email, password)) {
            authInfoLiveData = new AuthInfoLiveData(context, providerInfo, email, password);
        }
        return authInfoLiveData;
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

        public final ProviderInfo providerInfo;
        public final AuthInfo authInfo;

        public final String accountName;
        public final String accountDescription;

        private IncrementalSetupInfo(SetupState state, String email, String password, ProviderInfo providerInfo,
                AuthInfo authInfo, String accountName, String accountDescription) {
            this.state = state;
            this.email = email;
            this.password = password;
            this.providerInfo = providerInfo;
            this.authInfo = authInfo;
            this.accountName = accountName;
            this.accountDescription = accountDescription;
        }

        static IncrementalSetupInfo createEmptySetupInfo() {
            return new IncrementalSetupInfo(SetupState.EMPTY, null, null, null,
                    null, null, null);
        }

        IncrementalSetupInfo withCredentials(String email, String password) {
            return new IncrementalSetupInfo(SetupState.CREDENTIALS, email, password, null, authInfo,
                    accountName, null);
        }

        IncrementalSetupInfo withProviderInfo(ProviderInfo providerInfo) {
            return new IncrementalSetupInfo(SetupState.DETECTED, email, password, providerInfo, authInfo,
                    accountName,
                    null);
        }

        IncrementalSetupInfo withAccountInfo(String accountName, String description) {
            return new IncrementalSetupInfo(SetupState.DONE, email, password, providerInfo, authInfo,
                    accountName, description);
        }

        IncrementalSetupInfo withAuthInfo(AuthInfo authInfo) {
            return new IncrementalSetupInfo(
                    SetupState.DONE, email, password, providerInfo, authInfo, accountName, accountDescription);
        }

        public String getStoreUri() {
            switch (providerInfo.incomingType) {
                case ProviderInfo.INCOMING_TYPE_IMAP: {
                    ServerSettings server = new ServerSettings(Type.IMAP, providerInfo.incomingHost, providerInfo.incomingPort, providerInfo.incomingSecurity,
                            authInfo.incomingAuthType, authInfo.incomingUsername, authInfo.incomingPassword, null);
                    return ImapStore.createStoreUri(server);
                }
                case ProviderInfo.INCOMING_TYPE_POP3: {
                    ServerSettings server = new ServerSettings(Type.POP3, providerInfo.incomingHost, providerInfo.incomingPort, providerInfo.incomingSecurity,
                            authInfo.incomingAuthType, authInfo.incomingUsername, authInfo.incomingPassword, null);
                    return Pop3Store.createStoreUri(server);
                }
            }
            return null;
        }

        public String getTransportUri() {
            switch (providerInfo.incomingType) {
                case ProviderInfo.OUTGOING_TYPE_SMTP: {
                    ServerSettings server = new ServerSettings(Type.SMTP, providerInfo.outgoingHost, providerInfo.outgoingPort, providerInfo.outgoingSecurity,
                            authInfo.outgoingAuthType, authInfo.outgoingUsername, authInfo.outgoingPassword, null);
                    return TransportUris.createTransportUri(server);
                }
            }
            return null;
        }
    }

    enum SetupState {
        EMPTY, CREDENTIALS, DETECTED, VERIFIED, DONE
    }
}
