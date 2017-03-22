package com.fsck.k9.notification;


import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class AddNotificationResultTest {
    private static final int NOTIFICATION_ID = 23;


    private NotificationHolder notificationHolder;


    @Before
    public void setUp() throws Exception {
        notificationHolder = new NotificationHolder(NOTIFICATION_ID, null);
    }

    @Test
    public void newNotification_shouldCancelNotification_shouldReturnFalse() throws Exception {
        AddNotificationResult result = AddNotificationResult.newNotification(notificationHolder);

        assertFalse(result.shouldCancelNotification());
    }

    @Test(expected = IllegalStateException.class)
    public void newNotification_getNotificationId_shouldReturnNotificationId() throws Exception {
        AddNotificationResult result = AddNotificationResult.newNotification(notificationHolder);

        result.getNotificationId();
    }

    @Test
    public void replaceNotification_shouldCancelNotification_shouldReturnTrue() throws Exception {
        AddNotificationResult result = AddNotificationResult.replaceNotification(notificationHolder);

        assertTrue(result.shouldCancelNotification());
    }

    @Test
    public void replaceNotification_getNotificationId_shouldReturnNotificationId() throws Exception {
        AddNotificationResult result = AddNotificationResult.replaceNotification(notificationHolder);

        assertEquals(NOTIFICATION_ID, result.getNotificationId());
    }

    @Test
    public void getNotificationHolder_shouldReturnNotificationHolder() throws Exception {
        AddNotificationResult result = AddNotificationResult.replaceNotification(notificationHolder);

        assertEquals(notificationHolder, result.getNotificationHolder());
    }
}
