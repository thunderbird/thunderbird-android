package com.fsck.k9.mail.internet

import com.fsck.k9.mail.Header
import com.fsck.k9.mail.internet.MimeHeader.Field.NameValueField
import com.fsck.k9.mail.internet.MimeHeader.Field.RawField
import java.io.IOException
import java.io.OutputStream
import java.util.ArrayList
import java.util.LinkedHashSet

class MimeHeader {
    private val fields: MutableList<Field> = ArrayList()

    val headerNames: Set<String>
        get() = fields.mapTo(LinkedHashSet()) { it.name }

    val headers: List<Header>
        get() = fields.map { Header(it.name, it.value) }

    var checkHeaders = false

    fun clear() {
        fields.clear()
    }

    fun getFirstHeader(name: String): String? {
        return getHeader(name).firstOrNull()
    }

    fun addHeader(name: String, value: String) {
        requireValidHeader(name, value)
        val field = NameValueField(name, value)
        fields.add(field)
    }

    fun addRawHeader(name: String, raw: String) {
        requireValidRawHeader(name, raw)
        val field = RawField(name, raw)
        fields.add(field)
    }

    fun setHeader(name: String, value: String) {
        removeHeader(name)
        addHeader(name, value)
    }

    fun getHeader(name: String): Array<String> {
        return fields.asSequence()
            .filter { field -> field.name.equals(name, ignoreCase = true) }
            .map { field -> field.value }
            .toList()
            .toTypedArray()
    }

    fun removeHeader(name: String) {
        fields.removeAll { field -> field.name.equals(name, ignoreCase = true) }
    }

    override fun toString(): String {
        return buildString {
            appendFields()
        }
    }

    @Throws(IOException::class)
    fun writeTo(out: OutputStream) {
        val writer = out.writer().buffered(1024)
        writer.appendFields()
        writer.flush()
    }

    private fun Appendable.appendFields() {
        for (field in fields) {
            when (field) {
                is RawField -> append(field.raw)
                is NameValueField -> appendNameValueField(field)
            }
            append(CRLF)
        }
    }

    private fun Appendable.appendNameValueField(field: Field) {
        append(field.name)
        append(": ")
        append(field.value)
    }

    private fun requireValidHeader(name: String, value: String) {
        if (checkHeaders) {
            checkHeader(name, value)
        }
    }

    private fun requireValidRawHeader(name: String, raw: String) {
        if (checkHeaders) {
            if (!raw.startsWith(name)) throw AssertionError("Raw header value needs to start with header name")
            val delimiterIndex = raw.indexOf(':')
            val value = if (delimiterIndex == raw.lastIndex) "" else raw.substring(delimiterIndex + 1).trimStart()

            checkHeader(name, value)
        }
    }

    private fun checkHeader(name: String, value: String) {
        try {
            MimeHeaderChecker.checkHeader(name, value)
        } catch (e: MimeHeaderParserException) {
            // Use AssertionError so we crash the app
            throw AssertionError("Invalid header", e)
        }
    }

    companion object {
        const val SUBJECT = "Subject"
        const val HEADER_CONTENT_TYPE = "Content-Type"
        const val HEADER_CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding"
        const val HEADER_CONTENT_DISPOSITION = "Content-Disposition"
        const val HEADER_CONTENT_ID = "Content-ID"
    }

    private sealed class Field(val name: String) {
        abstract val value: String

        class NameValueField(name: String, override val value: String) : Field(name) {
            override fun toString(): String {
                return "$name: $value"
            }
        }

        class RawField(name: String, val raw: String) : Field(name) {
            override val value: String
                get() {
                    val delimiterIndex = raw.indexOf(':')
                    return if (delimiterIndex == raw.lastIndex) {
                        ""
                    } else {
                        raw.substring(delimiterIndex + 1).trim()
                    }
                }

            override fun toString(): String {
                return raw
            }
        }
    }
}
