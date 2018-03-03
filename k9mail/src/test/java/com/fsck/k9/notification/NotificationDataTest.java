package com.fsck.k9.notification;


import java.util.List;

import com.fsck.k9.Account;
import com.fsck.k9.activity.MessageReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
public class NotificationDataTest {
    private static final String ACCOUNT_UUID = "1-2-3";
    private static final int ACCOUNT_NUMBER = 23;
    private static final String FOLDER_NAME = "INBOX";


    private NotificationData notificationData;
    private Account account;


    @Before
    public void setUp() throws Exception {
        account = createFakeAccount();
        notificationData = new NotificationData(account);
    }

    @Test
    public void testAddNotificationContent() throws Exception {
        NotificationContent content = createNotificationContent("1");

        AddNotificationResult result = notificationData.addNotificationContent(content);

        assertFalse(result.shouldCancelNotification());
        NotificationHolder holder = result.getNotificationHolder();
        assertNotNull(holder);
        assertEquals(NotificationIds.getNewMailStackedNotificationId(account, 0), holder.notificationId);
        assertEquals(content, holder.content);
    }

    @Test
    public void testAddNotificationContentWithReplacingNotification() throws Exception {
        notificationData.addNotificationContent(createNotificationContent("1"));
        notificationData.addNotificationContent(createNotificationContent("2"));
        notificationData.addNotificationContent(createNotificationContent("3"));
        notificationData.addNotificationContent(createNotificationContent("4"));
        notificationData.addNotificationContent(createNotificationContent("5"));
        notificationData.addNotificationContent(createNotificationContent("6"));
        notificationData.addNotificationContent(createNotificationContent("7"));
        notificationData.addNotificationContent(createNotificationContent("8"));

        AddNotificationResult result = notificationData.addNotificationContent(createNotificationContent("9"));

        assertTrue(result.shouldCancelNotification());
        assertEquals(NotificationIds.getNewMailStackedNotificationId(account, 0), result.getNotificationId());
    }

    @Test
    public void testRemoveNotificationForMessage() throws Exception {
        NotificationContent content = createNotificationContent("1");
        notificationData.addNotificationContent(content);

        RemoveNotificationResult result = notificationData.removeNotificationForMessage(content.messageReference);

        assertFalse(result.isUnknownNotification());
        assertEquals(NotificationIds.getNewMailStackedNotificationId(account, 0), result.getNotificationId());
        assertFalse(result.shouldCreateNotification());
    }

    @Test
    public void testRemoveNotificationForMessageWithRecreatingNotification() throws Exception {
        notificationData.addNotificationContent(createNotificationContent("1"));
        NotificationContent content = createNotificationContent("2");
        notificationData.addNotificationContent(content);
        notificationData.addNotificationContent(createNotificationContent("3"));
        notificationData.addNotificationContent(createNotificationContent("4"));
        notificationData.addNotificationContent(createNotificationContent("5"));
        notificationData.addNotificationContent(createNotificationContent("6"));
        notificationData.addNotificationContent(createNotificationContent("7"));
        notificationData.addNotificationContent(createNotificationContent("8"));
        notificationData.addNotificationContent(createNotificationContent("9"));
        NotificationContent latestContent = createNotificationContent("10");
        notificationData.addNotificationContent(latestContent);

        RemoveNotificationResult result =
                notificationData.removeNotificationForMessage(latestContent.messageReference);

        assertFalse(result.isUnknownNotification());
        assertEquals(NotificationIds.getNewMailStackedNotificationId(account, 1), result.getNotificationId());
        assertTrue(result.shouldCreateNotification());
        NotificationHolder holder = result.getNotificationHolder();
        assertNotNull(holder);
        assertEquals(NotificationIds.getNewMailStackedNotificationId(account, 1), holder.notificationId);
        assertEquals(content, holder.content);
    }

    @Test
    public void testRemoveDoesNotLeakNotificationIds() {
        for (int i = 1; i <= NotificationData.MAX_NUMBER_OF_STACKED_NOTIFICATIONS + 1; i++) {
            NotificationContent content = createNotificationContent("" + i);
            notificationData.addNotificationContent(content);
            notificationData.removeNotificationForMessage(content.messageReference);
        }
    }

