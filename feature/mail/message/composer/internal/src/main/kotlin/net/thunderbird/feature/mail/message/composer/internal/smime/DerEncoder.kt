package net.thunderbird.feature.mail.message.composer.internal.smime

import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Minimal ASN.1 DER encoder (ITU-T X.690) for constructing RFC 5652 CMS SignedData structures.
 */
@Suppress("MagicNumber", "TooManyFunctions")
internal object DerEncoder {
    private const val TAG_INTEGER = 0x02
    private const val TAG_OCTET_STRING = 0x04
    private const val TAG_NULL = 0x05
    private const val TAG_OBJECT_IDENTIFIER = 0x06
    private const val TAG_UTC_TIME = 0x17
    private const val TAG_GENERALIZED_TIME = 0x18
    private const val TAG_SEQUENCE = 0x30
    private const val TAG_SET = 0x31
    private const val TAG_CONTEXT_SPECIFIC = 0x80
    private const val TAG_CONSTRUCTED = 0x20

    // RFC 5652 Section 11.3: dates between 1950 and 2049 inclusive MUST use UTCTime.
    private const val UTC_TIME_START_MILLIS = -631_152_000_000L // 1950-01-01T00:00:00Z
    private const val UTC_TIME_END_EXCLUSIVE_MILLIS = 2_524_608_000_000L // 2050-01-01T00:00:00Z

    private val UNSIGNED_BYTE_ARRAY_COMPARATOR = Comparator<ByteArray> { left, right ->
        val minLength = minOf(left.size, right.size)
        for (index in 0 until minLength) {
            val diff = (left[index].toInt() and 0xFF) - (right[index].toInt() and 0xFF)
            if (diff != 0) return@Comparator diff
        }
        left.size - right.size
    }

    fun encodeNull(): ByteArray = byteArrayOf(TAG_NULL.toByte(), 0x00)

    fun encodeInteger(value: BigInteger): ByteArray = encodeTag(TAG_INTEGER, value.toByteArray())

    fun encodeInteger(value: Long): ByteArray = encodeInteger(BigInteger.valueOf(value))

    fun encodeOctetString(bytes: ByteArray): ByteArray = encodeTag(TAG_OCTET_STRING, bytes)

    fun encodeOid(oid: String): ByteArray {
        val arcs = oid.split('.').map { arc ->
            arc.toLongOrNull() ?: throw IllegalArgumentException("Invalid OID component in $oid")
        }
        require(arcs.size >= 2) { "OID must have at least two components: $oid" }
        require(arcs[0] in 0L..2L) { "First OID arc must be 0, 1, or 2: $oid" }
        require(arcs[0] == 2L || arcs[1] < 40L) { "Second OID arc out of range: $oid" }

        val out = ByteArrayOutputStream()
        writeBase128(out, arcs[0] * 40L + arcs[1])
        for (index in 2 until arcs.size) {
            writeBase128(out, arcs[index])
        }
        return encodeTag(TAG_OBJECT_IDENTIFIER, out.toByteArray())
    }

    fun encodeSequence(vararg elements: ByteArray): ByteArray = encodeSequence(elements.asList())

    fun encodeSequence(elements: List<ByteArray>): ByteArray = encodeTag(TAG_SEQUENCE, concat(elements))

    fun encodeSet(vararg elements: ByteArray): ByteArray = encodeSet(elements.asList())

    fun encodeSet(elements: List<ByteArray>): ByteArray {
        val sorted = sortDerElements(elements)
        return encodeTag(TAG_SET, concat(sorted))
    }

    fun sortDerElements(elements: List<ByteArray>): List<ByteArray> =
        elements.sortedWith(UNSIGNED_BYTE_ARRAY_COMPARATOR)

    fun encodeContextSpecific(tagNumber: Int, constructed: Boolean, content: ByteArray): ByteArray {
        require(tagNumber in 0..30) { "Only short-form context-specific tags (0..30) are supported" }
        val tag = TAG_CONTEXT_SPECIFIC or (if (constructed) TAG_CONSTRUCTED else 0) or tagNumber
        return encodeTag(tag, content)
    }

    fun encodeTime(epochMillis: Long): ByteArray {
        val useUtcTime = epochMillis in UTC_TIME_START_MILLIS until UTC_TIME_END_EXCLUSIVE_MILLIS
        val pattern = if (useUtcTime) "yyMMddHHmmss'Z'" else "yyyyMMddHHmmss'Z'"
        val formatter = SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val formatted = formatter.format(Date(epochMillis)).toByteArray(Charsets.US_ASCII)
        val tag = if (useUtcTime) TAG_UTC_TIME else TAG_GENERALIZED_TIME
        return encodeTag(tag, formatted)
    }

    fun concat(elements: List<ByteArray>): ByteArray {
        val totalSize = elements.sumOf { it.size }
        val result = ByteArray(totalSize)
        var offset = 0
        for (element in elements) {
            element.copyInto(destination = result, destinationOffset = offset)
            offset += element.size
        }
        return result
    }

    private fun encodeTag(tag: Int, content: ByteArray): ByteArray {
        val lengthBytes = encodeLength(content.size)
        val result = ByteArray(1 + lengthBytes.size + content.size)
        result[0] = tag.toByte()
        lengthBytes.copyInto(destination = result, destinationOffset = 1)
        content.copyInto(destination = result, destinationOffset = 1 + lengthBytes.size)
        return result
    }

    private fun encodeLength(length: Int): ByteArray {
        require(length >= 0) { "DER length must be non-negative: $length" }
        if (length < 128) {
            return byteArrayOf(length.toByte())
        }
        val bytes = ByteArray(4)
        var remaining = length
        var count = 0
        while (remaining > 0) {
            bytes[3 - count] = (remaining and 0xFF).toByte()
            remaining = remaining ushr 8
            count++
        }
        val result = ByteArray(1 + count)
        result[0] = (0x80 or count).toByte()
        bytes.copyInto(destination = result, destinationOffset = 1, startIndex = 4 - count, endIndex = 4)
        return result
    }

    private fun writeBase128(out: ByteArrayOutputStream, value: Long) {
        require(value >= 0L) { "OID arc must be non-negative: $value" }
        if (value < 128L) {
            out.write(value.toInt())
            return
        }
        val buffer = ByteArray(10)
        var remaining = value
        var count = 0
        while (remaining > 0L) {
            buffer[9 - count] = (remaining and 0x7FL).toByte()
            remaining = remaining ushr 7
            count++
        }
        for (index in 10 - count until 9) {
            out.write((buffer[index].toInt() and 0x7F) or 0x80)
        }
        out.write(buffer[9].toInt() and 0x7F)
    }
}
