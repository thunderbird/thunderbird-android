package com.fsck.k9.mail.autoconfiguration;


import java.net.InetAddress;
import java.net.UnknownHostException;

import android.content.Context;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.TransportUris;
import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;
import com.fsck.k9.mail.transport.smtp.SmtpTransport;
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

    String[] DOMAIN_CANDIDATES = new String[] {
            "smtp.%s",
            "mail.%s",
            "%s"
    };


    public ProviderInfo findProviderInfo(String email, String password) {
        return null;
    }

    public ProviderInfo findProviderInfo(Address address, String password) {
        String domain = address.getHostname();

        SmtpGuess guess = null;
        String smtpHost = null;
        for (String formatCandidate : DOMAIN_CANDIDATES) {
            smtpHost = String.format(formatCandidate, domain);
            guess = guessSmtpPortForHost(smtpHost);
            if (guess != null) {
                break;
            }
        }

        try {
            attemptSmtpLogin(guess, smtpHost, address.toString(), password);
        } catch (AuthenticationFailedException e) {
            return new
        }
        attemptSmtpLogin(guess, smtpHost, address.getPersonal(), password);

        testOutgoing(guessedDomainForMailPrefix, ConnectionSecurity.STARTTLS_REQUIRED, false);

        testOutgoing(guessedDomainForMailPrefix, ConnectionSecurity.SSL_TLS_REQUIRED, false);

        String domainWithImapPrefix = "imap." + domain;
        guessSmtpSetting(domainWithImapPrefix, false);

        String domainWithSmtpPrefix = "smtp." + domain;
        testOutgoing(domainWithSmtpPrefix, ConnectionSecurity.STARTTLS_REQUIRED, false);

        testOutgoing(domainWithSmtpPrefix, ConnectionSecurity.SSL_TLS_REQUIRED, false);

        ProviderInfo providerInfo = new ProviderInfo();
        return providerInfo;
    }

    private boolean attemptSmtpLogin(SmtpGuess guess, String host, String username, String password)
            throws AuthenticationFailedException {
        try {
            ServerSettings serverSettings = new ServerSettings(Type.SMTP, host, guess.port,
                    guess.connectionSecurity, AuthType.PLAIN, username, password, null);

            SmtpTransport smtpTransport = new SmtpTransport(
                    TransportUris.createTransportUri(serverSettings),
                    new DefaultTrustedSocketFactory(context), null);
            smtpTransport.open();
            return true;
        } catch (MessagingException e) {
            if (e instanceof AuthenticationFailedException) {
                throw (AuthenticationFailedException) e;
            }
            return false;
        }

    }
}
