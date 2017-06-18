package com.fsck.k9.mail.autoconfiguration;


/**
 * An interface for autoconfiguration
 */

public interface AutoConfigure {
    ProviderInfo findProviderInfo(String email);

    public static class ProviderInfo {
        public String incomingUsernameTemplate;

        public String outgoingUsernameTemplate;

        public String incomingType;
        public String incomingSocketType;
        public String incomingAddr;
        public int incomingPort = -1;
        public String outgoingType;
        public String outgoingSocketType;
        public String outgoingAddr;
        public int outgoingPort = -1;

        public static String USERNAME_TEMPLATE_EMAIL = "$email";
        public static String USERNAME_TEMPLATE_USER = "$user";
        public static String USERNAME_TEMPLATE_DOMAIN = "$domain";
        public static String USERNAME_TEMPLATE_SRV = "$srv";

        public static String INCOMING_TYPE_IMAP = "imap";
        public static String INCOMING_TYPE_POP3 = "pop3";
        public static String OUTGOING_TYPE_SMTP = "smtp";

        @Override
        public String toString() {
            return "ProviderInfo{" +
                    "incomingUsernameTemplate='" + incomingUsernameTemplate + '\'' +
                    ", outgoingUsernameTemplate='" + outgoingUsernameTemplate + '\'' +
                    ", incomingType='" + incomingType + '\'' +
                    ", incomingSocketType='" + incomingSocketType + '\'' +
                    ", incomingAddr='" + incomingAddr + '\'' +
                    ", incomingPort=" + incomingPort +
                    ", outgoingType='" + outgoingType + '\'' +
                    ", outgoingSocketType='" + outgoingSocketType + '\'' +
                    ", outgoingAddr='" + outgoingAddr + '\'' +
                    ", outgoingPort=" + outgoingPort +
                    '}';
        }
    }
}
