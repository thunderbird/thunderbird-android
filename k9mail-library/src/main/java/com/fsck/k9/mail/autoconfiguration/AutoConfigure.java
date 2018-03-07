package com.fsck.k9.mail.autoconfiguration;


import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import timber.log.Timber;


/**
 * An interface for autoconfiguration
 */

public interface AutoConfigure {
    ProviderInfo findProviderInfo(ProviderInfo providerInfo, String localpart, String domain);

    class ProviderInfo {
        public final String incomingUsernameTemplate = "";
        public final String incomingType;
        public final ConnectionSecurity incomingSecurity;
        public final String incomingHost;
        public final Integer incomingPort;

        public final String outgoingUsernameTemplate = "";
        public final String outgoingType;
        public final ConnectionSecurity outgoingSecurity;
        public final String outgoingHost;
        public final Integer outgoingPort;

        public final boolean isFatalError;

        public static final String USERNAME_TEMPLATE_EMAIL = "$email";
        public static final String USERNAME_TEMPLATE_USER = "$user";
        public static final String USERNAME_TEMPLATE_DOMAIN = "$domain";
        public static final String USERNAME_TEMPLATE_SRV = "$srv";

        public static final String INCOMING_TYPE_IMAP = "imap";
        public static final String INCOMING_TYPE_POP3 = "pop3";
        public static final String OUTGOING_TYPE_SMTP = "smtp";

        public ProviderInfo(
                String incomingType, String incomingHost, Integer incomingPort, ConnectionSecurity incomingSecurity,
                String outgoingType, String outgoingHost, Integer outgoingPort, ConnectionSecurity outgoingSecurity,
                boolean isFatalError) {
            this.incomingType = incomingType;
            this.incomingSecurity = incomingSecurity;
            this.incomingHost = incomingHost;
            this.incomingPort = incomingPort;

            this.outgoingType = outgoingType;
            this.outgoingSecurity = outgoingSecurity;
            this.outgoingHost = outgoingHost;
            this.outgoingPort = outgoingPort;

            this.isFatalError = isFatalError;
        }

        public static ProviderInfo createEmpty() {
            return new ProviderInfo(null, null, null, null, null, null, null, null, false);
        }

        public static ProviderInfo createFatalError() {
            return new ProviderInfo(null, null, null, null, null, null, null, null, true);
        }

        public ProviderInfo withImapInfo(String host, int port, ConnectionSecurity security) {
            Timber.d("Found IMAP info, host %s:%d, %s", host, port, security.toString());
            return new ProviderInfo(INCOMING_TYPE_IMAP, host, port, security, null, null, null, null, false);
        }

        public ProviderInfo withPop3Info(String host, int port, ConnectionSecurity security) {
            Timber.d("Found POP3 info, host %s:%d, %s", host, port, security.toString());
            return new ProviderInfo(INCOMING_TYPE_POP3, host, port, security, null, null, null, null, false);
        }

        public ProviderInfo withSmtpInfo(String host, int port, ConnectionSecurity security) {
            // ProviderInfo.USERNAME_TEMPLATE_SRV;
            Timber.d("Found SMTP info, host %s:%d, %s", host, port, security.toString());
            return new ProviderInfo(incomingType, incomingHost, incomingPort, incomingSecurity, OUTGOING_TYPE_SMTP, host, port, security,
                    false);
        }

        public boolean hasOutgoing() {
            return outgoingPort != null;
        }

        public boolean hasIncoming() {
            return incomingPort != null;
        }

        private boolean hasFatalError() {
            return isFatalError;
        }

        public boolean isComplete() {
            return hasFatalError() || hasIncoming() && hasOutgoing();
        }
    }

    class AuthInfo {
        public final boolean incomingSuccessful;
        public final AuthType incomingAuthType;
        public final String incomingUsername;
        public final String incomingPassword;

        public final boolean outgoingSuccessful;
        public final AuthType outgoingAuthType;
        public final String outgoingUsername;
        public final String outgoingPassword;

        AuthInfo(boolean incomingSuccessful, AuthType authType, String username, String password,
                boolean outgoingSuccessful, AuthType outgoingAuthType,
                String outgoingUsername, String outgoingPassword) {
            this.incomingSuccessful = incomingSuccessful;
            this.incomingAuthType = authType;
            this.incomingUsername = username;
            this.incomingPassword = password;
            this.outgoingSuccessful = outgoingSuccessful;
            this.outgoingAuthType = outgoingAuthType;
            this.outgoingUsername = outgoingUsername;
            this.outgoingPassword = outgoingPassword;
        }

        public static AuthInfo createEmpty() {
            return new AuthInfo(false, null, null, null, false, null, null, null);
        }

        public AuthInfo withIncomingAuth(AuthType authType, String username, String password) {
            return new AuthInfo(true, authType, username, password, outgoingSuccessful, outgoingAuthType, outgoingUsername, outgoingPassword);
        }

        public AuthInfo withOutgoingAuth(AuthType authType, String username, String password) {
            return new AuthInfo(incomingSuccessful, incomingAuthType, incomingUsername, incomingPassword,
                    true, authType, username, password);
        }

        public AuthInfo withIncomingError() {
            return this;
        }
    }
}
