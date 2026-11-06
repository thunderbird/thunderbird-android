package com.fsck.k9.ui.messagelist

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.fsck.k9.FakeLegacyAccount
import kotlinx.coroutines.test.runTest
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.android.account.fake.FakeLegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderServerId
import net.thunderbird.feature.mail.folder.api.data.FolderError
import net.thunderbird.feature.mail.folder.api.data.repository.FolderQueryRepository
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import org.junit.Test

class DeletedFolderDetectorTest {
    private val account = FakeLegacyAccount.create()
    private val testSubject = DeletedFolderDetector(
        accountManager = FakeLegacyAccountManager(listOf(account)),
        folderQueryRepository = FakeFolderQueryRepository(presentFolderIds = setOf(PRESENT_FOLDER_ID)),
    )

    @Test
    fun `should return folder ID when the single displayed folder no longer exists`() = runTest {
        val search = createSearch(DELETED_FOLDER_ID)

        val result = testSubject.findDeletedFolderId(search)

        assertThat(result).isEqualTo(DELETED_FOLDER_ID)
    }

    @Test
    fun `should return null when the displayed folder exists`() = runTest {
        val search = createSearch(PRESENT_FOLDER_ID)

        val result = testSubject.findDeletedFolderId(search)

        assertThat(result).isNull()
    }

    @Test
    fun `should return null when search targets several folders`() = runTest {
        val search = createSearch(DELETED_FOLDER_ID, PRESENT_FOLDER_ID)

        val result = testSubject.findDeletedFolderId(search)

        assertThat(result).isNull()
    }

    @Test
    fun `should return null when search targets no folder`() = runTest {
        val search = createSearch()

        val result = testSubject.findDeletedFolderId(search)

        assertThat(result).isNull()
    }

    private fun createSearch(vararg folderIds: Long) = LocalMessageSearch().apply {
        addAccountId(account.id)
        folderIds.forEach { addAllowedFolder(it) }
    }

    private class FakeFolderQueryRepository(
        private val presentFolderIds: Set<Long>,
    ) : FolderQueryRepository {
        override suspend fun findById(accountId: AccountId, folderId: Long): Outcome<Folder?, FolderError> =
            error("Not used in this test")

        override suspend fun findFolderServerIdById(
            accountId: AccountId,
            folderId: Long,
        ): Outcome<FolderServerId?, FolderError> = error("Not used in this test")

        override suspend fun findIdByServerId(
            accountId: AccountId,
            folderServerId: FolderServerId,
        ): Outcome<Long?, FolderError> = error("Not used in this test")

        override suspend fun isPresent(accountId: AccountId, folderId: Long): Boolean = folderId in presentFolderIds
    }

    private companion object {
        const val PRESENT_FOLDER_ID = 1L
        const val DELETED_FOLDER_ID = 2L
    }
}