    @Test
    public void testNewMessagesCount() throws Exception {
        assertEquals(0, notificationData.getNewMessagesCount());

        NotificationContent contentOne = createNotificationContent("1");
        notificationData.addNotificationContent(contentOne);
        assertEquals(1, notificationData.getNewMessagesCount());

        NotificationContent contentTwo = createNotificationContent("2");
        notificationData.addNotificationContent(contentTwo);
        assertEquals(2, notificationData.getNewMessagesCount());
    }

    @Test
    public void testUnreadMessagesCount() throws Exception {
        notificationData.setUnreadMessageCount(42);
        assertEquals(42, notificationData.getUnreadMessageCount());

        NotificationContent content = createNotificationContent("1");
        notificationData.addNotificationContent(content);
        assertEquals(43, notificationData.getUnreadMessageCount());

        NotificationContent contentTwo = createNotificationContent("2");
        notificationData.addNotificationContent(contentTwo);
        assertEquals(44, notificationData.getUnreadMessageCount());
    }

    @Test
    public void testContainsStarredMessages() throws Exception {
        assertFalse(notificationData.containsStarredMessages());

        notificationData.addNotificationContent(createNotificationContentForStarredMessage());

        assertTrue(notificationData.containsStarredMessages());
    }

    @Test
    public void testContainsStarredMessagesWithAdditionalMessages() throws Exception {
        notificationData.addNotificationContent(createNotificationContent("1"));
        notificationData.addNotificationContent(createNotificationContent("2"));
        notificationData.addNotificationContent(createNotificationContent("3"));
        notificationData.addNotificationContent(createNotificationContent("4"));
        notificationData.addNotificationContent(createNotificationContent("5"));
        notificationData.addNotificationContent(createNotificationContent("6"));
        notificationData.addNotificationContent(createNotificationContent("7"));
        notificationData.addNotificationContent(createNotificationContent("8"));

        assertFalse(notificationData.containsStarredMessages());

        notificationData.addNotificationContent(createNotificationContentForStarredMessage());

        assertTrue(notificationData.containsStarredMessages());
    }

    @Test
    public void testIsSingleMessageNotification() throws Exception {
        assertFalse(notificationData.isSingleMessageNotification());

        notificationData.addNotificationContent(createNotificationContent("1"));
        assertTrue(notificationData.isSingleMessageNotification());

        notificationData.addNotificationContent(createNotificationContent("2"));
        assertFalse(notificationData.isSingleMessageNotification());
    }

    @Test
    public void testGetHolderForLatestNotification() throws Exception {
        NotificationContent content = createNotificationContent("1");
        AddNotificationResult addResult = notificationData.addNotificationContent(content);

        NotificationHolder holder = notificationData.getHolderForLatestNotification();

        assertEquals(addResult.getNotificationHolder(), holder);
    }

    @Test
    public void testGetContentForSummaryNotification() throws Exception {
        notificationData.addNotificationContent(createNotificationContent("1"));
        NotificationContent content4 = createNotificationContent("2");
        notificationData.addNotificationContent(content4);
        NotificationContent content3 = createNotificationContent("3");
        notificationData.addNotificationContent(content3);
        NotificationContent content2 = createNotificationContent("4");
        notificationData.addNotificationContent(content2);
        NotificationContent content1 = createNotificationContent("5");
        notificationData.addNotificationContent(content1);
        NotificationContent content0 = createNotificationContent("6");
        notificationData.addNotificationContent(content0);

        List<NotificationContent> contents = notificationData.getContentForSummaryNotification();

        assertEquals(5, contents.size());
        assertEquals(content0, contents.get(0));
        assertEquals(content1, contents.get(1));
        assertEquals(content2, contents.get(2));
        assertEquals(content3, contents.get(3));
        assertEquals(content4, contents.get(4));
    }

    @Test
    public void testGetActiveNotificationIds() throws Exception {
        notificationData.addNotificationContent(createNotificationContent("1"));
        notificationData.addNotificationContent(createNotificationContent("2"));

        int[] notificationIds = notificationData.getActiveNotificationIds();

        assertEquals(2, notificationIds.length);
        assertEquals(NotificationIds.getNewMailStackedNotificationId(account, 1), notificationIds[0]);
        assertEquals(NotificationIds.getNewMailStackedNotificationId(account, 0), notificationIds[1]);
    }

