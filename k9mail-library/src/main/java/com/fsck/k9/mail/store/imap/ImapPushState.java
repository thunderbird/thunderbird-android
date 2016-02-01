package com.fsck.k9.mail.store.imap;


import java.util.StringTokenizer;

import android.util.Log;

import static com.fsck.k9.mail.K9MailLib.LOG_TAG;


class ImapPushState {
    protected long uidNext;
    protected ImapPushState(long nUidNext) {
        uidNext = nUidNext;
    }
    protected static ImapPushState parse(String pushState) {
        long newUidNext = -1L;
        if (pushState != null) {
            StringTokenizer tokenizer = new StringTokenizer(pushState, ";");
            while (tokenizer.hasMoreTokens()) {
                StringTokenizer thisState = new StringTokenizer(tokenizer.nextToken(), "=");
                if (thisState.hasMoreTokens()) {
                    String key = thisState.nextToken();

                    if ("uidNext".equalsIgnoreCase(key) && thisState.hasMoreTokens()) {
                        String value = thisState.nextToken();
                        try {
                            newUidNext = Long.parseLong(value);
                        } catch (NumberFormatException e) {
                            Log.e(LOG_TAG, "Unable to part uidNext value " + value, e);
                        }

                    }
                }
            }
        }
        return new ImapPushState(newUidNext);
    }
    @Override
    public String toString() {
        return "uidNext=" + uidNext;
    }

}
