package com.fsck.k9.mail.message

import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.Part
import java.io.IOException
import java.io.InputStream
import org.apache.james.mime4j.MimeException
import org.apache.james.mime4j.parser.ContentHandler
import org.apache.james.mime4j.parser.MimeStreamParser
import org.apache.james.mime4j.stream.BodyDescriptor
import org.apache.james.mime4j.stream.Field
import org.apache.james.mime4j.stream.MimeConfig

object MessageHeaderParser {
    @Throws(MessagingException::class)
    @JvmStatic
    fun parse(part: Part, headerInputStream: InputStream) {
        val parser = createMimeStreamParser().apply {
            setContentHandler(MessageHeaderParserContentHandler(part))
        }

        try {
            parser.parse(headerInputStream)
        } catch (me: MimeException) {
            throw MessagingException("Error parsing headers", me)
        } catch (e: IOException) {
            throw MessagingException("I/O error parsing headers", e)
        }
    }

    private fun createMimeStreamParser(): MimeStreamParser {
        val parserConfig = MimeConfig.Builder()
            .setMaxHeaderLen(-1)
            .setMaxLineLen(-1)
            .setMaxHeaderCount(-1)
            .build()

        return MimeStreamParser(parserConfig)
    }

    private class MessageHeaderParserContentHandler(private val part: Part) : ContentHandler {
        override fun field(rawField: Field) {
            val name = rawField.name
            val raw = rawField.raw.toString()
            part.addRawHeader(name, raw)
        }

        override fun startMessage() = Unit

        override fun endMessage() = Unit

        override fun startBodyPart() = Unit

        override fun endBodyPart() = Unit

        override fun startHeader() = Unit

        override fun endHeader() = Unit

        override fun preamble(inputStream: InputStream) = Unit

        override fun epilogue(inputStream: InputStream) = Unit

        override fun startMultipart(bodyDescriptor: BodyDescriptor) = Unit

        override fun endMultipart() = Unit

        override fun body(bodyDescriptor: BodyDescriptor, inputStream: InputStream) = Unit

        override fun raw(inputStream: InputStream) = Unit
    }
}
