package com.fsck.k9.notification;


import android.app.Notification;
import android.support.v4.app.NotificationManagerCompat;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9.NotificationHideSubject;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mailstore.LocalMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 21)
public class NewMailNotificationsTest {
    private static final int ACCOUNT_NUMBER = 23;

    private Account account;
    private TestNewMailNotifications newMailNotifications;
    private NotificationManagerCompat notificationManager;
    private NotificationContentCreator contentCreator;
    private DeviceNotifications deviceNotifications;
    private WearNotifications wearNotifications;


    @Before
    public void setUp() throws Exception {
        account = createAccount();

        notificationManager = createNotificationManager();
        NotificationController controller = createNotificationController(notificationManager);
        contentCreator = createNotificationContentCreator();
        deviceNotifications = createDeviceNotifications();
        wearNotifications = createWearNotifications();

        newMailNotifications = new TestNewMailNotifications(controller, contentCreator, deviceNotifications,
                wearNotifications);
    }

    @Test
    public void testAddNewMailNotification() throws Exception {
        int notificationOffset = 1;
        LocalMessage message = createLocalMessage();
        NotificationContent content = createNotificationContent();
        NotificationHolder holder = createNotificationHolder(content, notificationOffset);
        addToNotificationContentCreator(message, content);
        whenAddingContentReturn(content, AddNotificationResult.newNotification(holder));
        Notification wearNotification = createNotification();
        Notification summaryNotification = createNotification();
        addToWearNotifications(holder, wearNotification);
        addToDeviceNotifications(summaryNotification);

        newMailNotifications.addNewMailNotification(account, message, 42);

        int wearNotificationId = NotificationIds.getNewMailNotificationId(account, notificationOffset);
        int summaryNotificationId = NotificationIds.getNewMailNotificationId(account);
        verify(notificationManager).notify(wearNotificationId, wearNotification);
        verify(notificationManager).notify(summaryNotificationId, summaryNotification);
    }

    @Test
    public void testAddNewMailNotificationWithCancelingExistingNotification() throws Exception {
        int notificationOffset = 1;
        LocalMessage message = createLocalMessage();
        NotificationContent content = createNotificationContent();
        NotificationHolder holder = createNotificationHolder(content, notificationOffset);
        addToNotificationContentCreator(message, content);
        whenAddingContentReturn(content, AddNotificationResult.replaceNotification(holder));
        Notification wearNotification = createNotification();
        Notification summaryNotification = createNotification();
        addToWearNotifications(holder, wearNotification);
        addToDeviceNotifications(summaryNotification);

        newMailNotifications.addNewMailNotification(account, message, 42);

        int wearNotificationId = NotificationIds.getNewMailNotificationId(account, notificationOffset);
        int summaryNotificationId = NotificationIds.getNewMailNotificationId(account);
        verify(notificationManager).notify(wearNotificationId, wearNotification);
        verify(notificationManager).cancel(wearNotificationId);
        verify(notificationManager).notify(summaryNotificationId, summaryNotification);
    }

    @Test
    public void testAddNewMailNotificationWithPrivacyModeEnabled() throws Exception {
        enablePrivacyMode();
        int notificationOffset = 1;
        LocalMessage message = createLocalMessage();
        NotificationContent content = createNotificationContent();
        NotificationHolder holder = createNotificationHolder(content, notificationOffset);
        addToNotificationContentCreator(message, content);
        whenAddingContentReturn(content, AddNotificationResult.newNotification(holder));
        Notification wearNotification = createNotification();
        addToDeviceNotifications(wearNotification);

        newMailNotifications.addNewMailNotification(account, message, 42);

        int wearNotificationId = NotificationIds.getNewMailNotificationId(account, notificationOffset);
        int summaryNotificationId = NotificationIds.getNewMailNotificationId(account);
        verify(notificationManager, never()).notify(eq(wearNotificationId), any(Notification.class));
        verify(notificationManager).notify(summaryNotificationId, wearNotification);
    }

