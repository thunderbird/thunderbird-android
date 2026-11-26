package net.thunderbird.feature.mail.message.composer.internal.smime

import java.io.ByteArrayOutputStream
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.feature.mail.message.composer.smime.SmimeVerificationResult
import net.thunderbird.feature.mail.message.composer.smime.SmimeVerificationStatus
import net.thunderbird.feature.mail.message.composer.smime.SmimeVerifier

internal class DefaultSmimeVerifier(
    private val cmsSignedDataVerifier: CmsSignedDataVerifier,
    private val logger: Logger,
) : SmimeVerifier {

    override fun verify(
        canonicalMimeContent: ByteArray,
        signatureBytes: ByteArray,
        senderEmail: String?,
    ): SmimeVerificationResult {
        val derBytes = signatureBytes.takeIf { it.isNotEmpty() }?.let(::decodeToDer)
        return if (derBytes == null) {
            SmimeVerificationResult(status = SmimeVerificationStatus.INVALID_SIGNATURE)
        } else {
            val result = cmsSignedDataVerifier.verifyDetachedSignature(
                canonicalMimeContent = canonicalMimeContent,
                cmsDerBytes = derBytes,
                senderEmail = senderEmail,
            )
            logger.debug(tag = TAG) { "S/MIME verification finished with status: ${result.status}" }
            result
        }
    }

    private fun decodeToDer(signatureBytes: ByteArray): ByteArray? {
        if (signatureBytes[0] == DerDecoder.TAG_SEQUENCE.toByte()) {
            return signatureBytes
        }
        val text = String(signatureBytes, Charsets.US_ASCII)
        val cleaned = buildString {
            for (line in text.lineSequence()) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("-----")) {
                    append(trimmed)
                }
            }
        }
        return decodeBase64(cleaned)
    }

    @Suppress("MagicNumber")
    private fun decodeBase64(input: String): ByteArray? {
        if (input.isEmpty()) return null
        val out = ByteArrayOutputStream((input.length * 3) / 4)
        var buffer = 0
        var bitsCollected = 0
        var isValid = true
        var index = 0

        while (index < input.length && isValid && input[index] != '=') {
            val value = decodeBase64Char(input[index])
            if (value == null) {
                isValid = false
            } else {
                buffer = (buffer shl 6) or value
                bitsCollected += 6
                if (bitsCollected >= 8) {
                    bitsCollected -= 8
                    out.write((buffer ushr bitsCollected) and 0xFF)
                }
            }
            index++
        }
        return if (isValid) out.toByteArray() else null
    }

    @Suppress("MagicNumber")
    private fun decodeBase64Char(ch: Char): Int? = when (ch) {
        in 'A'..'Z' -> ch.code - 'A'.code
        in 'a'..'z' -> ch.code - 'a'.code + 26
        in '0'..'9' -> ch.code - '0'.code + 52
        '+' -> 62
        '/' -> 63
        else -> null
    }

    private companion object {
        const val TAG = "DefaultSmimeVerifier"
    }
}
