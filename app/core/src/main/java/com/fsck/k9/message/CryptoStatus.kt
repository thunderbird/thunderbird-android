package com.fsck.k9.message

interface CryptoStatus {
    fun getOpenPgpKeyId(): Long?
    fun isProviderStateOk(): Boolean
    fun isSenderPreferEncryptMutual(): Boolean
    fun isSigningEnabled(): Boolean
    fun isEncryptionEnabled(): Boolean
    fun isPgpInlineModeEnabled(): Boolean
    fun isSignOnly(): Boolean
    fun isUserChoice(): Boolean
    fun isReplyToEncrypted(): Boolean
    fun hasRecipients(): Boolean
    fun isEncryptSubject(): Boolean
    fun getRecipientAddresses(): Array<String>
}