    @Test
    public void testAddNewMailNotificationTwice() throws Exception {
        int notificationOffsetOne = 1;
        int notificationOffsetTwo = 2;
        LocalMessage messageOne = createLocalMessage();
        LocalMessage messageTwo = createLocalMessage();
        NotificationContent contentOne = createNotificationContent();
        NotificationContent contentTwo = createNotificationContent();
        NotificationHolder holderOne = createNotificationHolder(contentOne, notificationOffsetOne);
        NotificationHolder holderTwo = createNotificationHolder(contentTwo, notificationOffsetTwo);
        addToNotificationContentCreator(messageOne, contentOne);
        addToNotificationContentCreator(messageTwo, contentTwo);
        whenAddingContentReturn(contentOne, AddNotificationResult.newNotification(holderOne));
        whenAddingContentReturn(contentTwo, AddNotificationResult.newNotification(holderTwo));
        Notification wearNotificationOne = createNotification();
        Notification wearNotificationTwo = createNotification();
        Notification summaryNotification = createNotification();
        addToWearNotifications(holderOne, wearNotificationOne);
        addToWearNotifications(holderTwo, wearNotificationTwo);
        addToDeviceNotifications(summaryNotification);

        newMailNotifications.addNewMailNotification(account, messageOne, 42);
        newMailNotifications.addNewMailNotification(account, messageTwo, 42);

        int wearNotificationIdOne = NotificationIds.getNewMailNotificationId(account, notificationOffsetOne);
        int wearNotificationIdTwo = NotificationIds.getNewMailNotificationId(account, notificationOffsetTwo);
        int summaryNotificationId = NotificationIds.getNewMailNotificationId(account);
        verify(notificationManager).notify(wearNotificationIdOne, wearNotificationOne);
        verify(notificationManager).notify(wearNotificationIdTwo, wearNotificationTwo);
        verify(notificationManager, times(2)).notify(summaryNotificationId, summaryNotification);
    }

    @Test
    public void testRemoveNewMailNotificationWithoutNotificationData() throws Exception {
        MessageReference messageReference = createMessageReference(1);

        newMailNotifications.removeNewMailNotification(account, messageReference);

        verify(notificationManager, never()).cancel(anyInt());
    }

    @Test
    public void testRemoveNewMailNotificationWithUnknownMessageReference() throws Exception {
        enablePrivacyMode();
        MessageReference messageReference = createMessageReference(1);
        int notificationOffset = 1;
        LocalMessage message = createLocalMessage();
        NotificationContent content = createNotificationContent();
        NotificationHolder holder = createNotificationHolder(content, notificationOffset);
        addToNotificationContentCreator(message, content);
        whenAddingContentReturn(content, AddNotificationResult.newNotification(holder));
        Notification summaryNotification = createNotification();
        addToDeviceNotifications(summaryNotification);
        newMailNotifications.addNewMailNotification(account, message, 23);
        whenRemovingContentReturn(messageReference, RemoveNotificationResult.unknownNotification());

        newMailNotifications.removeNewMailNotification(account, messageReference);

        verify(notificationManager, never()).cancel(anyInt());
    }

    @Test
    public void testRemoveNewMailNotification() throws Exception {
        enablePrivacyMode();
        MessageReference messageReference = createMessageReference(1);
        int notificationOffset = 1;
        int notificationId = NotificationIds.getNewMailNotificationId(account, notificationOffset);
        LocalMessage message = createLocalMessage();
        NotificationContent content = createNotificationContent();
        NotificationHolder holder = createNotificationHolder(content, notificationOffset);
        addToNotificationContentCreator(message, content);
        whenAddingContentReturn(content, AddNotificationResult.newNotification(holder));
        Notification summaryNotification = createNotification();
        addToDeviceNotifications(summaryNotification);
        newMailNotifications.addNewMailNotification(account, message, 23);
        whenRemovingContentReturn(messageReference, RemoveNotificationResult.cancelNotification(notificationId));

        newMailNotifications.removeNewMailNotification(account, messageReference);

        int summaryNotificationId = NotificationIds.getNewMailNotificationId(account);
        verify(notificationManager).cancel(notificationId);
        verify(notificationManager, times(2)).notify(summaryNotificationId, summaryNotification);
    }

