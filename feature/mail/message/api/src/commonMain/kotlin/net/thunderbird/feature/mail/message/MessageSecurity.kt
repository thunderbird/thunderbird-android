package net.thunderbird.feature.mail.message

/**
 * Encryption and signature state of a message.
 *
 * @property encryption Whether the message is encrypted.
 * @property signature Whether the message is signed and whether the signature is valid.
 */
data class MessageSecurity(
    val encryption: EncryptionStatus = EncryptionStatus.UNKNOWN,
    val signature: SignatureStatus = SignatureStatus.UNKNOWN,
)

/**
 * Whether a message is encrypted.
 */
enum class EncryptionStatus {
    /** Not yet checked, e.g. because the body has not been downloaded. */
    UNKNOWN,

    /** Message is not encrypted. */
    NONE,

    /** Message is encrypted and was decrypted successfully. */
    ENCRYPTED,

    /** Message is encrypted but could not be decrypted. */
    DECRYPTION_FAILED,
}

/**
 * Whether a message is signed and whether the signature is valid.
 */
enum class SignatureStatus {
    /** Not yet checked, e.g. because the body has not been downloaded. */
    UNKNOWN,

    /** Message is not signed. */
    NONE,

    /** Message is signed and the signature is valid. */
    VALID,

    /** Message is signed but the signature is invalid. */
    INVALID,

    /** Message is signed but the signing key is unknown. */
    UNKNOWN_KEY,
}
