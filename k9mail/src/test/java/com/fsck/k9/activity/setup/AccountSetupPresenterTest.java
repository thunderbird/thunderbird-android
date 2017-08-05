package com.fsck.k9.activity.setup;


import java.net.URI;
import java.net.URISyntaxException;

import android.content.Context;
import android.test.mock.MockContext;

import com.fsck.k9.Account;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.setup.AccountSetupPresenter.Stage;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings.Type;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(K9RobolectricTestRunner.class)
public class AccountSetupPresenterTest {
    private static final String DEFAULT_PORT_FOR_SMTP_SSL_TLS = "465";
    private static final String DEFAULT_PORT_FOR_SMTP_STARTTLS = "587";
    private static final String DEFAULT_PORT_FOR_IMAP_SSL_TLS = "993";
    private static final String DEFAULT_PORT_FOR_IMAP_STARTTLS = "143";
    private static final String DEFAULT_PORT_FOR_POP3_SSL_TLS = "995";
    private static final String DEFAULT_PORT_FOR_POP3_STARTTLS = "110";

    private AccountSetupPresenter presenter;
    private AccountSetupActivity view;
    private Account account;
    @SuppressWarnings("FieldCanBeLocal")
    private Context context;

    @Before
    public void setUp() {
        view = mock(AccountSetupActivity.class);
        context = mock(MockContext.class);

        Preferences preferences = mock(Preferences.class);

        presenter = new AccountSetupPresenter(context, preferences, view);

        account = mock(Account.class);
        doNothing().when(account).save(Mockito.any(Preferences.class));
        presenter.setAccount(account);
    }

    // region basics

    @Test
    public void testOnNextButtonInBasicViewClicked() {
        presenter.onNextButtonInBasicViewClicked("abc@test.com", "testpw");

        verify(view).goToAutoConfiguration();
    }

    @Test
    public void testOnInputChangeInBasics_withInvalidEmail() {
        presenter.onInputChangedInBasics("bea@dfa", "test");

        verify(view).setNextButtonInBasicsEnabled(false);
    }

    @Test
    public void testOnInputChangeInBasics_withValidEmailAndPassword() {
        presenter.onInputChangedInBasics("bea@dfa.co", "test");

        verify(view).setNextButtonInBasicsEnabled(true);
    }

    // region checking

    @Test
    public void testOnNegativeClickedInConfirmationDialog_incoming() {
        presenter.onCheckingStart(Stage.INCOMING_CHECKING);

        presenter.onNegativeClickedInConfirmationDialog();

        verify(view).goToOutgoing();
    }

    @Test
    public void testOnNegativeClickedInConfirmationDialog_outgoing() {
        presenter.onCheckingStart(Stage.OUTGOING_CHECKING);

        presenter.onNegativeClickedInConfirmationDialog();

        verify(view).goToAccountNames();
    }

    @Test
    public void testOnNegativeClickedInConfirmationDialog_editSettings() {
        mockAccountUrisImap();
        presenter.onIncomingStart(true);
        presenter.onCheckingStart(Stage.INCOMING_CHECKING);

        presenter.onNegativeClickedInConfirmationDialog();

        verify(view).end();
    }

    @Test
    public void testOnCertificatedRefused_incoming() {
        mockAccountUrisImap();
        presenter.onIncomingStart();
        presenter.onCheckingStart(Stage.INCOMING_CHECKING);

        presenter.onCertificateRefused();

        verify(view).goToIncoming();
    }

    @Test
    public void testOnCertificatedRefused_outgoing() {
        mockAccountUrisImap();
        presenter.onOutgoingStart();
        presenter.onCheckingStart(Stage.OUTGOING_CHECKING);

        presenter.onCertificateRefused();

        verify(view).goToOutgoing();
    }

    // endregion checking

    @Test
    public void testGetStatus() {
        mockAccountUrisImap();
        presenter.onIncomingStart();

        presenter.onInputChangedInIncoming(null, "test.com", "", "testusername", "testpw",
                AuthType.EXTERNAL, ConnectionSecurity.STARTTLS_REQUIRED);

        assertEquals(presenter.getStatus().getIncomingAuthType(), AuthType.EXTERNAL);
        assertEquals(presenter.getStatus().getIncomingSecurityType(), ConnectionSecurity.STARTTLS_REQUIRED);
    }

    // endregion basics

