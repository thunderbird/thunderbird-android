package com.fsck.k9.mail.store.imap;

/**
 * Represents a single response from the IMAP server.
 *
 * <p>
 * Tagged responses will have a non-null tag. Untagged responses will have a null tag. The
 * object will contain all of the available tokens at the time the response is received.
 * </p>
 */
public class ImapResponse extends ImapList {
    private static final long serialVersionUID = 6886458551615975669L;

    private ImapResponseCallback mCallback;

    private final boolean mCommandContinuationRequested;
    private final String mTag;


    public ImapResponse(ImapResponseCallback callback,
                        boolean mCommandContinuationRequested, String mTag) {
        this.mCallback = callback;
        this.mCommandContinuationRequested = mCommandContinuationRequested;
        this.mTag = mTag;
    }

    public boolean isContinuationRequested() {
        return mCommandContinuationRequested;
    }

    public String getTag() {
        return mTag;
    }

    public ImapResponseCallback getCallback() {
        return mCallback;
    }

    public void setCallback(ImapResponseCallback mCallback) {
        this.mCallback = mCallback;
    }

    public String getAlertText() {
        if (size() > 1 && ImapResponseParser.equalsIgnoreCase("[ALERT]", get(1))) {
            StringBuilder sb = new StringBuilder();
            for (int i = 2, count = size(); i < count; i++) {
                sb.append(get(i).toString());
                sb.append(' ');
            }
            return sb.toString();
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "#" + (mCommandContinuationRequested ? "+" : mTag) + "# " + super.toString();
    }
}
