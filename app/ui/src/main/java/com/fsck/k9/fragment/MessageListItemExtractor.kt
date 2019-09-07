package com.fsck.k9.fragment

import android.content.res.Resources
import android.database.Cursor
import androidx.annotation.ColorInt
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessageReference
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

    private val account: Account
        get() = preferences.getAccount(cursor.getString(MLFProjectionInfo.ACCOUNT_UUID_COLUMN))
    private val ccAddresses: Array<Address>
        get() = Address.unpack(cursor.getString(MLFProjectionInfo.CC_LIST_COLUMN))
    private val ccMe: Boolean get() = messageHelper.toMe(account, ccAddresses)
    private val fromAddresses: Array<Address>
        get() = Address.unpack(cursor.getString(MLFProjectionInfo.SENDER_LIST_COLUMN))
    private val fromMe: Boolean get() = messageHelper.toMe(account, fromAddresses)
    private val toAddresses: Array<Address>
        get() = Address.unpack(cursor.getString(MLFProjectionInfo.TO_LIST_COLUMN))
    private val toMe: Boolean get() = messageHelper.toMe(account, toAddresses)

    val answered: Boolean get() = cursor.getInt(MLFProjectionInfo.ANSWERED_COLUMN) == 1

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

    val date: Long get() = cursor.getLong(MLFProjectionInfo.DATE_COLUMN)

    val flagged: Boolean get() = cursor.getInt(MLFProjectionInfo.FLAGGED_COLUMN) == 1

    val forwarded: Boolean get() = cursor.getInt(MLFProjectionInfo.FORWARDED_COLUMN) == 1

    val hasAttachments: Boolean get() = cursor.getInt(MLFProjectionInfo.ATTACHMENT_COUNT_COLUMN) > 0

    val preview: String
        get() {
            val previewTypeString = cursor.getString(MLFProjectionInfo.PREVIEW_TYPE_COLUMN)
            val previewType = DatabasePreviewType.fromDatabaseValue(previewTypeString)

            return when (previewType) {
                DatabasePreviewType.NONE, DatabasePreviewType.ERROR -> {
                    ""
                }
                DatabasePreviewType.ENCRYPTED -> {
                    res.getString(R.string.preview_encrypted)
                }
                DatabasePreviewType.TEXT -> {
                    cursor.getString(MLFProjectionInfo.PREVIEW_COLUMN)
                }
                null -> throw AssertionError("Unknown preview type: $previewType")
            }
        }

    val read: Boolean get() = cursor.getInt(MLFProjectionInfo.READ_COLUMN) == 1

    val sigil: String
        get() {
            return when {
                toMe -> res.getString(R.string.messagelist_sent_to_me_sigil)
                ccMe -> res.getString(R.string.messagelist_sent_cc_me_sigil)
                else -> ""
            }
        }

    val threadCount: Int get() = cursor.getInt(MLFProjectionInfo.THREAD_COUNT_COLUMN)

    fun isActiveMessage(against: MessageReference?): Boolean {
        val uid = cursor.getString(MLFProjectionInfo.UID_COLUMN)
        val folderServerId = cursor.getString(MLFProjectionInfo.FOLDER_SERVER_ID_COLUMN)

        val activeAccountUuid = against?.accountUuid
        val activeFolderServerId = against?.folderServerId
        val activeUid = against?.uid
        return account.uuid == activeAccountUuid
                && folderServerId == activeFolderServerId
                && uid == activeUid
    }

    fun subject(threadCount: Int): String {
        return MlfUtils.buildSubject(cursor.getString(MLFProjectionInfo.SUBJECT_COLUMN),
                res.getString(R.string.general_no_subject), threadCount)
    }
}