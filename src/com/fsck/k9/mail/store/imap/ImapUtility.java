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

package com.fsck.k9.mail.store.imap;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static com.fsck.k9.mail.K9MailLib.LOG_TAG;

/**
 * Utility methods for use with IMAP.
 */
class ImapUtility {
    /**
     * Gets all of the values in a sequence set per RFC 3501.
     *
     * <p>
     * Any ranges are expanded into a list of individual numbers.
     * </p>
     *
     * <pre>
     * sequence-number = nz-number / "*"
     * sequence-range  = sequence-number ":" sequence-number
     * sequence-set    = (sequence-number / sequence-range) *("," sequence-set)
     * </pre>
     *
     * @param set
     *         The sequence set string as received by the server.
     *
     * @return The list of IDs as strings in this sequence set. If the set is invalid, an empty
     *         list is returned.
     */
    public static List<String> getImapSequenceValues(String set) {
        List<String> list = new ArrayList<String>();
        if (set != null) {
            String[] setItems = set.split(",");
            for (String item : setItems) {
                if (item.indexOf(':') == -1) {
                    // simple item
                    if (isNumberValid(item)) {
                        list.add(item);
                    }
                } else {
                    // range
                    list.addAll(getImapRangeValues(item));
                }
            }
        }

        return list;
    }

    /**
     * Expand the given number range into a list of individual numbers.
     *
     * <pre>
     * sequence-number = nz-number / "*"
     * sequence-range  = sequence-number ":" sequence-number
     * sequence-set    = (sequence-number / sequence-range) *("," sequence-set)
     * </pre>
     *
     * @param range
     *         The range string as received by the server.
     *
     * @return The list of IDs as strings in this range. If the range is not valid, an empty list
     *         is returned.
     */
    public static List<String> getImapRangeValues(String range) {
        List<String> list = new ArrayList<String>();
        try {
            if (range != null) {
                int colonPos = range.indexOf(':');
                if (colonPos > 0) {
                    long first  = Long.parseLong(range.substring(0, colonPos));
                    long second = Long.parseLong(range.substring(colonPos + 1));
                    if (is32bitValue(first) && is32bitValue(second)) {
                        if (first < second) {
                            for (long i = first; i <= second; i++) {
                                list.add(Long.toString(i));
                            }
                        } else {
                            for (long i = first; i >= second; i--) {
                                list.add(Long.toString(i));
                            }
                        }
                    } else {
                        Log.d(LOG_TAG, "Invalid range: " + range);
                    }
                }
            }
        } catch (NumberFormatException e) {
            Log.d(LOG_TAG, "Invalid range value: " + range, e);
        }

        return list;
    }

    private static boolean isNumberValid(String number) {
        try {
            long value = Long.parseLong(number);
            if (is32bitValue(value)) {
                return true;
            }
        } catch (NumberFormatException e) {
            // do nothing
        }

        Log.d(LOG_TAG, "Invalid UID value: " + number);

        return false;
    }

    private static boolean is32bitValue(long value) {
        return ((value & ~0xFFFFFFFFL) == 0L);
    }
}
