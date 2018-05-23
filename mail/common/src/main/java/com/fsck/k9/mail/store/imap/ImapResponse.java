package com.fsck.k9.mail.store.imap;

/**
 * Represents a single response from the IMAP server.
 *
 * <p>
 * Tagged responses will have a non-null tag. Untagged responses will have a null tag. The
 * object will contain all of the available tokens at the time the response is received.
 * </p>
 */
class ImapResponse extends ImapList {
    private static final long serialVersionUID = 6886458551615975669L;


    private ImapResponseCallback callback;
    private final boolean commandContinuationRequested;
    private final String tag;


    private ImapResponse(ImapResponseCallback callback, boolean commandContinuationRequested, String tag) {
        this.callback = callback;
        this.commandContinuationRequested = commandContinuationRequested;
        this.tag = tag;
    }

    public static ImapResponse newContinuationRequest(ImapResponseCallback callback) {
        return new ImapResponse(callback, true, null);
    }

    public static ImapResponse newUntaggedResponse(ImapResponseCallback callback) {
        return new ImapResponse(callback, false, null);
    }

    public static ImapResponse newTaggedResponse(ImapResponseCallback callback, String tag) {
        return new ImapResponse(callback, false, tag);
    }

    public boolean isContinuationRequested() {
        return commandContinuationRequested;
    }

    public String getTag() {
        return tag;
    }

    public boolean isTagged() {
        return tag != null;
    }

    public ImapResponseCallback getCallback() {
        return callback;
    }

    public void setCallback(ImapResponseCallback callback) {
        this.callback = callback;
    }

    @Override
    public String toString() {
        return "#" + (commandContinuationRequested ? "+" : tag) + "# " + super.toString();
    }
}
