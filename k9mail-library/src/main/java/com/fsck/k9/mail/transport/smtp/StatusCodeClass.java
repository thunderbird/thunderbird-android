package com.fsck.k9.mail.transport.smtp;


enum StatusCodeClass {
    SUCCESS(2),
    PERSISTENT_TRANSIENT_FAILURE(4),
    PERMANENT_FAILURE(5);


    private final int codeClass;


    static StatusCodeClass parse(String statusCodeClassString) {
        int value = Integer.parseInt(statusCodeClassString);
        for (StatusCodeClass classEnum : StatusCodeClass.values()) {
            if (classEnum.codeClass == value) {
                return classEnum;
            }
        }
        return  null;
    }

    StatusCodeClass(int codeClass) {
        this.codeClass = codeClass;
    }
}
