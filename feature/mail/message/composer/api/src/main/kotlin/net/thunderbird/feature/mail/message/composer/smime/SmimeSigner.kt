package net.thunderbird.feature.mail.message.composer.smime

data class SmimeSignatureResult(
    val derSignature: ByteArray,
    val base64Signature: ByteArray,
    val micAlg: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SmimeSignatureResult) return false
        return derSignature.contentEquals(other.derSignature) &&
            base64Signature.contentEquals(other.base64Signature) &&
            micAlg == other.micAlg
    }

    override fun hashCode(): Int {
        var result = derSignature.contentHashCode()
        result = 31 * result + base64Signature.contentHashCode()
        result = 31 * result + micAlg.hashCode()
        return result
    }
}

class SmimeSigningException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

interface SmimeSigner {
    @Throws(SmimeSigningException::class)
    fun sign(canonicalMimeContent: ByteArray, certificateAlias: String): SmimeSignatureResult
}
