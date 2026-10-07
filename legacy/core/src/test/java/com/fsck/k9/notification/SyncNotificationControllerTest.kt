package com.fsck.k9.notification

import android.app.Notification
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import app.k9mail.core.android.common.provider.NotificationIconResourceProvider
import com.fsck.k9.FakeLegacyAccount
import com.fsck.k9.mailstore.LocalFolder
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.android.testing.MockHelper.mockBuilder
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import net.thunderbird.legacy.core.mailstore.folder.FakeOutboxFolderManager
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never

private const val ACCOUNT_NUMBER = 1
private const val ACCOUNT_NAME = "TestAccount"
private const val FOLDER_SERVER_ID = "INBOX"
private const val FOLDER_NAME = "Inbox"
private const val TEST_ICON_ID = 0xCAFE
private const val NOTIFICATION_ID = 101

class SyncNotificationControllerTest : RobolectricTest() {
    private val resourceProvider: NotificationResourceProvider = TestNotificationResourceProvider()
    private val iconResourceProvider: NotificationIconResourceProvider =
        TestNotificationIconResourceProvider(pushNotificationIcon = TEST_ICON_ID)
    private val notification = mock<Notification>()
    private val lockScreenNotification = mock<Notification>()
    private val notificationManager = mock<NotificationManagerCompat>()
    private val builder = createFakeNotificationBuilder(notification)
    private val lockScreenNotificationBuilder = createFakeNotificationBuilder(lockScreenNotification)
    private val accountId = AccountIdFactory.create()
    private val notificationHelper = createFakeNotificationHelper(
        notificationManager,
        builder,
        lockScreenNotificationBuilder,
    )
    private val account = FakeLegacyAccount.ACCOUNT.copy(
        id = accountId,
        accountNumber = ACCOUNT_NUMBER,
        profile = ProfileDto(
            id = accountId,
            name = ACCOUNT_NAME,
            color = -1,
            avatar = AvatarDto(
                id = accountId,
                avatarType = AvatarTypeDto.MONOGRAM,
                avatarMonogram = "TA",
                avatarImageUri = null,
                avatarIconName = null,
            ),
        ),
    )
    private val accountManager = mock<LegacyAccountManager> {
        on { findById(accountId) } doReturn account
    }
    private val notificationIdRegistry = mock<AccountNotificationIdRegistry> {
        on { getOrAllocate(accountId, AccountNotificationKind.Sync) } doReturn NOTIFICATION_ID
    }
    private val contentIntent = mock<PendingIntent>()
    private val controller = SyncNotificationController(
        notificationHelper = notificationHelper,
        actionBuilder = createActionBuilder(contentIntent),
        resourceProvider = resourceProvider,
        outboxFolderManager = FakeOutboxFolderManager(outboxFolderId = 33L),
        iconResourceProvider = iconResourceProvider,
        accountManager = accountManager,
        notificationIdRegistry = notificationIdRegistry,
    )

    @Test
    fun testShowSendingNotification() {
        controller.showSendingNotification(accountId)

        verify(notificationHelper).notify(NOTIFICATION_ID, notification)
        verify(builder).setSmallIcon(resourceProvider.iconSendingMail)
        verify(builder).setTicker("Sending mail: $ACCOUNT_NAME")
        verify(builder).setContentTitle("Sending mail")
        verify(builder).setContentText(ACCOUNT_NAME)
        verify(builder).setContentIntent(contentIntent)
        verify(builder).setPublicVersion(lockScreenNotification)
        verify(lockScreenNotificationBuilder).setContentTitle("Sending mail")
        verify(lockScreenNotificationBuilder, never()).setContentText(any())
        verify(lockScreenNotificationBuilder, never()).setTicker(any())
    }

    @Test
    fun testClearSendingNotification() {
        controller.clearSendingNotification(accountId)

        verify(notificationManager).cancel(NOTIFICATION_ID)
    }

    @Test
    fun testGetFetchingMailNotificationId() {
        val localFolder = createFakeLocalFolder()

        controller.showFetchingMailNotification(accountId, localFolder)

        verify(notificationHelper).notify(NOTIFICATION_ID, notification)
        verify(builder).setSmallIcon(iconResourceProvider.pushNotificationIcon)
        verify(builder).setTicker("Checking mail: $ACCOUNT_NAME:$FOLDER_NAME")
        verify(builder).setContentTitle("Checking mail")
        verify(builder).setContentText("$ACCOUNT_NAME:$FOLDER_NAME")
        verify(builder).setContentIntent(contentIntent)
        verify(builder).setPublicVersion(lockScreenNotification)
        verify(lockScreenNotificationBuilder).setContentTitle("Checking mail")
        verify(lockScreenNotificationBuilder, never()).setContentText(any())
        verify(lockScreenNotificationBuilder, never()).setTicker(any())
    }

    @Test
    fun testShowEmptyFetchingMailNotification() {
        controller.showEmptyFetchingMailNotification(accountId)

        verify(notificationHelper).notify(NOTIFICATION_ID, notification)
        verify(builder).setSmallIcon(iconResourceProvider.pushNotificationIcon)
        verify(builder).setContentTitle("Checking mail")
        verify(builder).setContentText(ACCOUNT_NAME)
        verify(builder).setPublicVersion(lockScreenNotification)
        verify(lockScreenNotificationBuilder).setContentTitle("Checking mail")
        verify(lockScreenNotificationBuilder, never()).setContentText(any())
        verify(lockScreenNotificationBuilder, never()).setTicker(any())
    }

    @Test
    fun testClearSendFailedNotification() {
        controller.clearFetchingMailNotification(accountId)

        verify(notificationManager).cancel(NOTIFICATION_ID)
    }

    private fun createFakeNotificationBuilder(notification: Notification): NotificationCompat.Builder {
        return mockBuilder {
            on { build() } doReturn notification
        }
    }

    private fun createFakeNotificationHelper(
        notificationManager: NotificationManagerCompat,
        notificationBuilder: NotificationCompat.Builder,
        lockScreenNotificationBuilder: NotificationCompat.Builder,
    ): NotificationHelper {
        return mock {
            on { getContext() } doReturn ApplicationProvider.getApplicationContext()
            on { getNotificationManager() } doReturn notificationManager
            on {
                createNotificationBuilder(eq(accountId), any(), any())
            }.doReturn(notificationBuilder, lockScreenNotificationBuilder)
        }
    }

    private fun createActionBuilder(contentIntent: PendingIntent): NotificationActionCreator {
        return mock {
            on {
                createViewFolderPendingIntent(eq(accountId), anyLong())
            } doReturn contentIntent
        }
    }

    private fun createFakeLocalFolder(): LocalFolder {
        return mock {
            on { serverId } doReturn FOLDER_SERVER_ID
            on { name } doReturn FOLDER_NAME
        }
    }
}
