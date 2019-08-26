package com.fsck.k9.backend.eas

import com.fsck.k9.mail.*
import com.fsck.k9.mail.internet.MimeMessage
import java.util.Date

class EasMessage : MimeMessage() {
    fun setFolderServerId(folderServerId: String) {
        mFolder = EasFolder(folderServerId)
    }

    class EasFolder(@JvmField val serverId: String) : Folder<EasMessage>() {
        override fun getServerId() = serverId

        override fun open(mode: Int) {}

        override fun close() {}

        override fun isOpen(): Boolean {
            throw NotImplementedError()
        }

        override fun getMode(): Int {
            throw NotImplementedError()
        }

        override fun create(): Boolean {
            throw NotImplementedError()
        }

        override fun exists(): Boolean {
            throw NotImplementedError()
        }

        override fun getMessageCount(): Int {
            throw NotImplementedError()
        }

        override fun getUnreadMessageCount(): Int {
            throw NotImplementedError()
        }

        override fun getFlaggedMessageCount(): Int {
            throw NotImplementedError()
        }

        override fun getMessage(uid: String?): EasMessage {
            throw NotImplementedError()
        }

        override fun getMessages(start: Int, end: Int, earliestDate: Date?, listener: MessageRetrievalListener<EasMessage>?): MutableList<EasMessage> {
            throw NotImplementedError()
        }

        override fun areMoreMessagesAvailable(indexOfOldestMessage: Int, earliestDate: Date?): Boolean {
            throw NotImplementedError()
        }

        override fun appendMessages(messages: MutableList<out Message>?): MutableMap<String, String> {
            throw NotImplementedError()
        }

        override fun setFlags(messages: MutableList<out Message>?, flags: MutableSet<Flag>?, value: Boolean) {
            throw NotImplementedError()
        }

        override fun setFlags(flags: MutableSet<Flag>?, value: Boolean) {
            throw NotImplementedError()
        }

        override fun getUidFromMessageId(messageId: String?): String {
            throw NotImplementedError()
        }

        override fun fetch(messages: MutableList<EasMessage>?, fp: FetchProfile?, listener: MessageRetrievalListener<EasMessage>?) {
            throw NotImplementedError()
        }

        override fun getName(): String {
            throw NotImplementedError()
        }
    }
}
