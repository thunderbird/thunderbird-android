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
    @Suppress("NestedBlockDepth")
    fun getImapSequenceValues(set: String?): List<String> {
        val list = mutableListOf<String>()
        if (set != null) {
            val setItems = set.split(",")
            for (item in setItems) {
                if (item.indexOf(':') == -1) {
                    // simple item
                    if (isNumberValid(item)) {
                        list.add(item)
                    }
                } else {
                    // range
                    list.addAll(getImapRangeValues(item))
                }
            }
        }
        return list
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
    @Suppress("NestedBlockDepth")
    fun getImapRangeValues(range: String?): List<String> {
        val list = mutableListOf<String>()
        try {
            if (range != null) {
                val colonPos = range.indexOf(':')
                if (colonPos > 0) {
                    val first = range.substring(0, colonPos).toLong()
                    val second = range.substring(colonPos + 1).toLong()
                    if (is32bitValue(first) && is32bitValue(second)) {
                        if (first < second) {
                            for (i in first..second) {
                                list.add(i.toString())
                            }
                        } else {
                            for (i in first downTo second) {
                                list.add(i.toString())
                            }
                        }
                    } else {
                        Log.d("Invalid range: %s", range)
                    }
                }
            }
        } catch (e: NumberFormatException) {
            Log.d(e, "Invalid range value: %s", range)
        }
        return list
    }

    private fun isNumberValid(number: String): Boolean {
        try {
            val value = number.toLong()
            if (is32bitValue(value)) {
                return true
            }
        } catch (_: NumberFormatException) {
            // do nothing
        }
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

    internal fun getLastResponse(responses: List<ImapResponse>): ImapResponse {
        val lastIndex = responses.size - 1
        return responses[lastIndex]
    }

    internal fun combineFlags(flags: Iterable<Flag>, canCreateForwardedFlag: Boolean): String {
        val flagNames = mutableListOf<String>()
        for (flag in flags) {
            if (flag == Flag.SEEN) {
                flagNames.add("\\Seen")
            } else if (flag == Flag.DELETED) {
                flagNames.add("\\Deleted")
            } else if (flag == Flag.ANSWERED) {
                flagNames.add("\\Answered")
            } else if (flag == Flag.FLAGGED) {
                flagNames.add("\\Flagged")
            } else if (flag == Flag.FORWARDED && canCreateForwardedFlag) {
                flagNames.add("\$Forwarded")
            } else if (flag == Flag.DRAFT) {
                flagNames.add("\\Draft")
            }
        }
        return join(" ", flagNames) ?: ""
    }

    private fun join(delimiter: String, tokens: Collection<*>?): String? {
        if (tokens == null) {
            return null
        }
        val sb = StringBuilder()
        var firstTime = true
        for (token in tokens) {
            if (firstTime) {
                firstTime = false
            } else {
                sb.append(delimiter)
            }
            sb.append(token)
        }
        return sb.toString()
    }
}
