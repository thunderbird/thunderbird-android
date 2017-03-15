package com.fsck.k9.mail.transport.smtp;


class EnhancedNegativeSmtpReplyException extends NegativeSmtpReplyException {
    private final StatusCodeClass statusCodeClass;
    private final StatusCodeSubject statusCodeSubject;
    private final StatusCodeDetail statusCodeDetail;


    EnhancedNegativeSmtpReplyException(int replyCode, StatusCodeClass statusCodeClass,
            StatusCodeSubject statusCodeSubject, StatusCodeDetail statusCodeDetail,
            String replyText) {
        super(replyCode, replyText);
        this.statusCodeClass = statusCodeClass;
        this.statusCodeSubject = statusCodeSubject;
        this.statusCodeDetail = statusCodeDetail;
    }
}
