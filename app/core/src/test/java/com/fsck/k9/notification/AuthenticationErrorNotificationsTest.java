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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class AuthenticationErrorNotificationsTest extends RobolectricTest {
    private static final boolean INCOMING = true;
    private static final boolean OUTGOING = false;
    private static final int ACCOUNT_NUMBER = 1;
    private static final String ACCOUNT_NAME = "TestAccount";


    private NotificationResourceProvider resourceProvider = new TestNotificationResourceProvider();
    private Notification notification;
    private NotificationManagerCompat notificationManager;
    private NotificationCompat.Builder builder;
    private NotificationHelper notificationHelper;
    private Account account;
    private AuthenticationErrorNotifications authenticationErrorNotifications;
    private PendingIntent contentIntent;


    @Before
    public void setUp() throws Exception {
        notification = createFakeNotification();
        notificationManager = createFakeNotificationManager();
        builder = createFakeNotificationBuilder(notification);
        notificationHelper = createFakeNotificationHelper(notificationManager, builder);
        account = createFakeAccount();
        contentIntent = createFakeContentIntent();

        authenticationErrorNotifications = new TestAuthenticationErrorNotifications();
    }

    @Test
    public void showAuthenticationErrorNotification_withIncomingServer_shouldCreateNotification() throws Exception {
        int notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, INCOMING);

        authenticationErrorNotifications.showAuthenticationErrorNotification(account, INCOMING);

        verify(notificationManager).notify(notificationId, notification);
        assertAuthenticationErrorNotificationContents();
    }

    @Test
    public void clearAuthenticationErrorNotification_withIncomingServer_shouldCancelNotification() throws Exception {
        int notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, INCOMING);

        authenticationErrorNotifications.clearAuthenticationErrorNotification(account, INCOMING);

        verify(notificationManager).cancel(notificationId);
    }

    @Test
    public void showAuthenticationErrorNotification_withOutgoingServer_shouldCreateNotification() throws Exception {
        int notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, OUTGOING);

        authenticationErrorNotifications.showAuthenticationErrorNotification(account, OUTGOING);

        verify(notificationManager).notify(notificationId, notification);
        assertAuthenticationErrorNotificationContents();
    }

    @Test
    public void clearAuthenticationErrorNotification_withOutgoingServer_shouldCancelNotification() throws Exception {
        int notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, OUTGOING);

        authenticationErrorNotifications.clearAuthenticationErrorNotification(account, OUTGOING);

        verify(notificationManager).cancel(notificationId);
    }

    private void assertAuthenticationErrorNotificationContents() {
        verify(builder).setSmallIcon(resourceProvider.getIconWarning());
        verify(builder).setTicker("Authentication failed");
        verify(builder).setContentTitle("Authentication failed");
        verify(builder).setContentText("Authentication failed for " + ACCOUNT_NAME + ". Update your server settings.");
        verify(builder).setContentIntent(contentIntent);
        verify(builder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
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
            NotificationCompat.Builder builder) {
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


    class TestAuthenticationErrorNotifications extends AuthenticationErrorNotifications {
        public TestAuthenticationErrorNotifications() {
            super(notificationHelper, mock(NotificationActionCreator.class), resourceProvider);
        }

        @Override
        PendingIntent createContentIntent(Account account, boolean incoming) {
            return contentIntent;
        }
    }
}
