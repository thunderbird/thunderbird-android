package net.thunderbird.feature.search

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import net.thunderbird.feature.search.api.SearchAttribute
import net.thunderbird.feature.search.api.SearchCondition
import net.thunderbird.feature.search.api.SearchField
import org.junit.Test

class SearchConditionTreeNodeTest {

    @Test
    fun `should create a node with a condition`() {
        // Arrange
        val condition = SearchCondition(SearchField.SUBJECT, SearchAttribute.CONTAINS, "test")

        // Act
        val node = SearchConditionTreeNode.Builder(condition).build()

        // Assert
        assertThat(node.operator).isEqualTo(SearchConditionTreeNode.Operator.CONDITION)
        assertThat(node.condition).isEqualTo(condition)
        assertThat(node.left).isEqualTo(null)
        assertThat(node.right).isEqualTo(null)
    }

    @Test
    fun `should create a node with AND operator`() {
        // Arrange
        val condition1 = SearchCondition(SearchField.SUBJECT, SearchAttribute.CONTAINS, "test")
        val condition2 = SearchCondition(SearchField.SENDER, SearchAttribute.CONTAINS, "example.com")

        // Act
        val node = SearchConditionTreeNode.Builder(condition1)
            .and(condition2)
            .build()

        // Assert
        assertThat(node.operator).isEqualTo(SearchConditionTreeNode.Operator.AND)
        assertThat(node.condition).isEqualTo(null)
        assertThat(node.left).isNotNull()
        assertThat(node.right).isNotNull()

        // Left node should be the first condition
        assertThat(node.left?.operator).isEqualTo(SearchConditionTreeNode.Operator.CONDITION)
        assertThat(node.left?.condition).isEqualTo(condition1)

        // Right node should be the second condition
        assertThat(node.right?.operator).isEqualTo(SearchConditionTreeNode.Operator.CONDITION)
        assertThat(node.right?.condition).isEqualTo(condition2)
    }

    @Test
    fun `should create a node with OR operator`() {
        // Arrange
        val condition1 = SearchCondition(SearchField.SUBJECT, SearchAttribute.CONTAINS, "test")
        val condition2 = SearchCondition(SearchField.SENDER, SearchAttribute.CONTAINS, "example.com")

        // Act
        val node = SearchConditionTreeNode.Builder(condition1)
            .or(condition2)
            .build()

        // Assert
        assertThat(node.operator).isEqualTo(SearchConditionTreeNode.Operator.OR)
        assertThat(node.condition).isEqualTo(null)
        assertThat(node.left).isNotNull()
        assertThat(node.right).isNotNull()

        // Left node should be the first condition
        assertThat(node.left?.operator).isEqualTo(SearchConditionTreeNode.Operator.CONDITION)
        assertThat(node.left?.condition).isEqualTo(condition1)

        // Right node should be the second condition
        assertThat(node.right?.operator).isEqualTo(SearchConditionTreeNode.Operator.CONDITION)
        assertThat(node.right?.condition).isEqualTo(condition2)
    }

    @Test
    fun `should create a complex tree with nested conditions`() {
        // Arrange
        val condition1 = SearchCondition(SearchField.SUBJECT, SearchAttribute.CONTAINS, "test")
        val condition2 = SearchCondition(SearchField.SENDER, SearchAttribute.CONTAINS, "example.com")
        val condition3 = SearchCondition(SearchField.FLAGGED, SearchAttribute.EQUALS, "1")

        // Act
        val node = SearchConditionTreeNode.Builder(condition1)
            .and(
                SearchConditionTreeNode.Builder(condition2)
                    .or(condition3)
                    .build(),
            )
            .build()

        // Assert
        assertThat(node.operator).isEqualTo(SearchConditionTreeNode.Operator.AND)
        assertThat(node.condition).isEqualTo(null)
        assertThat(node.left).isNotNull()
        assertThat(node.right).isNotNull()

        // Left node should be the first condition
        assertThat(node.left?.operator).isEqualTo(SearchConditionTreeNode.Operator.CONDITION)
        assertThat(node.left?.condition).isEqualTo(condition1)

        // Right node should be an OR node
        assertThat(node.right?.operator).isEqualTo(SearchConditionTreeNode.Operator.OR)
        assertThat(node.right?.condition).isEqualTo(null)

        // Right node's left child should be condition2
        assertThat(node.right?.left?.operator).isEqualTo(SearchConditionTreeNode.Operator.CONDITION)
        assertThat(node.right?.left?.condition).isEqualTo(condition2)

        // Right node's right child should be condition3
        assertThat(node.right?.right?.operator).isEqualTo(SearchConditionTreeNode.Operator.CONDITION)
        assertThat(node.right?.right?.condition).isEqualTo(condition3)
    }

    @Test
    fun `should collect all leaf nodes`() {
        // Arrange
        val condition1 = SearchCondition(SearchField.SUBJECT, SearchAttribute.CONTAINS, "test")
        val condition2 = SearchCondition(SearchField.SENDER, SearchAttribute.CONTAINS, "example.com")
        val condition3 = SearchCondition(SearchField.FLAGGED, SearchAttribute.EQUALS, "1")

        val node = SearchConditionTreeNode.Builder(condition1)
            .and(
                SearchConditionTreeNode.Builder(condition2)
                    .or(condition3)
                    .build(),
            )
            .build()

        // Act
        val leafSet = node.getLeafSet()

        // Assert
        assertThat(leafSet.size).isEqualTo(3)

        // The leaf set should contain nodes with all three conditions
        val conditions = leafSet.mapNotNull { it.condition }
        assertThat(conditions).contains(condition1)
        assertThat(conditions).contains(condition2)
        assertThat(conditions).contains(condition3)
    }
}
