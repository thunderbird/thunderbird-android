package com.fsck.k9.controller

import app.k9mail.legacy.mailstore.ListenableMessageStore
import app.k9mail.legacy.mailstore.MessageStoreManager
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.fsck.k9.backend.api.BackendFolderUpdater
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mailstore.LegacyAccountDtoBackendStorageFactory
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mail.FolderType
import java.util.Calendar
import java.util.Date
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.feature.mail.folder.api.ArchiveGranularity
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ArchiveFolderResolverTest {
    @BeforeTest
    fun setup() {
        Log.logger = mock<Logger>()
    }
    private val messageStoreManager: MessageStoreManager = mock()
    private val messageStore: ListenableMessageStore = mock()
    private val backendStorageFactory: LegacyAccountDtoBackendStorageFactory = mock()
    private val backendStorage: BackendStorage = mock()
    private val backendFolderUpdater: BackendFolderUpdater = mock()

    private val resolver = ArchiveFolderResolver(messageStoreManager, backendStorageFactory)

    private val account = LegacyAccountDto(UUID.randomUUID().toString()).apply {
        archiveFolderId = BASE_ARCHIVE_FOLDER_ID
        archiveGranularity = ArchiveGranularity.DEFAULT
    }

    @Test
    fun `resolve to single folder when granularity is SINGLE_ARCHIVE_FOLDER`() {
        account.archiveGranularity = ArchiveGranularity.SINGLE_ARCHIVE_FOLDER
        val message = createMessage(year = 2025, month = 11)

        val result = resolver.resolveArchiveFolder(account, message)

        assertThat(result).isEqualTo(BASE_ARCHIVE_FOLDER_ID)
        verify(messageStoreManager, never()).getMessageStore(any<LegacyAccountDto>())
    }

    @Test
    fun `resolve to yearly folder when granularity is PER_YEAR_ARCHIVE_FOLDERS`() {
        account.archiveGranularity = ArchiveGranularity.PER_YEAR_ARCHIVE_FOLDERS
        val message = createMessage(year = 2025, month = 11)

        whenever(messageStoreManager.getMessageStore(account)).thenReturn(messageStore)
        whenever(messageStore.getFolderServerId(BASE_ARCHIVE_FOLDER_ID)).thenReturn("Archive")
        whenever(messageStore.getFolderId("Archive/2025")).thenReturn(null)
        whenever(backendStorageFactory.createBackendStorage(account)).thenReturn(backendStorage)
        whenever(backendStorage.createFolderUpdater()).thenReturn(backendFolderUpdater)
        whenever(backendFolderUpdater.createFolders(any<List<FolderInfo>>())).thenReturn(setOf(YEARLY_FOLDER_ID))

        val result = resolver.resolveArchiveFolder(account, message)

        assertThat(result).isEqualTo(YEARLY_FOLDER_ID)
    }

    @Test
    fun `resolve to monthly folder when granularity is PER_MONTH_ARCHIVE_FOLDERS`() {
        account.archiveGranularity = ArchiveGranularity.PER_MONTH_ARCHIVE_FOLDERS
        val message = createMessage(year = 2025, month = 11)

        whenever(messageStoreManager.getMessageStore(account)).thenReturn(messageStore)
        whenever(messageStore.getFolderServerId(BASE_ARCHIVE_FOLDER_ID)).thenReturn("Archive")
        whenever(messageStore.getFolderId("Archive/2025")).thenReturn(null)
        whenever(messageStore.getFolderServerId(YEARLY_FOLDER_ID)).thenReturn("Archive/2025")
        whenever(messageStore.getFolderId("Archive/2025/11")).thenReturn(null)
        whenever(backendStorageFactory.createBackendStorage(account)).thenReturn(backendStorage)
        whenever(backendStorage.createFolderUpdater()).thenReturn(backendFolderUpdater)
        whenever(backendFolderUpdater.createFolders(any<List<FolderInfo>>())).thenReturn(setOf(YEARLY_FOLDER_ID), setOf(MONTHLY_FOLDER_ID))

        val result = resolver.resolveArchiveFolder(account, message)

        assertThat(result).isEqualTo(MONTHLY_FOLDER_ID)
    }

    @Test
    fun `return null when archive folder is not configured`() {
        account.archiveFolderId = null
        val message = createMessage(year = 2025, month = 11)

        val result = resolver.resolveArchiveFolder(account, message)

        assertThat(result).isNull()
    }

    @Test
    fun `reuse existing yearly folder instead of creating new one`() {
        account.archiveGranularity = ArchiveGranularity.PER_YEAR_ARCHIVE_FOLDERS
        val message = createMessage(year = 2025, month = 11)

        whenever(messageStoreManager.getMessageStore(account)).thenReturn(messageStore)
        whenever(messageStore.getFolderServerId(BASE_ARCHIVE_FOLDER_ID)).thenReturn("Archive")
        whenever(messageStore.getFolderId("Archive/2025")).thenReturn(YEARLY_FOLDER_ID)

        val result = resolver.resolveArchiveFolder(account, message)

        assertThat(result).isEqualTo(YEARLY_FOLDER_ID)
        verify(backendStorageFactory, never()).createBackendStorage(any<LegacyAccountDto>())
    }

    @Test
    fun `reuse existing monthly folder instead of creating new one`() {
        account.archiveGranularity = ArchiveGranularity.PER_MONTH_ARCHIVE_FOLDERS
        val message = createMessage(year = 2025, month = 11)

        whenever(messageStoreManager.getMessageStore(account)).thenReturn(messageStore)
        whenever(messageStore.getFolderServerId(BASE_ARCHIVE_FOLDER_ID)).thenReturn("Archive")
        whenever(messageStore.getFolderId("Archive/2025")).thenReturn(YEARLY_FOLDER_ID)
        whenever(messageStore.getFolderServerId(YEARLY_FOLDER_ID)).thenReturn("Archive/2025")
        whenever(messageStore.getFolderId("Archive/2025/11")).thenReturn(MONTHLY_FOLDER_ID)

        val result = resolver.resolveArchiveFolder(account, message)

        assertThat(result).isEqualTo(MONTHLY_FOLDER_ID)
        verify(backendStorageFactory, never()).createBackendStorage(any<LegacyAccountDto>())
    }

    @Test
    fun `use internal date when available`() {
        account.archiveGranularity = ArchiveGranularity.PER_YEAR_ARCHIVE_FOLDERS
        val message = createMessage(year = 2025, month = 11)
        whenever(message.internalDate).thenReturn(createDate(2024, 5))

        whenever(messageStoreManager.getMessageStore(account)).thenReturn(messageStore)
        whenever(messageStore.getFolderServerId(BASE_ARCHIVE_FOLDER_ID)).thenReturn("Archive")
        whenever(messageStore.getFolderId("Archive/2024")).thenReturn(YEARLY_FOLDER_ID)

        val result = resolver.resolveArchiveFolder(account, message)

        assertThat(result).isEqualTo(YEARLY_FOLDER_ID)
    }

    @Test
    fun `fall back to sent date when internal date is null`() {
        account.archiveGranularity = ArchiveGranularity.PER_YEAR_ARCHIVE_FOLDERS
        val message = createMessage(year = 2025, month = 11, useInternalDate = false)
        whenever(message.internalDate).thenReturn(null)

        whenever(messageStoreManager.getMessageStore(account)).thenReturn(messageStore)
        whenever(messageStore.getFolderServerId(BASE_ARCHIVE_FOLDER_ID)).thenReturn("Archive")
        whenever(messageStore.getFolderId("Archive/2025")).thenReturn(YEARLY_FOLDER_ID)

        val result = resolver.resolveArchiveFolder(account, message)

        assertThat(result).isEqualTo(YEARLY_FOLDER_ID)
    }

    @Test
    fun `format month with leading zero for single digit months`() {
        account.archiveGranularity = ArchiveGranularity.PER_MONTH_ARCHIVE_FOLDERS
        val message = createMessage(year = 2025, month = 3)

        whenever(messageStoreManager.getMessageStore(account)).thenReturn(messageStore)
        whenever(messageStore.getFolderServerId(BASE_ARCHIVE_FOLDER_ID)).thenReturn("Archive")
        whenever(messageStore.getFolderId("Archive/2025")).thenReturn(YEARLY_FOLDER_ID)
        whenever(messageStore.getFolderServerId(YEARLY_FOLDER_ID)).thenReturn("Archive/2025")
        whenever(messageStore.getFolderId("Archive/2025/03")).thenReturn(MONTHLY_FOLDER_ID)

        val result = resolver.resolveArchiveFolder(account, message)

        assertThat(result).isEqualTo(MONTHLY_FOLDER_ID)
    }

    @Test
    fun `return null when yearly subfolder creation fails`() {
        account.archiveGranularity = ArchiveGranularity.PER_YEAR_ARCHIVE_FOLDERS
        val message = createMessage(year = 2025, month = 11)

        whenever(messageStoreManager.getMessageStore(account)).thenReturn(messageStore)
        whenever(messageStore.getFolderServerId(BASE_ARCHIVE_FOLDER_ID)).thenReturn("Archive")
        whenever(messageStore.getFolderId("Archive/2025")).thenReturn(null)
        whenever(backendStorageFactory.createBackendStorage(account)).thenReturn(backendStorage)
        whenever(backendStorage.createFolderUpdater()).thenReturn(backendFolderUpdater)
        whenever(backendFolderUpdater.createFolders(any<List<FolderInfo>>())).thenThrow(RuntimeException("Folder creation failed"))

        val result = resolver.resolveArchiveFolder(account, message)

        assertThat(result).isNull()
    }

    @Test
    fun `return null when yearly folder creation succeeds but monthly creation fails`() {
        account.archiveGranularity = ArchiveGranularity.PER_MONTH_ARCHIVE_FOLDERS
        val message = createMessage(year = 2025, month = 11)

        whenever(messageStoreManager.getMessageStore(account)).thenReturn(messageStore)
        whenever(messageStore.getFolderServerId(BASE_ARCHIVE_FOLDER_ID)).thenReturn("Archive")
        whenever(messageStore.getFolderId("Archive/2025")).thenReturn(null)
        whenever(messageStore.getFolderServerId(YEARLY_FOLDER_ID)).thenReturn("Archive/2025")
        whenever(messageStore.getFolderId("Archive/2025/11")).thenReturn(null)
        whenever(backendStorageFactory.createBackendStorage(account)).thenReturn(backendStorage)
        whenever(backendStorage.createFolderUpdater()).thenReturn(backendFolderUpdater)
        whenever(backendFolderUpdater.createFolders(any<List<FolderInfo>>()))
            .thenReturn(setOf(YEARLY_FOLDER_ID))
            .thenThrow(RuntimeException("Monthly folder creation failed"))

        val result = resolver.resolveArchiveFolder(account, message)

        assertThat(result).isNull()
    }

    private fun createMessage(year: Int, month: Int, useInternalDate: Boolean = true): LocalMessage {
        return mock<LocalMessage>().apply {
            whenever(sentDate).thenReturn(createDate(year, month))
            if (useInternalDate) {
                whenever(internalDate).thenReturn(createDate(year, month))
            }
        }
    }

    private fun createDate(year: Int, month: Int): Date {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 15)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    companion object {
        private const val BASE_ARCHIVE_FOLDER_ID = 100L
        private const val YEARLY_FOLDER_ID = 200L
        private const val MONTHLY_FOLDER_ID = 300L
    }
}
