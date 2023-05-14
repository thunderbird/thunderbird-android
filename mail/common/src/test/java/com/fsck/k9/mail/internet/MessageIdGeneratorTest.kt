package com.fsck.k9.mail.internet

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.Address
import org.junit.Test

class MessageIdGeneratorTest {
    private val messageIdGenerator = MessageIdGenerator(
        object : UuidGenerator {
            override fun randomUuid() = "00000000-0000-4000-0000-000000000000"
        },
    )

    @Test
    fun generateMessageId_withFromAndReplyToAddress() {
        val message = MimeMessage().apply {
            setFrom(Address("alice@example.org"))
            replyTo = Address.parse("bob@example.com")
        }

        val result = messageIdGenerator.generateMessageId(message)

        assertThat(result).isEqualTo("<00000000-0000-4000-0000-000000000000@example.org>")
    }

    @Test
    fun generateMessageId_withReplyToAddress() {
        val message = MimeMessage().apply {
            replyTo = Address.parse("bob@example.com")
        }

        val result = messageIdGenerator.generateMessageId(message)

        assertThat(result).isEqualTo("<00000000-0000-4000-0000-000000000000@example.com>")
    }

    @Test
    fun generateMessageId_withoutRelevantHeaders() {
        val message = MimeMessage()

        val result = messageIdGenerator.generateMessageId(message)

        assertThat(result).isEqualTo("<00000000-0000-4000-0000-000000000000@fallback.k9mail.app>")
    }
}
