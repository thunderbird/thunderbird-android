package com.fsck.k9.notification;


import com.fsck.k9.Account;
import com.fsck.k9.K9RobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(K9RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class NotificationIdsTest {
    private static final boolean INCOMING = true;
    private static final boolean OUTGOING = false;

    @Test
    public void getNewMailSummaryNotificationId_withDefaultAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);

        int notificationId = NotificationIds.getNewMailSummaryNotificationId(account);

        assertEquals(6, notificationId);
    }

    @Test
    public void getNewMailStackedNotificationId_withDefaultAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);
        int notificationIndex = 0;

        int notificationId = NotificationIds.getNewMailStackedNotificationId(account, notificationIndex);

        assertEquals(7, notificationId);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getNewMailStackedNotificationId_withTooLowIndex() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);

        NotificationIds.getNewMailStackedNotificationId(account, -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getNewMailStackedNotificationId_withTooLargeIndex() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);

        NotificationIds.getNewMailStackedNotificationId(account, 8);
    }

    @Test
    public void getNewMailSummaryNotificationId_withSecondAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(1);

        int notificationId = NotificationIds.getNewMailSummaryNotificationId(account);

        assertEquals(21, notificationId);
    }

    @Test
    public void getNewMailStackedNotificationId_withSecondAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(1);
        int notificationIndex = 7;

        int notificationId = NotificationIds.getNewMailStackedNotificationId(account, notificationIndex);

        assertEquals(29, notificationId);
    }

    @Test
    public void getFetchingMailNotificationId_withDefaultAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);

        int notificationId = NotificationIds.getFetchingMailNotificationId(account);

        assertEquals(5, notificationId);
    }

    @Test
    public void getFetchingMailNotificationId_withSecondAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(1);

        int notificationId = NotificationIds.getFetchingMailNotificationId(account);

        assertEquals(20, notificationId);
    }

    @Test
    public void getSendFailedNotificationId_withDefaultAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);

        int notificationId = NotificationIds.getSendFailedNotificationId(account);

        assertEquals(0, notificationId);
    }

    @Test
    public void getSendFailedNotificationId_withSecondAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(1);

        int notificationId = NotificationIds.getSendFailedNotificationId(account);

        assertEquals(15, notificationId);
    }

    @Test
    public void getCertificateErrorNotificationId_forIncomingServerWithDefaultAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);

        int notificationId = NotificationIds.getCertificateErrorNotificationId(account, INCOMING);

        assertEquals(1, notificationId);
    }

    @Test
    public void getCertificateErrorNotificationId_forIncomingServerWithSecondAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(1);

        int notificationId = NotificationIds.getCertificateErrorNotificationId(account, INCOMING);

        assertEquals(16, notificationId);
    }

    @Test
    public void getCertificateErrorNotificationId_forOutgoingServerWithDefaultAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);

        int notificationId = NotificationIds.getCertificateErrorNotificationId(account, OUTGOING);

        assertEquals(2, notificationId);
    }

    @Test
    public void getCertificateErrorNotificationId_forOutgoingServerWithSecondAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(1);

        int notificationId = NotificationIds.getCertificateErrorNotificationId(account, OUTGOING);

        assertEquals(17, notificationId);
    }

    @Test
    public void getAuthenticationErrorNotificationId_forIncomingServerWithDefaultAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);

        int notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, INCOMING);

        assertEquals(3, notificationId);
    }

    @Test
    public void getAuthenticationErrorNotificationId_forIncomingServerWithSecondAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(1);

        int notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, INCOMING);

        assertEquals(18, notificationId);
    }

    @Test
    public void getAuthenticationErrorNotificationId_forOutgoingServerWithDefaultAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);

        int notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, OUTGOING);

        assertEquals(4, notificationId);
    }

    @Test
    public void getAuthenticationErrorNotificationId_forOutgoingServerWithSecondAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(1);

        int notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, OUTGOING);

        assertEquals(19, notificationId);
    }

    private Account createMockAccountWithAccountNumber(int accountNumber) {
        Account account = mock(Account.class);
        when(account.getAccountNumber()).thenReturn(accountNumber);
        return account;
    }
}
