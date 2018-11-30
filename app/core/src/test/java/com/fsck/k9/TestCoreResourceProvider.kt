package com.fsck.k9

class TestCoreResourceProvider : CoreResourceProvider {
    override fun defaultSignature() = "\n--\nbrevity!"

    override fun defaultIdentityDescription() = "initial identity"

    override fun sendAlternateChooserTitle(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun internalStorageProviderName(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun externalStorageProviderName(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun contactDisplayNamePrefix() = "To:"

    override fun messageHeaderFrom() = "From:"
    override fun messageHeaderTo() = "To:"
    override fun messageHeaderCc() = "Cc:"
    override fun messageHeaderDate() = "Sent:"
    override fun messageHeaderSubject() = "Subject:"
    override fun messageHeaderSeparator() = "-------- Original Message --------"

    override fun noSubject() = "(No subject)"

    override fun userAgent(): String = "K-9 Mail for Android"
    override fun encryptedSubject(): String = "Encrypted message"

    override fun replyHeader(sender: String) = "$sender wrote:"
    override fun replyHeader(sender: String, sentDate: String) = "On $sentDate, $sender wrote:"

    override fun searchAllMessagesTitle() = "All messages"
    override fun searchAllMessagesDetail() = "All messages in searchable folders"
    override fun searchUnifiedInboxTitle() = "Unified Inbox"
    override fun searchUnifiedInboxDetail() = "All messages in unified folders"

    override fun outboxFolderName() = "Outbox"
}
