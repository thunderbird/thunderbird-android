package com.fsck.k9.ui.messagelist

import android.database.Cursor
import com.fsck.k9.Preferences
import com.fsck.k9.fragment.MLFProjectionInfo
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.helper.map
import com.fsck.k9.mail.Address
import com.fsck.k9.mailstore.DatabasePreviewType

class MessageListExtractor(
    private val preferences: Preferences,
    private val messageHelper: MessageHelper
) {
    fun extractMessageList(cursor: Cursor, uniqueIdColumn: Int, threadCountIncluded: Boolean): List<MessageListItem> {
        return cursor.map { extractMessageListItem(it, uniqueIdColumn, threadCountIncluded) }
    }

    private fun extractMessageListItem(
        cursor: Cursor,
        uniqueIdColumn: Int,
        threadCountIncluded: Boolean
    ): MessageListItem {
        val position = cursor.position
        val accountUuid = cursor.getString(MLFProjectionInfo.ACCOUNT_UUID_COLUMN)
        val account = preferences.getAccount(accountUuid)
        val fromList = cursor.getString(MLFProjectionInfo.SENDER_LIST_COLUMN)
        val toList = cursor.getString(MLFProjectionInfo.TO_LIST_COLUMN)
        val ccList = cursor.getString(MLFProjectionInfo.CC_LIST_COLUMN)
        val fromAddresses = Address.unpack(fromList)
        val toAddresses = Address.unpack(toList)
        val ccAddresses = Address.unpack(ccList)
        val fromMe = messageHelper.toMe(account, fromAddresses)
        val toMe = messageHelper.toMe(account, toAddresses)
        val ccMe = messageHelper.toMe(account, ccAddresses)
        val counterPartyAddress = getCounterPartyAddress(fromMe, toAddresses, ccAddresses, fromAddresses)
        val displayName = messageHelper.getDisplayName(account, fromAddresses, toAddresses)
        val messageDate = cursor.getLong(MLFProjectionInfo.DATE_COLUMN)
        val threadCount = if (threadCountIncluded) cursor.getInt(MLFProjectionInfo.THREAD_COUNT_COLUMN) else 0
        val subject = cursor.getString(MLFProjectionInfo.SUBJECT_COLUMN)
        val isRead = cursor.getBoolean(MLFProjectionInfo.READ_COLUMN)
        val isStarred = cursor.getBoolean(MLFProjectionInfo.FLAGGED_COLUMN)
        val isAnswered = cursor.getBoolean(MLFProjectionInfo.ANSWERED_COLUMN)
        val isForwarded = cursor.getBoolean(MLFProjectionInfo.FORWARDED_COLUMN)
        val hasAttachments = cursor.getInt(MLFProjectionInfo.ATTACHMENT_COUNT_COLUMN) > 0
        val previewTypeString = cursor.getString(MLFProjectionInfo.PREVIEW_TYPE_COLUMN)
        val previewType = DatabasePreviewType.fromDatabaseValue(previewTypeString)
        val isMessageEncrypted = previewType == DatabasePreviewType.ENCRYPTED
        val previewText = getPreviewText(previewType, cursor)
        val uniqueId = cursor.getLong(uniqueIdColumn)
        val folderId = cursor.getLong(MLFProjectionInfo.FOLDER_ID_COLUMN)
        val messageUid = cursor.getString(MLFProjectionInfo.UID_COLUMN)
        val databaseId = cursor.getLong(MLFProjectionInfo.ID_COLUMN)
        val senderAddress = fromAddresses.getOrNull(0)?.address
        val threadRoot = cursor.getLong(MLFProjectionInfo.THREAD_ROOT_COLUMN)

        return MessageListItem(
            position,
            account,
            subject,
            threadCount,
            messageDate,
            displayName,
            counterPartyAddress,
            fromMe,
            toMe,
            ccMe,
            previewText,
            isMessageEncrypted,
            isRead,
            isStarred,
            isAnswered,
            isForwarded,
            hasAttachments,
            uniqueId,
            folderId,
            messageUid,
            databaseId,
            senderAddress,
            threadRoot
        )
    }

    private fun getCounterPartyAddress(
        fromMe: Boolean,
        toAddresses: Array<Address>,
        ccAddresses: Array<Address>,
        fromAddresses: Array<Address>
    ): Address? {
        return when {
            fromMe && toAddresses.isNotEmpty() -> toAddresses[0]
            fromMe && ccAddresses.isNotEmpty() -> ccAddresses[0]
            fromAddresses.isNotEmpty() -> fromAddresses[0]
            else -> null
        }
    }

    private fun getPreviewText(previewType: DatabasePreviewType?, cursor: Cursor): String {
        return if (previewType == DatabasePreviewType.TEXT) {
            cursor.getString(MLFProjectionInfo.PREVIEW_COLUMN) ?: ""
        } else {
            ""
        }
    }

    private fun Cursor.getBoolean(columnIndex: Int): Boolean = getInt(columnIndex) == 1
}
