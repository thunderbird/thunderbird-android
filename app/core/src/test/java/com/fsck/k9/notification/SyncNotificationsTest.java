package com.fsck.k9.notification;


import android.app.Notification;
import android.app.PendingIntent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.NotificationManagerCompat;

import com.fsck.k9.Account;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.testing.MockHelper;
import com.fsck.k9.RobolectricTest;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class SyncNotificationsTest extends RobolectricTest {
    private static final int ACCOUNT_NUMBER = 1;
    private static final String ACCOUNT_NAME = "TestAccount";
    private static final String FOLDER_SERVER_ID = "INBOX";
    private static final String FOLDER_NAME = "Inbox";


    private NotificationResourceProvider resourceProvider = new TestNotificationResourceProvider();
    private Notification notification;
    private NotificationManagerCompat notificationManager;
    private Builder builder;
    private Account account;
    private SyncNotifications syncNotifications;
    private PendingIntent contentIntent;


    @Before
    public void setUp() throws Exception {
        notification = createFakeNotification();
        notificationManager = createFakeNotificationManager();
        builder = createFakeNotificationBuilder(notification);
        NotificationHelper notificationHelper = createFakeNotificationHelper(notificationManager, builder);
        account = createFakeAccount();
        contentIntent = createFakeContentIntent();
        NotificationActionCreator actionBuilder = createActionBuilder(contentIntent);

        syncNotifications = new SyncNotifications(notificationHelper, actionBuilder, resourceProvider);
    }

    @Test
    public void testShowSendingNotification() throws Exception {
        int notificationId = NotificationIds.getFetchingMailNotificationId(account);

        syncNotifications.showSendingNotification(account);

        verify(notificationManager).notify(notificationId, notification);
        verify(builder).setSmallIcon(resourceProvider.getIconSendingMail());
        verify(builder).setTicker("Sending mail: " + ACCOUNT_NAME);
        verify(builder).setContentTitle("Sending mail");
        verify(builder).setContentText(ACCOUNT_NAME);
        verify(builder).setContentIntent(contentIntent);
        verify(builder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }

    @Test
    public void testClearSendingNotification() throws Exception {
        int notificationId = NotificationIds.getFetchingMailNotificationId(account);

        syncNotifications.clearSendingNotification(account);

        verify(notificationManager).cancel(notificationId);
    }

    @Test
    public void testGetFetchingMailNotificationId() throws Exception {
        LocalFolder localFolder = createFakeLocalFolder();
        int notificationId = NotificationIds.getFetchingMailNotificationId(account);

        syncNotifications.showFetchingMailNotification(account, localFolder);

        verify(notificationManager).notify(notificationId, notification);
        verify(builder).setSmallIcon(resourceProvider.getIconCheckingMail());
        verify(builder).setTicker("Checking mail: " + ACCOUNT_NAME + ":" + FOLDER_NAME);
        verify(builder).setContentTitle("Checking mail");
        verify(builder).setContentText(ACCOUNT_NAME + ":" + FOLDER_NAME);
        verify(builder).setContentIntent(contentIntent);
        verify(builder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }

    @Test
    public void testClearSendFailedNotification() throws Exception {
        int notificationId = NotificationIds.getFetchingMailNotificationId(account);

        syncNotifications.clearFetchingMailNotification(account);

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

    private NotificationHelper createFakeNotificationHelper(
            NotificationManagerCompat notificationManager, Builder builder) {
        NotificationHelper notificationHelper = mock(NotificationHelper.class);
        when(notificationHelper.getContext()).thenReturn(RuntimeEnvironment.application);
        when(notificationHelper.getNotificationManager()).thenReturn(notificationManager);
        when(notificationHelper.createNotificationBuilder(any(Account.class),
                any(NotificationChannelManager.ChannelType.class)))
                .thenReturn(builder);
        when(notificationHelper.getAccountName(any(Account.class))).thenReturn(ACCOUNT_NAME);

        return notificationHelper;
    }

    private Account createFakeAccount() {
        Account account = mock(Account.class);
        when(account.getAccountNumber()).thenReturn(ACCOUNT_NUMBER);
        when(account.getDescription()).thenReturn(ACCOUNT_NAME);
        when(account.getOutboxFolder()).thenReturn("OUTBOX");

        return account;
    }

    private PendingIntent createFakeContentIntent() {
        return mock(PendingIntent.class);
    }

    private NotificationActionCreator createActionBuilder(PendingIntent contentIntent) {
        NotificationActionCreator actionBuilder = mock(NotificationActionCreator.class);
        when(actionBuilder.createViewFolderPendingIntent(eq(account), anyString(), anyInt()))
                .thenReturn(contentIntent);
        return actionBuilder;
    }

    private LocalFolder createFakeLocalFolder() {
        LocalFolder folder = mock(LocalFolder.class);
        when(folder.getServerId()).thenReturn(FOLDER_SERVER_ID);
        when(folder.getName()).thenReturn(FOLDER_NAME);
        return folder;
    }
}