    // region account type
    @Test
    public void testOnImapOrPop3Selected() throws URISyntaxException {
        mockAccountUrisPop3();
        presenter.onAccountTypeStart();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                URI storeUri = new URI((String) args[0]);
                assertEquals("imap+ssl+", storeUri.getScheme());
                return null;
            }
        }).when(account).setStoreUri(Matchers.anyString());

        presenter.onNextButtonInAccountTypeClicked(Type.IMAP);

        verify(view).goToIncomingSettings();
    }

    @Test
    public void testOnWebdavSelected() throws URISyntaxException {
        mockAccountUrisImap();
        presenter.onAccountTypeStart();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                URI storeUri = new URI((String) args[0]);
                assertEquals("webdav+ssl+", storeUri.getScheme());
                assertEquals("daquexian566:denx+ajs", storeUri.getUserInfo());
                return null;
            }
        }).when(account).setStoreUri(Matchers.anyString());

        presenter.onNextButtonInAccountTypeClicked(Type.WebDAV);

        verify(view).goToIncomingSettings();
    }

    // endregion account type

    // region incoming

    @Test
    public void testOnInputChangedInIncoming_withValidInput() {
        mockAccountUrisImap();
        presenter.onIncomingStart();

        presenter.onInputChangedInIncoming(null, "test.com", "123", "testusername", "testpw", AuthType.PLAIN,
                ConnectionSecurity.SSL_TLS_REQUIRED);

        verify(view).setNextButtonInIncomingEnabled(true);
    }

    @Test
    public void testOnInputChangedInIncoming_withIllegalInput() {
        mockAccountUrisImap();
        presenter.onIncomingStart();

        presenter.onInputChangedInIncoming(null, "test.com", "", "testusername", "testpw",
                AuthType.PLAIN, ConnectionSecurity.NONE);
        presenter.onInputChangedInIncoming(null, "test.com", "", "testusername", "testpw",
                AuthType.EXTERNAL, ConnectionSecurity.NONE);

        assertEquals(AuthType.PLAIN, presenter.getStatus().getIncomingAuthType());
    }

    @Test
    public void testOnInputChangedInIncoming_withPassword() {
        mockAccountUrisImap();
        presenter.onIncomingStart();

        presenter.onInputChangedInIncoming(null, "mail.abc.com", "123", "test", "password", AuthType.PLAIN,
                ConnectionSecurity.SSL_TLS_REQUIRED);

        verify(view).setNextButtonInIncomingEnabled(true);
    }

    @Test
    public void testOnInputChangedInIncoming_withCertificate() {
        mockAccountUrisImap();
        presenter.onIncomingStart();
        presenter.onInputChangedInIncoming("certificate", "mail.abc.com", "123", "test", null, AuthType.EXTERNAL,
                ConnectionSecurity.SSL_TLS_REQUIRED);
        verify(view).setNextButtonInIncomingEnabled(true);
    }

    @Test
    public void testOnInputChangedInIncoming_withInvalidPassword() {
        mockAccountUrisImap();
        presenter.onIncomingStart();
        presenter.onInputChangedInIncoming(null, "mail.abc.com", "123", "test", "", AuthType.PLAIN,
                ConnectionSecurity.SSL_TLS_REQUIRED);
        verify(view).setNextButtonInIncomingEnabled(false);
    }

    @Test
    public void testOnInputChangedInIncoming_withIncompleteServer() {
        mockAccountUrisImap();
        presenter.onIncomingStart();
        presenter.onInputChangedInIncoming(null, "mail.abc.", "123", "test", "password", AuthType.PLAIN,
                ConnectionSecurity.SSL_TLS_REQUIRED);
        verify(view).setNextButtonInIncomingEnabled(false);
    }

    @Test
    public void testOnInputChangedInIncoming_withInvalidPort() {
        mockAccountUrisImap();
        presenter.onIncomingStart();
        presenter.onInputChangedInIncoming(null, "mail.abc.com", "", "test", "password", AuthType.PLAIN,
                ConnectionSecurity.SSL_TLS_REQUIRED);
        verify(view).setNextButtonInIncomingEnabled(false);
    }

    @Test
    public void testOnInputChangeInIncoming_imap_updatePortFromSecurity() {
        mockAccountUrisImap();
        presenter.onIncomingStart();
        presenter.onInputChangedInIncoming("certificate", "mail.abc.com", "123", "username", "password",
                AuthType.EXTERNAL, ConnectionSecurity.SSL_TLS_REQUIRED);

        presenter.onInputChangedInIncoming("certificate", "mail.abc.com", "123", "username", "password",
                AuthType.EXTERNAL, ConnectionSecurity.STARTTLS_REQUIRED);

        assertEquals(DEFAULT_PORT_FOR_IMAP_STARTTLS, presenter.getStatus().getIncomingPort());

        presenter.onInputChangedInIncoming("certificate", "mail.abc.com", "123", "username", "password",
                AuthType.EXTERNAL, ConnectionSecurity.SSL_TLS_REQUIRED);

        assertEquals(DEFAULT_PORT_FOR_IMAP_SSL_TLS, presenter.getStatus().getIncomingPort());
    }

    @Test
    public void testOnInputChangeInIncoming_pop3_updatePortFromSecurity() {
        mockAccountUrisPop3();
        presenter.onIncomingStart();
        presenter.onInputChangedInIncoming("certificate", "mail.abc.com", "123", "username", "password",
                AuthType.EXTERNAL, ConnectionSecurity.SSL_TLS_REQUIRED);

        presenter.onInputChangedInIncoming("certificate", "mail.abc.com", "123", "username", "password",
                AuthType.EXTERNAL, ConnectionSecurity.STARTTLS_REQUIRED);

        assertEquals(presenter.getStatus().getIncomingPort(), DEFAULT_PORT_FOR_POP3_STARTTLS);

        presenter.onInputChangedInIncoming("certificate", "mail.abc.com", "123", "username", "password",
                AuthType.EXTERNAL, ConnectionSecurity.SSL_TLS_REQUIRED);

        assertEquals(DEFAULT_PORT_FOR_POP3_SSL_TLS, presenter.getStatus().getIncomingPort());
    }

    // endregion incoming

    // region outgoing

    @Test
    public void testOnInputChangedInOutgoing_withPassword() {
        mockAccountUrisImap();
        presenter.onOutgoingStart();

        presenter.onInputChangedInOutgoing(null, "mail.abc.com", "123", "test", "password", AuthType.PLAIN,
                ConnectionSecurity.SSL_TLS_REQUIRED, true);

        verify(view).setNextButtonInOutgoingEnabled(true);
    }

    @Test
    public void testOnInputChangedInOutgoing_withCertificate() {
        mockAccountUrisImap();
        presenter.onOutgoingStart();
        presenter.onInputChangedInOutgoing("certificate", "mail.abc.com", "123", "test", null, AuthType.EXTERNAL,
                ConnectionSecurity.SSL_TLS_REQUIRED, true);
        verify(view).setNextButtonInOutgoingEnabled(true);
    }

    @Test
    public void testOnInputChangedInOutgoing_withNotRequiredLogin() {
        mockAccountUrisImap();
        presenter.onOutgoingStart();
        presenter.onInputChangedInOutgoing(null, "mail.abc.com", "123", null, null, AuthType.PLAIN,
                ConnectionSecurity.SSL_TLS_REQUIRED, false);
        verify(view).setNextButtonInOutgoingEnabled(true);
    }

    @Test
    public void testOnInputChangedInOutgoing_withInvalidPassword() {
        mockAccountUrisImap();
        presenter.onOutgoingStart();
        presenter.onInputChangedInOutgoing(null, "mail.abc.com", "123", "test", "", AuthType.PLAIN,
                ConnectionSecurity.SSL_TLS_REQUIRED, true);
        verify(view).setNextButtonInOutgoingEnabled(false);
    }

    @Test
    public void testOnInputChangedInOutgoing_withIncompleteServer() {
        mockAccountUrisImap();
        presenter.onOutgoingStart();
        presenter.onInputChangedInOutgoing(null, "mail.abc.", "123", "test", "password", AuthType.PLAIN,
                ConnectionSecurity.SSL_TLS_REQUIRED, true);
        verify(view).setNextButtonInOutgoingEnabled(false);
    }

    @Test
    public void testOnInputChangedInOutgoing_withInvalidPort() {
        mockAccountUrisImap();
        presenter.onOutgoingStart();
        presenter.onInputChangedInOutgoing(null, "mail.abc.com", "", "test", "password", AuthType.PLAIN,
                ConnectionSecurity.SSL_TLS_REQUIRED, true);
        verify(view).setNextButtonInOutgoingEnabled(false);
    }

    @Test
    public void testOnInputChangedInOutgoing_withCertificateChanged() {
        mockAccountUrisImap();
        presenter.onOutgoingStart();
        presenter.onInputChangedInOutgoing(null, "mail.abc.com", "123", "username", "password",
                AuthType.EXTERNAL, ConnectionSecurity.NONE, true);

        assertEquals(AuthType.PLAIN, presenter.getStatus().getOutgoingAuthType());
    }

    @Test
    public void testOnInputChangedInOutgoing_withSecurityChanged() {
        mockAccountUrisImap();
        presenter.onOutgoingStart();
        presenter.onInputChangedInOutgoing("certificate", "mail.abc.com", "123", "username", "password",
                AuthType.EXTERNAL, ConnectionSecurity.STARTTLS_REQUIRED, true);

        presenter.onInputChangedInOutgoing("certificate", "mail.abc.com", "123", "username", "password",
                AuthType.EXTERNAL, ConnectionSecurity.NONE, true);

        assertEquals(ConnectionSecurity.STARTTLS_REQUIRED, presenter.getStatus().getOutgoingSecurityType());
    }

    @Test
    public void testOnInputChangeInOutgoing_updatePortFromSecurity() {
        mockAccountUrisImap();
        presenter.onOutgoingStart();
        presenter.onInputChangedInOutgoing("certificate", "mail.abc.com", "123", "username", "password",
                AuthType.EXTERNAL, ConnectionSecurity.SSL_TLS_REQUIRED, true);

        presenter.onInputChangedInOutgoing("certificate", "mail.abc.com", "123", "username", "password",
                AuthType.EXTERNAL, ConnectionSecurity.STARTTLS_REQUIRED, true);

        assertEquals(DEFAULT_PORT_FOR_SMTP_STARTTLS, presenter.getStatus().getOutgoingPort());

        presenter.onInputChangedInOutgoing("certificate", "mail.abc.com", "123", "username", "password",
                AuthType.EXTERNAL, ConnectionSecurity.SSL_TLS_REQUIRED, true);

        assertEquals(DEFAULT_PORT_FOR_SMTP_SSL_TLS, presenter.getStatus().getOutgoingPort());
    }

    @Test
    public void testUpdateViewFromAuthType_outgoing() {
        mockAccountUrisImap();
        presenter.onOutgoingStart();

        verify(view).setViewNotExternalInOutgoing();

        presenter.onInputChangedInOutgoing("certificate", "mail.abc.com", "123", "username", "password",
                AuthType.EXTERNAL, ConnectionSecurity.STARTTLS_REQUIRED, true);

        verify(view).setViewExternalInOutgoing();
    }

    // endregion outgoing

    // region options

    // TODO: 8/2/17 it's hard to test onNextButtonInOptionsClicked. Several static methods involved context are called
    /* @Test
    public void testOnNextButtonInOptionsClicked() {
        mockAccountUrisImap();
        presenter.onOptionsStart();

        presenter.onNextButtonInOptionsClicked(true, true, 20, 50, true);

        assertEquals(true, presenter.getStatus().isNotifyNewMail());
        assertEquals(true, presenter.getStatus().isShowOngoing());
        assertEquals(20, presenter.getStatus().getAutomaticCheckIntervalMinutes());
        assertEquals(50, presenter.getStatus().getDisplayCount());
        assertEquals(FolderMode.FIRST_CLASS, presenter.getStatus().getFolderPushMode());
        verify(view).goToAccountNames();
    }*/
    // endregion options

    // region names
    @Test
    public void testOnNextButtonInNamesClicked() {
        mockAccountUrisImap();
        presenter.onNamesStart();

        presenter.onNextButtonInNamesClicked("test_name", "test_description");

        verify(account).setName("test_name");
        verify(account).setDescription("test_description");
        verify(view).goToListAccounts();
    }
    // endregion names

    private void mockAccountUrisImap() {
        String incomingUri = "imap+ssl+://PLAIN:daquexian566:denx+ajs@mail.gmail.com/1%7C";
        String outgoingUri = "smtp+ssl+://daquexian566:denx+ajs:PLAIN@mail.gmail.com";
        when(account.getStoreUri()).thenReturn(incomingUri);
        when(account.getTransportUri()).thenReturn(outgoingUri);
        when(account.getEmail()).thenReturn("daquexian566@gmail.com");
    }

    private void mockAccountUrisPop3() {
        String incomingUri = "pop3+ssl+://PLAIN:daquexian566:denx+ajs@mail.gmail.com/1%7C";
        String outgoingUri = "smtp+ssl+://daquexian566:denx+ajs:PLAIN@mail.gmail.com";
        when(account.getStoreUri()).thenReturn(incomingUri);
        when(account.getTransportUri()).thenReturn(outgoingUri);
        when(account.getEmail()).thenReturn("daquexian566@gmail.com");
    }
}