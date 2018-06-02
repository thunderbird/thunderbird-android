package com.fsck.k9.controller

import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import com.fsck.k9.mailstore.MessageRemovalListener

//FIXME: add documentation
interface BackendFolder {
    val name: String
    val visibleLimit: Int

    fun getAllMessagesAndEffectiveDates(): Map<String, Long?>
    fun destroyMessages(messageServerIds: List<String>)
    fun getLastUid(): Long?
    fun getMoreMessages(): MoreMessages
    fun setMoreMessages(moreMessages: MoreMessages)
    fun getUnreadMessageCount(): Int
    fun setLastChecked(timestamp: Long)
    fun setStatus(status: String?)
    fun getPushState(): String?
    fun setPushState(pushState: String?)
    fun purgeToVisibleLimit(listener: MessageRemovalListener)
    fun isMessagePresent(messageServerId: String): Boolean
    fun getMessageFlags(messageServerId: String): Set<Flag>
    fun setMessageFlag(messageServerId: String, flag: Flag, value: Boolean)
    fun savePartialMessage(message: Message)
    fun saveCompleteMessage(message: Message)

    enum class MoreMessages {
        UNKNOWN,
        FALSE,
        TRUE
    }
}
