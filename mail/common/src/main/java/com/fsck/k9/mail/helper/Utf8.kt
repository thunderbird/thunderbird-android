/*
 * These functions are based on Okio's UTF-8 code.
 *
 * Copyright (C) 2018 The K-9 Dog Walkers
 * Copyright (C) 2017 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fsck.k9.mail.helper

/**
 * Encodes this string using UTF-8.
 */
inline fun String.encodeUtf8(beginIndex: Int = 0, endIndex: Int = length, crossinline writeByte: (Byte) -> Unit) {
    require(beginIndex >= 0) { "beginIndex < 0: $beginIndex" }
    require(endIndex >= beginIndex) { "endIndex < beginIndex: $endIndex < $beginIndex" }
    require(endIndex <= length) { "endIndex > length: $endIndex > $length" }

    // Transcode a UTF-16 Java String to UTF-8 bytes.
    var i = beginIndex
    while (i < endIndex) {
        val c = this[i].code

        if (c < 0x80) {
            // Emit a 7-bit character with 1 byte.
            writeByte(c.toByte()) // 0xxxxxxx
            i++
        } else if (c < 0x800) {
            // Emit a 11-bit character with 2 bytes.
            writeByte((c shr 6 or 0xc0).toByte()) // 110xxxxx
            writeByte((c and 0x3f or 0x80).toByte()) // 10xxxxxx
            i++
        } else if (c < 0xd800 || c > 0xdfff) {
            // Emit a 16-bit character with 3 bytes.
            writeByte((c shr 12 or 0xe0).toByte()) // 1110xxxx
            writeByte((c shr 6 and 0x3f or 0x80).toByte()) // 10xxxxxx
            writeByte((c and 0x3f or 0x80).toByte()) // 10xxxxxx
            i++
        } else {
            // c is a surrogate. Make sure it is a high surrogate and that its successor is a low surrogate.
            // If not, the UTF-16 is invalid, in which case we emit a replacement character.
            val low = if (i + 1 < endIndex) this[i + 1].code else 0
            if (c > 0xdbff || low < 0xdc00 || low > 0xdfff) {
                writeByte('?'.code.toByte())
                i++
                continue
            }

            // UTF-16 high surrogate: 110110xxxxxxxxxx (10 bits)
            // UTF-16 low surrogate:  110111yyyyyyyyyy (10 bits)
            // Unicode code point:    00010000000000000000 + xxxxxxxxxxyyyyyyyyyy (21 bits)
            val codePoint = 0x010000 + (c and 0xd800.inv() shl 10 or (low and 0xdc00.inv()))

            // Emit a 21-bit character with 4 bytes.
            writeByte((codePoint shr 18 or 0xf0).toByte()) // 11110xxx
            writeByte((codePoint shr 12 and 0x3f or 0x80).toByte()) // 10xxxxxx
            writeByte((codePoint shr 6 and 0x3f or 0x80).toByte()) // 10xxyyyy
            writeByte((codePoint and 0x3f or 0x80).toByte()) // 10yyyyyy
            i += 2
        }
    }
}

/**
 * Returns the number of bytes used to encode `string` as UTF-8 when using [Int.encodeUtf8].
 */
fun Int.utf8Size(): Int {
    return when {
        this < 0x80 -> 1
        this < 0x800 -> 2
        this < 0xd800 -> 3
        this < 0xe000 -> 1
        this < 0x10000 -> 3
        else -> 4
    }
}

/**
 * Encodes this code point using UTF-8.
 */
inline fun Int.encodeUtf8(crossinline writeByte: (Byte) -> Unit) {
    val codePoint = this
    if (codePoint < 0x80) {
        // Emit a 7-bit character with 1 byte.
        writeByte(codePoint.toByte()) // 0xxxxxxx
    } else if (codePoint < 0x800) {
        // Emit a 11-bit character with 2 bytes.
        writeByte((codePoint shr 6 or 0xc0).toByte()) // 110xxxxx
        writeByte((codePoint and 0x3f or 0x80).toByte()) // 10xxxxxx
    } else if (codePoint < 0xd800 || codePoint in 0xe000..0x10000) {
        // Emit a 16-bit character with 3 bytes.
        writeByte((codePoint shr 12 or 0xe0).toByte()) // 1110xxxx
        writeByte((codePoint shr 6 and 0x3f or 0x80).toByte()) // 10xxxxxx
        writeByte((codePoint and 0x3f or 0x80).toByte()) // 10xxxxxx
    } else if (codePoint in 0xd800..0xdfff) {
        // codePoint is a surrogate. Emit a replacement character
        writeByte('?'.code.toByte())
    } else {
        // Emit a 21-bit character with 4 bytes.
        writeByte((codePoint shr 18 or 0xf0).toByte()) // 11110xxx
        writeByte((codePoint shr 12 and 0x3f or 0x80).toByte()) // 10xxxxxx
        writeByte((codePoint shr 6 and 0x3f or 0x80).toByte()) // 10xxyyyy
        writeByte((codePoint and 0x3f or 0x80).toByte()) // 10yyyyyy
    }
}
