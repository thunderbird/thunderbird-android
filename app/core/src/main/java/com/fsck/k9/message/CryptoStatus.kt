package com.fsck.k9.message

interface CryptoStatus {
    val openPgpKeyId: Long?
    val isSenderPreferEncryptMutual: Boolean
    val isEncryptionEnabled: Boolean
    val isPgpInlineModeEnabled: Boolean
    val isSignOnly: Boolean
    fun isSigningEnabled(): Boolean
    fun isUserChoice(): Boolean
    val isReplyToEncrypted: Boolean
    fun hasRecipients(): Boolean
    val isEncryptAllDrafts: Boolean
    val isEncryptSubject: Boolean
    fun getRecipientAddresses(): Array<String>
}
