package com.fsck.k9.mail.transport.smtp;


enum SmtpEnhancedStatusCodeSubject {
    Undefined(0), Addressing(1), Mailbox(2), MailSystem(3), NetworkRouting(4),
    MailDeliveryProtocol(5), MessageContentOrMedia(6), SecurityOrPolicyStatus(7);

    private final int codeSubject;

    public static SmtpEnhancedStatusCodeSubject parse(String s) {
        int value = Integer.parseInt(s);
        for (SmtpEnhancedStatusCodeSubject classEnum: SmtpEnhancedStatusCodeSubject.values()) {
            if (classEnum.codeSubject == value) {
                return classEnum;
            }
        }
        return null;
    }

    SmtpEnhancedStatusCodeSubject(int codeSubject) {
        this.codeSubject = codeSubject;
    }
}
