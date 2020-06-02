package com.fsck.k9.notification;


import android.app.Notification;
import android.app.PendingIntent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.NotificationManagerCompat;

import com.fsck.k9.Account;
import com.fsck.k9.testing.MockHelper;
import com.fsck.k9.RobolectricTest;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class SendFailedNotificationsTest extends RobolectricTest {
    private static final int ACCOUNT_NUMBER = 1;
    private static final String ACCOUNT_NAME = "TestAccount";


    private NotificationResourceProvider resourceProvider = new TestNotificationResourceProvider();
    private Notification notification;
    private NotificationManagerCompat notificationManager;
    private Builder builder;
    private Account account;
    private SendFailedNotifications sendFailedNotifications;
    private PendingIntent contentIntent;
    private int notificationId;


    @Before
    public void setUp() throws Exception {
        notification = createFakeNotification();
        notificationManager = createFakeNotificationManager();
        builder = createFakeNotificationBuilder(notification);
        NotificationHelper notificationHelper = createFakeNotificationHelper(notificationManager, builder);
        account = createFakeAccount();
        contentIntent = createFakeContentIntent();
        NotificationActionCreator actionBuilder = createActionBuilder(contentIntent);
        notificationId = NotificationIds.getSendFailedNotificationId(account);

        sendFailedNotifications = new SendFailedNotifications(notificationHelper, actionBuilder, resourceProvider);
    }

    @Test
    public void testShowSendFailedNotification() throws Exception {
        Exception exception = new Exception();

        sendFailedNotifications.showSendFailedNotification(account, exception);

        verify(notificationManager).notify(notificationId, notification);
        verify(builder).setSmallIcon(resourceProvider.getIconWarning());
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

    private Notification createFakeNotification() {
        return mock(Notification.class);
    }

    private NotificationManagerCompat createFakeNotificationManager() {
        return mock(NotificationManagerCompat.class);
    }

    private Builder createFakeNotificationBuilder(Notification notification) {
        Builder builder = MockHelper.mockBuilder(Builder.class);
        when(builder.build()).thenReturn(notification);
        return builder;
    }

    private NotificationHelper createFakeNotificationHelper(NotificationManagerCompat notificationManager,
            Builder builder) {
        NotificationHelper notificationHelper = mock(NotificationHelper.class);
        when(notificationHelper.getContext()).thenReturn(RuntimeEnvironment.application);
        when(notificationHelper.getNotificationManager()).thenReturn(notificationManager);
        when(notificationHelper.createNotificationBuilder(any(Account.class),
                any(NotificationChannelManager.ChannelType.class)))
                .thenReturn(builder);

        return notificationHelper;
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
