package com.fsck.k9.mail.transport.smtp;


enum StatusCodeDetail {
    UNDEFINED(StatusCodeSubject.UNDEFINED, 0),
    OTHER_ADDRESS_STATUS(StatusCodeSubject.ADDRESSING, 0),
    BAD_DESTINATION_MAILBOX_ADDRESS(StatusCodeSubject.ADDRESSING, 1),
    BAD_DESTINATION_SYSTEM_ADDRESS(StatusCodeSubject.ADDRESSING, 2),
    BAD_DESTINATION_MAILBOX_ADDRESS_SYNTAX(StatusCodeSubject.ADDRESSING, 3),
    DESTINATION_MAILBOX_ADDRESS_AMBIGUOUS(StatusCodeSubject.ADDRESSING, 4),
    DESTINATION_ADDRESS_VALID(StatusCodeSubject.ADDRESSING, 5),
    DESTINATION_MAILBOX_MOVED(StatusCodeSubject.ADDRESSING, 6),
    BAD_SENDER_MAILBOX_SYNTAX(StatusCodeSubject.ADDRESSING, 7),
    BAD_SENDER_SYSTEM_ADDRESS(StatusCodeSubject.ADDRESSING, 8),

    OTHER_MAILBOX_STATUS(StatusCodeSubject.MAILBOX, 0),
    MAILBOX_DISABLED(StatusCodeSubject.MAILBOX, 1),
    MAILBOX_FULL(StatusCodeSubject.MAILBOX, 2),
    MESSAGE_LENGTH_EXCEEDED(StatusCodeSubject.MAILBOX, 3),
    MAILING_LIST_EXPANSION_PROBLEM(StatusCodeSubject.MAILBOX, 4),

    OTHER_MAIL_SYSTEM_STATUS(StatusCodeSubject.MAIL_SYSTEM, 0),
    MAIL_SYSTEM_FULL(StatusCodeSubject.MAIL_SYSTEM, 1),
    SYSTEM_NOT_ACCEPTING_MESSAGES(StatusCodeSubject.MAIL_SYSTEM, 2),
    SYSTEM_INCAPABLE_OF_FEATURE(StatusCodeSubject.MAIL_SYSTEM, 3),
    MESSAGE_TOO_BIG(StatusCodeSubject.MAIL_SYSTEM, 4),
    SYSTEM_INCORRECTLY_CONFIGURED(StatusCodeSubject.MAIL_SYSTEM, 5),

    OTHER_NETWORK_ROUTING(StatusCodeSubject.NETWORK_ROUTING, 0),
    NO_ANSWER_FROM_HOST(StatusCodeSubject.NETWORK_ROUTING, 1),
    BAD_CONNECTION(StatusCodeSubject.NETWORK_ROUTING, 2),
    DIRECTORY_SERVER_FAILURE(StatusCodeSubject.NETWORK_ROUTING, 3),
    UNABLE_TO_ROUTE(StatusCodeSubject.NETWORK_ROUTING, 4),
    MAIL_SYSTEM_CONGESTION(StatusCodeSubject.NETWORK_ROUTING, 5),
    ROUTING_LOOP_DETECTED(StatusCodeSubject.NETWORK_ROUTING, 6),
    DELIVERY_TIME_EXPIRED(StatusCodeSubject.NETWORK_ROUTING, 7),

    OTHER_MAIL_DELIVERY_PROTOCOL(StatusCodeSubject.MAIL_DELIVERY_PROTOCOL, 0),
    INVALID_COMMAND(StatusCodeSubject.MAIL_DELIVERY_PROTOCOL, 1),
    SYNTAX_ERROR(StatusCodeSubject.MAIL_DELIVERY_PROTOCOL, 2),
    TOO_MANY_RECIPIENTS(StatusCodeSubject.MAIL_DELIVERY_PROTOCOL, 3),
    INVALID_COMMAND_ARGUMENTS(StatusCodeSubject.MAIL_DELIVERY_PROTOCOL, 4),
    WRONG_PROTOCOL_VERSION(StatusCodeSubject.MAIL_DELIVERY_PROTOCOL, 5),

    OTHER_MESSAGE_CONTENT_OR_MEDIA(StatusCodeSubject.MESSAGE_CONTENT_OR_MEDIA, 0),
    MEDIA_NOT_SUPPORTED(StatusCodeSubject.MESSAGE_CONTENT_OR_MEDIA, 1),
    CONVERSION_REQUIRED_AND_PROHIBITED(StatusCodeSubject.MESSAGE_CONTENT_OR_MEDIA, 2),
    CONVERSION_REQUIRED_BUT_UNSUPPORTED(StatusCodeSubject.MESSAGE_CONTENT_OR_MEDIA, 3),
    CONVERSION_WITH_LOSS_PERFORMED(StatusCodeSubject.MESSAGE_CONTENT_OR_MEDIA, 4),
    CONVERSION_FAILED(StatusCodeSubject.MESSAGE_CONTENT_OR_MEDIA, 5),

    OTHER_SECURITY_OR_POLICY_STATUS(StatusCodeSubject.SECURITY_OR_POLICY_STATUS, 0),
    DELIVERY_NOT_AUTHORIZED(StatusCodeSubject.SECURITY_OR_POLICY_STATUS, 1),
    MAILING_LIST_EXPANSION_PROHIBITED(StatusCodeSubject.SECURITY_OR_POLICY_STATUS, 2),
    SECURITY_CONVERSION_REQUIRED(StatusCodeSubject.SECURITY_OR_POLICY_STATUS, 3),
    SECURITY_FEATURES_UNSUPPORTED(StatusCodeSubject.SECURITY_OR_POLICY_STATUS, 4),
    CRYPTOGRAPHIC_FAILURE(StatusCodeSubject.SECURITY_OR_POLICY_STATUS, 5),
    CRYPTOGRAPHIC_ALGORITHM_UNSUPPORTED(StatusCodeSubject.SECURITY_OR_POLICY_STATUS, 6),
    MESSAGE_INTEGRITY_FAILURE(StatusCodeSubject.SECURITY_OR_POLICY_STATUS, 7);


    private final StatusCodeSubject subject;
    private final int detail;


    public static StatusCodeDetail parse(StatusCodeSubject statusCodeSubject, String statusCodeDetailString) {
        int value = Integer.parseInt(statusCodeDetailString);
        for (StatusCodeDetail detailEnum : StatusCodeDetail.values()) {
            if (detailEnum.subject == statusCodeSubject && detailEnum.detail == value) {
                return detailEnum;
            }
        }
        return null;
    }

    StatusCodeDetail(StatusCodeSubject subject, int detail) {
        this.subject = subject;
        this.detail = detail;
    }
}
