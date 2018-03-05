package com.fsck.k9.mail.autoconfiguration;


import com.fsck.k9.mail.ConnectionSecurity;
import timber.log.Timber;


/**
 * An interface for autoconfiguration
 */

public interface AutoConfigure {
    ProviderInfo findProviderInfo(ProviderInfo providerInfo, String email);

    class ProviderInfo {
        public final String incomingUsernameTemplate = "";
        public final String incomingType;
        public final ConnectionSecurity incomingSecurity;
        public final String incomingAddr;
        public final Integer incomingPort;

        public final String outgoingUsernameTemplate = "";
        public final String outgoingType;
        public final ConnectionSecurity outgoingSecurity;
        public final String outgoingAddr;
        public final Integer outgoingPort;

        public final boolean isFatalError;

        public static String USERNAME_TEMPLATE_EMAIL = "$email";
        public static String USERNAME_TEMPLATE_USER = "$user";
        public static String USERNAME_TEMPLATE_DOMAIN = "$domain";
        public static String USERNAME_TEMPLATE_SRV = "$srv";

        public static String INCOMING_TYPE_IMAP = "imap";
        public static String INCOMING_TYPE_POP3 = "pop3";
        public static String OUTGOING_TYPE_SMTP = "smtp";

        public ProviderInfo(
                String incomingType, String incomingAddr, Integer incomingPort, ConnectionSecurity incomingSecurity,
                String outgoingType, String outgoingAddr, Integer outgoingPort, ConnectionSecurity outgoingSecurity,
                boolean isFatalError) {
            this.incomingType = incomingType;
            this.incomingSecurity = incomingSecurity;
            this.incomingAddr = incomingAddr;
            this.incomingPort = incomingPort;

            this.outgoingType = outgoingType;
            this.outgoingSecurity = outgoingSecurity;
            this.outgoingAddr = outgoingAddr;
            this.outgoingPort = outgoingPort;

            this.isFatalError = isFatalError;
        }

        public String getStoreUri() {
            return null;
        }

        public String getTransportUri() {
            return null;
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
            return new ProviderInfo(incomingType, incomingAddr, incomingPort, incomingSecurity, OUTGOING_TYPE_SMTP, host, port, security,
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
}
