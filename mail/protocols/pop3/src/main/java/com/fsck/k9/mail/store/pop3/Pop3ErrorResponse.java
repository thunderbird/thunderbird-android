package com.fsck.k9.mail.store.pop3;


import net.thunderbird.core.common.exception.MessagingException;


/**
 * Exception that is thrown if the server returns an error response.
 */
class Pop3ErrorResponse extends MessagingException {
    private static final long serialVersionUID = 3672087845857867174L;

    public Pop3ErrorResponse(String message) {
        super(message, true);
    }

    public String getResponseText() {
        // TODO: Extract response text from response line
        return getMessage();
    }
}
