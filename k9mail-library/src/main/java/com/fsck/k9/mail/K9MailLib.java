package com.fsck.k9.mail;


public class K9MailLib {
    private static DebugStatus debugStatus = new DefaultDebugStatus();
    private static ImapExtensionStatus imapExtensionStatus = new DefaultImapExtensionStatus();

    private K9MailLib() {
    }

    public static final int PUSH_WAKE_LOCK_TIMEOUT = 60000;
    public static final String IDENTITY_HEADER = "X-K9mail-Identity";

    /**
     * Should K-9 log the conversation it has over the wire with
     * SMTP servers?
     */
    public static boolean DEBUG_PROTOCOL_SMTP = true;

    /**
     * Should K-9 log the conversation it has over the wire with
     * IMAP servers?
     */
    public static boolean DEBUG_PROTOCOL_IMAP = true;

    /**
     * Should K-9 log the conversation it has over the wire with
     * POP3 servers?
     */
    public static boolean DEBUG_PROTOCOL_POP3 = true;

    /**
     * Should K-9 log the conversation it has over the wire with
     * WebDAV servers?
     */
    public static boolean DEBUG_PROTOCOL_WEBDAV = true;

    public static boolean isDebug() {
        return debugStatus.enabled();
    }

    public static boolean isDebugSensitive() {
        return debugStatus.debugSensitive();
    }

    public static boolean shouldUseCondstore() {
        return imapExtensionStatus.useCondstore();
    }

    public static boolean shouldUseQresync() {
        return imapExtensionStatus.useQresync();
    }

    public static void setDebugSensitive(boolean b) {
        if (debugStatus instanceof WritableDebugStatus) {
            ((WritableDebugStatus) debugStatus).setSensitive(b);
        }
    }

    public static void setDebug(boolean b) {
        if (debugStatus instanceof WritableDebugStatus) {
            ((WritableDebugStatus) debugStatus).setEnabled(b);
        }
    }

    public static void setUseCondstore(boolean useCondstore) {
        if (imapExtensionStatus instanceof WritableImapExtensionStatus) {
            ((WritableImapExtensionStatus) imapExtensionStatus).setUseCondstore(useCondstore);
        }
    }

    public static void setUseQresync(boolean useQresync) {
        if (imapExtensionStatus instanceof WritableImapExtensionStatus) {
            ((WritableImapExtensionStatus) imapExtensionStatus).setUseQresync(useQresync);
        }
    }

    public interface DebugStatus {
        boolean enabled();

        boolean debugSensitive();
    }

    public interface ImapExtensionStatus {
        boolean useCondstore();

        boolean useQresync();
    }

    public static void setDebugStatus(DebugStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        debugStatus = status;
    }

    public static void setImapExtensionStatus(ImapExtensionStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        imapExtensionStatus = status;
    }

    private interface WritableDebugStatus extends DebugStatus {
        void setEnabled(boolean enabled);

        void setSensitive(boolean sensitive);
    }

    private interface WritableImapExtensionStatus extends ImapExtensionStatus {
        void setUseCondstore(boolean useCondstore);

        void setUseQresync(boolean useQresync);
    }

    private static class DefaultDebugStatus implements WritableDebugStatus {
        private boolean enabled;
        private boolean sensitive;

        @Override
        public boolean enabled() {
            return enabled;
        }

        @Override
        public boolean debugSensitive() {
            return sensitive;
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public void setSensitive(boolean sensitive) {
            this.sensitive = sensitive;
        }
    }

    private static class DefaultImapExtensionStatus implements WritableImapExtensionStatus {
        private boolean useCondstore = true;
        private boolean useQresync = true;

        @Override
        public boolean useCondstore() {
            return useCondstore;
        }

        @Override
        public boolean useQresync() {
            return useQresync;
        }

        @Override
        public void setUseCondstore(boolean useCondstore) {
            this.useCondstore = useCondstore;
        }

        @Override
        public void setUseQresync(boolean useQresync) {
            this.useQresync = useQresync;
        }
    }
}
