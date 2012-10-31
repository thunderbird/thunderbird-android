package com.fsck.k9.search;

import java.util.List;

import com.fsck.k9.Account;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Folder.OpenMode;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.mail.store.LocalStore.LocalFolder;
import com.fsck.k9.search.SearchSpecification.SearchCondition;
import com.fsck.k9.search.SearchSpecification.Searchfield;


public class SqlQueryBuilder {
    public static void buildWhereClause(Account account, ConditionsTreeNode node,
            StringBuilder query, List<String> selectionArgs) {
        buildWhereClauseInternal(account, node, query, selectionArgs);
    }

    private static void buildWhereClauseInternal(Account account, ConditionsTreeNode node,
            StringBuilder query, List<String> selectionArgs) {
        if (node == null) {
            return;
        }

        if (node.mLeft == null && node.mRight == null) {
            SearchCondition condition = node.mCondition;
            switch (condition.field) {
                case FOLDER: {
                    String folderName = condition.value;
                    long folderId = getFolderId(account, folderName);
                    query.append("folder_id = ?");
                    selectionArgs.add(Long.toString(folderId));
                    break;
                }
                default: {
                    appendCondition(condition, query, selectionArgs);
                }
            }
        } else {
            query.append("(");
            buildWhereClauseInternal(account, node.mLeft, query, selectionArgs);
            query.append(") ");
            query.append(node.mValue.name());
            query.append(" (");
            buildWhereClauseInternal(account, node.mRight, query, selectionArgs);
            query.append(")");
        }
    }

    private static void appendCondition(SearchCondition condition, StringBuilder query,
            List<String> selectionArgs) {
        query.append(getColumnName(condition));
        appendExprRight(condition, query, selectionArgs);
    }

    private static long getFolderId(Account account, String folderName) {
        long folderId = 0;
        try {
            LocalStore localStore = account.getLocalStore();
            LocalFolder folder = localStore.getFolder(folderName);
            folder.open(OpenMode.READ_ONLY);
            folderId = folder.getId();
        } catch (MessagingException e) {
            //FIXME
            e.printStackTrace();
        }

        return folderId;
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
            case FOLDER: {
                columnName = "folder_id";
                break;
            }
            case ID: {
                columnName = "id";
                break;
            }
            case MESSAGE_CONTENTS: {
                columnName = "text_content";
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
            case THREAD_ROOT: {
                columnName = "thread_root";
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
        }

        if (columnName == null) {
            throw new RuntimeException("Unhandled case");
        }

        return columnName;
    }

    private static void appendExprRight(SearchCondition condition, StringBuilder query,
            List<String> selectionArgs) {
        String value = condition.value;
        Searchfield field = condition.field;

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

    private static boolean isNumberColumn(Searchfield field) {
        switch (field) {
            case ATTACHMENT_COUNT:
            case DATE:
            case DELETED:
            case FOLDER:
            case ID:
            case INTEGRATE:
            case THREAD_ROOT: {
                return true;
            }
            default: {
                return false;
            }
        }
    }
}
