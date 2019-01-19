package com.fsck.k9.mail.transport.smtp;


enum StatusCodeSubject {
    UNDEFINED(0),
    ADDRESSING(1),
    MAILBOX(2),
    MAIL_SYSTEM(3),
    NETWORK_ROUTING(4),
    MAIL_DELIVERY_PROTOCOL(5),
    MESSAGE_CONTENT_OR_MEDIA(6),
    SECURITY_OR_POLICY_STATUS(7);


    private final int codeSubject;


    static StatusCodeSubject parse(String statusCodeSubjectString) {
        int value = Integer.parseInt(statusCodeSubjectString);
        for (StatusCodeSubject classEnum : StatusCodeSubject.values()) {
            if (classEnum.codeSubject == value) {
                return classEnum;
            }
        }
        return null;
    }

    StatusCodeSubject(int codeSubject) {
        this.codeSubject = codeSubject;
    }
}
