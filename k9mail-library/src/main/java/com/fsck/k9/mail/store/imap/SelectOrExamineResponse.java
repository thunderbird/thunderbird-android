package com.fsck.k9.mail.store.imap;


import com.fsck.k9.mail.Folder;

import java.util.List;

import static com.fsck.k9.mail.store.imap.ImapResponseParser.equalsIgnoreCase;


class SelectOrExamineResponse {
    private final Boolean readWriteMode;


    private SelectOrExamineResponse(Boolean readWriteMode) {
        this.readWriteMode = readWriteMode;
    }

    public static SelectOrExamineResponse parse(ImapResponse response) {
        if (!response.isTagged() || !equalsIgnoreCase(response.get(0), Responses.OK)) {
            return null;
        }

        if (!response.isList(1)) {
            return noOpenModeInResponse();
        }

        ImapList responseTextList = response.getList(1);
        if (!responseTextList.isString(0)) {
            return noOpenModeInResponse();
        }

        String responseCode = responseTextList.getString(0);
        if ("READ-ONLY".equalsIgnoreCase(responseCode)) {
            return new SelectOrExamineResponse(false);
        } else if ("READ-WRITE".equalsIgnoreCase(responseCode)) {
            return new SelectOrExamineResponse(true);
        }

        return noOpenModeInResponse();
    }

    public static long extractUidValidity(List<ImapResponse> responses) {
        for(ImapResponse response : responses) {
            int index = 0;
            while (index < response.size()) {
                if (response.isList(index)) {
                    ImapList listResponse = response.getList(index);
                    if (listResponse.isString(0) && listResponse.getString(0).equals("UIDVALIDITY")) {
                        return Long.parseLong(listResponse.getString(1));
                    }
                }
                index++;
            }
        }

        //UIDVALIDITY was not found
        return -1L;
    }

    private static SelectOrExamineResponse noOpenModeInResponse() {
        return new SelectOrExamineResponse(null);
    }

    public boolean hasOpenMode() {
        return readWriteMode != null;
    }

    public int getOpenMode() {
        if (!hasOpenMode()) {
            throw new IllegalStateException("Called getOpenMode() despite hasOpenMode() returning false");
        }

        return readWriteMode ? Folder.OPEN_MODE_RW : Folder.OPEN_MODE_RO;
    }
}
