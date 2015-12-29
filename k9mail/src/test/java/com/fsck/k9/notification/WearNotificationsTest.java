package com.fsck.k9.notification;


import java.util.ArrayList;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat.Action;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationCompat.Extender;
import android.support.v4.app.NotificationCompat.WearableExtender;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9.NotificationQuickDelete;
import com.fsck.k9.MockHelper;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.controller.MessagingController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 21)
public class WearNotificationsTest {
    private static final int ACCOUNT_NUMBER = 42;
    private static final String ACCOUNT_NAME = "accountName";

    private Account account;
    private Builder builder;
    private NotificationActionCreator actionCreator;
    private TestWearNotifications wearNotifications;
    private Notification notification;

    @Before
    public void setUp() throws Exception {
        account = createAccount();
        notification = createNotification();
        builder = createNotificationBuilder(notification);
        actionCreator = createNotificationActionCreator();
        NotificationController controller = createNotificationController(RuntimeEnvironment.application, builder);
        MessagingController messagingController = createMessagingController();

        wearNotifications = new TestWearNotifications(controller, actionCreator, messagingController);
    }

    @Test
    public void testBuildStackedNotification() throws Exception {
        disableOptionalActions();
        int notificationIndex = 0;
        int notificationId = NotificationIds.getNewMailStackedNotificationId(account, notificationIndex);
        MessageReference messageReference = createMessageReference(1);
        NotificationContent content = createNotificationContent(messageReference);
        NotificationHolder holder = createNotificationHolder(notificationId, content);
        PendingIntent replyPendingIntent = createFakePendingIntent(1);
        when(actionCreator.createReplyPendingIntent(messageReference, notificationId)).thenReturn(replyPendingIntent);
        PendingIntent markAsReadPendingIntent = createFakePendingIntent(2);
        when(actionCreator.createMarkMessageAsReadPendingIntent(messageReference, notificationId))
                .thenReturn(markAsReadPendingIntent);

        Notification result = wearNotifications.buildStackedNotification(account, holder);

        assertEquals(notification, result);
        verifyExtendWasOnlyCalledOnce();
        verifyAddAction(R.drawable.ic_action_single_message_options_dark, "Reply", replyPendingIntent);
        verifyAddAction(R.drawable.ic_action_mark_as_read_dark, "Mark Read", markAsReadPendingIntent);
        verifyNumberOfActions(2);
    }

    @Test
    public void testBuildStackedNotificationWithDeleteActionEnabled() throws Exception {
        enableDeleteAction();
        int notificationIndex = 0;
        int notificationId = NotificationIds.getNewMailStackedNotificationId(account, notificationIndex);
        MessageReference messageReference = createMessageReference(1);
        NotificationContent content = createNotificationContent(messageReference);
        NotificationHolder holder = createNotificationHolder(notificationId, content);
        PendingIntent deletePendingIntent = createFakePendingIntent(1);
        when(actionCreator.createDeleteMessagePendingIntent(messageReference, notificationId))
                .thenReturn(deletePendingIntent);

        Notification result = wearNotifications.buildStackedNotification(account, holder);

        assertEquals(notification, result);
        verifyExtendWasOnlyCalledOnce();
        verifyAddAction(R.drawable.ic_action_delete_dark, "Delete", deletePendingIntent);
    }

    @Test
    public void testBuildStackedNotificationWithArchiveActionEnabled() throws Exception {
        enableArchiveAction();
        int notificationIndex = 0;
        int notificationId = NotificationIds.getNewMailStackedNotificationId(account, notificationIndex);
        MessageReference messageReference = createMessageReference(1);
        NotificationContent content = createNotificationContent(messageReference);
        NotificationHolder holder = createNotificationHolder(notificationId, content);
        PendingIntent archivePendingIntent = createFakePendingIntent(1);
        when(actionCreator.createArchiveMessagePendingIntent(messageReference, notificationId))
                .thenReturn(archivePendingIntent);

        Notification result = wearNotifications.buildStackedNotification(account, holder);

        assertEquals(notification, result);
        verifyExtendWasOnlyCalledOnce();
        verifyAddAction(R.drawable.ic_action_archive_dark, "Archive", archivePendingIntent);
    }

