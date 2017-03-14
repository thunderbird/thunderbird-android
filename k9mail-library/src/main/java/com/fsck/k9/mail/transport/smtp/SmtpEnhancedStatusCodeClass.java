package com.fsck.k9.mail.transport.smtp;


enum SmtpEnhancedStatusCodeClass {
    Success(2), PersistentTransientFailure(4), PermanentFailure(5);

    private final int codeClass;

    public static SmtpEnhancedStatusCodeClass parse(String s) {
        int value = Integer.parseInt(s);
        for (SmtpEnhancedStatusCodeClass classEnum: SmtpEnhancedStatusCodeClass.values()) {
            if (classEnum.codeClass == value) {
                return classEnum;
            }
        }
        return  null;
    }

    SmtpEnhancedStatusCodeClass(int codeClass) {
        this.codeClass = codeClass;
    }
}
