package com.fsck.k9.notification;


import java.util.Arrays;
import java.util.List;

import android.app.Application;
import android.app.Notification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationCompat.InboxStyle;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9.NotificationHideSubject;
import com.fsck.k9.K9.NotificationQuickDelete;
import com.fsck.k9.NotificationSetting;
import com.fsck.k9.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static com.fsck.k9.MockHelper.mockBuilder;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
public class DeviceNotificationsTest {
    private static final int UNREAD_MESSAGE_COUNT = 42;
    private static final int NEW_MESSAGE_COUNT = 2;
    private static final String ACCOUNT_NAME = "accountName";
    private static final int ACCOUNT_NUMBER = 3;
    private static final int ACCOUNT_COLOR = 0xABCDEF;
    private static final String SUMMARY = "summary";
    private static final String PREVIEW = "preview";
    private static final String SUBJECT = "subject";
    private static final String SENDER = "sender";
    private static final String SUMMARY_2 = "summary2";
    private static final String PREVIEW_2 = "preview2";
    private static final String SUBJECT_2 = "subject2";
    private static final String SENDER_2 = "sender2";
    private static final int NOTIFICATION_ID = 23;
    private static final Notification FAKE_NOTIFICATION = mock(Notification.class);


    private Account account;
    private NotificationData notificationData;
    private TestDeviceNotifications notifications;
    private Builder builder;
    private Builder builder2 = mockBuilder(Builder.class);
    private LockScreenNotification lockScreenNotification;


    @Before
    public void setUp() throws Exception {
        account = createFakeAccount();
        notificationData = createFakeNotificationData(account);

        builder = createFakeNotificationBuilder();
        lockScreenNotification = mock(LockScreenNotification.class);
        notifications = createDeviceNotifications(builder, lockScreenNotification);
    }

    @Test
    public void buildSummaryNotification_withPrivacyModeActive() throws Exception {
        K9.setNotificationHideSubject(NotificationHideSubject.ALWAYS);

        Notification result = notifications.buildSummaryNotification(account, notificationData, false);

        verify(builder).setSmallIcon(R.drawable.notification_icon_new_mail);
        verify(builder).setColor(ACCOUNT_COLOR);
        verify(builder).setAutoCancel(true);
        verify(builder).setNumber(UNREAD_MESSAGE_COUNT);
        verify(builder).setTicker("New mail");
        verify(builder).setContentText("New mail");
        verify(builder).setContentTitle(UNREAD_MESSAGE_COUNT + " Unread (" + ACCOUNT_NAME + ")");
        verify(lockScreenNotification).configureLockScreenNotification(builder, notificationData);
        assertEquals(FAKE_NOTIFICATION, result);
    }

    @Test
    public void buildSummaryNotification_withSingleMessageNotification() throws Exception {
        K9.setNotificationHideSubject(NotificationHideSubject.NEVER);
        K9.setNotificationQuickDeleteBehaviour(NotificationQuickDelete.ALWAYS);
        when(notificationData.isSingleMessageNotification()).thenReturn(true);

        Notification result = notifications.buildSummaryNotification(account, notificationData, false);

        verify(builder).setSmallIcon(R.drawable.notification_icon_new_mail);
        verify(builder).setColor(ACCOUNT_COLOR);
        verify(builder).setAutoCancel(true);
        verify(builder).setTicker(SUMMARY);
        verify(builder).setContentText(SUBJECT);
        verify(builder).setContentTitle(SENDER);
        verify(builder).setStyle(notifications.bigTextStyle);
        verify(notifications.bigTextStyle).bigText(PREVIEW);
        verify(builder).addAction(R.drawable.notification_action_reply, "Reply", null);
        verify(builder).addAction(R.drawable.notification_action_mark_as_read, "Mark Read", null);
        verify(builder).addAction(R.drawable.notification_action_delete, "Delete", null);
        verify(lockScreenNotification).configureLockScreenNotification(builder, notificationData);
        assertEquals(FAKE_NOTIFICATION, result);
    }

    @Test
    public void buildSummaryNotification_withMultiMessageNotification() throws Exception {
        K9.setNotificationHideSubject(NotificationHideSubject.NEVER);
        K9.setNotificationQuickDeleteBehaviour(NotificationQuickDelete.ALWAYS);
        when(notificationData.isSingleMessageNotification()).thenReturn(false);
        when(notificationData.containsStarredMessages()).thenReturn(true);

        Notification result = notifications.buildSummaryNotification(account, notificationData, false);

        verify(builder).setSmallIcon(R.drawable.notification_icon_new_mail);
        verify(builder).setColor(ACCOUNT_COLOR);
        verify(builder).setAutoCancel(true);
        verify(builder).setTicker(SUMMARY);
        verify(builder).setContentTitle(NEW_MESSAGE_COUNT + " new messages");
        verify(builder).setSubText(ACCOUNT_NAME);
        verify(builder).setGroup("newMailNotifications-" + ACCOUNT_NUMBER);
        verify(builder).setGroupSummary(true);
        verify(builder).setPriority(NotificationCompat.PRIORITY_HIGH);
        verify(builder).setStyle(notifications.inboxStyle);
        verify(notifications.inboxStyle).setBigContentTitle(NEW_MESSAGE_COUNT + " new messages");
        verify(notifications.inboxStyle).setSummaryText(ACCOUNT_NAME);
        verify(notifications.inboxStyle).addLine(SUMMARY);
        verify(notifications.inboxStyle).addLine(SUMMARY_2);
        verify(builder).addAction(R.drawable.notification_action_mark_as_read, "Mark Read", null);
        verify(builder).addAction(R.drawable.notification_action_delete, "Delete", null);
        verify(lockScreenNotification).configureLockScreenNotification(builder, notificationData);
        assertEquals(FAKE_NOTIFICATION, result);
    }

