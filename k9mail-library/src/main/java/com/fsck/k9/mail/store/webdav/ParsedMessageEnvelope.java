package com.fsck.k9.mail.store.webdav;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the mappings from the name returned from Exchange to the MIME format header name
 */
class ParsedMessageEnvelope {
    private static final Map<String, String> HEADER_MAPPINGS;
    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put("mime-version", "MIME-Version");
        map.put("content-type", "Content-Type");
        map.put("subject", "Subject");
        map.put("date", "Date");
        map.put("thread-topic", "Thread-Topic");
        map.put("thread-index", "Thread-Index");
        map.put("from", "From");
        map.put("to", "To");
        map.put("in-reply-to", "In-Reply-To");
        map.put("cc", "Cc");
        map.put("getcontentlength", "Content-Length");
        HEADER_MAPPINGS = Collections.unmodifiableMap(map);
    }

    private boolean readStatus = false;
    private String uid = "";
    private Map<String, String> messageHeaders = new HashMap<>();
    private List<String> headers = new ArrayList<>();

    public void addHeader(String field, String value) {
        String headerName = HEADER_MAPPINGS.get(field);

        if (headerName != null) {
            this.messageHeaders.put(HEADER_MAPPINGS.get(field), value);
            this.headers.add(HEADER_MAPPINGS.get(field));
        }
    }

    public Map<String, String> getMessageHeaders() {
        return this.messageHeaders;
    }

    public String[] getHeaderList() {
        return this.headers.toArray(WebDavConstants.EMPTY_STRING_ARRAY);
    }

    public void setReadStatus(boolean status) {
        this.readStatus = status;
    }

    public boolean getReadStatus() {
        return this.readStatus;
    }

    public void setUid(String uid) {
        if (uid != null) {
            this.uid = uid;
        }
    }

    public String getUid() {
        return this.uid;
    }
}

