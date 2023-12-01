package com.fsck.k9.account;


import app.k9mail.core.common.mail.Protocols;
import com.fsck.k9.Account.DeletePolicy;
import com.fsck.k9.mail.ConnectionSecurity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class AccountCreatorHelperTest {
    private final AccountCreatorHelper accountCreatorHelper = new AccountCreatorHelper();

    @Test
    public void getDefaultDeletePolicy_withImap_shouldReturn_ON_DELETE() {
        DeletePolicy result = accountCreatorHelper.getDefaultDeletePolicy(Protocols.IMAP);

        assertEquals(DeletePolicy.ON_DELETE, result);
    }

    @Test
    public void getDefaultDeletePolicy_withPop3_shouldReturn_NEVER() {
        DeletePolicy result = accountCreatorHelper.getDefaultDeletePolicy(Protocols.POP3);

        assertEquals(DeletePolicy.NEVER, result);
    }

    @Test(expected = AssertionError.class)
    public void getDefaultDeletePolicy_withSmtp_shouldFail() {
        accountCreatorHelper.getDefaultDeletePolicy(Protocols.SMTP);
    }

    @Test
    public void getDefaultPort_withNoConnectionSecurityAndImap_shouldReturnDefaultPort() {
        int result = accountCreatorHelper.getDefaultPort(ConnectionSecurity.NONE, Protocols.IMAP);

        assertEquals(143, result);
    }

    @Test
    public void getDefaultPort_withStartTlsAndImap_shouldReturnDefaultPort() {
        int result = accountCreatorHelper.getDefaultPort(ConnectionSecurity.STARTTLS_REQUIRED, Protocols.IMAP);

        assertEquals(143, result);
    }

    @Test
    public void getDefaultPort_withTlsAndImap_shouldReturnDefaultTlsPort() {
        int result = accountCreatorHelper.getDefaultPort(ConnectionSecurity.SSL_TLS_REQUIRED, Protocols.IMAP);

        assertEquals(993, result);
    }
}