    @Test
    public void testBuildStackedNotificationWithMarkAsSpamActionEnabled() throws Exception {
        enableSpamAction();
        int notificationIndex = 0;
        int notificationId = NotificationIds.getNewMailStackedNotificationId(account, notificationIndex);
        MessageReference messageReference = createMessageReference(1);
        NotificationContent content = createNotificationContent(messageReference);
        NotificationHolder holder = createNotificationHolder(notificationId, content);
        PendingIntent markAsSpamPendingIntent = createFakePendingIntent(1);
        when(actionCreator.createMarkMessageAsSpamPendingIntent(messageReference, notificationId))
                .thenReturn(markAsSpamPendingIntent);

        Notification result = wearNotifications.buildStackedNotification(account, holder);

        assertEquals(notification, result);
        verifyExtendWasOnlyCalledOnce();
        verifyAddAction(R.drawable.ic_action_spam_dark, "Spam", markAsSpamPendingIntent);
    }

    @Test
    public void testAddSummaryActions() throws Exception {
        disableOptionalSummaryActions();
        int notificationId = NotificationIds.getNewMailSummaryNotificationId(account);
        ArrayList<MessageReference> messageReferences = createMessageReferenceList();
        NotificationData notificationData = createNotificationData(messageReferences);
        PendingIntent markAllAsReadPendingIntent = createFakePendingIntent(1);
        when(actionCreator.getMarkAllAsReadPendingIntent(account, messageReferences, notificationId))
                .thenReturn(markAllAsReadPendingIntent);

        wearNotifications.addSummaryActions(builder, notificationData);

        verifyExtendWasOnlyCalledOnce();
        verifyAddAction(R.drawable.ic_action_mark_as_read_dark, "Mark All Read", markAllAsReadPendingIntent);
        verifyNumberOfActions(1);
    }

    @Test
    public void testAddSummaryActionsWithDeleteAllActionEnabled() throws Exception {
        enableDeleteAction();
        int notificationId = NotificationIds.getNewMailSummaryNotificationId(account);
        ArrayList<MessageReference> messageReferences = createMessageReferenceList();
        NotificationData notificationData = createNotificationData(messageReferences);
        PendingIntent deletePendingIntent = createFakePendingIntent(1);
        when(actionCreator.getDeleteAllPendingIntent(account, messageReferences, notificationId))
                .thenReturn(deletePendingIntent);

        wearNotifications.addSummaryActions(builder, notificationData);

        verifyExtendWasOnlyCalledOnce();
        verifyAddAction(R.drawable.ic_action_delete_dark, "Delete All", deletePendingIntent);
    }

    @Test
    public void testAddSummaryActionsWithArchiveAllActionEnabled() throws Exception {
        enableArchiveAction();
        int notificationId = NotificationIds.getNewMailSummaryNotificationId(account);
        ArrayList<MessageReference> messageReferences = createMessageReferenceList();
        NotificationData notificationData = createNotificationData(messageReferences);
        PendingIntent archivePendingIntent = createFakePendingIntent(1);
        when(actionCreator.createArchiveAllPendingIntent(account, messageReferences, notificationId))
                .thenReturn(archivePendingIntent);

        wearNotifications.addSummaryActions(builder, notificationData);

        verifyExtendWasOnlyCalledOnce();
        verifyAddAction(R.drawable.ic_action_archive_dark, "Archive All", archivePendingIntent);
    }

    private void disableOptionalActions() {
        disableDeleteAction();
        disableArchiveAction();
        disableSpamAction();
    }

    private void disableDeleteAction() {
        K9.setNotificationQuickDeleteBehaviour(NotificationQuickDelete.NEVER);
    }

    private void disableArchiveAction() {
        when(account.getArchiveFolderName()).thenReturn(K9.FOLDER_NONE);
    }

    private void disableSpamAction() {
        when(account.getSpamFolderName()).thenReturn(K9.FOLDER_NONE);
    }

    private void enableDeleteAction() {
        K9.setNotificationQuickDeleteBehaviour(NotificationQuickDelete.ALWAYS);
        K9.setConfirmDeleteFromNotification(false);
    }

    private void enableArchiveAction() {
        when(account.getArchiveFolderName()).thenReturn("Archive");
    }

    private void enableSpamAction() {
        when(account.getSpamFolderName()).thenReturn("Spam");
    }

    private void disableOptionalSummaryActions() {
        disableDeleteAction();
        disableArchiveAction();
    }

    private Builder createNotificationBuilder(Notification notification) {
        Builder builder = MockHelper.mockBuilder(Builder.class);
        when(builder.build()).thenReturn(notification);
        return builder;
    }

