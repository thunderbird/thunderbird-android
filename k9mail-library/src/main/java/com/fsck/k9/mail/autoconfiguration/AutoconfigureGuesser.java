package com.fsck.k9.mail.autoconfiguration;


import java.net.InetAddress;
import java.net.UnknownHostException;

import android.content.Context;

import com.fsck.k9.mail.ConnectionSecurity;
import timber.log.Timber;


public class AutoconfigureGuesser implements AutoConfigure {
    private final Context context;
    private ConnectionTester connectionTester = new ConnectionTester();

    public AutoconfigureGuesser(Context context) {
        this.context = context;
    }

    public static int IMAP_SSL_OR_TLS_DEFAULT_PORT = 993;
    public static int IMAP_STARTTLS_DEFAULT_PORT = 143;

    enum SmtpGuess {
        TLS (587, ConnectionSecurity.SSL_TLS_REQUIRED),
        STARTTLS (465, ConnectionSecurity.SSL_TLS_REQUIRED);

        final int port;
        final ConnectionSecurity connectionSecurity;

        SmtpGuess(int port, ConnectionSecurity connectionSecurity) {
            this.port = port;
            this.connectionSecurity = connectionSecurity;
        }
    }

    private SmtpGuess guessSmtpPortForHost(String host) {
        InetAddress[] inetAddresses;
        try {
            inetAddresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            Timber.d(e, "Probe failed, unresolved host %s", host);
            return null;
        }

        if (connectionTester.probeIsPortOpen(inetAddresses, SmtpGuess.TLS.port)) {
            return SmtpGuess.TLS;
        }

        if (connectionTester.probeIsPortOpen(inetAddresses, SmtpGuess.STARTTLS.port)) {
            return SmtpGuess.STARTTLS;
        }

        return null;
    }

    private void testOutgoing(String domain, ConnectionSecurity connectionSecurity, boolean useLocalPart) {
        /*
        try {
            Timber.d("Server %s is right for smtp and %s", domain, connectionSecurity.toString());
        } catch (AuthenticationFailedException afe) {
            if (!useLocalPart) {
                Timber.d("Server %s is connected, but authentication failed. Use local part as username this time", domain);
                testOutgoing(domain, connectionSecurity, true);
            } else {
                Timber.d("Server %s is connected, but authentication failed for both email address and local-part", domain);
            }
        } catch (URISyntaxException | MessagingException ignored) {
            Timber.d("Unknown error occurred when using OAuth 2.0");
        }
        */
    }

    /*
    private void testDomain(String domain) {
        String guessedDomainForMailPrefix;
        //noinspection ConstantConditions
        if (domain.startsWith("mail.")) {
            guessedDomainForMailPrefix = domain;
        } else {
            guessedDomainForMailPrefix = "mail." + domain;
        }

        Timber.d("Test %s for imap", guessedDomainForMailPrefix);
        testIncoming(guessedDomainForMailPrefix, false);

        Timber.d("Test %s for smtp and starttls", guessedDomainForMailPrefix);
        testOutgoing(guessedDomainForMailPrefix, ConnectionSecurity.STARTTLS_REQUIRED, false);

        Timber.d("Test %s for smtp and ssl/tls", guessedDomainForMailPrefix);
        testOutgoing(guessedDomainForMailPrefix, ConnectionSecurity.SSL_TLS_REQUIRED, false);

        String domainWithImapPrefix = "imap." + domain;
        Timber.d("Test %s for imap", domainWithImapPrefix);
        testIncoming(domainWithImapPrefix, false);

        String domainWithSmtpPrefix = "smtp." + domain;
        Timber.d("Test %s for smtp and starttls", domainWithSmtpPrefix);
        testOutgoing(domainWithSmtpPrefix, ConnectionSecurity.STARTTLS_REQUIRED, false);

        Timber.d("Test %s for smtp and ssl/tls", domainWithSmtpPrefix);
        testOutgoing(domainWithSmtpPrefix, ConnectionSecurity.SSL_TLS_REQUIRED, false);
    }

    private void testIncoming(String domain, boolean useLocalPart) {
        if (!incomingReady) {
            try {
                accountConfig.setStoreUri(getDefaultStoreURI(
                        useLocalPart ? EmailHelper.getLocalPartFromEmailAddress(email) : email,
                        password, domain).toString());
                accountConfig.getRemoteStore().checkSettings();
                incomingReady = true;
                Timber.d("Server %s is right for imap", domain);
            } catch (AuthenticationFailedException afe) {
                if (!useLocalPart) {
                    Timber.d("Server %s is connected, but authentication failed. Use local part as username this time", domain);
                    testIncoming(domain, true);
                } else {
                    Timber.d("Server %s is connected, but authentication failed for both email address and local-part", domain);
                }
            } catch (URISyntaxException | MessagingException ignored) {
                Timber.d("Unknown error occurred when using OAuth 2.0");
            }
        }
    }

    private void testOutgoing(String domain, ConnectionSecurity connectionSecurity, boolean useLocalPart) {
        if (!outgoingReady) {
            try {
                accountConfig.setTransportUri(getDefaultTransportURI(
                        useLocalPart ? EmailHelper.getLocalPartFromEmailAddress(email) : email,
                        password, domain, connectionSecurity).toString());
                Transport transport = TransportProvider.getInstance().getTransport(context, accountConfig,
                        Globals.getOAuth2TokenProvider());
                transport.close();
                try {
                    transport.open();
                } finally {
                    transport.close();
                }
                outgoingReady = true;
                Timber.d("Server %s is right for smtp and %s", domain, connectionSecurity.toString());
            } catch (AuthenticationFailedException afe) {
                if (!useLocalPart) {
                    Timber.d("Server %s is connected, but authentication failed. Use local part as username this time", domain);
                    testOutgoing(domain, connectionSecurity, true);
                } else {
                    Timber.d("Server %s is connected, but authentication failed for both email address and local-part", domain);
                }
            } catch (URISyntaxException | MessagingException ignored) {
                Timber.d("Unknown error occurred when using OAuth 2.0");
            }
        }
    }
    */

    String[] DOMAIN_CANDIDATES = new String[] {
            "smtp.%s",
            "mail.%s",
            "%s"
    };

    @Override
    public ProviderInfo findProviderInfo(String email) {
        return null;
    }
}
