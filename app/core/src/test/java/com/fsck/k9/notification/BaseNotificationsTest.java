package com.fsck.k9.notification;


import android.app.Notification;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationCompat.Builder;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9.NotificationQuickMoveTrigger;
import com.fsck.k9.K9.NotificationQuickMoveType;
import com.fsck.k9.MockHelper;
import com.fsck.k9.controller.MessageReference;
import com.fsck.k9.controller.MessagingController;

import org.junit.Before;
import org.junit.Test;

import static com.fsck.k9.MockHelper.mockBuilder;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


public class BaseNotificationsTest {
    private static final int ACCOUNT_COLOR = 0xAABBCC;
    private static final String ACCOUNT_NAME = "AccountName";
    private static final int ACCOUNT_NUMBER = 2;
    private static final String NOTIFICATION_SUMMARY = "Summary";
    private static final String SENDER = "MessageSender";
    private static final String SUBJECT = "Subject";
    private static final String NOTIFICATION_PREVIEW = "Preview";
    private static final Notification FAKE_NOTIFICATION = mock(Notification.class);


    private NotificationResourceProvider resourceProvider = new TestNotificationResourceProvider();
    private TestNotifications notifications;


    @Before
    public void setUp() throws Exception {
        notifications = createTestNotifications();
    }

    @Test
    public void testCreateAndInitializeNotificationBuilder() throws Exception {
        Account account = createFakeAccount();

        Builder builder = notifications.createAndInitializeNotificationBuilder(account);

        verify(builder).setSmallIcon(resourceProvider.getIconNewMail());
        verify(builder).setColor(ACCOUNT_COLOR);
        verify(builder).setAutoCancel(true);
    }

    @Test
    public void testIsQuickMoveEnabled_NotificationQuickMoveTrigger_ALWAYS() {
        K9.setNotificationQuickMoveTrigger(NotificationQuickMoveTrigger.ALWAYS);

        boolean result = notifications.isQuickMoveEnabled();

        assertTrue(result);
    }

    @Test
    public void testIsQuickMoveEnabled_NotificationQuickMoveTrigger_FOR_SINGLE_MSG() {
        K9.setNotificationQuickMoveTrigger(NotificationQuickMoveTrigger.FOR_SINGLE_MSG);

        boolean result = notifications.isQuickMoveEnabled();

        assertTrue(result);
    }

    @Test
    public void testIsQuickMoveEnabled_NotificationQuickMoveTrigger_NEVER() {
        K9.setNotificationQuickMoveTrigger(NotificationQuickMoveTrigger.NEVER);

        boolean result = notifications.isQuickMoveEnabled();

        assertFalse(result);
    }

    @Test
    public void testIsArchiveActionAvailable_archiveFolderFound_movePossible() {
        Account account = createFakeAccount();
        setArchiveFolder(account, "Archive");
        notifications.setMoveCapable(account,true);

        boolean result = notifications.isArchiveActionAvailable(account);

        assertTrue(result);
    }

    @Test
    public void testIsArchiveActionAvailable_archiveFolderFound_moveNotPossible() {
        Account account = createFakeAccount();
        setArchiveFolder(account, K9.FOLDER_NONE);

        boolean result = notifications.isArchiveActionAvailable(account);

        assertFalse(result);
    }

    @Test
    public void testIsArchiveActionAvailable_archiveFolderNotFound() {
        Account account = createFakeAccount();
        setArchiveFolder(account, null);

        boolean result = notifications.isArchiveActionAvailable(account);

        assertFalse(result);
    }

    @Test
    public void testIsMovePossible_validFolder_isCapableOfMove() {
        Account account = createFakeAccount();
        notifications.setMoveCapable(account, true);

        boolean result = notifications.isMovePossible(account, "Archive");

        assertTrue(result);
    }

    @Test
    public void testIsMovePossible_validFolder_notCapableOfMove() {
        Account account = createFakeAccount();
        notifications.setMoveCapable(account, false);

        boolean result = notifications.isMovePossible(account, "Archive");

        assertFalse(result);
    }

    @Test
    public void testIsMovePossible_invalidFolder() {
        Account account = createFakeAccount();

        boolean result = notifications.isMovePossible(account, K9.FOLDER_NONE);

        assertFalse(result);
    }

    @Test
    public void testAddQuickMoveAction_NotificationQuickMoveType_DELETE() {
        K9.setNotificationQuickMoveType(NotificationQuickMoveType.DELETE);
        K9.setNotificationQuickMoveTrigger(NotificationQuickMoveTrigger.ALWAYS);
        Account account = createFakeAccount();
        Builder builder = createFakeNotificationBuilder();
        NotificationContent notificationContent = createNotificationContent();
        int notificationId = 23;

        notifications.addQuickMoveAction(account, builder, notificationContent, notificationId);

        verify(builder).addAction(resourceProvider.getIconDelete(), "Delete", null);
    }

