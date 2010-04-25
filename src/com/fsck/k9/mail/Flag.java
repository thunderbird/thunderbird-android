
package com.fsck.k9.mail;

/**
 * Flags that can be applied to Messages.
 */
public enum Flag
{
    DELETED,
    SEEN,
    ANSWERED,
    FLAGGED,
    DRAFT,
    RECENT,

    /*
     * The following flags are for internal library use only.
     * TODO Eventually we should creates a Flags class that extends ArrayList that allows
     * these flags and Strings to represent user defined flags. At that point the below
     * flags should become user defined flags.
     */
    /**
     * Delete and remove from the LocalStore immediately.
     */
    X_DESTROYED,

    /**
     * Sending of an unsent message failed. It will be retried. Used to show status.
     */
    X_SEND_FAILED,

    /**
     * Sending of an unsent message is in progress.
     */
    X_SEND_IN_PROGRESS,

    /**
     * Indicates that a message is fully downloaded from the server and can be viewed normally.
     * This does not include attachments, which are never downloaded fully.
     */
    X_DOWNLOADED_FULL,

    /**
     * Indicates that a message is partially downloaded from the server and can be viewed but
     * more content is available on the server.
     * This does not include attachments, which are never downloaded fully.
     */
    X_DOWNLOADED_PARTIAL,

    /**
     * Indicates that the copy of a message to the Sent folder has started.
     */
    X_REMOTE_COPY_STARTED,
}
