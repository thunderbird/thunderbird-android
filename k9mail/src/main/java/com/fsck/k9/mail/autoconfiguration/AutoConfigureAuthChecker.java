package com.fsck.k9.mail.autoconfiguration;


import com.fsck.k9.helper.EmailHelper;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.autoconfiguration.AutoConfigure.AuthInfo;
import com.fsck.k9.mail.autoconfiguration.AutoConfigure.ProviderInfo;


public class AutoConfigureAuthChecker {
    public AutoConfigureAuthChecker() {
    }

    public AuthInfo checkAuthInfo(ProviderInfo providerInfo, String email, String password) {
        AuthInfo authInfo = AuthInfo.createEmpty();

        /*
        ServerSettings transportServer = TransportUris.decodeTransportUri(viewModel.accountConfig.getTransportUri());
        if (AuthType.EXTERNAL == incomingSettings.authenticationType) {
            transportServer = transportServer.newClientCertificateAlias(incomingSettings.clientCertificateAlias);
        } else {
            transportServer = transportServer.newPassword(incomingSettings.password);
        }

        String transportUri = TransportUris.createTransportUri(transportServer);
        viewModel.accountConfig.setTransportUri(transportUri);
        */

        String localPart = EmailHelper.getLocalPartFromEmailAddress(email);

        authInfo = authInfo.withIncomingAuth(AuthType.PLAIN, localPart, password);
        authInfo = authInfo.withOutgoingAuth(AuthType.PLAIN, localPart, password);

        return authInfo;
    }

}
