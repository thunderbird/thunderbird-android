package com.fsck.k9.search;

import java.util.List;

import app.k9mail.legacy.search.ConditionsTreeNode;
import app.k9mail.legacy.search.api.SearchAttribute;
import app.k9mail.legacy.search.api.SearchCondition;
import app.k9mail.legacy.search.api.SearchField;
import timber.log.Timber;


public class SqlQueryBuilder {
    public static void buildWhereClause(ConditionsTreeNode node, StringBuilder query, List<String> selectionArgs) {
        buildWhereClauseInternal(node, query, selectionArgs);
    }

    private static void buildWhereClauseInternal(ConditionsTreeNode node, StringBuilder query,
        List<String> selectionArgs) {

        if (node == null) {
            query.append("1");
            return;
        }

        if (node.mLeft == null && node.mRight == null) {
            SearchCondition condition = node.mCondition;
            if (condition.field == SearchField.MESSAGE_CONTENTS) {
                String fulltextQueryString = condition.value;
                if (condition.attribute != SearchAttribute.CONTAINS) {
                    Timber.e("message contents can only be matched!");
                }
                query.append("messages.id IN (SELECT docid FROM messages_fulltext WHERE fulltext MATCH ?)");
                selectionArgs.add(fulltextQueryString);
            } else {
                appendCondition(condition, query, selectionArgs);
            }
        } else {
            query.append("(");
            buildWhereClauseInternal(node.mLeft, query, selectionArgs);
            query.append(") ");
            query.append(node.mValue.name());
            query.append(" (");
            buildWhereClauseInternal(node.mRight, query, selectionArgs);
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
            case NOT_CONTAINS:
                query.append("NOT ");
                //$FALL-THROUGH$
            case CONTAINS: {
                query.append("LIKE ?");
                selectionArg = "%" + value + "%";
                break;
            }
            case NOT_STARTSWITH:
                query.append("NOT ");
                //$FALL-THROUGH$
            case STARTSWITH: {
                query.append("LIKE ?");
                selectionArg = "%" + value;
                break;
            }
            case NOT_ENDSWITH:
                query.append("NOT ");
                //$FALL-THROUGH$
            case ENDSWITH: {
                query.append("LIKE ?");
                selectionArg = value + "%";
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
