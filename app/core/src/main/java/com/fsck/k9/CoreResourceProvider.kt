package com.fsck.k9

interface CoreResourceProvider {
    fun defaultSignature(): String
    fun defaultIdentityDescription(): String

    fun sendAlternateChooserTitle(): String

    fun internalStorageProviderName(): String
    fun externalStorageProviderName(): String

    fun contactDisplayNamePrefix(): String

    fun messageHeaderFrom(): String
    fun messageHeaderTo(): String
    fun messageHeaderCc(): String
    fun messageHeaderDate(): String
    fun messageHeaderSubject(): String
    fun messageHeaderSeparator(): String

    fun noSubject(): String
    fun userAgent(): String

    fun encryptedSubject(): String
    fun replyHeader(sender: String): String
    fun replyHeader(sender: String, sentDate: String): String

    fun searchAllMessagesTitle(): String
    fun searchAllMessagesDetail(): String
    fun searchUnifiedInboxTitle(): String
    fun searchUnifiedInboxDetail(): String

    fun outboxFolderName(): String
}
