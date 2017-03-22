package com.fsck.k9.mail.store.imap;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fsck.k9.mail.store.imap.ImapResponseParser.equalsIgnoreCase;
import static com.fsck.k9.mail.store.imap.ImapUtility.getImapSequenceValues;


class CopyUidResponse {
    private final Map<String, String> uidMapping;


    private CopyUidResponse(Map<String, String> uidMapping) {
        this.uidMapping = Collections.unmodifiableMap(uidMapping);
    }

    public static CopyUidResponse parse(ImapResponse response) {
        if (!response.isTagged() || response.size() < 2 || !equalsIgnoreCase(response.get(0), Responses.OK) ||
                !response.isList(1)) {
            return null;
        }

        ImapList responseTextList = response.getList(1);
        if (responseTextList.size() < 4 || !equalsIgnoreCase(responseTextList.get(0), Responses.COPYUID) ||
                !responseTextList.isString(1) || !responseTextList.isString(2) || !responseTextList.isString(3)) {
            return null;
        }

        List<String> sourceUids = getImapSequenceValues(responseTextList.getString(2));
        List<String> destinationUids = getImapSequenceValues(responseTextList.getString(3));

        int size = sourceUids.size();
        if (size == 0 || size != destinationUids.size()) {
            return null;
        }

        Map<String, String> uidMapping = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String sourceUid = sourceUids.get(i);
            String destinationUid = destinationUids.get(i);
            uidMapping.put(sourceUid, destinationUid);
        }

        return new CopyUidResponse(uidMapping);
    }

    public Map<String, String> getUidMapping() {
        return uidMapping;
    }
}
