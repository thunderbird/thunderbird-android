package com.fsck.k9.backend.api

import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessageDownloadState
import java.util.Date
import net.thunderbird.core.common.mail.Flag

// FIXME: add documentation
interface BackendFolder {
    val name: String
    val visibleLimit: Int

    fun getMessageServerIds(): Set<String>
    fun getAllMessagesAndEffectiveDates(): Map<String, Long?>
    fun destroyMessages(messageServerIds: List<String>)
    fun clearAllMessages()
    fun getMoreMessages(): MoreMessages
    fun setMoreMessages(moreMessages: MoreMessages)
    fun setLastChecked(timestamp: Long)
    fun setStatus(status: String?)
    fun isMessagePresent(messageServerId: String): Boolean
    fun getMessageFlags(messageServerId: String): Set<Flag>
    fun setMessageFlag(messageServerId: String, flag: Flag, value: Boolean)
    fun saveMessage(message: Message, downloadState: MessageDownloadState)
    fun getOldestMessageDate(): Date?
    fun getFolderExtraString(name: String): String?
    fun setFolderExtraString(name: String, value: String?)
    fun getFolderExtraNumber(name: String): Long?
    fun setFolderExtraNumber(name: String, value: Long)

    enum class MoreMessages {
        UNKNOWN,
        FALSE,
        TRUE,
    }
}