    @Test
    public void testGetAccount() throws Exception {
        assertEquals(account, notificationData.getAccount());
    }

    @Test
    public void testGetAllMessageReferences() throws Exception {
        MessageReference messageReference0 = createMessageReference("1");
        MessageReference messageReference1 = createMessageReference("2");
        MessageReference messageReference2 = createMessageReference("3");
        MessageReference messageReference3 = createMessageReference("4");
        MessageReference messageReference4 = createMessageReference("5");
        MessageReference messageReference5 = createMessageReference("6");
        MessageReference messageReference6 = createMessageReference("7");
        MessageReference messageReference7 = createMessageReference("8");
        MessageReference messageReference8 = createMessageReference("9");
        notificationData.addNotificationContent(createNotificationContent(messageReference8));
        notificationData.addNotificationContent(createNotificationContent(messageReference7));
        notificationData.addNotificationContent(createNotificationContent(messageReference6));
        notificationData.addNotificationContent(createNotificationContent(messageReference5));
        notificationData.addNotificationContent(createNotificationContent(messageReference4));
        notificationData.addNotificationContent(createNotificationContent(messageReference3));
        notificationData.addNotificationContent(createNotificationContent(messageReference2));
        notificationData.addNotificationContent(createNotificationContent(messageReference1));
        notificationData.addNotificationContent(createNotificationContent(messageReference0));

        List<MessageReference> messageReferences = notificationData.getAllMessageReferences();

        assertEquals(9, messageReferences.size());
        assertEquals(messageReference0, messageReferences.get(0));
        assertEquals(messageReference1, messageReferences.get(1));
        assertEquals(messageReference2, messageReferences.get(2));
        assertEquals(messageReference3, messageReferences.get(3));
        assertEquals(messageReference4, messageReferences.get(4));
        assertEquals(messageReference5, messageReferences.get(5));
        assertEquals(messageReference6, messageReferences.get(6));
        assertEquals(messageReference7, messageReferences.get(7));
        assertEquals(messageReference8, messageReferences.get(8));
    }

    @Test
    public void testOverflowNotifications() {
        MessageReference messageReference0 = createMessageReference("1");
        MessageReference messageReference1 = createMessageReference("2");
        MessageReference messageReference2 = createMessageReference("3");
        MessageReference messageReference3 = createMessageReference("4");
        MessageReference messageReference4 = createMessageReference("5");
        MessageReference messageReference5 = createMessageReference("6");
        MessageReference messageReference6 = createMessageReference("7");
        MessageReference messageReference7 = createMessageReference("8");
        MessageReference messageReference8 = createMessageReference("9");
        
        notificationData.addNotificationContent(createNotificationContent(messageReference8));
        notificationData.addNotificationContent(createNotificationContent(messageReference7));
        notificationData.addNotificationContent(createNotificationContent(messageReference6));
        notificationData.addNotificationContent(createNotificationContent(messageReference5));
        notificationData.addNotificationContent(createNotificationContent(messageReference4));
        notificationData.addNotificationContent(createNotificationContent(messageReference3));
        notificationData.addNotificationContent(createNotificationContent(messageReference2));
        notificationData.addNotificationContent(createNotificationContent(messageReference1));
        notificationData.addNotificationContent(createNotificationContent(messageReference0));

        assertTrue(notificationData.hasSummaryOverflowMessages());
        assertEquals(4, notificationData.getSummaryOverflowMessagesCount());
    }

    private Account createFakeAccount() {
        Account account = mock(Account.class);
        when(account.getAccountNumber()).thenReturn(ACCOUNT_NUMBER);
        return account;
    }

    private MessageReference createMessageReference(String uid) {
        return new MessageReference(ACCOUNT_UUID, FOLDER_NAME, uid, null);
    }

    private NotificationContent createNotificationContent(String uid) {
        MessageReference messageReference = createMessageReference(uid);
        return createNotificationContent(messageReference);
    }

    private NotificationContent createNotificationContent(MessageReference messageReference) {
        return new NotificationContent(messageReference, "", "", "", "", false);
    }

    private NotificationContent createNotificationContentForStarredMessage() {
        MessageReference messageReference = createMessageReference("42");
        return new NotificationContent(messageReference, "", "", "", "", true);
    }
}
