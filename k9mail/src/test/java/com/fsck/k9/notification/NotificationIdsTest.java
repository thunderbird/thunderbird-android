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
@Config(manifest = Config.NONE, sdk = 21)
public class NotificationIdsTest {
    private static final boolean INCOMING = true;
    private static final boolean OUTGOING = false;

    private static final int NUM_ACCOUNTS = 2;

    @Test
    public void getNewMailSummaryNotificationId_withDefaultAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);

        int notificationId = NotificationIds.getNewMailSummaryNotificationId(account);

        assertEquals(NotificationIds.OFFSET_NEW_MAIL_SUMMARY, notificationId);
    }

    @Test
    public void getNewMailStackedNotificationId_withDefaultAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(0);
        int notificationIndex = 0;

        int notificationId = NotificationIds.getNewMailStackedNotificationId(account, notificationIndex);

        assertEquals(NotificationIds.OFFSET_NEW_MAIL_STACKED, notificationId);
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

        assertEquals(NotificationIds.NUMBER_OF_NOTIFICATIONS_PER_ACCOUNT * 1 + NotificationIds.OFFSET_NEW_MAIL_SUMMARY, notificationId);
    }

    @Test
    public void getNewMailStackedNotificationId_withSecondAccount() throws Exception {
        Account account = createMockAccountWithAccountNumber(1);
        int notificationIndex = 7;

        int notificationId = NotificationIds.getNewMailStackedNotificationId(account, notificationIndex);

        assertEquals(NotificationIds.NUMBER_OF_NOTIFICATIONS_PER_ACCOUNT * 1 + NotificationIds.OFFSET_NEW_MAIL_STACKED + notificationIndex, notificationId);
    }

    @Test
    public void getFetchingMailNotificationId_withDefaultAccount() throws Exception {
        for (int i = 0; i < NUM_ACCOUNTS; i++) {
            Account account = createMockAccountWithAccountNumber(i);

            int notificationId = NotificationIds.getFetchingMailNotificationId(account);

            assertEquals(NotificationIds.NUMBER_OF_NOTIFICATIONS_PER_ACCOUNT * i + NotificationIds.OFFSET_FETCHING_MAIL, notificationId);
        }
    }

    @Test
    public void getSendFailedNotificationId_withDefaultAccount() throws Exception {
        for (int i = 0; i < NUM_ACCOUNTS; i++) {
            Account account = createMockAccountWithAccountNumber(i);

            int notificationId = NotificationIds.getSendFailedNotificationId(account);

            assertEquals(NotificationIds.NUMBER_OF_NOTIFICATIONS_PER_ACCOUNT * i + NotificationIds.OFFSET_SEND_FAILED_NOTIFICATION, notificationId);
        }
    }

    @Test
    public void getCertificateErrorNotificationId_forIncomingServerWithDefaultAccount() throws Exception {
        for (int i = 0; i < NUM_ACCOUNTS; i++) {
            Account account = createMockAccountWithAccountNumber(i);

            int notificationId = NotificationIds.getCertificateErrorNotificationId(account, INCOMING);

            assertEquals(NotificationIds.NUMBER_OF_NOTIFICATIONS_PER_ACCOUNT * i + NotificationIds.OFFSET_CERTIFICATE_ERROR_INCOMING, notificationId);
        }
    }

    @Test
    public void getCertificateErrorNotificationId_forOutgoingServerWithDefaultAccount() throws Exception {
        for (int i = 0; i < NUM_ACCOUNTS; i++) {
            Account account = createMockAccountWithAccountNumber(i);


            int notificationId = NotificationIds.getCertificateErrorNotificationId(account, OUTGOING);

            assertEquals(NotificationIds.NUMBER_OF_NOTIFICATIONS_PER_ACCOUNT * i + NotificationIds.OFFSET_CERTIFICATE_ERROR_OUTGOING, notificationId);
        }
    }

    @Test
    public void getAuthenticationErrorNotificationId_forIncomingServerWithDefaultAccount() throws Exception {
        for (int i = 0; i < NUM_ACCOUNTS; i++) {
            Account account = createMockAccountWithAccountNumber(i);


            int notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, INCOMING);

            assertEquals(NotificationIds.NUMBER_OF_NOTIFICATIONS_PER_ACCOUNT * i + NotificationIds.OFFSET_AUTHENTICATION_ERROR_INCOMING, notificationId);
        }
    }

    @Test
    public void getAuthenticationErrorNotificationId_forOutgoingServerWithDefaultAccount() throws Exception {
        for (int i = 0; i < NUM_ACCOUNTS; i++) {
            Account account = createMockAccountWithAccountNumber(i);


            int notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, OUTGOING);

            assertEquals(NotificationIds.NUMBER_OF_NOTIFICATIONS_PER_ACCOUNT * i + NotificationIds.OFFSET_AUTHENTICATION_ERROR_OUTGOING, notificationId);
        }
    }


    private Account createMockAccountWithAccountNumber(int accountNumber) {
        Account account = mock(Account.class);
        when(account.getAccountNumber()).thenReturn(accountNumber);
        return account;
    }
}
