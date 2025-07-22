package net.thunderbird.feature.search.legacy

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.api.SearchAttribute
import net.thunderbird.feature.search.legacy.api.SearchCondition
import net.thunderbird.feature.search.legacy.api.SearchField
import net.thunderbird.feature.search.legacy.api.SearchFieldType
import org.junit.Test

class SearchConditionTreeNodeTest {

    data class TestSearchField(
        override val fieldName: String,
        override val fieldType: SearchFieldType,
        override val customQueryTemplate: String? = null,
    ) : SearchField

    @Test
    fun `should create a node with a condition`() {
        // Arrange
        val condition = SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, "test")

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
        val condition1 = SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, "test")
        val condition2 = SearchCondition(MessageSearchField.SENDER, SearchAttribute.CONTAINS, "example.com")

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
        val condition1 = SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, "test")
        val condition2 = SearchCondition(MessageSearchField.SENDER, SearchAttribute.CONTAINS, "example.com")

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
        val condition1 = SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, "test")
        val condition2 = SearchCondition(MessageSearchField.SENDER, SearchAttribute.CONTAINS, "example.com")
        val condition3 = SearchCondition(MessageSearchField.FLAGGED, SearchAttribute.EQUALS, "1")

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
        val condition1 = SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, "test")
        val condition2 = SearchCondition(MessageSearchField.SENDER, SearchAttribute.CONTAINS, "example.com")
        val condition3 = SearchCondition(MessageSearchField.FLAGGED, SearchAttribute.EQUALS, "1")

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

    @Test
    fun `should create a node with NOT operator`() {
        // Arrange
        val condition = SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, "test")

        // Act
        val node = SearchConditionTreeNode.Builder(condition)
            .not()
            .build()

        // Assert
        assertThat(node.operator).isEqualTo(SearchConditionTreeNode.Operator.NOT)
        assertThat(node.condition).isEqualTo(null)
        assertThat(node.left).isNotNull()
        assertThat(node.right).isEqualTo(null)

        // Left node should be the condition
        assertThat(node.left?.operator).isEqualTo(SearchConditionTreeNode.Operator.CONDITION)
        assertThat(node.left?.condition).isEqualTo(condition)
    }

    @Test
    fun `should throw exception when adding condition with custom field and non-CONTAINS attribute`() {
        // Arrange
        val condition = SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, "test")
        val builder = SearchConditionTreeNode.Builder(condition)

        val customField = TestSearchField(
            fieldName = "test_custom_field",
            fieldType = SearchFieldType.CUSTOM,
            customQueryTemplate = "custom_query_template",
        )

        // Act & Assert
        assertFailure {
            builder.and(SearchCondition(customField, SearchAttribute.EQUALS, "test value"))
        }.isInstanceOf<IllegalStateException>()
    }

    @Test
    fun `should throw exception when adding condition with custom field and non-CONTAINS attribute using or`() {
        // Arrange
        val condition = SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, "test")
        val builder = SearchConditionTreeNode.Builder(condition)

        val customField = TestSearchField(
            fieldName = "test_custom_field",
            fieldType = SearchFieldType.CUSTOM,
            customQueryTemplate = "custom_query_template",
        )

        // Act & Assert
        assertFailure {
            builder.or(SearchCondition(customField, SearchAttribute.EQUALS, "test value"))
        }.isInstanceOf<IllegalStateException>()
    }

    @Test
    fun `should throw exception when adding node with invalid condition`() {
        // Arrange
        val customField = TestSearchField(
            fieldName = "test_custom_field",
            fieldType = SearchFieldType.CUSTOM,
            customQueryTemplate = "custom_query_template",
        )
        val validCondition = SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, "valid")
        val validBuilder = SearchConditionTreeNode.Builder(validCondition)

        // Add an invalid condition to the builder
        val invalidCondition = SearchCondition(customField, SearchAttribute.EQUALS, "invalid")

        // Act & Assert
        // This should throw an exception when trying to add the invalid condition to the builder
        assertFailure {
            validBuilder.and(invalidCondition)
        }.isInstanceOf<IllegalStateException>()
    }
}
