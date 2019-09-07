package com.fsck.k9.fragment

import android.content.res.Resources
import android.database.Cursor
import com.fsck.k9.Account
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.mail.Address
import com.fsck.k9.ui.R

class MessageListItemExtractor(
        private val account: Account,
        private val cursor: Cursor,
        private val messageHelper: MessageHelper,
        private val res: Resources
) {

    val ccAddresses: Array<Address>
        get() = Address.unpack(cursor.getString(MLFProjectionInfo.CC_LIST_COLUMN))

    val displayName: CharSequence
        get() = messageHelper.getDisplayName(account, fromAddresses, toAddresses)

    val date: Long get() = cursor.getLong(MLFProjectionInfo.DATE_COLUMN)

    val flagged: Boolean get() = cursor.getInt(MLFProjectionInfo.FLAGGED_COLUMN) == 1

    val fromAddresses: Array<Address>
        get() = Address.unpack(cursor.getString(MLFProjectionInfo.SENDER_LIST_COLUMN))

    val hasAttachments: Boolean get() = cursor.getInt(MLFProjectionInfo.ATTACHMENT_COUNT_COLUMN) > 0

    val threadCount: Int get() = cursor.getInt(MLFProjectionInfo.THREAD_COUNT_COLUMN)

    val toAddresses: Array<Address>
        get() = Address.unpack(cursor.getString(MLFProjectionInfo.TO_LIST_COLUMN))

    fun subject(threadCount: Int): String {
        return MlfUtils.buildSubject(cursor.getString(MLFProjectionInfo.SUBJECT_COLUMN),
                res.getString(R.string.general_no_subject), threadCount)
    }
}