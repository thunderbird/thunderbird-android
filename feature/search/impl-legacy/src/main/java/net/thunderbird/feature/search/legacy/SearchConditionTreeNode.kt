package net.thunderbird.feature.search.legacy

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.thunderbird.feature.search.legacy.api.SearchAttribute
import net.thunderbird.feature.search.legacy.api.SearchCondition
import net.thunderbird.feature.search.legacy.api.SearchFieldType

/**
 * Represents a node in a boolean expression tree for evaluating search conditions.
 *
 * This tree is used to construct logical queries by combining simple {@link SearchCondition}
 * leaf nodes using logical operators: AND, OR, and NOT.
 *
 * The tree consists of:
 *  - Leaf nodes with `operator == CONDITION`, containing a single {@link SearchCondition}
 *  - Internal nodes with `operator == AND` or `OR`, referencing two child nodes
 *  - Unary nodes with `operator == NOT`, referencing one child node (`left`)
 *
 * The tree supports immutable construction via the {@link Builder} class.
 *
 * Example tree:
 *
 *             OR
 *            /  \
 *         NOT   CONDITION(subject contains "invoice")
 *          |
 *        AND
 *       /   \
 *      A    B
 *
 * Where:
 * - A = CONDITION(from CONTAINS "bob@example.com")
 * - B = CONDITION(to CONTAINS "alice@example.com")
 *
 * Represents logic:
 *   NOT (from CONTAINS "bob@example.com" AND to CONTAINS "alice@example.com")
 *   OR subject CONTAINS "invoice"
 *
 * Use `getLeafSet()` to extract all base conditions for analysis or UI rendering.
 *
 * Example usage (Kotlin):
 *
 * ```kotlin
 * val tree = SearchConditionTreeNode.Builder(conditionA)
 *    .and(conditionB)
 *    .not()
 *    .or(conditionC)
 *    .build()
 * ```
 *
 * This would produce: ((NOT (A AND B)) OR C)
 *
 * @see SearchCondition
 * @see LocalMessageSearch
 */
@Parcelize
class SearchConditionTreeNode private constructor(
    val operator: Operator,
    val condition: SearchCondition? = null,
    var left: SearchConditionTreeNode? = null,
    var right: SearchConditionTreeNode? = null,
) : Parcelable {
    enum class Operator {
        AND,
        NOT,
        OR,
        CONDITION,
    }

    fun getLeafSet(): Set<SearchConditionTreeNode> {
        val leafSet = mutableSetOf<SearchConditionTreeNode>()
        collectLeaves(this, leafSet)
        return leafSet
    }

    private fun collectLeaves(node: SearchConditionTreeNode?, leafSet: MutableSet<SearchConditionTreeNode>) {
        if (node == null) return

        when (node.operator) {
            Operator.CONDITION -> leafSet.add(node)

            Operator.NOT -> {
                // Unary: only traverse left
                collectLeaves(node.left, leafSet)
            }

            Operator.AND, Operator.OR -> {
                collectLeaves(node.left, leafSet)
                collectLeaves(node.right, leafSet)
            }
        }
    }

    override fun toString(): String {
        return when (operator) {
            Operator.AND, Operator.OR -> {
                val leftStr = left?.toString() ?: "null"
                val rightStr = right?.toString() ?: "null"
                "($leftStr ${operator.name} $rightStr)"
            }

            Operator.CONDITION -> condition.toString()
            Operator.NOT -> "(NOT ${left?.toString() ?: "null"})"
        }
    }

    class Builder(
        private var root: SearchConditionTreeNode,
    ) {
        constructor(condition: SearchCondition) : this(SearchConditionTreeNode(Operator.CONDITION, condition))

        private fun validateCondition(condition: SearchCondition) {
            if (condition.field.fieldType == SearchFieldType.CUSTOM &&
                condition.attribute != SearchAttribute.CONTAINS
            ) {
                error("Custom fields can only be used with the CONTAINS attribute")
            }
        }

        private fun validateTree(node: SearchConditionTreeNode?) {
            if (node == null) return

            if (node.operator == Operator.CONDITION) {
                if (node.condition == null) {
                    error("CONDITION nodes must have a condition")
                }
                validateCondition(node.condition)
            } else {
                validateTree(node.left)
                validateTree(node.right)
            }
        }

        fun and(condition: SearchCondition): Builder {
            validateCondition(condition)
            return and(SearchConditionTreeNode(Operator.CONDITION, condition))
        }

        fun and(node: SearchConditionTreeNode): Builder {
            validateTree(node)
            root = SearchConditionTreeNode(
                operator = Operator.AND,
                left = root,
                right = node,
            )
            return this
        }

        fun not(): Builder {
            root = SearchConditionTreeNode(
                operator = Operator.NOT,
                left = root,
            )
            return this
        }

        fun or(condition: SearchCondition): Builder {
            validateCondition(condition)
            return or(SearchConditionTreeNode(Operator.CONDITION, condition))
        }

        fun or(node: SearchConditionTreeNode): Builder {
            validateTree(node)
            root = SearchConditionTreeNode(
                operator = Operator.OR,
                left = root,
                right = node,
            )
            return this
        }

        fun build(): SearchConditionTreeNode {
            return root
        }
    }
}
