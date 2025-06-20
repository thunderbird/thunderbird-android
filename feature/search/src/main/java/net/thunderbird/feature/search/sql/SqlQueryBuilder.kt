package net.thunderbird.feature.search.sql

import net.thunderbird.feature.search.SearchConditionTreeNode
import net.thunderbird.feature.search.api.SearchAttribute
import net.thunderbird.feature.search.api.SearchCondition
import net.thunderbird.feature.search.api.SearchField
import net.thunderbird.feature.search.api.SearchFieldType

object SqlQueryBuilder {
    fun buildWhereClause(node: SearchConditionTreeNode?, query: StringBuilder, selectionArgs: MutableList<String?>) {
        buildWhereClauseInternal(node, query, selectionArgs)
    }

    private fun buildWhereClauseInternal(
        node: SearchConditionTreeNode?, query: StringBuilder,
        selectionArgs: MutableList<String?>
    ) {
        if (node == null) {
            query.append("1")
            return
        }

        if (node.left == null && node.right == null) {
            val condition = node.condition
            if (condition!!.field.fieldType == SearchFieldType.CUSTOM) {
                val fullQueryString = condition.value
                require(condition.attribute == SearchAttribute.CONTAINS) { "Custom fields only support CONTAINS" }
                require(!(condition.field.customQueryTemplate == null || condition.field.customQueryTemplate!!.isEmpty())) { "Custom field has no query template!" }

                query.append(condition.field.customQueryTemplate)
                selectionArgs.add(fullQueryString)
            } else {
                SqlQueryBuilder.appendCondition(condition, query, selectionArgs)
            }
        } else if (node.operator == SearchConditionTreeNode.Operator.NOT) {
            query.append("NOT (")
            buildWhereClauseInternal(node.left, query, selectionArgs)
            query.append(")")
        } else {
            // Handle binary operators (AND, OR)
            query.append("(")
            buildWhereClauseInternal(node.left, query, selectionArgs)
            query.append(") ")
            query.append(node.operator.name)
            query.append(" (")
            buildWhereClauseInternal(node.right, query, selectionArgs)
            query.append(")")
        }
    }

    private fun appendCondition(
        condition: SearchCondition, query: StringBuilder,
        selectionArgs: MutableList<String?>
    ) {
        query.append(getColumnName(condition))
        appendExprRight(condition, query, selectionArgs)
    }

    private fun getColumnName(condition: SearchCondition): String {
        return condition.field.fieldName
    }

    private fun appendExprRight(
        condition: SearchCondition, query: StringBuilder,
        selectionArgs: MutableList<String?>
    ) {
        val value = condition.value
        val field = condition.field

        query.append(" ")
        var selectionArg: String? = null
        when (condition.attribute) {
            SearchAttribute.CONTAINS -> {
                query.append("LIKE ?")
                selectionArg = "%" + value + "%"
            }

            SearchAttribute.NOT_EQUALS -> {
                if (isNumberColumn(field)) {
                    query.append("!= ?")
                } else {
                    query.append("NOT LIKE ?")
                }
                selectionArg = value
            }

            SearchAttribute.EQUALS -> {
                if (isNumberColumn(field)) {
                    query.append("= ?")
                } else {
                    query.append("LIKE ?")
                }
                selectionArg = value
            }
        }

        if (selectionArg == null) {
            throw RuntimeException("Unhandled case")
        }

        selectionArgs.add(selectionArg)
    }

    private fun isNumberColumn(field: SearchField): Boolean {
        return field.fieldType == SearchFieldType.NUMBER
    }

    fun addPrefixToSelection(columnNames: Array<String?>, prefix: String?, selection: String): String {
        var result = selection
        for (columnName in columnNames) {
            result = result.replace(("(?<=^|[^\\.])\\b$columnName\\b").toRegex(), "$prefix$columnName")
        }

        return result
    }
}
