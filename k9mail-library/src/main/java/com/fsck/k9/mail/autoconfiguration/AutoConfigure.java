package com.fsck.k9.mail.autoconfiguration;


/**
 * An interface for autoconfiguration
 */

public interface AutoConfigure {
    ProviderInfo findProviderInfo(String email);

    public static class ProviderInfo {
        public String incomingUsernameTemplate = "";

        public String outgoingUsernameTemplate = "";

        public String incomingType = "";
        public String incomingSocketType = "";
        public String incomingAddr = "";
        public int incomingPort = -1;
        public String outgoingType = "";
        public String outgoingSocketType = "";
        public String outgoingAddr = "";
        public int outgoingPort = -1;

        public static String USERNAME_TEMPLATE_EMAIL = "$email";
        public static String USERNAME_TEMPLATE_USER = "$user";
        public static String USERNAME_TEMPLATE_DOMAIN = "$domain";
        public static String USERNAME_TEMPLATE_SRV = "$srv";

        public static String INCOMING_TYPE_IMAP = "imap";
        public static String INCOMING_TYPE_POP3 = "pop3";
        public static String OUTGOING_TYPE_SMTP = "smtp";

        public static String SOCKET_TYPE_SSL_OR_TLS = "ssl";
        public static String SOCKET_TYPE_STARTTLS = "tls";

        public static int IMAP_SSL_OR_TLS_DEFAULT_PORT = 993;
        public static int IMAP_STARTTLS_DEFAULT_PORT = 143;
        public static int POP3_SSL_OR_TLS_DEFAULT_PORT = 995;
        public static int POP3_STARTTLS_DEFAULT_PORT = 110;
        public static int SMTP_SSL_OR_TLS_DEFAULT_PORT = 465;
        public static int SMTP_STARTTLS_DEFAULT_PORT = 587;

        public ProviderInfo fillDefaultPorts() {
            if (incomingPort == -1) {
                if (incomingType.equals(INCOMING_TYPE_IMAP)) {
                    if (incomingSocketType.equals(SOCKET_TYPE_SSL_OR_TLS)) {
                        incomingPort = IMAP_SSL_OR_TLS_DEFAULT_PORT;
                    } else if (incomingSocketType.equals(SOCKET_TYPE_STARTTLS)) {
                        incomingPort = IMAP_STARTTLS_DEFAULT_PORT;
                    }
                } else if (incomingType.equals(INCOMING_TYPE_POP3)) {
                    if (incomingSocketType.equals(SOCKET_TYPE_SSL_OR_TLS)) {
                        incomingPort = POP3_SSL_OR_TLS_DEFAULT_PORT;
                    } else if (incomingSocketType.equals(SOCKET_TYPE_STARTTLS)) {
                        incomingPort = POP3_STARTTLS_DEFAULT_PORT;
                    }
                }
            }

            if (outgoingPort == -1) {
                if (outgoingType.equals(OUTGOING_TYPE_SMTP)) {
                    if (outgoingSocketType.equals(SOCKET_TYPE_SSL_OR_TLS)) {
                        outgoingPort = SMTP_SSL_OR_TLS_DEFAULT_PORT;
                    } else if (outgoingSocketType.equals(SOCKET_TYPE_STARTTLS)) {
                        outgoingPort = SMTP_STARTTLS_DEFAULT_PORT;
                    }
                }
            }

            return this;
        }

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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ProviderInfo that = (ProviderInfo) o;

            if (incomingPort != that.incomingPort) {
                return false;
            }
            if (outgoingPort != that.outgoingPort) {
                return false;
            }
            if (!incomingUsernameTemplate.equals(that.incomingUsernameTemplate)) {
                return false;
            }
            if (!outgoingUsernameTemplate.equals(that.outgoingUsernameTemplate)) {
                return false;
            }
            if (!incomingType.equals(that.incomingType)) {
                return false;
            }
            if (!incomingSocketType.equals(that.incomingSocketType)) {
                return false;
            }
            if (!incomingAddr.equals(that.incomingAddr)) {
                return false;
            }
            if (!outgoingType.equals(that.outgoingType)) {
                return false;
            }
            if (!outgoingSocketType.equals(that.outgoingSocketType)) {
                return false;
            }
            return outgoingAddr.equals(that.outgoingAddr);

        }

        @Override
        public int hashCode() {
            int result = incomingUsernameTemplate.hashCode();
            result = 31 * result + outgoingUsernameTemplate.hashCode();
            result = 31 * result + incomingType.hashCode();
            result = 31 * result + incomingSocketType.hashCode();
            result = 31 * result + incomingAddr.hashCode();
            result = 31 * result + incomingPort;
            result = 31 * result + outgoingType.hashCode();
            result = 31 * result + outgoingSocketType.hashCode();
            result = 31 * result + outgoingAddr.hashCode();
            result = 31 * result + outgoingPort;
            return result;
        }
    }
}
