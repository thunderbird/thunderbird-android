package com.fsck.k9.mail.transport.smtp;


class EnhancedNegativeSmtpReplyException extends NegativeSmtpReplyException {
    public final StatusCode statusCode;


    EnhancedNegativeSmtpReplyException(int replyCode, String replyText, StatusCode statusCode) {
        super(replyCode, replyText);
        this.statusCode = statusCode;
    }
}
