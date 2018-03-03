package com.fsck.k9.notification;


import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationCompat.Builder;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9.NotificationQuickDelete;
import com.fsck.k9.MockHelper;
import com.fsck.k9.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
public class BaseNotificationsTest {
    private static final int ACCOUNT_COLOR = 0xAABBCC;
    private static final String ACCOUNT_NAME = "AccountName";
    private static final int ACCOUNT_NUMBER = 2;
    private static final String NOTIFICATION_SUMMARY = "Summary";
    private static final String SENDER = "MessageSender";
    private static final String SUBJECT = "Subject";
    private static final String NOTIFICATION_PREVIEW = "Preview";


    private TestNotifications notifications;


    @Before
    public void setUp() throws Exception {
        notifications = createTestNotifications();
    }

    @Test
    public void testCreateAndInitializeNotificationBuilder() throws Exception {
        Account account = createFakeAccount();

        Builder builder = notifications.createAndInitializeNotificationBuilder(account);

        verify(builder).setSmallIcon(R.drawable.notification_icon_new_mail);
        verify(builder).setColor(ACCOUNT_COLOR);
        verify(builder).setAutoCancel(true);
    }

    @Test
    public void testIsDeleteActionEnabled_NotificationQuickDelete_ALWAYS() throws Exception {
        K9.setNotificationQuickDeleteBehaviour(NotificationQuickDelete.ALWAYS);

        boolean result = notifications.isDeleteActionEnabled();

        assertTrue(result);
    }

    @Test
    public void testIsDeleteActionEnabled_NotificationQuickDelete_FOR_SINGLE_MSG() throws Exception {
        K9.setNotificationQuickDeleteBehaviour(NotificationQuickDelete.FOR_SINGLE_MSG);

        boolean result = notifications.isDeleteActionEnabled();

        assertTrue(result);
    }

    @Test
    public void testIsDeleteActionEnabled_NotificationQuickDelete_NEVER() throws Exception {
        K9.setNotificationQuickDeleteBehaviour(NotificationQuickDelete.NEVER);

        boolean result = notifications.isDeleteActionEnabled();

        assertFalse(result);
    }

    @Test
    public void testCreateBigTextStyleNotification() throws Exception {
        Account account = createFakeAccount();
        int notificationId = 23;
        NotificationHolder holder = createNotificationHolder(notificationId);

        Builder builder = notifications.createBigTextStyleNotification(account, holder, notificationId);

        verify(builder).setTicker(NOTIFICATION_SUMMARY);
        verify(builder).setGroup("newMailNotifications-" + ACCOUNT_NUMBER);
        verify(builder).setContentTitle(SENDER);
        verify(builder).setContentText(SUBJECT);
        verify(builder).setSubText(ACCOUNT_NAME);

        BigTextStyle bigTextStyle = notifications.bigTextStyle;
        verify(bigTextStyle).bigText(NOTIFICATION_PREVIEW);

        verify(builder).setStyle(bigTextStyle);
    }

    private NotificationHolder createNotificationHolder(int notificationId) {
        NotificationContent content = new NotificationContent(null, SENDER, SUBJECT, NOTIFICATION_PREVIEW,
                NOTIFICATION_SUMMARY, false);
        return new NotificationHolder(notificationId, content);
    }

    private TestNotifications createTestNotifications() {
        NotificationController controller = createFakeController();
        NotificationActionCreator actionCreator = mock(NotificationActionCreator.class);

        return new TestNotifications(controller, actionCreator);
    }

    private NotificationController createFakeController() {
        Builder builder = MockHelper.mockBuilder(Builder.class);
        NotificationController controller = mock(NotificationController.class);
        when(controller.createNotificationBuilder()).thenReturn(builder);
        when(controller.getAccountName(any(Account.class))).thenReturn(ACCOUNT_NAME);
        return controller;
    }

    private Account createFakeAccount() {
        Account account = mock(Account.class);
        when(account.getAccountNumber()).thenReturn(ACCOUNT_NUMBER);
        when(account.getChipColor()).thenReturn(ACCOUNT_COLOR);
        return account;
    }


    static class TestNotifications extends BaseNotifications {

        BigTextStyle bigTextStyle;

        protected TestNotifications(NotificationController controller, NotificationActionCreator actionCreator) {
            super(controller, actionCreator);
            bigTextStyle = mock(BigTextStyle.class);
        }

        @Override
        protected BigTextStyle createBigTextStyle(Builder builder) {
            return bigTextStyle;
        }
    }
}
