package com.fsck.k9.fragment;


import java.util.Arrays;

import com.fsck.k9.provider.EmailProvider.MessageColumns;
import com.fsck.k9.provider.EmailProvider.SpecialColumns;
import com.fsck.k9.provider.EmailProvider.ThreadColumns;


public final class MLFProjectionInfo {

    public static final String[] THREADED_PROJECTION = {
            MessageColumns.ID,
            MessageColumns.UID,
            MessageColumns.INTERNAL_DATE,
            MessageColumns.SUBJECT,
            MessageColumns.DATE,
            MessageColumns.SENDER_LIST,
            MessageColumns.TO_LIST,
            MessageColumns.CC_LIST,
            MessageColumns.READ,
            MessageColumns.FLAGGED,
            MessageColumns.ANSWERED,
            MessageColumns.FORWARDED,
            MessageColumns.ATTACHMENT_COUNT,
            MessageColumns.FOLDER_ID,
            MessageColumns.PREVIEW_TYPE,
            MessageColumns.PREVIEW,
            ThreadColumns.ROOT,
            SpecialColumns.ACCOUNT_UUID,
            SpecialColumns.FOLDER_SERVER_ID,

            SpecialColumns.THREAD_COUNT,
    };

    public static final int ID_COLUMN = 0;
    public static final int UID_COLUMN = 1;
    public static final int INTERNAL_DATE_COLUMN = 2;
    public static final int SUBJECT_COLUMN = 3;
    public static final int DATE_COLUMN = 4;
    public static final int SENDER_LIST_COLUMN = 5;
    public static final int TO_LIST_COLUMN = 6;
    public static final int CC_LIST_COLUMN = 7;
    public static final int READ_COLUMN = 8;
    public static final int FLAGGED_COLUMN = 9;
    public static final int ANSWERED_COLUMN = 10;
    public static final int FORWARDED_COLUMN = 11;
    public static final int ATTACHMENT_COUNT_COLUMN = 12;
    public static final int FOLDER_ID_COLUMN = 13;
    public static final int PREVIEW_TYPE_COLUMN = 14;
    public static final int PREVIEW_COLUMN = 15;
    public static final int THREAD_ROOT_COLUMN = 16;
    public static final int ACCOUNT_UUID_COLUMN = 17;
    public static final int FOLDER_SERVER_ID_COLUMN = 18;
    public static final int THREAD_COUNT_COLUMN = 19;

    public static final String[] PROJECTION = Arrays.copyOf(THREADED_PROJECTION,
            THREAD_COUNT_COLUMN);
}
