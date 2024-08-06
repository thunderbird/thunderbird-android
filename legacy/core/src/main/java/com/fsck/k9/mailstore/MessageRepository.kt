package com.fsck.k9.mailstore

import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Header
import com.fsck.k9.mail.internet.MimeUtility
import org.apache.james.mime4j.dom.field.DateTimeField
import org.apache.james.mime4j.field.DefaultFieldParser

class MessageRepository(private val messageStoreManager: MessageStoreManager) {
    fun getHeaders(messageReference: MessageReference): List<Header> {
        val messageStore = messageStoreManager.getMessageStore(messageReference.accountUuid)
        return messageStore.getHeaders(messageReference.folderId, messageReference.uid)
    }

    fun getMessageDetails(messageReference: MessageReference): MessageDetails {
        val messageStore = messageStoreManager.getMessageStore(messageReference.accountUuid)
        val headers = messageStore.getHeaders(messageReference.folderId, messageReference.uid, MESSAGE_DETAILS_HEADERS)

        val messageDate = headers.parseDate("date")
        val fromAddresses = headers.parseAddresses("from")
        val senderAddresses = headers.parseAddresses("sender")
        val replyToAddresses = headers.parseAddresses("reply-to")
        val toAddresses = headers.parseAddresses("to")
        val ccAddresses = headers.parseAddresses("cc")
        val bccAddresses = headers.parseAddresses("bcc")

        return MessageDetails(
            date = messageDate,
            from = fromAddresses,
            sender = senderAddresses.firstOrNull(),
            replyTo = replyToAddresses,
            to = toAddresses,
            cc = ccAddresses,
            bcc = bccAddresses,
        )
    }

    private fun List<Header>.firstHeaderOrNull(name: String): String? {
        return firstOrNull { it.name.equals(name, ignoreCase = true) }?.value
    }

    private fun List<Header>.parseAddresses(headerName: String): List<Address> {
        return Address.parse(MimeUtility.unfold(firstHeaderOrNull(headerName))).toList()
    }

    private fun List<Header>.parseDate(headerName: String): MessageDate {
        val dateHeader = firstHeaderOrNull(headerName) ?: return MessageDate.MissingDate

        return try {
            val dateTimeField = DefaultFieldParser.parse("Date: $dateHeader") as DateTimeField
            return MessageDate.ValidDate(date = dateTimeField.date)
        } catch (e: Exception) {
            MessageDate.InvalidDate(dateHeader)
        }
    }

    companion object {
        private val MESSAGE_DETAILS_HEADERS = setOf(
            "date",
            "from",
            "sender",
            "reply-to",
            "to",
            "cc",
            "bcc",
        )
    }
}
