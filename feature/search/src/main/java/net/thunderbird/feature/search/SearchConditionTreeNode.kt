package net.thunderbird.feature.search

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.thunderbird.feature.search.api.SearchCondition

/**
 * Represents a node in a boolean expression tree for evaluating search conditions.
 *
 * This data structure is used to construct complex logical queries by combining
 * simple `SearchCondition` objects using logical operators like `AND` and `OR`.
 *
 * Each node in the tree is one of:
 * - A leaf node: `operator == CONDITION`, contains a single `SearchCondition`
 * - An internal node: `operator == AND` or `OR`, with left and right children
 *
 * Example tree:
 *
 *      OR
 *     /  \
 *   AND   CONDITION(subject CONTAINS "invoice")
 *  /   \
 * A     B
 *
 * Where:
 * - A = CONDITION(from CONTAINS "bob@example.com")
 * - B = CONDITION(to CONTAINS "alice@example.com")
 *
 * Represents logic:
 *   (from CONTAINS "bob@example.com" AND to CONTAINS "alice@example.com")
 *   OR subject CONTAINS "invoice"
 *
 * Use `getLeafSet()` to extract all base conditions for analysis or UI rendering.
 *
 *  TODO implement NOT as a node again
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

        if (node.left == null && node.right == null) {
            leafSet.add(node)
        } else {
            collectLeaves(node.left, leafSet)
            collectLeaves(node.right, leafSet)
        }
    }

    override fun toString(): String {
        return when (operator) {
            Operator.CONDITION -> condition.toString()
            Operator.AND, Operator.OR -> {
                val leftStr = left?.toString() ?: "null"
                val rightStr = right?.toString() ?: "null"
                "($leftStr ${operator.name} $rightStr)"
            }
        }
    }

    class Builder(
        private var root: SearchConditionTreeNode,
    ) {

        constructor(condition: SearchCondition) : this(SearchConditionTreeNode(Operator.CONDITION, condition))

        fun and(condition: SearchCondition): Builder {
            return and(SearchConditionTreeNode(Operator.CONDITION, condition))
        }

        fun and(node: SearchConditionTreeNode): Builder {
            root = SearchConditionTreeNode(
                operator = Operator.AND,
                left = root,
                right = node,
            )
            return this
        }

        fun or(condition: SearchCondition): Builder {
            return or(SearchConditionTreeNode(Operator.CONDITION, condition))
        }

        fun or(node: SearchConditionTreeNode): Builder {
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
