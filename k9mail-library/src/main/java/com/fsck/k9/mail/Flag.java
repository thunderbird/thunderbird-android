
package com.fsck.k9.mail;

/**
 * Flags that can be applied to Messages.
 */
public enum Flag {
    DELETED("DELETED", "UNDELETED"),
    SEEN("SEEN", "UNSEEN"),
    ANSWERED("ANSWERED", "UNANSWERED"),
    FLAGGED("FLAGGED", "UNFLAGGED"),
    DRAFT("DRAFT", "UNDRAFT"),
    RECENT("RECENT", "UNRECENT"),
    FORWARDED(null, null),

    /*
     * The following flags are for internal library use only.
     */
    /**
     * Delete and remove from the LocalStore immediately.
     */
    X_DESTROYED(null, null),

    /**
     * Sending of an unsent message failed. It will be retried. Used to show status.
     */
    X_SEND_FAILED(null, null),

    /**
     * Sending of an unsent message is in progress.
     */
    X_SEND_IN_PROGRESS(null, null),

    /**
     * Indicates that a message is fully downloaded from the server and can be viewed normally.
     * This does not include attachments, which are never downloaded fully.
     */
    X_DOWNLOADED_FULL(null, null),

    /**
     * Indicates that a message is partially downloaded from the server and can be viewed but
     * more content is available on the server.
     * This does not include attachments, which are never downloaded fully.
     */
    X_DOWNLOADED_PARTIAL(null, null),

    /**
     * Indicates that the copy of a message to the Sent folder has started.
     */
    X_REMOTE_COPY_STARTED(null, null),

    /**
     * Messages with this flag have been migrated from database version 50 or earlier.
     * This earlier database format did not preserve the original mime structure of a
     * mail, which means messages migrated to the newer database structure may be
     * incomplete or broken.
     * TODO Messages with this flag should be redownloaded, if possible.
     */
    X_MIGRATED_FROM_V50(null, null),

    /**
     * This flag is used for drafts where the message should be sent as PGP/INLINE.
     */
    X_DRAFT_OPENPGP_INLINE(null, null);

    private String requiredImapString;
    private String forbiddenImapString;

    Flag(String requiredImapString, String forbiddenImapString) {
        this.requiredImapString = requiredImapString;
        this.forbiddenImapString = forbiddenImapString;
    }

    public String getRequiredImapString() {
        return requiredImapString;
    }

    public String getForbiddenImapString() {
        return forbiddenImapString;
    }

}
