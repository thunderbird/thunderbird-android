package com.fsck.k9.notification

internal enum class AccountNotificationKind {
    CertificateErrorIncoming,
    CertificateErrorOutgoing,
    AuthenticationErrorIncoming,
    AuthenticationErrorOutgoing,

    Sync,
    SendFailed,

    SingleMessage,

    NewMailSummary,
}
