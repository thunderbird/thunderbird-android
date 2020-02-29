package com.fsck.k9

class TestCoreResourceProvider : CoreResourceProvider {
    override fun defaultSignature() = throw UnsupportedOperationException("not implemented")

    override fun defaultIdentityDescription() = throw UnsupportedOperationException("not implemented")

    override fun sendAlternateChooserTitle() = throw UnsupportedOperationException("not implemented")

    override fun internalStorageProviderName() = throw UnsupportedOperationException("not implemented")

    override fun externalStorageProviderName() = throw UnsupportedOperationException("not implemented")

    override fun contactDisplayNamePrefix() = throw UnsupportedOperationException("not implemented")

    override fun messageHeaderFrom() = throw UnsupportedOperationException("not implemented")
    override fun messageHeaderTo() = throw UnsupportedOperationException("not implemented")
    override fun messageHeaderCc() = throw UnsupportedOperationException("not implemented")
    override fun messageHeaderDate() = throw UnsupportedOperationException("not implemented")
    override fun messageHeaderSubject() = throw UnsupportedOperationException("not implemented")
    override fun messageHeaderSeparator() = throw UnsupportedOperationException("not implemented")

    override fun noSubject() = throw UnsupportedOperationException("not implemented")

    override fun userAgent(): String = "K-9 Mail for Android"
    override fun encryptedSubject(): String = "Encrypted message"

    override fun replyHeader(sender: String) = throw UnsupportedOperationException("not implemented")
    override fun replyHeader(sender: String, sentDate: String) = throw UnsupportedOperationException("not implemented")

    override fun searchAllMessagesTitle() = throw UnsupportedOperationException("not implemented")
    override fun searchAllMessagesDetail() = throw UnsupportedOperationException("not implemented")
    override fun searchUnifiedInboxTitle() = throw UnsupportedOperationException("not implemented")
    override fun searchUnifiedInboxDetail() = throw UnsupportedOperationException("not implemented")

    override fun outboxFolderName() = throw UnsupportedOperationException("not implemented")
}
