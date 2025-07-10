package net.thunderbird.feature.search.sql

import net.thunderbird.feature.search.SearchConditionTreeNode
import net.thunderbird.feature.search.api.SearchAttribute
import net.thunderbird.feature.search.api.SearchCondition
import net.thunderbird.feature.search.api.SearchFieldType

/**
 * Builds a SQL query string based on a search condition tree and creates the selection arguments.
 *
 * This class constructs a SQL WHERE clause from a tree of search conditions, allowing for complex
 * logical expressions using AND, OR, and NOT operators. It supports custom fields with templates
 * for specific query formats.
 *
 * Example usage:
 * ```
 * val query = SqlWhereClause.Builder()
 *     .withConditions(searchConditionTree)
 *     .build()
 * ```
 */
class SqlWhereClause private constructor(
    val selection: String,
    val selectionArgs: List<String>,
) {
    class Builder {
        private var root: SearchConditionTreeNode? = null

        /**
         * Sets the root of the search condition tree.
         *
         * This method is used to specify the root node of the search condition tree that will be
         * used to build the SQL query. It will replace any previously set conditions.
         *
         * @param node The root node of the search condition tree.
         */
        fun withConditions(node: SearchConditionTreeNode): Builder {
            root = node
            return this
        }

        /**
         * Builds the SQL query string based on the provided conditions and creates the selection arguments.
         *
         * @return The constructed SQL query string.
         */
        fun build(): SqlWhereClause {
            val arguments = mutableListOf<String>()
            val query = StringBuilder()
            buildWhereClause(root, query, arguments)

            return SqlWhereClause(
                selection = query.toString(),
                selectionArgs = arguments,
            )
        }

        private fun buildWhereClause(
            node: SearchConditionTreeNode?,
            query: StringBuilder,
            selectionArgs: MutableList<String>,
        ) {
            if (node == null) {
                query.append("1")
                return
            }

            if (node.left == null && node.right == null) {
                val condition = node.condition ?: error("Leaf node missing condition")

                if (condition.field.fieldType == SearchFieldType.CUSTOM) {
                    require(condition.attribute == SearchAttribute.CONTAINS) {
                        "Custom fields only support CONTAINS"
                    }
                    require(
                        !(
                            condition.field.customQueryTemplate == null ||
                                condition.field.customQueryTemplate!!.isEmpty()
                            ),
                    ) {
                        "Custom field has no query template!"
                    }
                    query.append(condition.field.customQueryTemplate)
                    selectionArgs.add(condition.value)
                } else {
                    appendCondition(condition, query, selectionArgs)
                }
            } else if (node.operator == SearchConditionTreeNode.Operator.NOT) {
                query.append("NOT (")
                buildWhereClause(node.left, query, selectionArgs)
                query.append(")")
            } else {
                // Handle binary operators (AND, OR)
                query.append("(")
                buildWhereClause(node.left, query, selectionArgs)
                query.append(") ")
                query.append(node.operator.name)
                query.append(" (")
                buildWhereClause(node.right, query, selectionArgs)
                query.append(")")
            }
        }

        private fun appendCondition(
            condition: SearchCondition,
            query: StringBuilder,
            selectionArgs: MutableList<String>,
        ) {
            query.append(condition.field.fieldName)
            appendExpressionRight(condition, query, selectionArgs)
        }

        private fun appendExpressionRight(
            condition: SearchCondition,
            query: StringBuilder,
            selectionArgs: MutableList<String>,
        ) {
            val value = condition.value
            val field = condition.field

            query.append(" ")

            val selectionArg: String = when (condition.attribute) {
                SearchAttribute.CONTAINS -> {
                    query.append("LIKE ?")
                    "%$value%"
                }

                SearchAttribute.NOT_EQUALS -> {
                    if (field.fieldType == SearchFieldType.NUMBER) {
                        query.append("!= ?")
                        value
                    } else {
                        query.append("NOT LIKE ?")
                        value
                    }
                }

                SearchAttribute.EQUALS -> {
                    if (field.fieldType == SearchFieldType.NUMBER) {
                        query.append("= ?")
                        value
                    } else {
                        query.append("LIKE ?")
                        value
                    }
                }
            }

            selectionArgs.add(selectionArg)
        }
    }

    companion object Companion {
        // TODO: This is a workaround for ambiguous column names in the selection. Find a better solution.
        fun addPrefixToSelection(columnNames: Array<String>, prefix: String?, selection: String): String {
            var result = selection
            for (columnName in columnNames) {
                result = result.replace(("(?<=^|[^\\.])\\b$columnName\\b").toRegex(), "$prefix$columnName")
            }

            return result
        }
    }
}
