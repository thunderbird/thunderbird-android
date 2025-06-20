package net.thunderbird.feature.search.sql

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlinx.parcelize.Parcelize
import net.thunderbird.feature.search.SearchConditionTreeNode
import net.thunderbird.feature.search.api.MessageSearchField
import net.thunderbird.feature.search.api.SearchAttribute
import net.thunderbird.feature.search.api.SearchCondition
import net.thunderbird.feature.search.api.SearchField
import net.thunderbird.feature.search.api.SearchFieldType
import org.junit.Test

class SqlQueryBuilderTest {

    @Parcelize
    data class TestSearchField(
        override val fieldName: String,
        override val fieldType: SearchFieldType,
        override val customQueryTemplate: String? = null,
    ) : SearchField

    @Test
    fun `should build correct SQL query for NOT operator`() {
        // Arrange
        val condition = SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, "test")
        val node = SearchConditionTreeNode.Builder(condition)
            .not()
            .build()

        // Act
        val result = SqlQueryBuilder.Builder()
            .withConditions(node)
            .build()

        // Assert
        assertThat(result.selection).isEqualTo("NOT (subject LIKE ?)")
        assertThat(result.selectionArgs).hasSize(1)
        assertThat(result.selectionArgs[0]).isEqualTo("%test%")
    }

    @Test
    fun `should build correct SQL query for complex expression with NOT operator`() {
        // Arrange
        val condition1 = SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, "test")
        val condition2 = SearchCondition(MessageSearchField.SENDER, SearchAttribute.CONTAINS, "example.com")

        val node = SearchConditionTreeNode.Builder(condition1)
            .and(condition2)
            .not()
            .build()

        // Act
        val result = SqlQueryBuilder.Builder()
            .withConditions(node)
            .build()

        // Assert
        assertThat(result.selection).isEqualTo("NOT ((subject LIKE ?) AND (sender_list LIKE ?))")
        assertThat(result.selectionArgs).hasSize(2)
        assertThat(result.selectionArgs[0]).isEqualTo("%test%")
        assertThat(result.selectionArgs[1]).isEqualTo("%example.com%")
    }

    @Test
    fun `should build correct SQL query for NOT operator combined with AND`() {
        // Arrange
        val condition1 = SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, "test")
        val condition2 = SearchCondition(MessageSearchField.SENDER, SearchAttribute.CONTAINS, "example.com")
        val condition3 = SearchCondition(MessageSearchField.FLAGGED, SearchAttribute.EQUALS, "1")

        val node = SearchConditionTreeNode.Builder(condition1)
            .not()
            .and(condition2)
            .and(condition3)
            .build()

        // Act
        val result = SqlQueryBuilder.Builder()
            .withConditions(node)
            .build()

        // Assert
        assertThat(result.selection).isEqualTo("((NOT (subject LIKE ?)) AND (sender_list LIKE ?)) AND (flagged = ?)")
        assertThat(result.selectionArgs).hasSize(3)
        assertThat(result.selectionArgs[0]).isEqualTo("%test%")
        assertThat(result.selectionArgs[1]).isEqualTo("%example.com%")
        assertThat(result.selectionArgs[2]).isEqualTo("1")
    }

    @Test
    fun `should build correct SQL query for NOT operator combined with OR`() {
        // Arrange
        val condition1 = SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, "test")
        val condition2 = SearchCondition(MessageSearchField.SENDER, SearchAttribute.CONTAINS, "example.com")
        val condition3 = SearchCondition(MessageSearchField.FLAGGED, SearchAttribute.EQUALS, "1")

        val node = SearchConditionTreeNode.Builder(condition1)
            .not()
            .or(condition2)
            .or(condition3)
            .build()

        // Act
        val result = SqlQueryBuilder.Builder()
            .withConditions(node)
            .build()

        // Assert
        assertThat(result.selection).isEqualTo("((NOT (subject LIKE ?)) OR (sender_list LIKE ?)) OR (flagged = ?)")
        assertThat(result.selectionArgs).hasSize(3)
        assertThat(result.selectionArgs[0]).isEqualTo("%test%")
        assertThat(result.selectionArgs[1]).isEqualTo("%example.com%")
        assertThat(result.selectionArgs[2]).isEqualTo("1")
    }

    @Test
    fun `should build correct SQL query for multiple NOT operators`() {
        // Arrange
        val condition1 = SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, "test")
        val condition2 = SearchCondition(MessageSearchField.SENDER, SearchAttribute.CONTAINS, "example.com")

        val node = SearchConditionTreeNode.Builder(condition1)
            .not()
            .and(
                SearchConditionTreeNode.Builder(condition2)
                    .not()
                    .build(),
            )
            .build()

        // Act
        val result = SqlQueryBuilder.Builder()
            .withConditions(node)
            .build()

        // Assert
        assertThat(result.selection).isEqualTo("(NOT (subject LIKE ?)) AND (NOT (sender_list LIKE ?))")
        assertThat(result.selectionArgs).hasSize(2)
        assertThat(result.selectionArgs[0]).isEqualTo("%test%")
        assertThat(result.selectionArgs[1]).isEqualTo("%example.com%")
    }

    @Test
    fun `should build correct SQL query for NOT operator with MESSAGE_CONTENTS field`() {
        // Arrange
        val condition = SearchCondition(MessageSearchField.MESSAGE_CONTENTS, SearchAttribute.CONTAINS, "test content")
        val node = SearchConditionTreeNode.Builder(condition)
            .not()
            .build()

        // Act
        val result = SqlQueryBuilder.Builder()
            .withConditions(node)
            .build()

        // Assert
        assertThat(result.selection).isEqualTo(
            "NOT (messages.id IN (SELECT docid FROM messages_fulltext WHERE fulltext MATCH ?))",
        )
        assertThat(result.selectionArgs).hasSize(1)
        assertThat(result.selectionArgs[0]).isEqualTo("test content")
    }

    @Test
    fun `should build correct SQL query for TEXT field type`() {
        // Arrange
        val textField = TestSearchField("test_text_field", SearchFieldType.TEXT)
        val condition = SearchCondition(textField, SearchAttribute.CONTAINS, "test value")
        val node = SearchConditionTreeNode.Builder(condition).build()

        // Act
        val result = SqlQueryBuilder.Builder()
            .withConditions(node)
            .build()

        // Assert
        assertThat(result.selection).isEqualTo("test_text_field LIKE ?")
        assertThat(result.selectionArgs).hasSize(1)
        assertThat(result.selectionArgs[0]).isEqualTo("%test value%")
    }

    @Test
    fun `should build correct SQL query for NUMBER field type with EQUALS attribute`() {
        // Arrange
        val numberField = TestSearchField("test_number_field", SearchFieldType.NUMBER)
        val condition = SearchCondition(numberField, SearchAttribute.EQUALS, "42")
        val node = SearchConditionTreeNode.Builder(condition).build()

        // Act
        val result = SqlQueryBuilder.Builder()
            .withConditions(node)
            .build()

        // Assert
        assertThat(result.selection).isEqualTo("test_number_field = ?")
        assertThat(result.selectionArgs).hasSize(1)
        assertThat(result.selectionArgs[0]).isEqualTo("42")
    }

    @Test
    fun `should build correct SQL query for NUMBER field type with NOT_EQUALS attribute`() {
        // Arrange
        val numberField = TestSearchField("test_number_field", SearchFieldType.NUMBER)
        val condition = SearchCondition(numberField, SearchAttribute.NOT_EQUALS, "42")
        val node = SearchConditionTreeNode.Builder(condition).build()

        // Act
        val result = SqlQueryBuilder.Builder()
            .withConditions(node)
            .build()

        // Assert
        assertThat(result.selection).isEqualTo("test_number_field != ?")
        assertThat(result.selectionArgs).hasSize(1)
        assertThat(result.selectionArgs[0]).isEqualTo("42")
    }

    @Test
    fun `should build correct SQL query for CUSTOM field type with custom query template`() {
        // Arrange
        val customField = TestSearchField(
            fieldName = "test_custom_field",
            fieldType = SearchFieldType.CUSTOM,
            customQueryTemplate = "custom_table.id IN (SELECT id FROM custom_table WHERE custom_column MATCH ?)",
        )
        val condition = SearchCondition(customField, SearchAttribute.CONTAINS, "custom value")
        val node = SearchConditionTreeNode.Builder(condition).build()

        // Act
        val result = SqlQueryBuilder.Builder()
            .withConditions(node)
            .build()

        // Assert
        assertThat(result.selection)
            .isEqualTo("custom_table.id IN (SELECT id FROM custom_table WHERE custom_column MATCH ?)")
        assertThat(result.selectionArgs).hasSize(1)
        assertThat(result.selectionArgs[0]).isEqualTo("custom value")
    }

    @Test
    fun `should throw exception for CUSTOM field type without custom query template`() {
        // Arrange
        val customField = TestSearchField(
            fieldName = "test_custom_field",
            fieldType = SearchFieldType.CUSTOM,
            customQueryTemplate = null,
        )
        val condition = SearchCondition(customField, SearchAttribute.CONTAINS, "custom value")
        val node = SearchConditionTreeNode.Builder(condition).build()

        // Act & Assert
        assertFailure {
            SqlQueryBuilder.Builder()
                .withConditions(node)
                .build()
        }.isInstanceOf<IllegalArgumentException>()
    }

    @Test
    fun `should throw exception for CUSTOM field type with empty custom query template`() {
        // Arrange
        val customField = TestSearchField(
            fieldName = "test_custom_field",
            fieldType = SearchFieldType.CUSTOM,
            customQueryTemplate = "",
        )
        val condition = SearchCondition(customField, SearchAttribute.CONTAINS, "custom value")
        val node = SearchConditionTreeNode.Builder(condition).build()

        // Act & Assert
        assertFailure {
            SqlQueryBuilder.Builder()
                .withConditions(node)
                .build()
        }.isInstanceOf<IllegalArgumentException>()
    }

    @Test
    fun `should throw exception for CUSTOM field type with non-CONTAINS attribute`() {
        // Arrange
        val customField = TestSearchField(
            fieldName = "test_custom_field",
            fieldType = SearchFieldType.CUSTOM,
            customQueryTemplate = "custom_query",
        )
        val condition = SearchCondition(customField, SearchAttribute.EQUALS, "custom value")
        val node = SearchConditionTreeNode.Builder(condition).build()

        // Act & Assert
        assertFailure {
            SqlQueryBuilder.Builder()
                .withConditions(node)
                .build()
        }.isInstanceOf<IllegalArgumentException>()
    }
}