    @Test
    public void testAddQuickMoveAction_NotificationQuickMoveType_ARCHIVE() {
        K9.setNotificationQuickMoveType(NotificationQuickMoveType.ARCHIVE);
        K9.setNotificationQuickMoveTrigger(NotificationQuickMoveTrigger.ALWAYS);
        Account account = createFakeAccount();
        setArchiveFolder(account, "Archive");
        notifications.setMoveCapable(account, true);
        Builder builder = createFakeNotificationBuilder();
        NotificationContent notificationContent = createNotificationContent();
        int notificationId = 23;

        notifications.addQuickMoveAction(account, builder, notificationContent, notificationId);

        verify(builder).addAction(resourceProvider.getIconArchive(), "Archive", null);
    }

    @Test
    public void testAddQuickMoveAction_NotificationQuickMoveType_ARCHIVE_archiveActionNotAvailable() {
        K9.setNotificationQuickMoveType(NotificationQuickMoveType.ARCHIVE);
        K9.setNotificationQuickMoveTrigger(NotificationQuickMoveTrigger.FOR_SINGLE_MSG);
        Account account = createFakeAccount();
        setArchiveFolder(account, K9.FOLDER_NONE);
        Builder builder = createFakeNotificationBuilder();
        NotificationContent notificationContent = createNotificationContent();
        int notificationId = 23;

        notifications.addQuickMoveAction(account, builder, notificationContent, notificationId);

        verifyZeroInteractions(builder);
    }

    @Test
    public void testAddQuickMoveAction_quickMoveNotEnabled() {
        K9.setNotificationQuickMoveType(NotificationQuickMoveType.DELETE);
        K9.setNotificationQuickMoveTrigger(NotificationQuickMoveTrigger.NEVER);
        Account account = createFakeAccount();
        Builder builder = createFakeNotificationBuilder();
        NotificationContent notificationContent = createNotificationContent();
        int notificationId = 23;

        notifications.addQuickMoveAction(account, builder, notificationContent, notificationId);

        verifyZeroInteractions(builder);
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
        NotificationHelper notificationHelper = createFakeNotificationHelper();
        NotificationActionCreator actionCreator = mock(NotificationActionCreator.class);

        return new TestNotifications(notificationHelper, actionCreator, resourceProvider);
    }

    private NotificationHelper createFakeNotificationHelper() {
        Builder builder = MockHelper.mockBuilder(Builder.class);
        NotificationHelper notificationHelper = mock(NotificationHelper.class);
        when(notificationHelper.createNotificationBuilder()).thenReturn(builder);
        when(notificationHelper.getAccountName(any(Account.class))).thenReturn(ACCOUNT_NAME);
        return notificationHelper;
    }

    private Account createFakeAccount() {
        Account account = mock(Account.class);
        when(account.getAccountNumber()).thenReturn(ACCOUNT_NUMBER);
        when(account.getChipColor()).thenReturn(ACCOUNT_COLOR);
        return account;
    }

    private void setArchiveFolder(Account account, String folderName) {
        when(account.getArchiveFolder()).thenReturn(folderName);
    }

    private Builder createFakeNotificationBuilder() {
        Builder builder = mockBuilder(Builder.class);
        when(builder.build()).thenReturn(FAKE_NOTIFICATION);
        return builder;
    }

    private NotificationContent createNotificationContent() {
        return new NotificationContent(createMessageReference(), null, null, null, null, false);
    }

    private MessageReference createMessageReference() {
        return new MessageReference("account", "folder", "1234", null);
    }

    static class TestNotifications extends BaseNotifications {

        BigTextStyle bigTextStyle;
        MessagingController messagingController;

        protected TestNotifications(NotificationHelper notificationHelper, NotificationActionCreator actionCreator,
                NotificationResourceProvider resourceProvider) {
            super(notificationHelper, actionCreator, resourceProvider);
            bigTextStyle = mock(BigTextStyle.class);
            messagingController = mock(MessagingController.class);
        }

        @Override
        protected BigTextStyle createBigTextStyle(Builder builder) {
            return bigTextStyle;
        }

        @Override
        MessagingController createMessagingController() { return messagingController; }

        private void setMoveCapable(Account account, boolean moveCapable) {
            when(messagingController.isMoveCapable(account)).thenReturn(moveCapable);
        }
    }
}
