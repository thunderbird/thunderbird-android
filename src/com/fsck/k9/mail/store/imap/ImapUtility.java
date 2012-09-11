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

import com.fsck.k9.K9;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for use with IMAP.
 */
public class ImapUtility {
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
        ArrayList<String> list = new ArrayList<String>();
        if (set != null) {
            String[] setItems = set.split(",");
            for (String item : setItems) {
                if (item.indexOf(':') == -1) {
                    // simple item
                    try {
                        Integer.parseInt(item); // Don't need the value; just ensure it's valid
                        list.add(item);
                    } catch (NumberFormatException e) {
                        Log.d(K9.LOG_TAG, "Invalid UID value", e);
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
        ArrayList<String> list = new ArrayList<String>();
        try {
            if (range != null) {
                int colonPos = range.indexOf(':');
                if (colonPos > 0) {
                    int first  = Integer.parseInt(range.substring(0, colonPos));
                    int second = Integer.parseInt(range.substring(colonPos + 1));
                    if (first < second) {
                        for (int i = first; i <= second; i++) {
                            list.add(Integer.toString(i));
                        }
                    } else {
                        for (int i = first; i >= second; i--) {
                            list.add(Integer.toString(i));
                        }
                    }
                }
            }
        } catch (NumberFormatException e) {
            Log.d(K9.LOG_TAG, "Invalid range value", e);
        }

        return list;
    }
}
