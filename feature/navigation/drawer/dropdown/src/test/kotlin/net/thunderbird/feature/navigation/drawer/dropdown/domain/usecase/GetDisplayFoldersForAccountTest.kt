package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_RAW
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolderType
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData
import app.k9mail.legacy.ui.folder.DisplayFolder as LegacyDisplayFolder

internal class GetDisplayFoldersForAccountTest {

    @Test
    fun `should return account folders when account id is regular`() = runTest {
        val accountId = ACCOUNT_ID_RAW
        val legacyDisplayFolderFlow = MutableStateFlow(LEGACY_DISPLAY_FOLDERS)
        val displayFolderRepository = FakeDisplayFolderRepository(legacyDisplayFolderFlow)
        val unifiedFolderFlow = MutableStateFlow(DISPLAY_UNIFIED_FOLDER)
        val unifiedFolderRepository = FakeUnifiedFolderRepository(unifiedFolderFlow)
        val testSubject = GetDisplayFoldersForAccount(
            displayFolderRepository = displayFolderRepository,
            unifiedFolderRepository = unifiedFolderRepository,
        )

        val result = testSubject(accountId).first()

        assertThat(result).isEqualTo(DISPLAY_ACCOUNT_FOLDERS)
    }

    @Test
    fun `should return unifed account folders when account id is unified`() = runTest {
        val accountId = UnifiedDisplayAccount.UNIFIED_ACCOUNT_ID
        val legacyDisplayFolderFlow = MutableStateFlow(LEGACY_DISPLAY_FOLDERS)
        val displayFolderRepository = FakeDisplayFolderRepository(legacyDisplayFolderFlow)
        val unifiedFolderFlow = MutableStateFlow(DISPLAY_UNIFIED_FOLDER)
        val unifiedFolderRepository = FakeUnifiedFolderRepository(unifiedFolderFlow)
        val testSubject = GetDisplayFoldersForAccount(
            displayFolderRepository = displayFolderRepository,
            unifiedFolderRepository = unifiedFolderRepository,
        )

        val result = testSubject(accountId).first()

        assertThat(result).isEqualTo(DISPLAY_UNIFIED_FOLDERS)
    }

    @Test
    fun `should only emit new list when account folders emit new items`() = runTest {
        val accountId = ACCOUNT_ID_RAW
        val legacyDisplayFolderFlow = MutableStateFlow(LEGACY_DISPLAY_FOLDERS)
        val displayFolderRepository = FakeDisplayFolderRepository(legacyDisplayFolderFlow)
        val unifiedFolderFlow = MutableStateFlow(DISPLAY_UNIFIED_FOLDER)
        val unifiedFolderRepository = FakeUnifiedFolderRepository(unifiedFolderFlow)
        val testSubject = GetDisplayFoldersForAccount(
            displayFolderRepository = displayFolderRepository,
            unifiedFolderRepository = unifiedFolderRepository,
        )

        testSubject(accountId).test {
            assertThat(awaitItem()).isEqualTo(DISPLAY_ACCOUNT_FOLDERS)

            legacyDisplayFolderFlow.emit(LEGACY_DISPLAY_FOLDERS_2)

            assertThat(awaitItem()).isEqualTo(DISPLAY_ACCOUNT_FOLDERS_2)

            unifiedFolderFlow.emit(DISPLAY_UNIFIED_FOLDER_2)
        }
    }

    @Test
    fun `should only emit new list when unified account folders emit new items`() = runTest {
        val accountId = UnifiedDisplayAccount.UNIFIED_ACCOUNT_ID
        val legacyDisplayFolderFlow = MutableStateFlow(LEGACY_DISPLAY_FOLDERS)
        val displayFolderRepository = FakeDisplayFolderRepository(legacyDisplayFolderFlow)
        val unifiedFolderFlow = MutableStateFlow(DISPLAY_UNIFIED_FOLDER)
        val unifiedFolderRepository = FakeUnifiedFolderRepository(unifiedFolderFlow)
        val testSubject = GetDisplayFoldersForAccount(
            displayFolderRepository = displayFolderRepository,
            unifiedFolderRepository = unifiedFolderRepository,
        )

        testSubject(accountId).test {
            assertThat(awaitItem()).isEqualTo(DISPLAY_UNIFIED_FOLDERS)

            legacyDisplayFolderFlow.emit(LEGACY_DISPLAY_FOLDERS_2)
            unifiedFolderFlow.emit(DISPLAY_UNIFIED_FOLDER_2)

            assertThat(awaitItem()).isEqualTo(listOf(DISPLAY_UNIFIED_FOLDER_2))
        }
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

        val DISPLAY_UNIFIED_FOLDER = UnifiedDisplayFolder(
            id = "unified_inbox",
            unifiedType = UnifiedDisplayFolderType.INBOX,
            unreadMessageCount = 2,
            starredMessageCount = 2,
        )

        val DISPLAY_UNIFIED_FOLDER_2 = UnifiedDisplayFolder(
            id = "unified_inbox",
            unifiedType = UnifiedDisplayFolderType.INBOX,
            unreadMessageCount = 3,
            starredMessageCount = 3,
        )

        val DISPLAY_UNIFIED_FOLDERS = listOf(DISPLAY_UNIFIED_FOLDER)

        val DISPLAY_ACCOUNT_FOLDERS = listOf<DisplayFolder>(
            MailDisplayFolder(
                accountId = ACCOUNT_ID_RAW,
                folder = FakeData.FOLDER,
                isInTopGroup = false,
                unreadMessageCount = 0,
                starredMessageCount = 0,
                pathDelimiter = "/",
            ),
            MailDisplayFolder(
                accountId = ACCOUNT_ID_RAW,
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
            accountId = ACCOUNT_ID_RAW,
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
