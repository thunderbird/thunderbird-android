package com.fsck.k9.account;


import com.fsck.k9.Account;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AccountCreatorTest {

    @Test
    public void defaultIMAPdeletePolicy_is_ON_DELETE() {
        assertEquals(Account.DeletePolicy.ON_DELETE,
                AccountCreator.getDefaultDeletePolicy(ServerSettings.Type.IMAP));
    }

    @Test
    public void defaultPOP3deletePolicy_is_NEVER() {
        assertEquals(Account.DeletePolicy.NEVER,
                AccountCreator.getDefaultDeletePolicy(ServerSettings.Type.POP3));
    }

    @Test
    public void defaultWebDAVdeletePolicy_is_ON_DELETE() {
        assertEquals(Account.DeletePolicy.ON_DELETE,
                AccountCreator.getDefaultDeletePolicy(ServerSettings.Type.WebDAV));
    }

    @Test
    public void defaultPort_is_the_insecure_port_for_noSecurity() {
        assertEquals(ServerSettings.Type.IMAP.defaultPort,
                AccountCreator.getDefaultPort(ConnectionSecurity.NONE,
                        ServerSettings.Type.IMAP));
    }

    @Test
    public void defaultPort_is_the_insecure_port_for_starttls() {
        assertEquals(ServerSettings.Type.IMAP.defaultPort,
                AccountCreator.getDefaultPort(ConnectionSecurity.STARTTLS_REQUIRED,
                        ServerSettings.Type.IMAP));
    }

    @Test
    public void defaultPort_is_the_secure_port_for_ssltls() {
        assertEquals(ServerSettings.Type.IMAP.defaultTlsPort,
                AccountCreator.getDefaultPort(ConnectionSecurity.SSL_TLS_REQUIRED,
                        ServerSettings.Type.IMAP));
    }
}
