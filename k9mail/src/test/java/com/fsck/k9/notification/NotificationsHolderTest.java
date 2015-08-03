package com.fsck.k9.notification;


import java.util.List;

import com.fsck.k9.Account;
import com.fsck.k9.activity.MessageReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class NotificationsHolderTest {
    private static final String ACCOUNT_UUID = "1-2-3";
    private static final int ACCOUNT_NUMBER = 23;
    private static final String FOLDER_NAME = "INBOX";


    private NotificationsHolder notificationsHolder;
    private Account account;


    @Before
    public void setUp() throws Exception {
        account = createFakeAccount();
        notificationsHolder = new NotificationsHolder(account);
    }

    @Test
    public void testAddNotificationContent() throws Exception {
        NotificationContent content = createNotificationContent("1");

        AddNotificationResult result = notificationsHolder.addNotificationContent(content);

        assertFalse(result.shouldCancelNotification());
        NotificationHolder holder = result.getNotificationHolder();
        assertNotNull(holder);
        assertEquals(1023, holder.notificationId);
        assertEquals(content, holder.content);
    }

    @Test
    public void testAddNotificationContentWithReplacingNotification() throws Exception {
        NotificationContent content = createNotificationContent("1");
        notificationsHolder.addNotificationContent(content);
        notificationsHolder.addNotificationContent(createNotificationContent("2"));
        notificationsHolder.addNotificationContent(createNotificationContent("3"));
        notificationsHolder.addNotificationContent(createNotificationContent("4"));
        notificationsHolder.addNotificationContent(createNotificationContent("5"));
        notificationsHolder.addNotificationContent(createNotificationContent("6"));
        notificationsHolder.addNotificationContent(createNotificationContent("7"));
        notificationsHolder.addNotificationContent(createNotificationContent("8"));

        AddNotificationResult result = notificationsHolder.addNotificationContent(createNotificationContent("9"));

        assertTrue(result.shouldCancelNotification());
        assertEquals(1023, result.getNotificationId());
    }

    @Test
    public void testRemoveNotificationForMessage() throws Exception {
        NotificationContent content = createNotificationContent("1");
        notificationsHolder.addNotificationContent(content);

        RemoveNotificationResult result = notificationsHolder.removeNotificationForMessage(content.messageReference);

        assertFalse(result.isUnknownNotification());
        assertEquals(1023, result.getNotificationId());
        assertFalse(result.shouldCreateNotification());
    }

    @Test
    public void testRemoveNotificationForMessageWithRecreatingNotification() throws Exception {
        notificationsHolder.addNotificationContent(createNotificationContent("1"));
        NotificationContent content = createNotificationContent("2");
        notificationsHolder.addNotificationContent(content);
        notificationsHolder.addNotificationContent(createNotificationContent("3"));
        notificationsHolder.addNotificationContent(createNotificationContent("4"));
        notificationsHolder.addNotificationContent(createNotificationContent("5"));
        notificationsHolder.addNotificationContent(createNotificationContent("6"));
        notificationsHolder.addNotificationContent(createNotificationContent("7"));
        notificationsHolder.addNotificationContent(createNotificationContent("8"));
        notificationsHolder.addNotificationContent(createNotificationContent("9"));
        NotificationContent latestContent = createNotificationContent("10");
        notificationsHolder.addNotificationContent(latestContent);

        RemoveNotificationResult result =
                notificationsHolder.removeNotificationForMessage(latestContent.messageReference);

        assertFalse(result.isUnknownNotification());
        assertEquals(2023, result.getNotificationId());
        assertTrue(result.shouldCreateNotification());
        NotificationHolder holder = result.getNotificationHolder();
        assertNotNull(holder);
        assertEquals(2023, holder.notificationId);
        assertEquals(content, holder.content);
    }

    @Test
    public void testNewMessagesCount() throws Exception {
        assertEquals(0, notificationsHolder.getNewMessagesCount());

        NotificationContent contentOne = createNotificationContent("1");
        notificationsHolder.addNotificationContent(contentOne);
        assertEquals(1, notificationsHolder.getNewMessagesCount());

        NotificationContent contentTwo = createNotificationContent("2");
        notificationsHolder.addNotificationContent(contentTwo);
        assertEquals(2, notificationsHolder.getNewMessagesCount());
    }

    @Test
    public void testUnreadMessagesCount() throws Exception {
        notificationsHolder.setUnreadMessageCount(42);
        assertEquals(42, notificationsHolder.getUnreadMessageCount());

        NotificationContent content = createNotificationContent("1");
        notificationsHolder.addNotificationContent(content);
        assertEquals(43, notificationsHolder.getUnreadMessageCount());

        NotificationContent contentTwo = createNotificationContent("2");
        notificationsHolder.addNotificationContent(contentTwo);
        assertEquals(44, notificationsHolder.getUnreadMessageCount());
    }

    @Test
    public void testContainsStarredMessages() throws Exception {
        assertFalse(notificationsHolder.containsStarredMessages());

        notificationsHolder.addNotificationContent(createNotificationContentForStarredMessage());

        assertTrue(notificationsHolder.containsStarredMessages());
    }

    @Test
    public void testContainsStarredMessagesWithAdditionalMessages() throws Exception {
        notificationsHolder.addNotificationContent(createNotificationContent("1"));
        notificationsHolder.addNotificationContent(createNotificationContent("2"));
        notificationsHolder.addNotificationContent(createNotificationContent("3"));
        notificationsHolder.addNotificationContent(createNotificationContent("4"));
        notificationsHolder.addNotificationContent(createNotificationContent("5"));
        notificationsHolder.addNotificationContent(createNotificationContent("6"));
        notificationsHolder.addNotificationContent(createNotificationContent("7"));
        notificationsHolder.addNotificationContent(createNotificationContent("8"));

        assertFalse(notificationsHolder.containsStarredMessages());

        notificationsHolder.addNotificationContent(createNotificationContentForStarredMessage());

        assertTrue(notificationsHolder.containsStarredMessages());
    }

    @Test
    public void testIsSingleMessageNotification() throws Exception {
        assertFalse(notificationsHolder.isSingleMessageNotification());

        notificationsHolder.addNotificationContent(createNotificationContent("1"));
        assertTrue(notificationsHolder.isSingleMessageNotification());

        notificationsHolder.addNotificationContent(createNotificationContent("2"));
        assertFalse(notificationsHolder.isSingleMessageNotification());
    }

    @Test
    public void testGetHolderForLatestNotification() throws Exception {
        NotificationContent content = createNotificationContent("1");
        AddNotificationResult addResult = notificationsHolder.addNotificationContent(content);

        NotificationHolder holder = notificationsHolder.getHolderForLatestNotification();

        assertEquals(addResult.getNotificationHolder(), holder);
    }

    @Test
    public void testGetContentForSummaryNotification() throws Exception {
        notificationsHolder.addNotificationContent(createNotificationContent("1"));
        NotificationContent content4 = createNotificationContent("2");
        notificationsHolder.addNotificationContent(content4);
        NotificationContent content3 = createNotificationContent("3");
        notificationsHolder.addNotificationContent(content3);
        NotificationContent content2 = createNotificationContent("4");
        notificationsHolder.addNotificationContent(content2);
        NotificationContent content1 = createNotificationContent("5");
        notificationsHolder.addNotificationContent(content1);
        NotificationContent content0 = createNotificationContent("6");
        notificationsHolder.addNotificationContent(content0);

        List<NotificationContent> contents = notificationsHolder.getContentForSummaryNotification();

        assertEquals(5, contents.size());
        assertEquals(content0, contents.get(0));
        assertEquals(content1, contents.get(1));
        assertEquals(content2, contents.get(2));
        assertEquals(content3, contents.get(3));
        assertEquals(content4, contents.get(4));
    }

    @Test
    public void testGetActiveNotificationIds() throws Exception {
        notificationsHolder.addNotificationContent(createNotificationContent("1"));
        notificationsHolder.addNotificationContent(createNotificationContent("2"));

        int[] notificationIds = notificationsHolder.getActiveNotificationIds();

        assertEquals(2, notificationIds.length);
        assertEquals(2023, notificationIds[0]);
        assertEquals(1023, notificationIds[1]);
    }

    @Test
    public void testGetAccount() throws Exception {
        assertEquals(account, notificationsHolder.getAccount());
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
        notificationsHolder.addNotificationContent(createNotificationContent(messageReference8));
        notificationsHolder.addNotificationContent(createNotificationContent(messageReference7));
        notificationsHolder.addNotificationContent(createNotificationContent(messageReference6));
        notificationsHolder.addNotificationContent(createNotificationContent(messageReference5));
        notificationsHolder.addNotificationContent(createNotificationContent(messageReference4));
        notificationsHolder.addNotificationContent(createNotificationContent(messageReference3));
        notificationsHolder.addNotificationContent(createNotificationContent(messageReference2));
        notificationsHolder.addNotificationContent(createNotificationContent(messageReference1));
        notificationsHolder.addNotificationContent(createNotificationContent(messageReference0));

        List<MessageReference> messageReferences = notificationsHolder.getAllMessageReferences();

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
