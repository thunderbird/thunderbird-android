package com.fsck.k9.mail.store.imap;


import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;

import static com.fsck.k9.mail.store.imap.ImapResponseParser.equalsIgnoreCase;


class SelectOrExamineResponse {

    private long uidValidity;
    private long highestModSeq = -1L;
    private boolean canCreateKeywords;
    private QresyncResponse qresyncResponse;
    private Boolean readWriteMode;

    private SelectOrExamineResponse() {
    }

    public static SelectOrExamineResponse parse(List<ImapResponse> imapResponses, ImapFolder folder) throws IOException,
            MessagingException {
        SelectOrExamineResponse selectOrExamineResponse = new SelectOrExamineResponse();
        for (ImapResponse imapResponse : imapResponses) {
            if (imapResponse.isTagged() || !equalsIgnoreCase(imapResponse.get(0), Responses.OK)
                    || !imapResponse.isList(1)) {
                continue;
            }
            handleUidValidity(imapResponse, selectOrExamineResponse);
            handleHighestModSeq(imapResponse, selectOrExamineResponse);
            handlePermanentFlags(imapResponse, selectOrExamineResponse, folder.store.getPermanentFlagsIndex());
        }
        selectOrExamineResponse.readWriteMode = isModeReadWrite(ImapUtility.getLastResponse(imapResponses));
        selectOrExamineResponse.qresyncResponse = QresyncResponse.parse(imapResponses, folder);
        return selectOrExamineResponse;
    }

    private static void handleUidValidity(ImapResponse imapResponse, SelectOrExamineResponse selectOrExamineResponse) {
        ImapList responseTextList = imapResponse.getList(1);
        if (responseTextList.size() < 2 || !(equalsIgnoreCase(responseTextList.get(0), Responses.UIDVALIDITY))) {
            return;
        }
        selectOrExamineResponse.uidValidity = Long.parseLong(responseTextList.getString(1));
    }

    private static void handleHighestModSeq(ImapResponse imapResponse, SelectOrExamineResponse selectOrExamineResponse)
            throws IOException, MessagingException {
        ImapList responseTextList = imapResponse.getList(1);
        if (responseTextList.size() < 2 || !(equalsIgnoreCase(responseTextList.get(0), Responses.HIGHESTMODSEQ)
                || equalsIgnoreCase(responseTextList.get(1), Responses.NOMODSEQ)) ||
                !responseTextList.isString(1)) {
            return;
        }

        if (equalsIgnoreCase(responseTextList.get(0), Responses.HIGHESTMODSEQ)) {
            selectOrExamineResponse.highestModSeq = Long.parseLong(responseTextList.getString(1));
        }
    }

    private static void handlePermanentFlags(ImapResponse imapResponse, SelectOrExamineResponse selectOrExamineResponse,
            Set<Flag> permanentFlags) {
        PermanentFlagsResponse permanentFlagsResponse = PermanentFlagsResponse.parse(imapResponse);
        if (permanentFlagsResponse == null) {
            return;
        }

        permanentFlags.addAll(permanentFlagsResponse.getFlags());
        selectOrExamineResponse.canCreateKeywords = permanentFlagsResponse.canCreateKeywords();
    }

    private static Boolean isModeReadWrite(ImapResponse imapResponse) {
        if (!imapResponse.isTagged() || !equalsIgnoreCase(imapResponse.get(0), Responses.OK)) {
            return null;
        }

        if (!imapResponse.isList(1)) {
            return null;
        }

        ImapList responseTextList = imapResponse.getList(1);
        if (!responseTextList.isString(0)) {
            return null;
        }

        String responseCode = responseTextList.getString(0);
        if ("READ-ONLY".equalsIgnoreCase(responseCode)) {
            return Boolean.FALSE;
        } else if ("READ-WRITE".equalsIgnoreCase(responseCode)) {
            return Boolean.TRUE;
        }

        return null;
    }

    long getUidValidity() {
        return uidValidity;
    }

    long getHighestModSeq() {
        return highestModSeq;
    }

    boolean canCreateKeywords() {
        return canCreateKeywords;
    }

    QresyncResponse getQresyncResponse() {
        return qresyncResponse;
    }

    boolean hasOpenMode() {
        return readWriteMode != null;
    }

    int getOpenMode() {
        if (!hasOpenMode()) {
            throw new IllegalStateException("Called getOpenMode() despite hasOpenMode() returning false");
        }

        return readWriteMode ? Folder.OPEN_MODE_RW : Folder.OPEN_MODE_RO;
    }
}
