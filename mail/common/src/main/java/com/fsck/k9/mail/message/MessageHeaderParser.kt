package com.fsck.k9.mail.message

import java.io.IOException
import java.io.InputStream
import net.thunderbird.core.common.exception.MessagingException
import org.apache.james.mime4j.MimeException
import org.apache.james.mime4j.parser.AbstractContentHandler
import org.apache.james.mime4j.parser.MimeStreamParser
import org.apache.james.mime4j.stream.Field
import org.apache.james.mime4j.stream.MimeConfig

object MessageHeaderParser {
    @Throws(MessagingException::class)
    @JvmStatic
    fun parse(headerInputStream: InputStream, collector: MessageHeaderCollector) {
        val parser = createMimeStreamParser().apply {
            setContentHandler(MessageHeaderParserContentHandler(collector))
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

    private class MessageHeaderParserContentHandler(
        private val collector: MessageHeaderCollector,
    ) : AbstractContentHandler() {
        override fun field(rawField: Field) {
            val name = rawField.name
            val raw = rawField.raw.toString()
            collector.addRawHeader(name, raw)
        }
    }
}

fun interface MessageHeaderCollector {
    fun addRawHeader(name: String, raw: String)
}
