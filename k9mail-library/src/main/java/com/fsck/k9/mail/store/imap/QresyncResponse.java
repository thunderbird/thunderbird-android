package com.fsck.k9.mail.store.imap;


import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.mail.MessagingException;


public class QresyncResponse {

    private List<String> expungedUids;
    private List<ImapMessage> modifiedMessages;

    private QresyncResponse() {
        expungedUids = new ArrayList<>();
        modifiedMessages = new ArrayList<>();
    }

    public static QresyncResponse parse(List<ImapResponse> imapResponses, ImapFolder folder) throws MessagingException {
        QresyncResponse qresyncResponse = new QresyncResponse();
        for (ImapResponse imapResponse : imapResponses) {
            handleExpungedUids(imapResponse, qresyncResponse);
            handleModifiedMessages(imapResponse, qresyncResponse, folder);
        }
        return qresyncResponse;
    }

    private static void handleExpungedUids(ImapResponse imapResponse, QresyncResponse qresyncResponse) {
        if (imapResponse.getTag() == null && ImapResponseParser.equalsIgnoreCase(imapResponse.get(0), "VANISHED") &&
                imapResponse.isString(2)) {
            qresyncResponse.expungedUids.addAll(ImapUtility.getImapSequenceValues(imapResponse.getString(2)));
        }
    }

    private static void handleModifiedMessages(ImapResponse imapResponse, QresyncResponse qresyncResponse,
            ImapFolder folder) throws MessagingException {
        if (imapResponse.getTag() == null && ImapResponseParser.equalsIgnoreCase(imapResponse.get(1), "FETCH")) {
            ImapList fetchList = (ImapList) imapResponse.getKeyedValue("FETCH");
            String uid = fetchList.getKeyedString("UID");
            ImapMessage message = new ImapMessage(uid, folder);
            ImapUtility.setMessageFlags(fetchList, message, folder.store);
            qresyncResponse.modifiedMessages.add(message);
        }
    }

    public List<String> getExpungedUids() {
        return expungedUids;
    }

    public List<ImapMessage> getModifiedMessages() {
        return modifiedMessages;
    }
}
