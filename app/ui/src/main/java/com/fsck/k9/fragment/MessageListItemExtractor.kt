package com.fsck.k9.fragment

import android.content.res.Resources
import android.database.Cursor
import com.fsck.k9.ui.R

class MessageListItemExtractor(
        private val cursor: Cursor,
        private val res: Resources
) {

    val date: Long get() = cursor.getLong(MLFProjectionInfo.DATE_COLUMN)

    val flagged: Boolean get() = cursor.getInt(MLFProjectionInfo.FLAGGED_COLUMN) == 1

    val hasAttachments: Boolean get() = cursor.getInt(MLFProjectionInfo.ATTACHMENT_COUNT_COLUMN) > 0

    val threadCount: Int get() = cursor.getInt(MLFProjectionInfo.THREAD_COUNT_COLUMN)

    fun subject(threadCount: Int): String {
        return MlfUtils.buildSubject(cursor.getString(MLFProjectionInfo.SUBJECT_COLUMN),
                res.getString(R.string.general_no_subject), threadCount)
    }
}