    @Test
    public void testRemoveNewMailNotificationClearingAllNotifications() throws Exception {
        MessageReference messageReference = createMessageReference(1);
        int notificationOffset = 1;
        int notificationId = NotificationIds.getNewMailNotificationId(account, notificationOffset);
        LocalMessage message = createLocalMessage();
        NotificationContent content = createNotificationContent();
        NotificationHolder holder = createNotificationHolder(content, notificationOffset);
        addToNotificationContentCreator(message, content);
        whenAddingContentReturn(content, AddNotificationResult.newNotification(holder));
        Notification summaryNotification = createNotification();
        addToDeviceNotifications(summaryNotification);
        newMailNotifications.addNewMailNotification(account, message, 23);
        whenRemovingContentReturn(messageReference, RemoveNotificationResult.cancelNotification(notificationId));
        when(newMailNotifications.notificationData.getNewMessagesCount()).thenReturn(0);
        setActiveNotificationIds();

        newMailNotifications.removeNewMailNotification(account, messageReference);

        int summaryNotificationId = NotificationIds.getNewMailNotificationId(account);
        verify(notificationManager).cancel(notificationId);
        verify(notificationManager).cancel(summaryNotificationId);
    }

    @Test
    public void testRemoveNewMailNotificationWithCreateNotification() throws Exception {
        MessageReference messageReference = createMessageReference(1);
        int notificationOffset = 1;
        int notificationId = NotificationIds.getNewMailNotificationId(account, notificationOffset);
        LocalMessage message = createLocalMessage();
        NotificationContent contentOne = createNotificationContent();
        NotificationContent contentTwo = createNotificationContent();
        NotificationHolder holderOne = createNotificationHolder(contentOne, notificationOffset);
        NotificationHolder holderTwo = createNotificationHolder(contentTwo, notificationOffset);
        addToNotificationContentCreator(message, contentOne);
        whenAddingContentReturn(contentOne, AddNotificationResult.newNotification(holderOne));
        Notification summaryNotification = createNotification();
        addToDeviceNotifications(summaryNotification);
        Notification wearNotificationOne = createNotification();
        Notification wearNotificationTwo = createNotification();
        addToWearNotifications(holderOne, wearNotificationOne);
        addToWearNotifications(holderTwo, wearNotificationTwo);
        newMailNotifications.addNewMailNotification(account, message, 23);
        whenRemovingContentReturn(messageReference, RemoveNotificationResult.createNotification(holderTwo));

        newMailNotifications.removeNewMailNotification(account, messageReference);

        int summaryNotificationId = NotificationIds.getNewMailNotificationId(account);
        verify(notificationManager).cancel(notificationId);
        verify(notificationManager).notify(notificationId, wearNotificationTwo);
        verify(notificationManager, times(2)).notify(summaryNotificationId, summaryNotification);
    }

    @Test
    public void testClearNewMailNotificationsWithoutNotificationData() throws Exception {
        newMailNotifications.clearNewMailNotifications(account);

        verify(notificationManager, never()).cancel(anyInt());
    }

    @Test
    public void testClearNewMailNotifications() throws Exception {
        int notificationOffset = 1;
        int notificationId = NotificationIds.getNewMailNotificationId(account, notificationOffset);
        LocalMessage message = createLocalMessage();
        NotificationContent content = createNotificationContent();
        NotificationHolder holder = createNotificationHolder(content, notificationOffset);
        addToNotificationContentCreator(message, content);
        setActiveNotificationIds(notificationId);
        whenAddingContentReturn(content, AddNotificationResult.newNotification(holder));
        newMailNotifications.addNewMailNotification(account, message, 3);

        newMailNotifications.clearNewMailNotifications(account);

        verify(notificationManager).cancel(notificationId);
        verify(notificationManager).cancel(NotificationIds.getNewMailNotificationId(account));
    }

