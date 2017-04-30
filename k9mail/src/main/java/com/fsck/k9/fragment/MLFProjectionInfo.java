package com.fsck.k9.fragment;


import java.util.Arrays;

import com.fsck.k9.provider.EmailProvider.MessageColumns;
import com.fsck.k9.provider.EmailProvider.SpecialColumns;
import com.fsck.k9.provider.EmailProvider.ThreadColumns;


public final class MLFProjectionInfo {

    static final String[] THREADED_PROJECTION = {
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
            SpecialColumns.FOLDER_NAME,

            SpecialColumns.THREAD_COUNT,
    };

    static final int ID_COLUMN = 0;
    static final int UID_COLUMN = 1;
    static final int INTERNAL_DATE_COLUMN = 2;
    static final int SUBJECT_COLUMN = 3;
    static final int DATE_COLUMN = 4;
    static final int SENDER_LIST_COLUMN = 5;
    static final int TO_LIST_COLUMN = 6;
    static final int CC_LIST_COLUMN = 7;
    static final int READ_COLUMN = 8;
    static final int FLAGGED_COLUMN = 9;
    static final int ANSWERED_COLUMN = 10;
    static final int FORWARDED_COLUMN = 11;
    static final int ATTACHMENT_COUNT_COLUMN = 12;
    static final int FOLDER_ID_COLUMN = 13;
    static final int PREVIEW_TYPE_COLUMN = 14;
    static final int PREVIEW_COLUMN = 15;
    static final int THREAD_ROOT_COLUMN = 16;
    static final int ACCOUNT_UUID_COLUMN = 17;
    static final int FOLDER_NAME_COLUMN = 18;
    static final int THREAD_COUNT_COLUMN = 19;

    static final String[] PROJECTION = Arrays.copyOf(THREADED_PROJECTION,
            THREAD_COUNT_COLUMN);
}