    private NotificationController createNotificationController(Context context, Builder builder) {
        NotificationController controller = mock(NotificationController.class);
        when(controller.createNotificationBuilder()).thenReturn(builder);
        when(controller.getAccountName(account)).thenReturn(ACCOUNT_NAME);
        when(controller.getContext()).thenReturn(context);
        return controller;
    }

    private NotificationActionCreator createNotificationActionCreator() {
        return mock(NotificationActionCreator.class);
    }

    private Account createAccount() {
        Account account = mock(Account.class);
        when(account.getAccountNumber()).thenReturn(ACCOUNT_NUMBER);
        return account;
    }

    private MessagingController createMessagingController() {
        MessagingController messagingController = mock(MessagingController.class);
        when(messagingController.isMoveCapable(account)).thenReturn(true);
        return messagingController;
    }

    private NotificationContent createNotificationContent(MessageReference messageReference) {
        return new NotificationContent(messageReference, null, null, null, null, false);
    }

    private NotificationHolder createNotificationHolder(int notificationId, NotificationContent content) {
        return new NotificationHolder(notificationId, content);
    }

    private Notification createNotification() {
        return mock(Notification.class);
    }

    private MessageReference createMessageReference(int number) {
        return new MessageReference(null, null, String.valueOf(number), null);
    }

    private PendingIntent createFakePendingIntent(int requestCode) {
        return PendingIntent.getActivity(RuntimeEnvironment.application, requestCode, null, 0);
    }

    private ArrayList<MessageReference> createMessageReferenceList() {
        ArrayList<MessageReference> messageReferences = new ArrayList<MessageReference>();
        messageReferences.add(createMessageReference(1));
        messageReferences.add(createMessageReference(2));

        return messageReferences;
    }

    private NotificationData createNotificationData(ArrayList<MessageReference> messageReferences) {
        NotificationData notificationData = mock(NotificationData.class);
        when(notificationData.getAccount()).thenReturn(account);
        when(notificationData.getAllMessageReferences()).thenReturn(messageReferences);
        return notificationData;
    }

    private Builder verifyExtendWasOnlyCalledOnce() {
        return verify(builder, times(1)).extend(any(Extender.class));
    }

    private void verifyAddAction(int icon, String title, PendingIntent pendingIntent) {
        verify(builder).extend(action(icon, title, pendingIntent));
    }

    private Builder verifyNumberOfActions(int expectedNumberOfActions) {
        return verify(builder).extend(numberOfActions(expectedNumberOfActions));
    }

    private WearableExtender action(int icon, String title, PendingIntent pendingIntent) {
        return argThat(new ActionMatcher(icon, title, pendingIntent));
    }

    private WearableExtender numberOfActions(int expectedNumberOfActions) {
        return argThat(new NumberOfActionsMatcher(expectedNumberOfActions));
    }


    static class ActionMatcher extends ArgumentMatcher<WearableExtender> {
        private int icon;
        private String title;
        private PendingIntent pendingIntent;

        public ActionMatcher(int icon, String title, PendingIntent pendingIntent) {
            this.icon = icon;
            this.title = title;
            this.pendingIntent = pendingIntent;
        }

        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof WearableExtender)) {
                return false;
            }

            WearableExtender wearableExtender = (WearableExtender) argument;
            for (Action action : wearableExtender.getActions()) {
                if (action.icon == icon && action.title.equals(title) && action.actionIntent == pendingIntent) {
                    return true;
                }
            }

            return false;
        }
    }

    static class NumberOfActionsMatcher extends ArgumentMatcher<WearableExtender> {
        private final int expectedNumberOfActions;

        public NumberOfActionsMatcher(int expectedNumberOfActions) {
            this.expectedNumberOfActions = expectedNumberOfActions;
        }

        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof WearableExtender)) {
                return false;
            }

            WearableExtender wearableExtender = (WearableExtender) argument;
            return wearableExtender.getActions().size() == expectedNumberOfActions;
        }
    }

    static class TestWearNotifications extends WearNotifications {
        private final MessagingController messagingController;

        public TestWearNotifications(NotificationController controller, NotificationActionCreator actionCreator,
                MessagingController messagingController) {
            super(controller, actionCreator);
            this.messagingController = messagingController;
        }

        @Override
        MessagingController createMessagingController() {
            return messagingController;
        }
    }
}
