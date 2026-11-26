package net.thunderbird.feature.mail.message.composer.smime

enum class SmimeVerificationStatus {
    VALID_TRUSTED,
    VALID_UNTRUSTED,
    VALID_SENDER_MISMATCH,
    VALID_EXPIRED,
    INVALID_SIGNATURE,
    ERROR,
}

data class SmimeVerificationResult(
    val status: SmimeVerificationStatus,
    val signerDisplayName: String? = null,
    val signerEmail: String? = null,
    val issuerDisplayName: String? = null,
)

interface SmimeVerifier {
    fun verify(
        canonicalMimeContent: ByteArray,
        signatureBytes: ByteArray,
        senderEmail: String?,
    ): SmimeVerificationResult
}
