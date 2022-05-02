package com.fsck.k9.mail.internet

import com.fsck.k9.mail.Address
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageIdGeneratorTest {
    private val messageIdGenerator = MessageIdGenerator(
        object : UuidGenerator {
            override fun randomUuid() = "00000000-0000-4000-0000-000000000000"
        }
    )

    @Test
    fun generateMessageId_withFromAndReplyToAddress() {
        val message = MimeMessage().apply {
            setFrom(Address("alice@example.org"))
            replyTo = Address.parse("bob@example.com")
        }

        val result = messageIdGenerator.generateMessageId(message)

        assertEquals("<00000000-0000-4000-0000-000000000000@example.org>", result)
    }

    @Test
    fun generateMessageId_withReplyToAddress() {
        val message = MimeMessage().apply {
            replyTo = Address.parse("bob@example.com")
        }

        val result = messageIdGenerator.generateMessageId(message)

        assertEquals("<00000000-0000-4000-0000-000000000000@example.com>", result)
    }

    @Test
    fun generateMessageId_withoutRelevantHeaders() {
        val message = MimeMessage()

        val result = messageIdGenerator.generateMessageId(message)

        assertEquals("<00000000-0000-4000-0000-000000000000@fallback.k9mail.app>", result)
    }
}
