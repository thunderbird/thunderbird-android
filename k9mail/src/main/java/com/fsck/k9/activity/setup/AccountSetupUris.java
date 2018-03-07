package com.fsck.k9.activity.setup;


import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.TransportUris;
import com.fsck.k9.mail.autoconfiguration.AutoConfigure.AuthInfo;
import com.fsck.k9.mail.autoconfiguration.AutoConfigure.ProviderInfo;
import com.fsck.k9.mail.store.imap.ImapStore;
import com.fsck.k9.mail.store.pop3.Pop3Store;


public class AccountSetupUris {
    public static String getStoreUri(ProviderInfo providerInfo, AuthInfo authInfo) {
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
        throw new UnsupportedOperationException();
    }

    public static String getTransportUri(ProviderInfo providerInfo, AuthInfo authInfo) {
        switch (providerInfo.outgoingType) {
            case ProviderInfo.OUTGOING_TYPE_SMTP: {
                ServerSettings server = new ServerSettings(Type.SMTP, providerInfo.outgoingHost, providerInfo.outgoingPort, providerInfo.outgoingSecurity,
                        authInfo.outgoingAuthType, authInfo.outgoingUsername, authInfo.outgoingPassword, null);
                return TransportUris.createTransportUri(server);
            }
        }
        throw new UnsupportedOperationException();
    }
}
