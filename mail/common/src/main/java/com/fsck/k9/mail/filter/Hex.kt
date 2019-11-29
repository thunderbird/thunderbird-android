/*
 * Copyright 2018 The K-9 Dog Walkers
 * Copyright 2001-2004 The Apache Software Foundation.
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

package com.fsck.k9.mail.filter

/**
 * This code was copied from the Apache Commons project.
 * The unnecessary parts have been left out.
 */
object Hex {
    private val LOWER_CASE = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
    private val UPPER_CASE = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

    /**
     * Converts an array of bytes into an array of characters representing the hexadecimal values of each byte in order.
     * The returned array will be double the length of the passed array, as it takes two characters to represent any
     * given byte.
     *
     * @param data a byte[] to convert to Hex characters
     * @return A String containing lower-case hexadecimal characters
     */
    @JvmStatic
    fun encodeHex(data: ByteArray): String {
        val l = data.size
        val out = CharArray(l shl 1)

        // two characters form the hex value.
        var i = 0
        var j = 0
        while (i < l) {
            out[j++] = LOWER_CASE[data[i].toInt() shr 4 and 0x0F]
            out[j++] = LOWER_CASE[data[i].toInt() and 0x0F]
            i++
        }

        return String(out)
    }

    fun StringBuilder.appendHex(value: Byte, lowerCase: Boolean = true) {
        val digits = if (lowerCase) LOWER_CASE else UPPER_CASE
        append(digits[value.toInt() shr 4 and 0x0F])
        append(digits[value.toInt() and 0x0F])
    }
}
