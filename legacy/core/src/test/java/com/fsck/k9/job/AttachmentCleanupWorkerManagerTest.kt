package com.fsck.k9.job

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.ATTACHMENT_CLEANUP_NEVER
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class AttachmentCleanupWorkerManagerTest {
    private val workManager = mock<WorkManager>()
    private val testSubject = AttachmentCleanupWorkerManager(workManager)

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @Test
    fun `scheduleAttachmentCleanup should schedule periodic cleanup for IMAP account with retention enabled`() {
        val account = createAccount(protocol = Protocols.IMAP, attachmentCleanupDays = 30)

        testSubject.scheduleAttachmentCleanup(account)

        val requestCaptor = argumentCaptor<PeriodicWorkRequest>()
        verify(workManager).enqueueUniquePeriodicWork(
            eq(PERIODIC_WORK_NAME),
            eq(ExistingPeriodicWorkPolicy.REPLACE),
            requestCaptor.capture(),
        )
        val request = requestCaptor.firstValue
        assertThat(request.tags).contains(AttachmentCleanupWorkerManager.ATTACHMENT_CLEANUP_TAG)
        assertThat(request.workSpec.input.getString(AttachmentCleanupWorker.EXTRA_ACCOUNT_UUID)).isEqualTo(ACCOUNT_UUID)
    }

    @Test
    fun `scheduleAttachmentCleanup should cancel cleanup when retention is disabled`() {
        val account = createAccount(protocol = Protocols.IMAP, attachmentCleanupDays = ATTACHMENT_CLEANUP_NEVER)

        testSubject.scheduleAttachmentCleanup(account)

        verify(workManager).cancelUniqueWork(PERIODIC_WORK_NAME)
        verify(workManager).cancelUniqueWork(ONE_TIME_WORK_NAME)
        verify(workManager, never()).enqueueUniquePeriodicWork(
            eq(PERIODIC_WORK_NAME),
            eq(ExistingPeriodicWorkPolicy.REPLACE),
            any<PeriodicWorkRequest>(),
        )
    }

    @Test
    fun `scheduleAttachmentCleanup should cancel cleanup for non-IMAP account`() {
        val account = createAccount(protocol = Protocols.POP3, attachmentCleanupDays = 30)

        testSubject.scheduleAttachmentCleanup(account)

        verify(workManager).cancelUniqueWork(PERIODIC_WORK_NAME)
        verify(workManager).cancelUniqueWork(ONE_TIME_WORK_NAME)
        verify(workManager, never()).enqueueUniquePeriodicWork(
            eq(PERIODIC_WORK_NAME),
            eq(ExistingPeriodicWorkPolicy.REPLACE),
            any<PeriodicWorkRequest>(),
        )
    }

    @Test
    fun `scheduleAttachmentCleanupNow should schedule one-time cleanup for IMAP account with retention enabled`() {
        val account = createAccount(protocol = Protocols.IMAP, attachmentCleanupDays = 30)

        testSubject.scheduleAttachmentCleanupNow(account)

        val requestCaptor = argumentCaptor<OneTimeWorkRequest>()
        verify(workManager).enqueueUniqueWork(
            eq(ONE_TIME_WORK_NAME),
            eq(ExistingWorkPolicy.REPLACE),
            requestCaptor.capture(),
        )
        val request = requestCaptor.firstValue
        assertThat(request.tags).contains(AttachmentCleanupWorkerManager.ATTACHMENT_CLEANUP_TAG)
        assertThat(request.workSpec.input.getString(AttachmentCleanupWorker.EXTRA_ACCOUNT_UUID)).isEqualTo(ACCOUNT_UUID)
    }

    @Test
    fun `scheduleAttachmentCleanupNow should skip one-time cleanup when retention is disabled`() {
        val account = createAccount(protocol = Protocols.IMAP, attachmentCleanupDays = ATTACHMENT_CLEANUP_NEVER)

        testSubject.scheduleAttachmentCleanupNow(account)

        verify(workManager, never()).enqueueUniqueWork(
            eq(ONE_TIME_WORK_NAME),
            eq(ExistingWorkPolicy.REPLACE),
            any<OneTimeWorkRequest>(),
        )
    }

    @Test
    fun `cancelAllAttachmentCleanup should cancel work by cleanup tag`() {
        testSubject.cancelAllAttachmentCleanup()

        verify(workManager).cancelAllWorkByTag(AttachmentCleanupWorkerManager.ATTACHMENT_CLEANUP_TAG)
    }

    private fun createAccount(
        protocol: String,
        attachmentCleanupDays: Int,
    ): LegacyAccountDto {
        return LegacyAccountDto(ACCOUNT_UUID).apply {
            incomingServerSettings = createServerSettings(protocol)
            this.attachmentCleanupDays = attachmentCleanupDays
        }
    }

    private fun createServerSettings(protocol: String): ServerSettings {
        return ServerSettings(
            type = protocol,
            host = "irrelevant",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "username",
            password = "password",
            clientCertificateAlias = null,
        )
    }

    companion object {
        private const val ACCOUNT_UUID = "00000000-0000-0000-0000-000000000000"
        private const val PERIODIC_WORK_NAME = "AttachmentCleanup:$ACCOUNT_UUID"
        private const val ONE_TIME_WORK_NAME = "AttachmentCleanupNow:$ACCOUNT_UUID"
    }
}
