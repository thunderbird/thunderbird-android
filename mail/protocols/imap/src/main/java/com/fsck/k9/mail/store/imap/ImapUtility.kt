/*
 * Copyright (C) 2012 The K-9 Dog Walkers
 * Copyright (C) 2011 The Android Open Source Project
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
package com.fsck.k9.mail.store.imap

import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.logging.legacy.Log

/**
 * Utility methods for use with IMAP.
 */
internal object ImapUtility {

    /**
     * Gets all of the values in a sequence set per RFC 3501.
     *
     * <p>
     * Any ranges are expanded into a list of individual numbers.
     * </p>
     *
     * <pre>
     * sequence-number = nz-number / "*"
     * sequence-range = sequence-number ":" sequence-number
     * sequence-set = (sequence-number / sequence-range) *("," sequence-set)
     * </pre>
     *
     * @param set The sequence set string as received by the server.
     * @return The list of IDs as strings in this sequence set. If the set is invalid, an empty
     *         list is returned.
     */
    fun getImapSequenceValues(set: String?): List<String> {
        set ?: return emptyList()
        return set.split(",").flatMap { item ->
            if (':' !in item) {
                if (isNumberValid(item)) listOf(item) else emptyList()
            } else {
                getImapRangeValues(item)
            }
        }
    }

    /**
     * Expand the given number range into a list of individual numbers.
     *
     * <pre>
     * sequence-number = nz-number / "*"
     * sequence-range = sequence-number ":" sequence-number
     * sequence-set = (sequence-number / sequence-range) *("," sequence-set)
     * </pre>
     *
     * @param range The range string as received by the server.
     * @return The list of IDs as strings in this range. If the range is not valid, an empty list
     *         is returned.
     */
    fun getImapRangeValues(range: String?): List<String> {
        val (first, second) = parseRangeBounds(range) ?: return emptyList()
        return if (first <= second) {
            (first..second).map { it.toString() }
        } else {
            (first downTo second).map { it.toString() }
        }
    }
    private fun parseRangeBounds(range: String?): Pair<Long, Long>? {
        val colonPos = range?.indexOf(':')?.takeIf { it > 0 } ?: return null

        return try {
            val first = range.substring(0, colonPos).toLong()
            val second = range.substring(colonPos + 1).toLong()
            if (is32bitValue(first) && is32bitValue(second)) {
                first to second
            } else {
                Log.d("Invalid range: %s", range)
                null
            }
        } catch (e: NumberFormatException) {
            Log.d(e, "Invalid range value: %s", range)
            null
        }
    }

    private fun isNumberValid(number: String): Boolean {
        val value = number.toLongOrNull()
        if (value != null && is32bitValue(value)) return true
        Log.d("Invalid UID value: %s", number)
        return false
    }

    private fun is32bitValue(value: Long): Boolean {
        return (value and 0xFFFFFFFFL.inv()) == 0L
    }

    /**
     * Encode a string to be able to use it in an IMAP command.
     *
     * "A quoted string is a sequence of zero or more 7-bit characters,
     *  excluding CR and LF, with double quote (<">) characters at each
     *  end." - Section 4.3, RFC 3501
     *
     * Double quotes and backslash are escaped by prepending a backslash.
     *
     * @param str The input string (only 7-bit characters allowed).
     * @return The string encoded as quoted (IMAP) string.
     */
    // TODO use a literal string
    @JvmStatic
    fun encodeString(str: String): String {
        return "\"" + str.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }

    internal fun combineFlags(flags: Iterable<Flag>, canCreateForwardedFlag: Boolean): String {
        return flags.mapNotNull { flag ->
            when (flag) {
                Flag.SEEN -> "\\Seen"
                Flag.DELETED -> "\\Deleted"
                Flag.ANSWERED -> "\\Answered"
                Flag.FLAGGED -> "\\Flagged"
                Flag.FORWARDED -> if (canCreateForwardedFlag) $$"$Forwarded" else null
                Flag.DRAFT -> "\\Draft"
                else -> null
            }
        }.joinToString(" ")
    }
}
