package com.fsck.k9.mail.store.imap;

import com.fsck.k9.mail.filter.FixedLengthInputStream;

public interface ImapResponseCallback {
    /**
     * Callback method that is called by the parser when a literal string
     * is found in an IMAP response.
     *
     * @param response ImapResponse object with the fields that have been
     *                 parsed up until now (excluding the literal string).
     * @param literal  FixedLengthInputStream that can be used to access
     *                 the literal string.
     *
     * @return an Object that will be put in the ImapResponse object at the
     *         place of the literal string.
     *
     * @throws java.io.IOException passed-through if thrown by FixedLengthInputStream
     * @throws Exception if something goes wrong. Parsing will be resumed
     *                   and the exception will be thrown after the
     *                   complete IMAP response has been parsed.
     */
    public Object foundLiteral(ImapResponse response, FixedLengthInputStream literal) throws Exception;
}
