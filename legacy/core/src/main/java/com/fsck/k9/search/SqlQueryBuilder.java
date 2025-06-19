package com.fsck.k9.search;

import java.util.List;

import net.thunderbird.feature.search.SearchConditionTreeNode;
import net.thunderbird.feature.search.api.SearchAttribute;
import net.thunderbird.feature.search.api.SearchCondition;
import net.thunderbird.feature.search.api.SearchField;
import net.thunderbird.core.logging.legacy.Log;


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
            if (condition.field == SearchField.MESSAGE_CONTENTS) {
                String fulltextQueryString = condition.value;
                if (condition.attribute != SearchAttribute.CONTAINS) {
                    Log.e("message contents can only be matched!");
                }
                query.append("messages.id IN (SELECT docid FROM messages_fulltext WHERE fulltext MATCH ?)");
                selectionArgs.add(fulltextQueryString);
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
        String columnName = null;
        switch (condition.field) {
            case ATTACHMENT_COUNT: {
                columnName = "attachment_count";
                break;
            }
            case BCC: {
                columnName = "bcc_list";
                break;
            }
            case CC: {
                columnName = "cc_list";
                break;
            }
            case FOLDER: {
                columnName = "folder_id";
                break;
            }
            case DATE: {
                columnName = "date";
                break;
            }
            case DELETED: {
                columnName = "deleted";
                break;
            }
            case FLAG: {
                columnName = "flags";
                break;
            }
            case ID: {
                columnName = "id";
                break;
            }
            case REPLY_TO: {
                columnName = "reply_to_list";
                break;
            }
            case SENDER: {
                columnName = "sender_list";
                break;
            }
            case SUBJECT: {
                columnName = "subject";
                break;
            }
            case TO: {
                columnName = "to_list";
                break;
            }
            case UID: {
                columnName = "uid";
                break;
            }
            case INTEGRATE: {
                columnName = "integrate";
                break;
            }
            case NEW_MESSAGE: {
                columnName = "new_message";
                break;
            }
            case READ: {
                columnName = "read";
                break;
            }
            case FLAGGED: {
                columnName = "flagged";
                break;
            }
            case VISIBLE: {
                columnName = "visible";
                break;
            }
            case THREAD_ID: {
                columnName = "threads.root";
                break;
            }
            case MESSAGE_CONTENTS: {
                // Special case handled in buildWhereClauseInternal()
                break;
            }
        }

        if (columnName == null) {
            throw new RuntimeException("Unhandled case");
        }

        return columnName;
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
        switch (field) {
            case ATTACHMENT_COUNT:
            case DATE:
            case DELETED:
            case FOLDER:
            case ID:
            case INTEGRATE:
            case NEW_MESSAGE:
            case THREAD_ID:
            case READ:
            case VISIBLE:
            case FLAGGED: {
                return true;
            }
            default: {
                return false;
            }
        }
    }

    public static String addPrefixToSelection(String[] columnNames, String prefix, String selection) {
        String result = selection;
        for (String columnName : columnNames) {
            result = result.replaceAll("(?<=^|[^\\.])\\b" + columnName + "\\b", prefix + columnName);
        }

        return result;
    }
}
