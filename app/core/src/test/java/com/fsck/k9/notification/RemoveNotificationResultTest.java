package com.fsck.k9.notification;


import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class RemoveNotificationResultTest {
    private static final int NOTIFICATION_ID = 23;


    private NotificationHolder notificationHolder;


    @Before
    public void setUp() throws Exception {
        notificationHolder = new NotificationHolder(NOTIFICATION_ID, null);
    }

    @Test
    public void createNotification_shouldCancelNotification_shouldReturnTrue() throws Exception {
        RemoveNotificationResult result = RemoveNotificationResult.createNotification(notificationHolder);

        assertTrue(result.shouldCreateNotification());
    }

    @Test
    public void createNotification_getNotificationId_shouldReturnNotificationId() throws Exception {
        RemoveNotificationResult result = RemoveNotificationResult.createNotification(notificationHolder);

        assertEquals(NOTIFICATION_ID, result.getNotificationId());
    }

    @Test
    public void createNotification_isUnknownNotification_shouldReturnFalse() throws Exception {
        RemoveNotificationResult result = RemoveNotificationResult.createNotification(notificationHolder);

        assertFalse(result.isUnknownNotification());
    }

    @Test
    public void createNotification_getNotificationHolder_shouldReturnNotificationHolder() throws Exception {
        RemoveNotificationResult result = RemoveNotificationResult.createNotification(notificationHolder);

        assertEquals(notificationHolder, result.getNotificationHolder());
    }

    @Test
    public void cancelNotification_shouldCancelNotification_shouldReturnFalse() throws Exception {
        RemoveNotificationResult result = RemoveNotificationResult.cancelNotification(NOTIFICATION_ID);

        assertFalse(result.shouldCreateNotification());
    }

    @Test
    public void cancelNotification_getNotificationId_shouldReturnNotificationId() throws Exception {
        RemoveNotificationResult result = RemoveNotificationResult.cancelNotification(NOTIFICATION_ID);

        assertEquals(NOTIFICATION_ID, result.getNotificationId());
    }

    @Test
    public void cancelNotification_isUnknownNotification_shouldReturnFalse() throws Exception {
        RemoveNotificationResult result = RemoveNotificationResult.cancelNotification(NOTIFICATION_ID);

        assertFalse(result.isUnknownNotification());
    }

    @Test(expected = IllegalStateException.class)
    public void cancelNotification_getNotificationHolder_shouldThrowException() throws Exception {
        RemoveNotificationResult result = RemoveNotificationResult.cancelNotification(NOTIFICATION_ID);

        result.getNotificationHolder();
    }

    @Test
    public void unknownNotification_shouldCancelNotification_shouldReturnFalse() throws Exception {
        RemoveNotificationResult result = RemoveNotificationResult.unknownNotification();

        assertFalse(result.shouldCreateNotification());
    }

    @Test(expected = IllegalStateException.class)
    public void unknownNotification_getNotificationId_shouldThrowException() throws Exception {
        RemoveNotificationResult result = RemoveNotificationResult.unknownNotification();

        result.getNotificationId();
    }

    @Test
    public void unknownNotification_isUnknownNotification_shouldReturnTrue() throws Exception {
        RemoveNotificationResult result = RemoveNotificationResult.unknownNotification();

        assertTrue(result.isUnknownNotification());
    }

    @Test(expected = IllegalStateException.class)
    public void unknownNotification_getNotificationHolder_shouldThrowException() throws Exception {
        RemoveNotificationResult result = RemoveNotificationResult.unknownNotification();

        result.getNotificationHolder();
    }
}
