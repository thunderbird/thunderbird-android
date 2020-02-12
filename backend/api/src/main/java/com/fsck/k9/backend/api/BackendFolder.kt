package com.fsck.k9.backend.api

import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import java.util.Date

// FIXME: add documentation
interface BackendFolder {
    val name: String
    val visibleLimit: Int

    fun getMessageServerIds(): Set<String>
    fun getAllMessagesAndEffectiveDates(): Map<String, Long?>
    fun destroyMessages(messageServerIds: List<String>)
    fun getLastUid(): Long?
    fun getMoreMessages(): MoreMessages
    fun setMoreMessages(moreMessages: MoreMessages)
    fun setLastChecked(timestamp: Long)
    fun setStatus(status: String?)
    fun getPushState(): String?
    fun setPushState(pushState: String?)
    fun isMessagePresent(messageServerId: String): Boolean
    fun getMessageFlags(messageServerId: String): Set<Flag>
    fun setMessageFlag(messageServerId: String, flag: Flag, value: Boolean)
    fun savePartialMessage(message: Message)
    fun saveCompleteMessage(message: Message)
    fun getLatestOldMessageSeenTime(): Date
    fun setLatestOldMessageSeenTime(date: Date)
    fun getOldestMessageDate(): Date?
    fun getFolderExtraString(name: String): String?
    fun setFolderExtraString(name: String, value: String?)
    fun getFolderExtraNumber(name: String): Long?
    fun setFolderExtraNumber(name: String, value: Long)

    enum class MoreMessages {
        UNKNOWN,
        FALSE,
        TRUE
    }
}
