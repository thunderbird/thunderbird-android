package com.fsck.k9.mail.transport.smtp;


class EnhancedNegativeSmtpReplyException extends NegativeSmtpReplyException {


    EnhancedNegativeSmtpReplyException(int replyCode, StatusCodeClass statusCodeClass,
            StatusCodeSubject statusCodeSubject, StatusCodeDetail statusCodeDetail,
            String replyText) {
        super(replyCode, replyText);
        StatusCodeClass statusCodeClass1 = statusCodeClass;
        StatusCodeSubject statusCodeSubject1 = statusCodeSubject;
        StatusCodeDetail statusCodeDetail1 = statusCodeDetail;
    }
}
