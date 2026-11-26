package net.thunderbird.feature.search.legacy

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test
import net.thunderbird.feature.mail.folder.FolderRole
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.api.SearchAttribute
import net.thunderbird.feature.search.legacy.api.SearchCondition

class LocalMessageSearchConditionsForAccountTest {

    @Test
    fun `unified search should select the account's folder of the requested type`() {
        // Arrange
        val search = LocalMessageSearch.createUnifiedSearch(FolderRole.SENT)
        val folderIds = mapOf(FolderRole.INBOX to INBOX_FOLDER_ID, FolderRole.SENT to SENT_FOLDER_ID)

        // Act
        val result = search.conditionsForAccount { folderType -> folderIds[folderType] }

        // Assert
        assertThat(result.toString()).isEqualTo(folderCondition(SENT_FOLDER_ID).toString())
    }

    @Test
    fun `unified search should skip an account without the requested folder`() {
        val search = LocalMessageSearch.createUnifiedSearch(FolderRole.ARCHIVE)

        val result = search.conditionsForAccount { null }

        assertThat(result).isNull()
    }

    @Test
    fun `unified search should keep additional conditions`() {
        // Arrange
        val search = LocalMessageSearch.createUnifiedSearch(FolderRole.INBOX).apply {
            and(MessageSearchField.SUBJECT, "invoice", SearchAttribute.CONTAINS)
        }

        // Act
        val result = search.conditionsForAccount { INBOX_FOLDER_ID }

        // Assert
        val expected = SearchConditionTreeNode.Builder(
            SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, "invoice"),
        ).and(folderCondition(INBOX_FOLDER_ID)).build()
        assertThat(result.toString()).isEqualTo(expected.toString())
    }

    @Test
    fun `unified search saved with the integrate condition should ignore it`() {
        // Arrange
        val search = LocalMessageSearch.createUnifiedSearch(FolderRole.INBOX).apply {
            and(MessageSearchField.INTEGRATE, "1", SearchAttribute.EQUALS)
        }

        // Act
        val result = search.conditionsForAccount { INBOX_FOLDER_ID }

        // Assert
        assertThat(result.toString()).isEqualTo(folderCondition(INBOX_FOLDER_ID).toString())
    }

    @Test
    fun `account search should return its conditions unchanged`() {
        // Arrange
        val search = LocalMessageSearch().apply { addAllowedFolder(INBOX_FOLDER_ID) }

        // Act
        val result = search.conditionsForAccount { error("Not used for account searches") }

        // Assert
        assertThat(result).isSameInstanceAs(search.conditions)
    }

    private fun folderCondition(folderId: Long) =
        SearchCondition(MessageSearchField.FOLDER, SearchAttribute.EQUALS, folderId.toString())

    private companion object {
        const val INBOX_FOLDER_ID = 1L
        const val SENT_FOLDER_ID = 2L
    }
}
