package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID
import net.thunderbird.feature.account.UnifiedAccountId
import net.thunderbird.feature.mail.folder.FolderRole
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData
import app.k9mail.legacy.ui.folder.DisplayFolder as LegacyDisplayFolder

internal class GetDisplayFoldersForAccountTest {

    @Test
    fun `should return account folders when account id is regular`() = runTest {
        val accountId = ACCOUNT_ID
        val legacyDisplayFolderFlow = MutableStateFlow(LEGACY_DISPLAY_FOLDERS)
        val displayFolderRepository = FakeDisplayFolderRepository(legacyDisplayFolderFlow)
        val testSubject = GetDisplayFoldersForAccount(
            displayFolderRepository = displayFolderRepository,
            unifiedFolderRepository = FakeUnifiedFolderRepository(),
        )

        val result = testSubject(accountId).first()

        assertThat(result).isEqualTo(DISPLAY_ACCOUNT_FOLDERS)
    }

    @Test
    fun `should return all unified folders in drawer order when account id is unified`() = runTest {
        val accountId = UnifiedAccountId
        val legacyDisplayFolderFlow = MutableStateFlow(LEGACY_DISPLAY_FOLDERS)
        val displayFolderRepository = FakeDisplayFolderRepository(legacyDisplayFolderFlow)
        val unifiedFolderRepository = FakeUnifiedFolderRepository(createUnifiedFolderFlows())
        val testSubject = GetDisplayFoldersForAccount(
            displayFolderRepository = displayFolderRepository,
            unifiedFolderRepository = unifiedFolderRepository,
        )

        val result = testSubject(accountId).first()

        assertThat(result).isEqualTo(
            listOf(
                createUnifiedFolder(FolderRole.INBOX),
                createUnifiedFolder(FolderRole.OUTBOX),
                createUnifiedFolder(FolderRole.DRAFTS),
                createUnifiedFolder(FolderRole.SENT),
                createUnifiedFolder(FolderRole.ARCHIVE),
                createUnifiedFolder(FolderRole.SPAM),
                createUnifiedFolder(FolderRole.TRASH),
            ),
        )
    }

    @Test
    fun `should only emit new list when account folders emit new items`() = runTest {
        val accountId = ACCOUNT_ID
        val legacyDisplayFolderFlow = MutableStateFlow(LEGACY_DISPLAY_FOLDERS)
        val displayFolderRepository = FakeDisplayFolderRepository(legacyDisplayFolderFlow)
        val unifiedFolderFlows = createUnifiedFolderFlows()
        val unifiedFolderRepository = FakeUnifiedFolderRepository(unifiedFolderFlows)
        val testSubject = GetDisplayFoldersForAccount(
            displayFolderRepository = displayFolderRepository,
            unifiedFolderRepository = unifiedFolderRepository,
        )

        testSubject(accountId).test {
            assertThat(awaitItem()).isEqualTo(DISPLAY_ACCOUNT_FOLDERS)

            legacyDisplayFolderFlow.emit(LEGACY_DISPLAY_FOLDERS_2)

            assertThat(awaitItem()).isEqualTo(DISPLAY_ACCOUNT_FOLDERS_2)

            unifiedFolderFlows.getValue(FolderRole.INBOX).emit(
                createUnifiedFolder(FolderRole.INBOX, unreadMessageCount = 3),
            )
        }
    }

    @Test
    fun `should only emit new list when unified account folders emit new items`() = runTest {
        val accountId = UnifiedAccountId
        val legacyDisplayFolderFlow = MutableStateFlow(LEGACY_DISPLAY_FOLDERS)
        val displayFolderRepository = FakeDisplayFolderRepository(legacyDisplayFolderFlow)
        val unifiedFolderFlows = createUnifiedFolderFlows()
        val unifiedFolderRepository = FakeUnifiedFolderRepository(unifiedFolderFlows)
        val testSubject = GetDisplayFoldersForAccount(
            displayFolderRepository = displayFolderRepository,
            unifiedFolderRepository = unifiedFolderRepository,
        )
        val updatedSentFolder = createUnifiedFolder(FolderRole.SENT, unreadMessageCount = 3)

        testSubject(accountId).test {
            val initialFolders = awaitItem()

            legacyDisplayFolderFlow.emit(LEGACY_DISPLAY_FOLDERS_2)
            unifiedFolderFlows.getValue(FolderRole.SENT).emit(updatedSentFolder)

            assertThat(awaitItem()).isEqualTo(
                initialFolders.map { if (it.folderId == updatedSentFolder.folderId) updatedSentFolder else it },
            )
        }
    }

    private fun createUnifiedFolderFlows(): Map<FolderRole, MutableStateFlow<UnifiedDisplayFolder>> {
        return FolderRole.entries.associateWith { MutableStateFlow(createUnifiedFolder(it)) }
    }

    private fun createUnifiedFolder(
        role: FolderRole,
        unreadMessageCount: Int = 2,
    ): UnifiedDisplayFolder {
        return UnifiedDisplayFolder(
            role = role,
            unreadMessageCount = unreadMessageCount,
            starredMessageCount = 2,
        )
    }

    private companion object {
        val LEGACY_DISPLAY_FOLDERS = listOf(
            LegacyDisplayFolder(
                folder = FakeData.FOLDER,
                isInTopGroup = false,
                unreadMessageCount = 0,
                starredMessageCount = 0,
                pathDelimiter = "/",
            ),
            LegacyDisplayFolder(
                folder = FakeData.FOLDER.copy(
                    id = 2,
                    name = "Folder 2",
                ),
                isInTopGroup = false,
                unreadMessageCount = 1,
                starredMessageCount = 0,
                pathDelimiter = "/",
            ),
        )

        val LEGACY_DISPLAY_FOLDERS_2 = LEGACY_DISPLAY_FOLDERS + LegacyDisplayFolder(
            folder = FakeData.FOLDER.copy(
                id = 3,
                name = "Folder 3",
            ),
            isInTopGroup = false,
            unreadMessageCount = 0,
            starredMessageCount = 0,
            pathDelimiter = "/",
        )

        val DISPLAY_ACCOUNT_FOLDERS = listOf<DisplayFolder>(
            MailDisplayFolder(
                accountId = ACCOUNT_ID,
                folder = FakeData.FOLDER,
                isInTopGroup = false,
                unreadMessageCount = 0,
                starredMessageCount = 0,
                pathDelimiter = "/",
            ),
            MailDisplayFolder(
                accountId = ACCOUNT_ID,
                folder = FakeData.FOLDER.copy(
                    id = 2,
                    name = "Folder 2",
                ),
                isInTopGroup = false,
                unreadMessageCount = 1,
                starredMessageCount = 0,
                pathDelimiter = "/",
            ),
        )

        val DISPLAY_ACCOUNT_FOLDERS_2 = DISPLAY_ACCOUNT_FOLDERS + MailDisplayFolder(
            accountId = ACCOUNT_ID,
            folder = FakeData.FOLDER.copy(
                id = 3,
                name = "Folder 3",
            ),
            isInTopGroup = false,
            unreadMessageCount = 0,
            starredMessageCount = 0,
            pathDelimiter = "/",
        )
    }
}
