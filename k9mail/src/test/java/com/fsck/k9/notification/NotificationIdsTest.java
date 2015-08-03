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

        assertEquals(0, notificationId);
    }

    @Test
    public void getNewMailNotificationId_withDefaultAccountAndOffset() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);
        int notificationOffset = 1;

        int notificationId = NotificationIds.getNewMailNotificationId(account, notificationOffset);

        assertEquals(1000, notificationId);
    }

    @Test
    public void getNewMailNotificationId_withSecondAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(1);

        int notificationId = NotificationIds.getNewMailNotificationId(account);

        assertEquals(1, notificationId);
    }

    @Test
    public void getNewMailNotificationId_withSecondAccountAndOffset() throws Exception {
        Account account = createMockAccountWithAccountNumber(1);
        int notificationOffset = 8;

        int notificationId = NotificationIds.getNewMailNotificationId(account, notificationOffset);

        assertEquals(8001, notificationId);
    }

    @Test
    public void getFetchingMailNotificationId_withDefaultAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);

        int notificationId = NotificationIds.getFetchingMailNotificationId(account);

        assertEquals(-5000, notificationId);
    }

    @Test
    public void getFetchingMailNotificationId_withSecondAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(1);

        int notificationId = NotificationIds.getFetchingMailNotificationId(account);

        assertEquals(-4999, notificationId);
    }

    @Test
    public void getSendFailedNotificationId_withDefaultAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);

        int notificationId = NotificationIds.getSendFailedNotificationId(account);

        assertEquals(-1500, notificationId);
    }

    @Test
    public void getSendFailedNotificationId_withSecondAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(1);

        int notificationId = NotificationIds.getSendFailedNotificationId(account);

        assertEquals(-1499, notificationId);
    }

    @Test
    public void getCertificateErrorNotificationId_forIncomingServerWithDefaultAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);

        int notificationId = NotificationIds.getCertificateErrorNotificationId(account, INCOMING);

        assertEquals(-2000, notificationId);
    }

    @Test
    public void getCertificateErrorNotificationId_forIncomingServerWithSecondAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(1);

        int notificationId = NotificationIds.getCertificateErrorNotificationId(account, INCOMING);

        assertEquals(-1999, notificationId);
    }

    @Test
    public void getCertificateErrorNotificationId_forOutgoingServerWithDefaultAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);

        int notificationId = NotificationIds.getCertificateErrorNotificationId(account, OUTGOING);

        assertEquals(-2500, notificationId);
    }

    @Test
    public void getCertificateErrorNotificationId_forOutgoingServerWithSecondAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(1);

        int notificationId = NotificationIds.getCertificateErrorNotificationId(account, OUTGOING);

        assertEquals(-2499, notificationId);
    }

    private Account createMockAccountWithAccountNumber(int accountNumber) {
        Account account = mock(Account.class);
        when(account.getAccountNumber()).thenReturn(accountNumber);
        return account;
    }
}
