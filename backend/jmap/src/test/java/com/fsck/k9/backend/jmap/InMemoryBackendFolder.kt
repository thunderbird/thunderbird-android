package com.fsck.k9.backend.jmap

import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.Message
import java.util.Date

class InMemoryBackendFolder(override var name: String, var type: FolderType) : BackendFolder {
    val extraStrings: MutableMap<String, String> = mutableMapOf()
    val extraNumbers: MutableMap<String, Long> = mutableMapOf()

    override var visibleLimit: Int = 25

    override fun getAllMessagesAndEffectiveDates(): Map<String, Long?> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun destroyMessages(messageServerIds: List<String>) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getLastUid(): Long? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getMoreMessages(): BackendFolder.MoreMessages {
        throw UnsupportedOperationException("not implemented")
    }

    override fun setMoreMessages(moreMessages: BackendFolder.MoreMessages) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getUnreadMessageCount(): Int {
        throw UnsupportedOperationException("not implemented")
    }

    override fun setLastChecked(timestamp: Long) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun setStatus(status: String?) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getPushState(): String? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun setPushState(pushState: String?) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun isMessagePresent(messageServerId: String): Boolean {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getMessageFlags(messageServerId: String): Set<Flag> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun setMessageFlag(messageServerId: String, flag: Flag, value: Boolean) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun savePartialMessage(message: Message) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun saveCompleteMessage(message: Message) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getLatestOldMessageSeenTime(): Date {
        throw UnsupportedOperationException("not implemented")
    }

    override fun setLatestOldMessageSeenTime(date: Date) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getOldestMessageDate(): Date? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getFolderExtraString(name: String): String? = extraStrings[name]

    override fun setFolderExtraString(name: String, value: String) {
        extraStrings[name] = value
    }

    override fun getFolderExtraNumber(name: String): Long? = extraNumbers[name]

    override fun setFolderExtraNumber(name: String, value: Long) {
        extraNumbers[name] = value
    }
}
