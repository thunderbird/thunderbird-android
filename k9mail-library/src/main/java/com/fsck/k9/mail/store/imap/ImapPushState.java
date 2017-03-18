package com.fsck.k9.mail.store.imap;


import timber.log.Timber;


class ImapPushState {
    private static final long DEFAULT_UID_NEXT = -1L;
    private static final String PUSH_STATE_PREFIX = "uidNext=";
    private static final int PUSH_STATE_PREFIX_LENGTH = 8;


    public final long uidNext;

    public static ImapPushState parse(String pushState) {
        if (pushState == null || !pushState.startsWith(PUSH_STATE_PREFIX)) {
            return createDefaultImapPushState();
        }

        String value = pushState.substring(PUSH_STATE_PREFIX_LENGTH);
        try {
            long newUidNext = Long.parseLong(value);

            return new ImapPushState(newUidNext);
        } catch (NumberFormatException e) {
            Timber.e(e, "Unable to part uidNext value %s", value);
        }

        return createDefaultImapPushState();
    }

    static ImapPushState createDefaultImapPushState() {
        return new ImapPushState(DEFAULT_UID_NEXT);
    }

    public ImapPushState(long uidNext) {
        this.uidNext = uidNext;
    }

    @Override
    public String toString() {
        return "uidNext=" + uidNext;
    }
}
