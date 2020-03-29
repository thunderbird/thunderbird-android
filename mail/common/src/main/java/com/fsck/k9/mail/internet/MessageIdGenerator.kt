package com.fsck.k9.mail.internet

import androidx.annotation.VisibleForTesting
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import java.util.Locale
import java.util.UUID

open class MessageIdGenerator @VisibleForTesting internal constructor() {
    fun generateMessageId(message: Message): String {
        val uuid = generateUuid()
        val hostname = message.from.firstHostname ?: message.replyTo.firstHostname ?: "email.android.com"

        return "<$uuid@$hostname>"
    }

    @VisibleForTesting
    protected open fun generateUuid(): String {
        // We use upper case here to match Apple Mail Message-ID format (for privacy)
        return UUID.randomUUID().toString().toUpperCase(Locale.US)
    }

    private val Array<Address>?.firstHostname: String?
        get() = this?.firstOrNull()?.hostname

    companion object {
        @JvmStatic
        fun getInstance(): MessageIdGenerator = MessageIdGenerator()
    }
}
