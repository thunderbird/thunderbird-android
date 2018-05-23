package com.fsck.k9.mail.store.imap;


class ResponseCodeExtractor {
    public static final String AUTHENTICATION_FAILED = "AUTHENTICATIONFAILED";


    private ResponseCodeExtractor() {
    }

    public static String getResponseCode(ImapResponse response) {
        if (response.size() < 2 || !response.isList(1)) {
            return null;
        }

        ImapList responseTextCode = response.getList(1);
        return responseTextCode.size() != 1 ? null : responseTextCode.getString(0);
    }
}
