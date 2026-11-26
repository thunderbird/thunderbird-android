package net.thunderbird.feature.mail.message.composer.internal.smime

import java.math.BigInteger

internal class DerNode(
    val tag: Int,
    val value: ByteArray,
    val raw: ByteArray,
    val isIndefiniteLength: Boolean = false,
)

/**
 * Minimal ASN.1 DER/BER decoder supporting both definite-length DER and indefinite-length BER
 * encodings commonly produced by S/MIME mail clients.
 */
@Suppress("MagicNumber")
internal object DerDecoder {
    const val TAG_INTEGER = 0x02
    const val TAG_OCTET_STRING = 0x04
    const val TAG_OBJECT_IDENTIFIER = 0x06
    const val TAG_SEQUENCE = 0x30
    const val TAG_SET = 0x31
    const val TAG_CONTEXT_0_PRIMITIVE = 0x80
    const val TAG_CONTEXT_0_CONSTRUCTED = 0xA0
    const val TAG_CONTEXT_1_CONSTRUCTED = 0xA1

    fun decodeSingle(bytes: ByteArray): DerNode {
        val (node, _) = parseNode(bytes, 0, bytes.size)
        return node
    }

    fun decodeChildren(parent: DerNode): List<DerNode> {
        val nodes = mutableListOf<DerNode>()
        var offset = 0
        val data = parent.value
        while (offset < data.size) {
            if (parent.isIndefiniteLength && isEndOfContents(data, offset)) {
                break
            }
            val (node, nextOffset) = parseNode(data, offset, data.size)
            nodes.add(node)
            offset = nextOffset
        }
        return nodes
    }

    fun decodeOid(node: DerNode): String {
        require(node.tag == TAG_OBJECT_IDENTIFIER && node.value.isNotEmpty()) {
            "Expected OBJECT IDENTIFIER"
        }
        val bytes = node.value
        val result = StringBuilder()
        var offset = 0
        val (firstArcPair, nextOffset) = readBase128(bytes, offset)
        offset = nextOffset

        val firstArc: Long
        val secondArc: Long
        if (firstArcPair < 40L) {
            firstArc = 0L
            secondArc = firstArcPair
        } else if (firstArcPair < 80L) {
            firstArc = 1L
            secondArc = firstArcPair - 40L
        } else {
            firstArc = 2L
            secondArc = firstArcPair - 80L
        }
        result.append(firstArc).append('.').append(secondArc)

        while (offset < bytes.size) {
            val (arc, next) = readBase128(bytes, offset)
            result.append('.').append(arc)
            offset = next
        }
        return result.toString()
    }

    fun decodeInteger(node: DerNode): BigInteger {
        require(node.tag == TAG_INTEGER && node.value.isNotEmpty()) { "Expected INTEGER" }
        return BigInteger(node.value)
    }

    private fun parseNode(bytes: ByteArray, startOffset: Int, limit: Int): Pair<DerNode, Int> {
        require(startOffset + 2 <= limit) { "Truncated ASN.1 TLV header" }
        val tag = bytes[startOffset].toInt() and 0xFF
        val firstLenByte = bytes[startOffset + 1].toInt() and 0xFF

        if (firstLenByte == 0x80) {
            return parseIndefiniteNode(bytes, startOffset, tag, limit)
        }

        val length: Int
        val valueStart: Int
        if (firstLenByte < 0x80) {
            length = firstLenByte
            valueStart = startOffset + 2
        } else {
            val numLenBytes = firstLenByte and 0x7F
            require(numLenBytes in 1..4 && startOffset + 2 + numLenBytes <= limit) {
                "Invalid ASN.1 length encoding"
            }
            var acc = 0
            for (i in 0 until numLenBytes) {
                acc = (acc shl 8) or (bytes[startOffset + 2 + i].toInt() and 0xFF)
            }
            length = acc
            valueStart = startOffset + 2 + numLenBytes
        }

        val endOffset = valueStart + length
        require(length >= 0 && endOffset <= limit) { "ASN.1 element exceeds bounds" }

        val value = bytes.copyOfRange(valueStart, endOffset)
        val raw = bytes.copyOfRange(startOffset, endOffset)
        return DerNode(tag = tag, value = value, raw = raw) to endOffset
    }

    private fun parseIndefiniteNode(
        bytes: ByteArray,
        startOffset: Int,
        tag: Int,
        limit: Int,
    ): Pair<DerNode, Int> {
        val isConstructed = (tag and 0x20) != 0
        require(isConstructed) { "Indefinite length is only allowed on constructed ASN.1 tags" }
        val valueStart = startOffset + 2
        var cursor = valueStart
        while (cursor < limit) {
            if (isEndOfContents(bytes, cursor)) {
                val value = bytes.copyOfRange(valueStart, cursor)
                val endOffset = cursor + 2
                val raw = bytes.copyOfRange(startOffset, endOffset)
                return DerNode(tag = tag, value = value, raw = raw, isIndefiniteLength = true) to endOffset
            }
            val (_, nextCursor) = parseNode(bytes, cursor, limit)
            cursor = nextCursor
        }
        throw IllegalArgumentException("Unterminated indefinite-length ASN.1 structure")
    }

    private fun isEndOfContents(bytes: ByteArray, offset: Int): Boolean {
        return offset + 1 < bytes.size && bytes[offset] == 0.toByte() && bytes[offset + 1] == 0.toByte()
    }

    private fun readBase128(bytes: ByteArray, startOffset: Int): Pair<Long, Int> {
        var acc = 0L
        var offset = startOffset
        while (offset < bytes.size) {
            val b = bytes[offset].toInt() and 0xFF
            acc = (acc shl 7) or (b and 0x7F).toLong()
            offset++
            if ((b and 0x80) == 0) {
                return acc to offset
            }
        }
        throw IllegalArgumentException("Truncated base-128 OID arc")
    }
}
