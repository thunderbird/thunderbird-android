package com.fsck.k9.mail.store.imap;


import static com.fsck.k9.mail.store.imap.ImapResponseParser.equalsIgnoreCase;


class AlertResponse {
    private static final String ALERT_RESPONSE_CODE = "ALERT";


    private AlertResponse() {
    }

    public static String getAlertText(ImapResponse response) {
        if (response.size() < 3 || !response.isList(1)) {
            return null;
        }

        ImapList responseTextCode = response.getList(1);
        if (responseTextCode.size() != 1 || !equalsIgnoreCase(responseTextCode.get(0), ALERT_RESPONSE_CODE)) {
            return null;
        }

        return response.getString(2);
    }
}
