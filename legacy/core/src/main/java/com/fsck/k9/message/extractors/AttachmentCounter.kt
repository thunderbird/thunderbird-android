package com.fsck.k9.message.extractors

import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.MessageExtractor
import net.thunderbird.core.common.exception.MessagingException

class AttachmentCounter {
    @Throws(MessagingException::class)
    fun getAttachmentCount(message: Message): Int {
        val attachmentParts = ArrayList<Part>()
        MessageExtractor.findViewablesAndAttachments(
            message,
            null,
            attachmentParts,
        )

        return attachmentParts.size
    }

    companion object {
        fun newInstance() = AttachmentCounter()
    }
}
