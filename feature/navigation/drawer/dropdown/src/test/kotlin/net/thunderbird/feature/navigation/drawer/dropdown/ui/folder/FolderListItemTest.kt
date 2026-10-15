package net.thunderbird.feature.navigation.drawer.dropdown.ui.folder

import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.performClick
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.onNodeWithText
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import app.k9mail.legacy.ui.folder.FolderNameFormatter
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.test.Test
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID
import net.thunderbird.feature.mail.folder.FolderType
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayFolder

internal class FolderListItemTest : ComposeTest() {

    @Test
    fun `clicking a folder should open it`() {
        val folder = createFolder(isPlaceholder = false)
        val clickedFolders = mutableListOf<DisplayFolder>()
        setFolderListItem(folder, onClick = { clickedFolders += it })

        onNodeWithText(FOLDER_NAME).performClick()

        assertThat(clickedFolders).containsExactly(folder)
    }

    @Test
    fun `clicking a placeholder folder should not open it`() {
        val clickedFolders = mutableListOf<DisplayFolder>()
        setFolderListItem(createFolder(isPlaceholder = true), onClick = { clickedFolders += it })

        onNodeWithText(FOLDER_NAME).performClick()

        assertThat(clickedFolders).isEmpty()
    }

    private fun setFolderListItem(folder: MailDisplayFolder, onClick: (DisplayFolder) -> Unit) {
        setContentWithTheme {
            FolderListItem(
                displayFolder = folder,
                onClick = onClick,
                showStarredCount = false,
                folderNameFormatter = FolderNameFormatter(LocalResources.current),
                selectedFolderId = null,
            )
        }
    }

    private fun createFolder(isPlaceholder: Boolean) = MailDisplayFolder(
        accountId = ACCOUNT_ID,
        folder = Folder(id = 1, name = FOLDER_NAME, type = FolderType.REGULAR, isLocalOnly = false),
        isInTopGroup = false,
        unreadMessageCount = 0,
        starredMessageCount = 0,
        pathDelimiter = "/",
        isPlaceholder = isPlaceholder,
    )

    private companion object {
        const val FOLDER_NAME = "Turing"
    }
}
