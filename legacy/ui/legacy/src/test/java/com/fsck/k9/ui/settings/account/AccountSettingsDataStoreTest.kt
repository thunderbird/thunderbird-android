package com.fsck.k9.ui.settings.account

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.job.K9JobManager
import com.fsck.k9.notification.NotificationChannelManager
import com.fsck.k9.notification.NotificationController
import java.util.concurrent.ExecutorService
import net.thunderbird.core.android.account.LegacyAccountDto
import org.junit.Test
import org.mockito.kotlin.mock

class AccountSettingsDataStoreTest {
    private val account = LegacyAccountDto("00000000-0000-0000-0000-000000000000")
    private val testSubject = AccountSettingsDataStore(
        preferences = mock<Preferences>(),
        executorService = mock<ExecutorService>(),
        account = account,
        jobManager = mock<K9JobManager>(),
        notificationChannelManager = mock<NotificationChannelManager>(),
        notificationController = mock<NotificationController>(),
        messagingController = mock<MessagingController>(),
    )

    @Test
    fun `getBoolean should read use recipient address for reply`() {
        account.useRecipientAddressForReply = true

        val result = testSubject.getBoolean("use_recipient_address_for_reply", false)

        assertThat(result).isEqualTo(true)
    }

    @Test
    fun `putBoolean should update use recipient address for reply`() {
        testSubject.putBoolean("use_recipient_address_for_reply", true)

        assertThat(account.useRecipientAddressForReply).isEqualTo(true)
    }
}
