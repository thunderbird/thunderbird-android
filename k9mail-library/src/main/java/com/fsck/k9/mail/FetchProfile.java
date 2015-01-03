
package com.fsck.k9.mail;

import java.util.ArrayList;

/**
 * <pre>
 * A FetchProfile is a list of items that should be downloaded in bulk for a set of messages.
 * FetchProfile can contain the following objects:
 *      FetchProfile.Item:      Described below.
 *      Message:                Indicates that the body of the entire message should be fetched.
 *                              Synonymous with FetchProfile.Item.BODY.
 *      Part:                   Indicates that the given Part should be fetched. The provider
 *                              is expected have previously created the given BodyPart and stored
 *                              any information it needs to download the content.
 * </pre>
 */
public class FetchProfile extends ArrayList<FetchProfile.Item> {
    private static final long serialVersionUID = -5520076119120964166L;

    /**
     * Default items available for pre-fetching. It should be expected that any
     * item fetched by using these items could potentially include all of the
     * previous items.
     */
    public enum Item {
        /**
         * Download the flags of the message.
         */
        FLAGS,

        /**
         * Download the envelope of the message. This should include at minimum
         * the size and the following headers: date, subject, from, content-type, to, cc
         */
        ENVELOPE,

        /**
         * Download the structure of the message. This maps directly to IMAP's BODYSTRUCTURE
         * and may map to other providers.
         * The provider should, if possible, fill in a properly formatted MIME structure in
         * the message without actually downloading any message data. If the provider is not
         * capable of this operation it should specifically set the body of the message to null
         * so that upper levels can detect that a full body download is needed.
         */
        STRUCTURE,

        /**
         * A sane portion of the entire message, cut off at a provider determined limit.
         * This should generaly be around 50kB.
         */
        BODY_SANE,

        /**
         * The entire message.
         */
        BODY,
    }
}
