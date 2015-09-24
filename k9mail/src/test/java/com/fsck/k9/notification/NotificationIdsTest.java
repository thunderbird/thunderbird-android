package com.fsck.k9.notification;


import com.fsck.k9.Account;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class NotificationIdsTest {
    private static final boolean INCOMING = true;
    private static final boolean OUTGOING = false;

    @Test
    public void getNewMailNotificationId_withDefaultAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);

        int notificationId = NotificationIds.getNewMailNotificationId(account);

        assertEquals(4, notificationId);
    }

    @Test
    public void getNewMailNotificationId_withDefaultAccountAndOffset() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);
        int notificationOffset = 1;

        int notificationId = NotificationIds.getNewMailNotificationId(account, notificationOffset);

        assertEquals(5, notificationId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNewMailNotificationId_withTooLowOffset() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);

        NotificationIds.getNewMailNotificationId(account, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNewMailNotificationId_withTooLargeOffset() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);

        NotificationIds.getNewMailNotificationId(account, 9);
    }

    @Test
    public void getNewMailNotificationId_withSecondAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(1);

        int notificationId = NotificationIds.getNewMailNotificationId(account);

        assertEquals(17, notificationId);
    }

    @Test
    public void getNewMailNotificationId_withSecondAccountAndOffset() throws Exception {
        Account account = createMockAccountWithAccountNumber(1);
        int notificationOffset = 8;

        int notificationId = NotificationIds.getNewMailNotificationId(account, notificationOffset);

        assertEquals(25, notificationId);
    }

    @Test
    public void getFetchingMailNotificationId_withDefaultAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);

        int notificationId = NotificationIds.getFetchingMailNotificationId(account);

        assertEquals(3, notificationId);
    }

    @Test
    public void getFetchingMailNotificationId_withSecondAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(1);

        int notificationId = NotificationIds.getFetchingMailNotificationId(account);

        assertEquals(16, notificationId);
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

        assertEquals(13, notificationId);
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

        assertEquals(14, notificationId);
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

        assertEquals(15, notificationId);
    }

    private Account createMockAccountWithAccountNumber(int accountNumber) {
        Account account = mock(Account.class);
        when(account.getAccountNumber()).thenReturn(accountNumber);
        return account;
    }
}