    @Test
    public void buildSummaryNotification_withAdditionalMessages() throws Exception {
        K9.setNotificationHideSubject(NotificationHideSubject.NEVER);
        K9.setNotificationQuickDeleteBehaviour(NotificationQuickDelete.ALWAYS);
        when(notificationData.isSingleMessageNotification()).thenReturn(false);
        when(notificationData.hasSummaryOverflowMessages()).thenReturn(true);
        when(notificationData.getSummaryOverflowMessagesCount()).thenReturn(23);

        notifications.buildSummaryNotification(account, notificationData, false);

        verify(notifications.inboxStyle).setSummaryText("+ 23 more on " + ACCOUNT_NAME);
    }

    @Test
    public void buildSummaryNotification_withoutDeleteAllAction() throws Exception {
        K9.setNotificationHideSubject(NotificationHideSubject.NEVER);
        K9.setNotificationQuickDeleteBehaviour(NotificationQuickDelete.NEVER);
        when(notificationData.isSingleMessageNotification()).thenReturn(false);

        notifications.buildSummaryNotification(account, notificationData, false);

        verify(builder, never()).addAction(R.drawable.notification_action_delete, "Delete", null);
    }

    @Test
    public void buildSummaryNotification_withoutDeleteAction() throws Exception {
        K9.setNotificationHideSubject(NotificationHideSubject.NEVER);
        K9.setNotificationQuickDeleteBehaviour(NotificationQuickDelete.NEVER);
        when(notificationData.isSingleMessageNotification()).thenReturn(true);

        notifications.buildSummaryNotification(account, notificationData, false);

        verify(builder, never()).addAction(R.drawable.notification_action_delete, "Delete", null);
    }

    private Builder createFakeNotificationBuilder() {
        Builder builder = mockBuilder(Builder.class);
        when(builder.build()).thenReturn(FAKE_NOTIFICATION);
        return builder;
    }

    private Account createFakeAccount() {
        Account account = mock(Account.class);

        when(account.getChipColor()).thenReturn(ACCOUNT_COLOR);
        when(account.getAccountNumber()).thenReturn(ACCOUNT_NUMBER);

        NotificationSetting notificationSetting = mock(NotificationSetting.class);
        when(account.getNotificationSetting()).thenReturn(notificationSetting);

        return account;
    }

    private NotificationData createFakeNotificationData(Account account) {
        NotificationData notificationData = mock(NotificationData.class);
        when(notificationData.getUnreadMessageCount()).thenReturn(UNREAD_MESSAGE_COUNT);
        when(notificationData.getNewMessagesCount()).thenReturn(NEW_MESSAGE_COUNT);
        when(notificationData.getAccount()).thenReturn(account);

        NotificationContent content = new NotificationContent(null, SENDER, SUBJECT, PREVIEW, SUMMARY, false);
        NotificationContent content2 = new NotificationContent(null, SENDER_2, SUBJECT_2, PREVIEW_2, SUMMARY_2, true);
        List<NotificationContent> contents = Arrays.asList(content, content2);
        when(notificationData.getContentForSummaryNotification()).thenReturn(contents);

        NotificationHolder holder = new NotificationHolder(NOTIFICATION_ID, content);
        when(notificationData.getHolderForLatestNotification()).thenReturn(holder);

        return notificationData;
    }

    private TestDeviceNotifications createDeviceNotifications(Builder builder,
            LockScreenNotification lockScreenNotification) {
        NotificationController controller = createFakeNotificationController(builder);
        NotificationActionCreator actionCreator = mock(NotificationActionCreator.class);
        WearNotifications wearNotifications = mock(WearNotifications.class);

        return new TestDeviceNotifications(controller, actionCreator, lockScreenNotification, wearNotifications);
    }

    private NotificationController createFakeNotificationController(final Builder builder) {
        Application context = RuntimeEnvironment.application;

        NotificationController controller = mock(NotificationController.class);
        when(controller.getContext()).thenReturn(context);
        when(controller.getAccountName(any(Account.class))).thenReturn(ACCOUNT_NAME);
        when(controller.createNotificationBuilder()).thenAnswer(new Answer<Builder>() {
            private int invocationCount = 0;

            @Override
            public Builder answer(InvocationOnMock invocation) throws Throwable {
                invocationCount++;
                switch (invocationCount) {
                    case 1: {
                        return builder;
                    }
                    case 2: {
                        return builder2;
                    }
                }

                throw new AssertionError("createNotificationBuilder() invoked more than twice");
            }
        });

        return controller;
    }


    static class TestDeviceNotifications extends DeviceNotifications {
        BigTextStyle bigTextStyle = mockBuilder(BigTextStyle.class);
        InboxStyle inboxStyle = mockBuilder(InboxStyle.class);


        TestDeviceNotifications(NotificationController controller, NotificationActionCreator actionCreator,
                LockScreenNotification lockScreenNotification, WearNotifications wearNotifications) {
            super(controller, actionCreator, lockScreenNotification, wearNotifications);
        }

        @Override
        protected BigTextStyle createBigTextStyle(Builder builder) {
            return bigTextStyle;
        }

        @Override
        protected InboxStyle createInboxStyle(Builder builder) {
            return inboxStyle;
        }
    }
}
