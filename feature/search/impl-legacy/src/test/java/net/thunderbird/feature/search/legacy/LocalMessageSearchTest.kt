package net.thunderbird.feature.search.legacy

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.api.SearchAttribute
import net.thunderbird.feature.search.legacy.api.SearchCondition
import org.junit.Test

class LocalMessageSearchTest {

    @Test
    fun `should create an empty search`() {
        // Arrange & Act
        val testSubject = LocalMessageSearch()

        // Assert
        assertThat(testSubject.id).isEqualTo("")
        assertThat(testSubject.isManualSearch).isEqualTo(false)
        assertThat(testSubject.accountUuids).isEmpty()
        assertThat(testSubject.leafSet).isEmpty()
        assertThat(testSubject.conditions).isNotNull()
    }

    @Test
    fun `should add account uuid`() {
        // Arrange
        val testSubject = LocalMessageSearch()
        val uuid = "test-uuid"

        // Act
        testSubject.addAccountUuid(uuid)

        // Assert
        assertThat(testSubject.accountUuids.toList()).containsExactly(uuid)
    }

    @Test
    fun `should add condition with AND operator`() {
        // Arrange
        val testSubject = LocalMessageSearch()
        val condition = SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, "test")

        // Act
        val result = testSubject.and(condition)

        // Assert
        assertThat(result).isNotNull()
        assertThat(result.operator).isEqualTo(SearchConditionTreeNode.Operator.CONDITION)
        assertThat(result.condition).isEqualTo(condition)
        assertThat(testSubject.leafSet).hasSize(1)
        assertThat(testSubject.conditions).isEqualTo(result)
    }

    @Test
    fun `should add condition with OR operator`() {
        // Arrange
        val testSubject = LocalMessageSearch()
        val condition = SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, "test")

        // Act
        val result = testSubject.or(condition)

        // Assert
        assertThat(result).isNotNull()
        assertThat(result.operator).isEqualTo(SearchConditionTreeNode.Operator.CONDITION)
        assertThat(result.condition).isEqualTo(condition)
        assertThat(testSubject.leafSet).hasSize(1)
        assertThat(testSubject.conditions).isEqualTo(result)
    }

    @Test
    fun `should add multiple conditions with AND operator`() {
        // Arrange
        val testSubject = LocalMessageSearch()
        val condition1 = SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, "test1")
        val condition2 = SearchCondition(MessageSearchField.SENDER, SearchAttribute.CONTAINS, "test2")

        // Act
        testSubject.and(condition1)
        val result = testSubject.and(condition2)

        // Assert
        assertThat(result).isNotNull()
        assertThat(result.operator).isEqualTo(SearchConditionTreeNode.Operator.AND)
        assertThat(testSubject.leafSet).hasSize(2)

        // Verify both conditions are in the leaf set
        val conditions = testSubject.leafSet.mapNotNull { it.condition }
        assertThat(conditions).contains(condition1)
        assertThat(conditions).contains(condition2)
    }

    @Test
    fun `should add multiple conditions with OR operator`() {
        // Arrange
        val testSubject = LocalMessageSearch()
        val condition1 = SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, "test1")
        val condition2 = SearchCondition(MessageSearchField.SENDER, SearchAttribute.CONTAINS, "test2")

        // Act
        testSubject.or(condition1)
        val result = testSubject.or(condition2)

        // Assert
        assertThat(result).isNotNull()
        assertThat(result.operator).isEqualTo(SearchConditionTreeNode.Operator.OR)
        assertThat(testSubject.leafSet).hasSize(2)

        // Verify both conditions are in the leaf set
        val conditions = testSubject.leafSet.mapNotNull { it.condition }
        assertThat(conditions).contains(condition1)
        assertThat(conditions).contains(condition2)
    }

    @Test
    fun `should add allowed folder`() {
        // Arrange
        val testSubject = LocalMessageSearch()
        val folderId = 123L

        // Act
        testSubject.addAllowedFolder(folderId)

        // Assert
        assertThat(testSubject.folderIds.toList()).containsExactly(folderId)
    }

    @Test
    fun `should return empty list when no folder conditions exist`() {
        // Arrange
        val testSubject = LocalMessageSearch()

        // Act
        val result = testSubject.folderIds

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `should return remote search arguments when subject condition exists`() {
        // Arrange
        val testSubject = LocalMessageSearch()
        val searchValue = "test query"
        testSubject.and(MessageSearchField.SUBJECT, searchValue, SearchAttribute.CONTAINS)

        // Act
        val result = testSubject.remoteSearchArguments

        // Assert
        assertThat(result).isEqualTo(searchValue)
    }

    @Test
    fun `should return remote search arguments when sender condition exists`() {
        // Arrange
        val testSubject = LocalMessageSearch()
        val searchValue = "test@example.com"
        testSubject.and(MessageSearchField.SENDER, searchValue, SearchAttribute.CONTAINS)

        // Act
        val result = testSubject.remoteSearchArguments

        // Assert
        assertThat(result).isEqualTo(searchValue)
    }

    @Test
    fun `should return null for remote search arguments when no relevant conditions exist`() {
        // Arrange
        val testSubject = LocalMessageSearch()
        testSubject.and(MessageSearchField.FLAGGED, "1", SearchAttribute.EQUALS)

        // Act
        val result = testSubject.remoteSearchArguments

        // Assert
        assertThat(result).isEqualTo(null)
    }

    @Test
    fun `should return true for searchAllAccounts when no accounts are added`() {
        // Arrange
        val testSubject = LocalMessageSearch()

        // Act
        val result = testSubject.searchAllAccounts()

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `should return default condition when conditions is null`() {
        // Arrange
        val testSubject = LocalMessageSearch()

        // Act
        val result = testSubject.conditions

        // Assert
        assertThat(result).isNotNull()
        assertThat(result.operator).isEqualTo(SearchConditionTreeNode.Operator.CONDITION)
        assertThat(result.condition).isNotNull()
        assertThat(result.condition?.field).isEqualTo(MessageSearchField.SUBJECT)
        assertThat(result.condition?.attribute).isEqualTo(SearchAttribute.CONTAINS)
        assertThat(result.condition?.value).isEqualTo("")
    }
}
