package com.fsck.k9.mail.store.webdav;

import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.fsck.k9.mail.K9MailLib.LOG_TAG;

/**
 * Maintains WebDAV data
 */
class DataSet {
    private Map<String, Map<String, String>> mData = new HashMap<String, Map<String, String>>();
    private StringBuilder mUid = new StringBuilder();
    private Map<String, String> mTempData = new HashMap<String, String>();

    public void addValue(String value, String tagName) {
        if (tagName.equals("uid")) {
            mUid.append(value);
        }

        if (mTempData.containsKey(tagName)) {
            mTempData.put(tagName, mTempData.get(tagName) + value);
        } else {
            mTempData.put(tagName, value);
        }
    }

    public void finish() {
        String uid = mUid.toString();
        if (mTempData != null) {
            mData.put(uid, mTempData);
        } else if (mTempData != null) {
            //TODO: This logic is clearly wrong
                /*
                 * Lost Data are for requests that don't include a message UID. These requests should only have a depth
                 * of one for the response so it will never get stomped over.
                 */
        }

        mUid = new StringBuilder();
        mTempData = new HashMap<String, String>();
    }

    /**
     * Returns a hashmap of special folder name => special folder url
     */
    public Map<String, String> getSpecialFolderToUrl() {
        // We return the first (and only) map
        for (Map<String, String> folderMap : mData.values()) {
            return folderMap;
        }
        return new HashMap<String, String>();
    }

    /**
     * Returns a hashmap of Message UID => Message Url
     */
    public Map<String, String> getUidToUrl() {
        Map<String, String> uidToUrl = new HashMap<String, String>();

        for (String uid : mData.keySet()) {
            Map<String, String> data = mData.get(uid);
            String value = data.get("href");
            if (value != null &&
                    !value.equals("")) {
                uidToUrl.put(uid, value);
            }
        }

        return uidToUrl;
    }

    /**
     * Returns a hashmap of Message UID => Read Status
     */
    public Map<String, Boolean> getUidToRead() {
        Map<String, Boolean> uidToRead = new HashMap<String, Boolean>();

        for (String uid : mData.keySet()) {
            Map<String, String> data = mData.get(uid);
            String readStatus = data.get("read");
            if (readStatus != null && !readStatus.equals("")) {
                Boolean value = !readStatus.equals("0");
                uidToRead.put(uid, value);
            } else {
                // We don't actually want to have null values in our hashmap,
                // as it causes the calling code to crash with an NPE as it
                // does a lookup in the map.
                uidToRead.put(uid, false);
            }
        }

        return uidToRead;
    }

    /**
     * Returns an array of all hrefs (urls) that were received
     */
    public String[] getHrefs() {
        List<String> hrefs = new ArrayList<String>();

        for (String uid : mData.keySet()) {
            Map<String, String> data = mData.get(uid);
            String href = data.get("href");
            hrefs.add(href);
        }

        return hrefs.toArray(WebDavConstants.EMPTY_STRING_ARRAY);
    }

    /**
     * Return an array of all Message UIDs that were received
     */
    public String[] getUids() {
        List<String> uids = new ArrayList<String>();

        for (String uid : mData.keySet()) {
            uids.add(uid);
        }

        return uids.toArray(WebDavConstants.EMPTY_STRING_ARRAY);
    }

    /**
     * Returns the message count as it was retrieved
     */
    public int getMessageCount() {
        // It appears that Exchange is returning responses
        // without a visiblecount element for empty folders
        // Which resulted in this code returning -1 (as that was
        // the previous default.)
        // -1 is an error condition. Now the default is empty
        int messageCount = 0;

        for (String uid : mData.keySet()) {
            Map<String, String> data = mData.get(uid);
            String count = data.get("visiblecount");

            if (count != null &&
                    !count.equals("")) {
                messageCount = Integer.parseInt(count);
            }

        }

        return messageCount;
    }

    /**
     * Returns a Map of message UID => ParsedMessageEnvelope
     */
    public Map<String, ParsedMessageEnvelope> getMessageEnvelopes() {
        Map<String, ParsedMessageEnvelope> envelopes = new HashMap<String, ParsedMessageEnvelope>();

        for (String uid : mData.keySet()) {
            ParsedMessageEnvelope envelope = new ParsedMessageEnvelope();
            Map<String, String> data = mData.get(uid);

            if (data != null) {
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    String header = entry.getKey();
                    if (header.equals("read")) {
                        String read = entry.getValue();
                        boolean readStatus = !read.equals("0");

                        envelope.setReadStatus(readStatus);
                    } else if (header.equals("date")) {
                        /**
                         * Exchange doesn't give us rfc822 dates like it claims. The date is in the format:
                         * yyyy-MM-dd'T'HH:mm:ss.SSS<Single digit representation of timezone, so far, all instances
                         * are Z>
                         */
                        String date = entry.getValue();
                        date = date.substring(0, date.length() - 1);

                        DateFormat dfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
                        DateFormat dfOutput = new SimpleDateFormat("EEE, d MMM yy HH:mm:ss Z", Locale.US);
                        String tempDate = "";

                        try {
                            Date parsedDate = dfInput.parse(date);
                            tempDate = dfOutput.format(parsedDate);
                        } catch (java.text.ParseException pe) {
                            Log.e(LOG_TAG, "Error parsing date: " + pe + "\nTrace: " + WebDavUtils.processException(pe));
                        }
                        envelope.addHeader(header, tempDate);
                    } else {
                        envelope.addHeader(header, entry.getValue());
                    }
                }
            }

            envelopes.put(uid, envelope);
        }

        return envelopes;
    }
}