    private Account createAccount() {
        Account account = mock(Account.class);
        when(account.getAccountNumber()).thenReturn(ACCOUNT_NUMBER);
        return account;
    }

    private LocalMessage createLocalMessage() {
        return mock(LocalMessage.class);
    }

    private NotificationContent createNotificationContent() {
        return new NotificationContent(null, null, null, null, null, false);
    }

    private NotificationHolder createNotificationHolder(NotificationContent content, int offset) {
        int notificationId = NotificationIds.getNewMailNotificationId(account, offset);
        return new NotificationHolder(notificationId, content);
    }

    private NotificationManagerCompat createNotificationManager() {
        return mock(NotificationManagerCompat.class);
    }

    private NotificationController createNotificationController(NotificationManagerCompat notificationManager) {
        NotificationController controller = mock(NotificationController.class);
        when(controller.getNotificationManager()).thenReturn(notificationManager);
        return controller;
    }

    private NotificationContentCreator createNotificationContentCreator() {
        return mock(NotificationContentCreator.class);
    }

    private void addToNotificationContentCreator(LocalMessage message, NotificationContent content) {
        when(contentCreator.createFromMessage(account, message)).thenReturn(content);
    }

    private DeviceNotifications createDeviceNotifications() {
        return mock(DeviceNotifications.class);
    }

    private void addToDeviceNotifications(Notification notificationToReturn) {
        when(deviceNotifications.buildSummaryNotification(
                        eq(account), eq(newMailNotifications.notificationData), anyBoolean())
        ).thenReturn(notificationToReturn);
    }

    private Notification createNotification() {
        return mock(Notification.class);
    }

    private WearNotifications createWearNotifications() {
        return mock(WearNotifications.class);
    }

    private MessageReference createMessageReference(int number) {
        return new MessageReference(null, null, String.valueOf(number), null);
    }

    private void addToWearNotifications(NotificationHolder notificationHolder, Notification notificationToReturn) {
        when(wearNotifications.buildStackedNotification(account, notificationHolder)).thenReturn(notificationToReturn);
    }

    private void whenAddingContentReturn(NotificationContent content, AddNotificationResult result) {
        NotificationData notificationData = newMailNotifications.notificationData;
        when(notificationData.addNotificationContent(content)).thenReturn(result);

        int newCount = notificationData.getNewMessagesCount() + 1;
        when(notificationData.getNewMessagesCount()).thenReturn(newCount);
    }

    private void whenRemovingContentReturn(MessageReference messageReference, RemoveNotificationResult result) {
        NotificationData notificationData = newMailNotifications.notificationData;
        when(notificationData.removeNotificationForMessage(messageReference)).thenReturn(result);
    }

    private void setActiveNotificationIds(int... notificationIds) {
        NotificationData notificationData = newMailNotifications.notificationData;
        when(notificationData.getActiveNotificationIds()).thenReturn(notificationIds);
    }

    private void enablePrivacyMode() {
        K9.setNotificationHideSubject(NotificationHideSubject.ALWAYS);
    }

    static class TestNewMailNotifications extends NewMailNotifications {

        public final NotificationData notificationData;

        TestNewMailNotifications(NotificationController controller, NotificationContentCreator contentCreator,
                DeviceNotifications deviceNotifications, WearNotifications wearNotifications) {
            super(controller, contentCreator, deviceNotifications, wearNotifications);
            notificationData = mock(NotificationData.class);
        }

        @Override
        NotificationData createNotificationData(Account account, int unreadMessageCount) {
            return notificationData;
        }
    }
}
