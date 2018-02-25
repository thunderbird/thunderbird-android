package com.fsck.k9.notification;


import android.app.Notification;
import android.app.PendingIntent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationManagerCompat;

import com.fsck.k9.Account;
import com.fsck.k9.MockHelper;
import com.fsck.k9.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
public class SendFailedNotificationsTest {
    private static final int ACCOUNT_NUMBER = 1;
    private static final String ACCOUNT_NAME = "TestAccount";


    private NotificationManagerCompat notificationManager;
    private Builder builder;
    private Account account;
    private SendFailedNotifications sendFailedNotifications;
    private PendingIntent contentIntent;
    private int notificationId;


    @Before
    public void setUp() throws Exception {
        notificationManager = createFakeNotificationManager();
        builder = createFakeNotificationBuilder();
        NotificationController controller = createFakeNotificationController(notificationManager, builder);
        account = createFakeAccount();
        contentIntent = createFakeContentIntent();
        NotificationActionCreator actionBuilder = createActionBuilder(contentIntent);
        notificationId = NotificationIds.getSendFailedNotificationId(account);

        sendFailedNotifications = new SendFailedNotifications(controller, actionBuilder);
    }

    @Test
    public void testShowSendFailedNotification() throws Exception {
        Exception exception = new Exception();

        sendFailedNotifications.showSendFailedNotification(account, exception);

        verify(notificationManager).notify(eq(notificationId), any(Notification.class));
        verify(builder).setSmallIcon(R.drawable.notification_icon_new_mail);
        verify(builder).setTicker("Failed to send some messages");
        verify(builder).setContentTitle("Failed to send some messages");
        verify(builder).setContentText("Exception");
        verify(builder).setContentIntent(contentIntent);
        verify(builder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }

    @Test
    public void testClearSendFailedNotification() throws Exception {
        sendFailedNotifications.clearSendFailedNotification(account);

        verify(notificationManager).cancel(notificationId);
    }

    private NotificationManagerCompat createFakeNotificationManager() {
        return mock(NotificationManagerCompat.class);
    }

    private Builder createFakeNotificationBuilder() {
        return MockHelper.mockBuilder(Builder.class);
    }

    private NotificationController createFakeNotificationController(NotificationManagerCompat notificationManager,
            Builder builder) {
        NotificationController controller = mock(NotificationController.class);
        when(controller.getContext()).thenReturn(RuntimeEnvironment.application);
        when(controller.getNotificationManager()).thenReturn(notificationManager);
        when(controller.createNotificationBuilder()).thenReturn(builder);
        return controller;
    }

    private Account createFakeAccount() {
        Account account = mock(Account.class);
        when(account.getAccountNumber()).thenReturn(ACCOUNT_NUMBER);
        when(account.getDescription()).thenReturn(ACCOUNT_NAME);

        return account;
    }

    private PendingIntent createFakeContentIntent() {
        return mock(PendingIntent.class);
    }

    private NotificationActionCreator createActionBuilder(PendingIntent contentIntent) {
        NotificationActionCreator actionBuilder = mock(NotificationActionCreator.class);
        when(actionBuilder.createViewFolderListPendingIntent(any(Account.class), anyInt())).thenReturn(contentIntent);
        return actionBuilder;
    }
}
