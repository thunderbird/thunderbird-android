package com.fsck.k9.account;


import android.content.res.Resources;

import com.fsck.k9.Account.DeletePolicy;
import com.fsck.k9.Preferences;
import com.fsck.k9.RobolectricTest;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.preferences.Protocols;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;


public class AccountCreatorTest extends RobolectricTest {
    private AccountCreator accountCreator;

    @Before
    public void setUp() {
        Preferences preferences = mock(Preferences.class);
        Resources resources = mock(Resources.class);
        accountCreator = new AccountCreator(preferences, resources);
    }

    @Test
    public void getDefaultDeletePolicy_withImap_shouldReturn_ON_DELETE() {
        DeletePolicy result = accountCreator.getDefaultDeletePolicy(Protocols.IMAP);

        assertEquals(DeletePolicy.ON_DELETE, result);
    }

    @Test
    public void getDefaultDeletePolicy_withPop3_shouldReturn_NEVER() {
        DeletePolicy result = accountCreator.getDefaultDeletePolicy(Protocols.POP3);

        assertEquals(DeletePolicy.NEVER, result);
    }

    @Test
    public void getDefaultDeletePolicy_withWebDav_shouldReturn_ON_DELETE() {
        DeletePolicy result = accountCreator.getDefaultDeletePolicy(Protocols.WEBDAV);

        assertEquals(DeletePolicy.ON_DELETE, result);
    }

    @Test(expected = AssertionError.class)
    public void getDefaultDeletePolicy_withSmtp_shouldFail() {
        accountCreator.getDefaultDeletePolicy(Protocols.SMTP);
    }

    @Test
    public void getDefaultPort_withNoConnectionSecurityAndImap_shouldReturnDefaultPort() {
        int result = accountCreator.getDefaultPort(ConnectionSecurity.NONE, Protocols.IMAP);

        assertEquals(143, result);
    }

    @Test
    public void getDefaultPort_withStartTlsAndImap_shouldReturnDefaultPort() {
        int result = accountCreator.getDefaultPort(ConnectionSecurity.STARTTLS_REQUIRED, Protocols.IMAP);

        assertEquals(143, result);
    }

    @Test
    public void getDefaultPort_withTlsAndImap_shouldReturnDefaultTlsPort() {
        int result = accountCreator.getDefaultPort(ConnectionSecurity.SSL_TLS_REQUIRED, Protocols.IMAP);

        assertEquals(993, result);
    }
}
