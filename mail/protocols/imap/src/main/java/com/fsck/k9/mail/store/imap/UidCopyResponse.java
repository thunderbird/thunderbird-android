package com.fsck.k9.mail.store.imap;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.fsck.k9.mail.store.imap.ImapResponseParser.equalsIgnoreCase;
import static com.fsck.k9.mail.store.imap.ImapUtility.getImapSequenceValues;


class UidCopyResponse {
    private final Map<String, String> uidMapping;


    private UidCopyResponse(Map<String, String> uidMapping) {
        this.uidMapping = uidMapping;
    }

    public static UidCopyResponse parse(List<ImapResponse> imapResponses) {
        Map<String, String> uidMapping = new LinkedHashMap<>();
        for (ImapResponse imapResponse : imapResponses) {
            parseUidCopyResponse(imapResponse, uidMapping);
        }

        return uidMapping.isEmpty() ? null : new UidCopyResponse(uidMapping);
    }

    private static void parseUidCopyResponse(ImapResponse response, Map<String, String> uidMappingOutput) {
        if (!response.isTagged() || response.size() < 2 || !equalsIgnoreCase(response.get(0), Responses.OK) ||
                !response.isList(1)) {
            return;
        }

        ImapList responseTextList = response.getList(1);
        if (responseTextList.size() < 4 || !equalsIgnoreCase(responseTextList.get(0), Responses.COPYUID) ||
                !responseTextList.isString(1) || !responseTextList.isString(2) || !responseTextList.isString(3)) {
            return;
        }

        List<String> sourceUids = getImapSequenceValues(responseTextList.getString(2));
        List<String> destinationUids = getImapSequenceValues(responseTextList.getString(3));

        int size = sourceUids.size();
        if (size == 0 || size != destinationUids.size()) {
            return;
        }

        for (int i = 0; i < size; i++) {
            String sourceUid = sourceUids.get(i);
            String destinationUid = destinationUids.get(i);
            uidMappingOutput.put(sourceUid, destinationUid);
        }
    }

    public Map<String, String> getUidMapping() {
        return uidMapping;
    }
}
