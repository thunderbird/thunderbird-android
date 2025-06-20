package net.thunderbird.feature.search.sql;

import java.util.List;

import net.thunderbird.feature.search.SearchConditionTreeNode;
import net.thunderbird.feature.search.api.SearchAttribute;
import net.thunderbird.feature.search.api.SearchCondition;
import net.thunderbird.feature.search.api.SearchField;
import net.thunderbird.feature.search.api.SearchFieldType;


public class SqlQueryBuilder {
    public static void buildWhereClause(SearchConditionTreeNode node, StringBuilder query, List<String> selectionArgs) {
        buildWhereClauseInternal(node, query, selectionArgs);
    }

    private static void buildWhereClauseInternal(SearchConditionTreeNode node, StringBuilder query,
        List<String> selectionArgs) {

        if (node == null) {
            query.append("1");
            return;
        }

        if (node.getLeft() == null && node.getRight() == null) {
            SearchCondition condition = node.getCondition();
            if (condition.field.getFieldType() == SearchFieldType.CUSTOM) {
                String fullQueryString = condition.value;
                if (condition.attribute != SearchAttribute.CONTAINS) {
                    throw new IllegalArgumentException("Custom fields only support CONTAINS");
                }
                if (condition.field.getCustomQueryTemplate() == null || condition.field.getCustomQueryTemplate().isEmpty()) {
                    throw new IllegalArgumentException("Custom field has no query template!");
                }

                query.append(condition.field.getCustomQueryTemplate());
                selectionArgs.add(fullQueryString);
            } else {
                appendCondition(condition, query, selectionArgs);
            }
        } else if (node.getOperator() == SearchConditionTreeNode.Operator.NOT) {
            query.append("NOT (");
            buildWhereClauseInternal(node.getLeft(), query, selectionArgs);
            query.append(")");
        } else {
            // Handle binary operators (AND, OR)
            query.append("(");
            buildWhereClauseInternal(node.getLeft(), query, selectionArgs);
            query.append(") ");
            query.append(node.getOperator().name());
            query.append(" (");
            buildWhereClauseInternal(node.getRight(), query, selectionArgs);
            query.append(")");
        }
    }

    private static void appendCondition(SearchCondition condition, StringBuilder query,
            List<String> selectionArgs) {
        query.append(getColumnName(condition));
        appendExprRight(condition, query, selectionArgs);
    }

    private static String getColumnName(SearchCondition condition) {
        return condition.field.getFieldName();
    }

    private static void appendExprRight(SearchCondition condition, StringBuilder query,
            List<String> selectionArgs) {
        String value = condition.value;
        SearchField field = condition.field;

        query.append(" ");
        String selectionArg = null;
        switch (condition.attribute) {
            case CONTAINS: {
                query.append("LIKE ?");
                selectionArg = "%" + value + "%";
                break;
            }
            case NOT_EQUALS: {
                if (isNumberColumn(field)) {
                    query.append("!= ?");
                } else {
                    query.append("NOT LIKE ?");
                }
                selectionArg = value;
                break;
            }
            case EQUALS: {
                if (isNumberColumn(field)) {
                    query.append("= ?");
                } else {
                    query.append("LIKE ?");
                }
                selectionArg = value;
                break;
            }
        }

        if (selectionArg == null) {
            throw new RuntimeException("Unhandled case");
        }

        selectionArgs.add(selectionArg);
    }

    private static boolean isNumberColumn(SearchField field) {
        return field.getFieldType() == SearchFieldType.NUMBER;
    }

    public static String addPrefixToSelection(String[] columnNames, String prefix, String selection) {
        String result = selection;
        for (String columnName : columnNames) {
            result = result.replaceAll("(?<=^|[^\\.])\\b" + columnName + "\\b", prefix + columnName);
        }

        return result;
    }
}
