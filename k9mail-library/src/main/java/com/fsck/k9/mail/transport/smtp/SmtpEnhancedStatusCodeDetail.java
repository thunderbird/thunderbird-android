package com.fsck.k9.mail.transport.smtp;


enum SmtpEnhancedStatusCodeDetail {
    Undefined(SmtpEnhancedStatusCodeSubject.Undefined, 0),
    OtherAddressStatus(SmtpEnhancedStatusCodeSubject.Addressing, 0),
    BadDestinationMailboxAddress(SmtpEnhancedStatusCodeSubject.Addressing, 1),
    BadDestinationSystemAddress(SmtpEnhancedStatusCodeSubject.Addressing, 2),
    BadDestinationMailboxAddressSyntax(SmtpEnhancedStatusCodeSubject.Addressing, 3),
    DestinationMailboxAddressAmbiguous(SmtpEnhancedStatusCodeSubject.Addressing, 4),
    DestinationAddressValid(SmtpEnhancedStatusCodeSubject.Addressing, 5),
    DestinationMailboxMoved(SmtpEnhancedStatusCodeSubject.Addressing, 6),
    BadSenderMailboxSyntax(SmtpEnhancedStatusCodeSubject.Addressing, 7),
    BadSenderSystemAddress(SmtpEnhancedStatusCodeSubject.Addressing, 8),

    OtherMailboxStatus(SmtpEnhancedStatusCodeSubject.Mailbox,0),
    MailboxDisabled(SmtpEnhancedStatusCodeSubject.Mailbox,1),
    MailboxFull(SmtpEnhancedStatusCodeSubject.Mailbox,2),
    MessageLengthExceeded(SmtpEnhancedStatusCodeSubject.Mailbox,3),
    MailingListExpansionProblem(SmtpEnhancedStatusCodeSubject.Mailbox,4),

    OtherMailSystemStatus(SmtpEnhancedStatusCodeSubject.MailSystem,0),
    MailSystemFull(SmtpEnhancedStatusCodeSubject.MailSystem,1),
    SystemNotAcceptingMessages(SmtpEnhancedStatusCodeSubject.MailSystem,2),
    SystemIncapableOfFeature(SmtpEnhancedStatusCodeSubject.MailSystem,3),
    MessageTooBig(SmtpEnhancedStatusCodeSubject.MailSystem,4),
    SystemIncorrectlyConfigured(SmtpEnhancedStatusCodeSubject.MailSystem,5),

    OtherNetworkRouting(SmtpEnhancedStatusCodeSubject.NetworkRouting,0),
    NoAnswerFromHost(SmtpEnhancedStatusCodeSubject.NetworkRouting,1),
    BadConnection(SmtpEnhancedStatusCodeSubject.NetworkRouting,2),
    DirectoryServerFailure(SmtpEnhancedStatusCodeSubject.NetworkRouting,3),
    UnableToRoute(SmtpEnhancedStatusCodeSubject.NetworkRouting,4),
    MailSystemCongestion(SmtpEnhancedStatusCodeSubject.NetworkRouting,5),
    RoutingLoopDetected(SmtpEnhancedStatusCodeSubject.NetworkRouting,6),
    DeliveryTimeExpired(SmtpEnhancedStatusCodeSubject.NetworkRouting,7),

    OtherMailDeliveryProtocol(SmtpEnhancedStatusCodeSubject.MailDeliveryProtocol,0),
    InvalidCommand(SmtpEnhancedStatusCodeSubject.MailDeliveryProtocol,1),
    SyntaxError(SmtpEnhancedStatusCodeSubject.MailDeliveryProtocol,2),
    TooManyRecipients(SmtpEnhancedStatusCodeSubject.MailDeliveryProtocol,3),
    InvalidCommandArguments(SmtpEnhancedStatusCodeSubject.MailDeliveryProtocol,4),
    WrongProtocolVersion(SmtpEnhancedStatusCodeSubject.MailDeliveryProtocol,5),

    OtherMessageContentOrMedia(SmtpEnhancedStatusCodeSubject.MessageContentOrMedia,0),
    MediaNotSupported(SmtpEnhancedStatusCodeSubject.MessageContentOrMedia,1),
    ConversionRequiredAndProhibited(SmtpEnhancedStatusCodeSubject.MessageContentOrMedia,2),
    ConversionRequiredButUnsupported(SmtpEnhancedStatusCodeSubject.MessageContentOrMedia,3),
    ConversionWithLossPerformed(SmtpEnhancedStatusCodeSubject.MessageContentOrMedia,4),
    ConversionFailed(SmtpEnhancedStatusCodeSubject.MessageContentOrMedia,5),

    OtherSecurityOrPolicyStatus(SmtpEnhancedStatusCodeSubject.SecurityOrPolicyStatus, 0),
    DeliveryNotAuthorized(SmtpEnhancedStatusCodeSubject.SecurityOrPolicyStatus, 1),
    MailingListExpansionProhibited(SmtpEnhancedStatusCodeSubject.SecurityOrPolicyStatus, 2),
    SecurityConversionRequired(SmtpEnhancedStatusCodeSubject.SecurityOrPolicyStatus, 3),
    SecurityFeaturesUnsupported(SmtpEnhancedStatusCodeSubject.SecurityOrPolicyStatus, 4),
    CryptographicFailure(SmtpEnhancedStatusCodeSubject.SecurityOrPolicyStatus, 5),
    CryptographicAlgorithmUnsupported(SmtpEnhancedStatusCodeSubject.SecurityOrPolicyStatus, 6),
    MessageIntegrityFailure(SmtpEnhancedStatusCodeSubject.SecurityOrPolicyStatus, 7);

    private final SmtpEnhancedStatusCodeSubject subject;
    private final int detail;

    SmtpEnhancedStatusCodeDetail(SmtpEnhancedStatusCodeSubject subject, int detail) {
        this.subject = subject;
        this.detail = detail;
    }

    public static SmtpEnhancedStatusCodeDetail parse(SmtpEnhancedStatusCodeSubject subject, String s) {
        int value = Integer.parseInt(s);
        for (SmtpEnhancedStatusCodeDetail detailEnum: SmtpEnhancedStatusCodeDetail.values()) {
            if (detailEnum.subject == subject && detailEnum.detail == value) {
                return detailEnum;
            }
        }
        return null;
    }
}
