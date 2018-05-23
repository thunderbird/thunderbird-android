package com.fsck.k9.mail.store.imap;


import com.fsck.k9.mail.Folder;

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
