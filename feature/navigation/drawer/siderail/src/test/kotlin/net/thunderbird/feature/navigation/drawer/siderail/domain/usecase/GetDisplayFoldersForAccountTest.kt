package net.thunderbird.feature.navigation.drawer.siderail.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_RAW
import net.thunderbird.feature.navigation.drawer.siderail.domain.entity.DisplayAccountFolder
import net.thunderbird.feature.navigation.drawer.siderail.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.siderail.domain.entity.DisplayUnifiedFolder
import net.thunderbird.feature.navigation.drawer.siderail.domain.entity.DisplayUnifiedFolderType
import net.thunderbird.feature.navigation.drawer.siderail.ui.FakeData
import app.k9mail.legacy.ui.folder.DisplayFolder as LegacyDisplayFolder

internal class GetDisplayFoldersForAccountTest {

    @Test
    fun `should return only account folders when includeUnifiedFolders is false`() = runTest {
        val accountId = ACCOUNT_ID_RAW
        val legacyDisplayFolderFlow = MutableStateFlow(LEGACY_DISPLAY_FOLDERS)
        val displayFolderRepository = FakeDisplayFolderRepository(legacyDisplayFolderFlow)
        val unifiedFolderFlow = MutableStateFlow(DISPLAY_UNIFIED_FOLDER)
        val unifiedFolderRepository = FakeUnifiedFolderRepository(unifiedFolderFlow)
        val testSubject = GetDisplayFoldersForAccount(
            displayFolderRepository = displayFolderRepository,
            unifiedFolderRepository = unifiedFolderRepository,
        )

        val result = testSubject(accountId, includeUnifiedFolders = false).first()

        assertThat(result).isEqualTo(DISPLAY_ACCOUNT_FOLDERS)
    }

    @Test
    fun `should return account folders and unified folders when includeUnifiedFolders is true`() = runTest {
        val accountId = ACCOUNT_ID_RAW
        val legacyDisplayFolderFlow = MutableStateFlow(LEGACY_DISPLAY_FOLDERS)
        val displayFolderRepository = FakeDisplayFolderRepository(legacyDisplayFolderFlow)
        val unifiedFolderFlow = MutableStateFlow(DISPLAY_UNIFIED_FOLDER)
        val unifiedFolderRepository = FakeUnifiedFolderRepository(unifiedFolderFlow)
        val testSubject = GetDisplayFoldersForAccount(
            displayFolderRepository = displayFolderRepository,
            unifiedFolderRepository = unifiedFolderRepository,
        )

        val result = testSubject(accountId, includeUnifiedFolders = true).first()

        assertThat(result).isEqualTo(DISPLAY_UNIFIED_FOLDERS + DISPLAY_ACCOUNT_FOLDERS)
    }

    @Test
    fun `should emit new list when account folders or unified folders emit new items`() = runTest {
        val accountId = ACCOUNT_ID_RAW
        val legacyDisplayFolderFlow = MutableStateFlow(LEGACY_DISPLAY_FOLDERS)
        val displayFolderRepository = FakeDisplayFolderRepository(legacyDisplayFolderFlow)
        val unifiedFolderFlow = MutableStateFlow(DISPLAY_UNIFIED_FOLDER)
        val unifiedFolderRepository = FakeUnifiedFolderRepository(unifiedFolderFlow)
        val testSubject = GetDisplayFoldersForAccount(
            displayFolderRepository = displayFolderRepository,
            unifiedFolderRepository = unifiedFolderRepository,
        )

        testSubject(accountId, includeUnifiedFolders = true).test {
            assertThat(awaitItem()).isEqualTo(DISPLAY_UNIFIED_FOLDERS + DISPLAY_ACCOUNT_FOLDERS)

            legacyDisplayFolderFlow.emit(LEGACY_DISPLAY_FOLDERS_2)

            assertThat(awaitItem()).isEqualTo(DISPLAY_UNIFIED_FOLDERS + DISPLAY_ACCOUNT_FOLDERS_2)

            unifiedFolderFlow.emit(DISPLAY_UNIFIED_FOLDER_2)

            assertThat(awaitItem()).isEqualTo(listOf(DISPLAY_UNIFIED_FOLDER_2) + DISPLAY_ACCOUNT_FOLDERS_2)
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

        val DISPLAY_UNIFIED_FOLDER = DisplayUnifiedFolder(
            id = "unified_inbox",
            unifiedType = DisplayUnifiedFolderType.INBOX,
            unreadMessageCount = 2,
            starredMessageCount = 2,
        )

        val DISPLAY_UNIFIED_FOLDER_2 = DisplayUnifiedFolder(
            id = "unified_inbox",
            unifiedType = DisplayUnifiedFolderType.INBOX,
            unreadMessageCount = 3,
            starredMessageCount = 3,
        )

        val DISPLAY_UNIFIED_FOLDERS = listOf(DISPLAY_UNIFIED_FOLDER)

        val DISPLAY_ACCOUNT_FOLDERS = listOf<DisplayFolder>(
            DisplayAccountFolder(
                accountId = ACCOUNT_ID_RAW,
                folder = FakeData.FOLDER,
                isInTopGroup = false,
                unreadMessageCount = 0,
                starredMessageCount = 0,
            ),
            DisplayAccountFolder(
                accountId = ACCOUNT_ID_RAW,
                folder = FakeData.FOLDER.copy(
                    id = 2,
                    name = "Folder 2",
                ),
                isInTopGroup = false,
                unreadMessageCount = 1,
                starredMessageCount = 0,
            ),
        )

        val DISPLAY_ACCOUNT_FOLDERS_2 = DISPLAY_ACCOUNT_FOLDERS + DisplayAccountFolder(
            accountId = ACCOUNT_ID_RAW,
            folder = FakeData.FOLDER.copy(
                id = 3,
                name = "Folder 3",
            ),
            isInTopGroup = false,
            unreadMessageCount = 0,
            starredMessageCount = 0,
        )
    }
}
