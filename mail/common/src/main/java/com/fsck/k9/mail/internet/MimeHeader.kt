package com.fsck.k9.mail.internet

import com.fsck.k9.mail.internet.MimeHeader.Field.NameValueField
import com.fsck.k9.mail.internet.MimeHeader.Field.RawField
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.LinkedHashSet

class MimeHeader {
    private val fields: MutableList<Field> = ArrayList()
    private var charset: String? = null

    val headerNames: Set<String>
        get() = fields.mapTo(LinkedHashSet()) { it.name }

    fun clear() {
        fields.clear()
    }

    fun getFirstHeader(name: String): String? {
        return getHeader(name).firstOrNull()
    }

    fun addHeader(name: String, value: String) {
        val field = NameValueField(name, MimeUtility.foldAndEncode(value))
        fields.add(field)
    }

    fun addRawHeader(name: String, raw: String) {
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
        val writer = BufferedWriter(OutputStreamWriter(out), 1024)
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
        val value = field.value
        val encodedValue = if (hasToBeEncoded(value)) {
            val charset = this@MimeHeader.charset?.let { Charset.forName(it) }
            EncoderUtil.encodeEncodedWord(value, charset)
        } else {
            value
        }

        append(field.name)
        append(": ")
        append(encodedValue)
    }

    // encode non printable characters except LF/CR/TAB codes.
    private fun hasToBeEncoded(text: String): Boolean {
        return text.any { !it.isVChar() && !it.isWspOrCrlf() }
    }

    fun setCharset(charset: String?) {
        this.charset = charset
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
