package com.fsck.k9.account;


import com.fsck.k9.Account.DeletePolicy;
import com.fsck.k9.RobolectricTest;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.preferences.Protocols;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class AccountCreatorTest extends RobolectricTest {

    @Test
    public void getDefaultDeletePolicy_withImap_shouldReturn_ON_DELETE() {
        DeletePolicy result = AccountCreator.getDefaultDeletePolicy(Protocols.IMAP);

        assertEquals(DeletePolicy.ON_DELETE, result);
    }

    @Test
    public void getDefaultDeletePolicy_withPop3_shouldReturn_NEVER() {
        DeletePolicy result = AccountCreator.getDefaultDeletePolicy(Protocols.POP3);

        assertEquals(DeletePolicy.NEVER, result);
    }

    @Test
    public void getDefaultDeletePolicy_withWebDav_shouldReturn_ON_DELETE() {
        DeletePolicy result = AccountCreator.getDefaultDeletePolicy(Protocols.WEBDAV);

        assertEquals(DeletePolicy.ON_DELETE, result);
    }

    @Test(expected = AssertionError.class)
    public void getDefaultDeletePolicy_withSmtp_shouldFail() {
        AccountCreator.getDefaultDeletePolicy(Protocols.SMTP);
    }

    @Test
    public void getDefaultPort_withNoConnectionSecurityAndImap_shouldReturnDefaultPort() {
        int result = AccountCreator.getDefaultPort(ConnectionSecurity.NONE, Protocols.IMAP);

        assertEquals(143, result);
    }

    @Test
    public void getDefaultPort_withStartTlsAndImap_shouldReturnDefaultPort() {
        int result = AccountCreator.getDefaultPort(ConnectionSecurity.STARTTLS_REQUIRED, Protocols.IMAP);

        assertEquals(143, result);
    }

    @Test
    public void getDefaultPort_withTlsAndImap_shouldReturnDefaultTlsPort() {
        int result = AccountCreator.getDefaultPort(ConnectionSecurity.SSL_TLS_REQUIRED, Protocols.IMAP);

        assertEquals(993, result);
    }
}
