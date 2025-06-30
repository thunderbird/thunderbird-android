package com.fsck.k9.mail.testing.message

import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Multipart
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.MimeBodyPart
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.MimeMessageHelper
import com.fsck.k9.mail.internet.MimeMultipart
import com.fsck.k9.mail.internet.TextBody
import com.fsck.k9.mailstore.BinaryMemoryBody

fun buildMessage(block: PartBuilder.() -> Unit): Message {
    return MimeMessage().also { message ->
        PartBuilder(message).block()
    }
}

@DslMarker
annotation class MessageBuilderMarker

@MessageBuilderMarker
class PartBuilder(private val part: Part) {
    private var gotBodyBlock = false

    fun header(name: String, value: String) {
        part.addHeader(name, value)
    }

    fun textBody(text: String = "Hello World") {
        require(!gotBodyBlock) { "Only one body block allowed" }
        gotBodyBlock = true

        val body = TextBody(text)
        MimeMessageHelper.setBody(part, body)
    }

    fun dataBody(size: Int = 20 * 1024, encoding: String = "7bit") {
        require(!gotBodyBlock) { "Only one body block allowed" }
        gotBodyBlock = true

        val body = BinaryMemoryBody(ByteArray(size) { 'A'.code.toByte() }, encoding)
        MimeMessageHelper.setBody(part, body)
    }

    fun multipart(subType: String = "mixed", block: MultipartBuilder.() -> Unit) {
        require(!gotBodyBlock) { "Only one body block allowed" }
        gotBodyBlock = true

        val multipart = MimeMultipart.newInstance()
        multipart.setSubType(subType)
        MultipartBuilder(multipart).block()
        MimeMessageHelper.setBody(part, multipart)
    }
}

@MessageBuilderMarker
class MultipartBuilder(private val multipart: Multipart) {
    fun bodyPart(mimeType: String? = null, block: PartBuilder.() -> Unit) {
        MimeBodyPart().let { bodyPart ->
            if (mimeType != null) {
                bodyPart.addHeader("Content-Type", mimeType)
            }
            multipart.addBodyPart(bodyPart)
            PartBuilder(bodyPart).block()
        }
    }
}
