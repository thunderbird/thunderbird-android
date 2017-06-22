package com.fsck.k9.mail.store.imap.response;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fsck.k9.mail.store.imap.ImapList;
import com.fsck.k9.mail.store.imap.ImapResponse;
import com.fsck.k9.mail.store.imap.ImapUtility;
import com.fsck.k9.mail.store.imap.Responses;
import com.fsck.k9.mail.store.imap.command.ImapCommandFactory;

import static com.fsck.k9.mail.store.imap.ImapResponseParser.equalsIgnoreCase;
import static com.fsck.k9.mail.store.imap.ImapUtility.getImapSequenceValues;


public class CopyUidResponse extends BaseResponse {

    private Map<String, String> uidMapping;

    private CopyUidResponse(ImapCommandFactory commandFactory, List<ImapResponse> imapResponse) {
        super(commandFactory, imapResponse);
    }

    public static CopyUidResponse parse(ImapCommandFactory commandFactory, List<List<ImapResponse>> imapResponses) {

        CopyUidResponse combinedResponse = null;
        for (List<ImapResponse> imapResponse : imapResponses) {
            CopyUidResponse copyUidResponse = new CopyUidResponse(commandFactory, imapResponse);
            if (combinedResponse == null) {
                combinedResponse = copyUidResponse;
            } else {
                combinedResponse.combine(copyUidResponse);
            }
        }

        return combinedResponse;
    }

    @Override
    void parseResponse(List<ImapResponse> imapResponses) {

        ImapResponse response = ImapUtility.getLastResponse(imapResponses);

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

        uidMapping = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String sourceUid = sourceUids.get(i);
            String destinationUid = destinationUids.get(i);
            uidMapping.put(sourceUid, destinationUid);
        }
    }

    @Override
    void combine(BaseResponse baseResponse) {
        if (baseResponse == null) {
            return;
        }
        super.combine(baseResponse);
        CopyUidResponse copyUidResponse = (CopyUidResponse) baseResponse;
        this.uidMapping.putAll(copyUidResponse.getUidMapping());
    }


    public Map<String, String> getUidMapping() {
        return uidMapping;
    }
}
