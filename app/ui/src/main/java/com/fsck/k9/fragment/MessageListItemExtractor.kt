package com.fsck.k9.fragment

import android.content.res.Resources
import android.database.Cursor
import androidx.annotation.ColorInt
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.fragment.MLFProjectionInfo.ACCOUNT_UUID_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.ANSWERED_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.ATTACHMENT_COUNT_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.CC_LIST_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.DATE_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.FLAGGED_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.FOLDER_SERVER_ID_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.FORWARDED_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.ID_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.PREVIEW_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.PREVIEW_TYPE_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.READ_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.SENDER_LIST_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.SUBJECT_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.THREAD_COUNT_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.THREAD_ROOT_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.TO_LIST_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.UID_COLUMN
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.mail.Address
import com.fsck.k9.mailstore.DatabasePreviewType
import com.fsck.k9.ui.R

class MessageListItemExtractor(
        private val preferences: Preferences,
        val cursor: Cursor,
        private val messageHelper: MessageHelper,
        private val res: Resources
) {

    private val ccAddresses: Array<Address>
        get() = Address.unpack(cursor.getString(CC_LIST_COLUMN))
    private val ccMe: Boolean get() = messageHelper.toMe(account, ccAddresses)
    private val fromAddresses: Array<Address>
        get() = Address.unpack(cursor.getString(SENDER_LIST_COLUMN))
    private val fromMe: Boolean get() = messageHelper.toMe(account, fromAddresses)
    private val toAddresses: Array<Address>
        get() = Address.unpack(cursor.getString(TO_LIST_COLUMN))
    private val toMe: Boolean get() = messageHelper.toMe(account, toAddresses)
    val account: Account
        get() = preferences.getAccount(cursor.getString(ACCOUNT_UUID_COLUMN))

    val answered: Boolean get() = cursor.getInt(ANSWERED_COLUMN) == 1

    val counterPartyAddresses: Address?
        get() {
            if (fromMe) {
                if (toAddresses.isNotEmpty()) {
                    return toAddresses[0]
                } else if (ccAddresses.isNotEmpty()) {
                    return ccAddresses[0]
                }
            } else if (fromAddresses.isNotEmpty()) {
                return fromAddresses[0]
            }
            return null
        }


    val chipColor: Int @ColorInt get() = account.chipColor

    val displayName: CharSequence
        get() = messageHelper.getDisplayName(account, fromAddresses, toAddresses)

    val date: Long get() = cursor.getLong(DATE_COLUMN)

    val flagged: Boolean get() = cursor.getInt(FLAGGED_COLUMN) == 1

    val folderServerId: String? get() = cursor.getString(FOLDER_SERVER_ID_COLUMN)

    val forwarded: Boolean get() = cursor.getInt(FORWARDED_COLUMN) == 1

    val hasAttachments: Boolean get() = cursor.getInt(ATTACHMENT_COUNT_COLUMN) > 0

    val id: Long get() = cursor.getLong(ID_COLUMN)

    val preview: String
        get() {
            val previewTypeString = cursor.getString(PREVIEW_TYPE_COLUMN)
            val previewType = DatabasePreviewType.fromDatabaseValue(previewTypeString)

            return when (previewType) {
                DatabasePreviewType.NONE, DatabasePreviewType.ERROR -> {
                    ""
                }
                DatabasePreviewType.ENCRYPTED -> {
                    res.getString(R.string.preview_encrypted)
                }
                DatabasePreviewType.TEXT -> {
                    cursor.getString(PREVIEW_COLUMN)
                }
                null -> throw AssertionError("Unknown preview type: $previewType")
            }
        }

    val read: Boolean get() = cursor.getInt(READ_COLUMN) == 1

    val sigil: String
        get() {
            return when {
                toMe -> res.getString(R.string.messagelist_sent_to_me_sigil)
                ccMe -> res.getString(R.string.messagelist_sent_cc_me_sigil)
                else -> ""
            }
        }

    val threadCount: Int get() {
        try {
            return cursor.getInt(THREAD_COUNT_COLUMN)
        } catch (_: Exception) {
            return 0
        }
    }

    val threadRootId: Long get() = cursor.getLong(THREAD_ROOT_COLUMN)

    val uid: String get() = cursor.getString(UID_COLUMN)

    fun isActiveMessage(against: MessageReference?): Boolean {
        val uid = cursor.getString(UID_COLUMN)
        val folderServerId = cursor.getString(FOLDER_SERVER_ID_COLUMN)

        val activeAccountUuid = against?.accountUuid
        val activeFolderServerId = against?.folderServerId
        val activeUid = against?.uid
        return account.uuid == activeAccountUuid
                && folderServerId == activeFolderServerId
                && uid == activeUid
    }

    fun subject(threadCount: Int): String {
        return MlfUtils.buildSubject(cursor.getString(SUBJECT_COLUMN),
                res.getString(R.string.general_no_subject), threadCount)
    }

    fun selectionIdentifier(uniqueColumnId: Int): Long = cursor.getLong(uniqueColumnId)
}