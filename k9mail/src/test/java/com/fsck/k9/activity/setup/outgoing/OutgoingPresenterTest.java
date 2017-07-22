package com.fsck.k9.activity.setup.outgoing;


import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.Preferences;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(K9RobolectricTestRunner.class)
public class OutgoingPresenterTest {
    private static final String DEFAULT_PORT_FOR_SSL_TLS = "465";
    private static final String DEFAULT_PORT_FOR_STARTTLS = "587";

    OutgoingPresenter presenter;
    AccountSetupOutgoing view;
    Account account;

    @Before
    public void setUp() {
        String incomingUri = "imap+ssl+://PLAIN:daquexian566:denx+ajs@mail.gmail.com/1%7C";
        String outgoingUri = "smtp+ssl+://daquexian566:denx+ajs:PLAIN@mail.gmail.com";

        view = mock(AccountSetupOutgoing.class);

        account = mock(Account.class);
        when(account.getStoreUri()).thenReturn(incomingUri);
        when(account.getTransportUri()).thenReturn(outgoingUri);

        presenter = new OutgoingPresenter(view, account);

        verify(view, atLeastOnce()).setSecurityType(ConnectionSecurity.SSL_TLS_REQUIRED);
        verify(view, atLeastOnce()).setUsername("daquexian566");
        verify(view, atLeastOnce()).setPassword("denx ajs");
        verify(view, atLeastOnce()).setServer("mail.gmail.com");
        verify(view, atLeastOnce()).setAuthType(AuthType.PLAIN);
        verify(view, atLeastOnce()).setPort(DEFAULT_PORT_FOR_SSL_TLS);
    }

    @Test
    public void testAccount() {
        Assert.assertEquals(account, presenter.getAccount());
    }

    @Test
    public void testFields_withPassword() {
        presenter.onInputChanged(null, "mail.abc.com", "123", "test", "password", AuthType.PLAIN,
                ConnectionSecurity.SSL_TLS_REQUIRED, true);

        verify(view).setNextButtonEnabled(true);
    }

    @Test
    public void testFields_withCertificate() {
        presenter.onInputChanged("certificate", "mail.abc.com", "123", "test", null, AuthType.EXTERNAL,
                ConnectionSecurity.SSL_TLS_REQUIRED, true);
        verify(view).setNextButtonEnabled(true);
    }

    @Test
    public void testFields_withNotRequiredLogin() {
        presenter.onInputChanged(null, "mail.abc.com", "123", null, null, AuthType.PLAIN,
                ConnectionSecurity.SSL_TLS_REQUIRED, false);
        verify(view).setNextButtonEnabled(true);
    }

    @Test
    public void testFields_withInvalidPassword() {
        presenter.onInputChanged(null, "mail.abc.com", "123", "test", "", AuthType.PLAIN,
                ConnectionSecurity.SSL_TLS_REQUIRED, true);
        verify(view).setNextButtonEnabled(false);
    }

    @Test
    public void testFields_withIncompleteServer() {
        presenter.onInputChanged(null, "mail.abc.", "123", "test", "password", AuthType.PLAIN,
                ConnectionSecurity.SSL_TLS_REQUIRED, true);
        verify(view).setNextButtonEnabled(false);
    }

    @Test
    public void testFields_withInvalidPort() {
        presenter.onInputChanged(null, "mail.abc.com", "", "test", "password", AuthType.PLAIN,
                ConnectionSecurity.SSL_TLS_REQUIRED, true);
        verify(view).setNextButtonEnabled(false);
    }

    @Test
    public void testRevokeInvalidSettings_withCertificateChanged() {
        presenter.onInputChanged(null, "mail.abc.com", "123", "username", "password",
                AuthType.EXTERNAL, ConnectionSecurity.NONE, true);

        verify(view).setAuthType(AuthType.PLAIN);
    }

    @Test
    public void testRevokeInvalidSettings_withSecurityChanged() {
        verify(view).setSecurityType(any(ConnectionSecurity.class));
        presenter.onInputChanged("certificate", "mail.abc.com", "123", "username", "password",
                AuthType.EXTERNAL, ConnectionSecurity.STARTTLS_REQUIRED, true);

        presenter.onInputChanged("certificate", "mail.abc.com", "123", "username", "password",
                AuthType.EXTERNAL, ConnectionSecurity.NONE, true);

        verify(view).setSecurityType(ConnectionSecurity.STARTTLS_REQUIRED);
    }

    @Test
    public void testUpdatePortFromSecurity() {
        presenter.onInputChanged("certificate", "mail.abc.com", "123", "username", "password",
                AuthType.EXTERNAL, ConnectionSecurity.STARTTLS_REQUIRED, true);

        // any better way?
        verify(view, atLeastOnce()).setPort(DEFAULT_PORT_FOR_STARTTLS);
    }

    @Test
    public void testUpdateViewFromAuthType() {
        presenter.onInputChanged("certificate", "mail.abc.com", "123", "username", "password",
                AuthType.EXTERNAL, ConnectionSecurity.STARTTLS_REQUIRED, true);
        verify(view).onAuthTypeIsExternal();

        presenter.onInputChanged("certificate", "mail.abc.com", "123", "username", "password",
                AuthType.PLAIN, ConnectionSecurity.STARTTLS_REQUIRED, true);
        verify(view).onAuthTypeIsNotExternal();
    }
}
