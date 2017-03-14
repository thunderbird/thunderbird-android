package com.fsck.k9.mail.transport.smtp;


class EnhancedNegativeSmtpReplyException extends NegativeSmtpReplyException {

    private final SmtpEnhancedStatusCodeClass escClass;
    private final SmtpEnhancedStatusCodeSubject escSubject;
    private final SmtpEnhancedStatusCodeDetail escDetail;

    public EnhancedNegativeSmtpReplyException(int replyCode, SmtpEnhancedStatusCodeClass escClass,
            SmtpEnhancedStatusCodeSubject escSubject, SmtpEnhancedStatusCodeDetail escDetail, String replyText) {
        super(replyCode, replyText);
        this.escClass = escClass;
        this.escSubject = escSubject;
        this.escDetail = escDetail;
    }
}
