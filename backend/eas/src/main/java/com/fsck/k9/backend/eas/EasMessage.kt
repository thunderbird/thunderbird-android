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
            TODO("not implemented")
        }

        override fun getMode(): Int {
            TODO("not implemented")
        }

        override fun create(): Boolean {
            TODO("not implemented")
        }

        override fun exists(): Boolean {
            TODO("not implemented")
        }

        override fun getMessageCount(): Int {
            TODO("not implemented")
        }

        override fun getUnreadMessageCount(): Int {
            TODO("not implemented")
        }

        override fun getFlaggedMessageCount(): Int {
            TODO("not implemented")
        }

        override fun getMessage(uid: String?): EasMessage {
            TODO("not implemented")
        }

        override fun getMessages(start: Int, end: Int, earliestDate: Date?, listener: MessageRetrievalListener<EasMessage>?): MutableList<EasMessage> {
            TODO("not implemented")
        }

        override fun areMoreMessagesAvailable(indexOfOldestMessage: Int, earliestDate: Date?): Boolean {
            TODO("not implemented")
        }

        override fun appendMessages(messages: MutableList<out Message>?): MutableMap<String, String> {
            TODO("not implemented")
        }

        override fun setFlags(messages: MutableList<out Message>?, flags: MutableSet<Flag>?, value: Boolean) {
            TODO("not implemented")
        }

        override fun setFlags(flags: MutableSet<Flag>?, value: Boolean) {
            TODO("not implemented")
        }

        override fun getUidFromMessageId(messageId: String?): String {
            TODO("not implemented")
        }

        override fun fetch(messages: MutableList<EasMessage>?, fp: FetchProfile?, listener: MessageRetrievalListener<EasMessage>?) {
            TODO("not implemented")
        }

        override fun getName(): String {
            TODO("not implemented")
        }
    }
}
