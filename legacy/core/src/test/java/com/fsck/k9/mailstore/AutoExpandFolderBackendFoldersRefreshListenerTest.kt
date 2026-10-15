package com.fsck.k9.mailstore

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import com.fsck.k9.FakeLegacyAccount
import kotlinx.coroutines.test.runTest
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.fake.FakeLegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderServerId
import net.thunderbird.feature.mail.folder.api.data.FolderError
import net.thunderbird.feature.mail.folder.api.data.repository.FolderQueryRepository
import org.junit.Test

class AutoExpandFolderBackendFoldersRefreshListenerTest {
    private val account = FakeLegacyAccount.create().copy(inboxFolderId = INBOX_FOLDER_ID)
    private val accountManager = FakeLegacyAccountManager(listOf(account))
    private val folderQueryRepository = FakeFolderQueryRepository(
        folderIdsByServerId = mapOf(IMPORTED_FOLDER_SERVER_ID to IMPORTED_FOLDER_ID),
        presentFolderIds = setOf(INBOX_FOLDER_ID, IMPORTED_FOLDER_ID, USER_FOLDER_ID),
    )

    private val testSubject = AutoExpandFolderBackendFoldersRefreshListener(
        accountManager = accountManager,
        accountId = account.id,
        folderQueryRepository = folderQueryRepository,
    )

    @Test
    fun `refresh should apply the imported auto-expand folder and clear it`() = runTest {
        // Arrange
        accountManager.replace(account.copy(importedAutoExpandFolder = IMPORTED_FOLDER_SERVER_ID))

        // Act
        refreshFolderList()

        // Assert
        assertThat(accountManager.findById(account.id)).isNotNull().all {
            prop(LegacyAccount::autoExpandFolderId).isEqualTo(IMPORTED_FOLDER_ID)
            prop(LegacyAccount::importedAutoExpandFolder).isNull()
        }
    }

    @Test
    fun `later refresh should keep the auto-expand folder chosen after the import`() = runTest {
        // Arrange
        accountManager.replace(account.copy(importedAutoExpandFolder = IMPORTED_FOLDER_SERVER_ID))
        refreshFolderList()
        accountManager.updateSync(account.id) { it.copy(autoExpandFolderId = USER_FOLDER_ID) }

        // Act
        refreshFolderList()

        // Assert
        assertThat(accountManager.findById(account.id)).isNotNull()
            .prop(LegacyAccount::autoExpandFolderId).isEqualTo(USER_FOLDER_ID)
    }

    @Test
    fun `refresh should keep account changes made while folders are queried`() = runTest {
        // Arrange
        accountManager.replace(account.copy(importedAutoExpandFolder = IMPORTED_FOLDER_SERVER_ID))
        folderQueryRepository.onQuery = {
            // Simulates another component saving the account, e.g. a refreshed OAuth state
            accountManager.updateSync(account.id) { it.copy(oAuthState = "refreshed") }
        }

        // Act
        refreshFolderList()

        // Assert
        assertThat(accountManager.findById(account.id)).isNotNull().all {
            prop(LegacyAccount::oAuthState).isEqualTo("refreshed")
            prop(LegacyAccount::autoExpandFolderId).isEqualTo(IMPORTED_FOLDER_ID)
        }
    }

    private suspend fun refreshFolderList() {
        testSubject.onBeforeFolderListRefresh()
        testSubject.onAfterFolderListRefresh()
    }

    private class FakeFolderQueryRepository(
        private val folderIdsByServerId: Map<String, Long>,
        private val presentFolderIds: Set<Long>,
    ) : FolderQueryRepository {
        var onQuery: () -> Unit = {}

        override suspend fun findById(accountId: AccountId, folderId: Long): Outcome<Folder?, FolderError> =
            error("Not used in this test")

        override suspend fun findFolderServerIdById(
            accountId: AccountId,
            folderId: Long,
        ): Outcome<FolderServerId?, FolderError> = error("Not used in this test")

        override suspend fun findIdByServerId(
            accountId: AccountId,
            folderServerId: FolderServerId,
        ): Outcome<Long?, FolderError> {
            onQuery()
            return Outcome.success(folderIdsByServerId[folderServerId.serverId])
        }

        override suspend fun isPresent(accountId: AccountId, folderId: Long): Boolean {
            onQuery()
            return folderId in presentFolderIds
        }
    }

    private companion object {
        const val INBOX_FOLDER_ID = 1L
        const val IMPORTED_FOLDER_ID = 2L
        const val USER_FOLDER_ID = 3L
        const val IMPORTED_FOLDER_SERVER_ID = "Imported"
    }
}
