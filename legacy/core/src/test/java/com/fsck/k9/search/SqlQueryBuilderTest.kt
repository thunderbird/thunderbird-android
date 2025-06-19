package com.fsck.k9.search

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import net.thunderbird.feature.search.SearchConditionTreeNode
import net.thunderbird.feature.search.api.SearchAttribute
import net.thunderbird.feature.search.api.SearchCondition
import net.thunderbird.feature.search.api.SearchField
import org.junit.Test

class SqlQueryBuilderTest {

    @Test
    fun `should build correct SQL query for NOT operator`() {
        // Arrange
        val condition = SearchCondition(SearchField.SUBJECT, SearchAttribute.CONTAINS, "test")
        val node = SearchConditionTreeNode.Builder(condition)
            .not()
            .build()

        val query = StringBuilder()
        val selectionArgs = mutableListOf<String>()

        // Act
        SqlQueryBuilder.buildWhereClause(node, query, selectionArgs)

        // Assert
        assertThat(query.toString()).isEqualTo("NOT (subject LIKE ?)")
        assertThat(selectionArgs).hasSize(1)
        assertThat(selectionArgs[0]).isEqualTo("%test%")
    }

    @Test
    fun `should build correct SQL query for complex expression with NOT operator`() {
        // Arrange
        val condition1 = SearchCondition(SearchField.SUBJECT, SearchAttribute.CONTAINS, "test")
        val condition2 = SearchCondition(SearchField.SENDER, SearchAttribute.CONTAINS, "example.com")

        val node = SearchConditionTreeNode.Builder(condition1)
            .and(condition2)
            .not()
            .build()

        val query = StringBuilder()
        val selectionArgs = mutableListOf<String>()

        // Act
        SqlQueryBuilder.buildWhereClause(node, query, selectionArgs)

        // Assert
        assertThat(query.toString()).isEqualTo("NOT ((subject LIKE ?) AND (sender_list LIKE ?))")
        assertThat(selectionArgs).hasSize(2)
        assertThat(selectionArgs[0]).isEqualTo("%test%")
        assertThat(selectionArgs[1]).isEqualTo("%example.com%")
    }

    @Test
    fun `should build correct SQL query for NOT operator combined with AND`() {
        // Arrange
        val condition1 = SearchCondition(SearchField.SUBJECT, SearchAttribute.CONTAINS, "test")
        val condition2 = SearchCondition(SearchField.SENDER, SearchAttribute.CONTAINS, "example.com")
        val condition3 = SearchCondition(SearchField.FLAGGED, SearchAttribute.EQUALS, "1")

        val node = SearchConditionTreeNode.Builder(condition1)
            .not()
            .and(condition2)
            .and(condition3)
            .build()

        val query = StringBuilder()
        val selectionArgs = mutableListOf<String>()

        // Act
        SqlQueryBuilder.buildWhereClause(node, query, selectionArgs)

        // Assert
        assertThat(query.toString()).isEqualTo("((NOT (subject LIKE ?)) AND (sender_list LIKE ?)) AND (flagged = ?)")
        assertThat(selectionArgs).hasSize(3)
        assertThat(selectionArgs[0]).isEqualTo("%test%")
        assertThat(selectionArgs[1]).isEqualTo("%example.com%")
        assertThat(selectionArgs[2]).isEqualTo("1")
    }

    @Test
    fun `should build correct SQL query for NOT operator combined with OR`() {
        // Arrange
        val condition1 = SearchCondition(SearchField.SUBJECT, SearchAttribute.CONTAINS, "test")
        val condition2 = SearchCondition(SearchField.SENDER, SearchAttribute.CONTAINS, "example.com")
        val condition3 = SearchCondition(SearchField.FLAGGED, SearchAttribute.EQUALS, "1")

        val node = SearchConditionTreeNode.Builder(condition1)
            .not()
            .or(condition2)
            .or(condition3)
            .build()

        val query = StringBuilder()
        val selectionArgs = mutableListOf<String>()

        // Act
        SqlQueryBuilder.buildWhereClause(node, query, selectionArgs)

        // Assert
        assertThat(query.toString()).isEqualTo("((NOT (subject LIKE ?)) OR (sender_list LIKE ?)) OR (flagged = ?)")
        assertThat(selectionArgs).hasSize(3)
        assertThat(selectionArgs[0]).isEqualTo("%test%")
        assertThat(selectionArgs[1]).isEqualTo("%example.com%")
        assertThat(selectionArgs[2]).isEqualTo("1")
    }

    @Test
    fun `should build correct SQL query for multiple NOT operators`() {
        // Arrange
        val condition1 = SearchCondition(SearchField.SUBJECT, SearchAttribute.CONTAINS, "test")
        val condition2 = SearchCondition(SearchField.SENDER, SearchAttribute.CONTAINS, "example.com")

        val node = SearchConditionTreeNode.Builder(condition1)
            .not()
            .and(
                SearchConditionTreeNode.Builder(condition2)
                    .not()
                    .build(),
            )
            .build()

        val query = StringBuilder()
        val selectionArgs = mutableListOf<String>()

        // Act
        SqlQueryBuilder.buildWhereClause(node, query, selectionArgs)

        // Assert
        assertThat(query.toString()).isEqualTo("(NOT (subject LIKE ?)) AND (NOT (sender_list LIKE ?))")
        assertThat(selectionArgs).hasSize(2)
        assertThat(selectionArgs[0]).isEqualTo("%test%")
        assertThat(selectionArgs[1]).isEqualTo("%example.com%")
    }

    @Test
    fun `should build correct SQL query for NOT operator with MESSAGE_CONTENTS field`() {
        // Arrange
        val condition = SearchCondition(SearchField.MESSAGE_CONTENTS, SearchAttribute.CONTAINS, "test content")

        val node = SearchConditionTreeNode.Builder(condition)
            .not()
            .build()

        val query = StringBuilder()
        val selectionArgs = mutableListOf<String>()

        // Act
        SqlQueryBuilder.buildWhereClause(node, query, selectionArgs)

        // Assert
        assertThat(query.toString()).isEqualTo(
            "NOT (messages.id IN (SELECT docid FROM messages_fulltext WHERE fulltext MATCH ?))",
        )
        assertThat(selectionArgs).hasSize(1)
        assertThat(selectionArgs[0]).isEqualTo("test content")
    }
}
