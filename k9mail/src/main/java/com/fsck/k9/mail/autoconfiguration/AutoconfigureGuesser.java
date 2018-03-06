package com.fsck.k9.mail.autoconfiguration;


import com.fsck.k9.mail.ConnectionSecurity;


public class AutoconfigureGuesser implements AutoConfigure {
    private static final int IMAP_SSL_OR_TLS_DEFAULT_PORT = 993;
    private static final int IMAP_STARTTLS_DEFAULT_PORT = 143;
    private static final int POP3_SSL_OR_TLS_DEFAULT_PORT = 995;
    private static final int POP3_STARTTLS_DEFAULT_PORT = 110;
    private static final int SMTP_SSL_OR_TLS_DEFAULT_PORT = 465;
    private static final int SMTP_STARTTLS_DEFAULT_PORT = 587;

    private ConnectionTester connectionTester = new ConnectionTester();

    @Override
    public ProviderInfo findProviderInfo(ProviderInfo providerInfo, String localpart, String domain) {
        if (!providerInfo.hasIncoming()) {
            providerInfo = checkForOpenImapPort(domain, providerInfo);
        }

        if (!providerInfo.hasIncoming()) {
            providerInfo = checkForOpenImapPort("imap." + domain, providerInfo);
        }

        if (!providerInfo.hasIncoming()) {
            providerInfo = checkForOpenImapPort("mail." + domain, providerInfo);
        }

        if (!providerInfo.hasIncoming()) {
            providerInfo = checkForOpenPop3Port(domain, providerInfo);
        }

        if (!providerInfo.hasIncoming()) {
            providerInfo = checkForOpenPop3Port("pop." + domain, providerInfo);
        }

        if (!providerInfo.hasIncoming()) {
            providerInfo = checkForOpenPop3Port("mail." + domain, providerInfo);
        }

        if (!providerInfo.hasOutgoing()) {
            providerInfo = checkForOpenSubmissionPort(domain, providerInfo);
        }

        if (!providerInfo.hasOutgoing()) {
            providerInfo = checkForOpenSubmissionPort("smtp." + domain, providerInfo);
        }

        if (!providerInfo.hasOutgoing()) {
            providerInfo = checkForOpenSubmissionPort("mail." + domain, providerInfo);
        }

        return providerInfo;
    }

    private ProviderInfo checkForOpenImapPort(String domain, ProviderInfo providerInfo) {
        if (connectionTester.isPortOpen(domain, IMAP_STARTTLS_DEFAULT_PORT)) {
            return providerInfo.withImapInfo(domain, IMAP_STARTTLS_DEFAULT_PORT, ConnectionSecurity.STARTTLS_REQUIRED);
        }

        if (connectionTester.isPortOpen(domain, IMAP_SSL_OR_TLS_DEFAULT_PORT)) {
            return providerInfo.withImapInfo(domain, IMAP_SSL_OR_TLS_DEFAULT_PORT, ConnectionSecurity.SSL_TLS_REQUIRED);
        }

        return providerInfo;
    }

    private ProviderInfo checkForOpenPop3Port(String domain, ProviderInfo providerInfo) {
        if (connectionTester.isPortOpen(domain, POP3_STARTTLS_DEFAULT_PORT)) {
            return providerInfo.withPop3Info(domain, POP3_STARTTLS_DEFAULT_PORT, ConnectionSecurity.STARTTLS_REQUIRED);
        }

        if (connectionTester.isPortOpen(domain, POP3_SSL_OR_TLS_DEFAULT_PORT)) {
            return providerInfo.withPop3Info(domain, POP3_SSL_OR_TLS_DEFAULT_PORT, ConnectionSecurity.SSL_TLS_REQUIRED);
        }

        return providerInfo;
    }

    private ProviderInfo checkForOpenSubmissionPort(String domain, ProviderInfo providerInfo) {
        if (connectionTester.isPortOpen(domain, SMTP_STARTTLS_DEFAULT_PORT)) {
            return providerInfo.withSmtpInfo(domain, SMTP_STARTTLS_DEFAULT_PORT, ConnectionSecurity.STARTTLS_REQUIRED);
        }

        if (connectionTester.isPortOpen(domain, SMTP_SSL_OR_TLS_DEFAULT_PORT)) {
            return providerInfo.withSmtpInfo(domain, SMTP_SSL_OR_TLS_DEFAULT_PORT, ConnectionSecurity.SSL_TLS_REQUIRED);
        }

        return providerInfo;
    }

}
