package com.fsck.k9.mail.internet

import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import java.util.UUID

class MessageIdGenerator(private val uuidGenerator: UuidGenerator) {
    fun generateMessageId(message: Message): String {
        val uuid = uuidGenerator.randomUuid()
        val hostname = message.from.firstHostname ?: message.replyTo.firstHostname ?: "fallback.k9mail.app"

        return "<$uuid@$hostname>"
    }

    private val Array<Address>?.firstHostname: String?
        get() = this?.firstOrNull()?.hostname

    companion object {
        @JvmStatic
        fun getInstance(): MessageIdGenerator = MessageIdGenerator(K9UuidGenerator())
    }
}

interface UuidGenerator {
    fun randomUuid(): String
}

class K9UuidGenerator : UuidGenerator {
    override fun randomUuid(): String {
        // We use upper case here to match Apple Mail Message-ID format (for privacy)
        return UUID.randomUUID().toString().uppercase()
    }
}
