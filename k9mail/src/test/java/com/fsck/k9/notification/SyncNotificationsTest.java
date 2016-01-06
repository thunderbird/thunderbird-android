package com.fsck.k9.notification;


import android.app.Notification;
import android.app.PendingIntent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationManagerCompat;

import com.fsck.k9.Account;
import com.fsck.k9.MockHelper;
import com.fsck.k9.R;
import com.fsck.k9.mail.Folder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 21)
public class SyncNotificationsTest {
    private static final int ACCOUNT_NUMBER = 1;
    private static final String ACCOUNT_NAME = "TestAccount";
    private static final String FOLDER_NAME = "Inbox";


    private NotificationManagerCompat notificationManager;
    private Builder builder;
    private Account account;
    private SyncNotifications syncNotifications;
    private PendingIntent contentIntent;


    @Before
    public void setUp() throws Exception {
        notificationManager = createFakeNotificationManager();
        builder = createFakeNotificationBuilder();
        NotificationController controller = createFakeNotificationController(notificationManager, builder);
        account = createFakeAccount();
        contentIntent = createFakeContentIntent();
        NotificationActionCreator actionBuilder = createActionBuilder(contentIntent);

        syncNotifications = new SyncNotifications(controller, actionBuilder);
    }

    @Test
    public void testShowSendingNotification() throws Exception {
        int notificationId = NotificationIds.getFetchingMailNotificationId(account);

        syncNotifications.showSendingNotification(account);

        verify(notificationManager).notify(eq(notificationId), any(Notification.class));
        verify(builder).setSmallIcon(R.drawable.ic_notify_check_mail);
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
        Folder folder = createFakeFolder();
        int notificationId = NotificationIds.getFetchingMailNotificationId(account);

        syncNotifications.showFetchingMailNotification(account, folder);

        verify(notificationManager).notify(eq(notificationId), any(Notification.class));
        verify(builder).setSmallIcon(R.drawable.ic_notify_check_mail);
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
        when(controller.getAccountName(any(Account.class))).thenReturn(ACCOUNT_NAME);
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
        when(actionBuilder.createViewFolderPendingIntent(any(Account.class), anyString(), anyInt()))
                .thenReturn(contentIntent);
        return actionBuilder;
    }

    private Folder createFakeFolder() {
        Folder folder = mock(Folder.class);
        when(folder.getName()).thenReturn(FOLDER_NAME);
        return folder;
    }
}
