package com.fsck.k9.mail.transport.smtp;


enum StatusCodeClass {
    SUCCESS(2),
    PERSISTENT_TRANSIENT_FAILURE(4),
    PERMANENT_FAILURE(5);

    final int codeClass;

    StatusCodeClass(int codeClass) {
        this.codeClass = codeClass;
    }
}
