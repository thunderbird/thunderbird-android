package com.fsck.k9.controller

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.fsck.k9.mailstore.LocalMessage
import java.util.Calendar
import java.util.Date
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.feature.mail.folder.api.ArchiveGranularity
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ArchiveFolderResolverTest {
    @BeforeTest
    fun setup() {
        Log.logger = mock<Logger>()
    }

    private val account = LegacyAccountDto(UUID.randomUUID().toString()).apply {
        archiveFolderId = BASE_ARCHIVE_FOLDER_ID
        archiveGranularity = ArchiveGranularity.DEFAULT
    }

    @Test
    fun `resolve to single folder when granularity is SINGLE_ARCHIVE_FOLDER`() {
        account.archiveGranularity = ArchiveGranularity.SINGLE_ARCHIVE_FOLDER
        val message = createMessage(year = 2025, month = 11)

        val testSubject = createResolver()
        val result = testSubject.resolveArchiveFolder(account, message)

        assertThat(result).isEqualTo(BASE_ARCHIVE_FOLDER_ID)
    }

    @Test
    fun `resolve to yearly folder when granularity is PER_YEAR_ARCHIVE_FOLDERS`() {
        account.archiveGranularity = ArchiveGranularity.PER_YEAR_ARCHIVE_FOLDERS
        val message = createMessage(year = 2025, month = 11)

        val folderIdResolver = FakeFolderIdResolver(
            folderServerIds = mapOf(BASE_ARCHIVE_FOLDER_ID to "Archive"),
            folderIds = mapOf("Archive/2025" to null),
        )
        val archiveFolderCreator = FakeArchiveFolderCreator(
            createdFolderIds = mapOf("Archive/2025" to YEARLY_FOLDER_ID),
        )
        val testSubject = createResolver(folderIdResolver, archiveFolderCreator)

        val result = testSubject.resolveArchiveFolder(account, message)

        assertThat(result).isEqualTo(YEARLY_FOLDER_ID)
        assertThat(archiveFolderCreator.createdFolders[0].name).isEqualTo("Archive/2025")
    }

    @Test
    fun `resolve to monthly folder when granularity is PER_MONTH_ARCHIVE_FOLDERS`() {
        account.archiveGranularity = ArchiveGranularity.PER_MONTH_ARCHIVE_FOLDERS
        val message = createMessage(year = 2025, month = 11)

        val folderIdResolver = FakeFolderIdResolver(
            folderServerIds = mapOf(
                BASE_ARCHIVE_FOLDER_ID to "Archive",
                YEARLY_FOLDER_ID to "Archive/2025",
            ),
            folderIds = mapOf(
                "Archive/2025" to null,
                "Archive/2025/11" to null,
            ),
        )
        val archiveFolderCreator = FakeArchiveFolderCreator(
            createdFolderIds = mapOf(
                "Archive/2025" to YEARLY_FOLDER_ID,
                "Archive/2025/11" to MONTHLY_FOLDER_ID,
            ),
        )
        val testSubject = createResolver(folderIdResolver, archiveFolderCreator)

        val result = testSubject.resolveArchiveFolder(account, message)

        assertThat(result).isEqualTo(MONTHLY_FOLDER_ID)
    }

    @Test
    fun `return null when archive folder is not configured`() {
        account.archiveFolderId = null
        val message = createMessage(year = 2025, month = 11)

        val testSubject = createResolver()
        val result = testSubject.resolveArchiveFolder(account, message)

        assertThat(result).isNull()
    }

    @Test
    fun `reuse existing yearly folder instead of creating new one`() {
        account.archiveGranularity = ArchiveGranularity.PER_YEAR_ARCHIVE_FOLDERS
        val message = createMessage(year = 2025, month = 11)

        val folderIdResolver = FakeFolderIdResolver(
            folderServerIds = mapOf(BASE_ARCHIVE_FOLDER_ID to "Archive"),
            folderIds = mapOf("Archive/2025" to YEARLY_FOLDER_ID),
        )
        val testSubject = createResolver(folderIdResolver)

        val result = testSubject.resolveArchiveFolder(account, message)

        assertThat(result).isEqualTo(YEARLY_FOLDER_ID)
    }

    @Test
    fun `reuse existing monthly folder instead of creating new one`() {
        account.archiveGranularity = ArchiveGranularity.PER_MONTH_ARCHIVE_FOLDERS
        val message = createMessage(year = 2025, month = 11)

        val folderIdResolver = FakeFolderIdResolver(
            folderServerIds = mapOf(
                BASE_ARCHIVE_FOLDER_ID to "Archive",
                YEARLY_FOLDER_ID to "Archive/2025",
            ),
            folderIds = mapOf(
                "Archive/2025" to YEARLY_FOLDER_ID,
                "Archive/2025/11" to MONTHLY_FOLDER_ID,
            ),
        )
        val testSubject = createResolver(folderIdResolver)

        val result = testSubject.resolveArchiveFolder(account, message)

        assertThat(result).isEqualTo(MONTHLY_FOLDER_ID)
    }

    @Test
    fun `use internal date when available`() {
        account.archiveGranularity = ArchiveGranularity.PER_YEAR_ARCHIVE_FOLDERS
        val message = createMessage(year = 2025, month = 11)
        whenever(message.internalDate).thenReturn(createDate(2024, 5))

        val folderIdResolver = FakeFolderIdResolver(
            folderServerIds = mapOf(BASE_ARCHIVE_FOLDER_ID to "Archive"),
            folderIds = mapOf("Archive/2024" to YEARLY_FOLDER_ID),
        )
        val testSubject = createResolver(folderIdResolver)

        val result = testSubject.resolveArchiveFolder(account, message)

        assertThat(result).isEqualTo(YEARLY_FOLDER_ID)
    }

    @Test
    fun `fall back to sent date when internal date is null`() {
        account.archiveGranularity = ArchiveGranularity.PER_YEAR_ARCHIVE_FOLDERS
        val message = createMessage(year = 2025, month = 11, useInternalDate = false)
        whenever(message.internalDate).thenReturn(null)

        val folderIdResolver = FakeFolderIdResolver(
            folderServerIds = mapOf(BASE_ARCHIVE_FOLDER_ID to "Archive"),
            folderIds = mapOf("Archive/2025" to YEARLY_FOLDER_ID),
        )
        val testSubject = createResolver(folderIdResolver)

        val result = testSubject.resolveArchiveFolder(account, message)

        assertThat(result).isEqualTo(YEARLY_FOLDER_ID)
    }

    @Test
    fun `format month with leading zero for single digit months`() {
        account.archiveGranularity = ArchiveGranularity.PER_MONTH_ARCHIVE_FOLDERS
        val message = createMessage(year = 2025, month = 3)

        val folderIdResolver = FakeFolderIdResolver(
            folderServerIds = mapOf(
                BASE_ARCHIVE_FOLDER_ID to "Archive",
                YEARLY_FOLDER_ID to "Archive/2025",
            ),
            folderIds = mapOf(
                "Archive/2025" to YEARLY_FOLDER_ID,
                "Archive/2025/03" to MONTHLY_FOLDER_ID,
            ),
        )
        val testSubject = createResolver(folderIdResolver)

        val result = testSubject.resolveArchiveFolder(account, message)

        assertThat(result).isEqualTo(MONTHLY_FOLDER_ID)
    }

    @Test
    fun `fall back to base folder when yearly subfolder creation fails`() {
        account.archiveGranularity = ArchiveGranularity.PER_YEAR_ARCHIVE_FOLDERS
        val message = createMessage(year = 2025, month = 11)

        val folderIdResolver = FakeFolderIdResolver(
            folderServerIds = mapOf(BASE_ARCHIVE_FOLDER_ID to "Archive"),
            folderIds = mapOf("Archive/2025" to null),
        )
        val archiveFolderCreator = FakeArchiveFolderCreator(
            failAfterCalls = 0,
        )
        val testSubject = createResolver(folderIdResolver, archiveFolderCreator)

        val result = testSubject.resolveArchiveFolder(account, message)

        assertThat(result).isEqualTo(BASE_ARCHIVE_FOLDER_ID)
    }

    @Test
    fun `fall back to base folder when yearly succeeds but monthly creation fails`() {
        account.archiveGranularity = ArchiveGranularity.PER_MONTH_ARCHIVE_FOLDERS
        val message = createMessage(year = 2025, month = 11)

        val folderIdResolver = FakeFolderIdResolver(
            folderServerIds = mapOf(
                BASE_ARCHIVE_FOLDER_ID to "Archive",
                YEARLY_FOLDER_ID to "Archive/2025",
            ),
            folderIds = mapOf(
                "Archive/2025" to null,
                "Archive/2025/11" to null,
            ),
        )
        val archiveFolderCreator = FakeArchiveFolderCreator(
            createdFolderIds = mapOf("Archive/2025" to YEARLY_FOLDER_ID),
            failAfterCalls = 1,
        )
        val testSubject = createResolver(folderIdResolver, archiveFolderCreator)

        val result = testSubject.resolveArchiveFolder(account, message)

        assertThat(result).isEqualTo(BASE_ARCHIVE_FOLDER_ID)
    }

    @Test
    fun `use current date when both internal and sent dates are null`() {
        account.archiveGranularity = ArchiveGranularity.PER_YEAR_ARCHIVE_FOLDERS
        val message = mock<LocalMessage>().apply {
            whenever(internalDate).thenReturn(null)
            whenever(sentDate).thenReturn(null)
        }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()
        val folderIdResolver = FakeFolderIdResolver(
            folderServerIds = mapOf(BASE_ARCHIVE_FOLDER_ID to "Archive"),
            folderIds = mapOf("Archive/$currentYear" to YEARLY_FOLDER_ID),
        )
        val testSubject = createResolver(folderIdResolver)

        val result = testSubject.resolveArchiveFolder(account, message)

        assertThat(result).isEqualTo(YEARLY_FOLDER_ID)
    }

    @Test
    fun `use account folder delimiter for subfolder paths`() {
        val accountWithDotDelimiter = LegacyAccountDto(UUID.randomUUID().toString()).apply {
            archiveFolderId = BASE_ARCHIVE_FOLDER_ID
            archiveGranularity = ArchiveGranularity.PER_YEAR_ARCHIVE_FOLDERS
            folderPathDelimiter = "."
        }
        val message = createMessage(year = 2025, month = 11)

        val folderIdResolver = FakeFolderIdResolver(
            folderServerIds = mapOf(BASE_ARCHIVE_FOLDER_ID to "Archive"),
            folderIds = mapOf("Archive.2025" to YEARLY_FOLDER_ID),
        )
        val testSubject = createResolver(folderIdResolver)

        val result = testSubject.resolveArchiveFolder(accountWithDotDelimiter, message)

        assertThat(result).isEqualTo(YEARLY_FOLDER_ID)
    }

    private fun createResolver(
        folderIdResolver: FolderIdResolver = FakeFolderIdResolver(),
        archiveFolderCreator: ArchiveFolderCreator = FakeArchiveFolderCreator(),
    ): ArchiveFolderResolver {
        return ArchiveFolderResolver(folderIdResolver, archiveFolderCreator)
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
